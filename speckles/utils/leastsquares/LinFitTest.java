package speckles.utils.leastsquares;

/**
 * Created by IntelliJ IDEA.
 * User: mbs207
 * Date: Sep 7, 2010
 * Time: 9:42:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class LinFitTest {
    public static void main(String[] args){
        double[] z = {10,10,10,10,20,10,10,10,10};
        double[][] xy = {{-1,-1},
                         {0,-1},
                         {1,-1},
                         {-1,0},
                         {0,0},
                         {1,0},
                         {-1,1},
                         {0,1},
                         {1,1}};

        Function g = new gaussian(0,0,0.25);
        Fitter f = new LinearFitter(g);
        f.setData(xy,z);
        f.setParameters(new double[] {1,1});
        f.fitData();

        for(double d: f.getParameters())
            System.out.println(d);




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
