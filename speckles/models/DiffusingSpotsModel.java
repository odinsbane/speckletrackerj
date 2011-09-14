package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleCalculator;
import speckles.SpeckleTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *  This class is a factory for creating models to pedict speckle
 *  location by finding local intensity maximum, assuming the particle diffuses a small distance.
 *
 * 
 * */
public class DiffusingSpotsModel extends SpeckleModel{
    public double DS;

    double AVERAGE_INTENSITY;
    double AVERAGE_CHANGE;
    
    static int SEARCHSIZE = 12;
    static double f_deltaI = 0.01;
    static double f_d = 0.05;
    static double f_I = 0.94;
    static double w_min = 0.5;
    int DISPLACEMENT_COUNT = 0;

    double IMAGE_MEAN;
    DiffusingSpotsModel(){

    }
    
    /**
     *  Initiates a specklemodel factory for generating speckle
     * @param imp has all of the image data that will be used do not modifiy.
     **/
    public DiffusingSpotsModel(ImagePlus imp){
        implus = imp;
        IMAGE_MEAN = imp.getStatistics().mean;
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

        DiffusingSpotsModel x = new DiffusingSpotsModel(implus);
        x.AVERAGE_INTENSITY = value/tally;
        if(dtally>0){
            x.AVERAGE_CHANGE = deltas/dtally;
            x.DS = DS/dtally;
            x.DISPLACEMENT_COUNT = dtally;
        }else
            x.AVERAGE_CHANGE = 0;
            x.DS = 0;

        //x.evaluateSpeckleTrack(s);
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

        DiffusingSpotsModel x = new DiffusingSpotsModel(implus);
        x.AVERAGE_INTENSITY = value/tally;

        if(dtally>0){
            x.AVERAGE_CHANGE = deltas/dtally;
            x.DS = DS/dtally;
            x.DISPLACEMENT_COUNT = dtally;
        }else
            x.AVERAGE_CHANGE = 0;
            x.DS = 0;

        //for(Speckle s: specks)
        //    x.evaluateSpeckleTrack(s);

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
    public void estimateLocation(SpeckleEstimator speck,int frame){
        if(frame<= SpeckleApp.getSlices(implus)){
            ArrayList<double[]> pts = predictSpeckle(speck, frame);


            if(pts.size()==0){
                speck.end();
                return;

            }

            double[] best = new double[]{0,0,0};
            double[] best_pt = new double[]{0,0};
            double champ = 0;
            int counting = 0;
            for( double[] pt: pts){

                ImageProcessor ip = implus.getStack().getProcessor(frame);


                double p = SpeckleCalculator.averageValueCircle(pt, SpeckleCalculator.INNER_RADIUS,ip);
                double displacement = rootDisplacement(pt, speck.getCoordinates(frame - 1));
                double delta = p - SpeckleCalculator.averageValueCircle(speck.getCoordinates(frame - 1), SpeckleCalculator.INNER_RADIUS,implus.getStack().getProcessor(frame-1));

                double[] w = modelCriteria(p,delta,displacement);

                if(test(w)) counting++;
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

                if(speck.getFail()>= SpeckleTracker.MAX_FAIL){
                    speck.end();
                }
            }
        } else{

            speck.end();

        }


    }



    @Override
    public int modelType() {
        return EXTENDING_MODEL;
    }

    /**
     * Evaluates a speckle track and fills the 'weights' with weight values for tuning
     *
     * @param speck
     * @param weights
     */
    public void evaluateSpeckleTrack(Speckle speck,ArrayList<double[]> weights){


        for(int frame: speck){


                ImageProcessor ip = implus.getStack().getProcessor(frame);
                double[] pt = speck.getCoordinates(frame);
                double[] pt2;

                if(!speck.exists(frame-1))
                    continue;

                pt2 = speck.getCoordinates(frame -1);
                double p = SpeckleCalculator.averageValueCircle(pt, SpeckleCalculator.INNER_RADIUS,ip);
                double displacement = rootDisplacement(pt, pt2);
                double delta = p - SpeckleCalculator.averageValueCircle(pt2, SpeckleCalculator.INNER_RADIUS,implus.getStack().getProcessor(frame-1));

                double[] w = modelCriteriaDebug(p,delta,displacement);

                weights.add(w);

            }




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
     * @param diff intensity change between this frame and previous frame.
     * @param displacement between current frame and previous frame.
     * @return probability, ordering, number
     */
    public double[] modelCriteria(double value, double diff, double displacement){
        double[] results = new double[3];

        double[] w = calculateWeights(value,diff,displacement);

        double intensity_probability = w[0];
        double displacement_probability = w[1];
        double change_probability = w[2];

        double main = getMainWeight(intensity_probability,displacement_probability,change_probability);

        results[0] = main>1?1:main;
        results[1] =  intensity_probability + displacement_probability + change_probability;
        results[2] = (value - IMAGE_MEAN)/(AVERAGE_INTENSITY - IMAGE_MEAN)*Math.PI*Math.pow(SpeckleCalculator.INNER_RADIUS,2);


        return results;
        
    }

    public double[] calculateWeights(double value, double diff, double displacement){
        double diff_squared = Math.pow(diff,2);
        double x = AVERAGE_INTENSITY - value;
        double y = AVERAGE_INTENSITY - IMAGE_MEAN;
        x = x>0?x : 0;

        double intensity_probability = Math.exp(-(x/y));
        double displacement_probability = DS>0?Math.exp(-Math.pow(displacement,2)/(2*DS)):1/(1+displacement);
        double change_probability = AVERAGE_CHANGE>0?Math.exp(-diff_squared/(2*AVERAGE_CHANGE)):1/(1 + diff_squared);


        return new double[]{intensity_probability,displacement_probability,change_probability};
    }

    public double getMainWeight(double intensity_probability, double displacement_probability, double change_probability){
        double main = intensity_probability* f_I + displacement_probability* f_d + change_probability* f_deltaI;

        double t = f_deltaI + f_I + f_d;
        main = main/t;
        return main;
    }
    
    public boolean test(double[] w){
        return w[0]> w_min;
        
    }
/**
     * Model criteria creates the weights value for the estimators and the ordering of the
     *
     * @param value average intensity
     * @param diff intensity change between this frame and previous frame.
     * @param displacement between current frame and previous frame.
     * @return probability, ordering, number
     */
    public double[] modelCriteriaDebug(double value, double diff, double displacement){

        return calculateWeights(value,diff,displacement);

    }

    
    public static double rootDisplacement(double[] p1, double[] p2){

        return Math.sqrt(Math.pow(p1[0] - p2[0],2) + Math.pow(p1[1] - p2[1], 2));

    }

    public static double squareDisplacement(double[] p1, double[] p2){

        return Math.pow(p1[0] - p2[0],2) + Math.pow(p1[1] - p2[1], 2);
    }

    String title_f_d = "f_d (Displacement Factor)";
    String title_f_I = "f_I (Intensity Factor)";
    String title_f_deltaI = "f_deltaI (Relative Intensity Change Factor)";
    String title_w_min = "w_min (minimum threshold)";
    /**
     * For setting/changing model parameters.
     *
     * @return a hashmap for storing parameters.
     */
    public HashMap<String,Double> getParameters(){
        HashMap<String,Double> map = new HashMap<String,Double>();
        map.put("Search Size",(double)SEARCHSIZE);
        map.put(title_f_d, f_d);
        map.put(title_f_I, f_I);
        map.put(title_f_deltaI, f_deltaI);
        map.put(title_w_min , w_min);
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
        f_d = params.get(title_f_d);
        f_I = params.get(title_f_I);
        f_deltaI = params.get(title_f_deltaI);
        w_min = params.get(title_w_min);



    }
    
}
