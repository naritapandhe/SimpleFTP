package FTPClientServer;

/**
 * Required imports
 *
 */
import com.mysql.jdbc.StringUtils;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//Client class
public class FTP_Client implements Runnable {

    /**
     * Declaring the required variables
     *
     */
    static String serverAddress;
    String inputCommand;
    static int serverNormalPort;
    static int serverTerminatePort;
    Socket clientNormalPortSocket;
    Socket clientTerminatePortSocket;
    String[] commandSplitArray;
    String currentCommandID=null;
    ObjectOutputStream outputStreamObj = null;
    ObjectInputStream inputStreamObj = null;
    ObjectOutputStream terminateOutputStreamObj = null;
    ObjectInputStream terminateiInputStreamObj = null;
    Commands currentCommand;
    public static ConcurrentMap<String, String> filesLocked = new ConcurrentHashMap<String, String>();

     static String GET_COMMAND_ID = "1";
    static String PUT_COMMAND_ID = "2";
    static String DELETE_COMMAND_ID = "3";


    /**
     * Enumeration of all the allowed commands.
     *
     */
    public enum Commands {

        ls,
        pwd,
        mkdir,
        get,
        put,
        delete,
        cd,
        quit,
        terminate
    };

    //Default Client's constructor
    FTP_Client(String serverAddressParm, int serverNormalPortParm, int serverTerminatePortParm) {

        serverAddress = serverAddressParm;
        serverNormalPort = serverNormalPortParm;
        serverTerminatePort = serverTerminatePortParm;
        this.currentCommand = null;
    }

    /**
     * Function to display the list of available
     * commands
     *
     */
    public void displayCommandsToUser() {

        try {

            System.out.println("\n\nList of available commands: ");
            System.out.println("ls");
            System.out.println("pwd");
            System.out.println("cd <absolute_path>");
            System.out.println("mkdir <absolute_path>");
            System.out.println("get <absolute_path/filename>");
            System.out.println("put <absolute_path/filename>");
            System.out.println("delete <absolute_path/filename>");
            System.out.println("terminate <commandID>");
            System.out.println("quit\n");

            this.printStream("", false);



        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to read the User input.
     * Once the User input is accepted, assign it to the class
     *
     */
    public void readCommandFromUser() {

        try {


            BufferedReader bufferedInput = new BufferedReader(new InputStreamReader(System.in));

            //Assign to the class
            this.inputCommand = bufferedInput.readLine();

            //Parse the command to separate the command and its parameters
            this.parseCommand();



        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to separate the command and its parameters
     *
     * @return Object[]
     */
    public Object[] parseCommand() {

        try {

            //Initialize the variables
            Thread.currentThread().sleep(1000);
            this.commandSplitArray = new String[5];

            // Split the sentence based on spaces
            String[] splittedCommand = new String[5];


            splittedCommand = this.inputCommand.split("\\s+");

            //The first param is the command always
            this.commandSplitArray[0] = splittedCommand[0].toLowerCase();
            this.currentCommand = Commands.valueOf(this.commandSplitArray[0].toString());

            //The second param is the absolute_path of the file or directory always
            if (splittedCommand.length > 1 && splittedCommand[1] != null) {
                this.commandSplitArray[1] = splittedCommand[1];
            }

            /**
             * The third param can be any of the following:
             *  - If its a background process, it can be '&'.
             *    In this case '&' will be stored in commandSplitArray[4]
             *
             *  - If its a normal command, the third param can either
             *    be NULL or in case of GET / PUT: file contents
             */
            if (splittedCommand.length > 2 && splittedCommand[2] != null) {
                //The second param is the absolute_path of the file or directory always
                this.commandSplitArray[2] = splittedCommand[2];
            }

        } catch (Exception e) {
        }

        return this.commandSplitArray;

    }

    /**
     * Function to validate the command input by the User.
     * - If valid command is entered, the command is executed on the Server
     * - If not, an error message is displayed.
     *
     */
    public boolean validateCommandAndSendToServer() {
        boolean commandResult = false;

        try {

            //Thread.currentThread().sleep(2000);
            this.outputStreamObj = new ObjectOutputStream(this.clientNormalPortSocket.getOutputStream());
            switch (this.currentCommand) {

                case ls:
                case mkdir:
                case pwd:
                case delete:
                case cd:
                case get:
                    //Send the data to the Server
                    this.outputStreamObj.writeObject(this.commandSplitArray);
                    this.outputStreamObj.flush();
                    commandResult = true;
                    break;


                case put:
                    this.uploadToServer();
                    break;

                case terminate:
                    String cmd=this.commandSplitArray[1].split("_")[1];
                    if(cmd.equals(GET_COMMAND_ID)){
                        this.terminateOutputStreamObj = new ObjectOutputStream(this.clientTerminatePortSocket.getOutputStream());
                        this.terminateOutputStreamObj.writeObject(this.commandSplitArray);
                        this.outputStreamObj.writeObject(null);
                        this.terminateOutputStreamObj.flush();
                        commandResult = true;
                    }else if(cmd.equals(PUT_COMMAND_ID)){
                        if(this.executeTerminate(this.commandSplitArray[1])){
                            System.out.println("Command terminated successfully!!");
                        }else{
                            System.out.println("There was some problem in terminating the command. Please try again!!");
                        }
                    }
                    break;

                default:
                    //Display the error and shut down
                    System.out.println("Invalid command entered!!!");
                    System.out.println("Disconnecting from the Server.....");
                    break;

            }


        } catch (Exception e) {
            this.printStream("Invalid command entered!!!", true);
            e.printStackTrace();
        }
        return commandResult;

    }

    public boolean uploadToServer() {

        //Lakshat thev: To change it to 1000bytes
        int FIXED_BUFFER_SIZE = 10;
        System.out.println("Executing PUT...");
        boolean isCommandIDReceived = false;

        try {
            String fullFileName=this.commandSplitArray[1];
            File fileObject = new File(this.commandSplitArray[1]);
            String originalFileName = fileObject.getName();
            int iteration = 1;
            boolean isFileSent = false;
            String putCommandID = null;

            if (fileObject.exists()) {

                    FileInputStream fileInputStreamObj = new FileInputStream(this.commandSplitArray[1]);
                    int fsize = (int) fileInputStreamObj.getChannel().size();
                    byte[] fileBytes = new byte[FIXED_BUFFER_SIZE];

                    while (true && !isFileSent && !Thread.currentThread().interrupted() && this.isAllowed(fullFileName, this.currentCommandID)) {

                        if (!isCommandIDReceived) {

                            //Send the file name and command to the server
                            this.commandSplitArray[1] = originalFileName;
                            this.outputStreamObj.writeObject(this.commandSplitArray);
                            this.outputStreamObj.flush();

                            System.out.println("Waiting to receive CommandID from Server!!!");

                            this.inputStreamObj = new ObjectInputStream(this.clientNormalPortSocket.getInputStream());
                            Object inputFileContents = this.inputStreamObj.readObject();

                            //Check if the Client has received the CommandID
                            fileBytes = (byte[]) inputFileContents;
                            putCommandID = new String(fileBytes);
                            this.printStream("Command ID: " + putCommandID, true);
                            this.currentCommandID=putCommandID;

                            System.out.println("befpre pushing!!");
                            filesLocked.put(putCommandID, fullFileName);
                            isCommandIDReceived = true;
                            Thread.currentThread().sleep(2000);
                            this.outputStreamObj.flush();


                        } else {

                            Thread.currentThread().sleep(10000);
                            if (!this.isTerminated(this.currentCommandID)) {
                                this.outputStreamObj = new ObjectOutputStream(this.clientNormalPortSocket.getOutputStream());

                                int size = 0;
                                System.out.println("Sending file contents...");

                                if (fsize > (iteration * FIXED_BUFFER_SIZE)) {
                                    size = FIXED_BUFFER_SIZE;
                                    fileBytes = new byte[size];
                                    fileInputStreamObj.read(fileBytes);

                                    this.outputStreamObj.writeObject(fileBytes);

                                    //Flush the stream
                                    this.outputStreamObj.flush();



                                } else {

                                    size = fsize - ((iteration - 1) * FIXED_BUFFER_SIZE);
                                    isFileSent = true;
                                    fileBytes = new byte[size];
                                    fileInputStreamObj.read(fileBytes);

                                    //Send the commandSplitArray to the Server
                                    this.outputStreamObj.writeObject(fileBytes);
                                    //Flush the stream
                                    this.outputStreamObj.flush();

                                    this.executeTerminate(this.currentCommandID);

                                    System.out.println("File sent successfully!!! Please verify at the Server side");

                                }
                                iteration++;
                            }else{
                                
                                 System.out.println("Transfer terminated!!!");
                                // Thread.currentThread().interrupt();
                                 return false;

                            }

                        }

                }

                 /*else{
                     System.out.println("Please try again after sometime.");
                     Thread.currentThread().interrupt();
                     return false;
                }*/


            } else {
                System.out.println("File not found!!!Please try again");
                return false;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Function to download a file from the Server
     *
     */
    public void downloadFromServer(ObjectInputStream inputStreamObj, String destinationFileName) {
        String result = null;
        try {

            //Initialize variables
            int FIXED_BUFFER_SIZE = 10;
            byte[] fileBytes = null;
            boolean isFileRecieved = false;
            boolean isCommandIDRecieved = false;


            Object inputFileContents = inputStreamObj.readObject();
            if (inputFileContents instanceof String) {

                this.printStream(inputFileContents.toString(), true);
                return;

            }


            //Create a file
            File fileObject = new File(destinationFileName);
            FileOutputStream fileOutputStreamObject = new FileOutputStream(new File(System.getProperty("user.dir") + "/" + fileObject.getName()));


            while (true && !isFileRecieved) {

                if (!isCommandIDRecieved) {

                    Thread.currentThread().sleep(2000);
                    //Check if the Client has received the CommandID
                    fileBytes = (byte[]) inputFileContents;
                    String getCommandID = new String(fileBytes);
                    this.printStream(getCommandID, true);
                    isCommandIDRecieved = true;


                } else {
                    Thread.currentThread().sleep(1000);
                    //Receive the file Contents
                    if (((byte[]) inputFileContents).length == FIXED_BUFFER_SIZE) {
                        fileBytes = new byte[FIXED_BUFFER_SIZE];
                    } else {
                        fileBytes = new byte[((byte[]) inputFileContents).length];
                        isFileRecieved = true;


                    }
                    fileBytes = (byte[]) inputFileContents;

                    System.out.println("Sending file contents...");
                    //Write the contents to the file
                    fileOutputStreamObject.write(fileBytes);

                    //Close the stream
                    fileOutputStreamObject.flush();

                    if (isFileRecieved) {
                        this.printStream("File received successfully!!!", true);
                        return;
                    }

                }
                inputFileContents = inputStreamObj.readObject();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to display the response sent from
     * Server.
     *
     */
    public void processServerResponse() {

        try {
            this.inputStreamObj = new ObjectInputStream(this.clientNormalPortSocket.getInputStream());

            /**
             * Parse and display the response sent from Sever
             * based on the type of the response is expected.
             *
             */
            switch (this.currentCommand) {

                case ls:
                    this.parseLsResponse(this.inputStreamObj);
                    break;

                case get:
                    this.downloadFromServer(this.inputStreamObj, (String) this.commandSplitArray[1]);
                    break;

                default:
                    this.printStream(this.inputStreamObj.readObject().toString(), true);
                    break;

            }


        } catch (Exception e) {
        }
    }

    /**
     * Function to parse the 'ls' response
     *
     * @ObjectInputStream inputStream
     */
    public void parseLsResponse(ObjectInputStream inputStream) {
        try {

            /**
             * Since, 'ls' will send a list of files, looping through each file to
             * display its name.
             *
             */
            ArrayList<String> fileList = (ArrayList<String>) inputStream.readObject();
            for (String individualFile : fileList) {
                System.out.println(individualFile);
            }
        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    /**
     *
     * Function to display the prompt
     *
     * @String stream
     * @boolean newLine
     */
    public void printStream(String stream, boolean newLine) {
        String clientPrompt = "mytftp> ";
        if (newLine) {
            System.out.println(clientPrompt + stream);
        } else {
            System.out.print(clientPrompt + stream);
        }
    }

    /**
     *
     * Function to execute the Terminate command
     * @String commandID: Command to terminate
     * @return boolean
     */
    public boolean executeTerminate(String commandID) {
        boolean terminate = false;

        try {

            if (filesLocked.containsKey(commandID)) {
                filesLocked.remove(commandID);
                System.out.println("Command ID found!!!Removing corresponding thread from the map.");
                terminate = true;
            }
        } catch (Exception e) {
        }
        System.out.println("Is thread terminated: " + terminate);
        return terminate;
    }

    /**
     *
     * Function to check whether a thread is terminated
     * @param commandID
     * @return boolean
     */
    public boolean isTerminated(String commandID) {

        boolean terminated = false;
        try {

            if (!filesLocked.containsKey(commandID)) {
                terminated = true;
            }

        } catch (Exception e) {
        }

        System.out.println("Terminate current status: " + terminated);
        return terminated;

    }

    /**
     *
     * Function to check whether the incoming thread is allowed
     * to execute the get/put/delete on the file
     *
     * @String fileName
     * @String inputCommand
     * @return boolean
     */
    public boolean isAllowed(String fileName, String inputCommand) {
        boolean allowed = false;
        try {
            System.out.println("gfgfggfgfg");
             Set setOfKeys = filesLocked.keySet();
                            Iterator iterator = setOfKeys.iterator();
                            while (iterator.hasNext()) {
                            /**
                             * next() method returns the next key from Iterator instance.
                             * return type of next() method is Object so we need to do DownCasting to String
                             */
                            String key = (String) iterator.next();

                            /**
                             * once we know the 'key', we can get the value from the HashMap
                             * by calling get() method
                             */
                             String value = filesLocked.get(key);

                            System.out.println("Key: "+ key+", Value: "+ value);

                             }

            System.out.println("File name: "+fileName);
            if (filesLocked.containsValue(fileName)) {
                Object key = this.getKeyFromValue(fileName);
                String[] existingCommandID = key.toString().split("_");
                System.out.println("Existting coomad: "+existingCommandID[0]+":"+existingCommandID[1]);

                String[] inputCommandID=null;
                if(inputCommand!=null){
                    inputCommandID = inputCommand.split("_");
                }
                System.out.println("Existting coomad: "+inputCommandID[0]+":"+inputCommandID[1]);

                if ((existingCommandID[0].equals(inputCommandID[0])) && (existingCommandID[1].equals("2") && inputCommandID[1].equals("2"))) {
                    allowed = true;
                }else{
                    allowed = false;
                    System.out.println("Not allowed!!!");
                }
               
            }else{
                allowed = true;
                
            }
        } catch (Exception e) {
        }
        System.out.println("Is allowed: " + allowed);
        return allowed;
    }

    public Object getKeyFromValue(String value) {
        for (Object o : filesLocked.keySet()) {
            if (filesLocked.get(o).equals(value)) {
                System.out.println("Before returning from getvale: "+o);
                return o;
            }
        }

               return null;
    }

    public void run() {
        try {


            //Switch if its a valid command
            Thread.currentThread().sleep(1000);
            if (this.validateCommandAndSendToServer()) {

                this.processServerResponse();
                Thread.currentThread().sleep(2000);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
