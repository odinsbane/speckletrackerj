package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.utils.NormalizedCrossCorrelationFilter;

import java.util.HashSet;
/**
 *  This is how the speckle evolves.  The Refine model uses an intensity based speckle location for each frame, then it does a least
 * squares fit on the positions, finally it interpolates the points in between.
 * */
public class LinearRefineModel extends SpeckleModel{
    static final int SINGLE = 0;
    static final int MULTI = 1;
        
    public int TYPE;
    NormalizedCrossCorrelationFilter NCC;
    double[] PT;
    
    
    LinearRefineModel(){
        int N = 11;
        double PSF = 2;
        double[][] template = new double[N][N];
        for(int i = 0; i<N; i++){
            for(int j = 0; j<N; j++){

                template[i][j] = Math.exp(-(Math.pow(i - N/2, 2) + Math.pow(j - N/2, 2))/(2*Math.pow(PSF,2)));

            }
        }

        NCC = new NormalizedCrossCorrelationFilter(null);
        NCC.setTemplateSize(N);
        NCC.setTemplate(template);

        TYPE = SINGLE;
    }
    
    /**
     *  Initiates a specklemodel factory for generating speckle
     * 
     **/
    public LinearRefineModel(ImagePlus imp){
        this();
        implus = imp;
        

    }
    
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one speckle.
     * */
    public SpeckleModel createModel(Speckle s){
        LinearRefineModel x = new LinearRefineModel(implus);
        x.PT = s.getCoordinates(s.getLastFrame());
        return x;
        
    }
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one all of the speckles in the group.
     * */
    public SpeckleModel createModel(HashSet<Speckle> specks){
        LinearRefineModel x = new LinearRefineModel(implus);
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
      
    public SpeckleEstimator estimateLocation(SpeckleEstimator speck,int frame){
        double[] pt, npt;
        //add all of the frames before
        
        double[] weights;
        int radius = 1;
        //add all of the frames after
        for(Integer i: speck){
            
            pt = speck.getCoordinates(i);
            weights = new double[] {1,1};
            npt = refinePt(pt, 2, implus.getStack().getProcessor(i));
            speck.setWeights(weights, new double[] {npt[0], npt[1]}, i);
        
            
        }
        if(speck.getSize()>1)
            weightedLeastSquares(speck);

        speck.end();

        return speck;
    }
    
    public double[] refinePt(double[] xy, int radius, ImageProcessor ip){
        
        double max = ip.getInterpolatedPixel(xy[0], xy[1])*NCC.filter(ip,xy[0], xy[1]);
        
        double dx = 0;
        double dy = 0;
        
        for(int i = -radius; i<=radius; i++){
            for(int j = -radius; j<=radius; j++){
                double v = ip.getInterpolatedPixel(xy[0] + i, xy[1] + j)*NCC.filter(ip,xy[0] + i , xy[1] + j);
                if(v>max){
                    max=v;
                    dx = i;
                    dy = j;
                }
            
            }
        
        }
        
        
        return new double[] {xy[0] + dx, xy[1] + dy};
        
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
            if(ipt[0]>0&&ipt[1]>0&&ipt[0]<ip.getWidth()-1&&ipt[1]<ip.getHeight()-1){
                for(int x : vals){
                    for(int y: vals){

                        value += ip.getf(ipt[0]+x, ipt[1] + y);

                    
                    }
                
                }
            }//otherwise the weight is 0.
                
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
        
        double[] slope_intercept;
        
        double xm,xb, ym, yb;
        
        //fit x[] and y[] data sets
        
        slope_intercept = weightedLeastSquaresFit(frames, xs, weights);
        
        xm = slope_intercept[0];
        xb = slope_intercept[1];
        
        slope_intercept = weightedLeastSquaresFit(frames, ys, weights);
        
        ym = slope_intercept[0];
        yb = slope_intercept[1];
        
        
        //recreate the speckle positions
        /*
        for(int j = 0; j<frames.length; j++){
            
            speck.setWeights(new double[] { 0.5 , weights[j]}, new double[] { frames[j]*xm + xb, frames[j]*ym + yb}, (int)frames[j]);
            
            
        }
        */
        dex = 0;
        for(double i = frames[0]; i<=frames[frames.length - 1 ]; i++){

            speck.setWeights(new double[] { 0.5 , weights[dex]}, new double[] { i*xm + xb, i*ym + yb}, (int)i);
            if(frames[dex]==i)
                dex++;
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
            weight+= weights[i];
            
        }
        wx = wx/weight;
        wy = wy/weight;
        wx_sqd = wx_sqd/weight;
        wxy = wxy/weight;

        double m = (wxy - wx*wy)/(wx_sqd - Math.pow(wx,2));
        double b = wy - m*wx;
        
        return new double[] { m,b};
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
