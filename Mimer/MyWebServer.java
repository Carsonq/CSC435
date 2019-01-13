/**
 * Created by Zhen Qin on 10/15/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command:
 *   javac -cp "D:\Program Files\Java\jdk1.8.0_101\lib\xstream-1.2.1.jar;D:\Program Files\Java\jdk1.8.0_101\lib\xpp3_min-1.1.3.4.O.jar" MyWebServer.java
 *   compilation Note: To compile BCHandler.java, it need two third party libraries, xstream-1.2.1.jar and xpp3_min-1.1.3.4.O.jar
 *   When you compile it, you should point out where the libraries are, or directly put them in java default lib
 *   directory which you have already configured in System environment.
 * Command to run the program:
 *   set classpath=%classpath%D:\workspace\Mimer;D:\Program Files\Java\jdk1.8.0_101\lib\xstream-1.2.1.jar;D:\Program Files\Java\jdk1.8.0_101\lib\xpp3_min-1.1.3.4.O.jar;
 *   java MyWebServer
 * List of files needed for running the program:
 *   MyWebServer.java
 *   MIMETypes
 *   BCHandler.java
 * Notes:
 *   I implemented the first bragging rights, which need extra file called MIMETypes in the same directory to start up.
 *   I verify legitimacy of a request by comparing the abstract path of a request with the abstract path of the program's root directory.
 *   I did not use slash to identify a file whether is a directory or not. Java has a function to do so(File.isDirectory).
 *   If you visit a page which is not root, you may see a link called Parent Directory. You can go back to last directory by clicking it.
 *   When there is a whitespace existing in the url, you can still get the right file.
 *
 * New:
 *   Based on the old version of MyWebServer.java, the new version add another thread to handler back channel connection.
 */

// Input/Output lib
import java.io.*;
// net lib
import java.net.*;
// to parse and assemble path
import java.nio.file.Paths;
// some more complex data structure
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
// to get current time
import java.util.Date;

// third party Xstream lib
import com.thoughtworks.xstream.XStream;

// utility functions
class WebServerUtil {
    static HashMap<String, String> getMIMETypes() {
        HashMap<String, String> MIMETypes = new HashMap<>();

        try {
            BufferedReader file = new BufferedReader(new FileReader("./MIMETypes"));
            String line;
            while ((line = file.readLine()) != null) {
                if (!line.equals("")) {
                    String[] pairs = line.trim().split(" ");
                    MIMETypes.put(pairs[0], pairs[1]);
                }
            }

            return MIMETypes;
        } catch (IOException ioe) {
            // System.out.println(ioe);
            // when can not find the MIMETypes in current directory, set some default values.
            MIMETypes.put("txt", "text/plain");
            MIMETypes.put("html", "text/html");
            MIMETypes.put("htm", "text/html");
        }
        MIMETypes.put("xyz", "application/xyz");

        return MIMETypes;
    }

    // check if a path refer to a directory or not, return boolean value
    static boolean isDir(String filePath) {
        File file = new File(filePath);
        return file.isDirectory();
    }

    // security check
    static boolean securityCheck(String resourcePath) {
        // get current working directory
        String workingPath = "";
        try {
            workingPath = new File(".").getCanonicalPath();
        } catch (IOException ioe) {
            System.out.println("getCanonicalPath exception");
        }

        if (!workingPath.equals("")) {
            String requestPath;
            try {
                requestPath = new File(resourcePath).getCanonicalPath();
            } catch (IOException ioe) {
                System.out.print("getCanonicalPath exception");
                return false;
            }

            // check if what the user requests is in the current working directory
            if (requestPath.startsWith(workingPath)) {
                return true;
            }
        }

        return false;
    }

    // get the list of files in filePath
    static Iterator<String> getFileList(String filePath) {
        ArrayList<String> fileList = new ArrayList<>();
        File dir = new File("." + filePath);
        File[] dirFiles = dir.listFiles();

        for (File file : dirFiles) {
            fileList.add(file.getName());
        }

        return fileList.iterator();
    }

    // generate html file which list all the files in the filePath
    static String createHTMLFileList(String filePath) {
        Iterator fileNames = getFileList(filePath);

        String fileContent = "<pre>\r\n<h1> Index of " + filePath + "</h1><br>";
        // if it is not the root, show a link that can go back to upper directory
        if (!filePath.equals("/")) {
            fileContent += "<a>GO BACK: </a><a href=\"" + new File(filePath).getParent() + "\">Parent Directory</a><br><br><br>";
        }

        while (fileNames.hasNext()) {
            String fileName = fileNames.next().toString();
            String pathLink = new File(filePath, fileName).getPath();
            String dirFlag = "";
            if (isDir("." + pathLink)) {
                // if the file is directory, show the sign up
                dirFlag = File.separator;
            }
            fileContent += "<a href=\"" + pathLink + "\">" + fileName + dirFlag + "</a><br>";
        }

        fileContent += "</pre>";
        return fileContent;
    }

    // when user requests a file, show the content of the file
    static String getFileContent(String filePath) {
        try {
            String fileContent = "";
            BufferedReader file = new BufferedReader(new FileReader("." + filePath));
            String line;
            while ((line = file.readLine()) != null) {
                fileContent += line + "\r\n";
            }
            return fileContent;
        } catch (IOException ioe) {
            // if the file does not exist, the type of the message should be text/plain, set at getFileMIMEType.
            System.out.println(ioe);
            return "Warning: No such file.";
        }
    }

    // determine which content-type the file is.
    static String getFileMIMEType(String fileName) {
        File file = new File("." + fileName);
        // if the file does not exist, set the content-type text/plain for warning info.
        if (!file.exists()) {
            return "text/plain";
        }

        // set content-type based on the file extension
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
        String mimeType = MyWebServer.MIMETYPES.getOrDefault(fileExtension, "").toString();
        if (mimeType.equals("")) {
            MyWebServer.MIMETYPES = getMIMETypes();
            mimeType = MyWebServer.MIMETYPES.getOrDefault(fileExtension, "text/plain").toString();
        }

        return mimeType;
    }

    // assemble response header.
    static String httpResponseHeader(String cLen, String cType) {
        String responseState = "HTTP/1.1 200 OK";
        String responseContentLength = "Content-Length: ";
        String responseContentType = "Content-Type: ";
        String responseDate = "Date: " + new Date();
        String singleEnding = "\r\n";
        String doubleEnding = "\r\n\r\n";

        return responseState + singleEnding + responseContentLength + cLen + singleEnding + responseContentType + cType + singleEnding + responseDate + doubleEnding;
    }

    // parse url, get parameters from url
    static HashMap<String, String> parseCGIRequest(String requestPath) {
        HashMap<String, String> pairs = new HashMap<>();
        // ? has special meanings in regular expression
        String parameters = requestPath.split("\\?")[1];
        for (String pairString : parameters.split("&")) {
            String[] pairList = pairString.split("=");
            pairs.put(pairList[0], pairList.length == 1 ? "" : pairList[1]);
        }

        return pairs;
    }

    // if the request is for simulating cgi
    static String addnums(String requestPath) {
        HashMap<String, String> parameters = parseCGIRequest(requestPath);
        // get the form information
        // declare the numbers as float
        float num1 = Float.parseFloat(parameters.getOrDefault("num1", "0").toString());
        //noinspection unchecked
        float num2 = Float.parseFloat(parameters.getOrDefault("num2", "0").toString());
        String person = parameters.getOrDefault("person", "").toString();

        String resultTemplete = "<html><head><title>Calculate Result</title></head><body><h1>Dear %s, the sum of %s and %s is %s.</h1></body></html>";

        return String.format(resultTemplete, person, num1, num2, num1 + num2);
    }
}

class ServerWorker extends Thread {
    Socket sock;

    // constructor
    ServerWorker(Socket socket) {
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
            // read the first line of the request information, which tells program what resources the user wants.
            // For Example: GET /Zhen/cat.html HTTP/1.1
            String requestLine = streamIn.readLine();

            if (requestLine != null) {
                // parse the first line, to get the url.
                // decode is needed. if not, when there is a whitespace existing in the path, you may not find the file
                String requestPath = URLDecoder.decode(requestLine.split(" ")[1], "UTF-8");
                // security check first
                if (!WebServerUtil.securityCheck(Paths.get(".", requestPath).toString())) {
                    // server console warning message.
                    System.out.println("Warning, illegal URL");
                    // Show some messages to users
                    String WarningMsg = "Your URL contains illegal characters. Your request has been rejected.";
                    // message header, set type as text/plain
                    streamOut.print(WebServerUtil.httpResponseHeader(Integer.toString(WarningMsg.length()), "text/plain") + WarningMsg);
                } else if (requestPath.startsWith("/cgi/addnums.fake-cgi")) {        // when the request is to ask a result
                    // revoke addnums function to get the message of result.
                    String calculateResult = WebServerUtil.addnums(requestPath);
                    // header, set Content-Type text/html
                    streamOut.print(WebServerUtil.httpResponseHeader(Integer.toString(calculateResult.length()), "text/html") + WebServerUtil.addnums(requestPath));
                } else if (WebServerUtil.isDir("." + requestPath)) {
                    // create a html file which lists all the files in the request directory.
                    String fileListHtml = WebServerUtil.createHTMLFileList(requestPath);
                    // header, set Content-Type text/html
                    streamOut.print(WebServerUtil.httpResponseHeader(Integer.toString(fileListHtml.length()), "text/html") + fileListHtml);
                } else {
                    // determine what type the file is
                    String fileMIMEType = WebServerUtil.getFileMIMEType(requestPath);
                    // get the content of the file
                    String fileContent = WebServerUtil.getFileContent(requestPath);
                    // header, Content-Type depends on the extension of the file
                    streamOut.print(WebServerUtil.httpResponseHeader(Integer.toString(fileContent.length()), fileMIMEType) + fileContent);
                }
            }

            streamOut.flush();
            sock.close();
        } catch (IOException ioe) {
            System.out.println("Server read error");
            ioe.printStackTrace();
        }
    }
}

public class MyWebServer {
    // read MIME table which is a extra file
    public static HashMap<String, String>  MIMETYPES = WebServerUtil.getMIMETypes();

    public static void main(String[] args) throws IOException {
        // server socket is bound to the port 2540
        int port = 2540;

        // maximum length of the queue of connections
        int qLen = 5;
        Socket sock;

        // when the server is set up, show up the promotion
        System.out.println("Zhen Qin's MyWebServer starting up, listening at port " + port + ".");

        int xmlPort = 2570;
        // another thread to process xml stream requests
        XMLStreamController xmlStreamController = new XMLStreamController(xmlPort);
        xmlStreamController.start();

        // create a ServerSocket instance
        ServerSocket serverSocket = new ServerSocket(port, qLen);
        while (true) {
            // listen for requests from clients, it is a block method.
            sock = serverSocket.accept();
            // generate a new work thread to address a new request.
            new ServerWorker(sock).start();
        }
    }
}

class XMLStreamController extends Thread {
    int xmlPort;

    // constructor
    XMLStreamController(int adminPort) {
        this.xmlPort = adminPort;
    }

    // override the run method, when start a thread, it will use the run method
    @Override
    public void run() {
        System.out.println("Looking for xml stream, waiting for port " + xmlPort + " connections");
        int qLen = 5;
        Socket sock;

        try {
            // create xml server socket
            ServerSocket servsock = new ServerSocket(xmlPort, qLen);
            while (true) {
                sock = servsock.accept();
                // generate a new work thread to address xml stream request.
                new XMLWorker(sock).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

// the structure must be exactly the same as client side declaration
// otherwise, you can not deserialize the xml file correctly
class XmlDataArray {
    int lineNum = 0;
    String[] lines = new String[10];
}

// to process requests of XML stream
class XMLWorker extends Thread {
    Socket sock;

    // constructor
    XMLWorker(Socket socket) {
        sock = socket;
    }

    // when start method is invoked, the run method is executed.
    public void run() {
        BufferedReader streamIn;
        PrintStream streamOut;

        StringBuilder xml = new StringBuilder();
        String lineInfo;
        XStream xstream = new XStream();
        final String newLine = System.getProperty("line.separator");
        XmlDataArray da;
        System.out.println("Called XML worker.");

        try{
            // get InputStream from socket, read client message through it.
            streamIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // socket output stream that client can read message from it
            streamOut = new PrintStream(sock.getOutputStream());

            while (true) {
                lineInfo = streamIn.readLine();
                // determine if it is the last line of the xml file
                if (lineInfo.indexOf("end_of_xml") > -1) break;
                else xml.append(lineInfo + newLine);
            }
            // print xml data in server console
            System.out.println("The XML marshaled data:");
            System.out.println(xml);

            // deserialize xml data
            da = (XmlDataArray) xstream.fromXML(xml.toString());
            // prompt
            System.out.println("Here is the restored data: ");
            for(int i = 0; i < da.lineNum; i++){
                System.out.println(da.lines[i]);
            }

            // send back the ack signal to client side
            streamOut.println("Acknowledging Back Channel Data Receipt");
            streamOut.flush();
            // close socket
            sock.close();
        }catch (IOException ioe){
            System.out.println("Server read error");
            ioe.printStackTrace();
        }
    }
}