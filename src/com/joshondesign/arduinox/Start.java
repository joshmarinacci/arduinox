/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import com.joshondesign.arduino.common.Util;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
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
        if(Util.isMacOSX()) {
            Application.getApplication().setQuitHandler(new QuitHandler() {
                @Override
                public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr) {
                    Actions.quitAction.actionPerformed(null);
                    qr.cancelQuit();
                }
            });
        }
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
                    KeyboardUtils.setup(config);
                    
                    /*
                    config.put("CaretColor","0x00ff00"); //color of the blinking cursor
                    config.put("PairMarker.Color","0x000000"); //paren matching
                    config.put("SelectionColor","0x00ffff"); //actual selection
                    config.put("TokenMarker.Color","0xFF00ff"); //highlight matching tokens
                    */
                    
                    
                    Global global = Global.getGlobal();
                    if(global.getToolchainDir() == null) {
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
        if(Global.getGlobal().getOpenSketchCount() > 0)  return;
        try {
            Global global = Global.getGlobal();
            File sketchDir = new File(global.getDocumentsDir(),"Blink");
            Sketch sketch  = new Sketch(sketchDir);
            global.addSketch(sketch);

            Actions actions = new Actions(sketch);
            EditorWindow frame = new EditorWindow(actions);
            global.setWindowForSketch(sketch, frame);
            frame.pack();
            frame.resetPosition();
        } catch (Throwable thr) {
            thr.printStackTrace();
        }

    }
}
