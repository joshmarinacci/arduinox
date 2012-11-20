/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author josh
 */
public class ConsoleTextPane extends JTextPane {

    void appendInfo(String string) {
        try {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, Color.BLACK);
            StyleConstants.setBold(attributes, true);
            getDocument().insertString(getDocument().getLength(), string+"\n", attributes);
        } catch (BadLocationException ex) {
            Logger.getLogger(ConsoleTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void appendExec(String string) {
        try {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, new Color(0x008800));
            getDocument().insertString(getDocument().getLength(), string+"\n", attributes);
        } catch (BadLocationException ex) {
            Logger.getLogger(ConsoleTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void appendSTDOUT(String string) {
        try {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, Color.BLUE);
            getDocument().insertString(getDocument().getLength(), string, attributes);
        } catch (BadLocationException ex) {
            Logger.getLogger(ConsoleTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void appendSTDERR(String string) {
        try {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, Color.RED);
            getDocument().insertString(getDocument().getLength(), string, attributes);
        } catch (BadLocationException ex) {
            Logger.getLogger(ConsoleTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
