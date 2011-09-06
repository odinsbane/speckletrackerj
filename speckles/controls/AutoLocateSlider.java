package speckles.controls;

import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleDetector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;

/**
   *   This class if for adjusting the threshold for autolacating to best optimize the output.
   **/
   
public class AutoLocateSlider extends JDialog implements ActionListener, ChangeListener, WindowListener, Runnable {


    JButton accept_button, decline_button;
    JSlider threshold_slider, size_slider, proximity_slider;
    JLabel thresh_value,size_value,proximity_value;
    SpeckleApp PARENT;
    
    HashSet<Speckle> proofing,      //being displayed, can only be modified but not replaced
                     existing,      //Accepted speckles
                     sizelimitted;
    ArrayList<double[]> current;
    SpeckleDetector sd;
    ImageProcessor improc;
    
    //Image statistics
    int max_value, min_value, current_value;

    double SIZELIMIT;
    
    boolean CANCEL = true;
    boolean DISPOSED = false;

    BatchTrackingStarter BTS;
    boolean BATCH = false;

    
    public AutoLocateSlider(HashSet<Speckle> proof, HashSet<Speckle> existing, ImageProcessor improc,  SpeckleApp sap){
        //super((Frame)null, "adjust speckles",Dialog.ModalityType.APPLICATION_MODAL);
        super(sap.main_frame, "adjust speckles");
        initialize(proof, existing, improc, sap);
    }
    public AutoLocateSlider(HashSet<Speckle> proof, HashSet<Speckle> existing, ImageProcessor improc,  SpeckleApp sap, BatchTrackingStarter bts){
        //super((Frame)null, "adjust speckles",Dialog.ModalityType.APPLICATION_MODAL);
        super(bts, "adjust speckles");
        BATCH=true;
        BTS=bts;
        initialize(proof, existing, improc, sap);
    }
    private void initialize(HashSet<Speckle> proof, HashSet<Speckle> existing, ImageProcessor improc,  SpeckleApp sap){
        sizelimitted = new HashSet<Speckle>();
        
        PARENT = sap;
        proofing = proof;
        sd = new SpeckleDetector();
        this.existing = existing;
        this.improc = improc;
        
        
        JPanel contentpane = new JPanel();
        contentpane.setLayout(new BoxLayout(contentpane,BoxLayout.PAGE_AXIS));
    
        setContentPane(contentpane);
        
        JPanel row = new JPanel();
        accept_button = new JButton("Accept");
        decline_button = new JButton("Cancel");
        
        row.add(accept_button);
        row.add(decline_button);
        
        contentpane.add(row);
        
        decline_button.addActionListener(this);
        accept_button.addActionListener(this);
        
        thresh_value = new JLabel("      ");
        threshold_slider = createSlider("Threshold", contentpane, thresh_value);

        size_value = new JLabel("      ");
        size_slider = createSlider("Size",contentpane,size_value );

        proximity_value = new JLabel("      ");
        proximity_value.setMinimumSize(new Dimension(50,20));
        proximity_value.setPreferredSize(new Dimension(50, 20));
        proximity_value.setMaximumSize(new Dimension(50, 20));

        proximity_slider = createSlider("Minimum Distance",contentpane,proximity_value);
        
        proximity_slider.setValue(30);
        

        
    }

    private void updateLabels(){

        int value = (int)(threshold_slider.getValue()/100. * (max_value - min_value) + min_value);
        final String t = MessageFormat.format(" {0,number,integer} ",value);
        thresh_value.setText(t);

        size_value.setText(MessageFormat.format("{0,number,###.#}",SIZELIMIT));

        proximity_value.setText(MessageFormat.format("{0,number,###.#}",proximity_slider.getValue()/10.));



    }

    public JSlider createSlider(String label, JPanel cp, JLabel value){
        JSlider who = new JSlider(JSlider.HORIZONTAL,0,100,1);
        who.addChangeListener(this);
        
        JPanel row = new JPanel();
        row.add(new JLabel(label));
        row.add(who);
        row.add(value);
        cp.add(row);
        
        return who;
    }
    
    public void actionPerformed(ActionEvent evt){
        String cmd = evt.getActionCommand();
        if(cmd.compareTo("Cancel")==0){
            dispose();
        }
        else{
            accept();
        }
            
    }
    
    public void stateChanged(ChangeEvent e) {
        JSlider x = (JSlider)e.getSource();
        if (!x.getValueIsAdjusting()) {
            setWaiting();
            if(x==threshold_slider)
                updateThreshold();
            else if(x==size_slider)
                updateSizeCriteria();
            else if(x==proximity_slider)
                updateProximityCriteria();
            setReady();
        }
    }
    
    public void cancel(){
        if(!DISPOSED){
            DISPOSED=true;
            if(CANCEL){
                if(BATCH){
                    PARENT.cancelledLocate(BTS);
                }else{
                    PARENT.cancelledLocate();
                }
            }else{
                double thresh = threshold_slider.getValue()/100. * (max_value - min_value) + min_value;
                double proximity = Math.pow(proximity_slider.getValue(),1)/10;
                double size = SIZELIMIT;
                if(!BATCH)
                    PARENT.finishedLocate(thresh, proximity, size);
                else
                    PARENT.finishAcquire(thresh,proximity, size,BTS);
            }
        }
    }
    
    public void accept(){
        CANCEL=false;
        if(!BATCH){
            existing.addAll(proofing);
            PARENT.purgeSpeckles();
        }
        dispose();
    }
    
    void firstCalculateCentroids(){
        ImageProcessor t = sd.autoThreshold(improc);
        current = sd.getCentroids(t);
      
        
        max_value = sd.getImageMax();
        min_value = sd.getImageMin();
        current_value = sd.getImageThreshold();
        threshold_slider.setValue((int)(current_value*100./(max_value - min_value)));
        
        updateSizeCriteria();
    
    }
    
    
    public void setWaiting(){
        threshold_slider.setEnabled(false);
        size_slider.setEnabled(false);
        proximity_slider.setEnabled(false);
        decline_button.setEnabled(false);
        accept_button.setEnabled(false);
    }
    
    public void setReady(){
        threshold_slider.setEnabled(true);
        size_slider.setEnabled(true);
        proximity_slider.setEnabled(true);
        decline_button.setEnabled(true);
        accept_button.setEnabled(true);
    }

    /**
     * Third step - removes the smaller speckle that is too close.
     */
    public void updateProximityCriteria(){

        proofing.clear();
        int cur = PARENT.getCurrentSlice();
        double mindistance = Math.pow(proximity_slider.getValue(),2)/100;
        double distance;
        
        if(proximity_slider.getValue()>0){
        
            double[] pt,opt;
        
        
            outter: for(Speckle s: sizelimitted){
                
                pt = s.getCoordinates(cur);
                boolean add = true;
                for(Speckle exist: existing){
                    if(exist.exists(cur)){
                        
                        opt = exist.getCoordinates(cur);
                        distance = Math.pow(opt[0] - pt[0],2) + Math.pow(opt[1] - pt[1],2);
                        if(distance<mindistance)
                            continue outter;
                        
                    }
                    
                }
        
                
                for(Speckle proof: proofing){
                    
                    opt = proof.getCoordinates(cur);
                    distance = Math.pow(opt[0] - pt[0],2) + Math.pow(opt[1] - pt[1],2);
                    if(distance<mindistance)
                        continue outter;
                        
                }
                
                
                
                proofing.add(s);
                
            }
        } else {
            proofing.addAll(sizelimitted);
        }
        PARENT.updateSpeckleImage();

        updateLabels();
        
    }

    /**
     * Second Step goes through the existing speckles and removes the ones that are too small.
     */
    public void updateSizeCriteria(){
        sizelimitted.clear();
        int cur = PARENT.getCurrentSlice();

        PARENT.cullCentroidsToSelectedRegion(current);

        if(size_slider.getValue()>0){
            double max = 0;
            double min = 1e6;
            for(double[] xyw: current){
                max = max>xyw[2]?max:xyw[2];
                min = min<xyw[2]?min:xyw[2];
            }
            double value = size_slider.getValue();
            double factor = (min==max)?max/100:(max-min)/100;
            SIZELIMIT = value*factor + min;
            sizelimitted.addAll(SpeckleDetector.cullCentroidsBySize(current,cur, SIZELIMIT));
        }else{
            SIZELIMIT=0;
            sizelimitted.addAll(sd.getSpeckles(current, cur));
        }
            
        
        updateProximityCriteria();
    
    }

    /**
     * First step needs to go through and 'threshold' the image.
     *
     */
    public void updateThreshold(){
        
        int value = (int)(threshold_slider.getValue()/100. * (max_value - min_value) + min_value);
        ImageProcessor t = sd.threshold(improc, value);
        current = sd.getCentroids(t);

        updateSizeCriteria();
    }
    
    public void run(){
        pack();
        setVisible(true);
        addWindowListener(this);
        firstCalculateCentroids();
        
    }



    
    public void windowActivated(WindowEvent e){
    }
    public void windowClosed(WindowEvent e){
        cancel();
        
    }
    public void windowClosing(WindowEvent e){
        cancel();
    }
    public void windowDeactivated(WindowEvent e){
    }
    public void windowDeiconified(WindowEvent e){
    }
    public void windowIconified(WindowEvent e){
    }
    public void windowOpened(WindowEvent e){
    }
    
    
   
 }
