/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package FTPClientServer;

/**
 *
 * @author admin
 */

public class ThreadDemo implements Runnable{
  Thread testThread;

  ThreadDemo(){
      testThread=new Thread(this,"runnable");
      System.out.println("Thread created!!"+testThread);
      testThread.start();
  }
  public void run(){
      try{
          for(int i=0;i<10;i++){
              System.out.println(i+" thread running");
              Thread.sleep(5000);
          }
          System.out.println("testThread finished!!");
      }catch(Exception e){
          e.printStackTrace();
      }
    
  }
  public static void main(String args[]){
     ThreadDemo obj=new ThreadDemo();

     try{
         while(obj.testThread.isAlive()){
             System.out.println("Main thread alive till child exits!!");
             Thread.sleep(10000);
         }

     }catch(InterruptedException e){
             System.out.println("Main thread interrupted");

     }catch (Exception e) {
         e.printStackTrace();
     }
     System.out.println("Main thread exited!!");
     
 }
}
