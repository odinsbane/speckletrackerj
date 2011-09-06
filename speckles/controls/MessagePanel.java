package speckles.controls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


/**
   *   Displays messages and has the ..." cancel " button.
   **/
   
class MessagePanel{


    JPanel PANEL;
    JLabel message_label;
    JButton cancel_button;
    
    JProgressBar progress;
    public MessagePanel(){
        PANEL = new JPanel();
        
        PANEL.setBackground(Color.LIGHT_GRAY);
        PANEL.setLayout(new BoxLayout(PANEL, BoxLayout.LINE_AXIS));
        PANEL.add(Box.createHorizontalStrut(40));
        JPanel mp = new JPanel();
        mp.setLayout(new BoxLayout(mp, BoxLayout.PAGE_AXIS));
        
        mp.setBorder(BorderFactory.createLoweredBevelBorder());
       
        message_label = new JLabel("<html><body>Messages:</body></html>");

        cancel_button = StyledComponents.createStyledButton("cancel");
        cancel_button.setActionCommand(SpeckleCommands.cancel.name());
         //message_label.setMinimumSize(new Dimension(480,100));
        // message_label.setPreferredSize(new Dimension(480,100));
        // message_label.setMaximumSize(new Dimension(480,100));
        
        mp.add(Box.createVerticalStrut(20));

        message_label.setAlignmentX(Component.CENTER_ALIGNMENT);
        mp.add(message_label);
        cancel_button.setAlignmentX(Component.CENTER_ALIGNMENT);
        mp.add(cancel_button);
        
        progress = new JProgressBar();
        
        mp.add(progress);

        PANEL.add(mp);
        PANEL.add(Box.createHorizontalStrut(40));

        // PANEL.setPreferredSize(new Dimension(600,100));
        // PANEL.setMaximumSize(new Dimension(600,100));
         //PANEL.setMinimumSize(new Dimension(600,100));

        
    }
        
    
    public void addActionListener(ActionListener al){

        cancel_button.addActionListener(al);
            
    }

    /**
     * Shows message in the display, posts this action to the EventQueue.
     * @param message that which will be displayed.
     */
    public void showMessage(String message){
        final String m = message;
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                message_label.setText("<html><body>" + m + "</body></html>");
            }
        });

    
    }
    
    JComponent getComponent(){
   
        return PANEL;
    
   }
    public void setEnabled(boolean v){
        
        cancel_button.setEnabled(v);        
        
    }
    
    public void updateProgress(int value){
        
        progress.setIndeterminate(false);
        progress.setValue(value);
    }
    
    public void setRunning(boolean t){
        final boolean tt = t;
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                progress.setIndeterminate(tt);
            }
        });
        
    }
    

}
