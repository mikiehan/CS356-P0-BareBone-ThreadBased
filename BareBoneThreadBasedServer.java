import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.util.Scanner;

public class BareBoneThreadBasedServer {

    private static DatagramSocket socket = null;
    private static int serverPort = 12345; //default
    public static final short MAGIC = (short) 0xC356; //2 byte
    public static final byte VERSION = 1; //1 byte
    public static final byte HELLO = 0;
    public static final byte ALIVE = 2;

    //Without any argument then server port is 12345 by default
    public static void main(String[] args) throws SocketException { //very bad exception handling
        if (args.length > 0) {
            serverPort = Integer.parseInt(args[0]);
        }

        listenToNetwork(); //a new thread listens to network

        listenToStdin(); //main thread listens to stdin

    }

    private static void listenToStdin() {
        Scanner scan = new Scanner(System.in);
        String line;
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            if (line.equals("q")) {
                break;
            }
        }
        scan.close();
        shutdown();
    }

    private static void shutdown(){
        Receiver.getInstance().interrupt(); //interrupt the Receiver thread
        socket.close(); //will cause SocketException on socket.receive at Receiver
    }

    /**
     * This method starts a new thread to listen to network
     */
    private static void listenToNetwork() throws SocketException {
        socket = new DatagramSocket(serverPort);
        System.out.println("Waiting on port " + serverPort + "...");
        Receiver.getInstance().start();
    }

    /**
     * Singleton class
     */
    private static class Receiver extends Thread {
        //constructor must be private
        private Receiver() {
        }

        private static Receiver uniqueInstance = new Receiver();

        public static Receiver getInstance() {
            return uniqueInstance;
        }

        @Override
        public void run() {
            int sequenceNum = 0;
            while (true) {
                //Step 1: recv packet
                byte[] buf = new byte[65000]; //some large number

                DatagramPacket packetRecv = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packetRecv);
                } catch (SocketException e) {
                } catch (IOException e) {
                }

                byte[] data = packetRecv.getData();
                int packetSize = packetRecv.getLength();

                //Step 2: Print packet
                ByteBuffer bb_rcv = ByteBuffer.wrap(data, 0, packetSize).order(ByteOrder.BIG_ENDIAN);
                System.out.println(StandardCharsets.UTF_8.decode(bb_rcv).toString());

                byte cmd = bb_rcv.get(3);
                int seqNum = bb_rcv.getInt(4);
                int sesID = bb_rcv.getInt(8);


                //Step 3: Send response packet
                ByteBuffer bb_send = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
                // put magic 0xC356 header (2 byte)
                bb_send.putShort(MAGIC);

                // put version (1 byte)
                bb_send.put(VERSION);
                // put command (1 byte)
                if (cmd == 0) {
                    bb_send.put(HELLO);
                    //System.out.println("sending HELLO" + sequenceNum);
                } else { //if not hello assume DATA
                    bb_send.put(ALIVE);
                    //System.out.println("sending ALIVE" + sequenceNum);
                }

                // put sequence number (4 byte)
                bb_send.putInt(sequenceNum++);

                // put session id (4 byte)
                bb_send.putInt(sesID);
                bb_send.flip();


                InetAddress clientAddr = packetRecv.getAddress();
                int clientPort = packetRecv.getPort();
                byte[] msg = bb_send.array();

                DatagramPacket packet = new DatagramPacket(msg, msg.length, clientAddr, clientPort);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }
}