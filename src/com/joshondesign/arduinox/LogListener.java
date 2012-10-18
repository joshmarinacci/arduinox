/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

/**
 *
 * @author josh
 */
public interface LogListener {

    public void log(String str);

    public void log(Exception ex);
    
}
