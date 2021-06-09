import java.net.InetAddress;
import java.net.UnknownHostException;

public class RR {
    private String name;
    private byte[] IP = new byte[4];
    private boolean queryTypeA = false;
    private int byteLength;
    private int rdLength;

    public void printIP() {
        String address = "";
        try {
            InetAddress inetaddress = InetAddress.getByAddress(IP);
            address = inetaddress.toString().substring(1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (queryTypeA)
            System.out.println("IP\t" + address + "\t" + "Type A");
        else
            System.out.println("IP\t" + address + "\t");
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public void setRdLength(int rdLength) {
        this.rdLength = rdLength;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQueryType(boolean queryType) {
        this.queryTypeA = true;
    }

    public boolean getQueryType() {
        return queryTypeA;
    }

    public void setIP(byte[] IP) {
        this.IP = IP;
    }

    public byte[] getIP() {
        return IP;
    }
}
