
package FTPClientServer;

/**
 * Required imports
 *
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;

//Client class
public class FTP_Client {

        /**
         * Declaring the required variables
         *
         */
	static String serverAddress;
	static int serverPort;
	Socket clientSocket;
	String inputCommand;
        Object[] commandSplitArray = new Object[5];
        ObjectOutputStream outputStreamObj=null;
        ObjectInputStream inputStreamObj=null;
        
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
                quit

	};

        //Default Client's constructor
	FTP_Client(String serverAddressParm, int serverPortParm) {

		serverAddress = serverAddressParm;
		serverPort = serverPortParm;
	}


        /**
         * Function to display the list of available
         * commands
         * 
         */
        public void displayCommandsToUser() {
         
		try {
			
                        System.out.println("\n\nList of available commands: ");
                        System.out.println("ls");System.out.println("pwd");System.out.println("cd <absolute_path>");
                        System.out.println("mkdir <absolute_path>");
                        System.out.println("get <absolute_path/filename>");System.out.println("put <absolute_path/filename>");
                        System.out.println("delete <absolute_path/filename>");
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

		// Split the sentence based on spaces
		Object[] splittedCommand= this.inputCommand.split("\\s+");

                //The first param is the command always
                this.commandSplitArray[0]=splittedCommand[0].toString().toLowerCase();

                if(splittedCommand.length>1 && splittedCommand[1]!=null)
                    //The second param is the absolute_path of the file or directory always
                    this.commandSplitArray[1]=splittedCommand[1];


                return this.commandSplitArray;

	}

        /**
         * Function to validate the command input by the User.
         * - If valid command is entered, the command is executed on the Server
         * - If not, an error message is displayed.
         *
         */
	public boolean validateCommandAndSendToServer() {
            boolean commandResult=false;

            try {

               
                
                this.outputStreamObj= new ObjectOutputStream(this.clientSocket.getOutputStream());

                //Switch if its a valid command
		Commands currentCommand = Commands.valueOf((String)this.commandSplitArray[0]);

                switch (currentCommand) {

			case ls:
			case mkdir:
			case pwd:
			case delete:
			case cd:
                        case get:
                                //Send the data to the Server
				this.outputStreamObj.writeObject(this.commandSplitArray);
                                this.outputStreamObj.flush();
                                commandResult=true;
                                break;

                        
                       case put:
                                if(this.uploadToServer(this.commandSplitArray)){
                                    this.outputStreamObj.flush();
                                    commandResult=true;
                                }
                                
                                break;


                      default:
                                //Display the error and shut down
				System.out.println("Invalid command entered!!!");
                                System.out.println("Disconnecting from the Server.....");
				break;

			}
                

		} catch (Exception e) {
                    this.printStream("Invalid command entered!!!",true);
                    //e.printStackTrace();
                }
            return commandResult;
             
	}

         /**
         * Function to upload a file to the server
         *
         * @Object[] commandArray
         */
        public boolean uploadToServer(Object[] inputArray){
            boolean result=false;
            try{
               
                File fileObject=new File(inputArray[1].toString());

                if(fileObject.exists()){
                FileInputStream fileInputStreamObj = new FileInputStream(inputArray[1].toString());
                byte[] fileBytes = new byte[1024*1024];

                File originalFile=new File(inputArray[1].toString());

                //Store only the filename in commandSplitArray
                this.commandSplitArray[1]=originalFile.getName();

                //Store the contents of the file in commandSplitArray
                fileInputStreamObj.read(fileBytes);
                this.commandSplitArray[2]=fileBytes;

                //Send the commandSplitArray to the Server
                this.outputStreamObj.writeObject( this.commandSplitArray);
                
                //Flush the stream
                this.outputStreamObj.flush();

                System.out.println("File contents sent!!!");
                result=true;
                }else{
                    System.out.println("File not found!!!Please try again");
                    result=false;
                }


            }catch(Exception e){
                e.printStackTrace();
            }

            return result;


	}

        /**
         * Function to download a file from the Server
         * 
         */
        public void downloadFromServer(ObjectInputStream inputStreamObj ,String destinationFileName){
        String result=null;
        try{

            Object test=inputStreamObj.readObject();
            if(test instanceof byte[]){

            File fileObject=new File(destinationFileName);

            //Create a file
            FileOutputStream fileOutputStreamObject = new FileOutputStream(new File(System.getProperty("user.dir")+"/"+fileObject.getName()));

           
            System.out.println(test.toString());
            int count=test.toString().length();
            while(  count>0 ) {

                byte[] fileBytes = new byte[1024*1024];
                fileBytes = (byte[])test;

                //Write the contents to the file
                fileOutputStreamObject.write(fileBytes,0,1024*1024);

                count-=test.toString().length();
    

            }

            
            
            //Read the received contents
            /*byte[] fileBytes = new byte[1024*1024];
            fileBytes = (byte[])originalFile;

            //Write the contents to the file
            fileOutputStreamObject.write(fileBytes,0,1024*1024);*/

            //Close the stream
            fileOutputStreamObject.flush();
            

            this.printStream("File received successfully!!!",true);

            }else{
                this.printStream(inputStreamObj.readObject().toString(),true);
            }


            
            
      

        }catch(Exception e){
            e.printStackTrace();
        }

    }
	

        /**
         * Function to display the response sent from
         * Server.
         *
         */
        public void processServerResponse(){

         try{
                this.inputStreamObj = new ObjectInputStream(this.clientSocket.getInputStream());
                
                //Switch if its a valid command
		Commands currentCommand = Commands.valueOf((String)this.commandSplitArray[0]);

                /**
                 * Parse and display the response sent from Sever
                 * based on the type of the response is expected.
                 *
                 */
                switch(currentCommand){

                    case ls:  this.parseLsResponse(this.inputStreamObj);
                              break;

                    case get: this.downloadFromServer(this.inputStreamObj,(String)this.commandSplitArray[1]);
                              break;

                    default:  this.printStream(this.inputStreamObj.readObject().toString(),true);
                              break;

                }



            }catch(Exception e){

                System.out.println(e.getMessage());
            }
        }

        /**
         * Function to parse the 'ls' response
         *
         * @ObjectInputStream inputStream
         */
        public void parseLsResponse(ObjectInputStream inputStream){
            try{

             /**
              * Since, 'ls' will send a list of files, looping through each file to
              * display its name.
              *
              */
            ArrayList<String> fileList = (ArrayList<String>)inputStream.readObject();
                    for(String individualFile:fileList){
                        System.out.println(individualFile);
                    }
            }catch(Exception e){

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
        public void printStream(String stream, boolean newLine){
           String clientPrompt="mytftp> ";
           if(newLine)
                System.out.println(clientPrompt+stream);
           else
                System.out.print(clientPrompt+stream);
       }
 
        /**
         * Main function
         */
	public static void main(String[] args) {

            try
            {
		FTP_Client client=new FTP_Client(args[0],Integer.parseInt(args[1]));

                
                //Connect to the Server
                client.clientSocket = new Socket(args[0],Integer.parseInt(args[1]));
                System.out.println("Connecting to the server!!!!");
                
                while(true){
                    client.displayCommandsToUser();
                    client.readCommandFromUser();

                    if(client.commandSplitArray[0].toString().equalsIgnoreCase("quit")){

                          client.clientSocket.close();
                          client.printStream("Disconnecting from the Server.....",true);
                          System.exit(0);
				
                    }else{
                        if(client.validateCommandAndSendToServer()){
                            client.processServerResponse();
                        }
                    }

                }
                
            } catch (Exception e){
                e.printStackTrace();
            }


	}

}
