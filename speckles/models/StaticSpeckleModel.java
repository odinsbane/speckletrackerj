package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.utils.SavitzyGolayFilter;

import java.util.HashSet;

/**
 *  This is merely a convenience class to keep track of the model being used
 *  since an array will get confusing.
 * */
public class StaticSpeckleModel extends SpeckleModel{
    static final int SINGLE = 0;
    static final int MULTI = 1;
    
    public double DX,DY;
    
    public int TYPE;    
    
    StaticSpeckleModel(){
        TYPE = SINGLE;

    }
    
    /**
     *  Initiates a specklemodel factory for generating speckle
     * 
     **/
    public StaticSpeckleModel(ImagePlus imp){
        this();
        implus = imp;
    }
    
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one speckle.
     * */
    public SpeckleModel createModel(Speckle s){
        return new StaticSpeckleModel(implus);
        
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
     *  state information can be pass and anything can be updated/manipulated
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
        double[] pt = speck.getLastCoordinates();
        HashSet<Integer> frames = new HashSet<Integer>();
        for(Integer x: speck){
            frames.add(x);
        }
        for(int i = 1; i<= SpeckleApp.getSlices(implus); i++){
            
            if(!frames.contains(i))
                speck.setWeights(new double[] {0.25, 0.25}, new double[] {pt[0], pt[1]}, i);
            
        }
        speck.end();
        

    }
    
    private void showSpecklePlots(SpeckleEstimator speck){
        double[] values = new double[SpeckleApp.getSlices(implus)];
        double[] frames = new double[SpeckleApp.getSlices(implus)];
        
        ImageStack istack = implus.getStack();
        ImageProcessor ip;
        double value;
        int[] vals = {0,1};
        int[] ipt = new int[2];
        double[] pt;
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
            values[i-1] = value;
            frames[i-1] = i;
            
        }
        
        Plot plotter = new Plot("intensity", "frame", "intensity", frames, values);
        
        SavitzyGolayFilter f = new SavitzyGolayFilter(8,8,4);
        
        double[] nv = f.filterData(values);
        
        plotter.addPoints( frames, nv, plotter.LINE);
        
        plotter.show();
        
        
    }
    public int modelType(){
        return EXTENDING_MODEL;
    }
        
    }
