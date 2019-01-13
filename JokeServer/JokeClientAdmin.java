/**
 * Created by Zhen Qin on 9/12/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac JokeClientAdmin.java
 * command to run the program: java JokeClientAdmin [IP address of hostname of the primary server] [IP address of hostname of the secondary server]
     if want to only connect to the primary server, the first argument is optional, without an argument, client will connect to localhost
     if want to connect to both primary and secondary servers, two arguments are needed, they can be the same.
     For example:
         only connect to primary server: java JokeClientAdmin / java JokeClientAdmin 192.168.33.114
         connect to both primary and secondary servers: java JokeClientAdmin localhost 192.168.33.114
 * List of files needed for running the program:
     JokeServer.java
     JokeClient.java
     JokeClientAdmin.java
 * Notes: Administration client of the JokeServer program.
     Before switching to another server, it is necessary to check if the server is running. otherwise will mess the switch flag up.
     It is also possible when a server was running at the beginning and was out of order after a while, if another server is running,
         you can change to the running server with typing s. if another server is also not running or you indicate only connecting to
         primary server, you can quit the client or wait until the server is set up. if you want to a server which is not running, you willl
         still stay at the current server.
     Change mode for different servers separately.
 */

import java.io.*;        // I/O lib
import java.net.*;       // Networking lib

public class JokeClientAdmin {
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

    // connect to server
    static String modeAction(String serverName, int serverPort, String command) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String fromServerString = null;

        try {
            // create client socket, connect to specified server
            sock = new Socket(serverName, serverPort);
            // get InputStream from socket, read server message through it.
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // create output stream that server side can get message form it.
            toServer = new PrintStream(sock.getOutputStream());
            // pass customer command to server side
            toServer.println(command);
            // ensure server side can get the message immediately.
            toServer.flush();
            // feedback from server side
            fromServerString = fromServer.readLine();
            // close socket
            sock.close();
        } catch (IOException ex) {
            System.out.println("Socket error");
        }

        return fromServerString;
    }

    public static void main(String[] args) {
        System.out.println("Zhen Qin's Joke Administration Client.");
        // initialize server name and server port for both primary and secondary server
        // primary server (default value): localhost:5050
        String primaryServerName = "localhost";
        int primaryServerPort = 5050;
        String secondaryServerName = null;
        int secondaryServerPort = 5051;

        // set server name and server port, depends on the arguments
        if (args.length == 1) {
            primaryServerName = args[0];
        } else if (args.length > 1) {
            primaryServerName = args[0];
            secondaryServerName = args[1];
        }

        System.out.println("Server one: " + primaryServerName + ", Port: " + primaryServerPort);
        if (secondaryServerName != null) {
            // if need to connect to secondary server, give prompt
            System.out.println("Server two: " + secondaryServerName + ", Port: " + secondaryServerPort);
            // if a secondary server is available, you can type s to switch between primary and secondary servers
            System.out.println("to switch between primary and secondary server, please type s");
        }
        System.out.println("to change the server mode, please type x");
        System.out.println("to get the current mode of the server, please type c");

        System.out.flush();

        // connect to primary server by default
        String serverName = primaryServerName;
        int serverPort = primaryServerPort;
        System.out.println("Now communicating with: " + serverName + ", port " + serverPort);

        // to get the current mode of the server
        String currentModeMsg = modeAction(primaryServerName, primaryServerPort, "current mode");
        // if return message is null, the server is not set up yet.
        if (currentModeMsg != null) {
            System.out.println(currentModeMsg);
        } else {
            System.out.println("Can not connect to the server " + serverName + ", port" + serverPort);
        }

        // get keyboard input
        BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            String command;
            String switchServerName;
            int switchServerPort;
            Boolean serverFlag;
            do {
                // declare keyboard input as command
                command = sysIn.readLine();
                if (command.equals("x")) {        // command x means to change the mode of the connected server
                    // communicate with server
                    String switchModeMsg = modeAction(serverName, serverPort, "switch mode");
                    // print out the return message.
                    if (switchModeMsg != null) {
                        System.out.println(switchModeMsg);
                    } else {
                        // if there is no return from server side, it means the server is not running.
                        System.out.println("Can not connect to the server " + serverName + ", port" + serverPort + ". Try later.");
                    }
                } else if (command.equals("c")) {        // command c means to get the current mode of the server
                    currentModeMsg = modeAction(serverName, serverPort, "current mode");
                    if (currentModeMsg != null) {
                        System.out.println(currentModeMsg);
                    } else {
                        System.out.println("Can not connect to the server " + serverName + ", port" + serverPort + ". Try later.");
                    }
                } else if (command.equals("s")) {        // command s means to switch between primary and secondary server
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
                } else {
                    // this kind of command is not necessary to be passed to server side
                    System.out.println("Unrecognized Command");
                }
            } while (!command.equals("quit"));        // command quit means to quit the JokeClientAdmin service
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
