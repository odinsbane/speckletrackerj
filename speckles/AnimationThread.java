package speckles;

/**
 * Used for running through frames rapidly.
 *
 * 
 * User: mbs207
 * Date: Sep 23, 2010
 * Time: 11:46:50 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 *
 *
 */

class AnimationThread extends Thread{
    SpeckleApp parent;
    volatile boolean running=false;

    AnimationThread(SpeckleApp sap){
        parent = sap;
    }
    public void run(){
        try{
            for(;;){
                while(running){

                    performAnimation();

                }
                waitForIt();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void performAnimation() throws InterruptedException{
        Thread.sleep(30);
        parent.stepForward();
        if(parent.getCurrentSlice()==parent.getMaxSlice()){
            parent.setSlice(1);
        }
    }

    synchronized private void waitForIt(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void startAnimation(){
        notify();
        running=true;
    }

    synchronized public void stopAnimation(){
        running=false;
    }
}