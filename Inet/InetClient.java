/**
 * Created by Zhen Qin on 9/10/2016.
 * java version "1.8.0_92" (build 1.8.0_92-b14)
 * compliation command: javac InetClient.java
 * command to run the program: java InetClient [IP address of the server]
   When client and server run in different machine, passing the IP address of the server to 
   the clients is necessary. For example:
   client and server run in the same machine: java InetClient
   client and server run in different machine: java InetClient 69.171.230.68
 * List of files needed for running the program:
   InetServer.java
   InetClient.java
 * Notes: Client-side of the InetServer program.
 */

import java.io.*;        // I/O lib
import java.net.*;       // Networking lib

public class InetClient {        // fuction toText is not needed in client side.
    static void getRemoteAddress(String name, String serverName, int serverPort) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String fromServerString;

        try {
            sock = new Socket(serverName, serverPort);        // create client socket, connect to specified server
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));        // get InputStream from socket, read server message through it.
            toServer = new PrintStream(sock.getOutputStream());        // create output stream that server side can get message form it.
            toServer.println(name);        // write name to output stream, server side can get it
            toServer.flush();        // ensure server side can get the message immediately.

            for (int i = 1; i <= 3; i++) {        // maximum 3 lines, it needs server-side socket closed, when the message less than 3 lines (when UnknownHostException occurs). 
                fromServerString = fromServer.readLine();        // read message from server output stream
                if (fromServerString != null) {
                    System.out.println(fromServerString);
                }
            }
            sock.close();        // close socket
        } catch (IOException ex) {
            System.out.println("Socket error");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverName;
        if (args.length < 1) {        // use first parameter as serverName. if do not give the parameter, user default value localhost.
            serverName = "localhost";
        } else {
            serverName = args[0];
        }
        int serverPort = 2046;

        System.out.println("Zhen Qin's Inet Client.");
        System.out.println("Using server : " + serverName + ", Port: " + serverPort);
        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));        // get stdin
        try {
            String name;
            do {
                System.out.println("Enter a hostname or an IP address, (quit) to end: ");
                System.out.flush();
                name = sysin.readLine();        // get user typein as name 
                
                if (!name.equals("quit")) {        // do not use indexOf, otherwise any string contain "quit" will cuase service stop
                    getRemoteAddress(name, serverName, serverPort);
                }
            } while (!name.equals("quit"));
            System.out.println("Cancelled by user request.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
