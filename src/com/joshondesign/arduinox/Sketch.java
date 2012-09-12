/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.joshondesign.arduino.common.Util;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

/**
 *
 * @author josh
 */
public class Sketch {
    private String name;
    private List<SketchBuffer> buffers;
    private final File dir;

    Sketch(File sketchDir) throws IOException {
        this.dir = sketchDir;
        this.name = sketchDir.getName();
        this.buffers = new ArrayList<>();
        
        buffers.add(new SketchBuffer(new File(sketchDir,sketchDir.getName()+".ino")));
    }
    
    List<SketchBuffer> getBuffers() {
        return this.buffers;
    }

    String getName() {
        return this.name;
    }
    
    
    
    
    public static class SketchBuffer {
        private final File file;
        private final String code;
        private boolean dirty = false;
        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        
        private SketchBuffer(File file) throws IOException {
            this.file = file;
            this.code = Util.toString(file);
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
        
    }
}
