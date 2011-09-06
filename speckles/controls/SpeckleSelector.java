package speckles.controls;


import ij.ImagePlus;
import speckles.Speckle;
import speckles.SpeckleApp;
import speckles.gui.TextWindow;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
/**
   *   
   **/
   
public class SpeckleSelector extends MouseAdapter{
    JScrollPane PANEL;
    JPanel MAIN;
    SpeckleSelectorModel model;
    JTable table;
    SpeckleApp parent;
    ArrayList<JButton> buttons;

    public SpeckleSelector(){
        MAIN = new JPanel();
        MAIN.setLayout(new BoxLayout(MAIN,BoxLayout.PAGE_AXIS));
        model = new SpeckleSelectorModel();

        table = new JTable(model);
        table.setFillsViewportHeight(true);

        PANEL = new JScrollPane(table);
        PANEL.setFocusable(false);

        table.setFocusable(false);
        table.addMouseListener(this);

        model.listenTo(table.getTableHeader());

        PANEL.setPreferredSize(new Dimension(450,350));
        PANEL.setMaximumSize(new Dimension(450,350));
        PANEL.setMinimumSize(new Dimension(450,350));

        MAIN.add(PANEL);


        buttons = new ArrayList<JButton>();


        JButton update_selector = StyledComponents.createStyledButton("Update");
        update_selector.setActionCommand(SpeckleCommands.updateselector.name());
        buttons.add(update_selector);

        JButton table_selector = StyledComponents.createStyledButton("Show");
        table_selector.setActionCommand(SpeckleCommands.copyselector.name());
        table_selector.setToolTipText("Show the values of table in a text window.");

        buttons.add(table_selector);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row,BoxLayout.LINE_AXIS));
        row.add(update_selector);
        row.add(table_selector);

        row.setPreferredSize(new Dimension(450,50));
        row.setMaximumSize(new Dimension(450,50));
        row.setMinimumSize(new Dimension(450,50));
        
        MAIN.add(row);
    }

    public void setParent(SpeckleApp p){
        parent = p;
        model.parent = p;
    }
    public void addActionListener(ActionListener al){
        for(JButton b: buttons)
            b.addActionListener(al);
        
            
    }
    
    
    JComponent getComponent(){
   
        return MAIN;
    
   }
   
    public void setEnabled(boolean v){
        for(JButton b: buttons){
            b.setEnabled(v);
        }
    }

    public void updateSelector(HashSet<Speckle> speckles){
        model.updateRows(speckles);

        table.clearSelection();
        Speckle s = parent.getSelected();
        if(s!=null){
            int row = model.getSpeckleRow(s);
            if(row>=0){
                table.addRowSelectionInterval(row,row);
            }

        }
        table.doLayout();
        table.invalidate();
        MAIN.validate();
        MAIN.repaint(PANEL.getX(), PANEL.getY(), PANEL.getWidth(), PANEL.getHeight());

    }

    @Override
    public void mouseClicked(MouseEvent evt){

       int x = table.rowAtPoint(evt.getPoint());
       int y = table.columnAtPoint(evt.getPoint());
       if(x>=0){
           Speckle s = model.getSpeckleAt(x);

           try{
               if(y==2){
                    parent.setSlice(s.getLastFrame());
               } else{
                    parent.setSlice(s.getFirstFrame());
               }
                parent.selectSpeckle(s);
               } catch(NoSuchElementException e){

                    parent.deleteSpeckle(s);
                    parent.updateSelector();

            }
       }
    }

    public void updateAppearances(ImagePlus imp){

        model.updateAppearances(imp);

    }

    public void createTextWindowTable() {
        StringBuilder data = new StringBuilder();
        int rows = table.getRowCount();
        int columns = table.getColumnCount();

        data.append('#');
        for(int i = 0; i<columns; i++){
            data.append(table.getColumnName(i));
            data.append('\t');

        }
        data.append('\n');

        for(int i = 0; i<rows; i++){
            for(int j = 0; j<columns; j++){

                data.append(table.getValueAt(i,j));
                data.append('\t');

            }
            data.append('\n');

        }

        new TextWindow("Speckle Values",data.toString()).display();

    }
}


class SpeckleSelectorModel extends AbstractTableModel {

    //HashMap<Integer, Speckle> DATA;
    int LAST_ID;
    ArrayList<SpeckleHost> SPECKLES;
    SpeckleComparator COMPARATOR;
    SpeckleApp parent;
    String[] ColumnNames = {
                            "id",
                            "start",
                            "finish",
                            "inner change",
                            "outer change",
                            "max displacement",
                            "closest"
        };

    SpeckleSelectorModel(){
        COMPARATOR = new SpeckleComparator( this );
        COMPARATOR.ctype = 2;
        SPECKLES = new ArrayList<SpeckleHost>();
        LAST_ID=0;
    }
    public void listenTo(JTableHeader jth){

        jth.addMouseListener(COMPARATOR);
        COMPARATOR.setTableHeader(jth);

    }
    public void updateRows(HashSet<Speckle> speckles){
        //speckles to add
        HashSet<Speckle> n = new HashSet<Speckle>(speckles);
        Iterator<SpeckleHost> iter = SPECKLES.iterator();
        while(iter.hasNext()){
            SpeckleHost sh = iter.next();
            //If the speckle exists remove it from the speckles to add.
            if(speckles.contains(sh.speckle))
                n.remove(sh.speckle);
            else
                iter.remove();
        }


        for(Speckle s: n){
            SpeckleHost sh = new SpeckleHost(s,LAST_ID);
            SPECKLES.add(sh);
            
            LAST_ID++;
        }
        Collections.sort(SPECKLES, COMPARATOR);
    }
    public void sort(){
        Collections.sort(SPECKLES,COMPARATOR);
    }

    public int getRowCount(){
            return SPECKLES.size();
      }
    public int getColumnCount(){
        return 7;
    }

    /*
    public boolean getIsCellEditable(int row, int column){
        return false;
    }*/

    public Object getValueAt(int row, int column){
        SpeckleHost s = SPECKLES.get(row);
        try{
        switch(column){
            case 0:
                return row;
            case 1:
                return new Integer(s.getFirstFrame());
            case 2:
                return new Integer(s.getLastFrame());
            case 3:
                return s.getValue(0);
            case 4:
                return s.getValue(1);
            case 5:
                return s.getValue(2);
            case 6:
                return s.getValue(3);

        }
        } catch(NoSuchElementException e){

            //I should remove this speckle it means it doen't have anything.

        }
        return "NaN";

    }

    public Speckle getSpeckleAt(int i){

        return SPECKLES.get(i).speckle;

    }
    @Override
     public String getColumnName(int col) {

        return ColumnNames[col];
    }

    public int getSpeckleRow(Speckle s){

        for(int i = 0; i<SPECKLES.size(); i++){
            if(SPECKLES.get(i).speckle==s)
                return i;
        }
        return -1;
    }

    public void updateAppearances(ImagePlus imp){
        for(SpeckleHost s: SPECKLES)
            s.measureAppearanceProbabilities(imp, parent);

    }




}

class SpeckleComparator implements Comparator<SpeckleHost>, MouseListener{
    int ctype;
    SpeckleSelectorModel model;
    JTableHeader jth;
    int order;

    SpeckleComparator(SpeckleSelectorModel m){
        ctype=0;
        model = m;
        order = 1;
    }
    public int compare(SpeckleHost t, SpeckleHost o){
        int delta = 0;
        try{
            switch(ctype){
                case 1:
                    delta += t.getFirstFrame()-o.getFirstFrame();
                    break;
                case 2:
                    delta+= t.getLastFrame() - o.getLastFrame();
                    break;
                case 3:
                    delta = compareArrayValues(t,o,0);
                    break;
                case 4:
                    delta = compareArrayValues(t,o,1);
                    break;
                case 5:
                    delta = compareArrayValues(t,o,2);
                    break;
                case 6:
                    delta = compareArrayValues(t,o,3);
                    break;
                default:
                    break;
                
            }
        } catch(Exception e){
            //let it be
        }
        if(delta==0) delta=t.id-o.id;
        return order*(delta);
    
    }

    /**
     * Compares the array values of the speckle host, returns 0 if they're equal.
     * @param t one the objects
     * @param o the other
     * @param dex index of comparison
     * @return positive if t is greater than o.
     */
    public int compareArrayValues(SpeckleHost t, SpeckleHost o, int dex){
        int delta = 0;
        double diff = t.getValue(dex) - o.getValue(dex);

        if(diff!=0)
                delta= diff>0?1:-1;

        return delta;
    }


    public void setTableHeader(JTableHeader jth){

        this.jth = jth;

    }
    public void mouseExited(MouseEvent evt){

    }
    public void mousePressed(MouseEvent evt){

    }
    public void mouseReleased(MouseEvent evt){

    }
    public void mouseClicked(MouseEvent evt){

        int ntype = jth.columnAtPoint(evt.getPoint());
        if(ntype==ctype){
            order*=-1;
        }else{
            ctype = ntype;
            order=1;
        }
        model.sort();
    }

    public void mouseEntered(MouseEvent evt){

    }

}