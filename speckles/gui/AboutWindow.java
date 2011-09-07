package speckles.gui;

import speckles.SpeckleApp;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.net.URI;

/**
 * Utility class for showing the version information and link to website.
 * User: mbs207
 * Date: 9/7/11
 * Time: 11:24 AM
 */
public class AboutWindow implements HyperlinkListener {

    static String[] about = new String[]{
        "<html><head><title>About: Speckle TrackerJ</title></head>\n",
        "<body>version: ", SpeckleApp.VERSION,"<br> built:",SpeckleApp.DATE,"<br>\n",
        "<a href=\"http://athena.physics.lehigh.edu/speckletrackerj\">http://athena.physics.lehigh.edu/speckletrackerj</a>\n",
        "</body></html>"
    };

    public static void showAbout(){

        final JFrame shower = new JFrame("Speckle TrackerJ: About");
        StringBuffer sb = new StringBuffer();
        for(String s: about) sb.append(s);
        JEditorPane helper = new JEditorPane("text/html",sb.toString());

        shower.setSize(400,400);
        helper.setEditable(false);

        helper.addHyperlinkListener(new AboutWindow());
        shower.add(helper);

        shower.setVisible(true);




    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
         if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                 System.out.println(e.getURL());

                if (Desktop.isDesktopSupported()) {
                      Desktop desktop = Desktop.getDesktop();
                      try{
                          desktop.browse(new URI(e.getURL().toExternalForm()));
                      } catch(Exception ex){
                          //this is my own mistake
                      }
                }
            }
    }
}
