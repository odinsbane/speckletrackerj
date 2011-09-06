package speckles.controls;

import speckles.SpeckleApp;
import speckles.SpeckleCalculator;
import speckles.SpeckleParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * New imagej plugin that ...
 * User: mbs207
 * Date: Nov 19, 2010
 * Time: 4:05:46 PM
 */
public class BatchTrackingStarter extends JDialog implements ActionListener, WindowListener {
    ArrayList<Object> labels;
    ArrayList<JTextField> fields;
    boolean CANCELED=true;
    int base = 0;
    SpeckleApp app;

    public BatchTrackingStarter(SpeckleApp app, JFrame parent, Map<SpeckleParameters,Double> app_params, Map<String,Double> model_params){
        super(parent,"Adjust parameters before batch locate");
        this.app = app;
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

        JButton ok = StyledComponents.createStyledButton("Batch Locate");
        ok.setActionCommand(BatchActions.accept.name());
        JButton cancel = StyledComponents.createStyledButton("Cancel");
        cancel.setActionCommand(BatchActions.cancel.name());
        JButton learn = StyledComponents.createStyledButton("Learn");
        learn.setActionCommand(BatchActions.learn.name());

        JButton acquire = StyledComponents.createStyledButton("Acquire");
        acquire.setActionCommand(BatchActions.acquire.name());

        ok.addActionListener(this);
        cancel.addActionListener(this);
        learn.addActionListener(this);
        acquire.addActionListener(this);

        row.add(ok);
        row.add(cancel);
        row.add(learn);
        row.add(acquire);
        
        content.add(row);
        //content.add(save);

        wrapper.add(content,BorderLayout.CENTER);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.WEST);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.EAST);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.NORTH);
        wrapper.add(Box.createRigidArea(new Dimension(12,12)),BorderLayout.SOUTH);

        setContentPane(wrapper);

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("file");
        JMenuItem save = new JMenuItem("Save Parameters");
        save.setActionCommand(BatchActions.save.name());
        save.addActionListener(this);
        file.add(save);
        JMenuItem load = new JMenuItem("Load Parameters");
        load.setActionCommand(BatchActions.load.name());
        load.addActionListener(this);

        file.add(load);



        bar.add(file);
        setJMenuBar(bar);

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

    /**
     * prepares the set of label/field pairs.
     * 
     * @param map - the parameters
     * @param content - where it goes
     */
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
        BatchActions action = BatchActions.valueOf(e.getActionCommand());

        switch(action){
            case save:
                saveConstants();
                break;
            case accept:
                CANCELED=false;
                noters();
                break;
            case learn:
                initiateLearn();
                break;
            case load:
                loadConstants();
                break;
            case cancel:
                noters();
                break;
            case acquire:
                initiateAcquire();
                break;
            default:
                break;

        }

    }

    /**
     * This method will lock up the ui and calculate new values based on existing
     * speckles.
     *
     *  
     */
    private void initiateLearn(){
        this.setEnabled(false);
        SpeckleCalculator.INNER_RADIUS = Double.parseDouble(fields.get(labels.indexOf(SpeckleParameters.innerRadius)).getText());
        SpeckleCalculator.OUTER_RADIUS = Double.parseDouble(fields.get(labels.indexOf(SpeckleParameters.outerRadius)).getText());
        app.calculateSpeckleProperties(this);

    }

    private void initiateAcquire(){
        this.setEnabled(false);
        app.startAcquireParameters(this);
    }

    public void cancelAcquire(){
        this.setEnabled(true);
    }

    public void finishAcquire(double thresh, double size, double proximity){
        final int v = labels.indexOf(SpeckleParameters.thresholdValue);
        final int w = labels.indexOf(SpeckleParameters.sizeValue);
        final int x = labels.indexOf(SpeckleParameters.proximityValue);

        final double t = thresh;
        final double s = size;
        final double d = proximity;

        EventQueue.invokeLater(new Runnable(){
            public void run(){
                fields.get(v).setText(Double.toString(t));
                fields.get(w).setText(Double.toString(s));
                fields.get(x).setText(Double.toString(d));
                setEnabled(true);
            }
        });



    }
    public void finishLearning(SpeckleCalculator sc){
        final int v = labels.indexOf(SpeckleParameters.thresholdValue);
        final double thresh = sc.thresh;

        final int m = labels.indexOf(SpeckleParameters.maxMeanDisplacement);
        final double max_mean = sc.max_mean_displacement;
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                fields.get(v).setText(Double.toString(thresh));
                fields.get(m).setText(Double.toString(max_mean));
                setEnabled(true);
            }
        });


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
            bw.write("#Speckle Tracker parameter file version 1.0\n");
            bw.write("#this is not xml.\n");

            bw.write("#Global Parameters\n<parameters type=\"global\">\n");
            for(int i = 0; i<fields.size(); i++){
                if(i==base) bw.write("</parameters>\n#Model parameters\n<parameters type=\"model\">\n");
                bw.write(labels.get(i).toString());
                bw.write('\t');
                bw.write(fields.get(i).getText());
                bw.write('\n');
            }
            bw.write("</parameters>");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void loadConstants(){

        FileDialog fd = new FileDialog(this,"Load Parameters",FileDialog.LOAD);
        fd.setFile("parameters.txt");
        fd.setVisible(true);
        String fname = fd.getFile();
        if(fname==null)
            return;
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        try{
            BufferedReader br = new BufferedReader(new FileReader(fullname));
            boolean model = false;
            String s;
            do{
                s = br.readLine();

                //skip comments.
                if(s==null||s.charAt(0)=='#')
                    continue;
                //tag
                if(s.charAt(0)=='<'){
                    if(s.charAt(1)=='/'){
                        //closing ignore
                    } else{
                        model=!model;
                    }
                }
                String[] pair = s.split(Pattern.quote("\t"));

                //something went wrong.
                if(pair.length!=2)
                    continue;

                String key = pair[0];
                double value = Double.parseDouble(pair[1]);

                try{
                    SpeckleParameters sp = SpeckleParameters.fromTitle(key);
                    int dex = labels.indexOf(sp);
                    if(dex>=0){
                        fields.get(dex).setText("" + value);
                    } else{
                        System.out.println(key + " is a parameter but it is not currently being used");
                    }
                }catch(NoSuchElementException e){
                    int dex = labels.indexOf(key);
                    if(dex>=0){
                        
                        fields.get(dex).setText("" + value);

                    } else{
                        System.out.println(key + " is not a global parameter and it is not being used by the current model.");
                    }


                }
            } while(s!=null);
            br.close();

            //bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public boolean wasCancelled(){
        return CANCELED;
    }
}

enum BatchActions{
    accept,
    cancel,
    save,
    learn,
    acquire,
    load;


}
