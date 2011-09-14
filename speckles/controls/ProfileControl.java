package speckles.controls;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.SpeckleCalculator;
import speckles.utils.SavitzyGolayFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
public class ProfileControl implements MouseListener{
    /**
     *      Used for showing the profile of a speckle
     **/
    
    /** Integer is the frame, and points are the regular/smoothed data points*/
    ProfileImage PAINTED;
    SavitzyGolayFilter smooth;
    
    
    SpeckleApp parent;
    
    double AVERAGE;
    boolean ENABLED=false;

    int START_FRAME;
    int END_FRAME;
    int MAYBE_FRAME;

    boolean FULL_LENGTH=true;

    private Speckle PROFILED;
    private ImagePlus IMP;

    public ProfileControl(){
        
        
        
        
        PAINTED = new ProfileImage();
        PAINTED.setPreferredSize(new Dimension(600,100));
        PAINTED.setMaximumSize(new Dimension(600,100));
        PAINTED.setMinimumSize(new Dimension(600,100));
        PAINTED.setBackground(Color.LIGHT_GRAY);
        
        smooth = new SavitzyGolayFilter(8,8,2);
        
    }
    
    public JPanel getComponent(){
        
        return PAINTED;
        
    }

    public void setEnabled(boolean v){
        ENABLED=v;
    }
    
    public void profileSpeckle(Speckle speck, ImagePlus implus){
        PROFILED = speck;
        IMP = implus;
        profileSpeckle();
    }

    /**
     * measure the intensities of a speckles, scales the data for the small graph in
     * profiler.
     */
    private void profileSpeckle() {
        if(PROFILED==null||IMP==null)
            return;
        Speckle speck = PROFILED;
        ImagePlus implus = IMP;
        if(FULL_LENGTH){
            START_FRAME=1;
            END_FRAME = implus.getStack().getSize();
        } else{
            
        }


        int N = END_FRAME - START_FRAME + 1;
        double dn = 1./N;

        double[] values = new double[speck.getSize()];
        double[] frames = new double[speck.getSize()];
        double[] smoothed = new double[speck.getSize()];
        
        ImageStack istack = implus.getStack();
        ImageProcessor ip = null;
        double value;
        double smooth_value;

        double[] pt;
        
        int dex = 0;
        
        double max = - Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        
        
        for(Integer i: speck){

            pt = speck.getCoordinates(i);
            try{
                ip = istack.getProcessor(i);
            } catch(Exception e){
                System.out.println("broken implementation");
                e.printStackTrace();
            }



            value = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ip);
            smooth_value = SpeckleCalculator.averageValueAnnulus(pt,SpeckleCalculator.INNER_RADIUS, SpeckleCalculator.OUTER_RADIUS,ip);
            
            max = max>value?max:value;
            min = min<value?min:value;
            
            values[dex] = value;
            smoothed[dex] = smooth_value;
            frames[dex] = (i-START_FRAME+1)*dn;
            dex++;
        }

        
        AVERAGE = parent.getMean();
        max = max>AVERAGE?max:AVERAGE;
        min = min<AVERAGE?min:AVERAGE;
        
        PAINTED.clearPoints();
        
        double factor = max>min?(0.99/(max - min)):0.5;
        
        PAINTED.setAverage(1 + (min - AVERAGE)*factor);
        
        for(int i = 0; i<values.length; i++){
            values[i] = 1 + (min - values[i])* factor;
            smoothed[i] = 1 + (min - smoothed[i])*factor;
            PAINTED.addSpecklePoint(new double[]{ frames[i], values[i]});
            PAINTED.addSmoothPoint(new double[]{frames[i], smoothed[i]});
        }
        //get before values.
        for(int i = 1; i<=2; i++){
            pt = speck.getCoordinates(speck.getFirstFrame());
            int frame = speck.getFirstFrame() - i;
            if(frame<1)
                break;

            try{
                ip = istack.getProcessor(frame);
            } catch(Exception e){
                break;
            }
            double f = (frame-START_FRAME+1)*dn;
            double y_value = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ip);
            y_value= 1 + (min - y_value)* factor;
            double y2_value = SpeckleCalculator.averageValueAnnulus(pt,SpeckleCalculator.INNER_RADIUS, SpeckleCalculator.OUTER_RADIUS,ip);
            y2_value = 1 + (min - y2_value)*factor;
            PAINTED.addBeforePoint(new double[]{f,y_value});
            PAINTED.addBeforePoint(new double[]{f,y2_value});

        }

        //get before values.
        for(int i = 1; i<=2; i++){
            pt = speck.getCoordinates(speck.getLastFrame());
            int frame = speck.getLastFrame() + i;
            if(frame>parent.getMaxSlice())
                break;

            try{
                ip = istack.getProcessor(frame);
            } catch(Exception e){
                break;
            }
            double f = (frame-START_FRAME+1)*dn;
            double y_value = SpeckleCalculator.averageValueCircle(pt,SpeckleCalculator.INNER_RADIUS,ip);
            y_value= 1 + (min - y_value)* factor;
            double y2_value = SpeckleCalculator.averageValueAnnulus(pt,SpeckleCalculator.INNER_RADIUS, SpeckleCalculator.OUTER_RADIUS,ip);
            y2_value = 1 + (min - y2_value)*factor;
            PAINTED.addAfterPoint(new double[]{f,y_value});
            PAINTED.addAfterPoint(new double[]{f,y2_value});

        }

        PAINTED.refreshImage();
        
    }
        
    public void setParent(SpeckleApp p){

        parent = p;
        //nine points are collected for a 'speckle'
        //AVERAGE = p.getMean();
        PAINTED.addMouseListener(this);

        START_FRAME=1;
        END_FRAME=p.getMaxSlice();
        
    }



    /**
     * mouse clicked
     * @param e the event
     */
    public void mouseClicked(MouseEvent e){
            if(!ENABLED)
                return;
            if(e.getButton()==MouseEvent.BUTTON1){
                int slice = getSliceFromClick(e.getX());
                parent.setSlice(slice);
            } else if(!FULL_LENGTH){
                FULL_LENGTH = true;
                profileSpeckle();
                updateFrameMarker();
            }
        }
    public void mouseEntered(MouseEvent e){}

    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e){

        if(e.getButton()!=MouseEvent.BUTTON1){
            int slice = getSliceFromClick(e.getX());
            MAYBE_FRAME=slice;
        }

    }

    public int getSliceFromClick(int x){

        return (int)((END_FRAME-START_FRAME)*PAINTED.getPercentage(x)) + START_FRAME;

    }
    public void mouseReleased(MouseEvent e){
        if(e.getButton()!=MouseEvent.BUTTON1){
            int slice = getSliceFromClick(e.getX());
            if(slice<MAYBE_FRAME){
                START_FRAME = slice;
                END_FRAME = MAYBE_FRAME;
                FULL_LENGTH=false;
                updateFrameMarker();
                profileSpeckle();
            } else if(slice>MAYBE_FRAME){
                START_FRAME = MAYBE_FRAME;
                END_FRAME = slice;
                FULL_LENGTH=false;
                updateFrameMarker();
                profileSpeckle();
            }
        }

    }


    public void updateFrameMarker(){


        PAINTED.setMarker((parent.getCurrentSlice() - START_FRAME + 1 )*1./(END_FRAME-START_FRAME + 1));

    }

    private class ProfileImage extends JPanel{
        BufferedImage PLOT;
        ArrayList<double[]> POINTS;
        ArrayList<double[]> SMOOTH;
        ArrayList<double[]> BEFORE;
        ArrayList<double[]> AFTER;
        int MARKER = 0;
        int AVE = 0;
        final int IHEIGHT = 90;
        final int IWIDTH = 580;

        final int OFFSET = 10;
        double perpx = 1./IWIDTH;


        public ProfileImage(){
            PLOT = new BufferedImage(IWIDTH,IHEIGHT,BufferedImage.TYPE_INT_ARGB);
            POINTS = new ArrayList<double[]>();
            SMOOTH = new ArrayList<double[]>();
            BEFORE = new ArrayList<double[]>();
            AFTER = new ArrayList<double[]>();
            refreshImage();
            
        }
        public void addSpecklePoint(double[] pt){
            POINTS.add(pt);
        }
        
        public void addSmoothPoint(double[] pt){
            
            SMOOTH.add(pt);
            
        }

        public void addBeforePoint(double[] pt){

            BEFORE.add(pt);

        }

        public void addAfterPoint(double[] pt){

            AFTER.add(pt);

        }
        public void clearPoints(){
            POINTS.clear();
            SMOOTH.clear();
            BEFORE.clear();
            AFTER.clear();
        }
        
        /**
         *  Sets the marker to the corresponding position for the frame.
         * 
         * @param f - current frame divided by total frames, from 1 to N
         **/
        public void setMarker(double f){
            
            //first frame is 1 step to right.
            MARKER = (int)(f*IWIDTH) + OFFSET;
            repaint();
        }
        
        public void setAverage(double h){
            
            AVE = (int)(h*IHEIGHT);
            
        }
        
        @Override
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            g.drawImage(PLOT, OFFSET, OFFSET,IWIDTH, IHEIGHT,  this);
            g.setColor(Color.BLACK);
            g.drawLine(MARKER, OFFSET, MARKER, IHEIGHT+OFFSET);
        }
        
        /**
         *  find the frame corresponding to a click event based on the x 
         *  coordinate. 
         * 
         **/
        public double getPercentage(int x){
            //use the one because int's round down.
            return perpx*(x + 1 - OFFSET);
            
        }
        public void refreshImage(){
            Graphics2D g = PLOT.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0,0,IWIDTH, IHEIGHT);
            
            double[] ppt, npt;
            
            //draw a grid
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(0,IHEIGHT/2, IWIDTH, IHEIGHT/2);
            for(int i = 1; i<=4; i++)
                g.drawLine(i*IWIDTH/4, 0, i*IWIDTH/4, IHEIGHT);
            
            //draw average
            g.setColor(Color.GREEN);
            g.drawLine(0,AVE, IWIDTH, AVE);
            

            if(POINTS.size()>0){
                Iterator<double[]> it = POINTS.iterator();
                //first point.
                ppt = it.next();

                if(BEFORE.size()>0){
                    g.setColor(Color.GREEN);
                    npt = BEFORE.get(0);
                    g.drawLine((int)(IWIDTH*ppt[0]),(int)(IHEIGHT*ppt[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));
                    if(BEFORE.size()>2){
                        double[] another_point = BEFORE.get(2);
                        g.drawLine((int)(IWIDTH*another_point[0]),(int)(IHEIGHT*another_point[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));

                    }
                }

                g.setColor(Color.RED);
                while(it.hasNext()){
                    npt = it.next();
                    g.drawLine((int)(IWIDTH*ppt[0]),(int)(IHEIGHT*ppt[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));
                    ppt = npt;
                    
                }

                if(AFTER.size()>0){
                    g.setColor(Color.GREEN);
                    npt = AFTER.get(0);
                    g.drawLine((int)(IWIDTH*ppt[0]),(int)(IHEIGHT*ppt[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));
                    if(AFTER.size()>2){
                        double[] another_point = AFTER.get(2);
                        g.drawLine((int)(IWIDTH*another_point[0]),(int)(IHEIGHT*another_point[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));

                    }
                }

            }
            
            if(SMOOTH.size()>0){
                Iterator<double[]> it = SMOOTH.iterator();
                ppt = it.next();

                if(BEFORE.size()>0){
                    g.setColor(Color.GREEN);
                    npt = BEFORE.get(1);
                    g.drawLine((int)(IWIDTH*ppt[0]),(int)(IHEIGHT*ppt[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));
                    if(BEFORE.size()>2){
                        double[] another_point = BEFORE.get(3);
                        g.drawLine((int)(IWIDTH*another_point[0]),(int)(IHEIGHT*another_point[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));

                    }
                }

                g.setColor(Color.BLUE);
                while(it.hasNext()){
                    npt = it.next();
                    g.drawLine((int)(IWIDTH*ppt[0]),(int)(IHEIGHT*ppt[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));
                    ppt = npt;
                    
                }

                if(AFTER.size()>0){
                    g.setColor(Color.GREEN);
                    npt = AFTER.get(1);
                    g.drawLine((int)(IWIDTH*ppt[0]),(int)(IHEIGHT*ppt[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));
                    if(AFTER.size()>2){
                        double[] another_point = AFTER.get(3);
                        g.drawLine((int)(IWIDTH*another_point[0]),(int)(IHEIGHT*another_point[1]), (int)(IWIDTH*npt[0]), (int)(IHEIGHT*npt[1]));

                    }
                }
            }
            g.dispose();
            repaint();
            
        }
            
    }
    
}

