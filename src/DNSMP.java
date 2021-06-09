import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class DNSMP {
    // google.com [B@378fd1ac
    // aut.ac.ir [B@37a71e93
    // www.soft98.ir [B@37a71e93
    // godaddy.com [B@378fd1ac

    //from 9
    // google.com from 9: [B@6e2c634b
    // aut.ac.ir from : [B@37a71e93

    //from 8
    //soft98: [B@49097b5d
    //aut.ac.ir [B@49097b5d
    //google [B@37a71e93
    //godaddy [B@37a71e93
    //youtube [B@37a71e93

    private byte[] DNSserver = new byte[]{9,9,9,9};
    private byte[] rootServer = new byte[]{(byte)198,41,0,4};
    private String domainNAme;
    private byte[] responseBytes = new byte[1024];
    private byte[] requestBytes;
    private int qNameLength;
    private Response response;
    private Scanner scan = new Scanner(System.in);
    private DatagramPacket requestPacket;

    public DNSMP(){
        //localConnection();
        connect();
    }

    public static void main(String[] args){
        new DNSMP();
    }

    private void localConnection() {
        System.out.println("_____________LOCAL CONNECTION SETUP_____________");
        System.out.println("Enter the string that you want to send to the Local Host:");
        String msg = scan.nextLine();
        DatagramSocket local = null;
        try {
            local = new DatagramSocket();
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramPacket localrequest = new DatagramPacket(msg.getBytes(), msg.getBytes().length, localHost, 8950);
            local.send(localrequest);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("sent packet to local server");
        System.out.println("_____________LOCAL CONNECTION ENDED_____________");
    }

    private void connect(){
            try {
                //Create Datagram socket and request object(s)
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(5000);
                InetAddress inetaddress = InetAddress.getByAddress(rootServer);
                System.out.println("\n_____________DNS CONNECTION_____________");
                System.out.println("Enter your URL:");
                domainNAme = scan.nextLine();
                requestBytes = setRequest();

                DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, inetaddress, 53);
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);
                socket.send(requestPacket);
                System.out.println("\nsent packet to DNS server");
                socket.receive(responsePacket);
                System.out.println("received");
                socket.close();

                responseBytes = responsePacket.getData();
                System.out.println(responseBytes+"\n");
                response = new Response(responseBytes, requestBytes.length);
                response.printHeader();
                response.printAnswer();
                if(needIteration(response))
                    iterate(response);

            }catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    private boolean needIteration(Response response) {
        if (response.getANCount() >= 1 )
            return false;
        return true;
    }

    private Response connectServer(byte [] IPadress){
        byte[] recievedBytes = new byte[1024];
        Response newResponse = null;
        System.out.println("\n////Trying Another Server....");
        try{
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket recieved = new DatagramPacket(recievedBytes, 1024);
            InetAddress server = InetAddress.getByAddress(IPadress);
            DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, server, 53);
            //if (server.toString().substring(1).equals("193.189.123.2"))
              //  return null;
            System.out.println("..." + server.toString().substring(1) + "....");
            socket.send(requestPacket);
            socket.receive(recieved);
            recievedBytes = recieved.getData();
            newResponse = new Response(recievedBytes, requestBytes.length);
            newResponse.printHeader();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newResponse;
    }

    private void iterate(Response res) {
        Response response;
        for (RR r : res.getAdditionalRR()) {
            if (r.getQueryType()) {
                response = connectServer(r.getIP());
                if (response == null)
                    continue;
                if (response.getANCount() >= 1){
                    response.printAnswer();
                    return;
                }
                else{
                    for (RR rc : response.getAdditionalRR()){
                        if (rc.getQueryType()) {
                            response = connectServer(rc.getIP());
                            if (response == null)
                                continue;
                            if (response.getANCount() >= 1){
                                response.printAnswer();
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private byte[] setRequest(){
        //getbytelength()
        int qNameLength = 0;
        String[] items = domainNAme.split("\\.");
        for(int i=0; i < items.length; i ++){
            qNameLength += items[i].length() + 1;
        }
        ByteBuffer request = ByteBuffer.allocate(12 + 5 + qNameLength); //header //question //name
        this.qNameLength = qNameLength;
        request.put(setHeader());
        request.put(setQNAME(qNameLength));
        return request.array();
    }

    private byte[] setHeader(){
        ByteBuffer header = ByteBuffer.allocate(12);
        //byte[] randomID = new byte[2];
        //new Random().nextBytes(randomID);
        //ID, QR Opcode AA TC RD RA Z RCODE, QDCount, ANCount, NSCount, ARCount
        byte [] ID = new byte[]{0,27};
        header.put(ID);
        //System.out.println("req id  "+ ID[0] +ID[1]);
        header.put((byte)0x01);
        header.put((byte)0x00);
        header.put((byte)0x00);
        header.put((byte)0x01);
        return header.array();
    }

    private byte[] setQNAME(int qNameLength){
        ByteBuffer question = ByteBuffer.allocate(qNameLength+5);
        //first calculate how many bytes we need so we know the size of the array
        String[] items = domainNAme.split("\\.");
        for(int i=0; i < items.length; i ++){
            question.put((byte) items[i].length());
            for (int j = 0; j < items[i].length(); j++){
                question.put((byte) ((int) items[i].charAt(j)));
            }
        }
        question.put((byte) 0x00);
        //hexStringToByteArray
        int len = "0001".length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit("0001".charAt(i), 16) << 4)
                    + Character.digit("0001".charAt(i+1), 16));
        }
        //Add Query Type
        question.put(data);
        question.put((byte) 0x00);
        //Add Query Class - always  0x0001 for internet addresses
        question.put((byte) 0x0001);
        return question.array();
    }

}
