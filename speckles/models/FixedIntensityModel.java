package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *  This class is a factory for creating models to pedict speckle
 *  location by finding local intensity maximum, assuming the particle diffuses a small distance.
 *
 * 
 * */
public class FixedIntensityModel extends SpeckleModel{
    public double DS;

    static int SEARCHSIZE = 2;
    static double MINIMUM_INTENSITY = 260;
    static double MEAN_DISPLACEMENT = 0.05;

    double IMAGE_MEAN;
    FixedIntensityModel(){

    }

    /**
     *  Initiates a specklemodel factory for generating speckle
     * @param imp has all of the image data that will be used do not modifiy.
     **/
    public FixedIntensityModel(ImagePlus imp){
        implus = imp;
        IMAGE_MEAN = imp.getStatistics().mean;
    }
    
    
    /*
     * Uses the speckle to find the average intensity, and the change in intensity
     * 
     * */
    public SpeckleModel createModel(Speckle s){

        FixedIntensityModel x = new FixedIntensityModel(implus);
        return x;
    }
    
    /*
     *
     * */
    public SpeckleModel createModel(HashSet<Speckle> specks){
        
        FixedIntensityModel x = new FixedIntensityModel(implus);
        return x;

    }
    
    /** 
     * Checks the speck estimator for the next location.  Which assumes the model has
     * already been created.
     * 
     * @param speck estimator of current speckle
     * @param frame current frame
     * 
     **/
    public SpeckleEstimator estimateLocation(SpeckleEstimator speck,int frame){
        if(frame<= SpeckleApp.getSlices(implus)){
            ArrayList<double[]> pts = predictSpeckle(speck, frame);

            //
            if(pts.size()==0){
                speck.end();
                return speck;

            }

            double[] best = new double[]{0,0,0};
            double[] best_pt = new double[]{0,0};
            double champ = 0;
            for( double[] pt: pts){

                ImageProcessor ip = implus.getStack().getProcessor(frame);


                double value = SpeckleCalculator.averageValueCircle(pt, SpeckleCalculator.INNER_RADIUS,ip);
                double change = 0;
                if(speck.exists(frame-1)){
                    change = value - SpeckleCalculator.averageValueCircle(speck.getCoordinates(frame-1),
                                                                SpeckleCalculator.INNER_RADIUS,
                                                                implus.getStack().getProcessor(frame-1));
                }

                double displacement = DiffusingSpotsModel.rootDisplacement(pt, speck.getCoordinates(frame - 1));
                double[] w = modelCriteria(value,displacement,change);

                if(w[0] > champ||(w[0]==champ&&w[1]>best[1])){
                    best = w;
                    champ=w[0];
                    best_pt[0] = pt[0];
                    best_pt[1] = pt[1];
                }

            }
            speck.setWeights(best,best_pt,frame);
            if(test(best)){
                speck.success();
            } else{
                speck.fail(frame);

                if(speck.getFail()>=1){
                    speck.end();
                }
            }
        } else{

            speck.end();

        }

        return speck;
    }

    @Override
    public int modelType() {
        return EXTENDING_MODEL;
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
        double[] opt = speck.getCoordinates(n-1);
        //set new points
        int[] pt = new int[]{(int)opt[0], (int)opt[1]};

        
        ImageStack is = implus.getStack();
        
        
        ImageProcessor next = is.getProcessor(n);
        ArrayList<double[]> maxima = new ArrayList<double[]>();
        //find all of the maximum points
        for(int i = -SEARCHSIZE; i<=SEARCHSIZE; i++){
            for(int j = -SEARCHSIZE; j<=SEARCHSIZE; j++){

                if(isPtLocalMax(next,pt[0] + i,pt[1] + j)){
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

        if(maxima.size()==0)
            maxima.add(new double[]{opt[0],opt[1]});

        
        //return new double[]{x  + mx/sum,y + my/sum};
        return maxima;
    
    }

    static public boolean isPtLocalMax(ImageProcessor ip, int x, int y){
        double p = pv(ip,x,y);
        for(int i = -1; i<2; i++){
            for(int j = -1; j<2; j++){
                if(i==0&&j==0){

                    continue;

                }else{
                    double v = pv(ip,x+i,y+j);
                    if(v>p){
                        return false;
                    }
                }
            }
        }

        return true;

    }

    static public double pv(ImageProcessor ip, int x, int y){

        return 2*ip.getPixelValue(x,y) + ip.getPixelValue(x-1,y) + ip.getPixelValue(x+1, y) + ip.getPixelValue(x, y-1) + ip.getPixelValue(x, y+1);
    }

    /**
     * Model criteria creates the weights value for the estimators and the ordering of the
     *  
     * @param value average intensity
     * @param displacement between current frame and previous frame.
     * @param change intensity change between this frame and previous frame.
     * @return probability, ordering, number
     */
    public double[] modelCriteria(double value, double displacement,double change){
        double[] results = new double[3];
        
        results[0] = value>MINIMUM_INTENSITY?1:0;
        results[1] =  0.5*Math.exp(-displacement/MEAN_DISPLACEMENT ) + 0.5*Math.exp(-Math.pow(change/value,2));
        results[2] = (value - IMAGE_MEAN)/(MINIMUM_INTENSITY - IMAGE_MEAN)*Math.PI*Math.pow(SpeckleCalculator.INNER_RADIUS,2);


        return results;
        
    }




    
    public boolean test(double[] w){
        return w[0]>0;
        
    }

    /**
     * For setting/changing model parameters.
     *
     * @return a hashmap for storing parameters.
     */
    public HashMap<String,Double> getParameters(){
        HashMap<String,Double> map = new HashMap<String,Double>();
        map.put("Search Size",(double)SEARCHSIZE);
        map.put("I_min",MINIMUM_INTENSITY);
        map.put("d_bar",MEAN_DISPLACEMENT);
        return map;
    }

    /**
     * let the model decide
     * @param params
     *
     * @retrun a hashmap for storing parameters.
     */
    public void setParameters(HashMap<String,Double> params){
        SEARCHSIZE = params.get("Search Size").intValue();

        MINIMUM_INTENSITY =  params.get("I_min");
        MEAN_DISPLACEMENT = params.get("d_bar");


    }
    
}
