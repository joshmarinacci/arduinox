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
    private static final String DEVICE_KEY = "DEVICE";
    private int serialRate = 9600;
    private String SERIAL_RATE_KEY = "SERIAL_RATE";

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
        if(props.containsKey(DEVICE_KEY)) {
            String deviceName = props.getProperty(DEVICE_KEY);
            currentDevice = Global.getGlobal().getDeviceForName(deviceName);
        }
        if(props.containsKey(SERIAL_RATE_KEY)) {
            String rate = props.getProperty(SERIAL_RATE_KEY);
            serialRate = Integer.parseInt(rate);
        }
        autoScroll = Boolean.parseBoolean(props.getProperty(AUTO_SCROLL, "false"));
    }
    
    
    
    public void saveSettings() {
        try {
            File propsFile = new File(dir,"settings.properties");
            props.store(new FileWriter(propsFile), "");
            Util.p("saved settings to : " + propsFile.getAbsolutePath());
            Util.p("serial port = " + props.getProperty("SERIALPORT"));
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(Sketch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    SerialPort getCurrentPort() {
        return this.currentPort;
    }

    void setCurrentPort(SerialPort port) {
        this.currentPort = port;
        if(this.currentPort != null) {
            this.props.setProperty("SERIALPORT", currentPort.portName);
            saveSettings();
        }
    }
    
    void setCurrentDevice(Device device) {
        this.currentDevice = device;
        if(this.currentDevice != null) {
            this.props.setProperty(DEVICE_KEY, currentDevice.getName());
            saveSettings();
        }
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
        saveSettings();
    }
    
    Config getCurrentConfig() {
        return this.config;
    }

    void setAutoScroll(boolean selected) {
        this.autoScroll = selected;
        this.props.setProperty(AUTO_SCROLL, Boolean.toString(this.autoScroll));
        saveSettings();
    }

    boolean isAutoScroll() {
        return this.autoScroll;
    }

    void setSerialRate(int i) {
        serialRate = i;
        this.props.setProperty(SERIAL_RATE_KEY, ""+serialRate);
        saveSettings();
    }

    int getSerialRate() {
        return this.serialRate;
    }    

    int getIntSetting(String key, int i) {
        return Integer.parseInt(props.getProperty(key, i+""));
    }

    void setIntSetting(String key, int width) {
        props.setProperty(key, ""+width);
    }

    void setBooleanSetting(String key, boolean selected) {
        props.setProperty(key,Boolean.toString(selected));
    }

    boolean getBooleanSetting(String windowsplitsidebaropen, boolean b) {
        return Boolean.parseBoolean(props.getProperty(windowsplitsidebaropen, Boolean.toString(b)));
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
        }
    }
}
