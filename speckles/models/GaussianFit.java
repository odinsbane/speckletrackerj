package speckles.models;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.utils.leastsquares.Fitter;
import speckles.utils.leastsquares.Function;
import speckles.utils.leastsquares.LinearFitter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

/**
 *  This is how the speckle evolves.
 * */
public class GaussianFit extends SpeckleModel{
    static double MIN_SIGMA = 0.25;
    static double START_SIGMA = 1;
    static double MAX_SIGMA = 3;
    static double start_delta=0.75;

    static double TOLERANCE = 1e-6;
        
    int RADIUS = 5;
    
    double[][] xys = new double[(2*RADIUS+1)*(2*RADIUS+1)][2];
    
    double[] z = new double[(2*RADIUS+1)*(2*RADIUS+1)];
    
    Fitter solver;
    
    //local coordinates for finding maximum of A
    int[] loc = {-1, 0, 1};

    GaussianFit(){
        
    }
    
    /**
     *  Fits to a 2d gaussian
     *
     **/
    public GaussianFit(ImagePlus imp){
        implus = imp;

    }


    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one speckle.
     * */
    public SpeckleModel createModel(Speckle s){
        GaussianFit x = new GaussianFit(implus);
        
        return x;
        
    }
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one all of the speckles in the group.
     * */
    public SpeckleModel createModel(HashSet<Speckle> specks){
        GaussianFit x = new GaussianFit(implus);

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

        double[] pt, npt;
        double[] weights;
        for(Integer i: speck){
            
            pt = speck.getCoordinates(i);
            npt = refinePt(pt, implus.getStack().getProcessor(i));

            weights = new double[] {1,npt[2], npt[3], npt[4]};
            speck.setWeights(weights, new double[] {npt[0], npt[1]}, i);
        
            if(SpeckleApp.isStopped()) break;
        }
        speck.end();

    }
    
    /**
     *
     *
     * @param xy
     * @param ip
     * @return
     */
     public double[] refinePt(double[] xy, ImageProcessor ip){
        for(int i = 0; i<=2*RADIUS; i++){
            for(int j = 0; j<=2*RADIUS; j++){

                xys[i*(2*RADIUS+1) + j][0] = i - RADIUS;
                xys[i*(2*RADIUS+1) + j][1] = j - RADIUS;
                double v = ip.getInterpolatedPixel(xy[0] + i - RADIUS, xy[1] + j - RADIUS);
                z[i*(2*RADIUS+1) + j] = v;

            }
        }

        //create initial parameters assumes bright spot is close to center.
        double B = 0;
        for(int i = -RADIUS; i<=RADIUS; i++){
            B += ip.getInterpolatedPixel(xy[0] + RADIUS, xy[1] + i);
            B += ip.getInterpolatedPixel(xy[0] - RADIUS, xy[1] + i);
            B += ip.getInterpolatedPixel(xy[0] + i, xy[1] + RADIUS);
            B += ip.getInterpolatedPixel(xy[0] + i, xy[1] - RADIUS);
        
        }
        
        
        B = B/(8*RADIUS + 4);
        double A = 0;

         //uses a 3x3 region to find the central maxima starting point.
        for(int i: loc){
            for(int j: loc){
                A += ip.getInterpolatedPixel(xy[0] + i, xy[1] + j);
            }
        }
        
        A = A/(loc.length*loc.length) - B;



        double x = 0;
        double y = 0;

        SigmaCrawl crawler = new SigmaCrawl();

        crawler.sigma = START_SIGMA;

        FitSpace space = new FitSpace();
        space.A = A;
        space.B = B;
        space.xy = xys;
        space.z = z;

         TreeSet<FitResults> works= new TreeSet<FitResults>(new FitComparator());


        while(crawler.crawling){
            double sigma = crawler.sigma;
            //assume original guess is +- a pixel
            works.clear();
            works.add(fitLocation(0,0,sigma,space));

            works.add(fitLocation(start_delta, 0, sigma, space));
            works.add(fitLocation(-start_delta, 0, sigma, space));
            works.add(fitLocation(0,start_delta, sigma, space));
            works.add(fitLocation(0,-start_delta, sigma, space));

            if(works.size()==1){
                return new double[] { xy[0], xy[1], crawler.sigma, 0, 0};
            }

            //scan x-y values get the best x-y value, get the best error for a particular sigma

            boolean scanning = true;
            FitResults first, last, newest;
            first = works.first();
            last = works.pollLast();
            while(scanning){

                newest = fitLocation((first.x + last.x)*0.5, (first.y + last.y)*0.5, sigma, space);



                works.add(newest);

                first = works.first();
                last = works.pollLast();
                double diff = (last.error - first.error) / first.error;
                if(diff < TOLERANCE)
                    scanning = false;
            }
            crawler.step(first);
            if(crawler.sigma<MIN_SIGMA||crawler.sigma>MAX_SIGMA){
                return new double[] { xy[0], xy[1], crawler.sigma, 0, 0};
            }

        }

        FitResults best = crawler.results[0];
        return new double[] { xy[0] + best.x, xy[1] + best.y, crawler.sigma, best.A, best.B};
        
        
    }

    public FitResults fitLocation(double dx, double dy, double sigma, FitSpace space){
        Function f = new gaussian(dx,dy, sigma);

        Fitter solver = new LinearFitter(f);
        solver.setData(space.xy, space.z);
        solver.setParameters(new double[] { space.A, space.B});
        boolean broken = false;
        try{
            solver.fitData();
        } catch(Exception e){
            broken = true;
        }

        FitResults results = new FitResults();
        results.broken = broken;

        if(!broken){
            //acceptable sigma value
            double e = solver.calculateErrors();
            double[] p = solver.getParameters();

            results.error = e;
            results.A = p[0];
            results.B = p[1];
            results.x = dx;
            results.y = dy;
            results.sigma = sigma;
        }

        return results;
    }

    /**
     * Does them all.
     * @param to_track set of speckles for tracking.
     * @param not_tracking these speckles will be accounted for but not tracked.
     * @param frame - current frame.
     */
    @Override
    public void prepareSpeckles(HashSet<Speckle> to_track,HashSet<Speckle> not_tracking, int frame ){

    }

    public int modelType(){
        return REFINE_MODEL;
    }

    /**
     * For setting/changing model parameters.  One way to set parameters is to make static variables
     * in your model. Then create a map in this function with the appropriate
     *
     * @return set of parameters.
     */
    public HashMap<String,Double> getParameters(){
        HashMap<String, Double> parameters = new HashMap<String, Double>();
        parameters.put("minimum sigma",MIN_SIGMA);
        parameters.put("initial sigma", START_SIGMA);
        parameters.put("maximum sigma", MAX_SIGMA);
        parameters.put("initial space step",start_delta );
        parameters.put("fit region size",new Double(RADIUS) );

        return parameters;
    }
    /**
     * let the model decide
     * @param parameters
     *
     */
    public void setParameters(HashMap<String,Double> parameters){
        MIN_SIGMA = parameters.get("minimum sigma");
        START_SIGMA = parameters.get("initial sigma");
        MAX_SIGMA = parameters.get("maximum sigma");
        start_delta = parameters.get("initial space step");
        RADIUS = parameters.get("fit region size").intValue();

    }
}

class gaussian implements Function{
    double x0,y0, sigma;
    private gaussian(){
        //don't do this
    }
    gaussian(double x0, double y0, double sigma){
        this.x0 = x0;
        this.y0 = y0;
        this.sigma = sigma;

    }

    public double evaluate(double[] values, double[] parameters) {
        double a = parameters[0];
        double b = parameters[1];

        return a*Math.exp(-(Math.pow(values[0] - x0,2) + Math.pow(values[1] - y0,2))/(2*Math.pow(sigma,2))) + b;

    }

    public int getNParameters() {
        return 2;
    }

    public int getNInputs() {
        return 2;
    }
}

class FitResults{
    double A,B,x,y,sigma;
    double error =  Double.MAX_VALUE;

    boolean broken;
    public String toString(){
        return String.format("%.2f\t%.2f\t%.2f\t%.2f\t%.2f",A,B,x,y,sigma);
    }
}

class FitSpace{
    double A,B;
    double[] z;
    double[][] xy;
}

class FitComparator implements Comparator<FitResults> {

    public int compare(FitResults o1, FitResults o2) {
        double difference = o1.error - o2.error;
        if(difference>0) return 1;

        return -1;
    }


}

class SigmaCrawl{
    boolean crawling = true;
    double sigma;
    int count = 0;
    double error =  Double.MAX_VALUE;
    double step = 0.1;
    double current_step=step;

    /**
      *  left - 0
      *  middle - 1
      *  right - 2
     *
     *  by the time the third jump starts, middle will always be the best.
      **/
    FitResults[] results = new FitResults[3];
    public void step(FitResults new_result){
        switch(count){
            case 0:
                firstJump(new_result);
                break;
            case 1:
                secondJump(new_result);
                break;
            case 2:
                thirdJump(new_result);
                break;
            default:
                fourthJump(new_result);
                break;
        }

    }

    /**
     * Initializes error and increments sigma
     *
     * @param new_result first fit result
     */
    private void firstJump(FitResults new_result){
        results[0] = new_result;
        error = new_result.error;
        sigma+=step;
        count++;
    }

    /**
     * Transitional step, if the second step is worse stops crawling with out
     * any more check sigma would be less than the minimum starting value 0.25.
     *
     * @param new_result
     */
    private void secondJump(FitResults new_result){
        double change = error - new_result.error;
        if(change<0){
            //go down the slope.
            error = results[0].error;
            step = step*-1;
            results[1] = results[0];
            results[0] = new_result;
            sigma = results[1].sigma + step;
        } else{
            //improved
            error = new_result.error;
            results[1] = new_result;
            sigma+=step;
            count++;
        }
    }

    /**
     *
     * @param new_result
     */
    private void thirdJump(FitResults new_result){
        results[2] = new_result;
        double change = error - new_result.error;
        if(change<0){
            //enter stage - 4 where best result is between results[0], and results[2].
            count++;
            sigma = results[0].sigma + step*0.5;
            //do not update the error because the best is the middle.
        } else{
            //still improving just keep moving.
            results[0] = results[1];
            results[1] = results[2];
            //the newest error is the best.
            error = results[1].error;
            sigma+=step;
        }
    }

    /**
     * The minimum is contained between results[0] and results[2]
     *
     * @param new_result
     */
    private void fourthJump(FitResults new_result){

        double change = error - new_result.error;
        if(change<0){
            //did not improve, replace side, and step.

            //step keeps sign, in case the first sigma was greater than.
            if((new_result.sigma - results[1].sigma)*step<0){
                //left side
                results[0] = new_result;

            }else{
                //right size
                results[2] = new_result;

            }


        } else{
            //improved, replace middle results and update.

            //step keeps sign, in case the first sigma was greater than.
            if((new_result.sigma - results[1].sigma)*step<0){
                //left side
                results[2] =  results[1];
                results[1] = new_result;
                error = new_result.error;

            }else{
                //right side
                results[0] = results[1];
                results[1] = new_result;
                error = new_result.error;
            }

        }

        double delta = (results[2].sigma-results[0].sigma)*0.5;
        double size = delta/step;
        if(size<0.2){
            crawling = false;
            results[0] = results[1]; //place the best in the first spot.
        } else{
            sigma = results[0].sigma + delta;
            if(sigma==results[1].sigma){
                //choosing the same sigma.
                crawling = false;
                results[0] = results[1];

            }
        }


    }


}