import java.nio.ByteBuffer;

public class Response {

    private byte [] ID = new byte[2];
    private byte[] responseBytes;
    private int QR, AA, TC, RD, RA;
    private int RCode, QDCount, ANCount, NSCount, ARCount;
    private int requestSize;
    private int queryType = 1;
    private RR[] answerRR;
    private RR[] additionalRR;
    private RR[] nsRR;
    private boolean noRecords = false;

    public Response(byte[] responseBytes, int s) {
        this.responseBytes = responseBytes;
        this.requestSize = s;
        parseHeader();
        if (!checkError())
            return;
        parseResponse();
    }

    public int getANCount() {
        return ANCount;
    }

    public RR[] getAdditionalRR() {
        return additionalRR;
    }

    private boolean checkError() {
        if (QR == 0)
            throw new RuntimeException("the query is not an answer");
        if(RCode == 0)
            return true;
        switch( this.RCode) {
            case 1:
                throw new RuntimeException("unable to interpret the query");
            case 2:
                throw new RuntimeException("server failure");
            case 3:
                System.out.println("DOMAIN DOES NOT EXIST");
                return false;
            case 4:
                throw new RuntimeException("name server does not support this query");
            case 5:
                throw new RuntimeException("refused for policy reasons");
        }
        return true;
    }

    private void parseHeader() {
        //validateResponseQuestionType();
        ByteBuffer buffer;
        //ID
        ID[0] = responseBytes[0];
        ID[1] = responseBytes[1];
        //QR
        this.QR = (responseBytes[2] >> 7) & 1;
        //AA
        this.AA = (responseBytes[2] >> 2) & 1;
        //TC
        this.TC = (responseBytes[2] >> 1) & 1;
        //RD
        this.RD = (responseBytes[2] >> 0) & 1;
        //RA
        this.RA = (responseBytes[3] >> 7) & 1;
        //RCODE
        this.RCode = responseBytes[3] & 0x0F;
        //QDCount
        byte[] QDCountB = { responseBytes[4], responseBytes[5] };
        buffer = ByteBuffer.wrap(QDCountB);
        QDCount = buffer.getShort();
        //ANCount
        byte[] ANCountB = { responseBytes[6], responseBytes[7] };
        buffer = ByteBuffer.wrap(ANCountB);
        ANCount = buffer.getShort();
        //NSCount
        byte[] NSCountB = { responseBytes[8], responseBytes[9] };
        buffer = ByteBuffer.wrap(NSCountB);
        NSCount = buffer.getShort();
        //ARCount
        byte[] ARCountB = { responseBytes[10], responseBytes[11] };
        buffer = ByteBuffer.wrap(ARCountB);
        ARCount = buffer.getShort();

        }

    public void printHeader(){
        System.out.println("------------Header Information------------");
        System.out.println("Fixed Query ID: " + ID[0] + ID[1]);
        System.out.println("QR: "+QR+" | AA: "+AA+" | TC: "+TC+" | RD: "+RD+" | RA: "+RA+" | RCODE: "+RCode);
        System.out.println("FOLLOWED BY: " + QDCount + " questions, " + ANCount + " answers, " + NSCount + " name server records, "+ ARCount + " additional information records");
        System.out.println("___________________________________________\n");

    }

    private void parseResponse() {
        int p = requestSize; //directly after question

        answerRR = new RR[ANCount];
        for(int i = 0; i < ANCount; i ++){
            answerRR[i] = this.obtainRecords(p);
            p += answerRR[i].getByteLength();
        }

        nsRR = new RR[NSCount];
        for(int i = 0; i < NSCount; i++){
            nsRR[i] = this.obtainRecords(p);
            p += obtainRecords(p).getByteLength();
        }

        additionalRR = new RR[ARCount];
        for(int i = 0; i < ARCount; i++){
            additionalRR[i] = this.obtainRecords(p);
            p += additionalRR[i].getByteLength();
        }
    }


    private RR obtainRecords(int index){
        RR result = new RR();
        String name = "";
        int countByte = index;

        //NAME
        String[] recordNameSection; //0: domain 1:bytes
        recordNameSection = getNameSection(index);
        countByte += Integer.parseInt(recordNameSection[1]);
        name = recordNameSection[1];
        result.setName(name);

        //TYPE A
        byte[] Qtype = new byte[2];
        Qtype[0] = responseBytes[countByte];
        Qtype[1] = responseBytes[countByte + 1];
        if (Qtype[0] == 0 && Qtype[1] == 1)
            result.setQueryType(true);
        countByte += 2;

        //CLASS IN
        countByte +=2;

        //TTL
        countByte +=4;

        //RDLength
        byte[] RDLength = { responseBytes[countByte], responseBytes[countByte + 1] };
        ByteBuffer buffer = ByteBuffer.wrap(RDLength);
        int rdLength = buffer.getShort();
        result.setRdLength(rdLength);
        countByte +=2;

        //RDATA
        byte[] byteAddress= { responseBytes[countByte], responseBytes[countByte + 1], responseBytes[countByte + 2], responseBytes[countByte + 3] };
        result.setIP(byteAddress);
        result.setByteLength(countByte + rdLength - index);

        return result;
    }

    private String[] getNameSection(int position){

        String[] result = new String[2];
        int wordSize = responseBytes[position];
        String name = "";
        boolean start = true;
        int count = 0;
        while(wordSize != 0){
            if (!start){
                name += ".";
            }
            if ((wordSize & 0xC0) == (int) 0xC0) {
                byte[] offset = { (byte) (responseBytes[position] & 0x3F), responseBytes[position + 1] };
                ByteBuffer wrapped = ByteBuffer.wrap(offset);
                name += getNameSection(wrapped.getShort())[0];
                position += 2;
                count +=2;
                wordSize = 0;
            }else{
                name += getWord(position);
                position += wordSize + 1;
                count += wordSize + 1;
                wordSize = responseBytes[position];
            }
            start = false;

        }
        result[0] = name;
        result[1] = String.valueOf(count);
        return result;
    }
    private String getWord(int pos){
        String word = "";
        int wordSize = responseBytes[pos];
        for(int i =0; i < wordSize; i++){
            word += (char) responseBytes[pos + i + 1];
        }
        return word;
    }

    public void printAnswer() {
        System.out.println("\n************ R E S U L T ************\n");
        if (ANCount <= 0  || noRecords) {
            System.out.println("NO ANSWER RECEIVED FOR REQUESTED QUERY");
            //return;
        }
        if (answerRR != null)
            for (RR r : answerRR){
                r.printIP();
            }
        System.out.println();
        if (this.ARCount > 0) {
            System.out.println("    HERE ARE ADDITIONAL INFORMATION  ");
            for (RR r : additionalRR){
                r.printIP();
            }
            System.out.println("***********************************\n");
        }
    }
}