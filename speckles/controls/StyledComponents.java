package speckles.controls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class StyledComponents{
    static JButton createStyledButton(String title){
        
        JButton nb = new JButton(title);
        
        nb.setForeground(Color.BLUE);
        
        
        nb.setMinimumSize(new Dimension(100,25));
        nb.setMaximumSize(new Dimension(100,25));
        nb.setPreferredSize(new Dimension(100,25));
                
        nb.setFont(new Font("San Serif",Font.PLAIN, 14));

        
        return nb;
        
    }
    
    static JLabel createStyledLabel(String label){
    
        JLabel nl = new JLabel(label);
        
        
        nl.setFont(new Font("Arial",Font.PLAIN, 14));
        return nl;
    
    }
    
    static Border createStyledBorder(String title){
        Border b = BorderFactory.createEtchedBorder();
        TitledBorder titleborder = BorderFactory.createTitledBorder(b,title, TitledBorder.LEFT, TitledBorder.TOP);
        titleborder.setTitleFont(new Font("Ariel",Font.BOLD, 12));
        
        return titleborder;
    }
    
    static JTabbedPane createStyledTabbedPane(){
        JTabbedPane x = new JTabbedPane();
        
        x.setPreferredSize(new Dimension(300,175));
        x.setMaximumSize(new Dimension(300,175));
        x.setMinimumSize(new Dimension(300,175));
        return x;
    }
    
    

}