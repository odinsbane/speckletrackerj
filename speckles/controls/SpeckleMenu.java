package speckles.controls;

import speckles.models.SpeckleModel;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Enumeration;


/**
   *   
   **/
   
class SpeckleMenu{

    JMenuBar menu;
    
    JMenu file,image,macro, models;
    
    ArrayList<JMenuItem> items;
    
    ButtonGroup model_group;
    
    public SpeckleMenu(){
        items = new ArrayList<JMenuItem>();
        
        menu = new JMenuBar();
        file = new JMenu("file");
        file.setMnemonic(KeyEvent.VK_F);
        
        JMenuItem load_speckles = new JMenuItem("Load Speckles");
        load_speckles.setActionCommand("loadspeckles");
        file.add(load_speckles);
        
        items.add(load_speckles);
        
        JMenuItem save_speckles = new JMenuItem("Save Speckles");
        save_speckles.setActionCommand("savespeckles");
        file.add(save_speckles);

        items.add(save_speckles);

        JMenuItem append_speckles = new JMenuItem("Append Speckle File");
        append_speckles.setActionCommand("appendspeckles");
        file.add(append_speckles);
        items.add(append_speckles);
        
        JMenuItem new_speckles = new JMenuItem("New Speckles");
        new_speckles.setActionCommand("newspeckles");
        file.add(new_speckles);

        items.add(new_speckles);

        image = new JMenu("image");
        image.setMnemonic(KeyEvent.VK_I);
        
        JMenuItem load_image = new JMenuItem("Open Image");
        load_image.setActionCommand("loadimage");
        image.add(load_image);
        
        items.add(load_image);
        
        macro = new JMenu("macros");
        
        JMenuItem parse_stack = new JMenuItem("Batch Locate and Track");
        parse_stack.setActionCommand("batchlocate");
        macro.add(parse_stack);
        items.add(parse_stack);

        JMenuItem open_imagej = new JMenuItem("Start ImageJ");
        open_imagej.setActionCommand("startimagej");
        macro.add(open_imagej);
        items.add(open_imagej);

        JMenuItem find_broken_speckles = new JMenuItem("Find Discontinuous");
        find_broken_speckles.setActionCommand("findbroken");
        macro.add(find_broken_speckles);
        items.add(find_broken_speckles);

        JMenuItem measure_speckles = new JMenuItem("Measure Speckle Tracks");
        measure_speckles.setActionCommand("measurespeckles");
        macro.add(measure_speckles);
        items.add(measure_speckles);

        createModelMenu();
        model_group.getElements().nextElement().setSelected(true);
        
        JMenu status = new JMenu("about");
        JMenuItem version = new JMenuItem("version");
        version.setActionCommand("version");
        status.add(version);
        items.add(version);
        
        menu.add(file);
        menu.add(image);
        menu.add(macro);
        menu.add(models);
        menu.add(status);
    }
        
    
    public void addActionListener(ActionListener al){
        
        for(JMenuItem x: items)
            x.addActionListener(al);
        
            
    }
    
    private void createModelMenu(){
        models = new JMenu("models");
        JMenuItem adjust_parameters = new JMenuItem("Adjust Paramters");
        items.add(adjust_parameters);
        adjust_parameters.setActionCommand(SpeckleCommands.adjustparameters.name());
        models.add(adjust_parameters);
        models.add(new JSeparator());
        
        model_group = new ButtonGroup();


        for(SpeckleModel.Models model: SpeckleModel.Models.values()){
            addModelItem(model.name());
        }
        
    }
    
    private void addModelItem(String label){
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
        item.setActionCommand("modelchanged");
        model_group.add(item);
        models.add(item);
        items.add(item);
    }
    JMenuBar getMenuBar(){
   
        return menu;
    
   }
   
   public int getModelIndex(){
        int i = 0;
        Enumeration<AbstractButton> e = model_group.getElements();
        
        while(e.hasMoreElements()){
            AbstractButton b = e.nextElement();
            if(b.isSelected())
                return i;
            i++;
        }
        return -1;
   }

    public void setModelIndex(int i){
        Enumeration<AbstractButton> e = model_group.getElements();
        for(int j = 0; j<i;j++)
            e.nextElement();
        AbstractButton b = e.nextElement();
        b.setSelected(true);

   }
    
}
