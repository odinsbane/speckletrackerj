package speckles.models;

import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleCalculator;
import speckles.SpeckleDetector;

import java.util.HashSet;

/**
 * Model for adjusting the position of speckles.
 * 
 * User: mbs207
 * Date: Nov 23, 2010
 * Time: 2:35:21 PM
 */
public class AdjustModel extends SpeckleModel{
    @Override
    public SpeckleModel createModel(Speckle s) {
        return this;
    }

    @Override
    public SpeckleModel createModel(HashSet<Speckle> specks) {
        return this;
    }

    @Override
    public void estimateLocation(SpeckleEstimator speck, int frame) {

        for(int i: speck){
            ImageProcessor ip = implus.getStack().getProcessor(i);
            double[] w = speck.getCoordinates(i);
            double[] npt = SpeckleDetector.refinePt(w,2,ip);
            double d = SpeckleCalculator.averageValueCircle(npt,SpeckleCalculator.INNER_RADIUS,ip);
            speck.setWeights(new double[]{1,d},npt,i);
        }
        speck.end();

    }

    /**
     *  This will not parse any speckles and it will assume that all of the to_track are
     *  acceptable.
     *
     * @param to_track set of speckles for tracking.
     * @param not_tracking these speckles will be accounted for but not tracked.
     * @param frame - current frame.
     */
    public void prepareSpeckles(HashSet<Speckle> to_track,HashSet<Speckle> not_tracking, int frame ){
        return;
    }

    public int modelType(){
        return REFINE_MODEL;
    }
}
