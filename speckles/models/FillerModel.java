package speckles.models;

import speckles.Speckle;

import java.util.HashSet;

/**
 * Connects discontinuous speckle tracks by linking them with a straight line between
 * the two marks surrounding the break.
 * 
 * User: mbs207
 * Date: 9/12/11
 * Time: 3:34 PM
 */
public class FillerModel extends SpeckleModel{
    @Override
    public SpeckleModel createModel(Speckle s) {
        return new FillerModel();
    }

    @Override
    public SpeckleModel createModel(HashSet<Speckle> specks) {
        return new FillerModel();
    }

    /**
     * Works one time, finds any discontinuities and fills them in with a new mark.
     * 
     * @param speck estimator of current speckle
     * @param frame current frame
     */
    @Override
    public void estimateLocation(SpeckleEstimator speck, int frame) {
        int start = speck.getFirstFrame();
        int end = speck.getLastFrame();


        for(int i = start; i<end; i++){
            if(!speck.exists(i)){
                int last = i-1;
                i++;

                //find next time speckle exists
                while(!speck.exists(i)){
                    i++;
                }
                int next = i;
                int distance = next - last;
                double[] first_point = speck.getCoordinates(last);
                double[] last_point = speck.getCoordinates(next);
                double ds = 1.0/distance;
                double dx = last_point[0] - first_point[0];
                double dy = last_point[1] - first_point[1];
                for(int j =1; j<distance; j++){
                    speck.setWeights(new double[]{1,1},new double[]{first_point[0] + dx*ds*j, first_point[1] + dy*ds*j}, last + j);
                }

            }
        }

        speck.end();
    }

    @Override
    public int modelType() {
        return SpeckleModel.REFINE_MODEL;
    }
}
