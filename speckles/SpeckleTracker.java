package speckles;

import ij.ImagePlus;
import speckles.gui.DataTableWindow;
import speckles.models.SpeckleEstimator;
import speckles.models.SpeckleModel;

import java.util.ArrayList;
import java.util.HashSet;


public class SpeckleTracker implements Runnable{
    
    final public static int STATE_TRACK = 1;
    final public static int SINGLE_TRACK = 0;
    public static int MAX_FAIL=0;
    ImagePlus implus;
    
    SpeckleModel SPECKLE_MODEL, CANONICAL_MODEL;

    //for running
    Speckle speckle;
    HashSet<Speckle> all_speckles;
    HashSet<Speckle> trackable_speckles;
    int start;
    int TYPE;

    static double MIN_TRACK_SEPARATION = 2;
    public SpeckleTracker(ImagePlus implus, SpeckleModel model){
        this.implus = implus;
        CANONICAL_MODEL = model;
    }
    
    /**
     *  Creates a model based on this speckle.  This is the model that
     *  will be used until changed. 
     *     
     **/
    public void trainModel(Speckle speck){
        
        SPECKLE_MODEL = CANONICAL_MODEL.createModel(speck);

    }
        
    
    public void createModel(HashSet<Speckle> speckles){
        
        SPECKLE_MODEL = CANONICAL_MODEL.createModel(speckles);

    }
    
    /**
     *      Uses the current model and trys to place the speckle in the
     *      next location. 
     **/
    
    public boolean trackSpeckle(SpeckleEstimator speck){
            int n = speck.getLastFrame() + 1;



            SPECKLE_MODEL.estimateLocation(speck, n);
            return speck.isWorking();

    
    }
    
    
    /**
       *    give x,y's returns {slope, intercept}
       **/
    public static double[] leastSquares(double[] x, double[] y){
    
        double xbar = 0;
        double x2bar = 0;
        double xybar = 0;
        double ybar = 0;
        for(int i = 0; i<x.length; i++){
            xbar += x[i];
            x2bar += x[i]*x[i];
            xybar += x[i]*y[i];
            ybar += y[i];
        }
        
        double m = (xybar*x.length - xbar*ybar)/(x2bar*x.length - xbar*xbar);
        double b = (ybar- m*xbar)/x.length;
        return new double[] {m , b};
    }
    
    
    
    public HashSet<Speckle> mergeFrames(){
    
        return new HashSet<Speckle>();
    }
    
    /**
     *      This operation needs to go through each speckle to develop the criteria for tracking , then 
     *      once all of the criteria have been established the speckles need to see, who gets the new spot
     *      once that as been determined.  Everything will be done on the speckle estimator and not on the 
     *      actual speckles.
     *
     * @param all_speckles the speckles that the model is built off of, these should not be modified.
     * @param tracking_speckles the speckles that can be modified.
     * @param implus image to track too.
     * @param mod model that will be used to track speckles.
     * @param start frame where tracking begins.
     * @return
     */
    public static SpeckleTracker stateTrackSpeckle(HashSet<Speckle> all_speckles,HashSet<Speckle> tracking_speckles, ImagePlus implus, SpeckleModel mod, int start){
        SpeckleTracker tracker = new SpeckleTracker(implus, mod);
        tracker.all_speckles = all_speckles;
        tracker.trackable_speckles = tracking_speckles;
        tracker.start = start;
        tracker.TYPE = STATE_TRACK;
        return tracker;
    }

    public static SpeckleTracker measureSpeckles(HashSet<Speckle> all_speckles,Speckle selected, ImagePlus implus, SpeckleModel mod, int start){

        SpeckleTracker tracker = new SpeckleTracker(implus, mod){
            public void run(){
                this.runMeasureSpeckles();
            }
        };
        tracker.all_speckles = all_speckles;
        tracker.trackable_speckles = new HashSet<Speckle>();
        if(selected!=null)
            tracker.trackable_speckles.add(selected);
        else{
            System.out.println("null");
        }

        tracker.start = start;
        tracker.TYPE = STATE_TRACK;
        return tracker;

    }

    /**
     * Checks the last frame of the estimators to see if they have crossed
     * while being tracked.
     * 
     * @param a
     * @param b
     */
    public static void compareEstimators(SpeckleEstimator a, SpeckleEstimator b){

        if(a.isWorking() && b.isWorking()){
            boolean too_close = false;
            int i = a.getLastFrame();
            boolean test = true;
            while(test){
                if(b.exists(i)){
                    double[] pta = a.getCoordinates(i);
                    double[] ptb = b.getCoordinates(i);
                    double d = Math.sqrt(Math.pow(pta[0] - ptb[0], 2) + Math.pow(pta[1] - ptb[1], 2));
                    if(d<MIN_TRACK_SEPARATION){
                        too_close = true;
                        break;
                    }


                }

                int j = b.getLastFrame();

                //they should have the same last frame, but just in case.
                if(j==i){
                    test = false;
                } else{
                    i = j;
                }

            }
            if(too_close){
                if(a.getFail()!=b.getFail()){
                    if(a.getFail()>b.getFail())
                        a.end();
                    else
                        b.end();
                } else{
                    
                    int n = a.getLastFrame();
                    double[] aw = a.getWeight(n);
                    double[] bw = b.getWeight(n);

                    //compare the weights of the two estimators.
                    boolean t = aw[0]*(aw[1] + 1) < bw[0]*(bw[1]+1) ;
                    if(t)
                        a.end();
                    else
                        b.end();
                }
                    
            }


        }
    }

  /**
     * Checks the last frame to see if these speckles cross
     * 
     * @param a
     * @param b
     */
    public static void compareEstimatorAndSpeckle(SpeckleEstimator a, Speckle b){
        if(a.isWorking()){
            boolean too_close = false;

            int i = a.getLastFrame();
            if(b.exists(i)){
                double[] pta = a.getCoordinates(i);
                double[] ptb = b.getCoordinates(i);
                double d = Math.sqrt(Math.pow(pta[0] - ptb[0], 2) + Math.pow(pta[1] - ptb[1], 2));

                too_close = d<MIN_TRACK_SEPARATION;


            }

            if(too_close){

                //a has found a point that is already established as a speckle
                if(b.getSize()==1){
                    b.clear();
                    a.success();
                } else{

                    //a has crossed an existing track.  Fail this frame and end speckle.
                    if(a.getFail()==0)
                        a.fail(i);
                    a.end();
                }
                    
            }
        }
    }
    
   public static SpeckleTracker autoTrackSpeckle(Speckle speck, ImagePlus implus, SpeckleModel sm){
        SpeckleTracker tracker = new SpeckleTracker(implus, sm);
        tracker.speckle = speck;
        tracker.trainModel(speck);
        tracker.TYPE = SINGLE_TRACK;
        return tracker;

    }

    public void run(){
        switch(TYPE){
            case SINGLE_TRACK:
                runSingleTrack();
                break;
            case STATE_TRACK:
                runStateTrack();
                break;
        }
    }

    private void runSingleTrack(){
        boolean track = true;
        SpeckleEstimator speckle_estimator = new SpeckleEstimator(speckle);

        while(track){
            track = trackSpeckle(speckle_estimator);

            if(speckles.SpeckleApp.isStopped()) break;

        }

        speckle_estimator.updateSpeckle();

    }

    private void runStateTrack(){
        if(CANONICAL_MODEL.modelType()==SpeckleModel.REFINE_MODEL){
            refineStateTrack();
            return;
        }
        HashSet<Speckle> tracking_speckles = new HashSet<Speckle>(trackable_speckles);
        boolean track = true;
        createModel(all_speckles);

        HashSet<SpeckleEstimator> working_estimators = new HashSet<SpeckleEstimator>();
        HashSet<Speckle> not_tracking = new HashSet<Speckle>();

        //divide the speckles into - not tracking speckles and tracking.
        SPECKLE_MODEL.prepareSpeckles(tracking_speckles,not_tracking,start);

        for(Speckle s: tracking_speckles){

            working_estimators.add(new SpeckleEstimator(s));
        }
        
        for(Speckle s: all_speckles){
            if(!tracking_speckles.contains(s)){
                not_tracking.add(s);
            }


        }


        //outter loop as long as a speckle is tracked.

        HashSet<SpeckleEstimator> finished_estimators = new HashSet<SpeckleEstimator>();
        HashSet<SpeckleEstimator> transient_estimators = new HashSet<SpeckleEstimator>();

        int working_dex = start + 1;

        while(track){
            track = false;

            for(SpeckleEstimator estimator: working_estimators){
                if(estimator.isWorking()){
                    SPECKLE_MODEL.estimateLocation(estimator, working_dex);
                    track = true;
                } else{
                    finished_estimators.add(estimator);
                }
            }

            for(SpeckleEstimator estimator: finished_estimators){
                if(working_estimators.contains(estimator)){
                    working_estimators.remove(estimator);
                    not_tracking.add(estimator.getSpeckle());
                    estimator.updateSpeckle();
                }
            }

            transient_estimators.clear();

            for(SpeckleEstimator working: working_estimators){
                for(SpeckleEstimator other: transient_estimators){
                    compareEstimators(working,other);
                }
                transient_estimators.add(working);
            }


            for(SpeckleEstimator sest: working_estimators){
                for(Speckle s: not_tracking){

                    compareEstimatorAndSpeckle(sest, s);

                }
            }



            working_dex += 1;
            if(working_dex>SpeckleApp.getSlices(implus)){



            }
            if(SpeckleApp.isStopped()) break;

        }

    }


    //refine models
    private void refineStateTrack(){
        HashSet<Speckle> tracking_speckles = new HashSet<Speckle>(trackable_speckles);

        createModel(all_speckles);

        HashSet<Speckle> not_tracking = new HashSet<Speckle>();

        //divide the speckles into - not tracking speckles and tracking.
        SPECKLE_MODEL.prepareSpeckles(tracking_speckles,not_tracking,start);
        int working_dex = start + 1;

        for(Speckle s: tracking_speckles){
            SpeckleEstimator e = new SpeckleEstimator(s);
            SPECKLE_MODEL.estimateLocation(e, working_dex);
            e.updateSpeckle();
        }
        
    }

    /**
     * Goes through each speckle applies the model and then creates a data
     * table to show the model weights and values.
     *
     */
    void runMeasureSpeckles(){

        createModel(all_speckles);
        DataTableWindow dtw = new DataTableWindow("Speckle Values");

        for(Speckle s: trackable_speckles){
            ArrayList<Double> xs = new ArrayList<Double>();
            ArrayList<Double> ys = new ArrayList<Double>();
            ArrayList<Double> prob = new ArrayList<Double>();
            ArrayList<ArrayList<Double>> additional_weights = new ArrayList<ArrayList<Double>>();
            ArrayList<Double> frame = new ArrayList<Double>();
            SpeckleEstimator e = new SpeckleEstimator(s);

            boolean first = true;
            for(int i: e){

                //estimates the location. Refine models will do everything the first time
                if(e.isWorking())
                    SPECKLE_MODEL.estimateLocation(e,i);
                
                double[] weights = e.getWeight(i);
                if(first){
                    int n = weights.length-1;
                    for(int j = 0; j<n; j++){
                        additional_weights.add(new ArrayList<Double>());
                    }
                    first=false;
                }
                double[] position = e.getCoordinates(i);

                xs.add(position[0]);
                ys.add(position[1]);
                prob.add(weights[0]);
                for(int j = 0; j<additional_weights.size(); j++){
                    final ArrayList<Double> w = additional_weights.get(j);
                    w.add(weights[1+j]);
                }
                frame.add((double)i);

            }

            dtw.addColumn("frame",frame);
            dtw.addColumn("x",xs);
            dtw.addColumn("y",ys);
            dtw.addColumn("probability",prob);
            for(int j = 0; j<additional_weights.size(); j++){
                final ArrayList<Double> w = additional_weights.get(j);
                dtw.addColumn("weight-" + j,w);
            }

            


        }

        java.awt.EventQueue.invokeLater(dtw);



    }

}
