package speckles;

import java.text.MessageFormat;
import java.util.NoSuchElementException;

/**
 * New imagej plugin that ...
 * User: mbs207
 * Date: Dec 1, 2010
 * Time: 11:05:42 AM
 */
public enum SpeckleParameters {
    innerRadius("Inner Radius (px)"),
    outerRadius("Outer Radius (px)"),
    correlationTemplate("NCC Template size - odd number (px)"),
    proximityValue("Minimum Track Separation (px)"),
    startingFrame("Batch Locate Start Frame"),
    endingFrame("Batch Locate Ending Frame"),
    thresholdValue("Batch Locate Threshold Value"),
    sizeValue("Batch Locate Minimum Detection Size (px^2)"),
    fusionSwitch("Batch Locate Only Fusion Events (on:1/off:0)"),
    minimumDuration("Batch Locate Minimum Duration (frames)"),
    linearFit("Batch Locate Fit Tracks to Line (on:1/off:0)"),
    maxMeanDisplacement("Batch Locate Maximum Mean Displacement (px)"),
    linkFrames("Batch Locate Link Frames Distance (frames)");

    String title;
    SpeckleParameters(String t){
        title = t;
    }
    public String toString(){
        return title;
    }

    public static SpeckleParameters fromTitle(String t){

        for(SpeckleParameters s: values()){

            if(s.title.equals(t)){
                return s;
            }

        }

        throw new NoSuchElementException(MessageFormat.format("{0} is not a valid title.", t));
    }

}
