package speckles;

import java.awt.geom.Ellipse2D;
import java.util.Iterator;
import java.util.TreeMap;
/**
 *      Utility Class for storing speckle information, this does not
 *      store any tracking information only positions and a shape.
 *
 **/
 public class Speckle implements Iterable<Integer>{
    
    public static final int NORMAL_SPECKLE = 0;
    public static final int APPEARANCE_SPECKLE=1;
    public static final int DISAPPEARANCE_SPECKLE=2;
    public static final int PREVIOUS_SPECKLE = 3;
    public static final int RADIUS = 4;
    public static final double OFFSET = RADIUS - 0.5;
    
    Ellipse2D.Double shape;    
        
    TreeMap<Integer,Ellipse2D.Double> locations;

    /**
     * Creates a new speckle object without any coordiantes.
     */
    public Speckle(){
        
        locations = new TreeMap<Integer,Ellipse2D.Double>();

    }

    /**
     *  Create a new speckle with an exitsting point.
     *
     * @param x coordinate
     * @param y coordinate
     * @param frame time frame / image slice starts at 1 (as per imagej)
     */
    public Speckle(double x, double y, int frame){
        locations = new TreeMap<Integer,Ellipse2D.Double>();
        shape = new Ellipse2D.Double(x - OFFSET,y - OFFSET,2*RADIUS,2*RADIUS);
        
        locations.put(frame,shape);
    }


    /**
     *
     * @param frame imagej slice
     * @return coordinates for the specified frame / slice
     */
    public double[] getCoordinates(int frame){
        Ellipse2D.Double s = locations.get(frame);
        double x = s.getX() + OFFSET;
        double y = s.getY() + OFFSET;
        return new double[]{x,y};
    }

    /**
     * For a given frame, checks to see if the position is contained within the
     * speckle.
     * @param x ordinate
     * @param y ordinate
     * @param frame imagej slice
     * @return if the shape contains the point.
     */
    public boolean contains(double x, double y, int frame){
        try{
            shape = locations.get(frame);
            return shape.contains(x,y);
        } catch(Exception e){
            return false;
        }
    }

    /**
     * Adds a new point, overwrites existing points.
     *
     * @param x ordinate
     * @param y ordinate
     * @param frame imagej slice
     */
    public void addPoint(double x, double y, int frame){

        shape = new Ellipse2D.Double(x - OFFSET,y - OFFSET,2*RADIUS,2*RADIUS);
        locations.put(frame,shape);
        
    }

    /**
     * Removes the point if it exists in the supplied frame.
     * @param frame imagej slice
     */
    public void removePoint(int frame){
        Integer f = new Integer(frame);
        if(locations.containsKey(f))
            locations.remove(f);
    }

    /**
     * Whether this track exists in the supplied frame.
     *
     * @param frame imagej slice
     * @return test
     */
    public boolean exists(int frame){
        
        return locations.containsKey(frame);
    
    }

    /**
     * Removes all marks from this track.
     */
    public void clear(){
    
        locations.clear();
    }

    /**
     *
     * @return number of points in this speckle.
     */
    public int getSize(){
        return locations.size();
    }

    /**
     * For iterating through all frames that this speckle exists in.
     *
     * @return iterator of the keyset for the locations map.
     */
    public Iterator<Integer> iterator(){
        return locations.keySet().iterator();
    }

    /**
     * For drawing, or something...probably to be depracated.
     *
     * @param frame
     * @return
     */
    public Ellipse2D.Double getShape(int frame){
        
        return locations.get(frame);
    
    }
    
    /**
     * 
     * Gets the last frame this speckle exists in, this is not the last frame from "setLastFrame"
     *
     * @return last frame as per imagej slice
     * */
    public Integer getLastFrame(){
        
        return locations.lastKey();
        
    }

    /**
     * Gets the first frame this track exists in.
     *
     * @return first frame as per imagej slice.
     */
    public Integer getFirstFrame(){
        return locations.firstKey();
    }
    
 }
