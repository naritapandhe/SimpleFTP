/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package FTPClientServer;

/**
 *
 * @author admin
 */
public class ThreadDemo2 extends Thread {
    ThreadDemo2(){
        super();
        System.out.println("Constructor");
        start();

    }
    public void run(){

        try
     {
        for (int i=0 ;i<=10;i++)
        {
           System.out.println("Printing the count " + i);
           Thread.sleep(1000);
        }
     }
     catch(InterruptedException e)
     {
        System.out.println("ThreadDemo2 interrupted");
     }
     System.out.println("ThreadDemo2 is over" );
    }

    public static void main(String args[]){
        ThreadDemo2 testObj=new ThreadDemo2();
        try{
            while(testObj.isAlive()){
                System.out.println("Main thread will be alive till the child thread is live");
                Thread.sleep(1500);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("Main thread's run is over");
    }

}
