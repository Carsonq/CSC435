/**
 * Created by Zhen Qin on 11/01/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac AsyncJokeClient.java
 * command to run the program: java AsyncJokeClient [IP address of server name:port]
 *     The command of running with default setting: java AsyncJokeClient
 *         The client will connect to localhost, port 4545 by default.
 *     The command of running with arguments: java AsyncJokeClient [IP address of server name:port]
 *         Examples: java AsyncJokeClient localhost:4545 localhost:4546 75.102.226.122:4545
 *         It is support up to 26 arguments.
 * List of files needed for running the program:
 *     AsyncJokeServer.java
 *     AsyncJokeClient.java
 *     AsyncJokeAdminClient.java
 * Notes: Client-side of the JokeServer program.
 * before switching to another server, it is necessary to check if the server is running.
 * the client side cookie keep all states, every time request with cookies, server side takes charge of updating the cookies' value and pass new cookies to client side.
 */

// I/O lib
import java.io.*;
// Networking lib
import java.net.*;
// util lib
import java.util.*;

class StateHolder {
    private boolean[] processFlag;
    // counters for primary server, first character for joke, second character for proverb
    private String[] jpCounters;
    // the randomized sequences code for primary server, the first four character for joke, the last four for proverb
    private String[] jpOrders;
    // put feedback to a queue
    private Queue<String> displayQueue;

    // constructor
    StateHolder(int serverCount) {
        processFlag = new boolean[serverCount];
        jpCounters = new String[serverCount];
        jpOrders = new String[serverCount];

        for (int i = 0; i < serverCount; i++) {
            jpCounters[i] = "00";
            jpOrders[i] = "00000000";
            processFlag[0] = false;
        }

        displayQueue = new LinkedList<>();
    }

    public void insertQueue(String feedback) {
        displayQueue.add(feedback);
    }

    public String pollQueue() {
        return displayQueue.poll();
    }

    public boolean isDQEmpty() {
        return displayQueue.isEmpty();
    }

    public boolean getPFlag(int idx) {
        return processFlag[idx];
    }

    public String getJpCounters(int idx) {
        return jpCounters[idx];
    }

    public String getJpOrders(int idx) {
        return jpOrders[idx];
    }

    public void setPFlag(int idx, boolean f) {
        processFlag[idx] = f;
    }

    public void setJpCounters(int idx, String c) {
        jpCounters[idx] = c;
    }

    public void setJpOrders(int idx, String o) {
        jpOrders[idx] = o;
    }
}

// a thread to accept call back message from the server side
class UDPCallBackWorker extends Thread {
    DatagramSocket socket;
    DatagramPacket packet;
    StateHolder sHolder;
    ArrayList serverNamesPorts;

    // constructor
    UDPCallBackWorker(StateHolder holder, ArrayList snp) {
        sHolder = holder;
        serverNamesPorts = snp;
    }

    // override the run method, when start a thread, it will use the run method
    @Override
    public void run() {
        while (true) {
            String counterInfo;
            String newJpOder;
            String serverLabel;
            try {
                // set udp port 4444 by default
                socket = new DatagramSocket(4444);
                byte[] buf = new byte[256];
                // udp packet
                packet = new DatagramPacket(buf, buf.length);
                // accept packet
                socket.receive(packet);
                // parse the messages.
                String parsePacket = new String(packet.getData(), 0, packet.getLength());

                if (parsePacket != null) {
                    // parse information
                    counterInfo = parsePacket.substring(parsePacket.length() - 11, parsePacket.length() - 9);
                    newJpOder = parsePacket.substring(parsePacket.length() - 9, parsePacket.length() - 1);
                    serverLabel = parsePacket.substring(parsePacket.length() - 1);

                    // change the states
                    sHolder.setJpCounters(AsyncJokeClient.convertLetterToNumber(serverLabel) - 1, counterInfo);
                    sHolder.setJpOrders(AsyncJokeClient.convertLetterToNumber(serverLabel) - 1, newJpOder);
                    // insert to the message that want to be displayed to a queue
                    sHolder.insertQueue(serverLabel + parsePacket.substring(0, parsePacket.length() - 11));
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // close the socket
            socket.close();
        }
    }
}

public class AsyncJokeClient {
    // send and receive information with server side
    static void connectToServer(String userName, String serverName, int serverPort, String counters, String jpOrder, String serverLabel) {
        Socket sock;
        PrintStream toServer;

        try {
            // create client socket, connect to specified server
            sock = new Socket(serverName, serverPort);
            // create output stream that server side can get message form it.
            toServer = new PrintStream(sock.getOutputStream());
            // pass cookie info to server side
            toServer.println(serverLabel + counters + jpOrder + userName);
            // ensure server side can get the message immediately.
            toServer.flush();

            // close socket
            sock.close();
        } catch (IOException ex) {
            System.out.println("Socket error");
        }
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

    // convert letter to number
    static int convertLetterToNumber(String letterNumber) {
        return letterNumber.toUpperCase().charAt(0) - 65 + 1;
    }

    // convert number to letter
    static String convertNumberToLetter(int num) {
        return Character.toString((char) (num + 65 - 1));
    }

    // check if the a string only contains numbers except blank space
    static boolean isNumeric(String s) {
        return s.replace(" ", "").matches("[-+]?\\d*\\.?\\d+");
    }

    // check if it is a string with only one character.
    static boolean isSingleLetter(String s) {
        return s.length() == 1 && Character.isLetter(s.charAt(0));
    }

    // get the sum of all numbers that are listed in a string, split by blank space.
    static double getSum(String c) {
        double sumReuslt = 0;
        for (String n : c.trim().split(" ")) {
            sumReuslt += Double.parseDouble(n);
        }

        return sumReuslt;
    }

    public static void main(String[] args) {
        // declare variables with default values
        String userName = null;

        // the client can connect to many servers. the list contains all server's hostname and port number.
        ArrayList<String> serverNamesPorts = new ArrayList<String>();

        // set server name and server port, depends on the arguments
        if (args.length < 1) {
            // if there is no argument, connect to localhost, port 4545 by default
            serverNamesPorts.add("localhost:4545");
        } else {
            // if there are arguments
            for (String arg : args) {
                serverNamesPorts.add(arg);
            }
        }

        // prompt
        System.out.println("Zhen Qin's Async Joke Client.");
        System.out.println("You can connect to " + serverNamesPorts.toArray().length + " servers by typing in letter labels.");
        for (int i = 0; i < serverNamesPorts.toArray().length; i++) {
            System.out.println("Type in " + convertNumberToLetter(i + 1) + " means connect to server " + serverNamesPorts.toArray()[i]);
        }
        System.out.flush();

        // get stdin
        BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));

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

        // get all server labels.
        StringBuffer serverNameListPrompt = new StringBuffer();
        for (int i = 0; i < serverNamesPorts.toArray().length; i++) {
            serverNameListPrompt.append(convertNumberToLetter(i + 1));
            if (i != serverNamesPorts.toArray().length - 1) {
                serverNameListPrompt.append(", ");
            }
        }

        // new an instance of StateHolder
        // contain all states.
        StateHolder sHolder = new StateHolder(serverNamesPorts.toArray().length);

        // another thread to accept udp requests, which call back from server side to provide the jokes or proverbs.
        UDPCallBackWorker udpCallBackWorker = new UDPCallBackWorker(sHolder, serverNamesPorts);
        udpCallBackWorker.start();

        try {
            // read input
            String command;
            // to mark if the server which is wanted to be connected is set up
            Boolean serverFlag;

            do {
                // check if there is call back info
                // if so, display them
                while (! sHolder.isDQEmpty()) {
                    String disInfo = sHolder.pollQueue();
                    String serverLabel = disInfo.substring(0, 1).toUpperCase();
                    sHolder.setPFlag(convertLetterToNumber(serverLabel) - 1, false);
                    System.out.println("Server " + serverLabel + " responds: " + disInfo.substring(1));
                }

                // prompt
                System.out.format("Enter %s to get a joke or proverb, or numbers for sum: ", serverNameListPrompt);
                command = sysIn.readLine();

                if (isSingleLetter(command)) {        // single letter represent server label
                    // get the index of the server
                    int serverIndex = convertLetterToNumber(command);
                    if (serverIndex > serverNamesPorts.toArray().length) {
                        System.out.println("There is no server " + command);
                        continue;
                    }

                    // check if the client has already got the previous callback
                    // if not, it is forbidden to get ask for another joke or proverb from the same server.
                    if (sHolder.getPFlag(serverIndex - 1)) {
                        System.out.println("You have not gotten the previous callback from server " + command + ". Send request after you getting the response.");
                        continue;
                    }

                    // get the corresponding server name and port
                    String serverNamePort = serverNamesPorts.toArray()[serverIndex - 1].toString();
                    String serverName = serverNamePort.split(":")[0];
                    int serverPort = Integer.parseInt(serverNamePort.split(":")[1]);

                    // before switching, it is necessary to check if the server which we want to connect is already set up
                    serverFlag = isServerAvailable(serverName, serverPort);
                    if (serverFlag) {
                        connectToServer(userName, serverName, serverPort, sHolder.getJpCounters(serverIndex-1), sHolder.getJpOrders(serverIndex-1), command);
                        sHolder.setPFlag(serverIndex - 1, true);
                    } else {
                        System.out.println("Can not connect to server " + serverName + ":" + serverPort);
                    }
                } else if (isNumeric(command)) {
                    // get the sum of the numbers.
                    double commandSum = getSum(command);
                    System.out.println("Your sum is: " + commandSum);
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
