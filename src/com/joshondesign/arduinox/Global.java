/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
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

    public Global() {
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
    
}
