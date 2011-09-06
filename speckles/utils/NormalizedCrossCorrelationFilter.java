package speckles.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;

import java.util.HashSet;
public class NormalizedCrossCorrelationFilter{
    
    private double[][] template;
    private double TAVE, TSIGMA;
    
    public double SPECKMEAN, SPECKDEV,SPECKMIN;
    
    
    private ImagePlus implus;
    
    //must be odd
    static public int TEMPLATESIZE = 9;
    
    public NormalizedCrossCorrelationFilter(ImagePlus imp){
        implus = imp;
    }
    
    
    
    
    /**
     * 
     *  For each frame in the speckle this sums all of the cropped images
     *  with the center at the location of the speckle and the size of 
     *  TEMPLATESIZE.  The result is an averaged image that corresponds 
     *  to the Template to be used for the normalized cross correlation.
     * 
     * @param   s - the speckle that is 'templated'
     **/
    public void createTemplate(Speckle s){
        template = new double[TEMPLATESIZE][TEMPLATESIZE];
        
        int half = TEMPLATESIZE/2;
        double sum = 0;
        for(Integer i: s){
            ImageProcessor x = implus.getStack().getProcessor(i);
            double[] pt = s.getCoordinates(i);
            sum += 1;
            for(int j = 0;j<TEMPLATESIZE; j++){
                for(int k = 0; k<TEMPLATESIZE; k++){
                    template[j][k] += x.getInterpolatedPixel(k+pt[0] - half,j + pt[1] -  half);
                   
                }
            }
        }
        double f = 1/sum;
        for(int i = 0; i<TEMPLATESIZE; i++){
            for(int j = 0; j<TEMPLATESIZE; j++){
                template[i][j] = template[i][j]*f;
            }
        }
        
        templateStats();

    }
    
    /**
     * 
     *  For each frame in every speckle, sums all of the cropped images
     *  with the center at the location of the speckle and the size of 
     *  TEMPLATESIZE.  The result is an averaged image that corresponds 
     *  to the Template to be used for the normalized cross correlation.
     * 
     * @param   speckles - all of the speckles that are 'templated'
     * 
     **/
    public void createTemplate(HashSet<Speckle> speckles){
        template = new double[TEMPLATESIZE][TEMPLATESIZE];
        
        int half = TEMPLATESIZE/2;
        double sum = 0;
        for(Speckle s: speckles){
            for(Integer i: s){
                ImageProcessor x = implus.getStack().getProcessor(i);
                double[] pt = s.getCoordinates(i);
                sum += 1;
                for(int j = 0;j<TEMPLATESIZE; j++){
                    for(int k = 0; k<TEMPLATESIZE; k++){
                        template[j][k] += x.getInterpolatedPixel(k+pt[0] - half,j + pt[1] -  half);
                    
                    }
                }
            }
        }
        double f = 1/sum;
        for(int i = 0; i<TEMPLATESIZE; i++){
            for(int j = 0; j<TEMPLATESIZE; j++){
                
                template[i][j] = template[i][j]*f;
            
            }
        }
        
        templateStats();
    }
    
    /*
     * Required for using the filter, finds the average and the 
     * variance of the current template.
     * */
    private void templateStats(){
        double sum = 0;
        double sum_sqd = 0;
        for(int i = 0; i<TEMPLATESIZE; i++){
            for(int j = 0; j<TEMPLATESIZE; j++){
                double v = template[i][j];
                sum += v;
                sum_sqd += v*v;
                template[i][j] = v;
            }
        }
        double f = 1./(TEMPLATESIZE*TEMPLATESIZE);
        TAVE = sum*f;
        TSIGMA = Math.sqrt(sum_sqd*f - TAVE*TAVE);
    
    }
    
    public void setTemplate(double[][] template){
        
        this.template = template;
        
    }
    
    /*
     *  Performs the normalized cross correlation, the 'kernel' is 
     *  actually the template.  
     *  @param kernel - template
     *  @param ip - image processor storing the image
     *  @param x - x position of where filter will be applied
     *  @param y - y position. 
     **/
    public double filter( ImageProcessor ip, int x, int y){
        double sum = 0;
        double sum_squared = 0;
        int s = template.length/2;
        int N = template.length*template[0].length;
        //calculate mean
        for(int i = 0; i<template.length; i++){
            for(int j = 0; j<template[0].length; j++){
                double v = ip.getPixelValue(x -s +i, y - s +j);
                sum += v;
                sum_squared += v*v;
            }
        }
        double fave = sum/(N);
        double fsigma = Math.sqrt(sum_squared/N - fave*fave);
        
        double ret_value = 0;
        double denom = 1./(fsigma*TSIGMA);
        
        int width = ip.getWidth();
        int height = ip.getHeight();
        
        int l,m;
        
        for(int i = 0; i<template.length; i++){
            for(int j = 0; j<template[0].length; j++){
                l = x - s + i;
                m = y - s + j;
                if(l > 0 && l<width && m > 0 && m < height){ 
                
                    double v = ip.getPixelValue(l,m);
                    double t = template[j][i];
                    ret_value += (v - fave)*(t - TAVE)*denom;
                
                }
            }
        }

        return ret_value/(N-1);
    
    }
    
    public double filter( ImageProcessor ip, double x, double y){
        double sum = 0;
        double sum_squared = 0;
        double s = template.length/2.;
        int N = template.length*template[0].length;
        double[][] values = new double[template.length][template[0].length];
        //calculate mean
        for(int i = 0; i<template[0].length; i++){
            for(int j = 0; j<template.length; j++){
                double v = ip.getInterpolatedPixel(x -s +i, y - s +j);
                sum += v;
                sum_squared += v*v;
                values[j][i] = v;
            }
        }
        double fave = sum/(N);
        double fsigma = Math.sqrt(sum_squared/N - fave*fave);
        
        double ret_value = 0;
        double denom = 1./(fsigma*TSIGMA);

        for(int i = 0; i<template[0].length; i++){
            for(int j = 0; j<template.length; j++){
                
                double v = values[j][i];
                double t = template[j][i];
                ret_value += (v - fave)*(t - TAVE);
                
            }
        }

        return ret_value*denom/(N-1);
    
    }

    /**
       *    filters improc with the current template
       **/ 
    public void filterImage(ImageProcessor improc){
        ImageProcessor np = improc.duplicate();
        for(int i = 0; i<np.getWidth(); i++){
        
            for(int j= 0; j<np.getHeight(); j++){
                
                improc.putPixelValue(i,j,filter(np,i,j));
            
            }
            
        }
    
    }
    
     /**
       *    filters improc with the current includes intensity mments
       **/ 
    public void filterImage2(ImageProcessor improc){
        ImageProcessor np = improc.duplicate();
        for(int i = 0; i<np.getWidth(); i++){
        
            for(int j= 0; j<np.getHeight(); j++){
                
                improc.putPixelValue(i,j,filter(np,i,j)*improc.getf(i,j));
            
            }
            
        }
    
    }
    
    public void setTemplateSize(int i){
        
        TEMPLATESIZE = i;
        
    }
    
    /**
       *    Sets the statistics of a speckle to be in a specific location 
       **/
    public void speckleStats(HashSet<Speckle> speckles){
    
        double min = 2;
        double sum = 0;
        double sum_sqd = 0;
        double tally = 0;
        ImageStack imstack = implus.getStack();
        for(Speckle speck: speckles){
            for(Integer I: speck){
                double[] pt = speck.getCoordinates(I);
                ImageProcessor cp = imstack.getProcessor(I);
                double v = filter(cp,pt[0],pt[1]);
                min = (v>min)?min:v;
                sum += v;
                sum_sqd += v*v;
            
                tally++;
            }
        }
        double f = 1/tally;
        SPECKMEAN = sum*f;
        SPECKDEV = Math.sqrt(sum_sqd*f - SPECKMEAN*SPECKMEAN);
        SPECKMIN = min;
        
        
        
    }
    
}
