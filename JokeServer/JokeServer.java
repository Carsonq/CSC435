/**
 * Created by Zhen Qin on 9/12/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac JokeServer.java
 * Command to run the program: java JokeServer [secondary] [primary address]
     the command of running a primary server: java JokeServer
     the command of running a secondary server: java JokeServer secondary [primary address]
         to run a secondary server, we need to check if a primary is already set up.
         the primary server may be a remote server, so we need another argument to designate it,
         the second argument is optional, its default value is localhost
 * List of files needed for running the program:
     JokeServer.java
     JokeClient.java
     JokeClientAdmin.java
 * Notes: Server-side of the JokeServer program. A primary server listens at port 4545 and 5050.
   A secondary server listens at port 4546 and 5051. To maintain states, I keep all states on client side.
   The shuffle function can be implemented in a more efficient way.
 */

import java.io.*;        // I/O lib
import java.net.*;       // Networking lib
import java.util.*;      // util lib

// declare two kinds of server mode
enum ServerMode {JOKEMODE, PROVERBMODE}

// to process requests from JokeClient
class JPWorker extends Thread {
    Socket sock;
    String msgPrefix;

    // constructor
    JPWorker(Socket socket, String msgPrefix) {
        sock = socket;
        this.msgPrefix = msgPrefix;
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
                // every request keeps a lot of state information, depends on that, we can ensure what should be pushed to client
                String clientCookie = streamIn.readLine();
                // null check to avoid NullPointerException
                if (clientCookie != null) {
                    // parse client cookie
                    // counter for jokes
                    String jokeCounter = clientCookie.substring(0, 1);
                    // counter for proverbs
                    String proverbCounter = clientCookie.substring(1, 2);
                    // the string contains current order of joke set and proverb set
                    String jpOrder = clientCookie.substring(2,10);
                    // user name, come from user input, it is not necessary to be unique in this design
                    String userName = clientCookie.substring(10);

                    // depends on cookie, assemble return messages
                    outputClient(userName, streamOut, msgPrefix, jokeCounter, proverbCounter, jpOrder);
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

    static String assebmbleMessage(String userName, String msgPrefix, int jokeCounter, int proverbCounter, String jpOrder) {
        // declare the order of joke set, due to the size of joke set is smaller than 10, I do not use any delimiter to split each other
        String jOrder;
        // declare the order of proverb set, the same as jOrder
        String pOrder;
        // the real index of jokeTemplates or proverbTemplates
        int jpIndex;
        String[] jokeTemplates  =  {"Anton, do you think I’m a bad mother? My name is Paul.",
                                    "Can a kangaroo jump higher than a house? Of course, a house doesn’t jump at all.",
                                    "My wife’s cooking is so bad we usually pray after our food.",
                                    "I'd like to buy a new boomerang please. Also, can you tell me how to throw the old one away?"};
        String[] proverbTemplates  = {"There's no such thing as a free lunch.",
                                      "God helps those who help themselves.",
                                      "Too many cooks spoil the broth.",
                                      "There's no time like the present."};
        String[] jokeSetIndex = {"JA", "JB", "JC", "JD"};
        String[] proverbSetIndex = {"PA", "PB", "PC", "PD"};

        if (JokeServer.serverMode.equals(ServerMode.JOKEMODE)) {
            // get the order of joke set
            jOrder = jokeCounter == 0 ? getNewOrder() : jpOrder.substring(0,4);
            // get the order of joke set
            pOrder = jpOrder.substring(4);
            // get the index of joke set
            jpIndex = Integer.parseInt(jOrder.substring(jokeCounter, jokeCounter + 1));
            // assemble the return message which will be pushed to client
            String returnMsg = msgPrefix + jokeSetIndex[jokeCounter] + " " + userName + ": " + jokeTemplates[jpIndex];
            jokeCounter = (jokeCounter + 1) % jokeTemplates.length;
            String counterInfo = Integer.toString(jokeCounter) + Integer.toString(proverbCounter);
            // the message is not only contain the information that will be showed up on the client's screen
            // but also contain cookie information that will be parse by client side
            return returnMsg + counterInfo + jOrder + pOrder;
        } else {
            // get the order of joke set
            jOrder = jpOrder.substring(0,4);
            // get the order of joke set
            pOrder = proverbCounter == 0 ? getNewOrder() : jpOrder.substring(4);
            // get the index of proverb set
            jpIndex = Integer.parseInt(pOrder.substring(proverbCounter, proverbCounter + 1));
            // assemble the return message which will be pushed to client
            String returnMsg = msgPrefix + proverbSetIndex[proverbCounter] + " " + userName + ": " + proverbTemplates[jpIndex];
            proverbCounter = (proverbCounter + 1) % proverbTemplates.length;
            String counterInfo = Integer.toString(jokeCounter) + Integer.toString(proverbCounter);
            return returnMsg + counterInfo + jOrder + pOrder;
        }
    }

    // write result to socket that client can read.
    static void outputClient(String userName, PrintStream streamOut, String msgPrefix, String jokeCounter, String proverbCounter, String jpOrder) {
        streamOut.println(assebmbleMessage(userName, msgPrefix, Integer.parseInt(jokeCounter), Integer.parseInt(proverbCounter), jpOrder));
        streamOut.flush();
    }
}

// to process requests of JokeClientAdmin
class AdminWorker extends Thread {
    Socket sock;
    String msgPrefix;

    // constructor
    AdminWorker(Socket socket, String msgPrefix) {
        sock = socket;
        this.msgPrefix = msgPrefix;
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
                        streamOut.println(msgPrefix + "Server current mode: " + JokeServer.serverMode);
                    } else if (inputCommand.equals("switch mode")) {
                        // switch server mode
                        JokeServer.serverMode = JokeServer.serverMode == ServerMode.JOKEMODE ? ServerMode.PROVERBMODE : ServerMode.JOKEMODE;
                        streamOut.println(msgPrefix + "Server has been changed to mode: " + JokeServer.serverMode);
                        System.out.println("The server mode is changed. The current server mode is " + JokeServer.serverMode + ".");
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
    String msgPrefix;

    // constructor
    AdminController(int adminPort, String msgPrefix) {
        this.adminPort = adminPort;
        this.msgPrefix = msgPrefix;
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
                new AdminWorker(sock, msgPrefix).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

public class JokeServer {
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
        // the primary server socket is bound to the port 4545
        int port = 4545;
        // the primary administration is server socket bound to the port 5050
        int adminPort = 5050;
        // the program can be run with arguments
        // if the the first arguments is secondary, it means we want to run the server as the secondary server.
        if (args.length > 0 && args[0].equals("secondary")) {
            // to run the secondary server, we need to check if the primary server is running.
            // to do the check, it is necessary to know the ip or hostname of the primary server, the server may be a remote server.
            // the primary server ip or hostname can be get from the second argument, the default value is localhost
            String primaryServerName = "localhost";
            if (args.length > 1) primaryServerName = args[1];
            if (isServerUp(primaryServerName, port)) {
                // if the primary server is up, we can run a secondary server as we want
                // the secondary server is bound to the port 4546
                port = 4546;
                // the secondary administration server is socket bound to the port 5050
                adminPort = 5051;
            } else {
                // if the primary server has not been set up, we can not run a secondary server before running a primary server
                // the server will be set up as a primary server, even though we have secondary as the argument
                System.out.println("Primary server have not been set up. This time is to run primary server.");
            }
        }

        // to check if a server has already been set up on this machine, if one has been set up, exit the server
        // it is impossible to run two servers on the same port
        if (isServerUp("localhost", port)) {
            System.out.println("The server has already been set up.");
            System.exit(1);
        }

        // maximum length of the queue of connections
        int qLen = 5;
        Socket sock;

        // for mark the messages from secondary server
        String msgPrefix = port == 4545 ? "" : "<S2>";

        // process requests from administration client asynchronously
        AdminController admincontroller = new AdminController(adminPort, msgPrefix);
        admincontroller.start();

        // create a ServerSocket instance
        ServerSocket servSocket = new ServerSocket(port, qLen);

        // for the promotion when the server is set up
        String secondaryMsgFlag = port == 4545 ? "Primary" : "Secondary";
        System.out.println("Zhen Qin's " + secondaryMsgFlag + " Joke Server starting up, listening at port " + port + ".");
        while (true) {
            // listen for requests from clients, it is a block method.
            sock = servSocket.accept();
            // generate a new work thread to address a new request.
            new JPWorker(sock, msgPrefix).start();
        }
    }
}