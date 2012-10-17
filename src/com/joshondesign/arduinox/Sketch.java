package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Device;
import com.joshondesign.arduino.common.Util;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Sketch {
    private String name;
    private List<SketchBuffer> buffers;
    private final File dir;
    private SerialPort currentPort = null;
    private Properties props;
    private Device currentDevice;
    private Config config;
    private boolean autoScroll = false;
    private static final String AUTO_SCROLL = "AUTO_SCROLL";

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
        if(props.containsKey("CONFIG")) {
            String configName = props.getProperty("CONFIG");
            config = Global.getGlobal().getConfigForName(configName);
        }
        autoScroll = Boolean.parseBoolean(props.getProperty(AUTO_SCROLL, "false"));
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
    
    void setCurrentDevice(Device device) {
        this.currentDevice = device;
    }
    
    public Device getCurrentDevice() {
        return this.currentDevice;
    }

    void setCurrentConfig(Config config) {
        this.config = config;
        setCurrentDevice(config.getDevice());
        if(this.config != null) {
            this.props.setProperty("CONFIG", config.getName());
        }
        try {
            saveSettings();
        } catch (IOException ex) {
            Logger.getLogger(Sketch.class.getName()).log(Level.SEVERE, null, ex);            
        }
    }
    
    Config getCurrentConfig() {
        return this.config;
    }

    void setAutoScroll(boolean selected) {
        this.autoScroll = selected;
        this.props.setProperty(AUTO_SCROLL, Boolean.toString(this.autoScroll));
        try {
            saveSettings();
        } catch (IOException ex) {
            Logger.getLogger(Sketch.class.getName()).log(Level.SEVERE, null, ex);            
        }
    }

    boolean isAutoScroll() {
        return this.autoScroll;
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
