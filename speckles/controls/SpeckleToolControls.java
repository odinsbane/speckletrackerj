package speckles.controls;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;


/**
   *   
   **/
   
class SpeckleToolControls{


    JPanel PANEL;
    //JSpinner speckle_mark_spinner;
    
    JButton copy_previous, clear_current, toggle_shape,update_reslice_button,
            add_selection_button,clear_selection_button, split_speckle_button;

    
    
    public SpeckleToolControls(){
        PANEL = new JPanel();
        
        //PANEL.setLayout(new BoxLayout(PANEL, BoxLayout.PAGE_AXIS));
        PANEL.setLayout(new GridLayout(5,2));
        Border b = StyledComponents.createStyledBorder("Speckle Tools");
        PANEL.setBorder(b);
        
        copy_previous = StyledComponents.createStyledButton("Copy Previous");
        copy_previous.setActionCommand(SpeckleCommands.copyprevious.name());

        

        PANEL.add(copy_previous);

        clear_current = StyledComponents.createStyledButton("Clear Current");
        clear_current.setActionCommand(SpeckleCommands.clearspeckles.name());
        

        PANEL.add(clear_current);

        toggle_shape = StyledComponents.createStyledButton("Toggle Marker");
        toggle_shape.setActionCommand(SpeckleCommands.toggleshape.name());
        PANEL.add(toggle_shape);


        update_reslice_button = StyledComponents.createStyledButton("Show Resliced");
        update_reslice_button.setActionCommand(SpeckleCommands.updatereslice.name());
        update_reslice_button.setToolTipText("Remove all instances of speckle after current frame");

        PANEL.add(update_reslice_button);

        add_selection_button = StyledComponents.createStyledButton("Select Region");
        add_selection_button.setActionCommand(SpeckleCommands.setselection.name());
        add_selection_button.setToolTipText("Select a rectangle for autotracking/detecting.");

        PANEL.add(add_selection_button);

        clear_selection_button = StyledComponents.createStyledButton("Clear Selection");
        clear_selection_button.setActionCommand(SpeckleCommands.clearselection.name());
        clear_selection_button.setToolTipText("Removes the locate selection.");

        PANEL.add(clear_selection_button);

        split_speckle_button = StyledComponents.createStyledButton("Split Track");
        split_speckle_button.setActionCommand(SpeckleCommands.splitspeckle.name());
        split_speckle_button.setToolTipText("Splits the currently selected track.");

        PANEL.add(split_speckle_button);

        PANEL.setPreferredSize(new Dimension(300,150));
        PANEL.setMaximumSize(new Dimension(300,150));
        PANEL.setMinimumSize(new Dimension(300,150));

        
        
        
    }
        
    
    public void addActionListener(ActionListener al){
            copy_previous.addActionListener(al); 
            clear_current.addActionListener(al);
            toggle_shape.addActionListener(al);
            update_reslice_button.addActionListener(al);
            add_selection_button.addActionListener(al);
            clear_selection_button.addActionListener(al);
            split_speckle_button.addActionListener(al);

    }
    
    
    JComponent getComponent(){
   
        return PANEL;
    
    }
    public void setEnabled(boolean v){
        copy_previous.setEnabled(v);
        clear_current.setEnabled(v);
        toggle_shape.setEnabled(v);
        update_reslice_button.setEnabled(v);
        add_selection_button.setEnabled(v);
        clear_selection_button.setEnabled(v);
        split_speckle_button.setEnabled(v);
    }   
    

}
