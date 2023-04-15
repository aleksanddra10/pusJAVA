package pus2022.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Level;

import static pus2022.server.Server.jsonParser;

public class UdpServer implements Runnable {
    public static int udpPort;
    private DatagramSocket serverSocket;
    public UdpServer(int port) throws IOException {
        serverSocket = new DatagramSocket(port);
        Server.logger.log(Level.INFO, "Created UdpServer instance on port {0}", Integer.toString(port));
    }

    @Override
   public void run() {
        for(;;) {
            byte[] buffer = new byte[65536];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            try {
                serverSocket.receive(datagramPacket);
                String line = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                JSONObject jsonObject = null;
                if(line == null) continue;
                try {
                    jsonObject = (JSONObject) jsonParser.parse(line);
                } catch (ParseException ex) {
                    Server.logger.log(Level.WARNING, "Error parsing packet: " + line);
                    continue;
                }
                InetAddress address = datagramPacket.getAddress();
                int port = datagramPacket.getPort();
                Server.logger.log(Level.INFO, "UDP packet with uuid=" + jsonObject.get("uuid") + " received from " + address.toString() + ":" + Integer.toString(port));

                // DatagramPacket answerPacket = new DatagramPacket(line.getBytes(), line.length(), address, port);
                // serverSocket.send(answerPacket);
            } catch(IOException ex) {}
        }
    }
}
