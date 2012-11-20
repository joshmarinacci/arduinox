/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Util;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author josh
 */
public class Example {
    Set<String> keywords = new HashSet<>();
    String name = "";
    String description = "";
    File directory;

    String getName() {
        return name;
    }

    File getDirectory() {
        return this.directory;
    }

    File cloneTo(File documentsDir) {
        File newdir = new File(documentsDir,directory.getName());
        try {
            if(newdir.exists()) {
                Util.p("warning. the directory already exists! " + newdir.getAbsolutePath());
            } else {
                newdir.mkdir();
                for(File f : directory.listFiles()) {
                    File nf = new File(newdir,f.getName());
                    Util.p("copying: " + nf.getAbsolutePath());
                    Util.toFile(Util.toString(f),nf);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return newdir;
    }
}
