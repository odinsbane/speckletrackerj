package speckles.controls;

import speckles.Speckle;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
   *   
   **/
   
class AnalysisControls{

    public static final HashMap<String,Integer> TYPE_MAP = createTypeMap();
    
    
    JPanel PANEL;

    JSpinner speckle_type_spinner;
    JTextField halfwidth_value, relative_value;
    
    JButton analyze_button; 
    
    public AnalysisControls(){
        PANEL = new JPanel();
        
        PANEL.setLayout(new BoxLayout(PANEL, BoxLayout.PAGE_AXIS));
        
        Border b = StyledComponents.createStyledBorder("Analysis Controls");
        PANEL.setBorder(b);
        
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row,BoxLayout.LINE_AXIS));
        //image relative
        JLabel label = StyledComponents.createStyledLabel("Image Relative to a Speckle:");
        
        relative_value = new JTextField("0",10);
        relative_value.setHorizontalAlignment(JTextField.RIGHT);
        
        relative_value.setMaximumSize(new Dimension(200, 20));
        relative_value.setMinimumSize(new Dimension(200, 20));
        relative_value.setPreferredSize(new Dimension(200, 20));

        
        row.add(label);
        row.add(Box.createHorizontalGlue());
        row.add(relative_value);
        
        PANEL.add(row);
        
        //half width
        row = new JPanel();
        row.setLayout(new BoxLayout(row,BoxLayout.LINE_AXIS));
        //image relative
        label = StyledComponents.createStyledLabel("Half-Width of Collected Image:");
        
        halfwidth_value = new JTextField("15",10);
        halfwidth_value.setHorizontalAlignment(JTextField.RIGHT);
        
        halfwidth_value.setMaximumSize(new Dimension(200, 20));
        halfwidth_value.setMinimumSize(new Dimension(200, 20));
        halfwidth_value.setPreferredSize(new Dimension(200, 20));
        
        row.add(label);
        row.add(Box.createHorizontalGlue());
        row.add(halfwidth_value);
        
        PANEL.add(row);
        
        row = new JPanel();
        row.setLayout(new BoxLayout(row,BoxLayout.LINE_AXIS));
        //label = StyledComponents.createStyledLabel("Analyze Which Speckle:");
        String[] types_list = {"normal","appearance","disappearance"};
        SpinnerListModel speckletypes = new SpinnerListModel(types_list);
        speckle_type_spinner = new JSpinner(speckletypes);
        speckle_type_spinner.setEditor(new JSpinner.DefaultEditor(speckle_type_spinner));
        speckle_type_spinner.setToolTipText("Type of speckle to be selected for analysis.");
        speckle_type_spinner.setPreferredSize(new Dimension(200,30));
        speckle_type_spinner.setMaximumSize(new Dimension(200,30));
        speckle_type_spinner.setMinimumSize(new Dimension(200,30));
        
        //row.add(label);
        row.add(Box.createHorizontalGlue());
        row.add(speckle_type_spinner);
        
        
        PANEL.add(row);
        
        analyze_button = StyledComponents.createStyledButton("Capture Images");
        analyze_button.setActionCommand(SpeckleCommands.createdistribution.name());
        
        PANEL.add(Box.createVerticalStrut(5));
        PANEL.add(analyze_button);
        
        
        PANEL.setPreferredSize(new Dimension(600,150));
        PANEL.setMaximumSize(new Dimension(600,150));
        PANEL.setMinimumSize(new Dimension(600,150));

    }
        
    
    public void addActionListener(ActionListener al){
        
        analyze_button.addActionListener(al);
            
    }
    
    public int getRelative(){
    
        int ret_value = Integer.parseInt(relative_value.getText());
        
        return ret_value;
    
    }
    
    public int getSquareSize(){
        int ret_value = Integer.parseInt(halfwidth_value.getText());
        
        return ret_value;
    }
    
    JComponent getComponent(){
   
        return PANEL;
    
   }
   
   public int getSpeckleType(){
        String t = (String)speckle_type_spinner.getValue();
        return TYPE_MAP.get(t);
   }
   
   static HashMap<String, Integer> createTypeMap(){
        HashMap<String, Integer> type_map = new HashMap<String,Integer>();
        type_map.put( "normal", Speckle.NORMAL_SPECKLE );
        type_map.put( "appearance", Speckle.APPEARANCE_SPECKLE );
        type_map.put("disappearance", Speckle.DISAPPEARANCE_SPECKLE );
        return type_map;
   }
    public void setEnabled(boolean v){
        analyze_button.setEnabled(v);
    }

}
