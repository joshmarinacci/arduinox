package com.joshondesign.arduinox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.joshondesign.arduino.common.Util;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Sketch {
    private String name;
    private List<SketchBuffer> buffers;
    private final File dir;
    private SerialPort currentPort = null;
    private Properties props;

    Sketch(File sketchDir) throws IOException {
        this.dir = sketchDir;
        this.name = sketchDir.getName();
        this.buffers = new ArrayList<>();
        
        buffers.add(new SketchBuffer(new File(sketchDir,sketchDir.getName()+".ino")));
        loadSettings();
    }
    
    List<SketchBuffer> getBuffers() {
        return this.buffers;
    }

    String getName() {
        return this.name;
    }

    File getDirectory() {
        return this.dir;
    }
    
    private void loadSettings() throws IOException {
        props = new Properties();
        File propsFile = new File(dir,"settings.properties");
        if(propsFile.exists()) {
            props.load(new FileReader(propsFile));
        }
        if(props.containsKey("SERIALPORT")) {
            String portName = props.getProperty("SERIALPORT");
            currentPort = Global.getGlobal().getPortForPath(portName);
        }
        
        Util.p("the current serial port = " + currentPort);
        if(currentPort != null) {
            Util.p("current port name = " + currentPort.portName);
        }
    }
    
    
    
    public void saveSettings() throws IOException {
        File propsFile = new File(dir,"settings.properties");
        props.store(new FileWriter(propsFile), "");
        Util.p("saved settings to : " + propsFile.getAbsolutePath());
        Util.p("serial port = " + props.getProperty("SERIALPORT"));
    }

    SerialPort getCurrentPort() {
        return this.currentPort;
    }

    void setCurrentPort(SerialPort port) {
        this.currentPort = port;
        if(this.currentPort != null) {
            this.props.setProperty("SERIALPORT", currentPort.portName);
            try {
                saveSettings();
            } catch (IOException ex) {
                Logger.getLogger(Sketch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
    
    
    public static class SketchBuffer {
        private final File file;
        private String code;
        private boolean dirty = false;
        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        
        private SketchBuffer(File file) throws IOException {
            this.file = file;
            try {
                this.code = Util.toString(file);
            } catch (Exception ex) {
                ex.printStackTrace();
                this.code = "";
            }
        }
        
        public String getName() {
            return this.file.getName();
        }

        public File getFile() {
            return this.file;
        }

        boolean isDirty() {
            return dirty;
        }

        String getText() {
            return code;
        }

        void markDirty() {
            boolean old = this.dirty;
            this.dirty = true;
            pcs.firePropertyChange("dirty", old, dirty);
        }

        void markClean() {
            boolean old = this.dirty;
            this.dirty = false;
            pcs.firePropertyChange("dirty", old, dirty);
        }

        void addPropertyChangeListener(String name, PropertyChangeListener listener) {
            pcs.addPropertyChangeListener(name, listener);
        }

        void setText(String text) {
            this.code = text;
            Util.p("code updated to: " + text);
        }
        
    }
}
