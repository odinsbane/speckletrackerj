package speckles.models;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleCalculator;
import speckles.utils.NormalizedCrossCorrelationFilter;

import java.util.HashMap;
import java.util.HashSet;

import static speckles.models.NCCConstantVelocityModel.*;

public class DiffusingNCCModel extends SpeckleModel{
    public double MINVAR= 0.2;
    
    public double AVERAGE_CORRELATION, SIGMA_CORRELATION,
                  AVERAGE_PIXEL, SIGMA_PIXEL;
          
    NormalizedCrossCorrelationFilter ncc;
    
    private double tcor, tcor_sqd, tpix, tpix_sqd, tweight;
    
    public DiffusingNCCModel(){
        tcor = 0;
        tcor_sqd = 0;
        tpix = 0;
        tpix_sqd = 0;
        tweight = 0;

    }

    DiffusingNCCModel(ImagePlus imp){
        this();
        implus = imp;
    }
    
    /**
     * 
     * @param -
     * @param
     * @param
     * @param
     * @param
     * 
     * */
    /**
     *
     * @param correlated_sum correlated_sum sum of values for speckle from NCC in each frame
     * @param correlated_sum_sqd - correlated_sum_sqd squared sum
     * @param sum_pixels - sum_pixels sum of pixels for each speckle frame
     * @param sum_pixels_sqd - sum_pixels_sqd sqd sum
     * @param weight - weight - number of frames for pixel
     */
    private DiffusingNCCModel(

            double correlated_sum, double correlated_sum_sqd,
            double sum_pixels, double sum_pixels_sqd, double weight){
                                        
        implus = null;
        tweight = weight;
        tcor = correlated_sum;
        tcor_sqd = correlated_sum_sqd;
        tpix = sum_pixels;
        tpix_sqd = sum_pixels_sqd;

                                        
    }
                        
    /**
     * For accumulating models of different speckles and create one big bad model.
     *
     * @param values the individual model that is being included.
     */
    public void updateValues(SpeckleModel values){
        DiffusingNCCModel new_values = (DiffusingNCCModel)values;
        tcor += new_values.tcor;
        tcor_sqd += new_values.tcor_sqd;
        tpix += new_values.tpix;
        tpix_sqd += new_values.tpix_sqd;
        tweight += new_values.tweight;

    }
    
    
    public void prepare(){
        

        
        
        AVERAGE_CORRELATION = tcor/tweight;
        SIGMA_CORRELATION = Math.sqrt(tcor_sqd/tweight - AVERAGE_CORRELATION*AVERAGE_CORRELATION);
        
        SIGMA_CORRELATION = SIGMA_CORRELATION>AVERAGE_CORRELATION?SIGMA_CORRELATION:MINVAR*AVERAGE_CORRELATION;
            
        AVERAGE_PIXEL = tpix/tweight;
        
        SIGMA_PIXEL = Math.sqrt(tpix_sqd/tweight - AVERAGE_PIXEL*AVERAGE_PIXEL);
            
        SIGMA_PIXEL = SIGMA_PIXEL>AVERAGE_PIXEL*MINVAR?SIGMA_PIXEL:AVERAGE_PIXEL*MINVAR;
        
    }


    /**
     * This this will use the weights created by the speckle model to determine if the speckle should propagate.
     * @param weights is a 3 valued array, correlation variation, intensity variation, and displacement from mean trajector
     *                  as determined by the specklemodel
     * @return if this speckle should propagate.
     **/
    public boolean test(double[] weights){

        return weights[0]>LIMIT;
        
    }
    
    
    /**
     *   
     * 
     **/
    public SpeckleModel createModel(Speckle s){
        
        DiffusingNCCModel x = new DiffusingNCCModel(implus);
        
        x.ncc = new NormalizedCrossCorrelationFilter(implus);
        x.ncc.createTemplate(s);
        
        x.updateValues(x.model(s));
        x.prepare();
        return x;
    }
    
    
    public SpeckleModel createModel(HashSet<Speckle> specks){
        DiffusingNCCModel x = new DiffusingNCCModel(implus);
        
        x.ncc = new NormalizedCrossCorrelationFilter(implus);
        x.ncc.createTemplate(specks);

        for(Speckle s: specks){
            DiffusingNCCModel y = x.model(s);
            x.updateValues(y);
            
        }
        
        x.prepare();
        
        return x;
        
    }
    
    /**
     *  not a functional SpeckleModel used for building statistics.
     *
     * @param s speckle that will be modelled.
     * @return a model for building statistics only.
     */
    private DiffusingNCCModel model(Speckle s){

        ImageStack is = implus.getStack();
                


        double sum_pixels=0;
        double sum_pixels_sqd = 0;
        double sum_sqd = 0;
        double sum = 0;

        for(int i: s){
            ImageProcessor ip = is.getProcessor(i);
            double[] pt = s.getCoordinates(i);
            double v = filter(ip, pt[0], pt[1]);
            sum += v;
            sum_sqd += v*v;
            double px = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ip);
            sum_pixels += px;
            sum_pixels_sqd += px*px;
        }

        DiffusingNCCModel x = new DiffusingNCCModel(
                                    sum, sum_sqd,sum_pixels,
                                    sum_pixels_sqd, s.getSize());
        
        
        return x;
    
        
    }
    
    
     /**
     *      Uses the current model and tries to place the speckle frame
     *      specified. 
     **/
    public double[]  predictSpeckle(SpeckleEstimator speck, int n){

        //original
        double[] pt = speck.getLastCoordinates();
        //set new points
        
        ImageStack is = implus.getStack();
        
        if(n<= SpeckleApp.getSlices(implus)){
            ImageProcessor next = is.getProcessor(n);

            double max = -Double.MAX_VALUE;
            double x =  0;
            double y = 0;

            for(int i = -SEARCHSIZE; i<=SEARCHSIZE; i++){
                for(int j = -SEARCHSIZE; j<=SEARCHSIZE; j++){
                    double v = filter(next,pt[0] + i, pt[1] + j);

                    if(max<v){
                        max = v;
                        x = pt[0] + i;
                        y = pt[1] + j;
                        
                    }
                }
            }

            double dx = 0;
            double dy = 0;
            double sum = 0;

            for(int i = -1; i<=1; i++){
                for(int j = -1; j<=1; j++){

                    //double v = SpeckleCalculator.averageValueCircle(new double[]{x+i, y+j},SpeckleCalculator.INNER_RADIUS,next);;
                    double v = ncc.filter(next,x,y)*SpeckleCalculator.averageValueCircle(new double[]{x+i, y+j},SpeckleCalculator.INNER_RADIUS,next);
                    dx += v*i;
                    dy += v*j;
                    sum += v;

                }
            }

            if(sum>0){
                x += dx/sum;
                y += dy/sum;
            }
            return new double[]{x,y};
        }
        
        return null;
        

    
    }
    
    public double[] modelCriteria(double correlation, double pixel, double displacement){
        double pixel_probability = 1;
        if(pixel<AVERAGE_PIXEL){
            pixel_probability = Math.exp(-0.5*ALPHA*Math.pow((AVERAGE_PIXEL-pixel)/SIGMA_PIXEL,2));
        }
        double correlation_probability = 1;
        if(correlation<AVERAGE_CORRELATION){
            correlation_probability = Math.exp(-0.5*Math.pow((AVERAGE_CORRELATION-correlation)/SIGMA_CORRELATION,2));
        }
        double prob = pixel_probability*correlation_probability;
        double order = prob/displacement;
        return new double[]{prob, order, 1};
    }
    
    
    public void estimateLocation(SpeckleEstimator speck,int frame){
        if(frame<=SpeckleApp.getSlices(implus)){
            double[] pt = predictSpeckle(speck, frame);
                    
            ImageProcessor ip = implus.getStack().getProcessor(frame);
            
            double c = ncc.filter(implus.getStack().getProcessor(frame), pt[0],pt[1]);
            double p = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ip);
            double d = 0;
            if(speck.exists(frame - 1)){

                double[] pt2 = speck.getCoordinates(frame -1);
                d = Math.sqrt(Math.pow(pt[0] - pt2[0],2) - Math.pow(pt[1] - pt2[0],2));

            }

            double[] w = modelCriteria(c,p,d);
            speck.setWeights(w,pt,frame);
            if(test(w)){
                speck.success();
            } else
                speck.end();
        } else{
            speck.end();
        }

    }
    /**
     * For setting/changing model parameters.  One way to set parameters is to make static variables
     * in your model. Then create a map in this function with the appropriate
     *
     * @return
     */
    public HashMap<String,Double> getParameters(){
        return new NCCConstantVelocityModel().getParameters();
    }

    /**
     * let the model decide
     * @param params
     */
    public void setParameters(HashMap<String,Double> params){
        new NCCConstantVelocityModel().setParameters(params);
    }

    double filter( ImageProcessor ip,double x, double y){

        return ncc.filter(ip,x,y);

    }

    public int modelType(){
        return EXTENDING_MODEL;
    }
}