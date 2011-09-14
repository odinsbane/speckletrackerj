package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleDetector;

import java.util.HashSet;

/**
 *  This is how the speckle evolves.
 * */
public class StaticDriftModel extends SpeckleModel{
    static final int SINGLE = 0;
    static final int MULTI = 1;
    

    public int TYPE;

    double[] PT;
    
    
    StaticDriftModel(){
        TYPE = SINGLE;

    }
    
    /**
     *  Initiates a specklemodel factory for generating speckle
     * 
     **/
    public StaticDriftModel(ImagePlus imp){
        this();
        implus = imp;
    }
    
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one speckle.
     * */
    public SpeckleModel createModel(Speckle s){
        StaticDriftModel x = new StaticDriftModel(implus);
        x.PT = s.getCoordinates(s.getLastFrame());
        return x;
        
    }
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one all of the speckles in the group.
     * */
    public SpeckleModel createModel(HashSet<Speckle> specks){
        StaticSpeckleModel x = new StaticSpeckleModel(implus);
        x.TYPE=MULTI;
        return x;
    }
    
    /** Find the next This is how speckles will be tracked.  By using a speckle estimator
     *  state information can pass and anything can be updated/manipulated
     * via the model.  The requirements of this function are to provide the pass
     * and fails based on model criteria, and the first two elements of the 
     * SpeckleEstimator weights (double[]) have to be certainty and probability
     * values.
     * 
     * @param speck estimator of current speckle
     * @param frame current frame
     * 
     **/
      
    public void estimateLocation(SpeckleEstimator speck,int frame){
        double[] pt;
        HashSet<Integer> frames = new HashSet<Integer>();
        for(Integer x: speck){
            frames.add(x);
        }
        
        //add all of the frames before
        int start = speck.getFirstFrame();
        for(int i = start; i>1; i--){
            pt = speck.getCoordinates(i);
            double[] npt = SpeckleDetector.refinePt(pt, 1, implus.getStack().getProcessor(i - 1));
            speck.setWeights(new double[] {0.4, 0.4}, new double[] {npt[0], npt[1]}, i - 1);
            
        }
        double[] weights;
        int radius = 1;
        //add all of the frames after
        for(int i = start+1; i<= SpeckleApp.getSlices(implus);i++){
            
            if(!frames.contains(i)){
                pt = speck.getCoordinates(i-1);
                weights = new double[] {0.4,0.4};
            }else{
                pt = speck.getCoordinates(i);
                weights = new double[] {1,1};
            }
            double[] npt = SpeckleDetector.refinePt(pt, 1, implus.getStack().getProcessor(i));
            speck.setWeights(weights, new double[] {npt[0], npt[1]}, i);
        
            
        }
        weightedLeastSquares(speck);

        speck.end();

    }
    
    
    private void weightedLeastSquares(SpeckleEstimator speck){
        
        //get weights
        
        double[] weights = new double[speck.getSize()];
        double[] frames = new double[speck.getSize()];
        double[] xs = new double[speck.getSize()];
        double[] ys = new double[speck.getSize()];
        
        ImageStack istack = implus.getStack();
        ImageProcessor ip;
        
        double value;
        int[] vals = {-1,0,1};
        int[] ipt = new int[2];
        double[] pt;
        double max = 0;
        int dex = 0;
        
        double sum = 0;
        
        
        for(Integer i: speck){
            value = 0;
            pt = speck.getCoordinates(i);
            ipt[0] = (int)pt[0];
            ipt[1] = (int)pt[1];
            ip = istack.getProcessor(i);
            for(int x : vals){
                for(int y: vals){
                    
                    value += ip.getf(ipt[0]+x, ipt[1] + y);
                    
                    
                }
                
            }
            
                
            weights[dex] = Math.pow(value, 1)*speck.getWeight(i)[1];
            sum += weights[dex];    

            frames[dex] = i;
            xs[dex] = pt[0];
            ys[dex] = pt[1];
            
            dex++;
        }
        
        for(int i = 0; i<weights.length; i++){
            weights[i] = weights[i]/sum;
        }
        
        double[] slope_intercept = new double[2];
        
        double xm,xb, ym, yb;
        
        //fit x[] and y[] data sets
        
        slope_intercept = weightedLeastSquaresFit(frames, xs, weights);
        
        xm = slope_intercept[0];
        xb = slope_intercept[1];
        
        slope_intercept = weightedLeastSquaresFit(frames, ys, weights);
        
        ym = slope_intercept[0];
        yb = slope_intercept[1];
        
        
        //recreate the speckle positions
        for(int j = 0; j<frames.length; j++){
            
            speck.setWeights(new double[] { 0.5 , weights[j]}, new double[] { frames[j]*xm + xb, frames[j]*ym + yb}, (int)frames[j]);
            
            
        }
        
    }
    
    public static double[] weightedLeastSquaresFit(double[] x, double[] y, double[] weights){
        if(x.length!=y.length || y.length!=weights.length)
            throw new java.lang.IllegalArgumentException("all points must have the same length");
        
        double wx = 0, wx_sqd = 0, wxy = 0, wy = 0, weight=0;
        
        for(int i = 0; i<x.length; i++){
            wx += weights[i]*x[i];
            wy += weights[i]*y[i];
            wx_sqd += weights[i]*Math.pow(x[i],2);
            wxy += weights[i]*x[i]*y[i];
            weight += weights[i];
        }
        wx = wx/weight;
        wy = wy/weight;
        wx_sqd = wx_sqd/weight;
        wxy = wxy/weight;

        double m = (wxy - wx*wy)/(wx_sqd - Math.pow(wx,2));
        double b = wy - m*wx;
        
        return new double[] { m,b};
    }

    public int modelType(){
        return EXTENDING_MODEL;
    }
}
