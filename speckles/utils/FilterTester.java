package speckles.utils;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import speckles.Speckle;
import speckles.SpeckleWriter;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.HashSet;

/**
 * for checking the filter and finding out why the constant velocity/diffusing
 * version do not properly determine where the speckle should be.
 *
 * 
 */
public class FilterTester {
    NormalizedCrossCorrelationFilter ncc;
    ImageProcessor improc;
    static Color C[] = new Color[]{Color.YELLOW, Color.RED, Color.BLUE};

    FilterTester(ImagePlus imp, HashSet<Speckle> specks){
        improc = new FloatProcessor(imp.getWidth(), imp.getHeight());
        ncc = new NormalizedCrossCorrelationFilter(imp);
        ncc.createTemplate(specks);


    }
    public static void main(String[] args){
        new ImageJ();
        Frame a = new Frame();
        ImagePlus x=null;
        String fname = SpeckleWriter.getOpenFileName(a,"Select Image File to open");
        try{
            x = new ImagePlus(fname);


        } catch(Exception e) {
            System.out.println("File did not load properly");
            e.printStackTrace();
            System.exit(0);
        }

        final HashSet<Speckle> specks = SpeckleWriter.readCSVSpeckles(x.getWindow());
        //final HashSet<Speckle> copy = new HashSet<Speckle>(specks);
        ImageStack istack = x.getStack();
        ImageStack nstack = new ImageStack(istack.getWidth(), istack.getHeight());
        int N = istack.getSize();

        FilterTester ft = new FilterTester(x,specks);
        for(int i = N; i>0; i--){
            ImageProcessor ip = istack.getProcessor(i);

            nstack.addSlice("cross correlized", ft.filterSlice(ip), 0);
        }

        bestSpeckles(specks,istack,nstack);

        ImageCanvas ucan = new ImageCanvas(x){

                public void paint(Graphics g){
                    super.paint(g);
                    int f = this.imp.getCurrentSlice();
                    int dex = 0;
                    for(Speckle s: specks){
                        g.setColor(C[dex]);
                        if(s.exists(f)){
                            double p[] = s.getCoordinates(f);
                            int x = screenXD(p[0]);
                            int y = screenYD(p[1]);
                            g.drawOval(x-5-dex,y-5-dex,10+2*dex,10+2*dex);

                        }
                        dex = dex<2?dex+1:0;
                    }
                }
        };

            ImageWindow idub = new StackWindow(x, ucan){
                public void windowClosing(WindowEvent e){
                    System.exit(0);
                }
            };






        ImagePlus nimp = new ImagePlus("fills",nstack);
        ImageCanvas ican = new ImageCanvas(nimp){
                public void paint(Graphics g){
                    super.paint(g);
                    int dex = 0;
                    int f = this.imp.getCurrentSlice();
                    for(Speckle s: specks){
                        g.setColor(C[dex]);

                        if(s.exists(f)){
                            double p[] = s.getCoordinates(f);
                            int x = screenXD(p[0]);
                            int y = screenYD(p[1]);
                            g.drawOval(x-5 - dex,y-5 - dex,10+2*dex,10+2*dex);

                        }
                        dex = dex<2?dex+1:0;                        
                    }
                }
            };

        new StackWindow(nimp,ican);

    }
    static public void bestSpeckles(HashSet<Speckle> specks, ImageStack old, ImageStack now){
        Speckle s = specks.iterator().next();
        Speckle o = new Speckle();
        Speckle n = new Speckle();
        specks.add(o);
        specks.add(n);
        for(int f: s){

            double[] pt = s.getCoordinates(f);
            double[] r = getMaxPt(pt,old.getProcessor(f));
            o.addPoint(r[0],r[1],f);
            r = getMaxPt(pt,now.getProcessor(f));
            n.addPoint(r[0],r[1],f);





        }


    }

    static public double[] getMaxPt(double[] xy, ImageProcessor ip){
        double max = -Double.MAX_VALUE;
        double[] ret = new double[]{0,0};
        for(int i = -2; i<=2; i++){
            for(int j = -2; j<=2; j++){

                double v = ip.getInterpolatedValue(xy[0] + i, xy[1] + j);
                if(v>max){

                    ret = new double[]{xy[0] + i, xy[1] + j};
                    max = v;

                }




            }

        }

        return ret;
    }
    public ImageProcessor filterSlice(ImageProcessor ip){

        ImageProcessor ix = improc.duplicate();
        for(int i = 0; i<ip.getWidth(); i ++)
            for(int j = 0; j<ip.getHeight(); j++){
                {

                    ix.putPixelValue(i,j,ncc.filter(ip,i,j));

                }
        }
        return ix;
    }


}

