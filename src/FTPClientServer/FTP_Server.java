package FTPClientServer;

/**
 * Required imports
 *
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//Server Class
public class FTP_Server implements Runnable {

    /**
     * Declaring the required variables
     *
     */
    int normalPort;
    int terminatePort;
    String currentThreadID = null;
    String clientCommand;
    Socket clientNormalPortSocket;
    Socket clientTerminatePortSocket;
    ServerSocket serverNormalPortSocket;
    ServerSocket serverTerminatePortSocket;
    Object clientParams;
    Object clientFileContents;
    ObjectOutputStream clientOutputObj = null;
    ObjectInputStream inputStreamObj = null;
    ObjectOutputStream terminateClientOutputObj = null;
    ObjectInputStream terminateInputStreamObj = null;
    public static ConcurrentMap<String, String> filesLocked = new ConcurrentHashMap<String, String>();
    static String GET_COMMAND_ID = "1";
    static String PUT_COMMAND_ID = "2";
    static String DELETE_COMMAND_ID = "3";
    int receivedFileSize = 0;
    int totalFileSize = 0;
String currentFileName=null;
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
        terminate
    };
    Commands currentCommand;

    //Default Constructor
    FTP_Server(int nPort, int tPort) {

        try {

            System.out.println("Server Running!!!!");

            this.normalPort = nPort;
            this.serverNormalPortSocket = new ServerSocket(this.normalPort);
            System.out.println("To execute normal commands, please connect to the Server from Client using " + normalPort + " port.");

            this.terminatePort = tPort;
            this.serverTerminatePortSocket = new ServerSocket(this.terminatePort);
            System.out.println("To terminate execution of commands, please connect to the terminate port: " + terminatePort);


            this.currentCommand = null;


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Default Constructor
    FTP_Server(Socket normal, Socket terminate) {

        try {

            this.clientNormalPortSocket = normal;
            System.out.println("Normal port: " + this.clientNormalPortSocket);

            this.clientTerminatePortSocket = terminate;
            System.out.println("Terminate port: " + this.clientTerminatePortSocket);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to read the input sent from Client
     *
     */
    public void readCommandFromClient() {
        try {

            Object inputObj = null;
            if ((inputObj = new ObjectInputStream(this.clientNormalPortSocket.getInputStream()).readObject()) == null) {
                this.terminateInputStreamObj = new ObjectInputStream(this.clientTerminatePortSocket.getInputStream());
                inputObj = this.terminateInputStreamObj.readObject();

            }


            /**
             * inputObj can either be the buffered file contents(eg: put <fileName>)
             * or it can simply be a String of command to be executed (eg: ls)
             *
             */
            if (inputObj instanceof String[]) {

                /**
                 * clientInput is an array consisting of the
                 * command and its parameters separated.
                 * Eg: mkdir test
                 * clientInput[0]: mkdir
                 * clientInput[1]: test
                 *
                 */
                String[] clientInput = (String[]) inputObj;
                this.clientCommand = clientInput[0].toLowerCase();
                if (clientInput.length > 1 && clientInput[1] != null) {

                    this.clientParams = (String) clientInput[1];
                }

                if (clientInput.length > 3 && clientInput[3] != null) {

                    this.totalFileSize = Integer.parseInt(clientInput[3]);
                }



            }

            /**
             * clientInput is an array consisting of the
             * command, filename and the file contents as byte[]
             * Eg: put <absolute_path> <file_contents>
             * clientInput[0]: put
             * clientInput[1]: <absolute_path>
             * clientInput[2]: <file_contents>
             *

            Object[] clientInput = (Object[]) inputObj;
            System.out.println("fjfjfjfjfjfjblah: " + clientInput[0].toString());
            this.clientCommand = clientInput[0].toString();
            this.clientParams = (String) clientInput[1];
            this.clientFileContents = clientInput[2];

            }*/

            this.currentCommand = Commands.valueOf(this.clientCommand);
            System.out.println("\n\nCommand received from Client: '" + this.clientCommand + "'");
            System.out.println("Parameters received from Client: '" + this.clientParams + "'");



        } catch (EOFException e) {

            System.out.println("Client exited!!!");

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Function to execute the command
     *
     */
    public void validateAndExecuteCommand() {
        try {
            String commandResult = null;
            ArrayList<String> lsResult = new ArrayList<String>();
            this.clientOutputObj = new ObjectOutputStream(this.clientNormalPortSocket.getOutputStream());


            //Switch based on the command to be executed
            switch (this.currentCommand) {

                case ls:
                    lsResult = this.executeLs();
                    this.clientOutputObj.writeObject(lsResult);
                    break;

                case pwd:
                    commandResult = this.executePwd();
                    this.clientOutputObj.writeObject(commandResult);
                    break;

                case mkdir:
                    commandResult = this.executeMkdir((String) this.clientParams);
                    this.clientOutputObj.writeObject(commandResult);
                    break;

                case cd:
                    commandResult = this.executeCd((String) this.clientParams);
                    this.clientOutputObj.writeObject(commandResult);
                    break;

                case delete:
                    commandResult = this.executeDelete((String) this.clientParams);
                    this.clientOutputObj.writeObject(commandResult);
                    break;

               /* case put:
                    this.currentFileName=(String) this.clientParams;
                    commandResult=this.executePut((String) this.clientParams);
                    System.out.println(commandResult);
                    break;*/

                case get:
                    this.executeGet((String) this.clientParams);
                    break;

                case terminate:
                    boolean terminateResult = this.executeTerminate(this.clientParams.toString());
                    if (terminateResult) {
                        commandResult = "Command terminated succesfully";
                    } else {
                        commandResult = "Invalid command ID.";
                    }
                    this.clientOutputObj.writeObject(commandResult);
                    break;
                default:
                    commandResult = "Invalid command!!! Please try again.";
                    break;


            }


        } catch (Exception e) {
            //e.printStackTrace();
        }



    }

    /**
     * Function to execute the 'ls' command
     * @ArrayList<String>
     */
    public ArrayList<String> executeLs() {

        System.out.println("Executing ls...");
        ArrayList<String> fileList = new ArrayList<String>();
        try {

            //Find the required directory
            File fileObj = new File(System.getProperty("user.dir") + "/");

            //Find the files present inside the directory
            File[] listOfFiles = fileObj.listFiles();

            //Loop on the directory contents, to find the file names
            for (int i = 0; i < listOfFiles.length; i++) {
                fileList.add(listOfFiles[i].getName() + "\t");

            }

        } catch (Exception e) {
            // e.printStackTrace();
        }
        return fileList;
    }

    /**
     * Function to execute the 'pwd' command
     * @String
     */
    public String executePwd() {
        System.out.println("Executing pwd...");
        return (System.getProperty("user.dir").toString());

    }

    /**
     *
     * Function to execute the 'mkdir' command
     * @String dirName: Absolute path of the directory to be created
     */
    public String executeMkdir(String dirName) {
        System.out.println("Executing mkdir...");
        String result = null;


        /**
         * Obtain the fileHandle for that particular directory.
         * This is done to check if the directory already exists.
         */
        File currentDirectory = new File(System.getProperty("user.dir") + "/" + dirName);
        try {
            boolean status = false;

            //If the directory doesn't exists, then create it
            if (!currentDirectory.exists()) {
                currentDirectory.mkdir();
                currentDirectory.setExecutable(true);
                currentDirectory.setReadable(true);
                currentDirectory.setWritable(true);
                status = true;

                if (status) {
                    result = "Directory created successfully!!";
                } else {
                    result = "Directory cannot be created. Please try again.";
                }

            } else {
                result = "Directory already exists!!";
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return result;
    }

    /**
     * Function to execute the 'cd' command
     *
     * @String dirName: Absolute path of the directory to change to
     */
    public String executeCd(String dirName) {
        System.out.println("Executing cd...");
        String result = null;
        // If 'cd ..' command is to be exceuted, then point the working directory to parent
        if (dirName.equals("..")) {
            File fileObj = new File(System.getProperty("user.dir"));
            result = "Directory changed to: " + fileObj.getAbsoluteFile().getParent();

        } else {

            //If 'cd xxxx' is to be executed, then point to the directory name mentionsd
            File dirObj = new File(dirName);
            if (dirObj.exists()) {
                result = "Directory changed to: " + dirObj.getAbsolutePath();
            } else {
                result = "Directory not be found!!! Please try again";
            }

        }


        //Return the new working directory
        return result;

    }

    /**
     * Function to execute the 'delete' command
     * @param fileName: Absolute path of the directory/file to be deleted
     * @return String
     */
    public String executeDelete(String fileName) {
        String commandID = this.currentThreadID + "_" + DELETE_COMMAND_ID;
        System.out.println("Command ID: " + commandID);

        System.out.println("Executing delete...");
        String result = null;

        //Create a file handle for the required file to be deleted
        File fileObj = new File(fileName);

        //If the file doesn't exists, then display an error
        if (!fileObj.exists()) {
            result = "Directory / File does not exists!! Please try again.";

        } else if (fileObj.isFile()) {
            if (this.isAllowed(fileName, commandID)) {
                //If file exists, then delete the file
                fileObj.delete();
                result = "Directory / File has been deleted successfully.";
            } else {
                result = "Please try again after sometime";
            }

        } else if (fileObj.isDirectory()) {
            /**
             * If file is a directory, then
             *     - Recursively, delete the contents of the directory first
             *     - Delete the directory itself
             */
            File[] allFiles = new File(fileName).listFiles();
            for (File individualFile : allFiles) {
                //Delete individual file within the folder
                this.executeDelete(individualFile.getAbsolutePath());

            }
            if (fileObj.list().length == 0) {
                fileObj.delete();
                System.out.println("After final directory delete");
                result = "Directory / File has been deleted successfully.";

            }

        }
        return result;

    }

    /**
     *
     * Function to execute PUT
     * 
     * @param fileName
     * @return
     */
    /*public String executePut(String fileName) {
        System.out.println("Executing put...");
        String result = null;

        try {

            int FIXED_BUFFER_SIZE = 10;
            byte[] fileBytes = null;
            boolean isFileRecieved = false;
            boolean isCommandIDSent = false;


            while (true && !isFileRecieved) {

                if (!isCommandIDSent) {

                    String commandID = this.currentThreadID + "_" + PUT_COMMAND_ID;
                    result = commandID;
                    byte[] commandIDBytes = result.getBytes("UTF-8");
                    this.clientOutputObj.writeObject(commandIDBytes);
                    isCommandIDSent = true;
                    System.out.println("Command ID sent!!" + result);

                    //Flush the stream
                    this.clientOutputObj.flush();
                    this.clientOutputObj.reset();


                }else{

                if (this.receivedFileSize < this.totalFileSize) {

                    Thread.currentThread().sleep(1000);
                    this.inputStreamObj = new ObjectInputStream(this.clientNormalPortSocket.getInputStream());
                    FileOutputStream fileOutputStreamObject;
                    File f = new File(System.getProperty("user.dir") + "/" + fileName);
                    if (f.exists()) {
                        fileOutputStreamObject = new FileOutputStream(f, true);
                    } else {
                        fileOutputStreamObject = new FileOutputStream(f);
                    }

                    System.out.println("Now receiving the file contents....");
                    Object fileContents1 = this.inputStreamObj.readObject();
                    if (((byte[]) fileContents1).length == FIXED_BUFFER_SIZE) {
                        fileBytes = new byte[FIXED_BUFFER_SIZE];

                    } else {
                        fileBytes = new byte[((byte[]) fileContents1).length];
                        isFileRecieved = true;


                    }
                    this.receivedFileSize += fileBytes.length;
                    fileBytes = (byte[]) fileContents1;
                    System.out.println(this.receivedFileSize+" bytes written.");

                    //Write the contents to the file
                    fileOutputStreamObject.write(fileBytes);

                    //Close the stream
                    fileOutputStreamObject.flush();

                    if (isFileRecieved) {
                        result = "File received successfully!!!";
                        return result;

                    }

                } else {

                    isFileRecieved = true;
                    result = "Finished receiving file contents";
                    return result;


                }
            }
            }

        } catch (Exception e) {
            result = "Some error occurred!!!Please try again";
            e.printStackTrace();
        }

        return result;
    }*/

    /**
     * Function to execute the 'get' command
     * i.e. Send to Client
     * @String fileName
     */
    public void executeGet(String fileName) {

        try {

            //Lakshat thev: To change it to 1000bytes
            int FIXED_BUFFER_SIZE = 10;

            boolean commandIDSent = false;
            String result = null;
            System.out.println("Executing get...");
            File fileObj = new File(fileName);

            int iteration = 1;
            boolean isFileSent = false;
            String commandID = this.currentThreadID + "_" + GET_COMMAND_ID;

            System.out.println("Command ID: " + commandID);

            if (fileObj.exists()) {

                if (this.isAllowed(fileName, commandID)) {

                    filesLocked.put(commandID, fileName);

                    FileInputStream fileInputStreamObj = new FileInputStream(fileName);
                    int counter = 0;//used to track progress through upload
                    int fsize = (int) fileInputStreamObj.getChannel().size();


                    byte[] fileBytes = new byte[FIXED_BUFFER_SIZE];

                    //Read the contents of a file in a buffer and send it to Client
                    while (true && !isFileSent && !Thread.currentThread().interrupted()) {
                        if (!commandIDSent) {


                            result = "CommandID:" + commandID;
                            fileBytes = result.getBytes("UTF-8");
                            this.clientOutputObj.writeObject(fileBytes);
                            commandIDSent = true;

                            System.out.println("Command ID sent!!");

                            //Flush the stream
                            this.clientOutputObj.flush();


                        } else {

                            Thread.currentThread().sleep(10000);
                            if (!this.isTerminated(commandID)) {

                                System.out.println("Sending the file contents now: " + commandID);

                                int size = 0;

                                if (fsize > (iteration * FIXED_BUFFER_SIZE)) {
                                    size = FIXED_BUFFER_SIZE;
                                    fileBytes = new byte[size];
                                    fileInputStreamObj.read(fileBytes);

                                    //Send the contents of file to Client
                                    this.clientOutputObj.writeObject(fileBytes);

                                    //Flush the stream
                                    this.clientOutputObj.flush();

                                } else {

                                    size = fsize - ((iteration - 1) * FIXED_BUFFER_SIZE);
                                    isFileSent = true;
                                    fileBytes = new byte[size];
                                    fileInputStreamObj.read(fileBytes);

                                    //Send the contents of file to Client
                                    this.clientOutputObj.writeObject(fileBytes);

                                    //Flush the stream
                                    this.clientOutputObj.flush();

                                    System.out.println("File sent successfully!!! Please verify at the client side");

                                    this.clientOutputObj.reset();
                                    this.executeTerminate(commandID);


                                }

                                iteration++;

                            } else {
                                System.out.println("Client sent terminate signal");
                                Thread.currentThread().interrupt();
                                return;
                            }


                        }

                    }

                } else {
                    System.out.println("Please try again after sometime.");
                    Thread.currentThread().interrupt();
                    return;
                }



            } else {

                result = "File not found!!!Please try again.";
                Thread.currentThread().interrupt();
                this.clientOutputObj.writeObject(result);


            }
            this.clientOutputObj.flush();

        } catch (Exception e) {
            // e.printStackTrace();
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
            if (filesLocked.containsValue(fileName)) {
                Object key = this.getKeyFromValue(filesLocked, fileName);
                String[] existingCommandIDArray = key.toString().split("_");
                String[] inputCommandIDArray = inputCommand.split("_");
                if ((existingCommandIDArray[1].equals(inputCommandIDArray[1])) && (existingCommandIDArray[1].equals("1") && inputCommandIDArray[1].equals("1"))) {
                    allowed = true;
                }

            } else {
                allowed = true;
            }
        } catch (Exception e) {
        }
        System.out.println("Is allowed: " + allowed);
        return allowed;
    }

    public Object getKeyFromValue(ConcurrentMap hm, String value) {
        for (Object o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }

    public void run() {
        try {

            this.readCommandFromClient();
            this.validateAndExecuteCommand();


        } catch (Exception e) {

            System.out.println("Done!!");
            e.printStackTrace();

        }
    }
}
