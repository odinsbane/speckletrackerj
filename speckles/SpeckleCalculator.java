package speckles;

import Jama.Matrix;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.gui.TextWindow;
import speckles.models.DiffusingSpotsModel;
import speckles.models.SpeckleEstimator;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A collection of methods and routines for collecting data, and calculating Intensities.
 *
 *
 * User: mbs207
 * Date: Sep 26, 2010
 * Time: 2:26:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpeckleCalculator {
    public static double INNER_RADIUS = 1;
    public static double OUTER_RADIUS = 3;

    public double size;
    public double thresh;
    public double average;
    public double variance;
    public double after;
    public double max_mean_displacement;

    public SpeckleCalculator(HashSet<Speckle> s, ImagePlus imp,double radius){

        averageValue(s,radius, imp);
        averageAfter(s,radius, imp);
        calculateMaxMeanDisplacement(s);


        thresh = average - variance;
        
    }

    /**
     * Average value of all speckles.
     *
     * @param speckles - the coordinate values that will be measured at.
     * @param radius - the size of circle that will be averaged over.
     * @param imp - that image stack that will be used.
     */
    public void averageValue(HashSet<Speckle> speckles,double radius, ImagePlus imp){
        double value = 0;
        double value_sqd = 0;
        int count = 0;
        for(Speckle s: speckles){

            for(int f: s){
                double[] pt = s.getCoordinates(f);
                ImageProcessor ip = imp.getStack().getProcessor(f);
                double v = averageValueCircle(pt,radius,ip);
                value += v;
                value_sqd += v*v;
            }
            count += s.getSize();


        }

        average = value/count;
        variance = Math.sqrt(value_sqd/count - average*average);


    }

    /**
     * Static version of above method, the size is the INNER_RADIUS value.
     *
     * @param speckles - set of speckle/coordinates taht will be sampled at.
     * @param imp - image stack.
     * @return average over the area of INNER_RADIUS.
     */
    public static double averageSpeckleValue(HashSet<Speckle> speckles, ImagePlus imp){
        double value = 0;
        int count = 0;
        for(Speckle s: speckles){

            for(int f: s){
                double[] pt = s.getCoordinates(f);
                ImageProcessor ip = imp.getStack().getProcessor(f);
                double v = averageValueCircle(pt,INNER_RADIUS,ip);
                value += v;
                count++;
            }


        }

        return value/count;


    }

    /**
     * The average intensity for an area immediately after a speckle track has ended.
     *
     * @param speckles - the speckles that will be measured.
     * @param radius - the size of the circle used for measuring.
     * @param imp - the stack of the images used for measuring.
     */
    public void averageAfter(HashSet<Speckle> speckles,double radius, ImagePlus imp){
        double value = 0;
        int count = 0;
        for(Speckle s: speckles){
            int f = s.getLastFrame();
            double[] pt = s.getCoordinates(f);
            if(f+1>imp.getStack().getSize())
                continue;
            count++;
            ImageProcessor ip = imp.getStack().getProcessor(f+1);
            value += averageValueCircle(pt,radius,ip);
        }
    

        after = value/count;


    }

    /**
     *  sets the max_mean_displacement value for all speckles...used in the batchlocate
     *  learn.
     *
     * @param speckles
     */
    public void calculateMaxMeanDisplacement(HashSet<Speckle> speckles){

        max_mean_displacement = 0;
        for(Speckle s: speckles){
            double d = calculateMeanDisplacement(s);
            max_mean_displacement = max_mean_displacement<d?d:max_mean_displacement;

        }


    }

    /**
     * Calculates the mean magintude of displacement
     * @param s the collection of coordinates that will be calculated.
     * @return
     */
    public static double calculateMeanDisplacement(Speckle s){
        double d = 0;
        if(s.getSize()>1){
            int first = s.getFirstFrame();
            int last = s.getLastFrame();

            double[] pt = s.getCoordinates(first);
            double[] opt = s.getCoordinates(last);
            d = Math.sqrt(Math.pow(pt[0] - opt[0],2) + Math.pow(pt[1] - opt[1],2))/(last - first);
        }
        return d;
    }

    /**
     * Removes speckles with zero points or points outside of the frame of
     * the image.
     *
     * @param speckles - set of speckles to be purged.
     * @param imp - the image data.
     */
    public void purgeSpeckles(HashSet<Speckle> speckles,ImagePlus imp){
        Iterator<Speckle> iter = speckles.iterator();

        while(iter.hasNext()){
            Speckle speckle = iter.next();

            int last = speckle.getLastFrame();
            ImageStack istack = imp.getStack();
            if(last>=istack.getSize())
                continue;

            ImageProcessor ipA = istack.getProcessor(last);
            ImageProcessor ipB = istack.getProcessor(last+1);

            double[] pt= speckle.getCoordinates(speckle.getLastFrame());

            double sumA = averageValueCircle(pt,INNER_RADIUS,ipA);
            double maxA = averageValueAnnulus(pt,INNER_RADIUS,OUTER_RADIUS,ipA);


            double sumB = averageValueCircle(pt,INNER_RADIUS,ipB);;
            double maxB = averageValueAnnulus(pt,INNER_RADIUS,OUTER_RADIUS,ipB);;

            double average_difference = (sumA - sumB);
            double max_difference = (maxA - maxB);


            double t = Math.abs(average - after);

            if(max_difference<t||average_difference>t){
                iter.remove();
            }
        }

    }

    /**
     * Uses one set of speckles as reference, and makes sure that the candidates do not
     * cross paths with the other speckles.
     * @param originals set that should not be modified
     * @param candidates set that has speckles removed
     * @param proximity minimum distance of approach.
     */
    public void removeCrossedPaths(HashSet<Speckle> originals, HashSet<Speckle> candidates, double proximity){


        //first check for candidates too close to originals.
        Iterator<Speckle> iter = candidates.iterator();
        while(iter.hasNext()){
            Speckle can = iter.next();
            for(Speckle ori: originals){
                if(ori==can)
                    continue;
                double d = closestApproach(can, ori);
                if(d<proximity){
                    iter.remove();
                    break;
                }

            }
        }

        ConcurrentLinkedQueue<Speckle> remaining = new ConcurrentLinkedQueue<Speckle>(candidates);
        HashSet<Speckle> remove = new HashSet<Speckle>();

        //go through and check for candidates that cross.

        Speckle a = remaining.poll();
        while(a!=null){
            for(Speckle s: candidates){

                if(s==a)
                    continue;
                double d = closestApproach(a,s);
                if(d<proximity){

                    if(a.getSize()>1 && s.getSize()>1){
                        double astep = stepSize(a);
                        double sstep = stepSize(s);
                        if(astep>sstep){
                            remove.add(a);
                        } else{
                            remove.add(s);
                        }
                    } else{
                        Speckle smaller = a.getSize()>s.getSize()?a:s;
                        remove.add(smaller);
                    }

                }

            }

            for(Speckle s: remove){
                remaining.remove(s);
                candidates.remove(s);
            }
            a = remaining.poll();
        }

    }

    /**
     * calculates the displacement for the first frame of the speckle (why?)
     * @param s - speckle that is checked
     * @return - distance moved after first frame(relative to speckle track)
     */
    public static double stepSize(Speckle s){
        if(s.getSize()>1){
            int i = s.getFirstFrame();
            double[] a = s.getCoordinates(i);
            double[] b = s.getCoordinates(i+1);
            return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2));
        }
        return 0;
    }

    /**
     * Calculates the closest distance the two speckles are relative to each other
     * when they exist in the same frame.
     *
     * @param a - speckle
     * @param b - other speckle
     * @return distance
     */
    public static double closestApproach(Speckle a, Speckle b){
        double closest = Double.MAX_VALUE;
        Speckle shorter = a.getSize()<b.getSize()?a:b;
        Speckle longer = shorter==a?b:a;
        for(int frame: shorter){
            if(longer.exists(frame)){
                double[] pt = shorter.getCoordinates(frame);
                double[] opt = longer.getCoordinates(frame);

                double v = Math.pow(pt[0] - opt[0],2) + Math.pow(pt[1] - opt[1],2);
                closest = v<closest?v:closest;


            }
        }
        return Math.sqrt(closest);
    }

    /**
     * Average intensity over a circle.
     * @param pt center of circle.
     * @param radius - radius of circle
     * @param ip - image data.
     * @return average intensity
     */
    public static double averageValueCircle(double[] pt, double radius, ImageProcessor ip){
        double n = radius*5;
        Ellipse2D el = new Ellipse2D.Double(pt[0] - radius,pt[1] - radius,2*radius, 2*radius);

        //double dr = radius/n;
        double dr = radius/n;

        double integral = 0;

        int count=0;

        for(double x = pt[0] - radius; x <= pt[0] + radius; x+=dr){
            for(double y = pt[1] - radius; y <= pt[1] + radius; y+=dr){
                if(el.contains(x,y)&&x>0&&y>0&&x<ip.getWidth()&&y<ip.getHeight()){

                    integral += ip.getInterpolatedValue(x,y);
                    count++;

                }
            }
        }
        integral = count>0?integral/count:0;
        return integral;
    }

    /**
     * Calculates the average value over an annulus.
     * @param pt center of two concentric circles.
     * @param inner the inner portion that is not counted.
     * @param outter the outter portion that is counted.
     * @param ip - the image data.
     * @return average intensity.
     */
    public static double averageValueAnnulus(double[] pt, double inner, double outter, ImageProcessor ip){
        double n = outter*5;
        Ellipse2D el = new Ellipse2D.Double(pt[0] - inner,pt[1] - inner,2*inner, 2*inner);
        Ellipse2D ot = new Ellipse2D.Double(pt[0] - outter,pt[1] - outter,2*outter, 2*outter);

        double dr = outter/n;

        double integral = 0;

        int count=0;

        for(double x = pt[0] - outter; x <= pt[0] + outter; x+=dr){
            for(double y = pt[1] - outter; y <= pt[1] + outter; y+=dr){
                if((!el.contains(x,y))&&ot.contains(x,y)&&x>0&&y>0&&x<ip.getWidth()&&y<ip.getHeight()){

                    integral += ip.getInterpolatedValue(x,y);
                    count++;

                }
            }
        }
        integral = count>0?integral/count:0;
        return integral;
    }

    /**
     * Create a text window with data that can be used to determine the best combination
     * of values for the diffusing spot model. These values do not work well.
     *
     * @param speckles - speckles to be measured.
     * @param ip - image stack
     */
    public static void estimateOptimalParameters(HashSet<Speckle> speckles, ImagePlus ip){
        ImageStack istack = ip.getStack();
        DiffusingSpotsModel m = new DiffusingSpotsModel(ip);
        m = (DiffusingSpotsModel)m.createModel(speckles);
        ArrayList<double[]> weights = new ArrayList<double[]>();
        for(Speckle s: speckles){

            m.evaluateSpeckleTrack(s,weights);

        }

        //try to find the combination of factors that improves the measurements.
        double[][] matters = new double[3][3];
        double[] soln = new double[3];
        for(double[] d: weights){

            for(int i = 0; i<3; i++){
                soln[i] += d[i];
                for(int j=0;j<3;j++){
                    matters[i][j] += d[i]*d[j];
                }
            }

        }

        Matrix x = new Matrix(matters);
        Matrix y = new Matrix(soln,3);
        Matrix z = x.solve(y);

        StringBuilder output = new StringBuilder();

        double ic = z.get(0,0);
        double dc = z.get(1,0);
        double cc = z.get(2,0);

        output.append("Intensity Criteria\t");
        output.append(ic);

        output.append("\tDisplacement Weight\t");
        output.append(dc);

        output.append("\tChange Criteria\t");
        output.append(cc);
        output.append('\n');


        HashMap<String,Double> parameters = m.getParameters();
        parameters.put("Intensity Criteria",ic);
        parameters.put("Displacement Weight",dc);
        parameters.put("Change Criteria",cc);
        //m.setParameters(parameters);

        weights.clear();
        double average_tally = 0;
        double average_after = 0;
        for(Speckle s: speckles){

            SpeckleEstimator se = new SpeckleEstimator(s);
            int l = se.getLastFrame();
            if(l==istack.getSize())
                continue;


            ArrayList<double[]> pts = m.predictSpeckle(se,l+1);
            if(pts.size()==0){
                pts.add(se.getLastCoordinates());
            }
            double[] pt2 = se.getLastCoordinates();

            double max = 0;
            double after = 0;

            for(double[] pt: pts){


                double p = SpeckleCalculator.averageValueCircle(pt, SpeckleCalculator.INNER_RADIUS,istack.getProcessor(l+1));

                double displacement = DiffusingSpotsModel.rootDisplacement(pt, pt2);
                double delta = p - SpeckleCalculator.averageValueCircle(pt2, SpeckleCalculator.INNER_RADIUS,istack.getProcessor(l));

                double[] w = m.modelCriteria(p, delta, displacement);
                if(w[0]>max){
                    max=w[0];
                    after = p;
                }
            }
            average_after += after;
            average_tally += 1;
            weights.clear();
            m.evaluateSpeckleTrack(s,weights);
            double worse = 2;
            for(double[] w: weights){

                double value = m.getMainWeight(w[0],w[1],w[2]);

                if(value<worse)
                    worse=value;

            }

            output.append("#best position after fail: " + max + "\tworste value accepted:\t" +worse + "\tfirst frame\t" +s.getFirstFrame()+"\tsize\t" + s.getSize());
            output.append("\n");
        }
        if(average_tally>0)
            average_after = average_after/average_tally;
        output.append( "average value of speckles\t"+averageSpeckleValue(speckles,ip)+"average value of best position:\t" + average_after + '\n');

        EventQueue.invokeLater(new TextWindow("learn data",output.toString()));
    }
}


