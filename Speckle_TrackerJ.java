import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import speckles.SpeckleApp;

/**
 * This speckle plugin is for accumulating pieces of images from before a speckle appearance

 */

public class Speckle_TrackerJ implements PlugInFilter{
    ImagePlus implus;
    public int setup(String args, ImagePlus implus){
        this.implus = implus;
        return DOES_ALL;
    }
    public void run (ImageProcessor imp){
        SpeckleApp myapp = new SpeckleApp(implus,false);
    }

}


