package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.CompileTask;
import com.joshondesign.arduino.common.OutputListener;
import com.joshondesign.arduino.common.Util;
import com.joshondesign.arduinox.Sketch.SketchBuffer;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author josh
 */
public class Actions  {
    private List<LogListener> logListeners = new ArrayList<>();
    final Sketch sketch;
    
    private float fontSize = 12f;
    public ColorTheme STANDARD_THEME;
    private ColorTheme DARK_THEME;
    private ColorTheme LIGHT_THEME;
    private ColorTheme theme;

    
    
    public PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    Action saveAction = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveBuffers();
        }
    };
    
    Action checkAction = new AbstractAction("Check") {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveBuffers();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CompileTask task = new CompileTask();
                        task.setSketchDir(sketch.getDirectory());
                        task.setUserLibrariesDir(new File("/Users/josh/Documents/Arduino/Libraries"));
                        task.setArduinoRoot(new File(Global.getGlobal().getArduinoDir(),"Contents/Resources/Java"));
                        task.setDevice(sketch.getCurrentDevice());
                        task.setOutputListener(new CompilerOutput());
                        task.assemble();
                    } catch (Exception ex) {
                        log(ex);
                        Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }).start();
        }
    };

    ColorTheme getCurrentTheme() {
        return this.theme;
    }
    class CompilerOutput implements OutputListener {

        public CompilerOutput() {
        }

        @Override
        public void log(String string) {
            Actions.this.log("==== " + string);
        }

        @Override
        public void stdout(String string) {
            Actions.this.log(string);
        }

        @Override
        public void stderr(String string) {
            Actions.this.log("ERROR:" + string);
        }

        @Override
        public void exec(String string) {
            Actions.this.log(string);
        }
    }
    
    Action runAction = new AbstractAction("Run") {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveBuffers();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Compiling");
                        sketch.getCurrentPort().lock();
                        CompileTask task = new CompileTask();
                        task.setSketchDir(sketch.getDirectory());
                        task.setUserLibrariesDir(new File("/Users/josh/Documents/Arduino/Libraries"));
                        task.setArduinoRoot(new File("/Users/josh/projects/Arduino.app/Contents/Resources/Java"));
                        task.setUploadPortPath(sketch.getCurrentPort().portName);
                        task.setDevice(sketch.getCurrentDevice());
                        task.setOutputListener(new CompilerOutput());
                        task.assemble();
                        log("downloading to the device");
                        task.download();
                        log("finished downloading");
                        sketch.getCurrentPort().unlock();
                    } catch (Exception ex) {
                        log("error during compliation");
                        log(ex);
                        Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();
        }
    };
    
    Action quitAction = new AbstractAction("Quit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log("Quitting");
            boolean dirty = false;
            for(Sketch sketch : Global.getGlobal().getSketches()) {
                for(SketchBuffer buffer: sketch.getBuffers()) {
                    if(buffer.isDirty()) dirty = true;
                }
                EditorWindow window = Global.getGlobal().getWindowForSketch(sketch);
                window.shutdown();
            }
            if(dirty) {
                int result = JOptionPane.showConfirmDialog(null, "There are unsaved documents. Do you wish to save them?", "Unsaved Documents", JOptionPane.YES_NO_CANCEL_OPTION);
                if(result == JOptionPane.YES_OPTION) {
                    //save them then quit
                    saveBuffers();
                    quit();
                    
                }
                if(result == JOptionPane.NO_OPTION) {
                    //don't save them, then quit
                    quit();
                }
                if(result == JOptionPane.CANCEL_OPTION) {
                    //don't quit
                    return;
                }
            } else {
                quit();
            }
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
            Util.p("Switching to the standard theme");
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
    
    final ActionListener openAction = new AbstractAction("open") {
        @Override
        public void actionPerformed(ActionEvent e) {
            FileDialog fd = new FileDialog((Frame)null);
            fd.setMode(FileDialog.LOAD);
            fd.show();
            String fileName = fd.getFile();
            if(fileName == null) return;
            String dirName = fd.getDirectory();
            if(fileName.toLowerCase().endsWith(".ino")) {
                File file = new File(dirName,fileName);
                if(file.isDirectory()) {
                    Util.p("is dir");
                }
                if(file.isFile()) {
                    Util.p("is file");
                    File dir = file.getParentFile();
                    openNewSketch(dir);
                }
            }
        }
    };
    
    final Action newAction = new AbstractAction("new") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Util.p("creating a new sketch");
                String name = JOptionPane.showInputDialog(null, "Name for your new sketch", "NewSketch1");
                        
                File sketchDir = new File("/Users/josh/Documents/Arduino/",name);
                sketchDir.mkdir();
                Sketch sketch  = new Sketch(sketchDir);
                Global.getGlobal().addSketch(sketch);
                        
                Actions actions = new Actions(sketch);
                EditorWindow frame = new EditorWindow(actions);
                Global.getGlobal().setWindowForSketch(sketch, frame);
                frame.setTitle(name);
                frame.pack();
                frame.resetPosition();
            } catch (IOException ex) {
                Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    final Action deviceInfoAction = new AbstractAction("info") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Util.p("getting device info for device: " + sketch.getCurrentDevice().name);
            JFrame frame = new JFrame("Device Info");
            frame.add(new DeviceInfoPanel(sketch.getCurrentDevice()));
            frame.pack();
            frame.setSize(400,250);
            frame.setVisible(true);
        }
    };
    

    Actions(Sketch sketch) {
        this.sketch = sketch;
        
        STANDARD_THEME = new ColorTheme();
        STANDARD_THEME.backgroundColor = Color.WHITE;
        LIGHT_THEME = new ColorTheme();
        LIGHT_THEME.backgroundColor = new Color(255,255,255);
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
    void log(final Exception ex) {
        Util.p(ex.getMessage());
        ex.printStackTrace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(LogListener ll : logListeners) {
                    ll.log(ex);
                }
            }
        });
    }
    
    void addLogListener(LogListener listener) {
        this.logListeners.add(listener);
    }
    
    private void saveBuffers() {
        for(Sketch.SketchBuffer buffer : sketch.getBuffers()) {
            if(buffer.isDirty()) {
                try {
                    //Util.p("saving: " + buffer.getFile().getAbsolutePath());
                    //Util.p("text = " + buffer.getText());
                    Util.toFile(buffer.getText(), buffer.getFile());
                } catch (IOException ex) {
                    Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
                }
                buffer.markClean();
            }
        }
        sketch.saveSettings();
    }
    
    private void quit() {
        System.exit(0);
    }
    
    public static void openNewSketch(File dir) {
        try {
            Sketch sketch = new Sketch(dir);
            Global.getGlobal().addSketch(sketch);
            Actions actions = new Actions(sketch);
            EditorWindow frame = new EditorWindow(actions);
            Global.getGlobal().setWindowForSketch(sketch,frame);
            frame.pack();
            frame.setSize(800,600);        
            frame.setVisible(true);
        } catch (IOException ex) {
            Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
