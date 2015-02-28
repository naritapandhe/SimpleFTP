package FTPClientServer;

/**
 * Required imports
 *
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

//Server Class
public class FTP_Server implements Runnable {

    /**
     * Declaring the required variables
     *
     */
    int normalPort;
    int terminatePort;
    String currentThreadID;
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
    public static volatile HashMap<String, String> filesLocked = new HashMap<String, String>();
    static String GET_COMMAND_ID = "1";
    static String PUT_COMMAND_ID = "2";
    static String DELETE_COMMAND_ID = "3";
    int receivedFileSize = 0;
    int totalFileSize = 0;
    String currentFileName;
    String currentCommandID;
    Object mainData;
    String currentWorkingDirectory;
    String[] clientCommandString;
    Object[] clientCommandObject;
    public static volatile boolean isLocked = false;
    public static volatile boolean isSchedulable = true;

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


            System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));
            Object inputObj = null;
            this.inputStreamObj = new ObjectInputStream(this.clientNormalPortSocket.getInputStream());
            if ((inputObj = this.inputStreamObj.readObject()) == null) {
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

                //Store the params
                if (clientInput.length > 1 && clientInput[1] != null) {

                    this.clientParams = (String) clientInput[1];
                }

                //Store the fileContents
                if (clientInput.length > 2 && clientInput[2] != null) {
                    this.clientFileContents = clientInput[2];
                }

                //Store the file size
                if (clientInput.length > 3 && clientInput[3] != null) {

                    this.totalFileSize = Integer.parseInt(clientInput[3]);
                }

                if (clientInput.length > 6 && clientInput[6] != null) {

                    this.currentWorkingDirectory = clientInput[6].toString();
                }



            } else {

                Object[] clientInput = (Object[]) inputObj;
                this.clientCommand = clientInput[0].toString();
                this.clientParams = (String) clientInput[1];

                //Store the fileContents
                if (clientInput.length > 2 && clientInput[2] != null) {
                    this.clientFileContents = clientInput[2];
                }


                //Store the file size
                if (clientInput.length > 3 && clientInput[3] != null) {

                    this.totalFileSize = Integer.parseInt(clientInput[3].toString());
                }

                this.clientCommandObject = clientInput.clone();


            }

            this.currentCommand = Commands.valueOf(this.clientCommand);
            System.out.println("\n\nCommand received from Client: '" + this.clientCommand + "'");
            System.out.println("Parameters received from Client: '" + this.clientParams + "'");


        } catch (EOFException e) {

            System.out.println("Client exited!!!");

        } catch (Exception e) {
            //e.printStackTrace();
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
                    String fileName = null;
                    if (this.clientParams.toString().equals("..") && this.currentWorkingDirectory != null) {
                        fileName = this.currentWorkingDirectory;

                    } else {
                        fileName = this.clientParams.toString();
                    }
                    commandResult = this.executeCd(fileName);
                    this.clientOutputObj.writeObject(commandResult);
                    break;

                case delete:
                    commandResult = this.executeDelete((String) this.clientParams);
                    this.clientOutputObj.writeObject(commandResult);
                    break;


                case put:
                    System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));
                    while (!isSchedulable) {
                        System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));

                        Thread.currentThread().sleep(5000);
                        System.out.println("Thread is waiting.....");

                        if (isSchedulable) {
                            break;
                        }
                    }
                    if (isSchedulable) {
                        System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));
                        isSchedulable = false;
                        if (this.currentCommandID == null) {
                            this.currentFileName = (String) this.clientParams;
                            commandResult = this.executePut((String) this.clientParams);
                            this.clientOutputObj.writeObject(commandResult);
                            this.clientOutputObj.flush();
                            this.currentCommandID = commandResult;
                        }
                        if (commandResult.length() <= 6) {
                            this.readCommandFromClient();
                            commandResult = this.executePut((String) this.clientParams);
                        }
                        System.out.println(commandResult);
                    }
                    break;

                case get:
                    System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));
                    while (!isSchedulable) {
                        String existingKey=null;
                        if((existingKey=this.getKeyFromValue((String)this.clientParams).toString())!=null)
                        {
                          String[] existingCommandIDArray = existingKey.toString().split("_");
                          if(existingCommandIDArray[1].equals("1")){
                                 isSchedulable = true;

                             }else{
                                 isSchedulable = false;
                             }
                        }
                        System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));
                        Thread.currentThread().sleep(5000);
                        System.out.println("Thread is waiting.....");
                        if (isSchedulable) {
                            break;
                        }
                    }
                    if (isSchedulable) {
                        isSchedulable = false;
                        this.executeGet((String) this.clientParams);
                    }
                    break;

                case terminate:
                    boolean terminateResult = this.executeTerminate(this.clientParams.toString());
                    if (terminateResult) {
                        commandResult = "Command terminated succesfully";
                    } else {
                        commandResult = "";
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

            File fileObj;
            if (this.clientParams != null) {
                fileObj = new File(this.clientParams.toString());

            } else {
                //Find the required directory
                fileObj = new File(System.getProperty("user.dir") + "/");
            }
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
        File currentDirectory = new File(dirName);
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
            File fileObj;
            if (this.clientParams != null) {
                fileObj = new File(this.clientParams.toString());
            } else {
                fileObj = new File(System.getProperty("user.dir"));
            }
            result = fileObj.getAbsoluteFile().getParent();

        } else {

            //If 'cd xxxx' is to be executed, then point to the directory name mentionsd
            File dirObj = new File(dirName);
            if (dirObj.exists()) {
                result = dirObj.getAbsolutePath();
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
        this.currentCommandID = this.currentThreadID + "_" + DELETE_COMMAND_ID;

        System.out.println("Executing delete...");
        String result = null;

        //Create a file handle for the required file to be deleted
        File fileObj = new File(fileName);

        //If the file doesn't exists, then display an error
        if (!fileObj.exists()) {
            result = "Directory / File does not exists!! Please try again.";

        } else if (fileObj.isFile()) {
            if (this.isAllowed(fileName, this.currentCommandID)) {
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
    public String executePut(String fileName) {
        System.out.println("Executing put...");
        String result = null;

        try {

            byte[] fileBytes = null;
            boolean isFileRecieved = false;

            //Implies the Client command has not been sent
            if (this.clientCommandObject[5] == null) {
                this.currentCommandID = Long.toString(Thread.currentThread().getId()) + "_" + PUT_COMMAND_ID;
                if (this.isAllowed(this.clientCommandObject[1].toString(), this.currentCommandID)) {
                    this.pushToMap(this.currentCommandID, this.clientCommandObject[1].toString());
                    result = this.currentCommandID;

                }
            } else {

                this.currentCommandID = this.clientCommandObject[5].toString();
                while (true && !isFileRecieved && !Thread.currentThread().isInterrupted()) {

                    if (!this.isTerminated(this.currentCommandID)) {
                        Thread.currentThread().sleep(1000);
                        System.out.println("File receiving.....");

                        FileOutputStream fileOutputStreamObject;
                        File f = new File(System.getProperty("user.dir") + "/" + this.clientCommandObject[1]);
                        if (f.exists()) {
                            fileOutputStreamObject = new FileOutputStream(f, false);
                        } else {
                            fileOutputStreamObject = new FileOutputStream(f);
                        }

                        fileBytes = new byte[this.totalFileSize];
                        fileBytes = (byte[]) this.clientFileContents;
                        //Write the bytes to the file
                        fileOutputStreamObject.write(fileBytes, 0, this.totalFileSize);

                        result = "File received successfully!!!";
                        isFileRecieved = true;
                        this.executeTerminate(this.currentCommandID);

                    } else {
                        System.out.println("Client sent terminate signal");
                        Thread.currentThread().interrupt();
                        result = "TERMINATED";
                        fileBytes = result.getBytes();
                        this.clientOutputObj.writeObject(fileBytes);

                    }


                }

            }


        } catch (Exception e) {

            if (this.currentCommandID != null) {
                this.executeTerminate(this.currentCommandID);
            }
            result = "Some error occurred!!!Please try again";
            // e.printStackTrace();
        }

        return result;
    }

    /**
     * Function to execute the 'get' command
     * i.e. Send to Client
     * @String fileName
     */
    public void executeGet(String fileName) {

        try {

            //Lakshat thev: To change it to 1000bytes
            int FIXED_BUFFER_SIZE = 1000;

            boolean commandIDSent = false;
            String result = null;
            System.out.println("Executing get...");
            File fileObj = new File(fileName);

            int iteration = 1;
            boolean isFileSent = false;
            this.currentCommandID = this.currentThreadID + "_" + GET_COMMAND_ID;


            if (fileObj.exists()) {

                if (this.isAllowed(fileName, this.currentCommandID)) {

                    this.pushToMap(this.currentCommandID, fileName);
                    FileInputStream fileInputStreamObj = new FileInputStream(fileName);
                    int fsize = (int) fileInputStreamObj.getChannel().size();


                    byte[] fileBytes = new byte[FIXED_BUFFER_SIZE];

                    //Read the contents of a file in a buffer and send it to Client
                    while (true && !isFileSent && !Thread.currentThread().isInterrupted()) {
                        if (!commandIDSent) {

                            result = "CommandID:" + this.currentCommandID;
                            fileBytes = result.getBytes("UTF-8");
                            this.clientOutputObj.writeObject(fileBytes);
                            commandIDSent = true;

                            //Flush the stream
                            this.clientOutputObj.flush();


                        } else {

                            Thread.currentThread().sleep(15000);
                            if (!this.isTerminated(this.currentCommandID)) {
                                System.out.println("Sending the file contents now...");
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
                                    this.executeTerminate(this.currentCommandID);


                                }

                                iteration++;

                            } else {

                                System.out.println("Client sent terminate signal");
                                Thread.currentThread().interrupt();
                                String terminateResponse = "TERMINATED";
                                fileBytes = terminateResponse.getBytes();
                                this.clientOutputObj.writeObject(fileBytes);
                                Thread.currentThread().interrupt();
                                return;
                            }


                        }

                    }//<while end>

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
            if (!isLocked) {

                isLocked = true;
                if (filesLocked.containsKey(commandID)) {
                    filesLocked.remove(commandID);
                    System.out.println("Command ID found!!!Removing corresponding thread from the map.");
                    terminate = true;
                    isSchedulable = true;
                    System.out.println("Schedulable: " + isSchedulable + " :thread id: " + Long.toString(Thread.currentThread().getId()));


                }
            }
        } catch (Exception e) {
        } finally {

            isLocked = false;

        }
        // System.out.println("Is thread terminated: " + terminate);
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
            if (!isLocked) {
                isLocked = true;
                if (!filesLocked.containsKey(commandID)) {
                    terminated = true;
                }
            }

        } catch (Exception e) {
        } finally {

            isLocked = false;
        }

        //System.out.println("Terminate current status: " + terminated);
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
            if (!isLocked) {

                isLocked = true;
                if (filesLocked.containsValue(fileName)) {
                    Object key = this.getKeyFromValue(fileName);
                    String[] existingCommandIDArray = key.toString().split("_");
                    String[] inputCommandIDArray = inputCommand.split("_");
                    if (existingCommandIDArray[1].equals(inputCommandIDArray[1]) && ((existingCommandIDArray[1].equals("1")) && (inputCommandIDArray[1].equals("1")))) {
                        allowed = true;
                    }
                } else {
                    allowed = true;
                }

            }

        } catch (Exception e) {
        } finally {
            isLocked = false;
        }
        // System.out.println("Is allowed: " + allowed);
        return allowed;
    }

    public void pushToMap(String commandID, String fileName) {

        try {
            if (!isLocked) {
                isLocked = true;
                filesLocked.put(commandID, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isLocked = false;
        }


    }

    public Object getKeyFromValue(String value) {
        try {

            if (!isLocked) {

                isLocked = true;
                for (Object o : filesLocked.keySet()) {
                    if (filesLocked.get(o).equals(value)) {
                        return o;
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            isLocked = false;
        }
        return null;
    }

    public void run() {
        try {

            this.readCommandFromClient();
            this.validateAndExecuteCommand();


        } catch (Exception e) {
        }
    }
}
