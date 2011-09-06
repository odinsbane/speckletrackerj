package speckles.models;

import speckles.Speckle;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *  Used to keep track of the probabiity of a speckle occuring at a particular frame.
 * Keeps track of weights determined from the speckle model for comparing speckle intersections
 * and speckle persistence across multiple frames.
 * 
 * */

public class SpeckleEstimator implements Iterable<Integer>{
    static final int MAX_FAIL = 2;
    
    TreeMap<Integer, double[][]> POSITIONS;
    //TreeMap<Integer, double[]> WEIGHTS;
    TreeSet<Integer> FAILS;

    boolean working;
    
    private Speckle progeny;
    
    public SpeckleEstimator(){
        
    }
    /**
     *
     * Creates a speckle estimator for the speckle with both weights set at 1.
     * This speckle estimator can be exposed to models, but the models cannot
     * explicitely modify the speckle.  The reason is for the state tracking 
     * routines.
     * 
     * 
     * @param s - speckle that will be backed.    
     **/
    public SpeckleEstimator(Speckle s){
        
        POSITIONS = new TreeMap<Integer, double[][]>();
        //WEIGHTS = new TreeMap<Integer, double[]>();
        FAILS = new TreeSet<Integer>();
        working = true;
        progeny = s;

        for(Integer i: s){

            double[] xy = s.getCoordinates(i);
            double[] w = {1, 1, 1};
            setWeights(w,xy,i);

        }

    }
    
    /** 
     * Use this to add a point or to update a current point with a new
     * value the weight needs to describe the probability that a speckle
     * exists, a ranking value for comparing to other speckles that might be
     * nearby and
     * 
     * @param weight - {probability, ordering, number, ... }
     * @param xy
     * @param frame
     */
    public void setWeights(double[] weight, double[] xy, int frame){
        
        POSITIONS.put(frame,new double[][]{xy,weight});
        //WEIGHTS.put(frame,weight);
        
    }
    
    public double[] getCoordinates(int frame){
        
        return POSITIONS.get(frame)[0];
                
    }
    
    public double[] getWeight(int frame){
        //return WEIGHTS.get(frame);
        if(POSITIONS.containsKey(frame))
            return POSITIONS.get(frame)[1];

        return null;

    }

    /**
     *  checks for exists
     * @param frame frame under consideration.
     * @return true if the estimator has coordinates for the frame.
     */
    public boolean exists(int frame){
        return POSITIONS.containsKey(frame);
    }
    /** immediately terminates the estimator */
    public void end(){
        working = false;
        //remove all previous failed entries
        for(Integer i: FAILS){
            if(POSITIONS.containsKey(i))
                POSITIONS.remove(i);
        }
    }
    
    public int getLastFrame(){
        return POSITIONS.lastKey();
    }
    
    public double[] getLastCoordinates(){
        return POSITIONS.get(getLastFrame())[0];
    }
    
    public int getFirstFrame(){
        return POSITIONS.firstKey();
    }
    public boolean isWorking(){
        return working;
    }
    
    /**
     *  this is last.  When finished update the speckle.    
     **/
    public void updateSpeckle(){
        double[] pt;
        for(Integer i : this){
            pt = POSITIONS.get(i)[0];
            progeny.addPoint(pt[0], pt[1], i);
        }
        POSITIONS.clear();
    }
    
    public int getFail(){
        return FAILS.size();
    }
    
    public void fail( int frame){
        FAILS.add(frame);
    }

    public boolean failed(int frame){

        return FAILS.contains(frame);

    }

    public void removeSpeckle(){

        working = false;
        progeny.clear();
        POSITIONS.clear();
        FAILS.clear();




    }
    
    public void success(){
        FAILS.clear();
    }
    
    public int getSize(){
        
        return POSITIONS.size();
        
    }
    public Iterator<Integer> iterator(){
        return POSITIONS.keySet().iterator();
    }
    
    public Speckle getSpeckle(){
        return progeny;
    }
    

}
