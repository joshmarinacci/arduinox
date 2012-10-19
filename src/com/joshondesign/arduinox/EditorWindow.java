package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.CompileException;
import com.joshondesign.arduino.common.Device;
import com.joshondesign.arduino.common.MessageConsumer;
import com.joshondesign.arduino.common.Preferences;
import com.joshondesign.arduino.common.Serial;
import com.joshondesign.arduino.common.SerialException;
import com.joshondesign.arduino.common.Util;
import com.joshondesign.arduinox.Sketch.SketchBuffer;
import gnu.io.CommPortIdentifier;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import jsyntaxpane.actions.ActionUtils;

/**
 *
 * @author josh
 */
public class EditorWindow extends javax.swing.JFrame implements SerialPort.PortChange {
    
    private List<JEditorPane> editors = new ArrayList<>();
    private Font customFont;
    private Map<SketchBuffer,JScrollPane> scrolls = new HashMap<>();
    private Actions actions = null;
    private int editorSplitPosition;
    private int masterSplitPosition;
    private int currentSerialRate;
    private Serial serial;
    private boolean connected;

    public EditorWindow() {
        initComponents();
    }
    
    
    public EditorWindow(Actions actions) {
        this.actions = actions;
        try {
            customFont = Font.createFont(Font.TRUETYPE_FONT, EditorWindow.class.getResourceAsStream("resources/SourceCodePro-Regular.ttf"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        initComponents();
        
        newSketchItem.addActionListener(actions.newAction);
        openSketchItem.addActionListener(actions.openAction);
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

        actions.addLogListener(new LogListener() {
            @Override
            public void log(String str) {
                console.setText(console.getText()+str+"\n");
            }

            @Override
            public void log(Exception ex) {
                //console.append(null)
                if(ex instanceof CompileException) {
                    CompileException cex = (CompileException) ex;
                    console.append(
                            ex.getMessage()
                            + "\n"
                            + cex.getCompilerMessage()
                            + "\n"
                            );
                } else {
                    console.setText(console.getText() 
                            + ex.getMessage()
                            + "\n"
                            );
                }
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
        
        for(JEditorPane pane : editors) {
            pane.setBackground(actions.getCurrentTheme().backgroundColor);
        }
        
        JEditorPane pane = editors.get(0);       
        Util.p("editor = " + pane);
        List<Action> list = Arrays.asList(pane.getActions());
        Collections.sort(list, new Comparator<Action>() {
            @Override
            public int compare(Action o1, Action o2) {
                return ((String)o1.getValue(Action.NAME)).compareTo((String)o2.getValue(Action.NAME));
            }
        });
        
        for(Action a : list) {
            Util.p("action = " + a.getValue(Action.NAME) + "   shortcut = " + a.getValue(Action.ACCELERATOR_KEY) );
        }
        
        
        //fix up the actions. this should eventually move to some new location
        
        Action cutAction = ActionUtils.getAction(pane, DefaultEditorKit.CutAction.class);
        cutAction.putValue(Action.NAME, "Cut");
        cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("meta X"));
        cutItem.setAction(cutAction);
        
        Action copy =  ActionUtils.getAction(pane, DefaultEditorKit.CopyAction.class);
        copy.putValue(Action.NAME, "Copy");
        copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("meta C"));
        copyItem.setAction(copy);
        
        Action pasteAction =  ActionUtils.getAction(pane, DefaultEditorKit.PasteAction.class);
        pasteAction.putValue(Action.NAME, "Paste");
        pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("meta V"));        
        pasteItem.setAction(pasteAction);
        
        HashMap<Object, Action> map = createActionTable(pane);
        Action selectAllAction = map.get(DefaultEditorKit.selectAllAction);
        selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("meta A"));
        selectAll.setAction(selectAllAction);

        undoItem.setAction(ActionUtils.getAction(pane, jsyntaxpane.actions.UndoAction.class));
        redoItem.setAction(ActionUtils.getAction(pane, jsyntaxpane.actions.RedoAction.class));
        
        
        
        KeyboardUtils.setup(pane);
        
        
        List<Config> configs = new ArrayList<>(Global.getGlobal().getConfigs());
        configs.add(Global.getGlobal().editConfigStub);
        deviceDropdown.setModel(new DefaultComboBoxModel(configs.toArray()));
        deviceDropdown.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(comp instanceof JLabel && value instanceof Config) {
                    JLabel label = (JLabel) comp;
                    Config config = (Config) value;
                    label.setText(config.getName());
                }
                return comp;
            }
        });
        if(actions.sketch.getCurrentConfig() == null) {
            actions.sketch.setCurrentConfig(Global.getGlobal().getDefaultConfig());
        }
        deviceDropdown.setSelectedItem(actions.sketch.getCurrentConfig());
        
        rebuildWindowMenu();
        //register to listen for changes
        Global.getGlobal().addPropertyChangeListener("sketches", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                rebuildWindowMenu();
            }
        });
        
        //set sizing so that we can open and close the split pane
        consoleTabPane.setMinimumSize(new Dimension());
        helpScroll.setMinimumSize(new Dimension());
        
        try {
            String helptext = Util.toString(getClass().getResource("resources/cheatsheet.html"));
            Util.p("content = " + helptext);
            helpPane.setText(helptext);
        } catch (IOException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        String[] serialRateStrings = {
          "300","1200","2400","4800","9600","14400",
          "19200","28800","38400","57600","115200"
        };

        serialRateCombo.setModel(new DefaultComboBoxModel(serialRateStrings));
        autoScroll.setSelected(actions.sketch.isAutoScroll());
    }

    private void rebuildWindowMenu() {
        //setup the windows menu to auto update
        windowMenu.removeAll();
        for(final Sketch sketch :Global.getGlobal().getSketches()) {
            AbstractAction action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Global.getGlobal().getWindowForSketch(sketch).toFront();
                }
            };
            action.putValue(Action.NAME, sketch.getName());
            windowMenu.add(action);
        }
    }

    @Override
    public void lock() {
        serialActive.setEnabled(false);
        serialActive.setText("Uploading...");
        serialRateCombo.setEnabled(false);
        if(serial != null) {
            serial.dispose();
            serial = null;
        }
    }

    @Override
    public void unlock() {
        serialActive.setText("Connect");
        serialActive.setEnabled(true);
        serialRateCombo.setEnabled(true);
        if(connected) {
            connect(actions.sketch.getCurrentPort());
        }
    }

    private void disconnect() {
        connected = false;
        serialActive.setText("Connect");
        serialRateCombo.setEnabled(true);
        serial.dispose();
    }

    private void connect(SerialPort port) {
        try {
            connected = true;
            serialActive.setText("Disconnect");
            serialRateCombo.setEnabled(false);
            serial = new Serial(port.portName, 9600, 'N', 8, 1.0f);
            serial.addListener(new MessageConsumer() {
                @Override
                public void message(String s) {
                    serialConsole.append(s);
                    if(actions.sketch.isAutoScroll()) {
                        serialConsole.setCaretPosition(serialConsole.getDocument().getLength());                                
                    }
                }
            });
        } catch (SerialException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
            serialActive.setText("Connect");
        }

    }
    
    
    public static class SerialPortComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(comp instanceof JLabel && value instanceof SerialPort) {
                JLabel label = (JLabel) comp;
                SerialPort port = (SerialPort) value;
                label.setText(port.shortName);
            }
            return comp;
        }
        
    }
    
    private HashMap<Object, Action> createActionTable(JTextComponent textComponent) {
        HashMap<Object, Action> actions = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++) {
            Action a = actionsArray[i];
            actions.put(a.getValue(Action.NAME), a);
        }
        return actions;
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
        final CustomEditorPane pane = new CustomEditorPane(buffer);
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
                buffer.setText(pane.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                buffer.markDirty();
                buffer.setText(pane.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                buffer.markDirty();
                buffer.setText(pane.getText());
            }
        });
        

        //TODO: this could be made into a reusable listener shared by all buffers
        buffer.addPropertyChangeListener("dirty",new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                int n = tabs.indexOfComponent(scrolls.get(buffer));
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

        masterSplit = new javax.swing.JSplitPane();
        editorSplit = new javax.swing.JSplitPane();
        tabbedPane = new javax.swing.JTabbedPane();
        consoleTabPane = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        console = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        serialConsole = new javax.swing.JTextArea();
        serialActive = new javax.swing.JToggleButton();
        serialRateCombo = new javax.swing.JComboBox();
        serialPortLabel = new javax.swing.JLabel();
        autoScroll = new javax.swing.JCheckBox();
        helpScroll = new javax.swing.JScrollPane();
        helpPane = new javax.swing.JEditorPane();
        jToolBar1 = new javax.swing.JToolBar();
        checkButton = new javax.swing.JButton();
        runButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jLabel2 = new javax.swing.JLabel();
        deviceDropdown = new javax.swing.JComboBox();
        consoleToggle = new javax.swing.JToggleButton();
        rightToggle = new javax.swing.JToggleButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        newSketchItem = new javax.swing.JMenuItem();
        openSketchItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        checkMenuItem = new javax.swing.JMenuItem();
        quitMenu = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        cutItem = new javax.swing.JMenuItem();
        copyItem = new javax.swing.JMenuItem();
        pasteItem = new javax.swing.JMenuItem();
        undoItem = new javax.swing.JMenuItem();
        redoItem = new javax.swing.JMenuItem();
        indentMenuItem = new javax.swing.JMenuItem();
        selectAll = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        zoomInItem = new javax.swing.JMenuItem();
        zoomOutItem = new javax.swing.JMenuItem();
        standardThemeItem = new javax.swing.JMenuItem();
        lightThemeItem = new javax.swing.JMenuItem();
        darkThemeItem = new javax.swing.JMenuItem();
        windowMenu = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        masterSplit.setDividerLocation(500);
        masterSplit.setResizeWeight(1.0);

        editorSplit.setDividerLocation(200);
        editorSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        editorSplit.setResizeWeight(1.0);
        editorSplit.setLeftComponent(tabbedPane);

        console.setColumns(20);
        console.setRows(5);
        jScrollPane1.setViewportView(console);

        consoleTabPane.addTab("Compiler", jScrollPane1);

        serialConsole.setEditable(false);
        serialConsole.setColumns(20);
        serialConsole.setRows(5);
        jScrollPane3.setViewportView(serialConsole);

        serialActive.setText("Connect");
        serialActive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialActiveActionPerformed(evt);
            }
        });

        serialRateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        serialPortLabel.setText("serial port");

        autoScroll.setText("autoscroll");
        autoScroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoScrollActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(serialRateCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(serialActive)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(serialPortLabel))
                    .add(autoScroll))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
            .add(jPanel1Layout.createSequentialGroup()
                .add(serialActive)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(serialRateCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(serialPortLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(autoScroll)
                .addContainerGap())
        );

        consoleTabPane.addTab("Serial", jPanel1);

        editorSplit.setRightComponent(consoleTabPane);

        masterSplit.setLeftComponent(editorSplit);

        helpPane.setContentType("text/html"); // NOI18N
        helpPane.setText("<html>\n  <head>\n\n  </head>\n  <body>\n<h3>Help and Info</h3>\n    <p style=\"margin-top: 0\">\n       <b>This</b> is real help text.\n    </p>\n  </body>\n</html>\n");
        helpScroll.setViewportView(helpPane);

        masterSplit.setRightComponent(helpScroll);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        checkButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/joshondesign/arduinox/resources/noun_project_1307.png"))); // NOI18N
        checkButton.setText("Build");
        checkButton.setFocusable(false);
        checkButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        checkButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(checkButton);

        runButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/joshondesign/arduinox/resources/noun_project_2873.png"))); // NOI18N
        runButton.setText("Build & Run");
        runButton.setFocusable(false);
        runButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        runButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(runButton);

        jSeparator1.setMaximumSize(new java.awt.Dimension(40, 2147483647));
        jSeparator1.setMinimumSize(new java.awt.Dimension(10, 1));
        jToolBar1.add(jSeparator1);

        jLabel2.setText("Device");
        jToolBar1.add(jLabel2);

        deviceDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        deviceDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceChanged(evt);
            }
        });
        jToolBar1.add(deviceDropdown);

        consoleToggle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/joshondesign/arduinox/resources/consoleicon.png"))); // NOI18N
        consoleToggle.setSelected(true);
        consoleToggle.setText("Console");
        consoleToggle.setFocusable(false);
        consoleToggle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        consoleToggle.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        consoleToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleToggleActionPerformed(evt);
            }
        });
        jToolBar1.add(consoleToggle);

        rightToggle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/joshondesign/arduinox/resources/sidebaricon.png"))); // NOI18N
        rightToggle.setSelected(true);
        rightToggle.setText("Sidebar");
        rightToggle.setFocusable(false);
        rightToggle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rightToggle.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        rightToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightToggleActionPerformed(evt);
            }
        });
        jToolBar1.add(rightToggle);

        jMenu1.setText("File");

        newSketchItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.META_MASK));
        newSketchItem.setText("New Sketch");
        jMenu1.add(newSketchItem);

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

        cutItem.setText("Cut");
        jMenu2.add(cutItem);

        copyItem.setText("copy");
        jMenu2.add(copyItem);

        pasteItem.setText("paste");
        jMenu2.add(pasteItem);

        undoItem.setText("undo");
        jMenu2.add(undoItem);

        redoItem.setText("redo");
        jMenu2.add(redoItem);

        indentMenuItem.setText("indent");
        jMenu2.add(indentMenuItem);

        selectAll.setText("selectall");
        selectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllActionPerformed(evt);
            }
        });
        jMenu2.add(selectAll);

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

        windowMenu.setText("Window");
        jMenuBar1.add(windowMenu);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(masterSplit, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 661, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(masterSplit, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void deviceChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceChanged
        Config config = (Config) deviceDropdown.getSelectedItem();
        if(config == null) return;
        if(config == Global.getGlobal().editConfigStub) {
            JFrame frame = new JFrame("Edit Configurations");
            frame.add(new DeviceChooser());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } else {
            actions.sketch.setCurrentConfig(config);
            SerialPort port = config.getSerialPort();
            if(port == null) {
                serialPortLabel.setText("");
                serialActive.setEnabled(false);
            } else {
                port.addListener(this);
                serialActive.setEnabled(true);
                serialPortLabel.setText(port.shortName);
            }
        }
    }//GEN-LAST:event_deviceChanged

    private void selectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_selectAllActionPerformed

    private void consoleToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleToggleActionPerformed
        if(consoleToggle.isSelected()) {
            editorSplit.setDividerLocation(editorSplitPosition);
        } else {
            editorSplitPosition = editorSplit.getDividerLocation();
            editorSplit.setDividerLocation(1.0d);
        }
        
        
    }//GEN-LAST:event_consoleToggleActionPerformed

    private void rightToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightToggleActionPerformed
        if(rightToggle.isSelected()) {
            masterSplit.setDividerLocation(masterSplitPosition);
        } else {
            masterSplitPosition = masterSplit.getDividerLocation();
            masterSplit.setDividerLocation(1.0d);
        }
    }//GEN-LAST:event_rightToggleActionPerformed

    private void serialActiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialActiveActionPerformed
        SerialPort port = actions.sketch.getCurrentConfig().getSerialPort();
        if(port != null) {
            if(!serialActive.isSelected()) {
                disconnect();
            } else {
                connect(port);
            }
        }
    }//GEN-LAST:event_serialActiveActionPerformed

    private void autoScrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoScrollActionPerformed
        actions.sketch.setAutoScroll(autoScroll.isSelected());
    }//GEN-LAST:event_autoScrollActionPerformed

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
    private javax.swing.JCheckBox autoScroll;
    private javax.swing.JButton checkButton;
    private javax.swing.JMenuItem checkMenuItem;
    private javax.swing.JTextArea console;
    private javax.swing.JTabbedPane consoleTabPane;
    private javax.swing.JToggleButton consoleToggle;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JMenuItem cutItem;
    private javax.swing.JMenuItem darkThemeItem;
    private javax.swing.JComboBox deviceDropdown;
    private javax.swing.JSplitPane editorSplit;
    private javax.swing.JEditorPane helpPane;
    private javax.swing.JScrollPane helpScroll;
    private javax.swing.JMenuItem indentMenuItem;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem lightThemeItem;
    private javax.swing.JSplitPane masterSplit;
    private javax.swing.JMenuItem newSketchItem;
    private javax.swing.JMenuItem openSketchItem;
    private javax.swing.JMenuItem pasteItem;
    private javax.swing.JMenuItem quitMenu;
    private javax.swing.JMenuItem redoItem;
    private javax.swing.JToggleButton rightToggle;
    private javax.swing.JButton runButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem selectAll;
    private javax.swing.JToggleButton serialActive;
    private javax.swing.JTextArea serialConsole;
    private javax.swing.JLabel serialPortLabel;
    private javax.swing.JComboBox serialRateCombo;
    private javax.swing.JMenuItem standardThemeItem;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JMenuItem undoItem;
    private javax.swing.JMenu windowMenu;
    private javax.swing.JMenuItem zoomInItem;
    private javax.swing.JMenuItem zoomOutItem;
    // End of variables declaration//GEN-END:variables
}
