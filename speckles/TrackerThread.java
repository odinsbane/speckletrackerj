package speckles;

import speckles.gui.TextWindow;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queues jobs for execution for tracking.
 * 
 * User: mbs207
 * Date: Sep 23, 2010
 * Time: 11:49:13 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 * This is a persistent that will be used
 */
class TrackerThread extends Thread{
    SpeckleApp parent;
    ConcurrentLinkedQueue<Runnable> jobs;
    TrackerThread(SpeckleApp sap){
        parent = sap;
        jobs = new ConcurrentLinkedQueue<Runnable>();
        setName("Tracker Thread "  + System.currentTimeMillis());
    }
    public void run(){
        try{
            for(;;){
                waitForIt();
                performAction();
            }
        }
        catch(Exception e){
            StringBuffer mess = new StringBuffer(getName() + '\n' + "message: " + e.getMessage());
            mess.append('\n');
            for(StackTraceElement ste: e.getStackTrace()){
                mess.append(ste);
                mess.append('\n');
            }
            TextWindow tw = new TextWindow("Tracker Thread Exception",mess.toString());
            EventQueue.invokeLater(tw);

            e.printStackTrace();
            parent.startTrackerThread();
        }
        parent.finishTracking();
    }

    synchronized private void waitForIt(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void performAction(){
        while(jobs.size()>0)
            jobs.poll().run();
    }

    synchronized public void submit(SpeckleTracker r){
        jobs.add(r);
        notify();
    }

    synchronized public void submit(Runnable r){
        jobs.add(r);
        notify();
    }
    synchronized public void finish(){
        jobs.add(new Runnable(){public void run(){parent.finishTracking();}});
        notify();
    }

}



