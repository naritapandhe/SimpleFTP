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
    static int serverPort;
    Socket clientSocket;
    static ServerSocket serverSocket;
    String clientCommand;
    Object clientParams;
    Object clientFileContents;
    ObjectOutputStream clientOutputObj = null;
    ObjectInputStream inputStreamObj = null;
    Thread testThread = null;


    //Default Constructor
    FTP_Server_Main(int serverPortNo) {

        try {


            serverPort = serverPortNo;
            serverSocket = new ServerSocket(serverPort);

            System.out.println("Server Running!!!!");
            System.out.println("Please connect to the Server from Client using " + serverPort + " port.");


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void main(String args[]){

        FTP_Server_Main serverObject = new FTP_Server_Main(Integer.parseInt(args[0]));

        try {
            while (true) {

                serverObject.clientSocket = serverSocket.accept();

                System.out.println("Client accepted: " + serverObject.clientSocket);

                //A client has connected to this server. Send welcome message
                Thread thread = new Thread(new FTP_Server(serverObject.clientSocket));
                thread.start();


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
