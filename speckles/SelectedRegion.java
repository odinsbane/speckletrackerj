package speckles;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * This class will limit the region that speckles can be autodetected/autotracked too.
 *
 * 
 * User: mbs207
 * Date: Sep 23, 2010
 * Time: 11:54:57 AM
 */
public class SelectedRegion implements MouseMotionListener, MouseListener {
    int X=0;
    int Y=0;
    int WIDTH=0;
    int HEIGHT=0;
    int x1,y1,x2,y2;

    boolean DRAW=false;
    boolean USE=false;
    boolean ACTIVE=false;

    boolean DRAGGING = false;
    private Component c;
    final Color color = new Color(0xff5555ff);
    SelectedRegion(Component c){

        this.c = c;

    }
    public void paint(Graphics2D g){
        if(!DRAW)
            return;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.25f));
        g.setColor(color);

        g.fillRect(X,Y,WIDTH,HEIGHT);
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        int but = e.getButton();
        if(but==MouseEvent.BUTTON1)
            return;
        x1 = e.getX();
        y1 = e.getY();
        DRAGGING=true;
    }

    public void mouseReleased(MouseEvent e) {
        DRAGGING=false;
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {
        DRAGGING=false;
    }

    public void mouseDragged(MouseEvent e) {
        if(!DRAGGING)
            return;
        x2 = e.getX();
        y2 = e.getY();
        calculateRegion();
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void calculateRegion(){

        if(x1>x2){
            X = x2;
            WIDTH = x1 - x2;
        } else{
            X = x1;
            WIDTH = x2 - x1;
        }
        if(y1>y2){
            Y = y2;
            HEIGHT = y1 - y2;
        } else{
            Y = y1;
            HEIGHT = y2 - y1;
        }
        DRAW=true;
        EventQueue.invokeLater(new Runnable(){ public void run(){c.repaint();}});
    }

    public void setDraw(boolean v){
        DRAW = v;
        EventQueue.invokeLater(new Runnable(){ public void run(){c.repaint();}});
        
    }
    public void setActive(boolean t){
        ACTIVE = t;
        if(t){
            c.addMouseListener(this);
            c.addMouseMotionListener(this);
        } else{
            c.removeMouseListener(this);
            c.removeMouseMotionListener(this);
        }

    }
    
}
