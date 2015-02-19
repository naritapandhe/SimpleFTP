/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package FTPClientServer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author admin
 */
public class FTP_Client_Main {

         /**
         * Declaring the required variables
         *
         */
	static String serverAddress;
	static int serverNormalPort;
        static int serverTerminatePort;

        Socket clientNormalPortSocket;
        Socket clientTerminatePortSocket;

	String inputCommand;
        Object[] commandSplitArray = new Object[5];

        ObjectOutputStream outputStreamObj=null;
        ObjectInputStream inputStreamObj=null;

	ObjectOutputStream terminateOutputStreamObj=null;
        ObjectInputStream terminateiInputStreamObj=null;

        //Default Client's constructor
	FTP_Client_Main(String serverAddressParm, int serverNormalPortParm, int serverTerminatePortParm) {

            try{
		serverAddress = serverAddressParm;
		serverNormalPort = serverNormalPortParm;
                serverTerminatePort = serverTerminatePortParm;

                this.clientNormalPortSocket = new Socket(this.serverAddress,this.serverNormalPort);
                this.clientTerminatePortSocket = new Socket(this.serverAddress,this.serverTerminatePort);

            }catch(Exception e){
                e.printStackTrace();
            }


	}


         /**
         * Main function
         */
	/*public static void main(String[] args) {

            try
            {

                FTP_Client_Main clientObject = new FTP_Client_Main(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));
		

                //while(true){

                    //A client has connected to the Server on the normal port
                  //  Thread normalThread = new Thread(new FTP_Client(clientObject.serverAddress,clientObject.serverNormalPort,clientObject.serverTerminatePort));
                   // normalThread.start();
                    

                //}

            } catch (Exception e){
                e.printStackTrace();
            }


	}*/


         /**
         * Main function
         */
	public static void main(String[] args) {

            try
            {
		FTP_Client client=new FTP_Client(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));


                //Connect to the Server on Normal Port
                client.clientNormalPortSocket = new Socket(client.serverAddress,client.serverNormalPort);

                client.clientTerminatePortSocket = new Socket(client.serverAddress,client.serverTerminatePort);
                System.out.println("Connecting to the server!!!!");

                while(true){
                    client.displayCommandsToUser();
                    client.readCommandFromUser();

                    if(client.commandSplitArray[0].toString().equalsIgnoreCase("quit")){

                          client.clientNormalPortSocket.close();
                          client.clientTerminatePortSocket.close();
                          client.printStream("Disconnected from the Server.....",true);
                          System.exit(0);

                    }else if(client.commandSplitArray[4]!=null && client.commandSplitArray[4]=="&"){
                        Thread testThread = new Thread(client);
                        testThread.start();
                        
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