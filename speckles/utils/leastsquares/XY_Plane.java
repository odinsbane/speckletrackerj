package speckles.utils.leastsquares;

/**
 * Created by IntelliJ IDEA.
 * User: mbs207
 * Date: Jun 3, 2010
 * Time: 9:26:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XY_Plane implements Function{

    /**
     * Ax + By + C
     * @param values {x, y}
     * @param parameters {A,B,C}
     * @return f(x,y)
     */
    public double evaluate(double[] values, double[] parameters) {
        return parameters[0]*parameters[0]*values[0] + parameters[1]*values[1] + parameters[2];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getNParameters() {
        return 3;
    }

    public int getNInputs() {
        return 2;
    }
}
