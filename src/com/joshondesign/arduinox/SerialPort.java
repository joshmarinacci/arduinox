/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Util;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author josh
 */
class SerialPort {
    String shortName;
    String portName;
    private List<PortChange> listeners;

    SerialPort() {
        this.listeners = new ArrayList<>();
    }

    void lock() {
        Util.p("locking the port. notifying");
        for(PortChange l : listeners) {
            Util.p("notified");
            l.lock();
        }
    }

    void unlock() {
        for(PortChange l : listeners) {
            l.unlock();
        }
    }
    
    
    public static interface PortChange {
        public void lock();
        public void unlock();
    }
    
    public void addListener(PortChange l) {
        this.listeners.add(l);
    }
}
