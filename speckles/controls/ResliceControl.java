package speckles.controls;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleApp;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.HashSet;

/**
 * New imagej plugin that ...
 * User: mbs207
 * Date: Nov 10, 2010
 * Time: 8:42:15 AM
 */
public class ResliceControl implements MouseListener {
    ImagePlus display;
    SpeckleApp parent;
    ImageStack STACK;
    ResliceControlCanvas canners;
    boolean ENABLED=true;

    int HEIGHT = -1;
    public ResliceControl(String title, SpeckleApp sa, ImageStack is){
        parent = sa;
        //setStack(is);
        
    }
    public void setStack(ImageStack is){
        STACK=is;
        ImageProcessor np = emptySlice(is);
        display = new ImagePlus("Reslice Control",np);
        canners = new ResliceControlCanvas(display);

        canners.addMouseListener(this);
    }

    public ImageProcessor emptySlice(ImageStack istack){

        int new_width = istack.getSize();
        int new_height = istack.getHeight();

        FloatProcessor main_processor = new FloatProcessor(new_width, new_height);


        return main_processor;

    }
    public ImageProcessor reslice(ImageStack istack,SpeckleApp sa){

        int new_width = istack.getSize();
        int new_height = istack.getHeight();

        int scan_depth = istack.getWidth();

        FloatProcessor main_processor = new FloatProcessor(new_width, new_height);

        float[][] main_pixels = new float[new_width][new_height];

        Rectangle2D rect = parent.getSelectedRegion();
        int xlow = (int)rect.getX();
        int xhigh = (int)rect.getWidth() + xlow;
        if(xhigh>scan_depth)
            xhigh=scan_depth;

        int ylow = (int)rect.getY();
        int yhigh = (int)rect.getHeight() + ylow;
        yhigh = yhigh>new_height?new_height:yhigh;
        float[][] pixels;
        for(int i = 0; i<new_width; i++){
            String s = MessageFormat.format("processing {0} out of {1} frames.", i + 1, new_width);
            sa.showMessage(s);
            pixels = istack.getProcessor(i+1).getFloatArray();
            for(int j = ylow; j<yhigh; j++){
                for(int k = xlow;k<xhigh; k++){

                    float o = main_pixels[i][j];
                    float n = pixels[k][j];
                    main_pixels[i][j] = n>o?n:o;

                }

            }
        }
        sa.showMessage("seting pixels for main processor");
        main_processor.setFloatArray(main_pixels);
        sa.showMessage("setting processor for display");
        return main_processor;

    }

    public void reslice(SpeckleApp sa){
        display.setProcessor(reslice(STACK,sa));
    }


    public void mouseClicked(MouseEvent e) {
        if(ENABLED){
            int x = e.getX();
            int n = canners.offScreenX(x);
            HEIGHT = canners.offScreenY(e.getY());
            parent.setSlice(n);
        }
    }
    
    public int getYPosition(){
        if(canners!=null)
            return (int)(HEIGHT);
        else
            return 0;
    }

    public void setEnabled(boolean v){
        ENABLED=v;
    }

    public void updateSpeckles(HashSet<Speckle> speckles){
        Window win = display.getWindow();
        if(win!=null && display.getWindow().isVisible())
            canners.updateSpeckles(speckles, parent.getSelected());

    }

    public void unsetMarker(){
        HEIGHT = -1;
    }

    public void setVisible(boolean v){


        final Window win = display.getWindow();

        if(win!=null){
            if(v!=win.isVisible()){
                final boolean vv = v;
                EventQueue.invokeLater(new Runnable(){
                    public void run(){
                        win.setVisible(vv);
                    }
                });
            }
        }else{
            ImageWindow imw = new StackWindow(display,canners);
        }
    }
    public void mousePressed(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseReleased(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}

class ResliceControlCanvas extends ImageCanvas{
    HashSet<Speckle> speckles;
    HashSet<Shape> polygons;
    HashSet<Shape> select;
    boolean draw = true;
    private final Composite ALPHACOMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f);
    private final BasicStroke stroke = new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_BEVEL);
    ResliceLock LOCK;
    BufferedImage IMAGE;
    ResliceControlCanvas(ImagePlus imp){
        super(imp);
        speckles = new HashSet<Speckle>();
        polygons = new HashSet<Shape>();
        select = new HashSet<Shape>();
        LOCK = new ResliceLock();

    }

    /**
     * creates all of the lines and arrows to show the current speckles.
     * @param specks
     * @param selected
     */
    public void updateSpeckles(HashSet<Speckle> specks, Speckle selected){
        speckles.clear();
        ImagePlus imp = getImage();
        double mag = getMagnification();
        LOCK.acquire();
        BufferedImage bi = new BufferedImage((int)(imp.getWidth()*mag), (int)(imp.getHeight()*mag), BufferedImage.TYPE_INT_ARGB);
        speckles.addAll(specks);
        if(selected!=null){
            speckles.remove(selected);
            select.clear();
            int sdex = selected.getFirstFrame();
            int start = (int)(mag*sdex);
            int edex = selected.getLastFrame();
            int end = (int)(mag*edex);

            double[] pt = selected.getCoordinates(sdex);
            int y1 = (int)(pt[1]*mag);
            select.add(makeArrow(start,y1,-1));
            pt = selected.getCoordinates(edex);
            int y2 = (int)(pt[1]*mag);
            select.add(makeArrow(end,y2,1));
            select.add(new Line2D.Double(start,y1,end,y2));


            
        }
        polygons.clear();
        for(Speckle track: speckles){
            int sdex = track.getFirstFrame();
            int start = (int)(mag*sdex);
            int edex = track.getLastFrame();
            int end = (int)(mag*edex);

            double[] pt = track.getCoordinates(sdex);
            int y1 = (int)(pt[1]*mag);

            polygons.add(makeArrow(start,y1,-1));
            pt = track.getCoordinates(edex);
            int y2 = (int)(pt[1]*mag);
            polygons.add(makeArrow(end,y2,1));
            polygons.add(new Line2D.Double(start,y1,end,y2));



        }
        Graphics2D g2d = (Graphics2D)bi.getGraphics();

        //AffineTransform trans = new AffineTransform(mag,0,0,mag,0,0);
        g2d.setComposite(ALPHACOMPOSITE);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(stroke);
        //g2d.setTransform(trans);

        for(Shape mark: polygons){
            g2d.draw(mark);
        }
        g2d.setColor(Color.BLUE);
        for(Shape mark: select)
            g2d.draw(mark);



        IMAGE = bi;
        g2d.dispose();
        LOCK.release();
        repaint();
    }

    Polygon makeArrow(int xnot, int ynot,int dir){
        int[] xp = new int[]{xnot, xnot -10*dir, xnot - 15*dir};
        int[] yp = new int[]{ynot, ynot+15, ynot+10};
       
        return new Polygon(xp,yp,3);

    }

    @Override
    public void paint(Graphics g){
        try{
            super.paint(g);
        }catch(Exception e){
        }
        Graphics2D g2d = (Graphics2D)g;
        double mag = getMagnification();
        g2d.drawImage(IMAGE,-(int)(offScreenXD(0)*mag),-(int)(offScreenYD(0)*mag),this);
                
    }
}

class ResliceLock{
    boolean HELD=false;
    synchronized public void acquire(){
        if(HELD){
            try{
                wait();
            }catch(InterruptedException e){
                System.out.println("Broken Reslice Lock");
                e.printStackTrace();
            }
        }

        HELD=true;
    }
    synchronized public void release(){
        HELD = false;
        notifyAll();
    }

}
