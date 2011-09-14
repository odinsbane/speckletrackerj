package speckles.models;

import ij.ImagePlus;
import speckles.Speckle;
import speckles.SpeckleTracker;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

/**
 *  This class is a factory for creating models to pedict speckle
 *  location.
 * */

 public abstract class SpeckleModel{
    //public static HashMap<Integer,SpeckleModel> MODELS = loadModels();
    public static int EXTENDING_MODEL = 0;
    public static int REFINE_MODEL = 1;
    public static int CULL_MODEL = 2;
    public enum Models {
        diffusing_spots(new DiffusingSpotsModel()),
        diffusing_spots_backwards(new DiffusingSpotsBackwardsModel()),
        fixed_parameter_spots(new FixedIntensityModel()),
        diffusing_NCC(new DiffusingNCCModel()),
        constant_velocity_NCC(new NCCConstantVelocityModel()),
        static_model(new StaticSpeckleModel()),
        drift_model(new StaticDriftModel()),
        linear_refine(new LinearRefineModel()),
        adjustment_model(new AdjustModel()),
        gaussian_fit(new GaussianFit()),
        immobile(new ImmobileModel()),
        remove_candidates(new RemoveCandidates()),
        filler_model(new FillerModel());

        SpeckleModel model;


        public SpeckleModel getModel(){
            return model;
        }
        Models(SpeckleModel m){
            model = m;
        }
    };


    ImagePlus implus;

    
    SpeckleModel(){
    }
    
    /**
     *  Initiates a specklemodel factory for generating speckle
     * 
     *
     * @param imp image that will be used/displayed
     */
    public void setImagePlus(ImagePlus imp){
        implus = imp;
    }
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * one speckle.
     * */
    public abstract SpeckleModel createModel(Speckle s);
    
    /*
     * returns a SpeckleModel capable of predicting speckles based on
     * all of the speckles in the group.
     * */
    public abstract SpeckleModel createModel(HashSet<Speckle> specks);
    
    /** 
     * Checks the speck estimator for the next location.  Which assumes the model has
     * already been created.
     * 
     * @param speck estimator of current speckle
     * @param frame current frame
     * 
     **/
    public abstract void estimateLocation(SpeckleEstimator speck,int frame);

    /**
     *
      * @return the type of model, this is for use with the 'models' menu.
     */
    public abstract int modelType();

    /**
     * 
     *     This calculates only the expected trajectory of the speckle., ie dx and dy
     * 
     * */
    public static SpeckleModel modelSpeckleTrajectory(Speckle s){
        double x[] = new double[s.getSize()];
        double y[] = new double[s.getSize()];
        double z[] = new double[s.getSize()];
        int dex = 0;
        for(Integer i: s){
            double[] pt = s.getCoordinates(i);
            
            x[dex] = pt[0];
            y[dex] = pt[1];
            z[dex] = i;
            dex ++;
            
        }
        VelocityModel a = new VelocityModel();
        
        double[] values = SpeckleTracker.leastSquares( z, x);
        a.DX = values[0];
        values = SpeckleTracker.leastSquares( z, y);
        a.DY = values[0];
        
        return a;
        
    }
    
 
    private static class VelocityModel extends SpeckleModel{
        public double DX,DY;
        /** dummy class for instantiating a velocity only model */
        public void updateValues(SpeckleModel values){}
        
        public SpeckleModel createModel(Speckle s){return null;}
        
        public SpeckleModel createModel(HashSet<Speckle> specks){ return null;}
        
        public double[] predictSpeckle(Speckle speck, int frame){return null;}
        
        public void estimateLocation(SpeckleEstimator speck,int frame){return;}
        
        public SpeckleEstimator estimateLocation(Speckle speck,int frame, double[] pt){return null;}

        public int modelType(){
            return EXTENDING_MODEL;
        }
    }

    /**
     * For setting/changing model parameters.  One way to set parameters is to make static variables
     * in your model. Then create a map in this function with the appropriate
     *
     * @return
     */
    public HashMap<String,Double> getParameters(){
        return null;
    }

    /**
     * let the model decide
     * @param params
     */
    public void setParameters(HashMap<String,Double> params){
        //
    }

    /**
     *  This will parse the current speckles and decide if they should be tracked.
     *  For the default implementation it only leaves speckles that exist and end
     *  in the current frame.
     *
     * @param to_track set of speckles for tracking.
     * @param not_tracking these speckles will be accounted for but not tracked.
     * @param frame - current frame.
     */
    public void prepareSpeckles(HashSet<Speckle> to_track,HashSet<Speckle> not_tracking, int frame ){
        Iterator<Speckle> iter = to_track.iterator();
        while(iter.hasNext()){
            Speckle s = iter.next();
            if (s.exists(frame) && s.getLastFrame() == frame) {
                //keep it in the 'to track' speckles
                continue;
            }
            
            not_tracking.add(s);
            iter.remove();

        }

    }

    /**
     * Loads me some models.  Populates a list of models that are contained in the class path
     *
     * ****************broken until further notice.***********************
     * @return
     */
    private static HashMap<Integer,SpeckleModel> loadModels(){

        HashMap<Integer, SpeckleModel> models = new HashMap<Integer, SpeckleModel>();

        ArrayList<Class> classes = new ArrayList<Class>();
         try{
            URL jar = SpeckleModel.class.getProtectionDomain().getCodeSource().getLocation();

            JarInputStream jis = new JarInputStream(jar.openStream());
            for(JarEntry je = jis.getNextJarEntry();je!=null;je = jis.getNextJarEntry()){
                System.out.println('.');
                if(je.isDirectory()) continue;
                String name = je.getName();
                int last = name.indexOf(".class");
                name = name.replaceAll(Pattern.quote("/"),".");
                classes.add(Class.forName(name.substring(0,last)));


            }
        }catch(Exception e){
            e.printStackTrace();
            //fuck it

        }
        ArrayList<File> files = new ArrayList<File>();
        
        try{
            URI base = SpeckleModel.class.getResource("/speckles/models").toURI();

            if(base!=null){
                
                File dir = new File(base);
                
                if(dir.isDirectory())
                    Collections.addAll(files, dir.listFiles());
                for(File f: dir.listFiles()){
                    System.out.println(f);
                }
            } else{
                System.out.println("uri fail");
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        for(File f: files){

            String n = f.getName();

            int last = n.indexOf(".class");
            if(last>0){
                try{

                    classes.add(Class.forName("speckles.models." + n.substring(0,last)));
                }catch(Exception e){
                    System.out.println("failed: " + n);
                }
            }

        }
        Integer i = 0;
        for(Class c: classes){

                if(c.getSuperclass()==SpeckleModel.class&&Modifier.isPublic(c.getModifiers())){

                    try{
                        System.out.println("found model:" + c);
                        SpeckleModel m = (SpeckleModel)c.newInstance();
                        models.put(i,m);
                        i++;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                } else{

                    System.out.println("not a model: " + c);

                }

        }



        return models;


    }
}
