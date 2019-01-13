/**
 * Created by Zhen Qin on 10/27/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command: javac HostServer.java
 * Command to run the program: java HostServer
 * List of files needed for running the program: HostServer.java
 * Notes:
 *   I did not only add comment to the java program. I also modified it. In the given version, we get next available
 *   port by calculating manually. I use the provided code and make the system to allocate a port automatically.
 *   I found a small bug in function sendHTMLsubmit, there is a symbol ">" missed. Not a big problem.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * When it get request to migrate from one port to another, it will connect to 1565 as a client side to get a new
 * connection with another valid port first and then close the previous socket. When get another valid request, change the state which is
 * related to the specific socket.
 */
class AgentWorker extends Thread {
    Socket sock;
    agentHolder parentAgentHolder;
    int localPort;

    // constructor
    AgentWorker(Socket s, int prt, agentHolder ah) {
        sock = s;
        localPort = prt;
        parentAgentHolder = ah;
    }

    // when start method is invoked, the run method is executed.
    public void run() {
        // declare variable that needed
        PrintStream out = null;
        BufferedReader in = null;
        String NewHost = "localhost";
        int NewHostMainPort = 1565;
        String buf = "";
        int newPort;
        Socket clientSock;
        BufferedReader fromHostServer;
        PrintStream toHostServer;

        try {
            // instance of socket output stream
            out = new PrintStream(sock.getOutputStream());
            // instance of socket input stream
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            // read one line from socket input
            String inLine = in.readLine();
            // assemble html file that will be write to socket
            StringBuilder htmlString = new StringBuilder();

            // console prompt
            System.out.println();
            System.out.println("Request line: " + inLine);

            // if get migrate command from client

            if (inLine.indexOf("migrate") > -1) {
                // client socket, connect to localhost:1565
                clientSock = new Socket(NewHost, NewHostMainPort);
                // instance of socket input stream which can get info from the server side
                fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
                // instance of socket output stream which post info to server side
                toHostServer = new PrintStream(clientSock.getOutputStream());
                toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
                toHostServer.flush();

                // like while true
                for (; ; ) {
                    buf = fromHostServer.readLine();

                    // get port info from the html file
                    if (buf.indexOf("[Port=") > -1) {
                        break;
                    }
                }

                // extract port info
                String tempbuf = buf.substring(buf.indexOf("[Port=") + 6, buf.indexOf("]", buf.indexOf("[Port=")));
                // convert to int
                newPort = Integer.parseInt(tempbuf);
                // console prompt
                System.out.println("newPort is: " + newPort);

                // assemble html file with state info
                htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));
                htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
                htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
                // show in client browser
                htmlString.append(AgentListener.sendHTMLsubmit());

                System.out.println("Killing parent listening loop.");
                // get the last socket and close it. the purpose is to kill the previous socket.
                ServerSocket ss = parentAgentHolder.sock;
                // kill the last socket
                ss.close();
            } else if (inLine.indexOf("person") > -1) {
                // if the request URL contains "person"
                // change the value of state
                parentAgentHolder.agentState++;
                htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
                htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
                htmlString.append(AgentListener.sendHTMLsubmit());
            } else {
                // if the URL does not contain both person or migrate, it is an unavailable request.
                // Nothing change, just show up reminder
                htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
                htmlString.append("You have not entered a valid request!\n");
                htmlString.append(AgentListener.sendHTMLsubmit());
            }
            // write to socket
            AgentListener.sendHTMLtoStream(htmlString.toString(), out);
            // close the socket instance, but the socket port doesn't change
            sock.close();
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

}

/**
 * hold state for relative socket.
 * key-value relationship
 */
class agentHolder {
    ServerSocket sock;
    int agentState;

    // constructor
    agentHolder(ServerSocket s) {
        sock = s;
    }
}

/**
 * generate a new thread to give response when there is a request to the port 1565.
 * also create a new thread listen to a port that is allocated automatically by the system.
 */
class AgentListener extends Thread {
    Socket sock;
    int localPort;

    // constructor
    AgentListener(Socket As, int prt) {
        sock = As;
        localPort = prt;
    }

    // initial the state of an agent
    int agentState = 0;

    // when start method is invoked, the run method is executed.
    public void run() {
        BufferedReader in = null;
        PrintStream out = null;
        String NewHost = "localhost";
        System.out.println("In AgentListener Thread");
        try {
            String buf;
            // instance of socket output stream
            out = new PrintStream(sock.getOutputStream());
            // instance of socket input stream
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            // read the first line of msg that passed to hostserver
            buf = in.readLine();

            if (buf != null && buf.indexOf("[State=") > -1) {
                // when migrate, get the current state
                String tempbuf = buf.substring(buf.indexOf("[State=") + 7, buf.indexOf("]", buf.indexOf("[State=")));
                // convert state from string to int
                agentState = Integer.parseInt(tempbuf);
                // console prompt
                System.out.println("agentState is: " + agentState);
            }

            System.out.println(buf);
            // assemble html file of homepage
            StringBuilder htmlResponse = new StringBuilder();
            // set content
            htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
            htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
            htmlResponse.append("[Port=" + localPort + "]<br/>\n");
            htmlResponse.append(sendHTMLsubmit());

            // write html to socket.
            sendHTMLtoStream(htmlResponse.toString(), out);

            // ServerSocket instance for accept requests from detail pages which contain states.
            // localPort is a currently available port
            ServerSocket servsock = new ServerSocket(localPort, 2);
            // an instance contains state and sock info
            agentHolder agenthold = new agentHolder(servsock);
            // initial agent state
            agenthold.agentState = agentState;

            while (true) {
                sock = servsock.accept();
                System.out.println("Got a connection to agent at port " + localPort);
                // generate a new work thread to address a new request.
                new AgentWorker(sock, localPort, agenthold).start();
            }

        } catch (IOException ioe) {
            // When migrate, you'll see the reminder in server console.
            System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
            System.out.println(ioe);
        }
    }

    // assemble the Content of the html file
    static String sendHTMLheader(int localPort, String NewHost, String inLine) {

        StringBuilder htmlString = new StringBuilder();

        htmlString.append("<html><head> </head><body>\n");
        htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
        htmlString.append("<h3>You sent: " + inLine + "</h3>");
        htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost + ":" + localPort + "\">\n");
        htmlString.append("Enter text or <i>migrate</i>:");
        htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");

        return htmlString.toString();
    }

    // submit button
    static String sendHTMLsubmit() {
        return "<input type=\"submit\" value=\"Submit\">" + "</p>\n</form></body></html>\n";
    }

    static void sendHTMLtoStream(String html, PrintStream out) {
        // http header information
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Length: " + html.length());
        out.println("Content-Type: text/html");
        out.println("");
        // write data to socket
        out.println(html);
    }

}

/**
 * the entrance of HostServer, as main thread, it listens to a fixed port 1565.
 * the main purpose to connect to 1565 is to get the next working port.
 */
public class HostServer {
    public static void main(String[] a) throws IOException {
        // maximum length of the queue of connections at the same time
        int q_len = 6;
        // fixed port
        int port = 1565;

        Socket sock;
        int availablePort;

        ServerSocket servsock = new ServerSocket(port, q_len);
        // console prompt
        System.out.println("Zhen Qin's DIA Master receiver started at port " + port);
        System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:" + port + "\"\n");
        while (true) {
            try {
                // get an available port
                availablePort = getNAPort();
            } catch (IOException ioe) {
                System.out.println(ioe);
                continue;
            }
            // listen for requests
            sock = servsock.accept();
            System.out.println("Starting AgentListener at port " + availablePort);
            // generate a new work thread to address a new request.
            new AgentListener(sock, availablePort).start();
        }
    }

    // let system to allocate an available port
    static int getNAPort() throws IOException {
        try {
            // automatically allocate an available port by using zero
            ServerSocket servSocket = new ServerSocket(0);
            int availablePort = servSocket.getLocalPort();
            // need to close the socket here, otherwise, if we use the port later, the system will have exception that
            // shows the port has been used.
            servSocket.close();
            return availablePort;
        } catch (IOException ioe) {
            throw new IOException(ioe);
        }
    }
}