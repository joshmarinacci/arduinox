/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Device;
import com.joshondesign.arduino.common.Util;
import gnu.io.CommPortIdentifier;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author josh
 */
public class Global {
    private static Global _global;
    private List<Sketch> sketches = new ArrayList<>();
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private Map<Sketch,EditorWindow> windows = new HashMap<>();
    private final List<SerialPort> ports;
    private final List<Device> devices;
    private final List<Config> configs;
    public Config editConfigStub;
    private Config defaultConfig;
    private Device extracore;
    private File arduinoDir;
    private String ARDUINO_IDE_PATH = "ARDUINO_IDE_PATH";
    private String RECENT_SKETCHES = "RECENT_SKETCHES";
    
    public static final int SERIAL_RATE_INTS[] = {
        300,1200,2400,4800,9600,14400,
        19200,28800,38400,57600,115200
    };
    
    public static final String SERIAL_RATE_STRINGS[] = {
        "300","1200","2400","4800","9600","14400",
        "19200","28800","38400","57600","115200"
    };
    private List<String> recentSketches;
    private Set<String> recentUniqueSketches;


    private Global() {
        this.ports = scanForSerialPorts();
        this.devices = scanForDevices();
        this.configs = loadConfigs();
        loadSettings();
    }
    
    

    static Global getGlobal() {
        if(_global == null) {
            _global = new Global();
        }
        return _global;
    }

    void addSketch(Sketch sketch) {
        this.sketches.add(sketch);
        this.recentSketches.add(sketch.getDirectory().getAbsolutePath());
        saveSettings();
        pcs.firePropertyChange("sketches", sketches, sketch);
    }

    Iterable<Sketch> getSketches() {
        return sketches;
    }

    EditorWindow getWindowForSketch(Sketch sketch) {
        return windows.get(sketch);
    }

    void setWindowForSketch(Sketch sketch, EditorWindow frame) {
        this.windows.put(sketch,frame);
    }

    void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(property,listener);
    }
    
    List<SerialPort> scanForSerialPorts() {
        
        List<SerialPort> ports = new ArrayList<>();
        
        //get all ports
        for (Enumeration enumeration = CommPortIdentifier.getPortIdentifiers(); enumeration.hasMoreElements();) {
            CommPortIdentifier port = (CommPortIdentifier) enumeration.nextElement();
            SerialPort pt = new SerialPort();
            pt.portName = port.getName();
            ports.add(pt);
        }
        
        //filter out dupes
        if(Util.isMacOSX()) {
            Map<String,SerialPort> map = new HashMap<>();
            for(SerialPort port : ports) {
                String shortName = port.portName;
                if(port.portName.startsWith("/dev/tty")) {
                    shortName = port.portName.replaceFirst("/dev/tty.", "");
                }
                if(port.portName.startsWith("/dev/cu")) {
                    shortName = port.portName.replaceFirst("/dev/cu.", "");
                }
                port.shortName = shortName;
                map.put(shortName,port);
            }
            
            ports.clear();
            ports.addAll(map.values());
        }
        
        //remove manually blocked items (such as bluetooth)
        Iterator<SerialPort> it = ports.iterator();
        while(it.hasNext()) {
            SerialPort port = it.next();
            if(port.shortName.toLowerCase().contains("bluetooth")) it.remove();
        }
        
        for(SerialPort port : ports) {
            Util.p("final port = " + port.portName + " short = " + port.shortName);
        }
        
        //sort by name
        //if only one, use it.
        return ports;
    }

    List<SerialPort> getPorts() {
        return this.ports;
    }

    SerialPort getPortForPath(String portName) {
        for(SerialPort port : this.ports) {
            if(port.portName.equals(portName)) {
                return port;
            }
        }
        return null;
    }

    Config getConfigForName(String configName) {
        for(Config config : this.configs) {
            if(config.getName().equals(configName)) {
                return config;
            }
        }
        return null;
    }
    
    private List<Device> scanForDevices() {
        List<Device> devices  = new ArrayList<>();
        Device uno = new Device();
        uno.name = "Arduino Uno";
        uno.protocol = "arduino";
        uno.maximum_size = 32256;
        uno.upload_speed=115200;
        uno.low_fuses = 0xff;
        uno.high_fuses=0xde;
        uno.extended_fuses=0xde;
        uno.path="optiboot";
        uno.file="optiboot_atmega328.hex";
        uno.unlock_bits = 0x3f;
        uno.lock_bits = 0x0F;
        uno.mcu="atmega328p";
        uno.f_cpu="16000000L";
        uno.core = "arduino";
        uno.variant = "standard";
        devices.add(uno);
        
        
        
        Device atmega328 = new Device();
        atmega328.name="Arduino Duemilanove w/ ATmega328";
        atmega328.protocol="arduino";
        atmega328.maximum_size=30720;
        atmega328.upload_speed=57600;
        
        atmega328.low_fuses=0xFF;
        atmega328.high_fuses=0xDA;
        
        atmega328.extended_fuses=0x05;
        atmega328.path="atmega";
        atmega328.file="ATmegaBOOT_168_atmega328.hex";
        atmega328.unlock_bits=0x3F;
        atmega328.lock_bits=0x0F;

        atmega328.mcu="atmega328p";
        atmega328.f_cpu="16000000L";
        atmega328.core="arduino";
        atmega328.variant="standard";
        devices.add(atmega328);

        Device boarduino = new Device();
        boarduino.name = "Boarduino";
        boarduino.compatible = atmega328;
        devices.add(boarduino);
        

        Device leonardo = new Device();
        leonardo.name="Arduino Leonardo";
        leonardo.protocol="avr109";
        leonardo.maximum_size=28672;
        leonardo.upload_speed=57600;
        leonardo.disable_flushing=true;
        leonardo.low_fuses=0xff;
        leonardo.high_fuses=0xd8;
        leonardo.extended_fuses=0xcb;
        leonardo.path="caterina";
        leonardo.file="Caterina-Leonardo.hex";
        leonardo.unlock_bits=0x3F;
        leonardo.lock_bits=0x2F;
        leonardo.mcu="atmega32u4";
        leonardo.f_cpu="16000000L";
        leonardo.vid="0x2341";
        leonardo.pid="0x8036";
        leonardo.core="arduino";
        leonardo.variant="leonardo";

        devices.add(leonardo);
        
        Device pro5v328 = new Device();
        pro5v328.name= "Arduino Pro or Pro Mini (5V, 16 MHz) w/ ATmega328";
        pro5v328.protocol="arduino";
        pro5v328.maximum_size=30720;
        pro5v328.upload_speed=57600;
        pro5v328.low_fuses=0xFF;
        pro5v328.high_fuses=0xDA;
        pro5v328.extended_fuses=0x05;
        pro5v328.path="atmega";
        pro5v328.file="ATmegaBOOT_168_atmega328.hex";
        pro5v328.unlock_bits=0x3F;
        pro5v328.lock_bits=0x0F;
        pro5v328.mcu="atmega328p";
        pro5v328.f_cpu="16000000L";
        pro5v328.core="arduino";
        pro5v328.variant="standard";
        
        devices.add(pro5v328);

        extracore = new Device();
        extracore.name = "ExtraCore";
        extracore.compatible = pro5v328;
        
        devices.add(extracore);
        
        
        return devices;
    }
    
    List<Device> getDevices() {
        return this.devices;
    }
    
    List<Config> getConfigs() {
        return this.configs;
    }

    private List<Config> loadConfigs() {
        List<Config> configs = new ArrayList<>();
        Config c1 = new Config() {

            @Override
            SerialPort getSerialPort() {
                if(ports.size() == 0) return null;
                return ports.get(0);
            }
            
        };
        c1.name = "Default: auto";
        c1.device = devices.get(0);
        configs.add(c1);
        defaultConfig = c1;
        
        Config c2 = new Config();
        c2.name = "Boarduino Project";
        c2.device = devices.get(1);
        configs.add(c2);
        
        editConfigStub = new Config();
        editConfigStub.name = "Edit ...";
        
        Config c3 = new Config() {

            @Override
            SerialPort getSerialPort() {
                if(ports.size() == 0) return null;
                return ports.get(0);
            }
            
        };
        c3.name = "Extra Core: auto";
        c3.device = extracore;
        configs.add(c3);
        
        return configs;
    }

    Config getDefaultConfig() {
        return defaultConfig;
    }

    File getArduinoDir() {
        return arduinoDir;
    }

    void setArduinoDir(File arduinoPath) {
        this.arduinoDir = arduinoPath;
        saveSettings();
    }

    private void saveSettings() {
        try {
            Properties props = new Properties();
            props.setProperty(ARDUINO_IDE_PATH, this.arduinoDir.getAbsolutePath());
            StringBuffer sb = new StringBuffer();
            for(String s : recentSketches) {
                sb.append(s);
                sb.append(",");
            }
            props.setProperty(RECENT_SKETCHES, sb.toString());
            Util.p("write out" + sb.toString());
            props.storeToXML(new FileOutputStream("settings.xml"), "ArduinoX Settings");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadSettings() {
        try {
            Properties props = new Properties();
            props.loadFromXML(new FileInputStream("settings.xml"));
            if(props.containsKey(ARDUINO_IDE_PATH)) {
                this.arduinoDir = new File(props.getProperty(ARDUINO_IDE_PATH));
            }
            recentSketches = new ArrayList<>();
            recentUniqueSketches = new HashSet<>();
            if(props.containsKey(RECENT_SKETCHES)) {
                String[] s = props.getProperty(RECENT_SKETCHES).split(",");
                for(String ss : s) {
                    //first, skip the dupes
                    if(recentUniqueSketches.contains(ss)) continue;
                    
                    recentUniqueSketches.add(ss);
                    recentSketches.add(ss);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    Device getDeviceForName(String deviceName) {
        for(Device d : devices) {
            if(d.getName().equals(deviceName)) return d;
        }
        return null;
    }

    Iterable<File> getRecentSketches() {
        List<File> files = new ArrayList<>();
        for(String skdir : recentSketches) {
            files.add(new File(skdir));
        }
        return files;
    }

}
