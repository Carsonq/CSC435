/**
 * Created by Zhen Qin on 11/01/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac AsyncJokeAdminClient.java
 * command to run the program: java AsyncJokeAdminClient [IP address of server name:port]
 *     The command of running with default setting: java AsyncJokeAdminClient
 *         The client will connect to localhost, port 5050 by default.
 *     The command of running with arguments: java AsyncJokeAdminClient [IP address of server name:port]
 *         Examples: java AsyncJokeClient localhost:5051 localhost:5052 75.102.226.122:5053
 *         It is support up to 26 arguments.
 * List of files needed for running the program:
 *     AsyncJokeServer.java
 *     AsyncJokeClient.java
 *     AsyncJokeAdminClient.java
 * Notes: Administration client of the JokeServer program.
 * Before switching to server, it is necessary to check if the server is running. otherwise will mess the switch flag up.
 * It is also possible when a server was running at the beginning and was out of order after a while, if another server is running,
 * you can change to the running server with typing the server labels like A, B, C.
 * Change mode for different servers separately by typing in switch mode.
 * get the current mode of certain server by typing in current mode.
 */

// I/O lib
import java.io.*;
// Networking lib
import java.net.*;
// util lib
import java.util.*;

public class AsyncJokeAdminClient {
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

    // convert letter to number
    static int convertLetterToNumber(String letterNumber) {
        return letterNumber.toUpperCase().charAt(0) - 65 + 1;
    }

    // convert number to letter
    static String convertNumberToLetter(Long num) {
        return Character.toString((char) (num + 65 - 1));
    }

    // check if it is a string with only one character.
    static boolean isSingleLetter(String s) {
        return s.length() == 1 && Character.isLetter(s.charAt(0));
    }

    public static void main(String[] args) {
        System.out.println("Zhen Qin's Async Joke Administration Client.");

        ArrayList<String> serverNamesPorts = new ArrayList<String>();

        // set server name and server port, depends on the arguments
        if (args.length < 1) {
            // if there is no argument, connect to localhost, port 5050 by default
            serverNamesPorts.add("localhost:5050");
        } else {
            // if there are arguments
            for (String arg : args) {
                serverNamesPorts.add(arg);
            }
        }

        // prompts
        System.out.println("You can connect to " + serverNamesPorts.toArray().length + " servers by typing in letter labels.");
        for (int i = 0; i < serverNamesPorts.toArray().length; i++) {
            System.out.println("Type in " + convertNumberToLetter((long) i + 1) + " means connect to server " + serverNamesPorts.toArray()[i]);
        }
        System.out.println("to change the server mode, please type in \"switch mode\"");
        System.out.println("to get the current mode of the server, please type in \"current mode\"");
        System.out.flush();

        // connect to the first server by default
        String serverName = serverNamesPorts.toArray()[0].toString().split(":")[0];
        int serverPort = Integer.parseInt(serverNamesPorts.toArray()[0].toString().split(":")[1]);
        String serverNameLabel = "A";
        System.out.println("Now communicating with the server A: " + serverName + ", port " + serverPort);

        // to get the current mode of the server
        String currentModeMsg = modeAction(serverName, serverPort, "current mode");
        // if return message is null, the server is not set up yet.
        if (currentModeMsg != null) {
            System.out.println(currentModeMsg);
        } else {
            System.out.println("Can not connect to the server A: " + serverName + ", port " + serverPort + " now.");
        }

        // get keyboard input
        BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            String command;
            String switchServerName;
            int switchServerPort;
            boolean serverFlag;
            String switchServerNameLabel;

            do {
                // declare keyboard input as command
                command = sysIn.readLine();
                if (isSingleLetter(command)) {        // single letter represent server label
                    switchServerNameLabel = command.toUpperCase();
                    int serverIndex = convertLetterToNumber(switchServerNameLabel);
                    if (serverIndex > serverNamesPorts.toArray().length) {
                        System.out.println("There is no server " + switchServerNameLabel);
                        continue;
                    }

                    // get the corresponding server name and port by using server label
                    String serverNamePort = serverNamesPorts.toArray()[serverIndex - 1].toString();
                    switchServerName = serverNamePort.split(":")[0];
                    switchServerPort = Integer.parseInt(serverNamePort.split(":")[1]);

                    // before switching, it is necessary to check if the server which we want to connect is already set up
                    serverFlag = isServerAvailable(switchServerName, switchServerPort);
                    if (serverFlag) {
                        serverName = switchServerName;
                        serverPort = switchServerPort;
                        serverNameLabel = switchServerNameLabel;
                        System.out.println("Now communicating with the server " + serverNameLabel + ": " + serverName + ", port " + serverPort);
                    } else {
                        System.out.println("Can not connect to the server " + switchServerNameLabel + ": " + switchServerName + ", port " + switchServerPort + " now. Still in the Server " + serverNameLabel);
                    }
                } else if (command.equals("switch mode")) {        // command "switch mode" means to change the mode of the connected server
                    // communicate with server
                    String switchModeMsg = modeAction(serverName, serverPort, "switch mode");
                    // print out the return message.
                    if (switchModeMsg != null) {
                        System.out.println("Server " + serverNameLabel + " response: " + switchModeMsg);
                    } else {
                        // if there is no return from server side, it means the server is not running.
                        System.out.println("Can not connect to the server " + serverNameLabel + ": " + serverName + ", port " + serverPort + " now. Failed to switch mode.");
                    }
                } else if (command.equals("current mode")) {        // command "current mode" means to get the current mode of the server
                    currentModeMsg = modeAction(serverName, serverPort, "current mode");
                    if (currentModeMsg != null) {
                        System.out.println("Server " + serverNameLabel + " response: " + currentModeMsg);
                    } else {
                        System.out.println("Can not connect to the server " + serverNameLabel + ": " + serverName + ", port " + serverPort + " now. Failed to get current mode.");
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