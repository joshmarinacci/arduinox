/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Util;
import com.joshondesign.arduinox.Sketch.SketchBuffer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author josh
 */
public class EditorWindow extends javax.swing.JFrame {
    
    private List<JEditorPane> editors = new ArrayList<>();
    private Font customFont;
    private Map<SketchBuffer,JScrollPane> scrolls = new HashMap<>();

    /**
     * Creates new form EditorWindow
     */
    public EditorWindow() {
        initComponents();
    }
    public EditorWindow(Actions actions) {
        try {
            customFont = Font.createFont(Font.TRUETYPE_FONT, EditorWindow.class.getResourceAsStream("resources/UbuntuMono-R.ttf"));
            //Font font2 = Font.createFont(Font.TRUETYPE_FONT, EditorPane.class.getResourceAsStream("resources/UbuntuMono-B.ttf"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        initComponents();

        saveMenuItem.addActionListener(actions.saveAction);
        checkMenuItem.addActionListener(actions.checkAction);
        quitMenu.addActionListener(actions.quitAction);
        zoomInItem.addActionListener(actions.zoomInAction);
        zoomOutItem.addActionListener(actions.zoomOutAction);
        standardThemeItem.addActionListener(actions.switchStandardTheme);
        lightThemeItem.addActionListener(actions.switchLightTheme);
        darkThemeItem.addActionListener(actions.switchDarkTheme);
        
        for(Sketch.SketchBuffer buffer : actions.sketch.getBuffers()) {
            createNewTab(tabbedPane,buffer);
        }
        checkButton.addActionListener(actions.checkAction);
        runButton.addActionListener(actions.runAction);

        actions.addLogListener(new Actions.LogListener() {
            @Override
            public void log(String str) {
                console.setText(console.getText()+str+"\n");
            }
        });

        actions.pcs.addPropertyChangeListener("fontsize", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                float size = ((Float)evt.getNewValue()).floatValue();
                Util.p("setting to : " + size);
                for(JEditorPane pane : editors) {
                    pane.setFont(customFont.deriveFont(size));
                }
            }
        });
        
        actions.pcs.addPropertyChangeListener("theme", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ColorTheme theme = (ColorTheme) evt.getNewValue();
                for(JEditorPane pane : editors) {
                    pane.setBackground(theme.backgroundColor);
                }
            }
        });
        
    }
    
    
    private class CustomEditorPane extends JEditorPane {
        private final SketchBuffer buffer;

        public CustomEditorPane(SketchBuffer buffer) {
            super();
            LookAndFeel laf = UIManager.getLookAndFeel();
            if ( laf.getID().equals("Nimbus") )
            {
              //ugly fix so that we can change background in Nimbus
              setUI(new javax.swing.plaf.basic.BasicEditorPaneUI());
            }                    
            //setBackground(new Color(230,230,00));
            
            this.buffer = buffer;
        }
    }

    
    private void createNewTab(final JTabbedPane tabs, final Sketch.SketchBuffer buffer) {
        CustomEditorPane pane = new CustomEditorPane(buffer);
        final JScrollPane scroll = new JScrollPane(pane);
        pane.setContentType("text/java");
        //pane.setFont(new Font("Monaco",Font.PLAIN,12));
        pane.setFont(customFont.deriveFont(12f));
        try {
            pane.setText(Util.toString(buffer.getFile()));
        } catch (IOException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.editors.add(pane);
        this.scrolls.put(buffer,scroll);
        
        //TODO: this could probably be made shared
        pane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                buffer.markDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                buffer.markDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                buffer.markDirty();
            }
        });
        

        //TODO: this could be made into a reusable listener shared by all buffers
        buffer.addPropertyChangeListener("dirty",new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                int n = tabs.indexOfComponent(scrolls.get(buffer));
                Util.p("n = " + n);
                if(buffer.isDirty()) {
                    tabs.setTitleAt(0, buffer.getName() + " *");
                } else {
                    tabs.setTitleAt(0, buffer.getName());
                }
            }
        });
        
        tabs.add(buffer.getName(),scroll);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        tabbedPane = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        console = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jEditorPane1 = new javax.swing.JEditorPane();
        jToolBar1 = new javax.swing.JToolBar();
        checkButton = new javax.swing.JButton();
        runButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton3 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openSketchItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        checkMenuItem = new javax.swing.JMenuItem();
        quitMenu = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        zoomInItem = new javax.swing.JMenuItem();
        zoomOutItem = new javax.swing.JMenuItem();
        standardThemeItem = new javax.swing.JMenuItem();
        lightThemeItem = new javax.swing.JMenuItem();
        darkThemeItem = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(500);
        jSplitPane1.setResizeWeight(1.0);

        jSplitPane2.setDividerLocation(300);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(1.0);
        jSplitPane2.setLeftComponent(tabbedPane);

        console.setColumns(20);
        console.setRows(5);
        jScrollPane1.setViewportView(console);

        jSplitPane2.setRightComponent(jScrollPane1);

        jSplitPane1.setLeftComponent(jSplitPane2);

        jEditorPane1.setContentType("text/html"); // NOI18N
        jEditorPane1.setText("<html>\n  <head>\n\n  </head>\n  <body>\n<h3>Help and Info</h3>\n    <p style=\"margin-top: 0\">\n       <b>This</b> is real help text.\n    </p>\n  </body>\n</html>\n");
        jScrollPane2.setViewportView(jEditorPane1);

        jSplitPane1.setRightComponent(jScrollPane2);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        checkButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/joshondesign/arduinox/resources/noun_project_1307.png"))); // NOI18N
        checkButton.setFocusable(false);
        checkButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        checkButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(checkButton);

        runButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/joshondesign/arduinox/resources/noun_project_2873.png"))); // NOI18N
        runButton.setFocusable(false);
        runButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        runButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(runButton);

        jSeparator1.setMaximumSize(new java.awt.Dimension(40, 2147483647));
        jSeparator1.setMinimumSize(new java.awt.Dimension(10, 1));
        jToolBar1.add(jSeparator1);

        jButton3.setText("+");
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton3);

        jMenu1.setText("File");

        openSketchItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        openSketchItem.setText("Open Sketch");
        jMenu1.add(openSketchItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
        saveMenuItem.setText("Save");
        jMenu1.add(saveMenuItem);

        checkMenuItem.setText("Check");
        jMenu1.add(checkMenuItem);

        quitMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        quitMenu.setText("Quit");
        jMenu1.add(quitMenu);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItem1.setText("Cut");
        jMenu2.add(jMenuItem1);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("View");

        zoomInItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, java.awt.event.InputEvent.META_MASK));
        zoomInItem.setText("Zoom In");
        jMenu3.add(zoomInItem);

        zoomOutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, java.awt.event.InputEvent.META_MASK));
        zoomOutItem.setText("Zoom Out");
        jMenu3.add(zoomOutItem);

        standardThemeItem.setText("Standard Theme");
        jMenu3.add(standardThemeItem);

        lightThemeItem.setText("Light Theme");
        jMenu3.add(lightThemeItem);

        darkThemeItem.setText("Dark Theme");
        jMenu3.add(darkThemeItem);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("Window");
        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 661, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(EditorWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(EditorWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(EditorWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(EditorWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new EditorWindow().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton checkButton;
    private javax.swing.JMenuItem checkMenuItem;
    private javax.swing.JTextArea console;
    private javax.swing.JMenuItem darkThemeItem;
    private javax.swing.JButton jButton3;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem lightThemeItem;
    private javax.swing.JMenuItem openSketchItem;
    private javax.swing.JMenuItem quitMenu;
    private javax.swing.JButton runButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem standardThemeItem;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JMenuItem zoomInItem;
    private javax.swing.JMenuItem zoomOutItem;
    // End of variables declaration//GEN-END:variables
}
