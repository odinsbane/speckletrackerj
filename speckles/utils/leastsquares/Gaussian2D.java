package speckles.utils.leastsquares;

public class Gaussian2D implements Function{
    /**
     *  A 2-D gaussian with a center at x0, y0
     *  p[0] * exp( -((v[0] - p[2])^2 + (v[1] - p[3])^2)/(2*p[4])) + p[1]
     * 
     *  @param v  { x, y}
     *  @param p  { A, B, x0, y0, sigma }
     * 
     **/
    public double evaluate(double[] v, double[] p){
        
        return p[0] * Math.exp( -(Math.pow(v[0] - p[2],2) + Math.pow(v[1] - p[3],2))/(2*Math.pow(p[4],2))) + p[1];
        
        
    }
    public int getNParameters(){
        return 5;
    } 
    public int getNInputs(){
        return 2;
    }
    
    public static Function findAB(double x,double y,double sigma){
        
        return new Gaussian2DAB(x,y,sigma);
        
    }
    public static Function findSigma(double x ,double y ,double A ,double B){
        return new Gaussian2DSigma(x,y,A,B);
    }
            
    public static Function findXY(double A,double B,double sigma){
        
        return new Gaussian2DXY(A,B,sigma);
    }
            
            
}


class Gaussian2DAB implements Function{
    
    double X0, Y0, SIGMA;
    
    Gaussian2DAB(double x, double y, double sigma){
            X0 = x;
            Y0 = y;
            SIGMA = sigma;
    }
    /**
     *  A 2-D gaussian with a center at x0, y0
     *  p[0] * exp( -((v[0] - p[2])^2 + (v[1] - p[3])^2)/(2*p[4])) + p[1]
     * 
     *  @param v  { x, y}
     *  @param p  { A, B}
     * 
     **/
    public double evaluate(double[] v, double[] p){
        
        return p[0] * Math.exp( -(Math.pow(v[0] - X0,2) + Math.pow(v[1] - Y0,2))/(2*Math.pow(SIGMA,2))) + p[1];
        
        
    }
    public int getNParameters(){
        return 2;
    } 
    public int getNInputs(){
        return 2;
    }
            
}

class Gaussian2DSigma implements Function{
    
    double X0, Y0, A, B;
    
    Gaussian2DSigma(double x, double y, double a, double b){
            X0 = x;
            Y0 = y;
            A = a; 
            B = b;
    }
    /**
     *  A 2-D gaussian with a center at x0, y0
     *  p[0] * exp( -((v[0] - p[2])^2 + (v[1] - p[3])^2)/(2*p[4])) + p[1]
     * 
     *  @param v  { x, y}
     *  @param p  { A, B}
     * 
     **/
    public double evaluate(double[] v, double[] p){
        
        return A * Math.exp( -(Math.pow(v[0] - X0,2) + Math.pow(v[1] - Y0,2))/(2*Math.pow(p[0],2))) + B;
        
        
    }
    public int getNParameters(){
        return 1;
    } 
    public int getNInputs(){
        return 2;
    }
            
}

class Gaussian2DXY implements Function{
    
    double A, B, SIGMA;
    
    Gaussian2DXY(double a, double b, double sigma){
            A = a;
            B = b;
            SIGMA = sigma;
    }
    /**
     *  A 2-D gaussian with a center at x0, y0
     *  p[0] * exp( -((v[0] - p[2])^2 + (v[1] - p[3])^2)/(2*p[4])) + p[1]
     * 
     *  @param v  { x, y}
     *  @param p  { A, B}
     * 
     **/
    public double evaluate(double[] v, double[] p){
        
        return A * Math.exp( -(Math.pow(v[0] - p[0],2) + Math.pow(v[1] - p[1],2))/(2*Math.pow(SIGMA,2))) + B;
        
        
    }
    public int getNParameters(){
        return 2;
    } 
    public int getNInputs(){
        return 2;
    }
            
}
