/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author josh
 */
public class Global {
    private static Global _global;
    List<Sketch> sketches = new ArrayList<>();

    public Global() {
    }
    
    

    static Global getGlobal() {
        if(_global == null) {
            _global = new Global();
        }
        return _global;
    }
    
}
