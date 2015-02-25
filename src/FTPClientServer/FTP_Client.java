package FTPClientServer;

/**
 * Required imports
 *
 */
import com.mysql.jdbc.StringUtils;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//Client class
public class FTP_Client implements Runnable {

    /**
     * Declaring the required variables
     *
     */
    static String serverAddress;
    static int serverNormalPort;
    static int serverTerminatePort;
    String inputCommand;
    String currentCommandID = null;
    int totalFileSize = 0;
    int currentFileSize = 0;
    Socket clientNormalPortSocket;
    Socket clientTerminatePortSocket;
    Object[] commandSplitArray;
    ObjectOutputStream outputStreamObj = null;
    ObjectInputStream inputStreamObj = null;
    ObjectOutputStream terminateOutputStreamObj = null;
    ObjectInputStream terminateiInputStreamObj = null;
    Commands currentCommand;

    public static volatile HashMap<String, String> filesLocked = new HashMap<String, String>();

    static String GET_COMMAND_ID = "1";
    static String PUT_COMMAND_ID = "2";
    static String DELETE_COMMAND_ID = "3";
    String currentWorkingDir = null;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

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
            this.commandSplitArray = new Object[8];

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
             *  - If its a background process, the third param will be: '&'.
             *    In this case '&' will be stored in commandSplitArray[4]
             *
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
                    if (this.currentWorkingDir != null) {
                        this.commandSplitArray[1]=this.currentWorkingDir;
                    }
                    //Send the data to the Server
                    this.outputStreamObj.writeObject(this.commandSplitArray);
                    this.outputStreamObj.flush();
                    commandResult = true;
                    break;

                case cd:
                    int index=1;
                    if(this.commandSplitArray[1].toString().equals("..")){
                        index=6;
                    }
                    if (this.currentWorkingDir != null) {
                        this.commandSplitArray[index]=this.currentWorkingDir;
                    }
                    //Send the data to the Server
                    this.outputStreamObj.writeObject(this.commandSplitArray);
                    this.outputStreamObj.flush();
                    commandResult = true;
                    break;

                case mkdir:
                case delete:
                case get:
                    //Send the data to the Server
                    this.outputStreamObj.writeObject(this.commandSplitArray);
                    this.outputStreamObj.flush();
                    commandResult = true;
                    break;

                case pwd:
                    if (this.currentWorkingDir != null) {
                        this.printStream(this.currentWorkingDir, true);
                        
                    } else {

                        this.outputStreamObj.writeObject(this.commandSplitArray);
                        this.outputStreamObj.flush();
                    }
                    commandResult = true;
                    break;

                case put:
                    this.uploadToServer();
                    commandResult = true;
                    break;

                case terminate:
                    String cmd = this.commandSplitArray[1].toString().split("_")[1];
                    if (cmd.equals(GET_COMMAND_ID)) {
                        this.terminateOutputStreamObj = new ObjectOutputStream(this.clientTerminatePortSocket.getOutputStream());
                        this.terminateOutputStreamObj.writeObject(this.commandSplitArray);
                        this.outputStreamObj.writeObject(null);
                        this.terminateOutputStreamObj.flush();
                        commandResult = true;
                    } else if (cmd.equals(PUT_COMMAND_ID)) {
                        this.executeTerminate(this.currentCommandID);
                        this.terminateOutputStreamObj = new ObjectOutputStream(this.clientTerminatePortSocket.getOutputStream());
                        this.terminateOutputStreamObj.writeObject(this.commandSplitArray);
                        this.outputStreamObj.writeObject(null);
                        this.terminateOutputStreamObj.flush();
                        commandResult = true;

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

    /**
     *
     * Function to PUT file to the server
     * @return
     */
    public boolean uploadToServer() {

        //Lakshat thev: To change it to 1000bytes
        int FIXED_BUFFER_SIZE = 10;
        System.out.println("Executing PUT...");
        boolean isCommandIDReceived = false;

        try {

            String fullFileName = this.commandSplitArray[1].toString();
            File fileObject = new File(fullFileName);
            String originalFileName = fileObject.getName();
            int iteration = 1;
            boolean isFileSent = false;

            //Check if the file exists
            if (fileObject.exists()) {

                //Read the file
                FileInputStream fileInputStreamObj = new FileInputStream(fullFileName);
                int fsize = (int) fileInputStreamObj.getChannel().size();

                //All the bytes of files
                byte[] mainFileBytes = new byte[fsize];

                Random rn = new Random();
                int rnNumber = rn.nextInt(10) + 1;

                this.totalFileSize = fsize;
                byte[] fileBytes = new byte[FIXED_BUFFER_SIZE];
                this.currentCommandID = rnNumber + "_" + PUT_COMMAND_ID;

                if (this.isAllowed(fullFileName, this.currentCommandID)) {

                    //Lock the Hash Map
                    writeLock.lock();
                    try {
                        filesLocked.put(this.currentCommandID, fullFileName);
                    } finally {
                        writeLock.unlock();
                    }

                    //Until the file is sent
                    while (true && !isFileSent && !Thread.currentThread().isInterrupted()) {

                        Thread.currentThread().sleep(2000);

                        //Check if Command ID is present
                        if (!isCommandIDReceived) {

                            //Send the file name and command to the server
                            this.commandSplitArray[5] = this.currentCommandID;
                            Thread.currentThread().sleep(2000);
                            isCommandIDReceived = true;
                            this.printStream("Command ID: " + this.currentCommandID, true);


                        } else {

                            System.out.println("Sending file contents...");

                            if (!this.isTerminated(this.currentCommandID) && this.currentFileSize <= this.totalFileSize) {
                                int size = 0;
                                if (fsize > (iteration * FIXED_BUFFER_SIZE)) {
                                    size = FIXED_BUFFER_SIZE;

                                    fileBytes = new byte[size];
                                    fileInputStreamObj.read(fileBytes);

                                    int start = 0;

                                    if (iteration == 1) {
                                        start = 0;

                                    } else {
                                        start = ((iteration - 1) * FIXED_BUFFER_SIZE);

                                    }
                                    System.arraycopy(fileBytes, 0, mainFileBytes, start, size);
                                    this.currentFileSize += size;


                                } else {

                                    size = fsize - ((iteration - 1) * FIXED_BUFFER_SIZE);
                                    this.currentFileSize += size;
                                    fileBytes = new byte[size];
                                    fileInputStreamObj.read(fileBytes);

                                    int startx = (fsize - size);
                                    System.arraycopy(fileBytes, 0, mainFileBytes, startx, size);
                                    isFileSent = true;


                                }

                                System.out.println("No. of bytes sent: " + this.currentFileSize);
                                iteration++;
                                Thread.currentThread().sleep(10000);


                                if (isFileSent) {
                                    this.commandSplitArray[1] = originalFileName;
                                    this.commandSplitArray[2] = mainFileBytes;
                                    this.commandSplitArray[3] = Integer.toString(fsize);
                                    this.commandSplitArray[5] = this.currentCommandID;

                                    //Write the contents to the file
                                    this.totalFileSize = fsize;
                                    this.outputStreamObj.writeObject(this.commandSplitArray);
                                    this.outputStreamObj.flush();
                                    return true;

                                }
                            } else {
                                //File transfer terminated
                                isFileSent = true;
                                Thread.currentThread().interrupt();
                                System.out.println("Transfer terminated!!");
                                return true;
                            }

                        }


                    }//<While End>

                }//<isAllowed End>

            } else {
                System.out.println("File not found!!!Please try again");
                return false;
            }


        } catch (Exception e) {
            //e.printStackTrace();
        }
        return true;
    }

    /**
     * Function to GET the file from Server
     * 
     * @ObjectInputStream inputStreamObj
     * @String destinationFileName
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

                    System.out.println("Receiving file contents...");

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
                String s = new String((byte[]) inputFileContents);
                if (s.equals("TERMINATED")) {

                    File deleteFile = new File(System.getProperty("user.dir") + "/" + fileObject.getName());

                    inputStreamObj.close();
                    fileOutputStreamObject.flush();
                    fileOutputStreamObject.close();
                    fileOutputStreamObject = null;
                    System.gc();
                    Thread.sleep(2000);

                    this.cleanUp(deleteFile);
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * Function to clean up the file on Client
     * side if the 'GET' command is terminated
     * 
     * @File deleteFile
     */
    public void cleanUp(File deleteFile) {


        if (!deleteFile.exists()) {
            System.out.println("File does not exists!! Please try again.");

        } else if (deleteFile.isFile()) {

            //If file exists, then delete the file
            System.gc();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            deleteFile.delete();
            //System.out.println("File removed since the command was terminated.");


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

                case cd:
                    this.currentWorkingDir = this.inputStreamObj.readObject().toString();
                    System.out.println("Directory changed to: " + this.currentWorkingDir);
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

        writeLock.lock();
        try {

            if (filesLocked.containsKey(commandID)) {
                filesLocked.remove(commandID);
                System.out.println("Command ID found!!!Removing corresponding thread from the map.");
                terminate = true;
            }
        } catch (Exception e) {
        } finally {
            writeLock.unlock();

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
        readLock.lock();
        try {

            if (!filesLocked.containsKey(commandID)) {
                terminated = true;
            }

        } catch (Exception e) {
        } finally {
            readLock.unlock();
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
        readLock.lock();
        try {
            System.out.println("File name: " + fileName);
            if (filesLocked.containsValue(fileName)) {
                Object key = this.getKeyFromValue(fileName);
                String[] existingCommandID = key.toString().split("_");
                System.out.println("Existing: "+existingCommandID[0]);

                String[] inputCommandID = null;
                if (inputCommand != null) {
                    inputCommandID = inputCommand.split("_");
                }

                System.out.println("Input: "+inputCommandID[0]);
                if ((inputCommandID!=null) && (existingCommandID[0].equals(inputCommandID[0]))) {
                    allowed = true;

                } else {
                    allowed = false;

                }

            } else {
                allowed = true;

            }
        } catch (Exception e) {
        } finally {
            readLock.unlock();
        }
        System.out.println("Is allowed: " + allowed);
        return allowed;
    }

    public Object getKeyFromValue(String value) {
        try {
            readLock.lock();
            for (Object o : filesLocked.keySet()) {
                if (filesLocked.get(o).equals(value)) {
                    System.out.println("Before returning from getvale: " + o);
                    return o;
                }
            }
        } catch (Exception e) {
        } finally {
            readLock.unlock();
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
