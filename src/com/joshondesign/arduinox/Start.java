/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicEditorPaneUI;
import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.util.Configuration;

/**
 *
 * @author josh
 */
public class Start {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                            if ("Nimbus".equals(info.getName())) {
                                UIManager.setLookAndFeel(info.getClassName());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // If Nimbus is not available, you can set the GUI to another look and feel.
                    }
                    
                    DefaultSyntaxKit.initKit();
                    Configuration config = DefaultSyntaxKit.getConfig(DefaultSyntaxKit.class);
                    /*
                    //line number gutter
                    config.put("LineNumbers.Foreground", "0xff0000");
                    config.put("LineNumbers.Background", "0xffff00");
                    config.put("LineNumbers.CurrentBack","0xff00ff");
                    
                    config.put("CaretColor","0x00ff00"); //color of the blinking cursor
                    config.put("PairMarker.Color","0x000000"); //paren matching
                    config.put("SelectionColor","0x00ffff"); //actual selection
                    config.put("TokenMarker.Color","0xFF00ff"); //highlight matching tokens
                    */
                    
                    File sketchDir = new File("/Users/josh/Documents/Arduino/Blink");
                    Sketch sketch  = new Sketch(sketchDir);
                    
                    Actions actions = new Actions(sketch);
                    EditorWindow frame = new EditorWindow(actions);
                    /*
                    frame.setContentPane(new EditorPane(actions));
                    */
                    frame.pack();
                    frame.setSize(800,600);
                    frame.setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
