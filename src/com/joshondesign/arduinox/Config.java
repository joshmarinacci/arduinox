/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Device;

/**
 *
 * @author josh
 */
public class Config {
    public String name;
    public Device device;

    String getName() {
        return this.name;
    }

    Device getDevice() {
        return device;
    }
    
}
