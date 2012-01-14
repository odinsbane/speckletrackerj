package speckles;


import java.awt.event.*;
import java.awt.geom.Point2D;

/**
   *    The basic listener used for adding and removing speckles.
   **/
public class SpeckleListener implements MouseListener,MouseMotionListener,KeyListener{
    //ImageCanvas draw_canvas;
    SpeckleImage draw_panel;
    SpeckleApp parent;
    
    //Listening Modes
    static final int NORMAL_MODE = 0;
    static final int GET_TRACK_MODE = 3;
    static final int PLACE_TRACK_MODE=4;
    static final int GET_MERGE_MODE = 5;
    static final int PLACE_MERGE_MODE = 6;
    static final int GET_AUTOTRACK_MODE = 7;
    static final int GET_SHOW_MODE = 8;
    static final int GET_DELETE_MODE = 9;
    static final int GET_TRIMBEFORE_MODE = 11;
    static final int GET_TRIMAFTER_MODE = 12;
    static final int PLAYING = 13;
    
    int MODE;

    int CLICK_MOD;
    //Mouse clickes mods
    static final int REMOVE_MOD = InputEvent.SHIFT_DOWN_MASK | InputEvent.BUTTON1_DOWN_MASK;
    static final int ADD_MOD = InputEvent.CTRL_DOWN_MASK | InputEvent.BUTTON1_DOWN_MASK;
    static final int SHOW_MOD = InputEvent.BUTTON1_DOWN_MASK;
    Speckle track_speckle = null;
    Speckle merge_speckle = null;
    
    //cursor positions
    public int CX = 0;
    public int CY = 0;

    Point2D LAST;

    public boolean DRAGGING=false;

  public SpeckleListener(SpeckleImage draw_panel,SpeckleApp myapp){
    this.draw_panel = draw_panel;
    this.draw_panel.addMouseListener(this);
    this.draw_panel.addMouseMotionListener(this);
    this.draw_panel.addKeyListener(this);

    MODE = NORMAL_MODE;
    parent = myapp;
  }

  public void mouseClicked(MouseEvent e){
        //Remove existing points
        switch( MODE ){
            case NORMAL_MODE:
                normalClick(e);
                break;
            case PLACE_TRACK_MODE:
                placeTrack(e);
                break;
            case GET_TRACK_MODE:
                getTrackSpeckle(e);
                break;
            case GET_MERGE_MODE:
                getMergeSpeckle(e);
                break;
            case PLACE_MERGE_MODE:
                mergeSpeckle(e);
                break;
            case GET_AUTOTRACK_MODE:
                autoTrack(e);
                break;
            case GET_SHOW_MODE:
                getShowSpeckle(e);
                break;
            case GET_DELETE_MODE:
                getDeleteSpeckle(e);
                break;
            case GET_TRIMAFTER_MODE:
                getTrimAfterSpeckle(e);
                break;
            case GET_TRIMBEFORE_MODE:
                getTrimBeforeSpeckle(e);
                break;
        }
    }
    public void normalClick(MouseEvent e){
        if((CLICK_MOD & REMOVE_MOD) == REMOVE_MOD){
            parent.removeSpeckle(draw_panel.offScreenX(e.getX()),draw_panel.offScreenY(e.getY()));
        } else if((CLICK_MOD & ADD_MOD) == ADD_MOD){

            parent.addSpeckle(draw_panel.offScreenXD(e.getX()),draw_panel.offScreenYD(e.getY()));

        } else if((CLICK_MOD & InputEvent.BUTTON1_DOWN_MASK)==InputEvent.BUTTON1_DOWN_MASK){
            
            parent.selectSpeckle(draw_panel.offScreenXD(e.getX()),draw_panel.offScreenYD(e.getY()));
            
        }

    }
    
    public void placeTrack(MouseEvent e){
        parent.updateSpeckle(track_speckle , draw_panel.offScreenXD(e.getX()) ,draw_panel.offScreenYD(e.getY()));
        parent.endActions();
        parent.highlightSpeckle(draw_panel.offScreenX(e.getX()),draw_panel.offScreenY(e.getY()));

    }

    public void getTrackSpeckle(MouseEvent e){
        Speckle speck = parent.getSpeckleAt(osX(e),osY(e));
        if(speck != null){
            setTrackSpeckle(speck);
            setMode(PLACE_TRACK_MODE);
            parent.stepForward();
            parent.showSpeckleFromPrevious(speck);
            parent.showMessage("Select where the speckle will appear next");
        }
        
    }

  public void mouseEntered(MouseEvent e){
    }

  public void mouseExited(MouseEvent e){
  }

  public void mousePressed(MouseEvent e){
        CLICK_MOD = e.getModifiersEx();
        LAST = e.getPoint();
        DRAGGING = MODE==NORMAL_MODE&&e.getButton()==MouseEvent.BUTTON1&&parent.startedDragging(LAST);

  }

  public void mouseReleased(MouseEvent e){
    LAST=null;
    DRAGGING=false;
  }

  public void mouseDragged(MouseEvent e){

      if(LAST!=null&&DRAGGING)
        parent.dragSpeckle(e.getPoint(), LAST);
      LAST=e.getPoint();

  }
  public void mouseMoved(MouseEvent e){
        CX = draw_panel.offScreenX(e.getX());
        CY = draw_panel.offScreenY(e.getY());
        parent.highlightSpeckle(CX,CY);
  }
  
  public void keyPressed(KeyEvent e){
    if(MODE==NORMAL_MODE){
        switch(e.getKeyCode()){
            case KeyEvent.VK_Z:
            case KeyEvent.VK_LEFT:
                parent.stepBackward();
                break;
            case KeyEvent.VK_X:
            case KeyEvent.VK_RIGHT:
                parent.stepForward();
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_T:
                parent.trackSpeckle();
                break;
            case KeyEvent.VK_N:
            case KeyEvent.VK_P:
                parent.mergeSpeckles();
                break;
            case KeyEvent.VK_W:
                parent.showSpeckleAllFrames();
                break;
            case KeyEvent.VK_A:
                parent.autoTrack();
                break;
            case KeyEvent.VK_D:
                parent.deleteSpeckle();
                break;
            case KeyEvent.VK_EQUALS:
                parent.zoomIn();
                break;
            case KeyEvent.VK_MINUS:
                parent.zoomOut();
                break;
            case KeyEvent.VK_C:
            case KeyEvent.VK_M:
                parent.maxLocateSpeckle();
                break;
            case KeyEvent.VK_S: 
            case KeyEvent.VK_UP:
                parent.previousModel();
                break;
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_DOWN:
                parent.nextModel();
                break;
        }
    } else  if(e.getKeyCode()==KeyEvent.VK_ESCAPE){
        parent.endActions();
    } else if(MODE==PLACE_MERGE_MODE||MODE==PLACE_TRACK_MODE){
        //both of these modes have directions enabled.
        switch(e.getKeyCode()){
            case KeyEvent.VK_LEFT:
                parent.stepBackward();
                break;
            case KeyEvent.VK_RIGHT:
                parent.stepForward();
                break;
        }

    }
    
  }
  public void keyReleased(KeyEvent e){
  
  }
  
  public void keyTyped(KeyEvent e){

  }
  
  public void setMode(int mode){
  
        MODE=mode;
  }
  
  /**
     *      Cleans up everything as though
     **/
  public void resetMode(){
  
        MODE=NORMAL_MODE;
        track_speckle = null;
  }
  
  
  public void setTrackSpeckle(Speckle s){
    
        track_speckle = s;
  
  }

  public void getMergeSpeckle(MouseEvent e){

    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s != null)
        getMergeSpeckle(s);
  }
  
  public void getDeleteSpeckle(MouseEvent e){
    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s != null)
        parent.deleteSpeckle(s);
  }
  
  public void getMergeSpeckle(Speckle s){
        merge_speckle = s;
        setMode(PLACE_MERGE_MODE);
        parent.showSpeckleFromPrevious(s);
        parent.showMessage("Select Speckle to Merge With");
        parent.enableDirections();
  
  }
  public void mergeSpeckle(MouseEvent e){
    
    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s!=null){
        parent.mergeSpeckles(merge_speckle,s); 
        parent.endActions();
    }
  }
  
  public void autoTrack(MouseEvent e){
    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s!=null)
        parent.autoTrack(s);
  
  }
  
  public void getShowSpeckle(MouseEvent e){
    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s!=null)
        parent.showSpeckleAllFrames(s);
  
  }
  
    public int osX(MouseEvent e){
  
        return draw_panel.offScreenX(e.getX());
  
    }
    public int osY(MouseEvent e){
  
    return draw_panel.offScreenY(e.getY());
  
  }
  public void disableUI(){
      
  }
  public void getTrimBeforeSpeckle(MouseEvent e){
    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s!=null)
        parent.trimBefore(s);
  
  }
  
  public void getTrimAfterSpeckle(MouseEvent e){
    Speckle s = parent.getSpeckleAt(osX(e),osY(e));
    if(s!=null)
        parent.trimAfter(s);
  
  }
}
