package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import static speckles.models.DiffusingSpotsModel.*;

/**
 * This is a subset of the diffusive intensity models.  This one goes backwards.
 * User: mbs207
 * Date: Sep 20, 2010
 * Time: 10:07:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class DiffusingSpotsBackwardsModel extends SpeckleModel{
     public double DS;

    double AVERAGE_INTENSITY;
    double AVERAGE_CHANGE;
    int DISPLACEMENT_COUNT = 0;

    double IMAGE_MEAN;

    public DiffusingSpotsBackwardsModel(ImagePlus imp){
        implus = imp;
        IMAGE_MEAN = imp.getStatistics().mean;
    }

    public DiffusingSpotsBackwardsModel(){
        
    }
    /*
     * Uses the speckle to find the average intensity, and the change in intensity
     *
     * */
    public SpeckleModel createModel(Speckle s){
        int tally = 0;
        int dtally = 0;
        ImageStack istack = implus.getStack();
        double value = 0;
        double deltas = 0;

        boolean first = true;
        double old_val=0;
        double new_val;
        double[] pt;
        double[] opt = null;
        DS = 0;
        for(Integer i: s){

            pt = s.getCoordinates(i);
            new_val = SpeckleCalculator.averageValueCircle(s.getCoordinates(i), SpeckleCalculator.INNER_RADIUS, istack.getProcessor(i));

            value += new_val;
            tally++;

            if(first){
                first=false;
            } else{
                deltas += Math.pow(new_val - old_val, 2);
                DS += squareDisplacement(pt, opt);
                dtally++;
            }
            opt = pt;
            old_val = new_val;


        }
        DiffusingSpotsBackwardsModel x = new DiffusingSpotsBackwardsModel(implus);
        x.AVERAGE_INTENSITY = value/tally;

        if(dtally>0){
            x.AVERAGE_CHANGE = deltas/dtally;
            x.DS = DS/dtally;
            x.DISPLACEMENT_COUNT = dtally;
        }else
            x.AVERAGE_CHANGE = 0;
            x.DS = 0;

        return x;
    }

    /*
     *
     * */
    public SpeckleModel createModel(HashSet<Speckle> specks){

        int tally = 0;
        int dtally = 0;
        ImageStack istack = implus.getStack();
        double value = 0;
        double deltas = 0;

        double old_val=0;
        double new_val;


        double[] pt;
        double[] opt=null;
        DS = 0;

        for(Speckle s: specks){
            boolean first = true;
            for(Integer i: s){

                pt = s.getCoordinates(i);
                new_val = SpeckleCalculator.averageValueCircle(s.getCoordinates(i), SpeckleCalculator.INNER_RADIUS, istack.getProcessor(i));

                value += new_val;
                tally++;

                if(first){
                    first=false;
                } else{
                    deltas += Math.pow(new_val - old_val, 2);
                    DS += squareDisplacement(pt,opt);
                    dtally++;
                }

                opt = pt;
                old_val = new_val;


            }
        }

        DiffusingSpotsBackwardsModel x = new DiffusingSpotsBackwardsModel(implus);
        x.AVERAGE_INTENSITY = value/tally;

        if(dtally>0){
            x.AVERAGE_CHANGE = deltas/dtally;
            x.DS = DS/dtally;
            x.DISPLACEMENT_COUNT = dtally;
        }else
            x.AVERAGE_CHANGE = 0;
            x.DS = 0;

        return x;
    }

    static double squareDisplacement(double[] p1, double[] p2){

        return Math.pow(p1[0] - p2[0],2) + Math.pow(p1[1] - p2[1], 2);
    }
    
    @Override
    public void estimateLocation(SpeckleEstimator speck,int f){
        int frame = speck.getFirstFrame();
        while( frame>1 ){
            frame--;
            ArrayList<double[]> pts = predictSpeckle(speck, frame);

            //
            if(pts.size()==0){
                speck.end();
                return;

            }
            double[] best = new double[]{0,0,0};
            double[] best_pt = new double[]{0,0};
            double champ = 0;

            for( double[] pt: pts){

                ImageProcessor ip = implus.getStack().getProcessor(frame);


                double p = SpeckleCalculator.averageValueCircle(pt, SpeckleCalculator.INNER_RADIUS,ip);
                double displacement = DiffusingSpotsModel.rootDisplacement(pt, speck.getCoordinates(frame + 1));
                double delta = p - SpeckleCalculator.averageValueCircle(speck.getCoordinates(frame+1), 1,implus.getStack().getProcessor(frame+1));

                double[] w = modelCriteria(p,delta,displacement);
                if(w[0] > champ){
                    best = w;
                    champ=w[2];
                    best_pt[0] = pt[0];
                    best_pt[1] = pt[1];
                }

            }
            speck.setWeights(best,best_pt,frame);
            if(test(best)){
                speck.success();
            } else{
                speck.fail(frame);

                if(speck.getFail()>2){
                    speck.end();
                    return;
                }
            }
        }
        
        speck.end();

    }

    /**
     * Predict a series of points where a speckle could be.
     *
     * @param speck
     * @param n
     * @return
     */
    public ArrayList<double[]>  predictSpeckle(SpeckleEstimator speck, int n){

        //original
        double[] opt = speck.getCoordinates(n+1);
        //set new points
        int[] pt = new int[]{(int)opt[0], (int)opt[1]};


        ImageStack is = implus.getStack();


        ImageProcessor next = is.getProcessor(n);
        ArrayList<double[]> maxima = new ArrayList<double[]>();
        //find all of the maximum points
        for(int i = -SEARCHSIZE; i<=SEARCHSIZE; i++){
            for(int j = -SEARCHSIZE; j<=SEARCHSIZE; j++){

                if(DiffusingSpotsModel.isPtLocalMax(next, pt[0] + i, pt[1] + j)){
                    double mx = 0;
                    double my = 0;
                    double sum = 0;

                    for(int ii = -1; ii<=1; ii++){
                        for(int jj = -1; jj<=1; jj++){
                            double p = next.getPixelValue(pt[0] + ii + i,pt[1] + jj + j);
                            mx += ii*p;
                            my += jj*p;
                            sum += p;
                        }
                    }
                    maxima.add(new double[]{pt[0] + mx/sum + i, pt[1] + my/sum + j});

                }



            }
        }




        //return new double[]{x  + mx/sum,y + my/sum};
        return maxima;

    }

     /**
     * Model criteria creates the weights value for the estimators and the ordering of the
     *
     * @param value average intensity
     * @param diff change between this frame and previous frame.
     * @param displacement between current frame and previous frame.
     * @return probability, ordering, number
     */
    public double[] modelCriteria(double value, double diff, double displacement){
        double[] results = new double[3];
        double diff_squared = Math.pow(diff,2);
        double x = AVERAGE_INTENSITY - value;
        double y = AVERAGE_INTENSITY - IMAGE_MEAN;
        x = x>0?x : 0;

        double intensity_probability = Math.exp(-(x/y));
        double displacement_probability = DS>0?Math.exp(-Math.pow(displacement,2)/(2*DS)):1/(1+displacement);
        double change_probability = AVERAGE_CHANGE>0?Math.exp(-diff_squared/(2*AVERAGE_CHANGE)):1/(1 + diff_squared);


        double main = intensity_probability* f_I + displacement_probability* f_d + change_probability* f_deltaI;
        results[0] = main>1?1:main;
        results[1] = results[0];
        results[2] = (value - IMAGE_MEAN)/(AVERAGE_INTENSITY - IMAGE_MEAN);

        return results;
    }    

    public boolean test(double[] w){

        return w[0]>w_min;
        
    }

    /**
     *  This will parse the current speckles and decide if they should be tracked.
     *  For the default implementation it only leaves speckles that exist and end
     *  in the current frame.
     *
     * @param to_track set of speckles for tracking.
     * @param not_tracking these speckles will be accounted for but not tracked.
     * @param frame - current frame.
     */
    public void prepareSpeckles(HashSet<Speckle> to_track,HashSet<Speckle> not_tracking, int frame ){
        Iterator<Speckle> iter = to_track.iterator();
        while(iter.hasNext()){
            Speckle s = iter.next();
            if (s.exists(frame) && s.getFirstFrame() == frame) {
                continue;
            }
            not_tracking.add(s);
            iter.remove();

        }

    }

    /**
     * Changes the intensity model's parameters.
     *
     * @return a hashmap for storing parameters.
     */
    public HashMap<String,Double> getParameters(){
        return new DiffusingSpotsModel().getParameters();
    }

    /**
     * Sets the DiffusingSpotsModel's parameters
     * @param params
     *
     */
    public void setParameters(HashMap<String,Double> params){
        new DiffusingSpotsModel().setParameters(params);
    }

    public int modelType(){
        return EXTENDING_MODEL;
    }

}
