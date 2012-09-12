/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.CompileTask;
import com.joshondesign.arduino.common.Util;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

/**
 *
 * @author josh
 */
public class Actions  {
    
    private float fontSize = 12f;
    private ColorTheme STANDARD_THEME;
    private ColorTheme DARK_THEME;
    private ColorTheme LIGHT_THEME;
    private ColorTheme theme;

    
    
    public PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    Action saveAction = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log("the other save");
        }
    };
    
    Action checkAction = new AbstractAction("Check") {
        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("the other check");
                        CompileTask task = new CompileTask();
                        task.setSketchDir(new File("/Users/Josh/Documents/Arduino/Blink"));
                        task.assemble();
                        log("fully assembled");
                    } catch (IOException ex) {
                        Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();
        }
    };
    
    Action runAction = new AbstractAction("Run") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log("the other run");
        }
    };
    
    Action quitAction = new AbstractAction("Quit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log("Quitting");
            System.exit(0);
        }
    };
    
    
    Action zoomInAction = new AbstractAction("Zoom In") {
        @Override
        public void actionPerformed(ActionEvent e) {
            float old = fontSize;
            fontSize+=2f;
            pcs.firePropertyChange("fontsize", old,fontSize);
        }
    };
    
    Action zoomOutAction = new AbstractAction("Zoom Out") {
        @Override
        public void actionPerformed(ActionEvent e) {
            float old = fontSize;
            fontSize-=2f;
            if(fontSize < 8) fontSize = 8;
            pcs.firePropertyChange("fontsize", old,fontSize);
        }
    };
    
    Action switchStandardTheme = new AbstractAction("standardtheme") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ColorTheme old = theme;
            theme = STANDARD_THEME;
            pcs.firePropertyChange("theme", old, theme);
        }
    };
    
    Action switchLightTheme = new AbstractAction("lighttheme") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ColorTheme old = theme;
            theme = LIGHT_THEME;
            pcs.firePropertyChange("theme", old, theme);
        }
    };
    
    Action switchDarkTheme = new AbstractAction("darktheme") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ColorTheme old = theme;
            theme = DARK_THEME;
            pcs.firePropertyChange("theme", old, theme);
        }
    };
    
    private List<LogListener> logListeners = new ArrayList<>();
    final Sketch sketch;

    Actions(Sketch sketch) {
        this.sketch = sketch;
        
        STANDARD_THEME = new ColorTheme();
        STANDARD_THEME.backgroundColor = Color.WHITE;
        LIGHT_THEME = new ColorTheme();
        LIGHT_THEME.backgroundColor = new Color(240,240,240);
        DARK_THEME = new ColorTheme();
        DARK_THEME.backgroundColor = Color.DARK_GRAY;
        theme = STANDARD_THEME;
    }
    
    void log(final String str) {
        Util.p(str);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(LogListener ll : logListeners) {
                    ll.log(str);
                }
            }
        });
    }
    
    void addLogListener(LogListener listener) {
        this.logListeners.add(listener);
    }

    public static interface LogListener {
        public void log(String str);
    }
    
}
