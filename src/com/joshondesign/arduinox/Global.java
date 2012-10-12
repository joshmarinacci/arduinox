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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private Global() {
        this.ports = scanForSerialPorts();
        this.devices = scanForDevices();
    }
    
    

    static Global getGlobal() {
        if(_global == null) {
            _global = new Global();
        }
        return _global;
    }

    void addSketch(Sketch sketch) {
        this.sketches.add(sketch);
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
        
        Device boarduino = new Device();
        boarduino.name = "Boarduino (Deluiminvoa compatible)";
        boarduino.compatible = uno;
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
        
        return devices;
    }
    
    List<Device> getDevices() {
        return this.devices;
    }
}
