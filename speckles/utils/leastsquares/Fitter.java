package speckles.utils.leastsquares;

public interface Fitter {
    
    public void setData(double[][] xvalues, double[] zvalues);
    public void setParameters(double[] parameters);
    public double[] getParameters();
    public void fitData();
    public double calculateErrors();
}
