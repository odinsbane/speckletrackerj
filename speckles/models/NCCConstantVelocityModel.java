package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleTracker;
import speckles.utils.NormalizedCrossCorrelationFilter;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
/**
 *  This is merely a convenience class to keep track of the model being used
 *  since an array will get confusing.
 * */
public class NCCConstantVelocityModel extends SpeckleModel{

    public double MINVAR= 0.2;
    
    public double AVERAGE_CORRELATION, SIGMA_CORRELATION,
                  AVERAGE_PIXEL, SIGMA_PIXEL;
    public double DX,DY;
    NormalizedCrossCorrelationFilter ncc;

    Rectangle2D bounds = new Rectangle2D.Double(0,0,0,0);


    private double tcor, tcor_sqd, tpix, tpix_sqd, tmx, tmy, terror, tweight, tv_weight;

    static double LIMIT = 0.1;
    static int SEARCHSIZE = 1;
    static double ALPHA = 0.5;
    
    public NCCConstantVelocityModel(){
        tcor = 0;
        tcor_sqd = 0;
        tpix = 0;
        tpix_sqd = 0;
        tmx = 0;
        tmy = 0;
        terror= 0;
        tweight = 0;
        tv_weight = 0;
        
    }

    public NCCConstantVelocityModel(ImagePlus imp){
        this();
        implus = imp;

    }
    /**
     * 
     * @param - mx least squares fit of x displacement
     * @param - my least squares fit of y displacement
     * @param - correlated_sum sum of values for speckle from NCC in each frame
     * @param - correlated_sum_sqd squared sum
     * @param - sum_pixels sum of pixels for each speckle frame
     * @param - sum_pixels_sqd sqd sum
     * @param - weight - number of frames for pixel
     * 
     **/
    private NCCConstantVelocityModel(
            double mx, double my, double error,
            double correlated_sum, double correlated_sum_sqd,
            double sum_pixels, double sum_pixels_sqd, double weight){
                                        
        implus = null;
        terror = error;
        tweight = weight;
        tcor = correlated_sum;
        tcor_sqd = correlated_sum_sqd;
        tpix = sum_pixels;
        tpix_sqd = sum_pixels_sqd;
        if(weight>1){
            tmx = mx*weight;
            tmy = my*weight;
            tv_weight = weight;
        } else{
            
            tmx = 0;
            tmy = 0;
            tv_weight = 0;
        
        }
                                        
    }
                        
    /*
     *                              mx, 
     *                              my, 
                                    Math.sqrt(error),
                                    sum, 
                                    sum_sqd,
                                    sum_pixels, 
                                    sum_pixels_sqd, 
                                    frames.size()}
     */
    public void updateValues(SpeckleModel values){
        NCCConstantVelocityModel new_values = (NCCConstantVelocityModel)values;
        terror += new_values.terror;
        tcor += new_values.tcor;
        tcor_sqd += new_values.tcor_sqd;
        tpix += new_values.tpix;
        tpix_sqd += new_values.tpix_sqd;
        tweight += new_values.tweight;
        tmx += new_values.tmx;
        tmy += new_values.tmy;
        tv_weight += new_values.tv_weight;
        
    }
    
    
    public void prepare(){
        if(tv_weight>0){
            DX = tmx/tv_weight;
            DY = tmy/tv_weight;
        } else{
            DX = 0;
            DY = 0;
        }
        
        AVERAGE_CORRELATION = tcor/tweight;
        SIGMA_CORRELATION = Math.sqrt(tcor_sqd/tweight - AVERAGE_CORRELATION*AVERAGE_CORRELATION);
        
        SIGMA_CORRELATION = SIGMA_CORRELATION>AVERAGE_CORRELATION?SIGMA_CORRELATION:MINVAR*AVERAGE_CORRELATION;
            
        AVERAGE_PIXEL = tpix/tweight;
        
        SIGMA_PIXEL = Math.sqrt(tpix_sqd/tweight - AVERAGE_PIXEL*AVERAGE_PIXEL);
            
        SIGMA_PIXEL = SIGMA_PIXEL>AVERAGE_PIXEL*MINVAR?SIGMA_PIXEL:AVERAGE_PIXEL*MINVAR;
        
    }

    /**
     * This this will use the weights created by the speckle model to determine if the speckle should propagate.
     * @param weights is a 3 valued array - probability, weight, number
     * @return if the speckle should stay.
     **/
    public boolean test(double[] weights){

        return weights[0]>LIMIT;
        
    }
    
    
    /**
     *   
     * 
     **/
    public SpeckleModel createModel(Speckle s){
        
        NCCConstantVelocityModel x = new NCCConstantVelocityModel(implus);
        
        x.ncc = new NormalizedCrossCorrelationFilter(implus);
        x.ncc.createTemplate(s);
        
        x.updateValues(x.model(s));
        x.prepare();

        x.bounds.setRect(0,0,implus.getWidth(), implus.getHeight());
        return x;
    }
    
    
    public SpeckleModel createModel(HashSet<Speckle> specks){
        NCCConstantVelocityModel x = new NCCConstantVelocityModel(implus);
        
        x.ncc = new NormalizedCrossCorrelationFilter(implus);
        x.ncc.createTemplate(specks);

        for(Speckle s: specks){
            NCCConstantVelocityModel y = x.model(s);
            x.updateValues(y);
            
        }
        
        x.prepare();
        x.bounds.setRect(0,0,implus.getWidth(), implus.getHeight());
        return x;
        
    }
    
    /**
     *  not a functional SpeckleModel used for building statistics.
     * */
    private NCCConstantVelocityModel model(Speckle s){
        TreeSet<Integer> frames = new TreeSet<Integer>();
        for(Integer i: s)
            frames.add(i);
        
        ImageStack is = implus.getStack();
                
        double[] xs = new double[frames.size()];
        double[] ys = new double[frames.size()];
        double[] zs = new double[frames.size()];
        int k = 0;
        for(Integer i: frames){
            
            double[] pt = s.getCoordinates(i);
            
            xs[k] = pt[0];
            ys[k] = pt[1];
            zs[k] = i;

            k++;
            
        }
        double sum_pixels=0;
        double sum_pixels_sqd = 0;
        double sum_sqd = 0;
        double sum = 0;
        double error = 0;
        double mx = 0;
        double my = 0;
        
        if(frames.size()>1){
        
            double[] six = SpeckleTracker.leastSquares(zs,xs);
            mx = six[0];
            double[] siy = SpeckleTracker.leastSquares(zs,ys);
            my = siy[0];
            for(int i = 0; i<frames.size(); i++){
                error += Math.pow(xs[i] - zs[i]*six[0] - six[1], 2) +
                         Math.pow(ys[i] - zs[i]*siy[0] - siy[1], 2);
                double v = filter(is.getProcessor((int)zs[i]), xs[i], ys[i]);
                sum += v;
                sum_sqd += v*v;
                double px = is.getProcessor((int)zs[i]).getInterpolatedValue(xs[i],ys[i]);
                sum_pixels += px;
                sum_pixels_sqd += px*px;
                
            }

        } else {
            double v = filter(is.getProcessor((int)zs[0]), xs[0], ys[0]);
            sum += v;
            sum_sqd += v*v;
        
            double px = is.getProcessor((int)zs[0]).getInterpolatedValue(xs[0],ys[0]);
            sum_pixels += px;
            sum_pixels_sqd += px*px;
        }
        NCCConstantVelocityModel x = new NCCConstantVelocityModel(mx, my,
                                    Math.sqrt(error),sum, sum_sqd,sum_pixels, 
                                    sum_pixels_sqd, frames.size());
        
        
        return x;
    
        
    }
    
    
     /**
     *      Uses the current model and trys to place the speckle frame 
     *      specified. 
     **/
    public double[]  predictSpeckle(SpeckleEstimator speck, int n){
        TreeSet<Integer> frames = new TreeSet<Integer>();
        for(Integer i: speck)
            frames.add(i);
        
        
            
        //original
        double[] opt = speck.getCoordinates(frames.last());
        //set new points
        double[] pt = new double[]{opt[0] + DX, opt[1] + DY};
        pt[0] += DX;
        pt[1] += DY;
        
        ImageStack is = implus.getStack();
        
        
        ImageProcessor next = is.getProcessor(n);
        int counter = 0;
    
        double max = 0;
        double x =  pt[0];
        double y = pt[1];
       
        
        
        for(int i = -SEARCHSIZE; i<=SEARCHSIZE; i++){
            for(int j = -SEARCHSIZE; j<=SEARCHSIZE; j++){
                double v = filter(next,pt[0] + i, pt[1] + j);
                
                
                double p = next.getInterpolatedPixel(pt[0] + i,pt[1] + j);
                
                
                if(max<p*v){
                    max = v*p;
                    x = pt[0] + i;
                    y = pt[1] + j;
                    
                }
            }
        }
        
        
        if(!bounds.contains(x,y)){
            x = opt[0];
            y = opt[1];
        }
        return new double[]{x,y};

    
    }
    
    public double[] modelCriteria(double correlation, double pixel, double[] displacement){
        double off_path = 0;
        if(displacement!=null)
            off_path = Math.sqrt(Math.pow(DX - displacement[0],2)+Math.pow(DY - displacement[1],2));


        double cor = correlation>AVERAGE_CORRELATION?0:AVERAGE_CORRELATION-correlation;
        double val = pixel>AVERAGE_PIXEL?0:AVERAGE_PIXEL - pixel;
        double p = Math.exp(-Math.pow(cor/SIGMA_CORRELATION,2)/2) * Math.exp(-ALPHA*Math.pow(val/SIGMA_PIXEL,2)/2);
        double count = pixel/AVERAGE_PIXEL;

        return new double[]{p, p/(1+off_path), count};
    }
    
    
    public void estimateLocation(SpeckleEstimator speck,int frame){
        
        if(frame<= SpeckleApp.getSlices(implus)){
            double[] pt = predictSpeckle(speck, frame);
        
            ImageProcessor ip = implus.getStack().getProcessor(frame);
        
            double c = ncc.filter(implus.getStack().getProcessor(frame), pt[0],pt[1]);
            double p = ip.getInterpolatedPixel(pt[0],pt[1]);

            double[] dr = null;
            if(speck.exists(frame-1)){
                double[] opt = speck.getCoordinates(frame-1);
                dr = new double[]{pt[0] - opt[0] , pt[1] - opt[1]};
            }
            double[] w = modelCriteria(c,p,dr);
            speck.setWeights(w,pt,frame);
            if(test(w)){
                speck.success();
            }else{
                speck.fail(frame);
                
                
                if(speck.getFail()>2){
                    speck.end();
                }
            }
        } else{
            speck.end();
        }

    }
    public double filter( ImageProcessor improc, double x, double y){
        return ncc.filter(improc, x, y);
        
    }

    /**
     * For setting/changing model parameters.
     *
     * @return a hashmap for storing parameters.
     */
    public HashMap<String,Double> getParameters(){
        HashMap<String,Double> map = new HashMap<String,Double>();
        map.put("Search Size",(double)SEARCHSIZE);
        map.put("Minimum Probability",LIMIT);
        map.put("Alpha", ALPHA);
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
        LIMIT = params.get("Minimum Probability");

    }

    public int modelType(){
        return EXTENDING_MODEL;
    }


}
