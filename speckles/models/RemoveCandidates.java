package speckles.models;

import speckles.Speckle;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

/**
 *
 *
 * User: mbs207
 * Date: 2/6/11
 * Time: 12:27 PM
 */
public class RemoveCandidates extends SpeckleModel {

    private static int MINIMUM_DURATION = 2;
    private static double MAXIMUM_MEAN_DISPLACEMENT = 1.0;
    private static int LINK_FRAMES = 1;
    private static double LINK_RADIUS = 0.5;
    HashSet<Speckle> speckles = new HashSet<Speckle>();
    HashSet<Speckle> remove = new HashSet<Speckle>();

    @Override
    public SpeckleModel createModel(Speckle s) {
        return new RemoveCandidates();
    }

    @Override
    public SpeckleModel createModel(HashSet<Speckle> specks) {
        RemoveCandidates rc = new RemoveCandidates();
        rc.implus = implus;



        if(LINK_RADIUS>0&LINK_FRAMES>0)
            rc.linkSpeckles(specks);



        return rc;
    }

    /**
     * Goes through the set of speckles and attempts to 'link them' by looking for
     * speckles that ended before the current speckle started.  The speckles will be
     * prepared for removal, but not actually removed until the 'estimate location'
     * is used.
     *
     * @param specks all of the speckles that are used to create the model.
     */
    void linkSpeckles(HashSet<Speckle> specks){
        remove.clear();
        HashSet<Speckle> transients = new HashSet<Speckle>(specks);

        //as linkable cadidates are found they need to be sorted by last frame.
        //the highest last frame first.
        TreeSet<Speckle> linkable = new TreeSet<Speckle>(new Comparator<Speckle>(){
            public int compare(Speckle o1, Speckle o2) {

                return o2.getLastFrame() - o1.getLastFrame();
            }
        });


        double min_d_squared = Math.pow(LINK_RADIUS,2);

        for(Speckle s: specks){
            if(remove.contains(s))
                continue;
            linkable.clear();
            int n = s.getFirstFrame();

            //starts on first frame, no linking.
            if(n==1)
                continue;

            double[] spt = s.getCoordinates(n);
            for(Speckle o: transients){
                //o shouldn't be in here.
                if(o==s)
                    continue;

                int m = o.getLastFrame();

                //criteria to be linkable, within min_d and ends before this starts.
                if(m<n){
                    double[] pt = o.getCoordinates(m);
                    double d = Math.pow(spt[0] - pt[0],2) + Math.pow(spt[1] - pt[1],2);
                    if(d<min_d_squared){
                        linkable.add(o);
                    }
                }




            }


            //linkable has all of the speckles that end sometime before the current speckle.
            //linkable is ordered so the closest frame containing the specke is first so
            //that the results will daisey chain together.
            for(Speckle o: linkable){
                //empty frames between end of o and start of s
                int empty = s.getFirstFrame() - o.getLastFrame()-1;
                if(empty < LINK_FRAMES){
                    double[] pt = o.getCoordinates(o.getLastFrame());
                    double[] start_pt = s.getCoordinates(s.getFirstFrame());
                    double dx = pt[0] - start_pt[0];
                    double dy = pt[1] - start_pt[1];

                    //interpolate for missing frames.
                    for(int i = 0;i<empty; i++){
                        double factor = (i+1)/(empty + 1.0);
                        s.addPoint(start_pt[0] + factor*dx, start_pt[1] + factor*dy, s.getFirstFrame() -1);
                    }

                    //add all of the points from o.
                    for(int i: o){
                        pt = o.getCoordinates(i);
                        s.addPoint(pt[0],pt[1],i);
                    }

                    //stop checking o it has been 'consumed'
                    transients.remove(o);

                    //remove it when it is actually tracked.
                    remove.add(o);



                }

            }




        }


    }
    @Override
    public SpeckleEstimator estimateLocation(SpeckleEstimator speck, int frame) {
        if(remove.contains(speck.getSpeckle())||speck.getSize()<MINIMUM_DURATION||testMeanDisplacement(speck)){
            speck.removeSpeckle();
        }
        speck.end();
        return speck;
    }

    /**
     * Tests whether the speckle should be removed because its mean displacement
     * is larger than MAXIMUM_MEAN_DISPLACEMENT.
     * @param speck
     * @return
     */
    public boolean testMeanDisplacement(SpeckleEstimator speck){

        if(speck.getSize()<2) return false;

        int a = speck.getFirstFrame();
        int b = speck.getLastFrame();

        double[] pt1 = speck.getCoordinates(a);
        double[] pt2 = speck.getCoordinates(b);
        double delta = b - a;

        double d = Math.sqrt(Math.pow(pt1[0] - pt2[0],2) + Math.pow(pt1[1] - pt2[1],2))/delta;

        return d>MAXIMUM_MEAN_DISPLACEMENT;

    }

    /**
     *  This will not parse any speckles and it will assume that all of the to_track are
     *  acceptable.
     *
     * @param to_track set of speckles for tracking.
     * @param not_tracking these speckles will be accounted for but not tracked.
     * @param frame - current frame.
     */
    public void prepareSpeckles(HashSet<Speckle> to_track,HashSet<Speckle> not_tracking, int frame ){
        return;
    }

    public int modelType(){
        return REFINE_MODEL;
    }

    /**
     * For setting/changing model parameters.
     *
     * @return a hashmap for storing parameters.
     */
    public HashMap<String,Double> getParameters(){
        HashMap<String,Double> map = new HashMap<String,Double>();
        map.put("Minimum Duration(frames)",(double)MINIMUM_DURATION);
        map.put("Maximum Mean Displacement(px)",MAXIMUM_MEAN_DISPLACEMENT);
        map.put("Frames to consider Linking(frames)",(double)LINK_FRAMES);
        map.put("Linking Radius(px)", LINK_RADIUS);
        return map;
    }

    /**
     * let the model decide
     * @param params
     *
     * @retrun a hashmap for storing parameters.
     */
    public void setParameters(HashMap<String,Double> params){

        MINIMUM_DURATION = params.get("Minimum Duration(frames)").intValue();
        MAXIMUM_MEAN_DISPLACEMENT = params.get("Maximum Mean Displacement(px)");
        LINK_FRAMES = params.get("Frames to consider Linking(frames)").intValue();
        LINK_RADIUS = params.get("Linking Radius(px)");

    }
}
