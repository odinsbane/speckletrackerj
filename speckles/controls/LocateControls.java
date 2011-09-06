package speckles.controls;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;


/**
   *   
   **/
   
class LocateControls{


    JPanel PANEL;
    JButton loc_max_button, loc_thresh_button,loc_correlate_button, loc_max_all_button;

    
    
    public LocateControls(){
        PANEL = new JPanel();
        
        //PANEL.setLayout(new BoxLayout(PANEL, BoxLayout.PAGE_AXIS));
        PANEL.setLayout(new GridLayout(5,2));
                
        Border b = StyledComponents.createStyledBorder("Auto-Locate Speckles");
        PANEL.setBorder(b);
        
        loc_max_button = StyledComponents.createStyledButton("Adjust Speckle");
        loc_max_button.setActionCommand(SpeckleCommands.maxlocatespeckle.name());
        loc_max_button.setToolTipText("Moves Speckles to thier Closest maximum and removes sufficient overlap speckles (m)");
        PANEL.add(loc_max_button);
        
        loc_max_all_button = StyledComponents.createStyledButton("Adjust All");
        loc_max_all_button.setActionCommand(SpeckleCommands.maxlocate.name());
        loc_max_all_button.setToolTipText("Moves all speckles in current frame a small amount.");
        PANEL.add(loc_max_all_button);
        
        loc_thresh_button = StyledComponents.createStyledButton("Locate Speckles");
        loc_thresh_button.setActionCommand(SpeckleCommands.thresholdlocate.name());
        loc_thresh_button.setToolTipText("Attempts to autolocate speckles based on thresholds.");
        
        
        loc_correlate_button = StyledComponents.createStyledButton("Template Locate");
        loc_correlate_button.setActionCommand(SpeckleCommands.correlatelocate.name());
        loc_correlate_button.setToolTipText("Uses the current speckles to create a criteria for a speckle and autolocate based on that.");
        

        PANEL.add(loc_thresh_button);
        PANEL.add(loc_correlate_button);

        //PANEL.setPreferredSize(new Dimension(600,100));
        //PANEL.setMaximumSize(new Dimension(600,100));
        //PANEL.setMinimumSize(new Dimension(600,100));

        
    }
        
    
    public void addActionListener(ActionListener al){

        loc_thresh_button.addActionListener(al);
        loc_max_button.addActionListener(al);
        loc_max_all_button.addActionListener(al);
        loc_correlate_button.addActionListener(al);

    }
    
    
    JComponent getComponent(){
   
        return PANEL;
    
   }
    public void setEnabled(boolean v){
        loc_max_button.setEnabled(v);
        loc_thresh_button.setEnabled(v);
        loc_correlate_button.setEnabled(v);
        loc_max_all_button.setEnabled(v);
    }
    

}
