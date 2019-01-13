/**
 * Created by Zhen Qin on 11/01/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac AsyncJokeServer.java
 * Command to run the program: java AsyncJokeServer [port number for joke & proverb service] [port number for admin]
 *     The command of running with default setting: java AsyncJokeServer
 *         The default port number for joke & proverbs service is 4545.
 *         The default port number for admin is 5050.
 *         They are as the same as the default port settings of normal joke server.
 *     The command of running with custom port number for joke & proverb service: java AsyncJokeServer [port number for joke & proverb service]
 *         Example: java AsyncJokeServer 4546.
 *         The admin port is 5050 as the default setting.
 *     The command of running with both custom port number for joke & proverb service and admin service: java AsyncJokeServer [port number for joke & proverb service] [port number for admin]
 *         Example: java AsyncJokeServer 4546 5051.
 * List of files needed for running the program:
 *     AsyncJokeServer.java
 *     AsyncJokeClient.java
 *     AsyncJokeAdminClient.java
 * Notes: Server-side of the AsyncJokeServer program. The server listens at port 4545 and 5050 by default.
 * It accept command line arguments. The first argument is the port number for normal joke and proverb service.
 * The second argument is for admin service. Both of them are all optional.
 * To maintain states, I keep all states on client side.
 */

import java.io.*;              // I/O lib
import java.net.*;             // Networking lib
import java.util.*;            // util lib

// declare two kinds of server mode
enum ServerMode {JOKEMODE, PROVERBMODE}

// to process requests from JokeClient
class JPWorker extends Thread {
    Socket sock;
    int sleepTime;

    // constructor
    JPWorker(Socket socket, int timeGap) {
        sock = socket;
        sleepTime = timeGap;
    }

    // when start method is invoked, the run method is executed.
    public void run() {
        BufferedReader streamIn;
        // PrintStream streamOut;
        try {
            // get InputStream from socket, read client message through it.
            streamIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            try {
                // every request keeps a lot of state information, depends on that, we can ensure what should be pushed to client
                String clientCookie = streamIn.readLine();

                // get remote IP address for call back using UDP
                String remoteAddress = sock.getInetAddress().getHostAddress();
                // after receiving a request, close socket
                sock.close();

                // the thread sleeps
                Thread.sleep(sleepTime);

                // null check to avoid NullPointerException
                if (clientCookie != null) {
                    // parse client cookie
                    String serverLabel = clientCookie.substring(0, 1);
                    // counter for jokes
                    String jokeCounter = clientCookie.substring(1, 2);
                    // counter for proverbs
                    String proverbCounter = clientCookie.substring(2, 3);
                    // the string contains current order of joke set and proverb set
                    String jpOrder = clientCookie.substring(3,11);
                    // user name, come from user input, it is not necessary to be unique in this design
                    String userName = clientCookie.substring(11);

                    // depends on cookie, assemble return messages
                    callbackClient(userName, jokeCounter, proverbCounter, jpOrder, serverLabel, remoteAddress);
                }
            } catch (IOException ioe) {
                System.out.println("Server read error");
                ioe.printStackTrace();
            } catch (InterruptedException ioe) {
                ioe.printStackTrace();
            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    static String getNewOrder() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(i);
        }
        Collections.shuffle(list);
        StringBuffer orderString = new StringBuffer();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            orderString.append(iter.next());
        }
        return orderString.toString();
    }

    static String assebmbleMessage(String userName, int jokeCounter, int proverbCounter, String jpOrder, String serverLabel) {
        // declare the order of joke set, due to the size of joke set is smaller than 10, I do not use any delimiter to split each other
        String jOrder;
        // declare the order of proverb set, the same as jOrder
        String pOrder;
        // the real index of jokeTemplates or proverbTemplates
        int jpIndex;
        String[] jokeTemplates  =  {"Anton, do you think I'm a bad mother? My name is Paul.",
                "Can a kangaroo jump higher than a house? Of course, a house doesn't jump at all.",
                "My wife's cooking is so bad we usually pray after our food.",
                "I'd like to buy a new boomerang please. Also, can you tell me how to throw the old one away?"};
        String[] proverbTemplates  = {"There's no such thing as a free lunch.",
                "God helps those who help themselves.",
                "Too many cooks spoil the broth.",
                "There's no time like the present."};
        String[] jokeSetIndex = {"JA", "JB", "JC", "JD"};
        String[] proverbSetIndex = {"PA", "PB", "PC", "PD"};

        if (AsyncJokeServer.serverMode.equals(ServerMode.JOKEMODE)) {
            // get the order of joke set
            jOrder = jokeCounter == 0 ? getNewOrder() : jpOrder.substring(0,4);
            // get the order of joke set
            pOrder = jpOrder.substring(4);
            // get the index of joke set
            jpIndex = Integer.parseInt(jOrder.substring(jokeCounter, jokeCounter + 1));
            // assemble the return message which will be pushed to client
            String returnMsg = jokeSetIndex[jokeCounter] + " " + userName + ": " + jokeTemplates[jpIndex];
            jokeCounter = (jokeCounter + 1) % jokeTemplates.length;
            String counterInfo = Integer.toString(jokeCounter) + Integer.toString(proverbCounter);
            // the message is not only contain the information that will be showed up on the client's screen
            // but also contain cookie information that will be parse by client side
            return returnMsg + counterInfo + jOrder + pOrder + serverLabel;
        } else {
            // get the order of joke set
            jOrder = jpOrder.substring(0,4);
            // get the order of joke set
            pOrder = proverbCounter == 0 ? getNewOrder() : jpOrder.substring(4);
            // get the index of proverb set
            jpIndex = Integer.parseInt(pOrder.substring(proverbCounter, proverbCounter + 1));
            // assemble the return message which will be pushed to client
            String returnMsg = proverbSetIndex[proverbCounter] + " " + userName + ": " + proverbTemplates[jpIndex];
            proverbCounter = (proverbCounter + 1) % proverbTemplates.length;
            String counterInfo = Integer.toString(jokeCounter) + Integer.toString(proverbCounter);
            return returnMsg + counterInfo + jOrder + pOrder + serverLabel;
        }
    }

    // write result to socket that client can read.
    static void callbackClient(String userName, String jokeCounter, String proverbCounter, String jpOrder, String serverLabel, String remoteAddress) throws IOException {
        // get feedback message
        String resultMsg = assebmbleMessage(userName, Integer.parseInt(jokeCounter), Integer.parseInt(proverbCounter), jpOrder, serverLabel);

        // UDP socket
        DatagramSocket clientUDPSocket = new DatagramSocket();

        byte[] sendBuf;
        sendBuf = resultMsg.getBytes();
        InetAddress addr = InetAddress.getByName(remoteAddress);
        // UDP using port 4444
        int port = 4444;
        // UDP packet
        DatagramPacket sendPacket = new DatagramPacket(sendBuf ,sendBuf.length , addr , port);
        // send the message using UDP
        clientUDPSocket.send(sendPacket);
        // close the UDP socket.
        clientUDPSocket.close();
    }
}

// to process requests of JokeClientAdmin
class AdminWorker extends Thread {
    Socket sock;

    // constructor
    AdminWorker(Socket socket) {
        sock = socket;
    }

    // when start method is invoked, the run method is executed.
    public void run() {
        BufferedReader streamIn;
        PrintStream streamOut;
        try {
            // get InputStream from socket, read client message through it.
            streamIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // socket output stream that client can read message from it
            streamOut = new PrintStream(sock.getOutputStream());
            try {
                // declare the requests from adminClient as input command
                String inputCommand = streamIn.readLine();
                // null value check is necessary, otherwise it will cause NullPointerException
                if (inputCommand != null) {
                    if (inputCommand.equals("current mode")) {        // request current server mode
                        streamOut.println("Server current mode: " + AsyncJokeServer.serverMode);
                    } else if (inputCommand.equals("switch mode")) {
                        // switch server mode
                        AsyncJokeServer.serverMode = AsyncJokeServer.serverMode == ServerMode.JOKEMODE ? ServerMode.PROVERBMODE : ServerMode.JOKEMODE;
                        streamOut.println("Server has been changed to mode: " + AsyncJokeServer.serverMode);
                        System.out.println("The server mode is changed. The current server mode is " + AsyncJokeServer.serverMode + ".");
                    }
                    // close socket, otherwise it may cause readline blocking on cliend side.
                    sock.close();
                }
            } catch (IOException ioe) {
                System.out.println("Server read error");
                ioe.printStackTrace();
            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

class AdminController extends Thread {
    int adminPort;

    // constructor
    AdminController(int adminPort) {
        this.adminPort = adminPort;
    }

    // override the run method, when start a thread, it will use the run method
    @Override
    public void run() {
        int qLen = 5;
        Socket sock;

        try {
            // administration server socket
            ServerSocket servsock = new ServerSocket(adminPort, qLen);
            while (true) {
                sock = servsock.accept();
                // generate a new work thread to address a new request.
                new AdminWorker(sock).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

public class AsyncJokeServer {
    // default server mode is joke mode.
    static ServerMode serverMode = ServerMode.JOKEMODE;

    // to check if certain server is already set up.
    // make a new socket connection to the designated server. if it success, it means the designated server is running now.
    private static boolean isServerUp(String serverName, int port) {
        try {
            Socket sock = new Socket(serverName, port);
            sock.close();
            return true;        // if it connect successfully, it will come to return true
        } catch (IOException ex) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        // the server socket is bound to the port 4545 by default.
        int port = 4545;
        // the admin socket bound to the port 5050 by default.
        int adminPort = 5050;
        // the sleep time is 40 seconds by default
        int timeGap = 40 * 1000;
        // the program can be run with arguments
        // the first two arguments are available. Both of them are optional.
        if (args.length > 0) {
            // the first argument represents the port number for normal joke & proverb service
            port = Integer.parseInt(args[0]);
            // the second argument represents the port number for admin service.
            if (args.length > 1) adminPort = Integer.parseInt(args[1]);
            // if there are arguments detected, set the sleep time 70 seconds.
            timeGap = 70 * 1000;
        }

        // to check if a server has already been set up on this machine, if one has been set up using the same port, exit the server
        // it is impossible to run two servers on the same port
        if (isServerUp("localhost", port)) {
            System.out.println("The server has already been set up.");
            System.exit(1);
        }

        if (isServerUp("localhost", adminPort)) {
            System.out.println("The admin port has already been used.");
            System.exit(1);
        }

        // maximum length of the queue of connections
        int qLen = 5;
        Socket sock;

        // process requests from administration client using a different port
        AdminController admincontroller = new AdminController(adminPort);
        admincontroller.start();

        // create a ServerSocket instance
        ServerSocket servSocket = new ServerSocket(port, qLen);

        // for the promotion when the server is set up
        System.out.println("Zhen Qin's Async Joke Server starting up, listening at port " + port + ", admin listening at port " + adminPort + ".");

        while (true) {
            // listen for requests from clients, it is a block method.
            sock = servSocket.accept();
            // generate a new work thread to address a new request.
            new JPWorker(sock, timeGap).start();
        }
    }
}