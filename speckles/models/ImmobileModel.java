package speckles.models;

import ij.ImagePlus;
import speckles.Speckle;
import speckles.SpeckleCalculator;
import speckles.SpeckleTracker;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This model is to be used for vesicles.  It will be exclusively threshold based and it will not even scan
 * the region nearby to find the best point.
 *
 *  
 * User: mbs207
 * Date: Sep 26, 2010
 * Time: 12:42:03 PM
 */
public class ImmobileModel extends SpeckleModel {
    static double MINIMUM_INTENSITY = 0;
    ImmobileModel(){

    }

    /**
     *  Initiates a specklemodel factory for generating speckle
     * @param imp has all of the image data that will be used do not modifiy.
     **/
    public ImmobileModel(ImagePlus imp){
        implus = imp;
    }


    @Override
    public SpeckleModel createModel(Speckle s) {
        ImmobileModel imod = new ImmobileModel(implus);

        return imod;
    }

    @Override
    public SpeckleModel createModel(HashSet<Speckle> specks) {
        ImmobileModel imod = new ImmobileModel(implus);


        return imod;
    }

    @Override
    public void estimateLocation(SpeckleEstimator speck, int frame) {
        if(frame>implus.getStack().getSize()){
            speck.end();
            return;

        }

        double[] pt = speck.getLastCoordinates();
        double v = SpeckleCalculator.averageValueCircle(pt, SpeckleCalculator.INNER_RADIUS, implus.getStack().getProcessor(frame));
        if(v>MINIMUM_INTENSITY){
            speck.setWeights(new double[]{1,1,1}, new double[]{pt[0],pt[1]},frame);
        }else {
            speck.fail(frame);
        }

        if(speck.getFail()> SpeckleTracker.MAX_FAIL)
            speck.end();
    }

    /**
     * For setting/changing model parameters.  One way to set parameters is to make static variables
     * in your model. Then create a map in this function with the appropriate
     *
     * @return map for the parameters
     */
    public HashMap<String,Double> getParameters(){
        HashMap<String, Double> map = new HashMap<String,Double>();
        map.put("Minimum Intensity",MINIMUM_INTENSITY);
        return map;
    }

    /**
     * let the model decide
     * @param params
     */
    public void setParameters(HashMap<String,Double> params){
        MINIMUM_INTENSITY = params.get("Minimum Intensity");
    }

    public int modelType(){
        return EXTENDING_MODEL;
    }

}
