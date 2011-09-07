package speckles;

import javax.sound.midi.*;
import java.io.IOException;

/**
 * Used for making noises, when speckles are added and when tracking is finished.
 *
 * Created by IntelliJ IDEA.
 * User: mbs207
 * Date: Sep 23, 2010
 * Time: 11:48:10 AM
 */
class SpeckleNoises implements Runnable{
    MidiChannel x;
    static int root = 65;
    final static int ADD = 0;
    final static int AUTOTRACK = 1;
    int SOUND=0;
    SpeckleNoises(){

        try{
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();

            Soundbank sb = MidiSystem.getSoundbank(getClass().getResourceAsStream("/soundbank.gm"));
            Instrument ins = sb.getInstruments()[408];

            synth.loadInstrument(ins);

            MidiChannel[] channels = synth.getChannels();

            x = channels[0];
            x.programChange(ins.getPatch().getProgram());

            //x.setOmni(true);


        }catch(javax.sound.midi.MidiUnavailableException e){
            System.out.println("No sounds are Available, remaining mute.");
        } catch (InvalidMidiDataException e) {
            System.out.println("No sounds are Available, remaining mute.");

        } catch (IOException e) {
            System.out.println("Unable to read sound bank. remaining mute.");
        } catch (NullPointerException e){
            System.out.println("sound bank not found. remaining mute");
        }
        if(x!=null){
            new Thread(this).start();
        }
    }
    public void run(){

        for(;;){
            waitForIt();
            playSound();

        }

    }

    synchronized public void requestSound(int type){
        SOUND=type;
        notify();
    }

    private void playSound(){
        switch(SOUND){
            case ADD:
                addSound();
                break;
            case AUTOTRACK:
                autoSound();
                break;
        }

    }
    private void addSound(){
            try{
                x.noteOn(root-2,127);
                Thread.sleep(150);
                x.noteOff(root-2,50);

            } catch(Exception e){
                    e.printStackTrace();
            }

    }

    private void autoSound(){

        try{
                x.noteOn(root,127);
                Thread.sleep(200);
                x.noteOff(root,50);
                x.noteOn(root-1,127);
                Thread.sleep(200);
                x.noteOff(root-1,50);
                x.noteOn(root,127);
                Thread.sleep(200);
                x.noteOff(root,10);

                x.noteOn(root+1,127);
                Thread.sleep(500);
                x.noteOff(root+1,10);
                //Thread.sleep(100);
            } catch(Exception e){
                    e.printStackTrace();
            }

    }
    synchronized void waitForIt(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
