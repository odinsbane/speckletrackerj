package speckles.utils;

/**
 * Author: Timothy Chow 2009.
 * 
 * This performs a simple transformation from the global x-y corrdinates
 * to a set of coordinates with the orgin at the 'speckle' and the x axis
 * along the vector provided.
 * 
 * */

public class SnakeTransformer{
    
    double[] DIRECTION;
    double[] SPECKLEPOINT;
    
    /**
     *  Creaets a new transformer with pts xnot and dir.
     * 
     * @param xnot is an array with {x, y} data in it.
     * @param dir, the direction that will be the new x axis. this is
     *              assumed to be a normalized vector (ie sin/cosine)
     **/
    public SnakeTransformer(double[] xnot, double[] dir){
        SPECKLEPOINT = xnot;
        DIRECTION = dir;
    }
  
    /**
     * Perofrms a 2D transformation from the global xy values of the image
     * to the local xy values of the associated pt.
     * 
     * @param snakepoint 2D transformation only
     **/
    public double[] transformPoint(double[] snakepoint){
        double[] ret_value = new double[2];
        
        ret_value[0] = (snakepoint[0]-SPECKLEPOINT[0])*DIRECTION[0] + (snakepoint[1]-SPECKLEPOINT[1])*DIRECTION[1];
        ret_value[1] = (snakepoint[1]-SPECKLEPOINT[1])*DIRECTION[0] - (snakepoint[0]-SPECKLEPOINT[0])*DIRECTION[1];
        
        return ret_value;
    }
    
    
    
    
    
    
  
            
    
}
