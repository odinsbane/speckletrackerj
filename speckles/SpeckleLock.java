package speckles;

import java.util.ConcurrentModificationException;

/**
 * This class is a testament to my crap-style programming techniques.
 * User: mbs207
 * Date: Oct 14, 2010
 * Time: 1:31:37 PM
 */
public class SpeckleLock{
    private volatile boolean held = false;
    private volatile boolean run=true;

    /**
     * For checking checkning the intended state.  Long running models should
     * check this via the static method isStopped to allow a user to interrupt
     * the process.
     * 
     * @return the intended state of the process.
     */
    public boolean isInterrupted(){
        return !run;
    }

    /**
     * releases the lock and notifies any waiting actions.
     */
    synchronized void release(){
        held = false;
        notifyAll();
    }


    /**
     * This checks if AllSpeckles are being modified.  This prevents multiple tracking/modification
     * algorithms from occuring, it does not block but throws an exception if the lock is held.
     *
     **/
    synchronized void get(){
        if(held)
            throw new ConcurrentModificationException("Attempting to track speckles concurrently");
        held = true;
    }

    /**
     * Blocks until lock is released.  Should be an indicator to stop tracking/processing. For returning from
     * a process.  This will wait until the current process has released the lock/notified.
     */
    synchronized void interrupt(){
        if(held){
            run = false;
            try{
                wait();
            }catch(Exception e){
                //what
            }
        }
    }

}