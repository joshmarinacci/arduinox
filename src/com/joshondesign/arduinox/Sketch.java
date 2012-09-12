/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.joshondesign.arduino.common.Util;
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
        private SketchBuffer(File file) throws IOException {
            this.file = file;
            this.code = Util.toString(file);
            Util.p("code = " + code);
        }
        
        public String getName() {
            return this.file.getName();
        }

        public File getFile() {
            return this.file;
        }
        
    }
}
