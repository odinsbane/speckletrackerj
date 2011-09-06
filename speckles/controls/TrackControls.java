package speckles.controls;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;


/**
   *   
   **/
   
class TrackControls{


    JPanel PANEL;
    JButton track_speckle_button, merge_button, auto_track_button,show_speckle_button,
            auto_track_all_button, trim_speckle_before_button, trim_speckle_after_button;

    
    
    public TrackControls(){
        PANEL = new JPanel();
        
        //PANEL.setLayout(new BoxLayout(PANEL, BoxLayout.PAGE_AXIS));
        GridLayout gl = new GridLayout(5,2);

        PANEL.setLayout(gl);
        
        Border b = StyledComponents.createStyledBorder("Multi-Frame Speckles");
        PANEL.setBorder(b);
        
        track_speckle_button = StyledComponents.createStyledButton("Track Speckle");
        track_speckle_button.setActionCommand(SpeckleCommands.trackspeckle.name());
        track_speckle_button.setToolTipText("Used to manually track speckle in next frame (t) .");

        merge_button = StyledComponents.createStyledButton("Merge Speckles");
        merge_button.setActionCommand(SpeckleCommands.merge.name());
        merge_button.setToolTipText("Used to merge two speckles this frame and previous frame (p) .");
        
        auto_track_button = StyledComponents.createStyledButton("Auto-track");
        auto_track_button.setActionCommand(SpeckleCommands.autotrack.name());
        auto_track_button.setToolTipText("Tracks this speckle through frames (a)");
        
        auto_track_all_button = StyledComponents.createStyledButton("Auto-track All");
        auto_track_all_button.setActionCommand("autotrackall");
        auto_track_all_button.setToolTipText("Tracks all speckles through frames.");
        
        show_speckle_button = StyledComponents.createStyledButton("Show Speckle");
        show_speckle_button.setToolTipText("Creates a Stack of images containing the current speckle (s)");
        show_speckle_button.setActionCommand(SpeckleCommands.showspeckle.name());
        
        trim_speckle_before_button = StyledComponents.createStyledButton("Trim Before");
        trim_speckle_before_button.setActionCommand(SpeckleCommands.trimbefore.name());
        trim_speckle_before_button.setToolTipText("Remove all instances of speckle before current frame");
        
        trim_speckle_after_button = StyledComponents.createStyledButton("Trim After");
        trim_speckle_after_button.setActionCommand(SpeckleCommands.trimafter.name());
        trim_speckle_after_button.setToolTipText("Remove all instances of speckle after current frame");

        PANEL.add(track_speckle_button);
        PANEL.add(merge_button);
        PANEL.add(show_speckle_button);
        
        PANEL.add(auto_track_button);
        
        PANEL.add(auto_track_all_button);
        PANEL.add(trim_speckle_after_button);
        PANEL.add(trim_speckle_before_button);

        // PANEL.setPreferredSize(new Dimension(600,100));
        // PANEL.setMaximumSize(new Dimension(600,100));
        // PANEL.setMinimumSize(new Dimension(600,100));

        
    }
        
    
    public void addActionListener(ActionListener al){

        merge_button.addActionListener(al);
        track_speckle_button.addActionListener(al);
        show_speckle_button.addActionListener(al);
        auto_track_button.addActionListener(al);
        auto_track_all_button.addActionListener(al);
        trim_speckle_after_button.addActionListener(al);
        trim_speckle_before_button.addActionListener(al);
    }
    
    
    JComponent getComponent(){
   
        return PANEL;
    
   }

    public void setEnabled(boolean v){

        track_speckle_button.setEnabled(v);
        merge_button.setEnabled(v);
        show_speckle_button.setEnabled(v);
        auto_track_button.setEnabled(v);
        auto_track_all_button.setEnabled(v);
        trim_speckle_after_button.setEnabled(v);
        trim_speckle_before_button.setEnabled(v);
    }
    

}
