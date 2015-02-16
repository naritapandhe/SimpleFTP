/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package FTPClientServer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author admin
 */
public class FTP_Server_Main extends Thread {
     /**
     * Declaring the required variables
     *
     */
    static int normalPort;
    static int terminatePort;

    Socket clientNormalPortSocket;
    Socket clientTerminatePortSocket;

    static ServerSocket serverNormalPortSocket;
    static ServerSocket serverTerminatePortSocket;

    String clientCommand;
    Object clientParams;
    Object clientFileContents;
    ObjectOutputStream clientOutputObj = null;
    ObjectInputStream inputStreamObj = null;
    Thread testThread = null;


    //Default Constructor
    FTP_Server_Main(int nPort, int tPort) {

        try {

            System.out.println("Server Running!!!!");

            normalPort = nPort;
            serverNormalPortSocket = new ServerSocket(normalPort);
            System.out.println("To execute normal commands, please connect to the Server from Client using " + normalPort + " port.");

            terminatePort=tPort;
            serverTerminatePortSocket=new ServerSocket(terminatePort);
            System.out.println("To terminate execution of commands, please connect to the terminate port: " + terminatePort );


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void main(String args[]){

        FTP_Server_Main serverObject = new FTP_Server_Main(Integer.parseInt(args[0]),Integer.parseInt(args[1]));

        try {
            while (true) {

                //A client has connected to the Server on the normal port
                serverObject.clientNormalPortSocket = serverNormalPortSocket.accept();
                serverObject.clientTerminatePortSocket = serverTerminatePortSocket.accept();
                Thread normalThread = new Thread(new FTP_Server(serverObject.clientNormalPortSocket,serverObject.clientTerminatePortSocket));
                normalThread.start();
                System.out.println("Client connection accepted on normal port: " + serverObject.clientNormalPortSocket);
                System.out.println("Client connection accepted on terminate port: " + serverObject.clientTerminatePortSocket);


               


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
