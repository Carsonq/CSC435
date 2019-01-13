/**
 * Created by Zhen Qin on 9/12/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac JokeClient.java
 * command to run the program: java JokeClient [IP address of hostname of the primary server] [IP address of hostname of the secondary server]
     if want to only connect to the primary server, the first argument is optional, without an argument, client will connect to localhost
     if want to connect to both primary and secondary servers, two arguments are needed, they can be the same.
     For example:
         only connect to primary server: java JokeClient / java JokeClient 192.168.33.114
         connect to both primary and secondary servers: java JokeClient localhost 192.168.33.114
 * List of files needed for running the program:
     JokeServer.java
     JokeClient.java
     JokeClientAdmin.java
 * Notes: Client-side of the JokeServer program.
     before switching to another server, it is necessary to check if the server is running.
     It is also possible when a server was running at the beginning and was out of order after a while, if another server is running,
         you can change to the running server with typing s. if another server is also not running or you indicate only connecting to
         primary server, you can quit the client or wait until the server is set up. if you want to a server which is not running, you willl
         still stay at the current server.
     use port to check which server is communicating with, server names may be the same.
     the client side cookie keep all states, every time request with cookies, server side takes charge of updating the cookies' value and pass new cookies to client side.
     When a server is stopped and then restarted, the client will continue the last conversation.
 */

import java.io.*;        // I/O lib
import java.net.*;       // Networking lib

public class JokeClient {
    // send and receive information with server side
    static String connectToServer(String userName, String serverName, int serverPort, String counters, String jpOrder) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String fromServerString;
        String counterInfo = counters;
        String newJpOder = jpOrder;

        try {
            // create client socket, connect to specified server
            sock = new Socket(serverName, serverPort);
            // get InputStream from socket, read server message through it.
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // create output stream that server side can get message form it.
            toServer = new PrintStream(sock.getOutputStream());
            // pass cookie info to server side
            toServer.println(counters+jpOrder+userName);
            // ensure server side can get the message immediately.
            toServer.flush();

            // read message from server output stream
            fromServerString = fromServer.readLine();
            if (fromServerString != null) {
                // parse information
                counterInfo = fromServerString.substring(fromServerString.length()-10, fromServerString.length()-8);
                newJpOder = fromServerString.substring(fromServerString.length()-8);
                System.out.println(fromServerString.substring(0, fromServerString.length()-10));
            }

            // close socket
            sock.close();
        } catch (IOException ex) {
            System.out.println("Socket error");
        }
        return counterInfo + newJpOder;
    }

    // to see if the server side is already set up
    static boolean isServerAvailable(String serverName, int serverPort) {
        boolean flag;

        try {
            Socket sock;
            // create client socket, connect to specified server
            sock = new Socket(serverName, serverPort);
            // close socket
            sock.close();
            flag = true;
        } catch (IOException ex) {
            flag = false;
        }

        return flag;
    }

    public static void main(String[] args) {
        // declare variables with default values
        String userName = null;
        String primaryServerName = "localhost";
        String secondaryServerName = null;
        int primaryServerPort = 4545;
        int secondaryServerPort = 4546;

        // set server name and server port, depends on the arguments
        // to use feature of switching between different servers, the second argument is needed to designate the secondary server
        if (args.length == 1) {
            primaryServerName = args[0];
        } else if (args.length > 1) {
            primaryServerName = args[0];
            secondaryServerName = args[1];
        }

        System.out.println("Zhen Qin's Joke Client.");

        // get stdin
        BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));

        // prompt
        System.out.println("Server one: " + primaryServerName + ", Port: " + primaryServerPort);
        if (secondaryServerName != null) {
            System.out.println("Server two: " + secondaryServerName + ", Port: " + secondaryServerPort);
            System.out.println("To switch servers, please type s");
        }
        System.out.flush();

        // default connect to primary server
        String serverName = primaryServerName;
        int serverPort = primaryServerPort;
        System.out.println("Now communicating with: " + serverName + ", port " + serverPort);

        // before start the request loop, it is required to input user name
        try {
            do {
                System.out.print("To use the system, please enter the username first (cannot be empty): ");
                System.out.flush();
                // user Name, delete blank space at the beginning and at the end
                userName = sysIn.readLine().trim();
            } while (userName.equals(""));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Press <Enter> to get joke or proverb.");

        try {
            // read input
            String command;
            // counters for primary server, first character for joke, second character for proverb
            String primaryCounters = "00";
            // counters for secondary server, first character for joke, second character for proverb
            String secondaryCounters = "00";
            // the randomized sequences code for primary server, the first four character for joke, the last four for proverb
            String primaryJpOrder = "00000000";
            // the randomized sequences code for secondary server, the first four character for joke, the last four for proverb
            String secondaryJpOrder = "00000000";
            String switchServerName;
            String returnInfo;
            int switchServerPort;
            // to mark if the server which is wanted to be connected is set up
            Boolean serverFlag;
            do {
                // read customer input as command
                command = sysIn.readLine();
                if (command.equals("s")) {        // s means to switch server between primary and secondary
                    if (secondaryServerName == null) {
                        // if the client was started up without second argument, we can not use switch function
                        System.out.println("No secondary server being used.");
                        continue;
                    }
                    // can not use server name to determine which server is connected, the name may be same
                    switchServerName = serverPort == secondaryServerPort ? primaryServerName : secondaryServerName;
                    switchServerPort = serverPort == secondaryServerPort ? primaryServerPort : secondaryServerPort;
                    // before switching, it is necessary to check if the server which we want to connect is already set up
                    serverFlag = isServerAvailable(switchServerName, switchServerPort);
                    if (serverFlag) {
                        System.out.println("Now communicating with: " + switchServerName + ", port " + switchServerPort);
                        serverName = switchServerName;
                        serverPort = switchServerPort;
                    } else {
                        System.out.println("Still in server " + serverName + ":" +  serverPort + ". Can not switch to server " + switchServerName + ":" +  switchServerPort + ". That server is down. Try later.");
                    }
                } else if (command.equals("")) {        // <Enter> empty input means to send request to server
                    // request joke or proverb, the returnInfo includes new cookies which contains counter and sequence pattern
                    if (serverPort == primaryServerPort) {
                        returnInfo = connectToServer(userName, serverName, serverPort, primaryCounters, primaryJpOrder);
                    } else {
                        returnInfo = connectToServer(userName, serverName, serverPort, secondaryCounters, secondaryJpOrder);
                    }

                    // when the connection fails, the return information may be null, if so, do nothing
                    if (returnInfo != null) {
                        // modify the value of cookie depends on which server is connected to
                        if (serverPort == primaryServerPort) {
                            primaryCounters = returnInfo.substring(0,2);
                            primaryJpOrder = returnInfo.substring(2);
                        } else {
                            secondaryCounters = returnInfo.substring(0,2);
                            secondaryJpOrder = returnInfo.substring(2);
                        }
                    } else {
                        System.out.println("Can not connect to the server " + serverName + ", port" + serverPort + ". Try later.");
                    }
                } else {
                    // this kind of commands will not be passed to server side
                    System.out.println("Unrecognized Command");
                }
            } while (!command.equals("quit"));        // quit means end the server.
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
