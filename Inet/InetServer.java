/**
 * Created by Zhen Qin on 9/10/2016.
 * java version "1.8.0_92" (build 1.8.0_92-b14)
 * compliation command: javac InetServer.java
 * command to run the program: java InetServer
 * List of files needed for running the program:
   InetServer.java
   InetClient.java
 * Notes: Server-side of the InetServer program. The service listens at port 2046.
 */

import java.io.*;        // I/O lib
import java.net.*;       // Networking lib

class Worker extends Thread {
    Socket sock;
    Worker(Socket socket) {sock = socket;}        // constructor with one parameter which is client socket

    public void run() {        // when start method is invoked, the run method is executed.
        BufferedReader streamin;
        PrintStream streamout;
        try {
            streamin = new BufferedReader(new InputStreamReader(sock.getInputStream()));        // get InputStream from socket, read client message through it.
            streamout = new PrintStream(sock.getOutputStream());        // socket output stream that client can read message from it
            try {
                String name = streamin.readLine();      // get message from socket input stream
                System.out.println("Looking up " + name);
                printRemoteAddress(name, streamout);
                sock.close();        // close socket, otherwise it will cause readline blocking on cliend side.
            } catch (IOException ioe) {
                System.out.println("Server read error");
                ioe.printStackTrace();
            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    static void printRemoteAddress(String name, PrintStream streamout) {        // write result to socket that client can read.
        try {
            streamout.println("Looking up " + name + "...");
            InetAddress machine = InetAddress.getByName(name);        // use build-in class to process requests.
            streamout.println("Host name : " + machine.getHostName());
            streamout.println("Host IP : " + toText(machine.getAddress()));
        } catch (UnknownHostException ex) {
            streamout.println("Failed in attempt to look up " + name);
        }
    }

    static String toText(byte ip[]) {        // make ip address readable.
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < ip.length; ++i) {
            if (i > 0) result.append(".");
            result.append(0xff & ip[i]);
        }
        return result.toString();
    }
}

public class InetServer {
    public static void main(String[] args) throws IOException {
        int qlen = 5;        // maximum length of the queue of connections
        int port = 2046;      // the server socket will bound to the port 2046
        Socket sock;

        ServerSocket servSocket = new ServerSocket(port, qlen);        // create a ServerSocket instance

        System.out.println("Zhen Qin's Inet Server starting up, listening at port " + port + ".");
        while (true) {
            sock = servSocket.accept();        // listen for requests from clients, it is a block method.
            new Worker(sock).start();        // generate a new work thread to address a new request.

        }
    }
}
