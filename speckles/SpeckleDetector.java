package speckles;

import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

  /*
    This class will add automatic speckle detection routines, it will use two core routines, 
    connected regions algorithm and a region growing algorithm.  The goal will be to generate a binary image
    and then cause the binary image to grow, with different criteria.which will leave us with a 
  */

/**
 * This is primarily used for locating and detecting speckles.  The main algorithm 
 */
public class SpeckleDetector{


  GrowCriteria ALPHA;
  
  static final int EDGE = 10;
  static double MINDISTANCE = 9; //minimum distance, squared.
  static double MIN_TRACK_SEPARATION = 4;
  //filled after first pass, this has all of the mappings
  ArrayList<Short[]> premap;
  
  //This is the actual map that has all of the mappings after they are reduced
  HashMap<Short,Short> final_map;
  
  //Contains all of the points for an associated value
  HashMap<Short,ArrayList<Integer[]>> log;
  
  //list of centroids, x,y,weight they are stored as doubles
  ArrayList<double[]> output;
  
  short last_added;
  
  int CURRENT_THRESHOLD, LASTMAX, LASTMIN;
  public ArrayList<double[]> getCentroids(ImageProcessor threshed){
    firstPass(threshed);
    secondPass(threshed);
    calculateCentroids();
    return output;
  
  }

    /**
     * Changes an arraylist of centroids into a new speckle.
     *
     * @param cents contains a list of possible speckles, each double[] is {x,y,size}
     * @param ndex
     * @return
     */
  public HashSet<Speckle> getSpeckles(ArrayList<double[]> cents, int ndex){
    HashSet<Speckle> nl = new HashSet<Speckle>();
    
    for(double[] xy: cents)
            nl.add(new Speckle(xy[0],xy[1],ndex));
    
    return nl;
  
  }
  
  
    /**
     *   culls the array list of centroids by the size.
     *
     * @param cents list of centroid coordinates and a size.
     * @param ndex index for creating speckles
     * @param value minimum size.
     * @return
     */
  public static HashSet<Speckle> cullCentroidsBySize(ArrayList<double[]> cents,int ndex, double value){
    HashSet<Speckle> nl = new HashSet<Speckle>(); 
    for(double[] xy: cents){
        
        if(xy[2]>value)            
            nl.add(new Speckle(xy[0],xy[1],ndex));
        
    }
    
    return nl;
    
  }
  
  public HashSet<Speckle> refineSpeckles(HashSet<Speckle> oldspecks, int ndex, ImageProcessor improc){
    ImageProcessor working = improc.duplicate();
    GaussianBlur gb = new GaussianBlur();
    gb.blurGaussian(working, 2, 2, 0.01);
    
    double[] pt;
    for(Speckle speck: oldspecks){
        pt = refinePt(speck.getCoordinates(ndex),2, working);
        speck.addPoint(pt[0], pt[1], ndex);
    }
    HashSet<Speckle> output = new HashSet<Speckle>();
    
    for(Speckle a: oldspecks){
        boolean remove = false;
        pt = a.getCoordinates(ndex);
        Speckle close = null;
        if(pt[0] <EDGE||pt[1]<EDGE||improc.getWidth()-pt[0]<EDGE||improc.getHeight()-pt[1]<EDGE){
            remove = true;
            a.removePoint(ndex);
        } else {
            for(Speckle b: output){
                
                double[] pt2 = b.getCoordinates(ndex);
                double d = Math.pow(pt[0] - pt2[0],2) + Math.pow(pt[1] - pt2[1], 2);
                if(d<MINDISTANCE){
                    remove = true;
                    close = b;
                    break;
                }
                
            }
        }
        if(!remove) 
            output.add(a);
        else if(close!=null)
            removeShorter(a,close,output,ndex);

        
    }
    return output;
  }

    /**
     * moves the supplied speckle to the nearest weighted maxima.  This acts
     * iteratively so a speckle could move up a large gradient rather quickly.
     * 
     * @param speck the speckle that will be adjusted
     * @param working adjustments made according to this image
     * @param ndex index under scrutiny
     */
    public static void refineSpeckleD(Speckle speck, ImageProcessor working, int ndex){
        double dx;
        double dy;
        int loop = 0;
        if (speck.exists(ndex)) do{
            double[] xy = speck.getCoordinates(ndex);
            int n = 2;
            double sum = 0;
            double xsum = 0;
            double ysum = 0;
            for(int i = -n; i<=n; i++){
                for(int j = -n; j<=n; j++){
                    double v = working.getInterpolatedPixel(xy[0] + i, xy[1] + j);
                    sum += v;
                    xsum += i*v;
                    ysum += j*v;

                }


            }



            if(sum!=0&&(xsum!=0||ysum!=0)){
                dx = xsum/sum;
                dy = ysum/sum;
                speck.addPoint(xy[0] + dx, xy[1] + dy, ndex);

            } else{
                dx = 0;
                dy = 0;
            }
            loop++;
            if(loop>5)
                break;
        } while(dx*dx>0.0001||dy*dy>0.0001);

    }



    
    /** returns the xy coordinates to for the maximum value of working, adjusts all but moves slowly.*/
    public static double[] refinePt(double[] xy, int radius, ImageProcessor working){
        double dx = 0;
        double dy = 0;

        double sum = 0;
        double xsum = 0;
        double ysum = 0;

        for(int i = -radius; i<=radius; i++){
            for(int j = -radius; j<=radius; j++){
                double v = working.getInterpolatedPixel(xy[0] + i, xy[1] + j);
                sum += v;
                xsum += v*i;
                ysum += v*j;
            
            }
        
        }

        if(sum!=0&&(xsum!=0||ysum!=0)){
                dx = xsum/sum;
                dy = ysum/sum;
         }
        
        
        return new double[] {xy[0] + dx, xy[1] + dy};
    }
    
    
  /**
     * assumes that old is already in the list of speckels out, if fresh is a speckle with a track then
     * it chooses the longer of the two speckle tracks.
     *
     * @param fresh
     * @param old
     * @param out
     * @param ndex
     */
  public void removeShorter(Speckle fresh, Speckle old, HashSet<Speckle> out,int ndex){
        
        if(fresh.getSize()>old.getSize()){
            old.removePoint(ndex);
            out.remove(old);
            out.add(fresh);
        } else{
            fresh.removePoint(ndex);
        }
  
  }

    /**
     * Takes a binary and performs a first pass connected regions filter on it.
     * Goes through pixel by pixel and checks its top and left neighbors for values
     * Then marks what value this pixel should be.
     * @param threshed binary image
     */
  private void firstPass(ImageProcessor threshed){

    premap = new ArrayList<Short[]>();
    last_added = 0;
    
    final_map = new HashMap<Short,Short>();
    final_map.put(new Short((short)0),new Short((short)0));
    
    int h = threshed.getHeight();
    int w = threshed.getWidth();
    
    for(int i = 0; i<h; i++){
        for(int j = 0; j<w; j++){
            short x = rowBy(threshed,j,i);
            threshed.putPixel(j,i,x);
        }
    }
    reduceMap();
  }

    /**
     * Essential the Kernel for the firstPast.  Filters pixel by checking
     * for a value.  If yes it takes the number above or the number to the left.
     *
     * If there is both a number above and a number to the left then a map values
     * is added.
     *
     * If the pixel is zero, then there is now change
     *
     * @param threshed image data
     * @param j - x coordinate
     * @param i - y coordinate
     * @return the index for the last added.
     */
  private short rowBy(ImageProcessor threshed, int j, int i){

    short above,left,now;
    above = (short)threshed.getPixel(j,i-1);
    left = (short)threshed.getPixel(j-1,i);
    now = (short)threshed.getPixel(j,i);
    if(now>0){
        if(above>0 && left>0){
            if(above != left){
                    Short[] a = {new Short(above),new Short(left)};
                    premap.add(a);
                }
            return above;
        } else if(above>0 || left>0) {
            return above>0?above:left;
        } else{
            last_added += 1;
            Short[] a = {new Short(last_added),new Short(last_added)};
            premap.add(a);
            return last_added;
        }
    } else
        return (short)0;
  }

    /**
     * Goes through the pre-map and groups all of the linking numbers together.
     *
     */
  private void reduceMap(){
    while(premap.size()>0){
        //Set for looping
        Short[] next = premap.get(0);
        premap.remove(0);
        HashSet<Short> next_set = new HashSet<Short>();
        Short source = next[0];
        next_set.add(next[0]);
        next_set.add(next[1]);
        ArrayList<Short> trying = new ArrayList<Short>();
        for(Short e: next_set)
            trying.add(e);
        while(trying.size()>0){
            Short cur = trying.get(0);
            trying.remove(0);
            ArrayList<Short[]> replacement = new ArrayList<Short[]>();
            for(int i=0;i<premap.size(); i++ ){
                Short[] test = premap.get(i);
                if(cur.equals(test[0])||cur.equals(test[1])){
                    int size = next_set.size();
                    next_set.add(test[0]);
                    if(next_set.size()>size){
                        size += 1;
                        trying.add(test[0]);
                    }
                    next_set.add(test[1]);
                    if(next_set.size()>size)
                        trying.add(test[1]);
                }
                else
                    replacement.add(test);
            }
            premap=replacement; 
        }
        //place value into hashmaps values
        for(Short e: next_set)
            final_map.put(e,source);
    }
    
  }

    /**
     * Uses the map created from the numbered image processor, creates the log, which contains all
     * of the coordinates for each point in the connected region.
     *
     * @param separate image processor with numbers that have been mapped, modifies map to be a binary image 0's or 255's.
     */
  private void secondPass(ImageProcessor separate){

    log = new HashMap<Short,ArrayList<Integer[]>>();
    
    for(Short v: final_map.values()){
        ArrayList<Integer[]> points = new ArrayList<Integer[]>();
        log.put(v,points);
    }
    int h = separate.getHeight();
    int w = separate.getWidth();
    for(int i = 0; i<h; i++){
        for(int j = 0; j<w; j++){
            short cur = (short)separate.getPixel(j,i);
            short rep = final_map.get(cur);
            Integer[] point = {new Integer(j),new Integer(i)};
            if(rep!=0){
                log.get(rep).add(point);
                separate.putPixel(j,i,(short)255);
            }
        }
    }
  }
  
  private void calculateCentroids(){
    output = new ArrayList<double[]>();

    for(Short key: log.keySet()){
        //each key represents a region
        if(key != (short)0){
            ArrayList<Integer[]> pts = log.get(key);
            double sumx = 0;
            double sumy = 0;
            double weight = pts.size();
            for(Integer[] pt: pts){
                sumx += pt[0];
                sumy += pt[1];
            }
            double[] next = {sumx/weight,sumy/weight,weight};
            output.add(next);
        }
    }
  }

    /**
     *
     *
     * @return Current list of centroids each double is {x,y,size}
     */
  public ArrayList<double[]> getOutput(){
  /*
        Gets the current arraylist output
  */
    return output;
  }
  
    /**
      *     uses the class variable ALPHA, which is an interface with all the image data
      *     /region growing crtieria in it.
      **/
    public int[][] regionGrowing(int x, int y){
        boolean growing;
        int h = 3;
        int[][] region,oldregion;
        oldregion = new int[1][1];
        oldregion[0][0] = 1;
        

        //main loop

        do{
            growing = false;

            //initialize a new region
            region = new int[h][h];
            for(int i = 1; i< h; i++){
                region[i][0] = 0;
                region[i][h-1] = 0;
                region[0][i] = 0;
                region[h-1][i] = 0;
            }
            for(int i = 0; i<h -2; i++)
                System.arraycopy(oldregion[i],0,region[i+1],1,h-2);
            
            //make it grow centered around the center of mass
            int x_top_left = x - h/2;
            int y_top_left = y - h/2;


            for(int i = 0; i<h-2; i++){
                for(int j = 0; j<h-2; j++){
                    if(oldregion[i][j]>0){
                    
                    
                        //above
                        if(region[i][j+1]==0 && 
                            ALPHA.checkPixel(x_top_left + j + 1, y_top_left + i)){


                            region[i][j+1] = 1;
                                growing = true;
                        
                        
                        }
                        //below
                        if(region[i+2][j+1]==0 && 
                            ALPHA.checkPixel(x_top_left + j + 1,y_top_left + i+2)){
                        
                        
                            region[i+2][j+1] = 1;
                            growing = true;
                        
                        
                        }
                        //left
                        if(region[i+1][j]==0 && 
                            ALPHA.checkPixel(x_top_left + j, y_top_left + i + 1)){
                            region[i+1][j] = 1;
                            growing = true;
                        }
                        //right
                        if(region[i+1][j+2]==0 && 
                            ALPHA.checkPixel(x_top_left + j + 2, y_top_left + i+1)){
                            region[i+1][j+2] = 1;
                            growing = true;
                        }

                    }

                }
            }
            
            
            oldregion = region;
            h += 2;
            if(h>15)
                growing = false;
            
        }while(growing);
        
        return region;
      }
      
      public ImageProcessor autoThreshold(ImageProcessor o){
        ImageProcessor pre = o.duplicate();
        //pre.convolve(sharpen_kernel,7,7);
        
        ImageProcessor retproc = pre.convertToShort(true);
        int[] histo = retproc.getHistogram();
        double partial = 0.003*o.getHeight()*o.getWidth();
        int sum = 0;
        int i = histo.length;
        boolean maxlooking = true;
        while(sum<partial){
            i--;
            sum += histo[i];
            if(maxlooking&&sum>0){
                LASTMAX = i;
                maxlooking = false;
            }
        }
        CURRENT_THRESHOLD = i;
        
        boolean minlooking = true;
        i = 0;
        
        while(minlooking){
            if(histo[i]>0)
                minlooking=false;
            else
                i++;
        }
        LASTMIN = i;
        retproc.threshold(CURRENT_THRESHOLD);
        return retproc;
            
      }
      
      public ImageProcessor threshold(ImageProcessor ip, int value){
            ImageProcessor retproc = ip.duplicate().convertToShort(true);
            retproc.threshold((short)value);
            return retproc;
      }
     
    public static ImageProcessor threshold(ImageProcessor x, double threshold){
        ImageProcessor y = new ShortProcessor(x.getWidth(),x.getHeight());
        for(int i = 0; i<x.getHeight(); i++){
            for(int j = 0; j<x.getWidth(); j++){
            
                if(x.getPixelValue(j,i)>=threshold){
                    y.set(j,i,255);
                } else
                    y.set(j,i,0);
            
            }
        
        }
        
        return y;
    
    }
    
    public int getImageMin(){
        return (int)LASTMIN;
    }
    public int getImageMax(){
        return (int)LASTMAX;
    }
    
    public int getImageThreshold(){
        return (int)CURRENT_THRESHOLD;
    }
    
    
}

/**
    This interface is used for the region growing where more complicated methods can be defined else where.
**/
interface GrowCriteria{
    public boolean checkPixel(int x, int y);

}

