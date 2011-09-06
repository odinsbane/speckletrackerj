package speckles.controls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
   *    Controls the displayed Image
   *    Actions: update_slider
   **/
   
class ImageControls implements ChangeListener{


    JPanel PANEL;
    JSlider image_slider;
    JLabel frame_label;
    ArrayList<ActionListener> LISTENERS;
    JButton next_button, back_button;
    JButton zoom_in, zoom_out, to_beginning, to_end,play;
    
    public ImageControls(){        
                
        PANEL = new JPanel();
        PANEL.setLayout(new BoxLayout(this.PANEL, BoxLayout.PAGE_AXIS));

        LISTENERS = new ArrayList<ActionListener>();
        PANEL.setLayout(new BoxLayout(PANEL, BoxLayout.PAGE_AXIS));
        
        Border b = StyledComponents.createStyledBorder("Image Controls");
        
        Border matte = BorderFactory.createMatteBorder(2,2,2,2,new Color(200,200,255));
        Border c = BorderFactory.createCompoundBorder(matte,b);
        c = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),c);
        PANEL.setBorder(c);
        
        image_slider = new JSlider(JSlider.HORIZONTAL,1,1,1);
        image_slider.addChangeListener(this);
        
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        
        frame_label = StyledComponents.createStyledLabel("1/1");
        JLabel fl_lead = StyledComponents.createStyledLabel("current/total");
        row.add(fl_lead);
        row.add(Box.createHorizontalStrut(50));
        row.add(frame_label);

        PANEL.add(row);
        
        row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        

        PANEL.add(row);
        
        PANEL.add(image_slider);
        
        row = new JPanel();
        row.setBorder(BorderFactory.createEmptyBorder(10,5,0,5));
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));

        repeater r = new repeater();
        next_button = StyledComponents.createStyledButton("Next");
        next_button.setActionCommand("forward");

        next_button.addMouseListener(r);
        
        back_button = StyledComponents.createStyledButton("Previous");
        back_button.setActionCommand("backward");
        back_button.addMouseListener(r);

        row.add(back_button);
        row.add(next_button);
        
        PANEL.add(row);
        
        zoom_in = new JButton("+");
        zoom_in.setActionCommand("zoomin");
        zoom_out = new JButton("-");
        zoom_out.setActionCommand("zoomout");
        to_beginning = new JButton("|<");
        to_beginning.setActionCommand("tobeginning");
        to_beginning.setMnemonic(KeyEvent.VK_COMMA );
        to_beginning.setToolTipText("move to the first frame of highlighted speckle (alt + <)");
        
        to_end = new JButton(">|");
        to_end.setActionCommand("toend");
        to_beginning.setMnemonic(KeyEvent.VK_COMMA );
        to_beginning.setToolTipText("move to the first frame of highlighted speckle (alt + <)");

        play = new JButton(">>");
        play.setActionCommand("play");


        row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        
        row.add(zoom_in);
        row.add(zoom_out);
        row.add(to_end);
        row.add(to_beginning);
        row.add(play);

        PANEL.add(row);
        
        PANEL.setPreferredSize(new Dimension(300,150));
        PANEL.setMaximumSize(new Dimension(300,150));
        PANEL.setMinimumSize(new Dimension(300,150));

    }
    
    //This will create an action when the slider changes values so it can be handled the same as the buttons
    public void stateChanged(ChangeEvent e) {
        if (!image_slider.getValueIsAdjusting()) {
            //parent.setImageSlice(image_slider.getValue());
            ActionEvent ae = new ActionEvent(image_slider,ActionEvent.ACTION_FIRST,"sliderupdate");
            for(ActionListener al: LISTENERS) 
                al.actionPerformed(ae);
            
        }
    }
    
    public void updateSlider(int b){
        if(image_slider.getValue()!=b)
            image_slider.setValue(b);
        frame_label.setText(b + "/" + image_slider.getMaximum());
        
     }
     
     public void setSliderMaximum(int v){
            image_slider.setMaximum(v);
            frame_label.setText(image_slider.getValue() + "/" + v);
     }
     
     public int getSliderValue(){
        return image_slider.getValue();
     }
    public void addActionListener(ActionListener al){
    
        LISTENERS.add(al);
        next_button.addActionListener(al);
        back_button.addActionListener(al);
        zoom_in.addActionListener(al);
        zoom_out.addActionListener(al);
        to_beginning.addActionListener(al);
        to_end.addActionListener(al);
        play.addActionListener(al);
    }
    
    
    JComponent getComponent(){
   
        return PANEL;
    
   }
    

    /**
      *     This mouse adapter causes a repeated increament to occure if the button is held down
      **/
    class repeater extends MouseAdapter implements Runnable{
        RepeaterLoop loop;
        repeater(){
            loop = new RepeaterLoop();
            loop.start();
        }
        volatile Object pressed = new Object();
        int direction = 0;
        
        /**
          *     When the button is pressed a new Object is created and stored in the variable pressed
          *     direction then stores the direction the slider will move.
         *
          * @param e
         */
        public void mousePressed(MouseEvent e){ 
            JButton src = (JButton)e.getSource();
            pressed = new Object();
            if(src.getActionCommand()=="forward"){
                direction = 1;
                loop.post(this);
            } else if(src.getActionCommand()=="backward") {
                direction = -1;
                loop.post(this);
            }
        }
        
        /**
          *     A Local variable mypress is set to be the class member pressed, and so long as no other process
          *     changes the class variable the following loop will proceed
          **/
          
        public void run(){
            Object mypress = pressed;
            int set = 0;
            while(pressed==mypress){
            
                if(set==0){                 //delay before repeating
                    
                    //try/catch statement because of the thread.
                    try{
                    
                        Thread.sleep((long)750,20);
                        set = 1;
                    
                    } catch(Exception exc){

                        set = 0;
                        mypress = new Object();

                    }
                    
                }else{
                 
                    //short delay to make sure the program isn't bogged down.
                    try {
                    
                        Thread.sleep((long)25,20);
                       
                    } catch(Exception exc){
                       
                       mypress = new Object();

                    }
                    
                    image_slider.setValue(image_slider.getValue()+direction);
                    }
            }
		}
        
    /**
      *     Creates a new object thus terminating any pending  repeats
      **/
      
    public void mouseReleased(MouseEvent ee)
		{
            pressed = new Object();
		}
    
    }

    public void setEnabled(boolean v){
        image_slider.setEnabled(v);
        next_button.setEnabled(v);
        back_button.setEnabled(v);
        zoom_in.setEnabled(v);
        zoom_out.setEnabled(v);
        to_beginning.setEnabled(v);
        to_end.setEnabled(v);
        play.setEnabled(v);
        
    }

    public void enableDirections(){

        next_button.setEnabled(true);
        back_button.setEnabled(true);
        image_slider.setEnabled(true);

    }
   
}

class RepeaterLoop extends Thread{
    ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();

    public void run(){
        for(;;){
            while(queue.size()>0){
                queue.poll().run();
            }
            try{
                waitFor();
            }catch(Exception e){
                break;
            }
        }
    }

    public synchronized void waitFor() throws InterruptedException {
        wait();
    }
    synchronized public void post(Runnable r){
        queue.add(r);
        notifyAll();
    }

}
