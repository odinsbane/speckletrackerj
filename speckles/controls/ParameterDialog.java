package speckles.controls;

import speckles.SpeckleParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * 
 * Create String/Double pairs of values for setting parameters.
 *
 *
 * User: mbs207
 * Date: Sep 27, 2010
 * Time: 5:11:33 PM
 */
public class ParameterDialog extends JDialog implements ActionListener, WindowListener {
    ArrayList<Object> labels;
    ArrayList<JTextField> fields;
    boolean CANCELED=true;
    int base = 0;
    public ParameterDialog(JFrame parent,Map<SpeckleParameters,Double> app_params, Map<String,Double> model_params){
        super(parent,"Adjust parameters");
        fields = new ArrayList<JTextField>();
        labels = new ArrayList<Object>();

        BorderLayout b = new BorderLayout();
        JPanel wrapper = new JPanel();
        wrapper.setLayout(b);

        JPanel content = new JPanel();


        content.add(Box.createVerticalGlue());
        BoxLayout column = new BoxLayout(content,BoxLayout.PAGE_AXIS);
        content.setLayout(column);

        content.add(new JLabel("Global Parameters"));
        content.add(separate());
        addParameterSet(app_params,content);
        base = app_params.size();
        content.add(Box.createHorizontalGlue());

        if(model_params!=null){
            content.add(new JLabel("Model Parameters"));
            content.add(new JSeparator());
            addParameterSet(model_params,content);
        } else{
            content.add(new JLabel("Model does not have adjustable parameters."));
            content.add(new JSeparator());                        
        }
        content.add(Box.createVerticalGlue());
        JPanel row = new JPanel();
        BoxLayout lay = new BoxLayout(row,BoxLayout.LINE_AXIS);
        row.setLayout(lay);
        
        JButton ok = StyledComponents.createStyledButton("Accept");
        JButton cancel = StyledComponents.createStyledButton("Cancel");
        JButton save = StyledComponents.createStyledButton("Save");

        ok.addActionListener(this);
        cancel.addActionListener(this);
        save.addActionListener(this);

        row.add(ok);
        row.add(cancel);

        content.add(row);
        content.add(save);

        wrapper.add(content,BorderLayout.CENTER);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.WEST);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.EAST);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.NORTH);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.SOUTH);

        setContentPane(wrapper);


        

    }

    synchronized public void requestConstants(){
        pack();
        setModal(true);
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        addWindowListener(this);
        EventQueue.invokeLater(
            new Runnable(){
                public void run(){
                    setVisible(true);
                }
            }
        );
        try{
            wait();
        } catch(Exception e){
            
        } finally{
            EventQueue.invokeLater(
                new Runnable(){
                    public void run(){
                        setVisible(false);
                    }
                }
            );
        }
    }
    public void updateConstants(Map<SpeckleParameters,Double> app, Map<String,Double> model){
        ArrayList<Double> values = new ArrayList<Double>();
        if(CANCELED)
            return;
        try{
            for(JTextField tf: fields){
                values.add(new Double(tf.getText()));
            }

        } catch(Exception e){
            JOptionPane.showMessageDialog(getParent(),"Not all of the values were acceptable");
            return;
        }

        int n = app.size();
        for(int i = 0; i<n; i++)
            app.put((SpeckleParameters)labels.get(i),values.get(i));


        for(int i = n; i<values.size(); i++)
            model.put((String)labels.get(i),values.get(i));






    }
    private void addParameterSet(Map<? extends Object,Double> map, JPanel content){
        JPanel row;
        BoxLayout lay;


        for(Object s: map.keySet()){
            row = new JPanel();
            lay = new BoxLayout(row,BoxLayout.LINE_AXIS);
            row.setLayout(lay);
            JLabel l = StyledComponents.createStyledLabel(s.toString());
            JTextField tf = new JTextField();
            tf.setText(" " + map.get(s));
            tf.setPreferredSize(new Dimension(200,20));
            tf.setMaximumSize(new Dimension(200,20));

            row.add(l);
            row.add(Box.createHorizontalGlue());
            row.add(tf);

            content.add(row);

            fields.add(tf);
            labels.add(s);

        }


    }
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Save")){
            saveConstants();
            return;
        }
        if(e.getActionCommand().equals("Accept")) CANCELED=false;
        noters();
    }

    public JSeparator separate(){
        JSeparator s = new JSeparator();
        s.setMaximumSize(new Dimension(600,5));
        return s;
    }
    private synchronized void noters(){
        notify();
    }
    public void windowOpened(WindowEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void windowClosing(WindowEvent e) {
        noters();
    }

    public void windowClosed(WindowEvent e) {
        noters();
    }

    public void windowIconified(WindowEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void windowDeiconified(WindowEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void windowActivated(WindowEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void windowDeactivated(WindowEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void saveConstants(){

        FileDialog fd = new FileDialog(this,"Save Parameters",FileDialog.SAVE);
        fd.setFile("parameters.txt");
        fd.setVisible(true);
        String fname = fd.getFile();
        if(fname==null)
            return;
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(fullname));


            bw.write("#Global Parameters\n");
            for(int i = 0; i<fields.size(); i++){
                if(i==base) bw.write("#Model Parameters\n");
                bw.write(labels.get(i).toString());
                bw.write('\t');
                bw.write(fields.get(i).getText());
                bw.write('\n');
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
