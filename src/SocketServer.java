import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class SocketServer {

    public static void main(String[] args) throws IOException {
            byte[] buffer = new byte[1000];
            DatagramPacket packet = new DatagramPacket(buffer, 1000);
            DatagramSocket server = new DatagramSocket(8950);
            System.out.println("Listening...");
            server.receive(packet);
            String s = new String(packet.getData(), StandardCharsets.UTF_8);
            System.out.println("Received \"" + packet.getData() + "\"\nwhich is : " + "\"" + s+ "\"");
    }
}
