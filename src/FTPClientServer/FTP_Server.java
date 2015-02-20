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
public class FTP_Server implements Runnable  {

    /**
     * Declaring the required variables
     *
     */
//    static int serverPort;
//    Socket clientSocket;
//    static ServerSocket serverSocket;

     int normalPort;
     int terminatePort;

    Socket clientNormalPortSocket;
    Socket clientTerminatePortSocket;

     ServerSocket serverNormalPortSocket;
     ServerSocket serverTerminatePortSocket;



    String clientCommand;
    Object clientParams;
    Object clientFileContents;

    ObjectOutputStream clientOutputObj = null;
    ObjectInputStream inputStreamObj = null;

    ObjectOutputStream terminateClientOutputObj = null;
    ObjectInputStream terminateInputStreamObj = null;

    //String commandID="1";

    HashMap<String,String> filesLocked=new HashMap<String, String>();

    String currentThreadID=null;

    boolean connStatus=false;
    
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

    
    //Default Constructor
    FTP_Server(int nPort, int tPort) {

        try {

            System.out.println("Server Running!!!!");

            this.normalPort = nPort;
            this.serverNormalPortSocket = new ServerSocket(this.normalPort);
            System.out.println("To execute normal commands, please connect to the Server from Client using " + normalPort + " port.");

            this.terminatePort=tPort;
            this.serverTerminatePortSocket=new ServerSocket(this.terminatePort);
            System.out.println("To terminate execution of commands, please connect to the terminate port: " + terminatePort );


        } catch (Exception e) {
           e.printStackTrace();
        }

    }

     //Default Constructor
    FTP_Server(Socket normal, Socket terminate) {

        try {

                    this.clientNormalPortSocket = normal;
                    System.out.println("normal port: "+ this.clientNormalPortSocket);

                    this.clientTerminatePortSocket = terminate;
                    System.out.println("terminate port: "+ this.clientTerminatePortSocket);


        } catch (Exception e) {
           e.printStackTrace();
        }

    }

    public void acceptConnection(){
        try{

                    this.clientNormalPortSocket = this.serverNormalPortSocket.accept();
                    System.out.println("normal port: "+ this.clientNormalPortSocket);

                    this.clientTerminatePortSocket = this.serverTerminatePortSocket.accept();
                    System.out.println("terminate port: "+ this.clientTerminatePortSocket);
                    
        }catch(Exception e){
//            this.currentThread().interrupt();
//            this.currentThread().yield();
            
        }
        
    }

    
    /**
     * Function to read the input sent from Client
     *
     */
    public void readCommandFromClient() {
        try {

            Object inputObj=null;
            if((inputObj=new ObjectInputStream(this.clientNormalPortSocket.getInputStream()).readObject()) == null){
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


            } else {

                /**
                 * clientInput is an array consisting of the
                 * command, filename and the file contents as byte[]
                 * Eg: put <absolute_path> <file_contents>
                 * clientInput[0]: put
                 * clientInput[1]: <absolute_path>
                 * clientInput[2]: <file_contents>
                 *
                 */
                Object[] clientInput = (Object[]) inputObj;
                this.clientCommand = clientInput[0].toString();
                this.clientParams = (String) clientInput[1];
                this.clientFileContents = clientInput[2];

            }
            System.out.println("\n\nCommand received from Client: '" + this.clientCommand + "'");
            System.out.println("Parameters received from Client: '" + this.clientParams + "'");



        }catch (EOFException e) {

           System.out.println("Client exited!!!");
//           this.currentThread().interrupt();
               
        }catch (Exception e) {
            //System.out.println("Exception: "+e.getMessage());
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
            Commands currentCommand = Commands.valueOf(this.clientCommand);

            //Switch based on the command to be executed
            switch (currentCommand) {

                case ls:
                    lsResult = this.executeLs();
                    this.clientOutputObj.writeObject(lsResult);
                   // this.currentThread().interrupt();
                    //this.currentThread().yield();
                    break;

                case pwd:
                    commandResult = this.executePwd();
                    this.clientOutputObj.writeObject(commandResult);
                    //this.currentThread().interrupt();
                    //this.currentThread().yield();
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

                case put:
                    commandResult = this.executePut((String) this.clientParams, this.clientFileContents);
                    this.clientOutputObj.writeObject(commandResult);
                    break;

                case get:
                    this.executeGet((String) this.clientParams);
                    break;

                case terminate:
                     this.filesLocked.put("1", "GET");
                     boolean terminateResult=this.executeTerminate(this.clientParams.toString());
                     if(terminateResult){
                         commandResult="Command terminated succesfully";
                     }else{
                         commandResult="Invalid command ID.";
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
            System.setProperty("user.dir", fileObj.getAbsoluteFile().getParent());
            result = "Directory changed to: " + System.getProperty("user.dir");

        } else {

            //If 'cd xxxx' is to be executed, then point to the directory name mentionsd
            File dirObj = new File(dirName);
            if (dirObj.exists()) {
                System.setProperty("user.dir", dirObj.getAbsolutePath());
                result = "Directory changed to: " + System.getProperty("user.dir");
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
        System.out.println("Executing delete...");
        String result = null;

        //Create a file handle for the required file to be deleted
        File fileObj = new File(fileName);

        //If the file doesn't exists, then display an error
        if (!fileObj.exists()) {
            result = "Directory / File does not exists!! Please try again.";

        } else if (fileObj.isFile()) {
            //If file exists, then delete the file
            fileObj.delete();
            result = "Directory / File has been deleted successfully.";

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
     * Function to execute the 'put' command
     * @String
     */
    public String executePut(String fileName, Object fileContents) {
        System.out.println("Executing put...");
        String result = null;
        try {

            FileOutputStream fileOutputStreamObject = new FileOutputStream(new File(System.getProperty("user.dir") + "/" + fileName));
            byte[] fileBytes = new byte[1024 * 1024];


            /**
             * Create a file on the Server
             * Write the contents sent by Client, in that file
             *
             */
            fileBytes = (byte[]) fileContents;

            //Write the bytes to the file
            fileOutputStreamObject.write(fileBytes, 0, 1024 * 1024);

            result = "File uploaded successfully!!!";

            //Close the stream
            fileOutputStreamObject.close();

            



            

        } catch (Exception e) {
            result = "Some error occurred!!!Please try again";
            //e.printStackTrace();
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
            int FIXED_BUFFER_SIZE=10;
            String GET_COMMAND_ID="1";

            boolean commandIDSent=false;
            String result = null;
            System.out.println("Executing get...");
            File fileObj = new File(fileName);
            
            int iteration=1;
            boolean isFileSent=false;
            String commandID=this.currentThreadID+"_"+GET_COMMAND_ID;

            System.out.println("Command ID: "+commandID);

            if (fileObj.exists()) {

                this.filesLocked.put(commandID, fileName);
                
                FileInputStream fileInputStreamObj = new FileInputStream(fileName);
                int counter = 0;//used to track progress through upload
                int fsize=(int)fileInputStreamObj.getChannel().size();


                byte[] fileBytes = new byte[FIXED_BUFFER_SIZE];

                //Read the contents of a file in a buffer and send it to Client
                while(true && !isFileSent){
                    if(!commandIDSent){

                        
                        result = "CommandID:"+commandID;
                        fileBytes = result.getBytes("UTF-8");
                        this.clientOutputObj.writeObject(fileBytes);
                        commandIDSent=true;

                        System.out.println("Command ID sent!!");

                        //Flush the stream
                        this.clientOutputObj.flush();
                     

                    }else{

                      //  this.sleep(20000);
                        if(!this.isTerminated(commandID)){

                        System.out.println("Sending the file contents now");

                        int size=0;

                        if(fsize>(iteration*FIXED_BUFFER_SIZE)){
                            size=FIXED_BUFFER_SIZE;
                            fileBytes = new byte[size];
                            fileInputStreamObj.read(fileBytes);

                            //Send the contents of file to Client
                            this.clientOutputObj.writeObject(fileBytes);

                            //Flush the stream
                            this.clientOutputObj.flush();

                        } else{

                            size=fsize-((iteration-1)*FIXED_BUFFER_SIZE);
                            isFileSent=true;
                            fileBytes = new byte[size];
                            fileInputStreamObj.read(fileBytes);

                            //Send the contents of file to Client
                            this.clientOutputObj.writeObject(fileBytes);

                            //Flush the stream
                            this.clientOutputObj.flush();

                        }

                        iteration++;

                        }else{
                            System.out.println("Client sent terminate signal");
                            return;
                        }
                        
                        
                   }

                }

                
                

            } else {

                result = "File not found!!!Please try again.";
                this.clientOutputObj.writeObject(result);


            }
            this.clientOutputObj.flush();

        } catch (Exception e) {
           // e.printStackTrace();
        }

    }

    public boolean executeTerminate(String commandID){
        boolean terminate=false;

        try{

             if(this.filesLocked.containsKey(commandID)){
                 this.filesLocked.remove(commandID);
                 terminate=true;
             }
        }catch(Exception e){

        }
        System.out.println("File remove keli ka: "+terminate);
        return terminate;
    }

    public boolean isTerminated(String commandID){
        
        boolean terminated=false;
        try{

            if(!this.filesLocked.containsKey(commandID))
             terminated=true;
             
        }catch(Exception e){

        }

        System.out.println("Terminate current status: "+terminated);
        return terminated;

    }



    public void run() {
        try {

                    this.readCommandFromClient();
                    this.validateAndExecuteCommand();

           
        }catch (Exception e) {

            System.out.println("Done!!");
            
        }
  }

         
   
}


