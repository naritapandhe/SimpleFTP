/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package FTPClientServer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * @author admin
 */
public class FTP_Server_Main {
     /**
     * Declaring the required variables
     *
     */
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
    int MAX_THREAD_COUNT=100;

    FTP_Server ftpServerObj=null;
    


    //Default Constructor
    FTP_Server_Main(int nPort, int tPort) {

        try {

            
            this.normalPort = nPort;
            this.terminatePort=tPort;
            this.serverNormalPortSocket = new ServerSocket(this.normalPort);
            System.out.println("To execute normal commands, please connect to the Server from Client using " + this.normalPort + " port.");

            this.terminatePort=tPort;
            this.serverTerminatePortSocket=new ServerSocket(this.terminatePort);
            System.out.println("To terminate execution of commands, please connect to the terminate port: " + this.terminatePort );
            

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    /**
    * Main function
    */
    public static void main(String[] args) {

            int currentThreadCount=0;
            try
            {
                FTP_Server_Main ftpMain=new FTP_Server_Main(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
		FTP_Server serverObject;
                while (true) {

                    serverObject=new FTP_Server(ftpMain.serverNormalPortSocket.accept(),ftpMain.serverTerminatePortSocket.accept());
                    Thread t=new Thread(serverObject);
                    serverObject.currentThreadID=Long.toString(t.getId());
                    t.start();;
                    
                   
                }
               

            } catch (Exception e){
                e.printStackTrace();
            }


	}

    
}
