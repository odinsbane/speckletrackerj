package speckles.controls;

/**
   *    The purpose of this class is merely to start the controls
   *
   *
   **/

import ij.ImagePlus;
import speckles.Speckle;
import speckles.models.SpeckleModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;

public class SpeckleControls implements ActionListener{
    JFrame basic;
    ImageControls image_controls;
    SpeckleToolControls speckle_tool_controls;
    AnalysisControls analysis_controls;
    LocateControls locate_controls;
    TrackControls track_controls;
    MessagePanel message_panel;
    SpeckleMenu speckle_menu;
    ProfileControl profiler;
    SpeckleSelector speckle_selector;
    ResliceControl reslice_control;

    JScrollPane image_display;
    
    speckles.SpeckleApp parent;
    SpeckleWorker WORKER;
    ModelMessages MODMESS;

    boolean REQUEST_FOCUS = true;
    private boolean CHANGED = false;
    int DEFAULT_CLOSE = JFrame.DISPOSE_ON_CLOSE;
    public static final String[] TITLES = {
        "Monty Python: Knights of the holy speckle",
        "On any speckle",
        "Specklestruck",
        "Lord of the Speckles",
        "The speckle before Christmas",
        "Mrs. Specklefire",
        "Driving Ms. Speckle",
        "Speckle me this",
        "Speckle me if you can",
        "Days of our Speckle",
        "Independence Speckle",
        "National Lampoons: Speckle House",
        "Bottle Speckle",
        "Night of the Living Speckle",
        "Reservoir Speckles",
        "Speckle Holmes",
        "Speckleman",
        "IronSpeckle",
        "Speckle about Mary",
        "Speckle and Speckler"
        };
        
    public SpeckleControls(){
        basic = new JFrame(TITLES[(int)(Math.random()*TITLES.length)]);
        basic.setSize(1200,760);

        basic.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        basic.addWindowListener(new WindowListener(){
            public void windowClosing(WindowEvent e) {
                int test = JOptionPane.OK_OPTION;
                if(CHANGED)
                    test = JOptionPane.showConfirmDialog(basic, "The speckles have been modified do you still wish to close?");

                if (test==JOptionPane.OK_OPTION)
                    basic.dispose();
                
            }

            public void windowClosed(WindowEvent e) {
                if(DEFAULT_CLOSE==JFrame.EXIT_ON_CLOSE)
                    System.exit(0);
            }

            public void windowOpened(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowActivated(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
        });


        
        JFrame.setDefaultLookAndFeelDecorated(true); 

        
        
        image_controls = new ImageControls();
        speckle_tool_controls = new SpeckleToolControls();
        analysis_controls = new AnalysisControls();
        locate_controls = new LocateControls();
        track_controls = new TrackControls();
        message_panel = new MessagePanel();
        speckle_menu = new SpeckleMenu();
        speckle_selector = new SpeckleSelector();
        
        profiler = new ProfileControl();
        
        /** new */
        //Container cp = basic.getContentPane();
        Container cp = new JPanel();
        
        
        cp.setLayout(new BoxLayout(cp,BoxLayout.PAGE_AXIS));
        cp.add(Box.createVerticalStrut(10));
        add(cp,image_controls.getComponent());

        JTabbedPane tp = StyledComponents.createStyledTabbedPane();
        //tp.add("tools",speckle_tool_controls.getComponent());
        //tp.add("selector", speckle_selector.getComponent());

        add(cp,speckle_tool_controls.getComponent());
        
        //tp = StyledComponents.createStyledTabbedPane();
        tp.add("track",track_controls.getComponent());
        tp.add("analyze",analysis_controls.getComponent());
        tp.add("locate",locate_controls.getComponent());
        
        add(cp,tp);

        cp.add(Box.createVerticalGlue());
        
        
        //basic.setVisible(true);
        
        cp.setBackground(Color.LIGHT_GRAY);
        
        /** new */
        Container content = basic.getContentPane();
        content.add(cp,BorderLayout.WEST);
        
        JComponent x = new JPanel();
        x.setBackground(Color.LIGHT_GRAY);
        x.setLayout(new BoxLayout(x,BoxLayout.PAGE_AXIS));
        x.add(Box.createVerticalStrut(20));
        JComponent y = new JPanel();
        y.setLayout(new BoxLayout(y,BoxLayout.LINE_AXIS));
        y.add(profiler.getComponent());
        y.add(message_panel.getComponent());
        add(x,y);
        x.add(Box.createVerticalStrut(20));
        
        content.add(x,BorderLayout.SOUTH);

        createSelectorPane(content, speckle_selector.getComponent());

        MODMESS = new ModelMessages();

        /** setup image display */
        image_display = new JScrollPane(
                                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED ,
                                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                        ){
            @Override
            public void paint(Graphics g){
                super.paint(g);
                MODMESS.paint(g);
            }
        };

        MODMESS.addPanel(image_display);
        new Thread(MODMESS).start();

        image_display.setFocusable(false);
        
        InputMap am = image_display.getInputMap();
        //am.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0),null);
        image_display.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,am);
        image_display.setInputMap(JComponent.WHEN_FOCUSED, am);
        
        
        //image_display.setHorizontalScrollBar(new JScrollBar(Adjustable.HORIZONTAL));
        content.add(image_display,BorderLayout.CENTER);
        
        basic.setJMenuBar(speckle_menu.getMenuBar());
    }

    public void setVisible(boolean t){
        basic.setVisible(t);
    }

    public void startWorkerThread(){
        WORKER = new SpeckleWorker(parent,this);
        WORKER.start();
    }

    public SpeckleControls(speckles.SpeckleApp sa){
        this();
        
        parent = sa;
        profiler.setParent(sa);
        speckle_selector.setParent(sa);
        
        addActionListener(this);
        JViewport vp = new JViewport();
        vp.setView(parent.getImagePanel());
        image_display.setViewport(vp);

        reslice_control = parent.startResliceControl();

        startWorkerThread();

      
    }
    
    public void addActionListener(ActionListener t){
        
        image_controls.addActionListener(t);
        speckle_tool_controls.addActionListener(t);
        analysis_controls.addActionListener(t);
        locate_controls.addActionListener(t);
        track_controls.addActionListener(t);
        speckle_menu.addActionListener(t);
        message_panel.addActionListener(t);
        speckle_selector.addActionListener(t);
    }
    
    public void setCloseOnExit(boolean v){
        if(v)
            DEFAULT_CLOSE = JFrame.EXIT_ON_CLOSE;
        else
            DEFAULT_CLOSE = JFrame.DISPOSE_ON_CLOSE;
    }
    
    public static void main(String[] args){
        SpeckleControls x = new SpeckleControls();
        
        ActionListener t = new TestListener();
        x.addActionListener(t);
        
    }
    public JFrame getFrame(){
        return basic;
    }
    
    static private void add(Container cp, JComponent a){
        cp.add(a);
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        a.setAlignmentX(Component.CENTER_ALIGNMENT);
        
    }
    /**
       *  TRANSITION
       **/
    public void actionFinished(){
        updateStatus();

    }
        
    public void setMaximumImage(int v){
        image_controls.setSliderMaximum(v);
    }
    
    public int getSliderValue(){
        return image_controls.getSliderValue();
    }
    /**
      *     Updates the relevant display mechanisms
      **/
    public void updateStatus(){
        image_controls.updateSlider(parent.getCurrentSlice());
        if(REQUEST_FOCUS)
            parent.getImagePanel().requestFocus();
        profiler.updateFrameMarker();
    }
    /**
       *    Gets the speckle type for collecting images
       **/
    public int getSpeckleType(){
        
        return analysis_controls.getSpeckleType();
    
    }
    
    public int getSquareSize(){
            return analysis_controls.getSquareSize();
    }
    
    public int getRelative(){
            return analysis_controls.getRelative();
    }
        
    public int getForegroundColor(){
            return 0xffffff;
    }
    
    public int getBackgroundColor(){
            return 0xff0000;
    }
    
    public void actionPerformed(ActionEvent e) {

        WORKER.submitCommand(e.getActionCommand());

     }
    /**
     * Shows message in the display, this action will be posted to the EventQueue.
     * @param message that which will be displayed.
     */
     public void showMessage(String message){
        
        message_panel.showMessage(message);
     
     }

    /**
      * goes back to normal
      **/
    public void resetReady(){
        REQUEST_FOCUS=true;
        image_controls.setEnabled(true);
        reslice_control.setEnabled(true);
        speckle_tool_controls.setEnabled(true);
        analysis_controls.setEnabled(true);
        locate_controls.setEnabled(true);
        track_controls.setEnabled(true);
        speckle_selector.setEnabled(true);
        profiler.setEnabled(true);

        message_panel.setEnabled(false);
        message_panel.setRunning(false);

        showMessage("ready");

           

    }

    public void setWaiting(){
        image_controls.setEnabled(false);
        reslice_control.setEnabled(false);
        speckle_tool_controls.setEnabled(false);
        analysis_controls.setEnabled(false);
        locate_controls.setEnabled(false);
        track_controls.setEnabled(false);
        message_panel.setEnabled(true);
        speckle_selector.setEnabled(false);
        profiler.setEnabled(false);
        setRunning(true);

    }
    
    public void setDialogWaiting(){
        REQUEST_FOCUS = false;
        image_controls.setEnabled(false);
        reslice_control.setEnabled(false);
        speckle_tool_controls.setEnabled(false);
        analysis_controls.setEnabled(false);
        locate_controls.setEnabled(false);
        track_controls.setEnabled(false);
        message_panel.setEnabled(false);
        speckle_selector.setEnabled(false);
        profiler.setEnabled(false);

    }
    
    public int getModelIndex(){
        int i = speckle_menu.getModelIndex();
        MODMESS.setMessage(SpeckleModel.Models.values()[i].name());
        return i;
        
    }

    public void nextModel(){
        int i = speckle_menu.getModelIndex();
        i++;
        if(i>= SpeckleModel.Models.values().length)
            i = 0;
        speckle_menu.setModelIndex(i);
    }

    public void previousModel(){
        int i = speckle_menu.getModelIndex();
        i--;
        if(i<0)
            i = SpeckleModel.Models.values().length-1;
        speckle_menu.setModelIndex(i);

    }
    
    public void profileSpeckle(Speckle speck, ImagePlus imp){
        reslice_control.unsetMarker();
        parent.updateResliceSpeckles();
        profiler.profileSpeckle(speck, imp);
        
    }
    
    synchronized public void setRunning(boolean t){

        message_panel.setRunning(t);
    }


    public void updateSelector(HashSet<Speckle> speckles){
        final HashSet<Speckle> set = speckles;
        EventQueue.invokeLater(new Runnable(){
                public void run(){
                    speckle_selector.updateSelector(set);
                }
            }
        );


    }

    public void updateAppearances(ImagePlus imp){

        speckle_selector.updateAppearances(imp);
        
    }

    public void copySelectorToTextWindow() {
        speckle_selector.createTextWindowTable();
    }

    private void createSelectorPane(Container content, JComponent selector){
        JPanel moving = new JPanel();
        BoxLayout lay = new BoxLayout(moving,BoxLayout.LINE_AXIS);
        moving.setLayout(lay);

        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(25,150));
        left.setMaximumSize(new Dimension(25,150));
        left.setMinimumSize(new Dimension(25,150));

        MoverListener ml = new MoverListener(content,moving);
        JButton toggle = new JButton("<");
        toggle.setToolTipText("Show Selector Table");
        toggle.setFocusable(false);
        toggle.addActionListener(ml);

        ml.setButton(toggle);
        
        left.add(toggle);
        left.setAlignmentY(0);
        moving.add(left);
        moving.add(selector);        
        moving.setPreferredSize(new Dimension(25,150));
        moving.setMaximumSize(new Dimension(25,150));
        moving.setMinimumSize(new Dimension(25,150));
        content.add(moving,BorderLayout.EAST);


    }

    public void enableDirections(){
        reslice_control.setEnabled(true);
        profiler.setEnabled(true);
        image_controls.enableDirections();
        
    }

    public void touchSpeckles(){
        CHANGED = true;
    }

    public boolean checkChange(){

        return CHANGED;
        
    }

    public void clearChange(){

        CHANGED = false;

    }
}

class SlidingThread extends Thread{
    Container parent;
    JComponent child;
    boolean running=false;
    int end_state;
    int current;
    int delta = 125;

    boolean opening;

    SlidingThread(Container parent, JComponent child){
        this.parent = parent;
        this.child = child;

    }
    public void run(){
        try{
            for(;;){
                while(running){
                    performAnimation();
                }
                waitForIt();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void performAnimation() throws InterruptedException{
        Thread.sleep(50);




        if(opening){
            current += delta;
            if(current>=end_state){
                current=end_state;
                stopAnimation();
            }


        } else{
            current-=delta;
            if(current<=end_state){
                current = end_state;
                stopAnimation();
            }

        }



        EventQueue.invokeLater(new Runnable(){
           public void run(){
               child.setPreferredSize(new Dimension(current,150));
               child.setMaximumSize(new Dimension(current,150));
               child.setMinimumSize(new Dimension(current,150));
               child.invalidate();
               parent.validate();
               //parent.repaint(child.getX(),child.getY(),current,parent.getHeight());
           }

        });


    }

    synchronized private void waitForIt(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void startAnimation(){
        notify();
        running=true;
    }

    synchronized public void stopAnimation(){
        running=false;
    }

    public void setFinalState(int end_state){
        if(this.end_state==end_state)
            return;

        this.end_state=end_state;
        opening = end_state>50;
        startAnimation();
    }



}

class MoverListener implements ActionListener{
    Container parent;
    JComponent child;
    int x = 25;
    JButton button;
    SlidingThread t;

    MoverListener(Container p, JComponent c){
        parent = p;
        child = c;
        t = new SlidingThread(p,c);
        t.start();
    }
    public void setButton(JButton b){
        button = b;
    }

    public void actionPerformed(ActionEvent e) {
        x = x==25?500:25;

        t.setFinalState(x);

        parent.validate();
        if(button!=null){
            String s = x==25?"<":">";
            button.setText(s);
        }
    }
}
class TestListener implements ActionListener{
        public TestListener(){
        
    }
    public void actionPerformed(ActionEvent e){
        String cmd = e.getActionCommand();
        SpeckleCommands trans;
        try{
            trans = SpeckleCommands.valueOf(cmd);
        } catch(java.lang.IllegalArgumentException exc){
            System.out.println("Command not found: " + cmd);
            trans = SpeckleCommands.nothing;
        }
    }
}

class ModelMessages implements Runnable{
    float alpha = 0f;
    private Component component;
    private String message = "";

    public void paint(Graphics g){
        if(alpha>0){
            Graphics2D g2d = (Graphics2D)g;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));
            g.setColor(new Color(0xffffff));

            g.setFont(new Font("monospace",Font.BOLD,36));
            g.drawString(message,50,50);
        }
    }
    public synchronized void setMessage(String mess){
        message = mess;
        alpha = 1f;
        notify();
    }

    public void run(){

        for(;;){
            waitFor();
            fadeOut();
        }


    }

    public void fadeOut(){
        while(alpha>0){

            EventQueue.invokeLater(new Runnable(){public void run(){component.repaint();}});
            pause();
            alpha -= 0.1f;

        }

        EventQueue.invokeLater(new Runnable(){public void run(){component.repaint();}});
    }

    public void pause(){
        try{
            Thread.sleep(50);
        } catch(Exception e){
            alpha = 0;
        }

    }

    synchronized private void waitFor(){
        try{
            wait();
        } catch(Exception e){
            alpha = 0;
        }

    }

    public void addPanel(Component c){
        component = c;
    }


}

