package speckles.utils;

import Jama.LUDecomposition;
import Jama.Matrix;
/** Class implements the Savitzy-Golay filter.  The algorithm is based on
 * the algorithm used in 
 * 
 * Numerical Methods in C: The art of scientific computing second edition
 * 
 * This filter is used for smoothing data.  The current implementation is
 * a 1-d approach only.
 * 
 **/ 
public class SavitzyGolayFilter{
    double[] coefficients;
    int LEFT, RIGHT, ORDER;
    /**
     * Prepares the filter coefficients
     * 
     * @param left number of places to the left of the origion
     * @param right number of places to the right of the origion
     * @param order of polynomial used to fit data
     * */
    public SavitzyGolayFilter(int left, int right, int order){
        
        coefficients = SavitzyGolayCoefficients(left,right,order);
        LEFT = left;
        RIGHT = right;
        ORDER = order;
    }
    
    /**
     * filters the data by assuming the ends are reflected.
     * @return new array containing a filtered version of the data.
     * */
    public double[] filterData(double[] data){
        int pts = data.length;
        double[] ret_value = new double[pts];
        
        int j;
        //reflected
        for(j = 0; j<LEFT; j++)
            ret_value[j] = rFilter(data, coefficients, j, LEFT);
        
        
        //normal
        for(j = LEFT; j<pts - RIGHT; j++)
            ret_value[j] = filter(data, coefficients, j, LEFT);
            
        
        //reflected
        for(j = pts - RIGHT; j<pts; j++)
            ret_value[j] = rFilter(data, coefficients, j, LEFT);
        
        return ret_value;
    }
        
        
    
    public float filter(float[] o, double[] mask, int start, int middle){
        float out = 0;
        for(int i = 0; i<mask.length; i++)
            out += (float)mask[i]*o[start - middle + i];
            
        return out;
        
    }
    
    public double filter(double[] o, double[] mask, int start, int middle){
        double out = 0;
        for(int i = 0; i<mask.length; i++)
            out += mask[i]*o[start - middle + i];
            
        return out;
        
    }
    
    //relfecting filter has conditionals so it is slower.  Assumes the mask is shorter than the object.
    public double rFilter(double[] o, double[] mask, int start, int middle){
        double out = 0;
        int dex;
        for(int i = 0; i<mask.length; i++){
            dex = Math.abs(start - middle + 1 +i);
            dex = (dex<o.length)?dex: 2*o.length - dex - 1;
            out += mask[i]*o[dex];
        }
            
        return out;
        
    }
    
    /**
     *   Creates the array of coefficients for using a least squares polynomial fit
     *   of the data.  Due to the fact the points are equally spaced 
     *   and the polynomial least squares is linear in the coefficients,
     *   a set of coefficients can be used for every element.
     *   @param nl - spaces to the left
     *   @param nr - spaces to the right
     *   @param order - the order of the polynomial being used, not that nl+nr+1 must be more than the order of the polynomial
     * */
    
    
    public static double[] SavitzyGolayCoefficients(int nl, int nr, int order){
        int N = nr + nl + 1;
        double[] ret_values = new double[N];
        double[] xvalues = new double[N];
        for(int i = 0; i<N; i++){
            
            xvalues[i] = -nl + i;
            
        }
        
        int counts = 2*order+1;
        double[] moments = new double[counts];
        for(int i = 0; i<counts; i++){
            for(int j = 0; j<N; j++){
                
                moments[i] += Math.pow(xvalues[j],i);
                
            }
            
            moments[i] = moments[i]/N;
        }
        
        
        
        double[][] matrix = new double[order+1][order+1];
        
        for(int i = 0; i<order+1; i++){
            for(int j = 0; j<order+1; j++){
                matrix[i][j] = moments[counts - i - j - 1];
            }
        }
        
        Matrix A = new Matrix(matrix);
       
        LUDecomposition lu = A.lu();
       
        Matrix x = new Matrix(new double[order+1],order+1);
        Matrix y;
        double[] polynomial;
       
        for(int i = 0; i<N; i++){
            
            for(int j = 0; j<order+1; j++)
                x.set( j , 0, Math.pow(xvalues[i],order - j));
            
            y = lu.solve(x);
            
            polynomial = y.getColumnPackedCopy();
            ret_values[i] = evaluatePolynomial(polynomial, xvalues[nl])/N;
        }
        
        
        return ret_values;
        
    }
    /**
     *      Evaluates a polynomial where:
     *      p(x) = poly[0] * x**m + poly[1] * x**(m-1) + ... + poly[m]
     *      
     *  @param poly - double array representation of a polynomial
     *  @param x - the variable that will be evaluated. 
     **/
    public static double evaluatePolynomial(double[] poly, double x){
        
        double val = 0;
        int m = poly.length;
        for(int j = 0; j<m; j++){
            val += Math.pow(x,m-j-1)*poly[j];
        }
        return val;
    }
}
