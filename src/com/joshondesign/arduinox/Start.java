/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicEditorPaneUI;
import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.actions.ActionUtils;
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
                    config.put("CaretColor","0x00ff00"); //color of the blinking cursor
                    config.put("PairMarker.Color","0x000000"); //paren matching
                    config.put("SelectionColor","0x00ffff"); //actual selection
                    config.put("TokenMarker.Color","0xFF00ff"); //highlight matching tokens
                    */
                    
                    
                    Global global = Global.getGlobal();
                    if(global.getArduinoDir() == null) {
                        JDialog dialog = new JDialog();
                        JPanel panel = new JPanel();
                        SelectIDEDialog p = new SelectIDEDialog();
                        panel.add(p);
                        dialog.add(panel);
                        dialog.pack();
                        dialog.setModal(true);
                        dialog.setVisible(true);
                    } else {
                        openLastSketch();
                    }
                    
            }

        });
    }
    public static void openLastSketch() {
        try {
            Global global = Global.getGlobal();
            File sketchDir = new File("/Users/josh/Documents/Arduino/Blink");
            Sketch sketch  = new Sketch(sketchDir);
            global.addSketch(sketch);

            Actions actions = new Actions(sketch);
            EditorWindow frame = new EditorWindow(actions);
            global.setWindowForSketch(sketch, frame);
            frame.pack();
            frame.resetPosition();
        } catch (IOException ex) {
            Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
