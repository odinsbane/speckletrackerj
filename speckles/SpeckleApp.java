package speckles;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import speckles.controls.*;
import speckles.gui.AboutWindow;
import speckles.models.AdjustModel;
import speckles.models.LinearRefineModel;
import speckles.models.SpeckleModel;
import speckles.utils.NormalizedCrossCorrelationFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;


/**
 *      This is the main speckle plugin app.  It handles all of the callbacks from button
 *      events.  Starting and stoping tracking.  And maintaining speckle track integrity.
 *
 *      There is some legacy code in here that is on its way out.
 *
 **/
public class SpeckleApp{

    /**
       *    The two ways to store speckle data will be, one keeps track of the underlying speckles
       *    while the second will be used for referencing a particlular frame.
       *    
       **/
       
    public static String VERSION = "1.0";
    public static String DATE = "1/14/2011";
    
    HashSet<Speckle> AllSpeckles,       //Stores all of the speckle data.
                     proof_speckles,    //used for the autolocate slider for drawing speckles
                     CurrentSpeckles;   //Keeps track of speckles in the current frame.  Good for adding/removing points.
    
    ImageStatistics working_stats;

    ImagePlus working_plus;
    Rectangle2D working_bounds;
    ImageProcessor working_proc;
    public JFrame main_frame;
    SpeckleControls speckle_controls;
    
    SpeckleImage draw_panel;
    
    TrackerThread TRACKER;
    AnimationThread ANIMATOR;
    
    //Speckle Colors
    public static final Color NORMAL_COLOR = new Color(0xff9999);
    public static final Color PREVIOUS_COLOR = new Color(0x0000ff);
    public static final Color APPEARANCE_COLOR = new Color(0x00ffff);
    public static final Color DISAPPEARANCE_COLOR=new Color(0xaa9900);
    public static final Color HIGHLIGHT_COLOR=new Color(0xFFD700);
    public static final Color PROOF_COLOR = new Color(0xFFFFFF);
    
    HashMap<Integer,Color> COLORS;
    
    //State Variables
    int    CUR,SCUR;                        //  Index of the current frame, uses 1 to N
            
    boolean DRAWPREVIOUS;

    SpeckleListener draw_listener;
    Speckle highlighted_speckle = null;
    Speckle SELECTED;
    HashMap<Speckle,int[]> specials;
    boolean PROOFING;
    /** batch tracker is used so that the tracker doesn't get changed from frame to frame */
    SpeckleModel MODEL;

    SpeckleNoises horns = new SpeckleNoises();
    private static SpeckleLock LOCK = new SpeckleLock();

    TreeMap<SpeckleParameters,Double> PARAMETERS = new TreeMap<SpeckleParameters,Double>();
    private ResliceControl reslice_control;
    /**
    *     Starts the program for an ImagePlus imagej plugin.
     *
     * @param imp already loaded image plus
     * @param close_on_exit if closing the speckle tracker ends the program.
     */
    public SpeckleApp(ImagePlus imp, boolean close_on_exit){
        this(close_on_exit);
        final ImagePlus tender = imp;

        final Runnable r =  new Runnable(){
                public void run(){

                    loadImage(tender);
                }
            };

        EventQueue.invokeLater(new Runnable(){
                public void run() {

                    setVisible(true);
                    setWaiting();
                    TRACKER.submit(r);
                    TRACKER.finish();
                    clearSpeckleMod();

                }
            });

    }

    /**
    *     Starts the program with out an existing image Plus.  If the program is
    *     started from the command line a file dialog opens to select an image.
    *     If the program is started via the ImageJ plugin, then the selected
     *    ImagePlus is subsequently loaded via the plugin.
    *
    *   @param close_on_exit closing the speckle tracker ends the program.
    */
    public SpeckleApp(boolean close_on_exit){
        lookAndFeel();
        specials = new HashMap<Speckle,int[]>();
        ImageStack is = new ImageStack(200,200);
        is.addSlice("dummy",new FloatProcessor(200,200));
        this.working_plus = new ImagePlus("loading screen",is);  //Original imageplus
        working_bounds = new Rectangle2D.Double(0,0,working_plus.getWidth(), working_plus.getHeight());

        working_stats = working_plus.getStatistics();
        createColorMap();   
        working_proc = working_plus.getProcessor();                             //it's processor
        //draw_proc = working_proc.duplicate().convertToRGB();            //The imageprocessor for displaying output

        CUR = working_plus.getSlice();
                
        draw_panel = new SpeckleImage();    //Display

        draw_panel.setImage(working_proc);
        draw_panel.setFocusable(true);
        
        draw_listener = new SpeckleListener(draw_panel,this);       //Mouse Listener for collecting points and drawing
        
        CurrentSpeckles = new HashSet<Speckle>();
        AllSpeckles = new HashSet<Speckle>();
        
                     
        speckle_controls = new SpeckleControls(this);                         //buttons
        speckle_controls.setCloseOnExit(close_on_exit);

        startTrackerThread();
        
        ANIMATOR = new AnimationThread(this);
        ANIMATOR.start();
        initializeParameters();
        
        updateStatus();

    }

    /**
     * Sets the current ImagePlus (image data).
     *
     * @param original_plus the data that will be used, this ImagePlus does not need to be displayed.
     */
    public void loadImage(ImagePlus original_plus){



        this.working_plus = original_plus;                               //Original imageplus
        working_bounds = new Rectangle2D.Double(0,0,working_plus.getWidth(), working_plus.getHeight());
        working_stats = working_plus.getStatistics();

        working_proc = working_plus.getProcessor();
        CUR = working_plus.getSlice();
        startResliceControl();

        draw_panel.setImage(working_proc);

        speckle_controls.setMaximumImage(getSlices(working_plus));

        modelChanged();        
        updateStatus();


    }

    /**
     * Makes the controls inactive and the progress bar run.
     */
    public void setWaiting(){
        speckle_controls.setWaiting();
        speckle_controls.showMessage("Loading Image ...");
        speckle_controls.setRunning(true);
    }

    /**
     *
     * @return mean value of the working image plus.
     */
    public double getMean(){
        return working_stats.mean;
    }

    /**
     *
     * @return number of slices in the current image plus.
     */
    public int getMaxSlice(){
        return getSlices(working_plus);
    }

    /**
     *
     * @return current slice
     */
    public int getCurrentSlice(){
        return CUR;
    }
    
    /**
     * creates a new speckle with the center at x,y
     *
      * @param x pos x
     * @param y pos y
     */
    public void addSpeckle(double x, double y){

        Speckle speck = new Speckle(x,y,CUR);
        
        AllSpeckles.add(speck);
        CurrentSpeckles.add(speck);
        SELECTED = speck;

        highlightSpeckle((int)x,(int)y);

        speckle_controls.touchSpeckles();
        horns.requestSound(SpeckleNoises.ADD);
    }
    
    /**
      * Goes through speckles in the current frame and removes the first one
     *  that contains the point x,y.  This method is only called through
     *  the speckle listener during a 'shift + click'
     *
     * @param x ordinate
     * @param y ordinate
     */
    public void removeSpeckle(int x, int y){
        Iterator<Speckle> iter = CurrentSpeckles.iterator();
        Speckle s;
        while(iter.hasNext()){
            s = iter.next();
            if(s.contains(x,y,CUR)){
                s.removePoint(CUR);
                if(s.getSize()==0){
                    AllSpeckles.remove(s);
                    verifySelected();
                }
                iter.remove();
                updateSpeckleImage();
                speckle_controls.touchSpeckles();
                break;
            }
        }

    }
    
    /**
      *     highlights the first speckle, that contains x-y or un-highlights an speckles if no speckle
      *     contains x-y
      * @param x - x loc
      * @param y - y lco
      */
    void highlightSpeckle(int x, int y){
        Iterator<Speckle> iter = CurrentSpeckles.iterator();
        
        Speckle ns = null;
        
        //find if a speckle is being hovered over
        while(iter.hasNext()){
            Speckle s = iter.next();
            if(s.contains(x,y,CUR)){
                ns = s;
                break;
            }
        
        }
        
        //determine if the speckle needs to be redrawn
        if(ns != highlighted_speckle){
            highlighted_speckle = ns;
            updateSpeckleImage();
        } 
    }

    public Speckle getSpeckleAt(int x, int y){
        
        Speckle ret = null;
        for(Speckle speck: CurrentSpeckles){
            if(speck.contains(x,y,CUR)){
                ret = speck;
                break;
            }
        }            

        return ret;
    }
    public boolean startedDragging(Point2D pt){
        if(SELECTED!=null){
            double x = draw_panel.offScreenXD(pt.getX());
            double y = draw_panel.offScreenYD(pt.getY());
            return SELECTED.contains(x, y, CUR);
        }
        return false;
    }
    public void dragSpeckle(Point2D npt, Point2D opt){
        double x = draw_panel.offScreenXD(opt.getX());
        double y = draw_panel.offScreenYD(opt.getY());
        if(SELECTED!=null&&SELECTED.contains(x, y, CUR)){
            double[] xy = SELECTED.getCoordinates(CUR);
            x = xy[0] + draw_panel.offScreenXD(npt.getX()) - x;
            y = xy[1] + draw_panel.offScreenYD(npt.getY()) - y;
            SELECTED.addPoint(x, y ,CUR);
            speckle_controls.touchSpeckles();
            updateSpeckleImage();

        }
    }

    public HashSet<Speckle> getSpeckles(int frame){
        HashSet<Speckle> sset = new HashSet<Speckle>();

        for(Speckle s: AllSpeckles)
            if(s.exists(frame)) sset.add(s);
        
        return sset;
        
    }
    
    /**
      *     Draws a speckle with color set as a parameter
     *
     * @param speck - speckle that will be drawn
     * @param color - the color to be used
     * @param frame - which frame the coordinates will be taken from
     */
    void drawSpeckle(Speckle speck, Color color, int frame){
        if(speck.exists(frame)){
            draw_panel.addEllipse(speck.getShape(frame),color);

        }
    }
    
    /**
      *     refreshes the display image with a fresh copy of the original image, speckles from the previous
      *     image and speckles from the current image.
      **/
    public synchronized void updateSpeckleImage() {
        
        //draw_panel.resetView();
        draw_panel.setImage(working_proc);

        //draw speckles
        for(Speckle speck: CurrentSpeckles)
            drawSpeckle(speck,COLORS.get(getType(speck,CUR)),CUR);

        //draw previous speckles.
        if(DRAWPREVIOUS && CUR > 0){
            HashSet<Speckle> speckles = getSpeckles(CUR-1);
            for(Speckle speck: speckles)
                drawSpeckle(speck,PREVIOUS_COLOR,CUR-1);
        }
        for(Speckle s: specials.keySet()){
            int[] color_frame = specials.get(s);
            drawSpeckle(s,COLORS.get(color_frame[0]),color_frame[1]);
        }
        
        if(highlighted_speckle!=null){
            drawSpeckle(highlighted_speckle,HIGHLIGHT_COLOR,CUR);
        }

        draw_panel.addHorizontalLine(reslice_control.getYPosition());
        
        drawSelected();
        
        if(PROOFING){
            for(Speckle speck: proof_speckles)
                drawSpeckle(speck,PROOF_COLOR,CUR);
        }
        
        draw_panel.refreshView();
    }

    public void drawSelected(){
        if(SELECTED!=null&&SELECTED.exists(CUR)){
            double[] pt = SELECTED.getCoordinates(CUR);
            double x = pt[0];
            double y = pt[1];
            Ellipse2D outline = new Ellipse2D.Double(x - 7.5,y - 7.5,16,16);

            draw_panel.addEllipse(outline,HIGHLIGHT_COLOR);
            
        }
    }
    
    /**
      *     Toggles showing previous speckles.
     *     **not used**
      **/
    public void togglePrevious_(){
        DRAWPREVIOUS = !DRAWPREVIOUS;
        updateSpeckleImage();
    }
   
    /**
      *     Next Image
      **/
      public void stepForward(){
        if(CUR< getMaxSlice()){
            CUR += 1;
            updateStatus();            
            
        }
    }
    
    /**
      *     previous image
      **/
    public void stepBackward(){
        if(CUR >1){
            CUR -= 1;
            updateStatus();            
        }
    }
    
    /**
      *     Convenience for use with the slider bar
     *
     * @param n slice to be set to.
     */
    public void setImageSlice(int n){
        CUR = n;
        
        if(SCUR != CUR){
            CurrentSpeckles = getSpeckles(CUR);
            SCUR=CUR;
        }
        
        ImageStack s = working_plus.getStack();
        working_proc = s.getProcessor(CUR);
        highlightSpeckle(draw_listener.CX,draw_listener.CY);
        updateSpeckleImage();
    }
    

    
    /**
      *     Goes through and crops each image for each speckle that shares the same type as 
      *     the speckle_values. 
      **/
    public void createDistribution(){
        
        int mod_type = speckle_controls.getSpeckleType();
        purgeSpeckles();
        switch(mod_type){
            case Speckle.NORMAL_SPECKLE:
                createNormalStack();
                break;
            case Speckle.APPEARANCE_SPECKLE:
                createAppearanceStack();
                break;
            case Speckle.DISAPPEARANCE_SPECKLE:
                createDisappearanceStack();
                break;
        }
        
    }

    /**
     * Creates a stack of images from the square region cropped out around the
     * end of a speckle mark.  The frame relative is the frame number relative
     * to the last frame.
     * 
     */
    public void createDisappearanceStack(){
        
        int relative = speckle_controls.getRelative();
        int square_size = speckle_controls.getSquareSize();
        
        ImageStack outstack = new ImageStack(2*square_size+1,2*square_size + 1);
        ImageStack working_stack = working_plus.getStack();

        int m = getMaxSlice();
        
        
        
        Integer f;
        ImageProcessor x,p;
        
        for(Speckle s: AllSpeckles){
            f = s.getLastFrame();
            if(f<m&&f+relative>1&& f+relative<m){
                p = working_stack.getProcessor(f+relative);
                x = cropRegion(p,s.getCoordinates(f), square_size);
                if(x!=null)
                    outstack.addSlice("frame: " + f,x);
            }
                
            
            
        }
        
        if(outstack.getSize()>0){
            ImagePlus output_image = new ImagePlus("Disappearance Events, retaltive: " + relative,outstack);
            output_image.show();
        } else {
            JOptionPane.showMessageDialog(main_frame,"No speckles matched you criteria, Perhaps they are too close to the edge?");
        }
        
        
        
    }

    /**
     * Create a stack of images near the start
     */
    public void createAppearanceStack(){
        
        int relative = speckle_controls.getRelative();
        int square_size = speckle_controls.getSquareSize();
        
        ImageStack outstack = new ImageStack(2*square_size+1,2*square_size + 1);
        ImageStack working_stack = working_plus.getStack();

        int m = getMaxSlice();
        
        
        
        Integer f;
        ImageProcessor x,p;
        
        for(Speckle s: AllSpeckles){
            f = s.getFirstFrame();
            if(f>1&&f+relative>1&& f+relative<m){
                p = working_stack.getProcessor(f+relative);
                x = cropRegion(p,s.getCoordinates(f), square_size);
                if(x!=null)
                    outstack.addSlice("frame: " + f,x);
            }
                
            
            
        }
        
        if(outstack.getSize()>0){
            ImagePlus output_image = new ImagePlus("Appearance Events, retaltive: " + relative,outstack);
            output_image.show();
        } else {
            JOptionPane.showMessageDialog(main_frame,"No speckles matched you criteria, Perhaps they are too close to the edge?");
        }
        
        
        
    
        
    }

    /**
     * Create a stack of images that are neither, appearance or disappearance.
     *
     */
    public void createNormalStack(){
        
        int relative = speckle_controls.getRelative();
        int square_size = speckle_controls.getSquareSize();
        
        ImageStack outstack = new ImageStack(2*square_size+1,2*square_size + 1);
        ImageStack working_stack = working_plus.getStack();

        int m = getMaxSlice();
        
        ImageProcessor x,p;
        
        for(Speckle s: AllSpeckles){
            for(Integer f: s){
                if(f+relative>1&& f+relative<m){
                    p = working_stack.getProcessor(f+relative);
                    x = cropRegion(p,s.getCoordinates(f), square_size);
                    if(x!=null)
                        outstack.addSlice("frame: " + f,x);
                }
            }
            
            
        }
        
        if(outstack.getSize()>0){
            ImagePlus output_image = new ImagePlus("Normal Speckles",outstack);
            output_image.show();
        } else {
            JOptionPane.showMessageDialog(main_frame,"No speckles matched you criteria, Perhaps they are too close to the edge?");
        }
        
        
        
    
        
        
    }

    /**
     * A utility method for cropping images, especially used when
     * creating stacks
     *
     * @param now_proc - processor to be cropped.
     * @param pt - center of cropped region.
     * @param square_size - half-width of region to be cropped.
     * @return - image data of cropped region.
     */
    public ImageProcessor cropRegion(ImageProcessor now_proc, double[] pt, int square_size){
        ImageProcessor x = null;
        if( 
            pt[0]-square_size>0 && pt[0]+square_size + 1 < now_proc.getWidth() &&   //  xbounds
            pt[1] - square_size >0 && pt[1] + square_size < now_proc.getHeight()    //  ybounds
            
                                ){
                
            x = now_proc.createProcessor(2*square_size+1, 2*square_size+1);
            
            for(int m = -square_size; m<square_size+1; m++){
                for(int n = -square_size; n<square_size+1; n++){
                         
                    x.putPixelValue(m+square_size, n+square_size , now_proc.getInterpolatedPixel(pt[0] + m, pt[1] + n));
                                
                }
            }
        }
        
        
        return x;
    }
    
    /**
      *     Deletes all speckles in the current selected region.
      **/
    public void clearSpeckles(){
        HashSet<Speckle> framed = cullSpecklesToSelectedRegion();

        for(Speckle s: framed){
            if(s.exists(CUR))
                deleteSpeckle(s);
        }

        purgeSpeckles();
        updateSelector();
        updateSpeckleImage();

    }

    /**
     * For determining what color the speckle should be painted.
     * @param speck track that will be evaluated
     * @param f frame
     * @return integer corresponding to type.
     */
    public int getType(Speckle speck, int f){
        if(speck.getFirstFrame()==f)
            return Speckle.APPEARANCE_SPECKLE;
        else if (speck.getLastFrame()==f)
            return Speckle.DISAPPEARANCE_SPECKLE;
    
        return Speckle.NORMAL_SPECKLE;
    }
    
    /**
      *     Copies the speckles from the previous frame to this frame
      **/
    public void copyPreviousSpeckles(){
        if(CUR>1){
            HashSet<Speckle> oldspeckles = getSpeckles(CUR - 1);
            for(Speckle speck: oldspeckles){
                double[] pt = speck.getCoordinates(CUR-1);
                speck.addPoint(pt[0],pt[1],CUR);
                CurrentSpeckles.add(speck);
            }
            updateSpeckleImage();
        }
    }
    
    /**
      *     Writes the speckles to a file.
      **/
    public void saveSpeckles(){
        validateSpeckles();
        
        String title = working_plus.getShortTitle();

        boolean v = SpeckleWriter.writeCSVSpeckles(AllSpeckles,main_frame,title);
        if(v) speckle_controls.clearChange();
        
    }
    
    /**
     *   remove any points not in the image.
     * */
    public void validateSpeckles(){
        HashSet<Integer> bad = new HashSet<Integer>();
        double[] pt;
        for(Speckle s: AllSpeckles){
            bad.clear();
            for(Integer f: s){
                pt = s.getCoordinates(f);
                if(pt[0]>0 && pt[1] > 0 && pt[0]<working_plus.getWidth() && pt[1]<working_plus.getHeight()){
                    //good
                } else{
                    bad.add(f);
                }
                
            }
            for(Integer i: bad)
                s.removePoint(i);
                
            
        }
        
        purgeSpeckles();

    }
    
    /**
      *     Provides a dialogue to read the speckles from a file.
      **/
    public void  loadSpeckles(){
        int test = JOptionPane.OK_OPTION;
        if(speckle_controls.checkChange())
            test = JOptionPane.showConfirmDialog(speckle_controls.getFrame(), "The speckles have been modified do you wish to load new Speckles?");

        if (test!=JOptionPane.OK_OPTION)
            return;


        HashSet<Speckle> loaded = SpeckleWriter.readCSVSpeckles(main_frame);
        CurrentSpeckles.clear();
        if(loaded!=null){
            AllSpeckles = loaded;
            validateSpeckles();
            CurrentSpeckles = getSpeckles(CUR);
            SCUR = CUR;
            speckle_controls.clearChange();

        }

        updateSpeckleImage();
        updateSelector();


    }

    /**
     * Removes all speckles.
     */
    public void newSpeckles(){
        int test = JOptionPane.OK_OPTION;
        if(speckle_controls.checkChange())
            test = JOptionPane.showConfirmDialog(speckle_controls.getFrame(), "The speckles have been modified do you wish to load new Speckles?");

        if (test!=JOptionPane.OK_OPTION)
            return;

        AllSpeckles.clear();
        CurrentSpeckles.clear();
        finishTracking();
        speckle_controls.clearChange();        
        
    }

    /**
     * Starts the autolocate based on threshold.
     */
    public void thresholdLocate(){



        speckle_controls.setDialogWaiting();
        speckle_controls.setRunning(true);
        proof_speckles = new HashSet<Speckle>();
        PROOFING=true;
        AutoLocateSlider slides = new AutoLocateSlider(proof_speckles, CurrentSpeckles,working_proc, this );
        java.awt.EventQueue.invokeLater(slides);


    }

    /** uses intensity to locate speckles.*/
    public void maxLocate(){
        SpeckleDetector sd = new SpeckleDetector();
        HashSet<Speckle> ns = sd.refineSpeckles(CurrentSpeckles, CUR,working_proc);
        CurrentSpeckles = ns;
        purgeSpeckles();
        updateSpeckleImage();


    }

    /** uses intensity to locate speckles.*/
    public void maxLocateSpeckle(){
        if(SELECTED!=null){
            //SpeckleDetector sd = new SpeckleDetector();
            SpeckleDetector.refineSpeckleD(SELECTED, working_proc, CUR);

            purgeSpeckles();
            updateSpeckleImage();
        }

    }

    /**
     * when the slider moves.
     */
    public void sliderUpdate(){
    
        setImageSlice(speckle_controls.getSliderValue());
    
    }

    /**
     * For keeping the display up to date.
     */
    public void updateStatus(){
        speckle_controls.updateStatus();
    }

    /**
     * Trackes the selected speckle.
     */
    public void trackSpeckle(){
        Speckle s = SELECTED;
        if(s != null){
            draw_listener.setTrackSpeckle(s);

            stepForward();
            
            draw_listener.setMode(SpeckleListener.PLACE_TRACK_MODE);

            showSpeckleFromPrevious(s);
            
    
            speckle_controls.showMessage("Select where the next speckle will appear");
            speckle_controls.setWaiting();
            speckle_controls.enableDirections();
        } else{
            draw_listener.setMode(SpeckleListener.GET_TRACK_MODE);
            speckle_controls.showMessage("Select which speckle you want to track");
            speckle_controls.setWaiting();

        }
        purgeSpeckles();
    }

    /**
     * Merge two speckle tracks, the selected track
     * and another track to be decided. Can create discontinuous tracks.
     */
    public void mergeSpeckles(){
        Speckle s = SELECTED;
        if(s!=null){
            draw_listener.getMergeSpeckle(s);
            speckle_controls.setWaiting();
            speckle_controls.enableDirections();

        } else{
        
            draw_listener.setMode(SpeckleListener.GET_MERGE_MODE);
            showMessage("Select which speckle you will merge");
            speckle_controls.setWaiting();

        }
    }

    /**
     * Merges two speckles, removes overlapping frames by taking
     * the dominant speckle tracks frames.
     * @param a - dominant speckle track
     * @param b - subordinate
     */
    public void mergeSpeckles(Speckle a, Speckle b){
        if(a!=b){
            for(Integer i: a){
                double[] pt = a.getCoordinates(i);
                b.addPoint(pt[0],pt[1],i);
            }
            a.clear();
        }
        purgeSpeckles();
        selectSpeckle(b);
    }

    /**
     * For enabling the directions to change image slice when
     * the rest of the ui is disabled.
     */
    public void enableDirections(){
        speckle_controls.enableDirections();
    }

    /**
     * Change the speckle shape to the next speckle shape.
     * Circles, X's or nothing.
     */
    public void toggleSpeckleShape(){

        draw_panel.nextSpeckleShape();

    }

    /**
     * Sets UI look and feel.
     */
    public static void lookAndFeel(){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
        } catch (UnsupportedLookAndFeelException e) {
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }

    }

    /**
     * Loads an image plus with the given path.  If the
     * image plus is null it will show an error message.
     *
     * @param file_name - path of the file that will be opened.
     */
    public void loadImage(String file_name){
        final String fname = file_name;
        TRACKER.submit(
            new Runnable(){
                public void run(){
                    if(fname!=null){
                        ImagePlus x;
                        x = new ImagePlus(fname);
                        if(x!=null){
                            x.resetDisplayRange();
                            loadImage(x);
                        } else{
                            JOptionPane.showMessageDialog(speckle_controls.getFrame(),"Unable to load image.");

                        }
                    }
                }
            });
        TRACKER.finish();
    }

    public void loadImage(){
        speckle_controls.setWaiting();
        String fname = SpeckleWriter.getOpenFileName(speckle_controls.getFrame(),"Select Image File to open");
        loadImage(fname);
        

    }

    /**
     * To indicate that speckles do not need to be saved.
     */
    public void clearSpeckleMod(){
        TRACKER.submit(new Runnable(){
           public void run(){
               speckle_controls.clearChange();
           }
        });
    }

    /**
     * Start it from scratch.
     * @param args cmd line args.
     */
    public static void main(String[] args){
        try{
            //x.show();
            final SpeckleApp y = new SpeckleApp(true);
            EventQueue.invokeLater(new Runnable(){
                public void run() {
                    y.setVisible(true);
                    y.loadImage();
                    y.clearSpeckleMod();
                }
            });



        } catch(Exception e) {
            System.out.println("File did not load properly");
            e.printStackTrace();
            System.exit(0);
        }
        
    }
    
    /**
       *    Places speckles in their appropriate frames.  Removes them if they don't exist anymore
       **/
    public synchronized void purgeSpeckles(){
        speckle_controls.touchSpeckles();
        AllSpeckles.addAll(CurrentSpeckles);
        
        /** remove size zero speckles */
        ArrayList<Speckle> none = new ArrayList<Speckle>();
        ArrayList<Integer> bad_pts = new ArrayList<Integer>();

        for(Speckle s: AllSpeckles){
            bad_pts.clear();
            for(Integer i: s){
                double[] d = s.getCoordinates(i);
                if(!working_bounds.contains(d[0],d[1])){
                    bad_pts.add(i);
                }
            }
            for(Integer i: bad_pts)
                s.removePoint(i);
            
            if(s.getSize()==0)
                none.add(s);
        }
        for(Speckle s: none)
            AllSpeckles.remove(s);
        
        CurrentSpeckles = getSpeckles(CUR);
        SCUR = CUR;
        verifySelected();
        updateSelector();
    
    }

    public void verifySelected(){
        if(SELECTED!=null&&SELECTED.getSize()==0)
            SELECTED=null;
    }
    
    public void updateSpeckle(Speckle s,double x, double y){
        s.addPoint(x,y,CUR);
        CurrentSpeckles.add(s);
        
    }

    public void createColorMap(){
        COLORS = new HashMap<Integer,Color>();
        COLORS.put(Speckle.NORMAL_SPECKLE,NORMAL_COLOR);
        COLORS.put(Speckle.APPEARANCE_SPECKLE,APPEARANCE_COLOR);
        COLORS.put(Speckle.DISAPPEARANCE_SPECKLE,DISAPPEARANCE_COLOR);
        COLORS.put(Speckle.PREVIOUS_SPECKLE, PREVIOUS_COLOR);
    }

    public void showSpeckleFromPrevious(Speckle speck){
        int n = speck.exists(CUR-1)?CUR-1:speck.getLastFrame();

        drawSpeckle(speck, PREVIOUS_COLOR, n);

        specials.put(speck,new int[]{Speckle.PREVIOUS_SPECKLE, n});

    }

    /**
     * Shows the position of this speckle in the next frame, on the current
     * image.
     *
     * @param speck mark that will be displayed.
     */
    public void showSpeckleFromNext(Speckle speck){
        int n = speck.exists(CUR+1)?CUR+1:speck.getFirstFrame();
        drawSpeckle(speck,PREVIOUS_COLOR,n);
        
         specials.put(speck,new int[]{Speckle.PREVIOUS_SPECKLE, n});
    }

    /**
      * Politely finished and resets the state.  As of now, sets the speckle_controls to read draw_listener to normal mode
      * and clears any specials.  This will block until the cmd has finished.
     * 
      **/
    public void endActions(){
        LOCK.interrupt();
        LOCK=new SpeckleLock();
        ANIMATOR.stopAnimation();
        draw_listener.resetMode();
        speckle_controls.resetReady();
        specials.clear();
        updateSpeckleImage();

    }
    
    public void showMessage(String s){
    
        speckle_controls.showMessage(s);
    
    }
    
    public void autoTrack(){
        Speckle s = SELECTED;
        if(s!=null){
            
            autoTrack(s);

        } else{
        
            draw_listener.setMode(SpeckleListener.GET_AUTOTRACK_MODE);
            showMessage("Select Speckle to Auto-Track");
            speckle_controls.setWaiting();

        }
        
    
    }
    
    public synchronized void autoTrackAll(){
        speckle_controls.setWaiting();
        speckle_controls.setRunning(true);
        speckle_controls.showMessage("auto-tracking.");
        try{
            LOCK.get();
        }catch(ConcurrentModificationException e){
            System.out.println("Already Tracking...Should be disabled.");
            return;
        }

        HashSet<Speckle> selected = cullSpecklesToSelectedRegion();

        TRACKER.submit(SpeckleTracker.stateTrackSpeckle(AllSpeckles, selected ,working_plus, MODEL, CUR));
        TRACKER.finish();

        
    }

    public void autoTrack(Speckle s){

        try{
            LOCK.get();
        }catch(ConcurrentModificationException e){
            System.out.println("Already Tracking...Should be disabled.");
            return;
        }
        speckle_controls.setWaiting();
        speckle_controls.setRunning(true);
        speckle_controls.showMessage("auto-tracking.");

        TRACKER.submit(SpeckleTracker.autoTrackSpeckle(s,working_plus,MODEL));
        TRACKER.finish();
        
    }
    public void finishTracking(){

        purgeSpeckles();
        if(SELECTED!=null)
            speckle_controls.profileSpeckle(SELECTED,working_plus);
        LOCK.release();
        endActions();
        horns.requestSound(SpeckleNoises.AUTOTRACK);


    }

    public void correlateLocate(){
        
       
        proof_speckles = new HashSet<Speckle>();
        speckle_controls.setDialogWaiting();
        NormalizedCrossCorrelationFilter ncc_filter = new NormalizedCrossCorrelationFilter(working_plus);
        ncc_filter.createTemplate(AllSpeckles);
        
        ImageProcessor filtered = working_proc.duplicate();
        
        ncc_filter.filterImage2(filtered);
        
        PROOFING=true;
        
        AutoLocateSlider slides = new AutoLocateSlider(proof_speckles, CurrentSpeckles,filtered, this );
        
        java.awt.EventQueue.invokeLater(slides);

        

 
    }

    /**
     * Called back after an autolocate.
     *
     * @param thresh - threshold value for creating binary image
     * @param proximity - distance value 
     * @param size - minimum size
     */
    public void finishedLocate(double thresh, double proximity, double size){

        PARAMETERS.put(SpeckleParameters.thresholdValue,thresh);
        PARAMETERS.put(SpeckleParameters.proximityValue,proximity);
        PARAMETERS.put(SpeckleParameters.sizeValue,size);

        PROOFING=false;
        updateSelector();
        endActions();
        
    }

    /**
     * If an auto locate was cancelled, stops showing proof speckles
     * and enables the UI.
     *
     */
    public void cancelledLocate(){

        PROOFING=false;
        endActions();
    }

    /**
     * If the batchtrack routine was cancelled.
     * @param bts the dialog
     */
    public void cancelledLocate(BatchTrackingStarter bts){
        PROOFING=false;
        bts.cancelAcquire();
    }

    /**
     * Crops a region around ever frame of the selected speckle.
     *
     */
    public void showSpeckleAllFrames(){
        Speckle s = SELECTED;
        if(s!=null){
            
            showSpeckleAllFrames(s);

        } else{
        
            draw_listener.setMode(SpeckleListener.GET_SHOW_MODE);
            showMessage("Select Speckle to See at every frame it is present in");
            speckle_controls.setWaiting();

        }

    }

    /**
     * Shows this speckle in its own imageplus with the size from the analysis
     * window.
     * @param s speckle that will be shown.
     */
    public void showSpeckleAllFrames(Speckle s){
        TreeSet<Integer> indicies = new TreeSet<Integer>();
        for(Integer i: s)
            indicies.add(i);
        
        int square_size = speckle_controls.getSquareSize();        
        
        ImageStack outstack = new ImageStack(2*square_size+1,2*square_size + 1);
        ImageStack working_stack = working_plus.getStack();
                            
        for(Integer i: indicies){
            ImageProcessor now_proc = working_stack.getProcessor(i);
            double[] pt = s.getCoordinates(i);
            
            ImageProcessor x = cropRegion(now_proc, pt, square_size); 

            if(x!=null)
                outstack.addSlice("x: " + pt[0] +" y: " + pt[1] + " frame: " + i ,x);
            
            
        }
        if(outstack.getSize()>0){
            ImagePlus output_image = new ImagePlus("One speckle all frames",outstack);
            output_image.show();
        } else {
            JOptionPane.showMessageDialog(main_frame,"No speckles matched you criteria, Perhaps they are too close to the edge?");
        }
        
        
        
        
        endActions();
    }

    /**
     * Deletes the currently selected speckle.
     */
    public void deleteSpeckle(){
        Speckle s = SELECTED;
        if(s!=null){

            deleteSpeckle(s);

        } else{
            draw_listener.setMode(SpeckleListener.GET_DELETE_MODE);
            showMessage("Select speckle to remove completely");
            speckle_controls.setWaiting();
        }
    
    }

    /**
     * Removes speckle from program irretrievably
     * @param s - candidate
     */
    public void deleteSpeckle(Speckle s){
        
        s.clear();
        purgeSpeckles();
        endActions();
        
    }

    /**
     * gets the panel containing the image.
     * @return jpanel that the image is drawn on.
     */
    public JPanel getImagePanel(){
    
        return draw_panel;
        
    }

    /**
     * zoom in image
     */
    public void zoomIn(){
        draw_panel.zoomIn();
    }

    /**
     * zoom out image.
     */
    public void zoomOut(){
        draw_panel.zoomOut();
    }

    /**
     *  Starts batch locate by bringing up a dialog.
     */
    public void startBatchLocate(){
        //make sure they aren't using a refine model when they batch locate.
        if(MODEL.modelType()==SpeckleModel.REFINE_MODEL){

            JOptionPane.showMessageDialog(
                    speckle_controls.getFrame(),
                    "The selected model only updates speckles \n and should not be used in batch locate.");
            return;

        }

        try{
            LOCK.get();
        }catch(ConcurrentModificationException e){
            JOptionPane.showMessageDialog(speckle_controls.getFrame(),"Tracking in progress, cancel any current tracking and try again.");
            return;
        }


        speckle_controls.setWaiting();
        speckle_controls.setRunning(true);

        HashMap<String,Double> params = MODEL.getParameters();

        PARAMETERS.put(SpeckleParameters.startingFrame,1d);
        PARAMETERS.put(SpeckleParameters.endingFrame,(double)getMaxSlice());
        BatchTrackingStarter bts = new BatchTrackingStarter(this,speckle_controls.getFrame(),PARAMETERS,params);
        bts.requestConstants();
        bts.updateConstants(PARAMETERS,params);
        MODEL.setParameters(params);

        if (bts.wasCancelled()) {
            TRACKER.finish();
        } else {
            TRACKER.submit(new Runnable(){public void run(){ batchLocate();}});
        }

    }

    /**
     * Starts the batch locate routine.
     */
    public void batchLocate(){

        SpeckleCalculator sc = new SpeckleCalculator(AllSpeckles,working_plus,3);
        batchLocate(sc);
    }

    /**
     * This runs the batch locate, it better be off of the event queue by now.
     *
     *
     * @param sc outdated but it distinguishes the two batch locate methods.
     */
    public void batchLocate(SpeckleCalculator sc){
        double thresh = PARAMETERS.get(SpeckleParameters.thresholdValue);
        double min_dist = PARAMETERS.get(SpeckleParameters.proximityValue);
        double size = PARAMETERS.get(SpeckleParameters.sizeValue);
        double link = PARAMETERS.get(SpeckleParameters.linkFrames);
        int STOP_FRAME = PARAMETERS.get(SpeckleParameters.endingFrame).intValue();
        STOP_FRAME=STOP_FRAME>getMaxSlice()?getMaxSlice():STOP_FRAME;
        int START_FRAME = PARAMETERS.get(SpeckleParameters.startingFrame).intValue();
        START_FRAME=START_FRAME>0?START_FRAME:1;

        setSlice(START_FRAME);
        int current_frame = START_FRAME;
        Rectangle2D rect = getSelectedRegion();

        SpeckleModel local_model = MODEL;
        updateStatus();
        purgeSpeckles();

        double min_distance_sqd = Math.pow(min_dist,2);

        boolean linear_fit = PARAMETERS.get(SpeckleParameters.linearFit)>0;
        SpeckleModel refiner = linear_fit?new LinearRefineModel(working_plus):new AdjustModel();
        refiner.setImagePlus(working_plus);

        //SpeckleModel correlated = new NCCConstantVelocityModel(working_plus);
        ArrayList<double[]> current;

        HashSet<Speckle> next = new HashSet<Speckle>();
        HashSet<Speckle> trackable = new HashSet<Speckle>();

        SpeckleDetector sd = new SpeckleDetector();
        SpeckleLock lock = LOCK;

        double max_displacement = Math.pow(PARAMETERS.get(SpeckleParameters.maxMeanDisplacement), 2);

        int min_size = PARAMETERS.get(SpeckleParameters.minimumDuration).intValue();
        min_size = min_size<=0?1:min_size;

        SpeckleCalculator calc = new SpeckleCalculator(AllSpeckles,working_plus,11);

        HashSet<Speckle> saved = new HashSet<Speckle>();
        String started = "started with: " + AllSpeckles.size();
        saved.addAll(AllSpeckles);
        while(current_frame<STOP_FRAME&&!lock.isInterrupted()){
            ImageProcessor local_processor = working_plus.getStack().getProcessor(current_frame);

            next.clear();
            trackable.clear();


            ImageProcessor t = SpeckleDetector.threshold(local_processor, thresh);
            
            current = sd.getCentroids(t);


            speckle_controls.showMessage(started + ", total: " + saved.size() + ", found: " + current.size());


            cullCentroidsToRegion(current,rect);

            //keep the largest of the centroids that are too close together.
            ArrayList<double[]> isolated = new ArrayList<double[]>();

            if(min_distance_sqd>0 && current.size()>0){
                for(double[] pt: current){
                    boolean add = true;
                    if(isolated.size()>0){
                        Iterator<double[]> iter = isolated.iterator();
                        while(iter.hasNext()){
                            double[] opt = iter.next();
                            double distance = Math.pow(opt[0] - pt[0],2) + Math.pow(opt[1] - pt[1],2);
                            if(distance<min_distance_sqd){
                                if(opt[2]>pt[2]){
                                    add= false;
                                    break;
                                } else{
                                    iter.remove();
                                    break;
                                }
                            }
                        }
                    }

                    if(add) isolated.add(pt);


                }
            }

            next.addAll(SpeckleDetector.cullCentroidsBySize(isolated,current_frame, size));


            double distance;

            if(min_distance_sqd>0){

                double[] pt,opt;
        
        
                outter: for(Speckle s: next){
                    SpeckleDetector.refineSpeckleD(s, local_processor, current_frame);
                    pt = s.getCoordinates(current_frame);
                                        
                    for(Speckle exist: saved){
                        if(exist.getSize()==0)
                            continue;
                        if(exist.exists(current_frame)){
                        
                            opt = exist.getCoordinates(current_frame);
                            distance = Math.pow(opt[0] - pt[0],2) + Math.pow(opt[1] - pt[1],2);
                            if(distance<min_distance_sqd)
                                continue outter;
                        
                        }

                        //check if exist ended in the same spot sometime before
                        int f = exist.getLastFrame();
                        if(current_frame-f<link){
                            opt = exist.getCoordinates(f);
                            distance = Math.pow(opt[0] - pt[0],2) + Math.pow(opt[1] - pt[1],2);

                            if(distance<min_dist){
                                for(int i = f;i<current_frame;i++){
                                    exist.addPoint(pt[0],pt[1],i+1);

                                }
                                trackable.add(exist);
                                continue outter;

                            }
                        }
                    
                    }
                    trackable.add(s);
                
                }
            } else {
                    trackable.addAll(next);
            }
            speckle_controls.showMessage(started + ", total: " + saved.size() + ", tracking: " + trackable.size());
            SpeckleTracker.stateTrackSpeckle(saved, trackable ,working_plus, local_model, current_frame).run();
            SpeckleTracker.stateTrackSpeckle(saved, trackable ,working_plus, refiner, current_frame).run();


            if(PARAMETERS.get(SpeckleParameters.fusionSwitch)>0)
                calc.purgeSpeckles(trackable,working_plus);

            
            Iterator<Speckle> iter = trackable.iterator();

            
            while(iter.hasNext()){
                Speckle s = iter.next();

                if(s.getSize()<min_size){
                    iter.remove();
                } else if(s.getSize()>1){
                    int i = s.getLastFrame();
                    double[] a = s.getCoordinates(i);
                    int j = s.getFirstFrame();
                    double[] b = s.getCoordinates(j);
                    int delta = i - j;
                    double d = (Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2))*Math.pow(delta,-2);
                    if(d>max_displacement)
                        iter.remove();
                        
                }
            }

            saved.addAll(trackable);

            current_frame+=1;
            CUR = current_frame;
            updateStatus();
        }


        //just to make changes
        AllSpeckles.clear();
        AllSpeckles.addAll(saved);

        purgeSpeckles();

        lock.release();
        updateSelector();
        endActions();

    }
    
    public void trimAfter(){
        Speckle s = SELECTED;
        if(s!=null){
            
            trimAfter(s);

        } else{
        
            draw_listener.setMode(SpeckleListener.GET_TRIMAFTER_MODE);
            showMessage("Select Speckle to remove from all following frames");
            speckle_controls.setWaiting();

        }
        
    }
    
    public void trimBefore(){
        Speckle s = SELECTED;
        if(s!=null){
            
            trimBefore(s);

        } else{
        
            draw_listener.setMode(SpeckleListener.GET_TRIMBEFORE_MODE);
            showMessage("Select Speckle to remove from all previous frames");
            speckle_controls.setWaiting();

        }
        
    }
    
    public void trimBefore(Speckle s){

        ArrayList<Integer> duds = new ArrayList<Integer>();
        for(int i: s)
            if(i<CUR)
                duds.add(i);
        for(int i: duds)        
            s.removePoint(i);
        selectSpeckle(s);
        endActions();

    }
    
    public void trimAfter(Speckle s){

        ArrayList<Integer> duds = new ArrayList<Integer>();
        for(int i: s)
            if(i>CUR)
                duds.add(i);
        for(int i: duds)        
            s.removePoint(i);
        selectSpeckle(s);
        endActions();

    }
    
    public void toEnd(){
        CUR = getMaxSlice();
        updateStatus();
        
    }
    
    
    public void toBeginning(){
        CUR = 1;
        updateStatus();
    }
    
    public void setSlice(int s){
        
        if(s>0 && s<=getMaxSlice()){
            CUR = s;
            updateStatus();
        }
    }
    
    
    public void modelChanged(){
        int type = speckle_controls.getModelIndex();
        MODEL = SpeckleModel.Models.values()[type].getModel();
        MODEL.setImagePlus(working_plus);
        
            
            
    }
        
    public void selectSpeckle(double x, double y){
        Iterator<Speckle> iter = CurrentSpeckles.iterator();
        
        Speckle ns = null;
        
        //find if a speckle is being hovered over
        while(iter.hasNext()){
            Speckle s = iter.next();
            if(s.contains(x,y,CUR)){
                ns = s;
                break;
            }
        
        }
        
        if(ns != null)
            selectSpeckle(ns);
        else if(SELECTED!=null){
            SELECTED=null;
            updateSpeckleImage();
        }
        
    }

    public void selectSpeckle(Speckle s){
        if(s!=SELECTED){
            SELECTED = s;
            updateSpeckleImage();
        }

        speckle_controls.profileSpeckle(SELECTED, working_plus);
        updateSelector();
    }
    
    public void showVersion(){
        
        AboutWindow.showAbout();
        
    }

    public void findBroken(){
        try{
            LOCK.get();
            showMessage("finding broken speckles");
            setWaiting();
            outter: for(Speckle s: AllSpeckles ){
                int last = -1;
                for(int i: s){
                    if(last<0){
                        last = i;
                        continue;
                    }
                    int delta = i - last;
                    if(delta>1){
                        selectSpeckle(s);
                        setSlice(last);
                        break outter;
                    }

                    last = i;
                }
            }
            LOCK.release();
            speckle_controls.resetReady();


        } catch(ConcurrentModificationException e){
            showMessage("speckles currently being modified");
        }

    }

    /**
     * Brings up an imagej window.  For use if the program was started w/out
     * imagej.  It will also show the imageplus corresponding to the curreint image.
     *
     */
    public void startImageJ(){
        if(IJ.getInstance()==null)
            new ImageJ();
        ImageWindow a = new StackWindow(working_plus){
            public void windowClosed(java.awt.event.WindowEvent e){
                setVisible(false);
            }
            @Override
            public void windowClosing(java.awt.event.WindowEvent e){
                setVisible(false);
            }
        };
        a.setVisible(true);
    }

    /**
     * updates the selector table data..
     */
    public void updateSelector(){
        speckle_controls.updateSelector(AllSpeckles);
    }

    /**
     * Button call back that initiates the data table update routine, which
     * can be resource intensive.
     *
     */
    public void updateSelectorButton(){
        speckle_controls.showMessage("updating data table");
        speckle_controls.setWaiting();
        speckle_controls.setRunning(true);
        TRACKER.submit(new Runnable(){
            public void run(){
                speckle_controls.updateAppearances(working_plus);
                updateSelector();
            }
        });
        TRACKER.finish();
    }

    /**
     *
     * @return the currently selected speckle.
     */
    public Speckle getSelected(){
        return SELECTED;
    }

    /**
     * During initilization startes a thread which is the main loop.
     */
    synchronized void startTrackerThread(){
        TRACKER = new TrackerThread(this);
        TRACKER.start();
    }

    /**
     * Changes to the next model.
     */
    public void nextModel(){
        speckle_controls.nextModel();
        modelChanged();        
    }

    /**
     * Changes to the previous model.
     */
    public void previousModel(){
        speckle_controls.previousModel();
        modelChanged();
    }

    /**
     * For playing through the image.
     */
    public void startAnimation(){

        if(ANIMATOR.running){
            //stop running
            endActions();

        } else{
            //start running
            draw_listener.setMode(SpeckleListener.PLAYING);
            speckle_controls.setWaiting();
            speckle_controls.setRunning(true);
            ANIMATOR.startAnimation();

        }



    }

    /**
     * for selecting a region.
     */
    public void startSelection(){

        draw_panel.requestSelection();

    }

    /**
     * clears the selected region.
     */
    public void clearSelection(){

        draw_panel.clearSelection();

    }

    /**
     * Call back after editing parameters is finished with the
     * accept button.
     */
    public void changeParameters(){
        TRACKER.submit(new Runnable() {
            public void run() {
                HashMap<String, Double> params = MODEL.getParameters();
                ParameterDialog pd = new ParameterDialog(speckle_controls.getFrame(), PARAMETERS, params);
                pd.requestConstants();
                pd.updateConstants(PARAMETERS, params);
                MODEL.setParameters(params);
                SpeckleCalculator.INNER_RADIUS = PARAMETERS.get(SpeckleParameters.innerRadius);
                SpeckleCalculator.OUTER_RADIUS = PARAMETERS.get(SpeckleParameters.outerRadius);
                NormalizedCrossCorrelationFilter.TEMPLATESIZE = PARAMETERS.get(SpeckleParameters.correlationTemplate).intValue();
                SpeckleTracker.MIN_TRACK_SEPARATION = PARAMETERS.get(SpeckleParameters.proximityValue);
            }

        });
    }

    /**
     * Initializes global parameters.
     *
     */
    public void initializeParameters(){

        PARAMETERS.put(SpeckleParameters.proximityValue,SpeckleTracker.MIN_TRACK_SEPARATION);
        PARAMETERS.put(SpeckleParameters.sizeValue,4.0);
        PARAMETERS.put(SpeckleParameters.fusionSwitch,0.0);
        PARAMETERS.put(SpeckleParameters.minimumDuration,1.0);
        PARAMETERS.put(SpeckleParameters.maxMeanDisplacement,0.1);
        PARAMETERS.put(SpeckleParameters.thresholdValue,0d);
        PARAMETERS.put(SpeckleParameters.innerRadius,SpeckleCalculator.INNER_RADIUS);
        PARAMETERS.put(SpeckleParameters.outerRadius,SpeckleCalculator.OUTER_RADIUS);
        PARAMETERS.put(SpeckleParameters.linearFit, 0d);
        PARAMETERS.put(SpeckleParameters.correlationTemplate,(double)NormalizedCrossCorrelationFilter.TEMPLATESIZE);
        PARAMETERS.put(SpeckleParameters.linkFrames,0d);

    }

    /**
     * If there is a selected region only use selected region for detecting speckles.
     *
     * @param cents array list of centroids that will be culled.
     */
    public void cullCentroidsToSelectedRegion(ArrayList<double[]> cents){
        if(draw_panel.hasSelection()){
            Iterator<double[]> iter = cents.iterator();
            Rectangle2D rect = draw_panel.getImageSelectedRegion();
            while(iter.hasNext()){
                double[] pt = iter.next();
                if(!rect.contains(pt[0],pt[1]))
                    iter.remove();
            }
        }

    }

    /**
     * Removes all centroids from the List that are not contained w/in
     * the rectangle.
     * @param cents - centroids to be culled.
     * @param rect - area that is acceptable.
     */
    public void cullCentroidsToRegion(ArrayList<double[]> cents,Rectangle2D rect){
        if(draw_panel.hasSelection()){
            Iterator<double[]> iter = cents.iterator();
            while(iter.hasNext()){
                double[] pt = iter.next();
                if(!rect.contains(pt[0],pt[1]))
                    iter.remove();
            }
        }

    }

    /**
     * Selected region area.
     *
     * @return rectangular reguin that represents the selected area.
     */
    public Rectangle2D getSelectedRegion(){

        if(draw_panel.hasSelection()){
            Rectangle2D panel = draw_panel.getImageSelectedRegion();
            return panel;
        }else
            return new Rectangle2D.Double(0,0,working_plus.getWidth(), working_plus.getHeight());
        
    }

    /**
     * Gets all speckles that are in the selected region.
     * @return a subset of the existing speckle tracks that are contained in the selected region.
     */
    public HashSet<Speckle> cullSpecklesToSelectedRegion(){
        HashSet<Speckle> track_dogs = new HashSet<Speckle>();
        if(draw_panel.hasSelection()){
            Rectangle2D rect = getSelectedRegion();

            for(Speckle s: AllSpeckles){
                for(int frame: s){
                    double[] pt = s.getCoordinates(frame);
                    if(rect.contains(pt[0],pt[1])){
                        track_dogs.add(s);
                    }
                }
            }


        } else{
            track_dogs.addAll(AllSpeckles);
        }

        return track_dogs;
    }

    /**
     * Checks to see if the lock has been stopped/interrupted.
     * @return lock is running?
     */
    public static boolean isStopped(){
        return LOCK.isInterrupted();
    }

    /**
     * Displays an image plus with a resliced version of the current image.
     *
     * @return new reslice control with geometry based on the current image.
     */
    public ResliceControl startResliceControl(){
        if(reslice_control==null)
            reslice_control =  new ResliceControl("Reslice Control", this,working_plus.getStack());
        else{
            reslice_control.setStack(working_plus.getStack());
        }
        return reslice_control;
    }

    /**
     * Shows the reslice control, and draws the representitive speckle tracks on the
     * image.
     */
    public void showResliceControl(){
        try{
            LOCK.get();
        }catch(ConcurrentModificationException e){
            System.out.println("Already Tracking...Should be disabled.");
            return;
        }
        speckle_controls.setWaiting();
        speckle_controls.showMessage("starting reslice control");
        speckle_controls.setRunning(true);
        final SpeckleApp sa = this;
        TRACKER.submit(new Runnable(){
            public void run(){
                reslice_control.reslice(sa);
                sa.showMessage("setting visible");
                reslice_control.setVisible(true);
                sa.showMessage("updating speckles");
                updateResliceSpeckles();
                sa.showMessage("finished with reslice ctl, should move onto to say 'ready'");
            }
        });
        TRACKER.finish();

    }

    /**
     * Redraws speckle tracks onto reslice control, only including the
     * tracks that are in the selected region.
     */
    public void updateResliceSpeckles(){
        reslice_control.updateSpeckles(cullSpecklesToSelectedRegion());
    }

    /**
     * Gets the closest speckle to the provided speckle
     * @param s speckle that is being searched for.
     * @param v an array for storing the distance to the nearest speckle track
     * @return the speckle track that is closest.
     */
    public Speckle getClosestSpeckle(Speckle s, double[] v){
        double min = Double.MAX_VALUE;
        Speckle closest = null;
        for(Speckle o: AllSpeckles){
            if(s!=o){
                for(int f: s){
                    if(o.exists(f)){
                        double[] a,b;
                        a = o.getCoordinates(f);
                        b = s.getCoordinates(f);
                        double d = Math.pow(a[0] - b[0],2) + Math.pow(a[1] - b[1],2);
                        if(d<min){
                            closest = o;
                            min = d;
                        }
                    }
                }
            }
        }
        v[0] = Math.sqrt(min);
        return closest;
    }


    /**
     * set the application visible
     * @param t show/hide the main controls.
     */
    public void setVisible(boolean t){

        speckle_controls.setVisible(t);

    }

    /**
     * Copies the data table to a text window.
     */
    public void copySelectorToTextWindow() {
        speckle_controls.copySelectorToTextWindow();

        
    }

    /**
     * The application starts this and not the BatchTrackingStarter because the calculations need to be posted to the
     * tracker thread.
     * 
     * @param bts a running dialog.
     */
    public void calculateSpeckleProperties(BatchTrackingStarter bts){

        final BatchTrackingStarter final_bts = bts;
        final HashSet<Speckle> working = AllSpeckles;
        TRACKER.submit(new Runnable(){
            public void run(){
                SpeckleCalculator sc = new SpeckleCalculator(working,working_plus,SpeckleCalculator.INNER_RADIUS);
                try{
                    SpeckleCalculator.estimateOptimalParameters(AllSpeckles,working_plus);
                }catch(Exception e){
                    //?
                }
                final_bts.finishLearning(sc);
            }
        });

    }



    /**
     * Starts an autolocate dialog for use with a batch tracking.
     *
     * @param bts
     */
    public void startAcquireParameters(BatchTrackingStarter bts){

        proof_speckles = new HashSet<Speckle>();
        PROOFING=true;

        AutoLocateSlider slides = new AutoLocateSlider(proof_speckles, CurrentSpeckles,working_proc, this,bts );
        java.awt.EventQueue.invokeLater(slides);


    }

    /**
     * Call back after using an autolocate dialog to obtain parameters for the
     * batch track starter.
     *
     * @param thresh threshold value for determining new candidates
     * @param proximity minimum distance
     * @param size minimum size of centroid that will be kept.
     * @param bts dialog that started process.
     */
    public void finishAcquire(double thresh, double proximity, double size,BatchTrackingStarter bts){


        PROOFING=false;
        proof_speckles.clear();
        updateSelector();
        bts.finishAcquire(thresh,size,proximity);
    }

    /**
     * Uses the current model to display properties about the speckle.  Creates a table of data
     * for existing speckles
     *
     */
    public void measureSpeckles(){

        speckle_controls.setWaiting();
        speckle_controls.setRunning(true);
        speckle_controls.showMessage("Measure Speckle Values.");
        try{
            LOCK.get();
        }catch(ConcurrentModificationException e){
            System.out.println("Already Tracking...Should be disabled.");
            return;
        }

        //HashSet<Speckle> selected = cullSpecklesToSelectedRegion();
        TRACKER.submit(SpeckleTracker.measureSpeckles(AllSpeckles, SELECTED ,working_plus, MODEL, CUR));
        TRACKER.finish();

        


    }

    /**
     * Get number of slices in an image file.  Uses size of stack not getNSlices.
     * @param imp image data
     * @return number of slices in the stack.
     */
    static public int getSlices(ImagePlus imp){

        return imp.getStack().getSize();

    }

    /**
     * Turns the currently selected speckle track into two tracks.  The current
     * frame is added to the later speckle.
     */
    public void splitSpeckle() {
        if(SELECTED!=null){
            if(SELECTED.getFirstFrame()<getCurrentSlice()&&SELECTED.getLastFrame()>=getCurrentSlice()){
                Speckle a = new Speckle();
                for(Integer i: SELECTED){

                    if(i<getCurrentSlice()){
                        double[] pt = SELECTED.getCoordinates(i);
                        a.addPoint(pt[0],pt[1],i);
                    }
                    
                }
                for(Integer i: a){
                    SELECTED.removePoint(i);
                }
                AllSpeckles.add(a);
                speckle_controls.touchSpeckles();
                selectSpeckle(SELECTED);
            }
        }
    }

    /**
     * Opens a dialog to get more speckles to append to existing speckles.
     * 
     */
    public synchronized void appendSpeckles(){
        int test = JOptionPane.OK_OPTION;
        if(speckle_controls.checkChange())
            test = JOptionPane.showConfirmDialog(speckle_controls.getFrame(),
                    "Current speckles have been modified continue to append more speckles?");

        if (test!=JOptionPane.OK_OPTION)
            return;


        HashSet<Speckle> loaded = SpeckleWriter.readCSVSpeckles(main_frame);

        if(loaded!=null){
            AllSpeckles.addAll(loaded);
            validateSpeckles();
            CurrentSpeckles = getSpeckles(CUR);
            SCUR = CUR;
            speckle_controls.touchSpeckles();

        }

        updateSpeckleImage();
        updateSelector();

        

    }
}
