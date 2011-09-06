package speckles.utils.leastsquares;

/**
 * Created by IntelliJ IDEA.
 * User: mbs207
 * Date: Jun 3, 2010
 * Time: 6:35:14 AM
 */
public class Exponential implements Function{

    /**
     * Evaluates the function
     * @param values {x}
     * @param parameters { A, lambda, B}
     * @return f(x)
     */
    public double evaluate(double[] values, double[] parameters) {
        return parameters[0]*Math.exp(-values[0]/parameters[1]) + parameters[2];
    }

    public int getNParameters() {
        return 3;
    }

    public int getNInputs() {
        return 1;
    }
}
