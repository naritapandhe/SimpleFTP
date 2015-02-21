package FTPClientServer;

/**
 * Required imports
 *
 */
import com.mysql.jdbc.StringUtils;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

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
    Object[] commandSplitArray;
    ObjectOutputStream outputStreamObj = null;
    ObjectInputStream inputStreamObj = null;
    ObjectOutputStream terminateOutputStreamObj = null;
    ObjectInputStream terminateiInputStreamObj = null;
    Commands currentCommand;

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
            this.commandSplitArray = new Object[5];

            // Split the sentence based on spaces
            Object[] splittedCommand = new Object[5];


            splittedCommand = this.inputCommand.split("\\s+");

            //The first param is the command always
            this.commandSplitArray[0] = splittedCommand[0].toString().toLowerCase();
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
                this.commandSplitArray[4] = splittedCommand[2];
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
                    if (this.uploadToServer(this.commandSplitArray)) {
                        this.outputStreamObj.flush();
                        commandResult = true;
                    }

                    break;

                case terminate:
                    this.terminateOutputStreamObj = new ObjectOutputStream(this.clientTerminatePortSocket.getOutputStream());
                    this.terminateOutputStreamObj.writeObject(this.commandSplitArray);
                    this.outputStreamObj.writeObject(null);
                    this.terminateOutputStreamObj.flush();
                    commandResult = true;
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

    /**
     * Function to upload a file to the server
     *
     * @Object[] commandArray
     */
    public boolean uploadToServer(Object[] inputArray) {
        boolean result = false;
        try {

            File fileObject = new File(inputArray[1].toString());

            if (fileObject.exists()) {
                FileInputStream fileInputStreamObj = new FileInputStream(inputArray[1].toString());
                byte[] fileBytes = new byte[1024 * 1024];

                File originalFile = new File(inputArray[1].toString());

                //Store only the filename in commandSplitArray
                this.commandSplitArray[1] = originalFile.getName();

                //Store the contents of the file in commandSplitArray
                fileInputStreamObj.read(fileBytes);
                this.commandSplitArray[2] = fileBytes;

                //Send the commandSplitArray to the Server
                this.outputStreamObj.writeObject(this.commandSplitArray);

                //Flush the stream
                this.outputStreamObj.flush();

                System.out.println("File contents sent!!!");
                result = true;
            } else {
                System.out.println("File not found!!!Please try again");
                result = false;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;


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
