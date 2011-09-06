package speckles.utils.leastsquares;

import Jama.Matrix;
 /**
 * for solving linear least squares fit of f(x:A) = z with sets of x,z data points.
  * derivatives are evaluated numerically and the function is assumed to be linear in A.
  * Then a linear least squares fit is performed.
 * */
public class LinearFitter implements Fitter {

    double[][] X;    //values
    double[]   A,    //Parameter Set
               Z;    //output vaues

    Function FUNCTION;
    double[] ERROR;

    double[][] DERIVATIVES;
    double[] BETA;
    double[][] ALPHA;
    double DELTA = 0.000001;   //used for calculating derivatives

    public LinearFitter(Function funct){

        FUNCTION=funct;

    }
    /**
     *      Sets the values of of the original data points that are going to be fit.
     * */
    public void setData(double[][] xvalues, double[] zvalues){

        if(xvalues.length != zvalues.length)
            throw new IllegalArgumentException("there must be 1 z value for each set of x values");
        else if(xvalues[0].length != FUNCTION.getNInputs())
            throw new IllegalArgumentException("The length of parameters is longer that the parameters accepted by the function");
        X = xvalues;
        Z = zvalues;

    }

    /**
     *
     * */
    public void setParameters(double[] parameters){
        if(parameters.length != FUNCTION.getNParameters())
            throw new IllegalArgumentException("the number of parameters must equal the required number for the function: " + FUNCTION.getNParameters());
        A = new double[parameters.length];
        System.arraycopy(parameters,0,A,0,parameters.length);
    }

     /**
      *     returns the cumulative error for current values
      **/
    public double calculateErrors(){
        double new_error = 0;
        for(int i = 0; i<Z.length; i++){

            double v = FUNCTION.evaluate(X[i],A);
            ERROR[i] = Z[i] - v;
            new_error += Math.pow(ERROR[i],2);
        }
        return new_error;

    }

    /**
     *  Given a set of parameters, and inputs, calculates the derivative
     *  of the k'th parameter
     *  d/d a_k (F)
     *
     * @param k - index of the parameter that the derivative is being taken of
     * @param params - array that will be used to calculate derivatives, note params will be modified.
     * @param x - set of values to use.
     * @return derivative of function
     **/
    public double calculateDerivative(int k, double[] params, double[] x){
        double b, a;

        params[k] -= DELTA;
        b = FUNCTION.evaluate(x, params);
        params[k] += 2*DELTA;
        a = FUNCTION.evaluate(x, params);
        params[k] -= DELTA;
        return (a - b)/(2*DELTA);

    }

    /**
     *  Creates an array of derivatives since each one is used 3x's
     **/
    public void calculateDerivatives(){
        double[] working = new double[A.length];
        System.arraycopy(A,0,working,0,A.length);
        for(int j = 0; j<A.length; j++){

            for(int i = 0; i<Z.length; i++){
                DERIVATIVES[i][j] = calculateDerivative(j, working, X[i]);
            }
        }


    }


    public void createBetaMatrix(){
        BETA = new double[A.length];
        double[] working = new double[A.length];
        System.arraycopy(A,0,working,0,A.length);

        for(int k = 0; k<BETA.length; k++){
            for(int i = 0; i<X.length; i++){

                BETA[k] += ERROR[i]*DERIVATIVES[i][k];

            }
        }

    }

    public void createAlphaMatrix(){
        ALPHA = new double[A.length][A.length];

        int n = A.length;
        for(int k = 0; k<n; k++){
            for(int l = 0; l<n; l++){

                    for(int i = 0; i<X.length; i++)
                        ALPHA[l][k] += DERIVATIVES[i][k] * DERIVATIVES[i][l];

            }

        }



    }

    /**
     *  Takes the current error, and the current parameter set and calculates the
     *  changes, then returns the maximum changed value
     * */
    public void iterateValues(){
        calculateErrors();
        calculateDerivatives();

        createBetaMatrix();
        createAlphaMatrix();

        Matrix alpha_matrix = new Matrix(ALPHA);
        Matrix beta = new Matrix(BETA, BETA.length);
        
        Matrix out = alpha_matrix.solve(beta);

        double[][] delta_a = out.getArray();

        for(int i = 0; i<A.length; i++)
            A[i] += delta_a[i][0];

    }

    public void printMatrix(){
        for(int i = 0; i<ALPHA.length; i++){
            for(int j = 0; j<ALPHA[0].length; j++){
                System.out.print(ALPHA[i][j] + "\t");

            }

            System.out.println("| " + BETA[i] );
        }
    }

    public void initializeWorkspace(){
        ERROR = new double[Z.length];
        DERIVATIVES = new double[Z.length][A.length];
    }

    /**
     *  Main routine, call this and the Parameters are iterated one time because
     *  the equations are assumed to be linear in parameter space.
     * */
    public void fitData(){
        initializeWorkspace();

        try{
          iterateValues();

        } catch(Exception exc){
            printMatrix();
            exc.printStackTrace();

         }

    }
    /**
     *      Gets the current set of parameters values.
     * */
    public double[] getParameters(){
        return A;
    }

}