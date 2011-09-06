package speckles.controls;

import speckles.SpeckleApp;
import speckles.gui.TextWindow;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
  *     The speckle worker is similar to a swing worker, it performs an action and then provides a callback to the 
  *     controls which started it.
  **/
public class SpeckleWorker extends Thread {
    
    private final SpeckleApp parent;
    private SpeckleCommands cmd;
    private final SpeckleControls ctl;
    ConcurrentLinkedQueue<SpeckleCommands> commands;

  public SpeckleWorker ( SpeckleApp parent, SpeckleControls ctl){
        this.parent = parent;
        SpeckleCommands trans;
        commands = new ConcurrentLinkedQueue<SpeckleCommands>();

        this.ctl = ctl;

        setName("Worker Thread " + System.currentTimeMillis());
  }
  @Override
  public void run(){
        try{
            for(;;){
                waitForIt();
                while(commands.size()>0){
                    cmd = commands.poll();
                    chooseAction();

                }
                ctl.actionFinished();
            }
        }
        catch(Exception e){
            StringBuffer mess = new StringBuffer(getName() + '\n' + "message: " + e.getMessage());
            mess.append('\n');
            for(StackTraceElement ste: e.getStackTrace()){
                mess.append(ste);
                mess.append('\n');
            }
            TextWindow tw = new TextWindow("Worker Thread Exception",mess.toString());
            EventQueue.invokeLater(tw);

            e.printStackTrace();
            ctl.startWorkerThread();
        }


  }

  synchronized private void waitForIt(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

  public synchronized void submitCommand(String command){
      SpeckleCommands trans;
        try{
            trans = SpeckleCommands.valueOf(command);
        } catch(java.lang.IllegalArgumentException e){
            System.out.println("Command not found: " + command);
            trans = SpeckleCommands.nothing;
        }
      if(trans==null)
        return;

      commands.add(trans);
      notify();
  }


  public void chooseAction(){
    switch(cmd){
        case forward:
            parent.stepForward();
            break;
        case backward:
            parent.stepBackward();
            break;
        case createdistribution:
           parent.createDistribution();
           break;
        case copyprevious:
            parent.copyPreviousSpeckles();
            break;
        case clearspeckles:
            parent.clearSpeckles();
            break;
        case savespeckles:
            parent.saveSpeckles();
            break;
        case loadspeckles:
            parent.loadSpeckles();
            break;
        case newspeckles:
            parent.newSpeckles();
            break;
        case thresholdlocate:
            parent.thresholdLocate();
            break;
        case maxlocate:
            parent.maxLocate();
            break;
        case maxlocatespeckle:
            parent.maxLocateSpeckle();
            break;
        case correlatelocate:
            parent.correlateLocate();
            break;
        case sliderupdate:
            parent.sliderUpdate();
            break;
        case trackspeckle:
            parent.trackSpeckle();
            break;
        case merge:
            parent.mergeSpeckles();
            break;
        case cancel:
            parent.endActions();
            break;
        case autotrack:
            parent.autoTrack();
            break;
        case showspeckle:
            parent.showSpeckleAllFrames();
            break;
        case autotrackall:
            parent.autoTrackAll();
            break;
        case batchlocate:
            parent.startBatchLocate();
            break;
        case trimbefore:
            parent.trimBefore();
            break;
        case trimafter:
            parent.trimAfter();
            break;
        case startimagej:
            parent.startImageJ();
            break;
        case toend:
            parent.toEnd();
            break;
        case tobeginning:
            parent.toBeginning();
            break;
        case zoomin:
            parent.zoomIn();
            break;
        case zoomout:
            parent.zoomOut();
            break;
        case modelchanged:
            parent.modelChanged();
            break;
        case version:
            parent.showVersion();
            break;
        case updateselector:
            parent.updateSelectorButton();
            break;
        case toggleshape:
            parent.toggleSpeckleShape();
            break;
        case play:
            parent.startAnimation();
            break;
        case setselection:
            parent.startSelection();
            break;
        case clearselection:
            parent.clearSelection();
            break;
        case adjustparameters:
            parent.changeParameters();
            break;
        case updatereslice:
            parent.showResliceControl();
            break;
        case copyselector:
            parent.copySelectorToTextWindow();
            break;
        case loadimage:
            parent.loadImage();
            break;
        case findbroken:
            parent.findBroken();
            break;
        case splitspeckle:
            parent.splitSpeckle();
            break;
        case appendspeckles:
            parent.appendSpeckles();
            break;
        case measurespeckles:
            parent.measureSpeckles();
            break;
        default:
            System.out.println("Not Implemented");
        }
  }

}

enum SpeckleCommands{
    loadimage(),
    forward(),
    backward(),
    createdistribution(),
    copyprevious(),
    clearspeckles(),
    loadspeckles(),
    newspeckles(),
    savespeckles(),
    thresholdlocate(),
    correlatelocate(),
    maxlocate(),
    maxlocatespeckle(),
    sliderupdate(),
    trackspeckle(),
    merge(),
    nothing(),
    autotrack(),
    showspeckle(),
    cancel(),
    autotrackall(),
    batchlocate(),
    trimbefore(),
    trimafter(),
    startimagej(),
    toend(),
    tobeginning(),
    zoomin(),
    zoomout(),
    modelchanged(),
    version(),
    updateselector(),
    toggleshape,
    play,
    setselection,
    clearselection,
    adjustparameters,
    copyselector,
    updatereslice,
    findbroken,
    splitspeckle,
    appendspeckles,
    measurespeckles;
    SpeckleCommands(){
    }
}
