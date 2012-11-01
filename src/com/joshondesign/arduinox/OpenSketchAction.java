/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;

/**
 *
 * @author josh
 */
class OpenSketchAction extends AbstractAction {
    private final File skdir;

    public OpenSketchAction(File skdir) {
        super(skdir.getName());
        this.skdir = skdir;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Actions.openNewSketch(skdir);
    }
    
}
