
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

//Just quit with Ctrl+C
public class BareBoneThreadBasedClient {
    private static DatagramSocket socket = null;
    private static int serverPort = 1234; //default
    public static final short MAGIC = (short) 0xC356; //2 byte
    public static final byte VERSION = 1; //1 byte
    public static final byte HELLO = 0;
    public static final byte DATA = 1;
    public static final int SessionID = ThreadLocalRandom.current().nextInt();
    private static int sequenceNum = 0;
    private static InetSocketAddress serverSocketAddress;

    //Without any argument then server is localhost:12345
    public static void main(String[] args) {
        init(args);

        listenToNetwork(); //a new thread listens to network

        listenToStdin(); //main thread listens to stdin
    }

    private static void listenToStdin() {
        Sender.getInstance().start();
    }

    //setup logger and server address and port
    private static void init(String[] args) {
        InetAddress serverAddr = InetAddress.getLoopbackAddress();
        int serverPort = 12345;

        if (args.length == 2) {
            try {
                serverAddr = InetAddress.getByName(args[0]);
                serverPort = Integer.parseInt(args[1]);
            } catch (UnknownHostException e) {
            }
        }
        serverSocketAddress = new InetSocketAddress(serverAddr, serverPort);
        try {
            socket = new DatagramSocket(serverSocketAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void listenToNetwork() {
       Receiver.getInstance().start();
    }

    private static class Receiver extends Thread {
        private Receiver() {
        }

        private static Receiver uniqueInstance = new Receiver();

        public static Receiver getInstance() {
            return uniqueInstance;
        }

        @Override
        public void run() {
            while(true){
                byte[] buf = new byte[65000]; //some large number
                DatagramPacket packetRecv = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packetRecv);
                } catch (SocketException e) {
                } catch (IOException e) {
                }

                byte[] data = packetRecv.getData();  //recv packet but don't do anything
                int packetSize = packetRecv.getLength();
            }
        }
    }

    private static class Sender extends Thread{
        //constructor must be private

        private Sender() {
        }

        private static Sender uniqueInstance = new Sender();

        public static Sender getInstance() {
            return uniqueInstance;
        }

        @Override
        public void run() {
            sendHello();
            Scanner scan = new Scanner(System.in); //Scanner could be slower
            String line;
            while (scan.hasNextLine()) {
                line = scan.nextLine();
                if (line.equals("q")) {
                    break;
                } else {
                    byte[] payload = line.getBytes();
                    ByteBuffer bb = ByteBuffer.allocate(12 + payload.length);
                    bb.order(ByteOrder.BIG_ENDIAN);
                    // put magic 0xC356 header (2 byte)
                    bb.putShort(MAGIC);

                    // put version (1 byte)
                    bb.put(VERSION);

                    // put command (1 byte)
                    bb.put(DATA);

                    // put sequence number (4 byte)
                    bb.putInt(sequenceNum++);

                    // put session id (4 byte)
                    bb.putInt(SessionID);

                    bb.put(payload, 0, payload.length);

                    bb.flip();

                    byte[] msg = bb.array();

                    //System.out.println(new String(bb.array(), StandardCharsets.UTF_8)); //print entire packet
                    DatagramPacket packet = new DatagramPacket(msg, msg.length, serverSocketAddress);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            }
            scan.close();
        }
    }


    private static void sendHello(){
        ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        // put magic 0xC356 header (2 byte)
        bb.putShort(MAGIC);

        // put version (1 byte)
        bb.put(VERSION);

        // put command (1 byte)
        bb.put(HELLO);

        // put sequence number (4 byte)
        bb.putInt(sequenceNum++);

        // put session id (4 byte)
        bb.putInt(SessionID);

        bb.flip();
        byte[] msg = bb.array();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, serverSocketAddress);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}