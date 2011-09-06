package speckles.controls;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleCalculator;

import java.util.Iterator;

/**
 * This class is used by the speckle collector for storing data.
 *
 * User: mbs207
 * Date: Sep 26, 2010
 * Time: 11:27:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class SpeckleHost{
    int id;
    Speckle speckle;
    double[] measurements;
    public SpeckleHost(Speckle guest, int id){
        speckle = guest;
        this.id = id;
        measurements = new double[5];

    }

    int getFirstFrame(){
        return speckle.getFirstFrame();
    }

    int getLastFrame(){
        return speckle.getLastFrame();
    }


    /**
     * This allows the speckle to be updated so that we can see properties of it in
     * the columns.  currently
     *
     * column zero - change in peak intensity.
     * column one - change in average intensity, 5px x 5px.
     * @param imp
     */
    public void measureAppearanceProbabilities(ImagePlus imp, SpeckleApp app){
        int last = speckle.getLastFrame();
        ImageStack istack = imp.getStack();
        if(last<istack.getSize()){

            ImageProcessor ipA = istack.getProcessor(last);
            ImageProcessor ipB = istack.getProcessor(last+1);

            double sumA = 0;
            double maxA = 0;

            double sumB = 0;
            double maxB = 0;

            double[] pt= speckle.getCoordinates(speckle.getLastFrame());

            double before = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ipA);
            double after = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ipB);
            measurements[0] = before - after;

            before = SpeckleCalculator.averageValueAnnulus(pt,SpeckleCalculator.INNER_RADIUS,SpeckleCalculator.OUTER_RADIUS,ipA);
            after = SpeckleCalculator.averageValueAnnulus(pt,SpeckleCalculator.INNER_RADIUS,SpeckleCalculator.OUTER_RADIUS,ipB);
            measurements[1] = before - after;

            /*
            int count = 10;

            for(int i = -count/2; i<=count/2; i++){
                for(int j = -count/2; j<=count/2; j++){

                    double a = ipA.getInterpolatedValue(pt[0] + i, pt[1] + j);
                    sumA += a;
                    if(maxA<a){
                        maxA = a;
                    }

                    double b = ipB.getInterpolatedValue(pt[0] + i, pt[1] + j);
                    sumB += b;
                    if(maxB<b){
                        maxB = b;
                    }
                }
            }

            measurements[0] = (sumA - sumB)/(Math.pow(count + 1,2));
            measurements[1] = maxA - maxB;
            */
        }
        if(speckle.getSize()>1){
            measurements[2] = 0;
            Iterator<Integer> iter = speckle.iterator();
            double[] prev = speckle.getCoordinates(iter.next());
            while(iter.hasNext()){
                double[] n = speckle.getCoordinates(iter.next());
                double d = Math.sqrt(Math.pow(n[0] - prev[0], 2) + Math.pow(n[1] - prev[1],2));
                measurements[2] = d>measurements[2]?d:measurements[2];
                prev = n;
            }

        }

        double[] v = new double[1];
        app.getClosestSpeckle(speckle,v);
        measurements[3] = v[0];

    }

    public double getValue(int i){

        return measurements[i];

    }

}
