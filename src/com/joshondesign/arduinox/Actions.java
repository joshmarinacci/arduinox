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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author josh
 */
public class Actions  {
    private List<OutputListener> logListeners = new ArrayList<>();
    final Sketch sketch;
    
    private float fontSize = 12f;
    public ColorTheme STANDARD_THEME;
    private ColorTheme DARK_THEME;
    private ColorTheme LIGHT_THEME;
    private ColorTheme theme;

    boolean compileInProcess = false;
    
    
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
            if(compileInProcess)return;
            compileInProcess = true;
            this.setEnabled(false);
            runAction.setEnabled(false);
            saveBuffers();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CompileTask task = new CompileTask();
                        task.setSketchDir(sketch.getDirectory());
                        task.setUserLibrariesDir(new File(Global.getGlobal().getDocumentsDir(),"Libraries"));
                        task.setArduinoRoot(Global.getGlobal().getToolchainDir());
                        task.setDevice(sketch.getCurrentDevice());
                        task.setOutputListener(new CompilerOutput());
                        task.assemble();
                    } catch (Exception ex) {
                        log(ex);
                        Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    setEnabled(true);
                    runAction.setEnabled(true);
                    compileInProcess = false;
                }


            }).start();
        }
    };

    ColorTheme getCurrentTheme() {
        return this.theme;
    }
    
    private CompilerOutput compilerOutput = new CompilerOutput();
    private void log(Exception ex) {
        compilerOutput.log("error!");
        compilerOutput.log(ex.getMessage());
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        compilerOutput.log(sw.toString());
    }
    private void log(String message) {
        compilerOutput.log(message);
    }
    
    class CompilerOutput implements OutputListener {

        public CompilerOutput() {
        }

        @Override
        public void log(String string) {
            for(OutputListener ol : logListeners) {
                ol.log(string);
            }
        }

        @Override
        public void stdout(String string) {
            for(OutputListener ol : logListeners) {
                ol.stdout(string);
            }
        }

        @Override
        public void stderr(String string) {
            for(OutputListener ol : logListeners) {
                ol.stderr(string);
            }
        }

        @Override
        public void exec(String string) {
            for(OutputListener ol : logListeners) {
                ol.exec(string);
            }
        }
    }
    
    
    Action runAction = new AbstractAction("Run") {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveBuffers();
            if(sketch.getCurrentPort() == null) {
                log("ERROR: no serialport selected");
                return;
            }
            if(compileInProcess)return;
            compileInProcess = true;
            this.setEnabled(false);
            checkAction.setEnabled(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Compiling");                        
                        sketch.getCurrentPort().lock();
                        CompileTask task = new CompileTask();
                        task.setSketchDir(sketch.getDirectory());
                        task.setUserLibrariesDir(new File(Global.getGlobal().getDocumentsDir(),"Libraries"));
                        task.setArduinoRoot(Global.getGlobal().getToolchainDir());
                        task.setUploadPortPath(sketch.getCurrentPort().portName);
                        task.setDevice(sketch.getCurrentDevice());
                        task.setOutputListener(compilerOutput);
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
                    setEnabled(true);
                    checkAction.setEnabled(true);
                    compileInProcess = false;
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
                if(name == null || name.trim().equals("")) return;
                        
                File sketchDir = new File(Global.getGlobal().getDocumentsDir(),name);
                sketchDir.mkdir();
                File mainfile = new File(sketchDir,sketchDir.getName()+".ino");
                if(!mainfile.exists()) {
                    String text = Util.toString(Actions.class.getResource("resources/newsketch.ino"));
                    Util.toFile(text, mainfile);
                }

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
        
        checkAction.putValue(Action.LARGE_ICON_KEY,
                new ImageIcon(Actions.class.getResource("resources/noun_project_1307.png")));
        runAction.putValue(Action.LARGE_ICON_KEY,
                new ImageIcon(Actions.class.getResource("resources/noun_project_2873.png")));
    }
    
    
    void addLogListener(OutputListener listener) {
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
        sketch.saveSettings();
        Global.getGlobal().saveSettings();
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
            frame.resetPosition();
        } catch (IOException ex) {
            Logger.getLogger(Actions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
