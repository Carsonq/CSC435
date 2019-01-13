/**
 * Created by Zhen Qin on 10/15/2016.
 * Java version "1.8.0_101" (build 1.8.0_101-b13)
 * Compilation command:
 *   javac -cp "D:\Program Files\Java\jdk1.8.0_101\lib\xstream-1.2.1.jar;D:\Program Files\Java\jdk1.8.0_101\lib\xpp3_min-1.1.3.4.O.jar" BCHandler.java
 *   compilation Note: To compile BCHandler.java, it need two third party libraries, xstream-1.2.1.jar and xpp3_min-1.1.3.4.O.jar
 *   When you compile it, you should point out where the libraries are, or directly put them in java default lib
 *   directory which you have already configured in System environment.
 * Command to run the program:
 *   java -Dfirstarg=[XMLFILE] BCHandler
 *   example: java -Dfirstarg=C:\Users\Z\AppData\Local\Temp\mimer-data.xyz BCHandler
 *   Note: shim.bat will revoke the program automatically. Do not need to run it manually.
 * List of files needed for running the program:
 *   MyWebServer.java
 * Notes:
 *   This program is handler. It works with shim.bat.
 *   When you compile program, you can not only use "javac *.java", because both client side code and server side code all contain
 *   a same class called XmlDataArrary.
 *   The program is developed using Windows system.
 */

// Input/Output lib
import java.io.*;
// util lib
import java.util.*;
// network lib
import java.net.*;
// third party Xstream lib
import com.thoughtworks.xstream.XStream;

// the structure must be exactly the same as server side declaration
// otherwise, it will cause deserialization problem
class XmlDataArray {
    int lineNum = 0;
    String[] lines = new String[10];
}

public class BCHandler {
    // designate a temp file which is used to store xml info, hard code
    private static String XMLFileName = "C:\\temp\\mimer.output";
    private static PrintWriter      toXmlOutputFile;
    private static File             xmlFile;
    private static BufferedReader   fromMimeDataFile;

    public static void main(String args[]) {
        // default connect to localhost, otherwise connect to the server which designate in args[0]
        String serverName = "localhost";
        if (args.length > 1) {
            serverName = args[0];
        }

        System.out.println("Executing the java application BCHandler.");
        System.out.println("Zhen Qin's back channel Client.\n");
        System.out.println("Connect to server: " + serverName + ", Port: 2540 / 2570");

        // Properties is used to get java configuration
        Properties p = new Properties(System.getProperties());
        // to get the value of firstarg which is set in java commands (java -D)
        // the value is the path of mimer-data.xyz which is stored by the browser temporarily
        String xmlFilePath = p.getProperty("firstarg");
        System.out.println("First var is: " + xmlFilePath);
        System.out.flush();

        try {
            // create a new xml structure
            XmlDataArray da = new XmlDataArray();
            // read the file
            fromMimeDataFile = new BufferedReader(new FileReader(xmlFilePath));
            // Only read 10 lines of data from the file
            while (((da.lines[da.lineNum++] = fromMimeDataFile.readLine()) != null) && da.lineNum < da.lines.length) {
                System.out.println("Data is: " + da.lines[da.lineNum - 1]);
            }
            da.lineNum--;
            System.out.println("line number is: " + da.lineNum);

            // create one new file that will store information
            xmlFile = new File(XMLFileName);
            // check if the file is available or not
            // if a file with the same name already exists, delete it.
            if (xmlFile.exists() == true && xmlFile.delete() == false){
                throw (IOException) new IOException("XML file delete failed.");
            }
            // check if the parent directory exists or not, if it does not exist, create it
            new File(xmlFile.getParent()).mkdirs();
            // make sure it creates a empty file
            if (xmlFile.createNewFile() == false){
                throw (IOException) new IOException("XML file creation failed.");
            }
            else{
                // write information into the temp xml file
                toXmlOutputFile = new PrintWriter(new BufferedWriter(new FileWriter(XMLFileName)));
                toXmlOutputFile.println("First arg to Handler is: " + xmlFilePath + "\n");

                // create a new XStream obj
                XStream xstream = new XStream();
                // convert data in xml form
                String xml = xstream.toXML(da);
                // write to file
                toXmlOutputFile.println(xml);
                // close file
                toXmlOutputFile.close();

                // send xml data to the server side
                sendToBC(xml, serverName);

                // print out the content of xml file in client console
                System.out.println("\n\nHere is the XML version:");
                System.out.print(xml);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    static void sendToBC (String sendData, String serverName){
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String textFromServer;
        try{
            // Open connection Back Channel on server, port 2570
            sock = new Socket(serverName, 2570);
            toServer = new PrintStream(sock.getOutputStream());
            // wait for the input from server side
            fromServer = new  BufferedReader(new InputStreamReader(sock.getInputStream()));

            // send xml data
            toServer.println(sendData);
            // xml file end sign
            toServer.println("end_of_xml");
            toServer.flush();
            // get the feedback from the server
            System.out.println("Blocking on acknowledgment from Server...");
            textFromServer = fromServer.readLine();
            // print out the feedback info
            if (textFromServer != null){System.out.println(textFromServer);}
            // close socket
            sock.close();
        } catch (IOException x) {
            System.out.println ("Socket error.");
            x.printStackTrace ();
        }
    }
}