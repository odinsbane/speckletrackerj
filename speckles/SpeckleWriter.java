package speckles;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashSet;
import java.util.StringTokenizer;

public class SpeckleWriter{

    public static final String HEADING = "#speckles csv ver 1.2\n" + 
                                         "#x(double)\ty(double)\tsize(double)\tframe(int)\ttype(int)\n";
    
    public static String getOpenFileName(Frame parent){
        return getOpenFileName(parent,"Choose Speckle Data File");
    }
    public static String getOpenFileName(Frame parent,String title){
        FileDialog fd = new FileDialog(parent,title,FileDialog.LOAD);
        
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null)
            return fullname;
        else
            return null;
    }

    //Save File dialog
    public static String getSaveFileName(Frame parent, String title){
        FileDialog fd = new FileDialog(parent,"Save CSV File",FileDialog.SAVE);
        fd.setFile(title + "_speckles.csv");
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null)
            return fullname;
        else
            return null;
    }
    
    /**
       *    Creates a dialog for saving speckles.
       **/
    public static boolean writeCSVSpeckles(HashSet<Speckle> speckle_set, Frame parent,String title){
        
        String fname = getSaveFileName(parent,title);
        if(fname!=null){
            try{
                
                writeCSVSpeckles(speckle_set,fname);
                
                return true;
            } catch(Exception e) {
                JOptionPane.showMessageDialog(parent,"Could not write" + e.getMessage());    
            }
        }
        return false;
    }
    
    
    public static void writeCSVSpeckles(HashSet<Speckle> speckle_set, String fname) throws Exception{
        
       
           
        BufferedWriter bw = new BufferedWriter( new FileWriter(fname));
        bw.write(HEADING);
        for(Speckle speck: speckle_set){
            bw.write("#%start speckle%\n");
            for(Integer i: speck){
                
                if(speck.exists(i)){
                    double[] pt = speck.getCoordinates(i);
                    String line = ""            + pt[0] + "\t"
                                                + pt[1]  + "\t"
                                                + i  + "\n";
                    bw.write(line);
                }
            }
            bw.write("#%stop speckle%\n");
        
        } 
        bw.close();
   
        
    }
    
    
    public static HashSet<Speckle> readCSVSpeckles(Frame parent){
        String fname = getOpenFileName(parent);
        try{
            return readCSVSpeckles(fname);
        } catch(Exception e){
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent,"Error reading Speckles." + e.getMessage());
        }
        return null;
    }
    
    public static HashSet<Speckle> readCSVSpeckles(String fname)throws IOException {
        HashSet<Speckle> unsorted = new HashSet<Speckle>();
        if(fname != null){
                FileInputStream fis = new FileInputStream(fname);

                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                String s;
                Speckle a = null;

                try{
                    while((s=br.readLine()) != null){

                        if(s.charAt(0)=='#' && s.length()>1 && s.charAt(1)=='%'){
                            int n = s.indexOf("%",2);
                            String cmd = s.substring(2,n);
                            if(cmd.equals("start speckle")){
                                a = new Speckle();
                            } else if(cmd.equals("stop speckle")){
                                unsorted.add(a);
                                a = null;
                            }
                        }
                        if(s.charAt(0)!='#'){

                            StringTokenizer ft = new StringTokenizer(s,"\t");
                            double x,y;
                            int frame;
                            x = Double.parseDouble(ft.nextToken());
                            y = Double.parseDouble(ft.nextToken());
                            frame = Integer.parseInt(ft.nextToken());
                            a.addPoint(x,y,frame);

                        }
                    }
                } catch(RuntimeException e){
                    throw e;
                }finally{
                    br.close();
                }

            return unsorted;
        } else {
            return null;
        }
    }
}
