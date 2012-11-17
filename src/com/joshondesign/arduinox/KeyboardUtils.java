/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Keymap;
import jsyntaxpane.util.Configuration;

/**
 *
 * @author josh
 */
public class KeyboardUtils {
    private static JEditorPane pane;
    private static Keymap keys;
    
    static void setup(JEditorPane editorpane, Actions actions) {
        pane = editorpane;
        //Action upAction = pane.getActionMap().get(DefaultEditorKit.upAction);
        //Util.p("foo = " + DefaultEditorKit.upAction);
        //upAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control P"));

        
        
        for(KeyStroke stroke : pane.getKeymap().getBoundKeyStrokes()) {
//            Util.p("stroke = " + stroke);
        }
        
        keys = pane.getKeymap();
        
//        Util.p("e = " + keys.getAction(KeyStroke.getKeyStroke("control E")));
        
        bind("control P",DefaultEditorKit.upAction);
        bind("control N",DefaultEditorKit.downAction);
        bind("control F",DefaultEditorKit.forwardAction);
        bind("control B",DefaultEditorKit.backwardAction);
        bind("control D",DefaultEditorKit.deleteNextCharAction);
        bind("control A",DefaultEditorKit.beginLineAction);
        bind("control E",DefaultEditorKit.endLineAction);
        bind("meta K",actions.checkAction);
        bind("meta R",actions.runAction);
        
        
        for(Action a : pane.getKeymap().getBoundActions()) {
            //Util.p("bound action: " + a.getValue(Action.NAME) + " " + a.getValue(Action.ACCELERATOR_KEY));
        }
        
    }

    private static void bind(String name, String upAction) {
        Action act = pane.getActionMap().get(upAction);
        keys.addActionForKeyStroke(KeyStroke.getKeyStroke(name), act);
    }
    private static void bind(String name, Action act) {
        keys.addActionForKeyStroke(KeyStroke.getKeyStroke(name), act);
    }

    static void setup(Configuration config) {
        //Util.p("config");
        List<String> keys = new ArrayList<String>(config.keySet());
        Collections.sort(keys);
        for(String key : keys) {
            Util.p(key + " " + config.get(key));
        }
        config.put("Action.quick-find", "jsyntaxpane.actions.QuickFindAction, meta F");
        config.put("Action.goto-line",  "jsyntaxpane.actions.GotoLineAction, meta G");
        config.put("Action.undo","jsyntaxpane.actions.UndoAction, meta Z");
        config.put("Action.redo","jsyntaxpane.actions.RedoAction, meta shift Z");
        config.put("Action.delete-lines","jsyntaxpane.actions.DeleteLinesAction");
        config.put("Action.delete-lines","jsyntaxpane.actions.DeleteLinesAction, control K");
    }
    
}
