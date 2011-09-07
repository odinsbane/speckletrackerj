package speckles;

/**
   *    The purpose of this class is merely to start the controls
   *
   *
   **/

import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class SpeckleImage extends JPanel{

    private BufferedImage IMG =  null;

    private int W,H,WO,HO;
    private double ZOOM = 1;
    private double FACTOR = 1;
    
    private Stroke stroke;
    
    private final Composite ALPHACOMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,0.5f);
    private int ZOOMDEX = 0;
    private Map<Ellipse2D,Color> SPECKLES = new ConcurrentHashMap<Ellipse2D,Color>();
    private SelectedRegion region;
    int SHAPE_DEX = 0;
    public SpeckleShape speckle_shape = new SpeckleEllipse();
    int LINE;
    public SpeckleImage(){
        region = new SelectedRegion(this);

    }
    
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        if(IMG!=null){
            g.drawImage(IMG,0,0,W,H,this);
            Graphics2D g2d = (Graphics2D)g;
            

            g2d.setComposite(ALPHACOMPOSITE);
            g2d.setStroke(stroke);
            speckle_shape.setZoom(ZOOM);
            for(Ellipse2D s: SPECKLES.keySet())
                speckle_shape.draw(s,SPECKLES.get(s),g2d);

            region.paint(g2d);
            g2d.setColor(Color.RED);
            if(LINE>0)
                g2d.draw(new Rectangle(0,(int)(ZOOM*(LINE-5)),W,(int)(10*ZOOM)));
        }
            
    }

    public double offScreenXD(double x){
        return x*FACTOR;
    }
    
    public double offScreenYD(double y){
        return y*FACTOR;
    }
    
    public int offScreenX(int x){
    
        return (int)(x*FACTOR);
    }
    
    public int offScreenY(int y){
    
        return (int)(y*FACTOR);
    }
    
    synchronized public void setImage(ImageProcessor improc){
        
        IMG = improc.getBufferedImage();
        
        HO = improc.getHeight();
        WO = improc.getWidth();
        
        
        SPECKLES.clear();
        
        resetView();
    }
    
    public void resetView(){
        
        H = (int)(HO*ZOOM);
        W= (int)(WO*ZOOM);
        
        stroke = new BasicStroke((float)(ZOOM),BasicStroke.CAP_ROUND,BasicStroke.JOIN_BEVEL);
        setSize(W,H);
        
        
        Dimension d = new Dimension(W,H);
        
        setMinimumSize(d);
        setPreferredSize(d);
        setMaximumSize(d);
        
        repaint();
    }
    
    public void refreshView(){
        
        repaint();
    }
    
    public void zoomIn(){
        ZOOMDEX += 1;
        ZOOM = 1 + 0.2*ZOOMDEX;
        FACTOR = 1/ZOOM;
        resetView();
    }
    
    public void zoomOut(){
        ZOOMDEX = (ZOOMDEX>-4)?ZOOMDEX-1: ZOOMDEX;
        ZOOM = 1 + 0.2*ZOOMDEX;
        FACTOR = 1/ZOOM;
        resetView();
    }
    
    synchronized public void addHorizontalLine(int pt){
        LINE = pt;
    }
    
    synchronized public void addEllipse(Ellipse2D s, Color a){
        SPECKLES.put(s,a);
    
    
    }
    
    public void nextSpeckleShape(){
        SHAPE_DEX++;
        SHAPE_DEX=SHAPE_DEX>2?0:SHAPE_DEX;
        switch(SHAPE_DEX){
            case 0:
                speckle_shape = new SpeckleEllipse();
                break;
            case 1:
                speckle_shape = new SpeckleCross();
                break;
            case 2:
                speckle_shape = new NullShape();
                break;

        }
        repaint();

    }

    public void requestSelection(){
        if(region.ACTIVE)
            return;
        region.setActive(true);
        region.setDraw(true);
    }


    public void clearSelection(){
        region.setDraw(false);
        region.setActive(false);
        
    }

    public boolean hasSelection(){
        return region.ACTIVE;
    }

    Rectangle2D getImageSelectedRegion(){
        Rectangle2D.Double rect = new Rectangle2D.Double(offScreenXD(region.X),offScreenYD(region.Y),region.WIDTH/ZOOM,region.HEIGHT/ZOOM);
        return rect;
    }



}


class SpeckleEllipse implements SpeckleShape{

    double r,w,ZOOM;
    Ellipse2D scaled = new Ellipse2D.Double();
    public void setZoom(double ZOOM){

        this.ZOOM = ZOOM;
    }
    public void draw(Ellipse2D s, Color a,Graphics2D g2d){
        r = s.getWidth()*ZOOM*0.5;
        w = 2*r;
        //Ellipse2D scaled = new Ellipse2D.Double(s.getCenterX()*ZOOM - r,s.getCenterY()*ZOOM - r,w,w);
        scaled.setFrame(s.getCenterX()*ZOOM - r,s.getCenterY()*ZOOM - r,w,w);
        g2d.setColor(a);
        g2d.draw(scaled);

    }

}

class SpeckleCross implements SpeckleShape{
    double r,w,ZOOM;
    public void setZoom(double ZOOM){
        this.ZOOM = ZOOM;
    }

    public void draw(Ellipse2D s, Color a,Graphics2D g2d){
        g2d.setColor(a);
        r = s.getWidth()*ZOOM*0.5;
        w = 1.5*r;

        double x = s.getCenterX()*ZOOM;
        double y = s.getCenterY()*ZOOM;
        g2d.drawLine((int)(x - w),(int)(y - w), (int)(x - r), (int)(y - r));
        g2d.drawLine((int)(x - w),(int)(y + w), (int)(x - r), (int)(y + r));
        g2d.drawLine((int)(x + w),(int)(y - w), (int)(x + r), (int)(y - r));
        g2d.drawLine((int)(x + w),(int)(y + w), (int)(x + r), (int)(y + r));


    }

}

class NullShape implements SpeckleShape{

    public void draw(Ellipse2D s, Color a, Graphics2D g2d) {
    }

    public void setZoom(double zoom) {
    }
}

interface SpeckleShape{
    public void draw(Ellipse2D s, Color a, Graphics2D g2d);
    public void setZoom(double zoom);

}
