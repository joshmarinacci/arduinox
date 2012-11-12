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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
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
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.actions.ActionUtils;

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
            customFont = Font.createFont(Font.TRUETYPE_FONT, EditorWindow.class.getResourceAsStream("resources/SourceCodePro-Semibold.ttf"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        initComponents();
        
        setupMenu();
        
        for(Sketch.SketchBuffer buffer : actions.sketch.getBuffers()) {
            createNewTab(tabbedPane,buffer);
        }
        //setup toolbar
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
                    updateTheme(theme,pane);
                }
            }
        });
        
        for(JEditorPane pane : editors) {
            pane.setBackground(actions.getCurrentTheme().backgroundColor);
        }
        
        JEditorPane pane = editors.get(0);       
        /*
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
        * */
        
        
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
        
        
        List<Device> boards = new ArrayList<>(Global.getGlobal().getDevices());
        boardDropdown.setModel(new DefaultComboBoxModel(boards.toArray()));
        boardDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(comp instanceof JLabel && value instanceof Device) {
                    JLabel label = (JLabel) comp;
                    Device device = (Device) value;
                    label.setText(device.getName());
                }
                return comp;
            }
        });
        if(actions.sketch.getCurrentDevice() == null) {
            actions.sketch.setCurrentDevice(Global.getGlobal().getDevices().get(0));
        }
        boardDropdown.setSelectedItem(actions.sketch.getCurrentDevice());
        
        List<SerialPort> ports = Global.getGlobal().getPorts();
        portDropdown.setModel(new DefaultComboBoxModel(ports.toArray()));
        portDropdown.setRenderer(new SerialPortComboBoxRenderer());
        if(actions.sketch.getCurrentPort() == null && !Global.getGlobal().getPorts().isEmpty()) {
            actions.sketch.setCurrentPort(Global.getGlobal().getPorts().get(0));
        }
        if(actions.sketch.getCurrentPort() != null) {
            actions.sketch.getCurrentPort().addListener(this);
        }
        
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
            helpPane.setText(helptext);
        } catch (IOException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        

        serialRateCombo.setModel(new DefaultComboBoxModel(Global.SERIAL_RATE_STRINGS));
        try {
        int rate = actions.sketch.getSerialRate();
        int n = Arrays.binarySearch(Global.SERIAL_RATE_INTS, rate);
        serialRateCombo.setSelectedIndex(n);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        autoScroll.setSelected(actions.sketch.isAutoScroll());
        
        examplesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(value != null && value instanceof Example) {
                    Example ex = (Example) value;
                    JLabel label = (JLabel) comp;
                    label.setText(ex.getName());
                }
                return comp;
            }
        });
        
        final List<Example> examples = Global.getGlobal().getExamples();
        AbstractListModel<Example> model = new AbstractListModel<Example>() {
            @Override
            public int getSize() {
                return examples.size();
            }

            @Override
            public Example getElementAt(int index) {
                return examples.get(index);
            }
        };
        examplesList.setModel(model);
        
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
        serial = null;
    }

    private void connect(SerialPort port) {
        try {
            connected = true;
            serialActive.setText("Connected");
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

    private void setupMenu() {
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
        
        for(File skfile : Global.getGlobal().getRecentSketches()){
            recentSketchesMenu.add(new JMenuItem(new OpenSketchAction(skfile)));
        }
    }

    void resetPosition() {
        Sketch sketch = actions.sketch;
        int width = sketch.getIntSetting("window.width",800);
        int height = sketch.getIntSetting("window.height", 600);
        this.setSize(width, height);

        consoleToggle.setSelected(sketch.getBooleanSetting("window.split.editor.open",true));
        rightToggle.setSelected(sketch.getBooleanSetting("window.split.sidebar.open",true));
        editorSplitPosition = sketch.getIntSetting("window.split.editor", 300);
        masterSplitPosition = sketch.getIntSetting("window.split.sidebar", 550);
        this.setVisible(true);
        if(consoleToggle.isSelected()) {
            editorSplit.setDividerLocation(editorSplitPosition);
        } else {
            editorSplit.setDividerLocation(1.0d);
        }
        if(rightToggle.isSelected()) {
            masterSplit.setDividerLocation(masterSplitPosition);
        } else {
            masterSplit.setDividerLocation(1.0d);
        }
    }

    void shutdown() {
        Sketch sketch = actions.sketch;
        sketch.setIntSetting("window.width",getWidth());
        sketch.setIntSetting("window.height",getHeight());
        sketch.setIntSetting("window.split.sidebar", masterSplitPosition);
        sketch.setIntSetting("window.split.editor", editorSplitPosition);
        sketch.setBooleanSetting("window.split.editor.open",consoleToggle.isSelected());
        sketch.setBooleanSetting("window.split.sidebar.open",rightToggle.isSelected());
        actions.sketch.saveSettings();
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
        pane.setContentType("text/c");
        
        //pane.setFont(new Font("Monaco",Font.PLAIN,12));
        try {
            pane.setText(Util.toString(buffer.getFile()));
        } catch (IOException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        updateTheme(actions.STANDARD_THEME,pane);
        this.editors.add(pane);
        this.scrolls.put(buffer,scroll);
        pane.requestFocus();
        
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
        jButton1 = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        helpScroll = new javax.swing.JScrollPane();
        helpPane = new javax.swing.JEditorPane();
        jPanel2 = new javax.swing.JPanel();
        searchField = new javax.swing.JTextField();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        examplesList = new javax.swing.JList();
        jScrollPane4 = new javax.swing.JScrollPane();
        exampleDescription = new javax.swing.JEditorPane();
        jButton2 = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        checkButton = new javax.swing.JButton();
        runButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jLabel2 = new javax.swing.JLabel();
        boardDropdown = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        portDropdown = new javax.swing.JComboBox();
        consoleToggle = new javax.swing.JToggleButton();
        rightToggle = new javax.swing.JToggleButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        newSketchItem = new javax.swing.JMenuItem();
        openSketchItem = new javax.swing.JMenuItem();
        recentSketchesMenu = new javax.swing.JMenu();
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

        masterSplit.setDividerLocation(400);
        masterSplit.setResizeWeight(1.0);
        masterSplit.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                masterSplitPropertyChange(evt);
            }
        });

        editorSplit.setDividerLocation(200);
        editorSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        editorSplit.setResizeWeight(1.0);
        editorSplit.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                editorSplitPropertyChange(evt);
            }
        });
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
        serialRateCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialRateComboActionPerformed(evt);
            }
        });

        serialPortLabel.setText("Baud Rate");

        autoScroll.setText("autoscroll");
        autoScroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoScrollActionPerformed(evt);
            }
        });

        jButton1.setText("Clear");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(serialPortLabel)
                    .add(serialActive)
                    .add(autoScroll)
                    .add(jButton1)
                    .add(serialRateCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .add(jPanel1Layout.createSequentialGroup()
                .add(serialActive)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(serialPortLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(serialRateCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jButton1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(autoScroll)
                .addContainerGap())
        );

        consoleTabPane.addTab("Serial", jPanel1);

        editorSplit.setRightComponent(consoleTabPane);

        masterSplit.setLeftComponent(editorSplit);

        helpPane.setContentType("text/html"); // NOI18N
        helpPane.setText("<html>\n  <head>\n\n  </head>\n  <body>\n<h3>Help and Info</h3>\n    <p style=\"margin-top: 0\">\n       <b>This</b> is real help text.\n    </p>\n  </body>\n</html>\n");
        helpScroll.setViewportView(helpPane);

        jTabbedPane1.addTab("Help", helpScroll);

        searchField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFieldActionPerformed(evt);
            }
        });

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        examplesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                examplesListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(examplesList);

        jSplitPane1.setTopComponent(jScrollPane2);

        exampleDescription.setContentType("text/html"); // NOI18N
        jScrollPane4.setViewportView(exampleDescription);

        jSplitPane1.setRightComponent(jScrollPane4);

        jButton2.setText("Clone");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(searchField)
                        .addContainerGap())
                    .add(jSplitPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(0, 101, Short.MAX_VALUE)
                        .add(jButton2))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(searchField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton2))
        );

        jTabbedPane1.addTab("Examples", jPanel2);

        masterSplit.setRightComponent(jTabbedPane1);

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

        jLabel2.setText("Board");
        jToolBar1.add(jLabel2);

        boardDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        boardDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceChanged(evt);
            }
        });
        jToolBar1.add(boardDropdown);

        jLabel1.setText("Port");
        jToolBar1.add(jLabel1);

        portDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jToolBar1.add(portDropdown);

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

        recentSketchesMenu.setText("Recent Sketches");
        jMenu1.add(recentSketchesMenu);

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
            .add(masterSplit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(masterSplit))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void deviceChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceChanged
        actions.sketch.setCurrentDevice((Device)boardDropdown.getSelectedItem());
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
        SerialPort port = actions.sketch.getCurrentPort();
        if(port != null) {
            port.addListener(this);
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

    private void serialRateComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialRateComboActionPerformed
        actions.sketch.setSerialRate(Global.SERIAL_RATE_INTS[serialRateCombo.getSelectedIndex()]);
    }//GEN-LAST:event_serialRateComboActionPerformed

    private void searchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchFieldActionPerformed
        Util.p("Searching");
        final List<Example> examples = Global.getGlobal().findExamplesByText(searchField.getText());
        AbstractListModel<Example> model = new AbstractListModel<Example>() {
            @Override
            public int getSize() {
                return examples.size();
            }

            @Override
            public Example getElementAt(int index) {
                return examples.get(index);
            }
        };
        examplesList.setModel(model);
    }//GEN-LAST:event_searchFieldActionPerformed

    private void examplesListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_examplesListValueChanged
        if(examplesList.getSelectedValue() == null) return;
        Util.p("selection changed");
        Example ex = (Example) examplesList.getSelectedValue();
        StringBuffer desc = new StringBuffer();
        desc.append("<html><body>");
        desc.append("<h1>"+ex.name+"</h1>");
        desc.append("<p>"+ex.description+"</p>");
        desc.append("</body></html>");
        exampleDescription.setText(desc.toString());
    }//GEN-LAST:event_examplesListValueChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        serialConsole.setText("");
    }//GEN-LAST:event_jButton1ActionPerformed

    private void editorSplitPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_editorSplitPropertyChange
        if(evt.getPropertyName().equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) {
            if(consoleToggle.isSelected()) {
                editorSplitPosition = editorSplit.getDividerLocation();
            }
        }
    }//GEN-LAST:event_editorSplitPropertyChange

    private void masterSplitPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_masterSplitPropertyChange
        if(evt.getPropertyName().equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) {
            if(rightToggle.isSelected()) {
                masterSplitPosition = masterSplit.getDividerLocation();
            }
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_masterSplitPropertyChange


    private void updateTheme(ColorTheme theme, JEditorPane pane) {
        EditorKit editorKit = pane.getEditorKit();
        
        Color background = new Color(0xffffff);
        Color currentLine = new Color(0xefefef);
        Color selection = new Color(0xd6d6d6);
        Color foreground = new Color(0x4d4d4c);
        Color comment = new Color(0x8e908c);
        
        Color blue = new Color(0x4271ae);
        Color green = new Color(0x718c00);
        Color purple = new Color(0x8959a8);
        Color red = new Color(0xc82829);
        Color orange = new Color(0xf5871f);
        Color yellow = new Color(0xeab700);


        
        DefaultSyntaxKit kit = (DefaultSyntaxKit) editorKit;
        pane.setBackground(background);
        //0 = default style
        //2 = italic
        //1 = bold
        kit.setProperty("Style.COMMENT", toHex(comment,2));  //block and line comments
        kit.setProperty("Style.COMMENT2", toHex(Color.PINK,0)); //??
        kit.setProperty("Style.DEFAULT", toHex(foreground,0));
        kit.setProperty("Style.DELIMITER", toHex(Color.PINK,1));
        kit.setProperty("Style.ERROR", toHex(Color.PINK,3));
        kit.setProperty("Style.IDENTIFIER", toHex(blue,0)); //everything?
        kit.setProperty("Style.KEYWORD", toHex(purple,1)); // 
        kit.setProperty("Style.KEYWORD2", toHex(purple,1)); // #include
        kit.setProperty("Style.NUMBER", toHex(red,1)); //number literals
        kit.setProperty("Style.OPERATOR", toHex(foreground,0)); //plus, dot, comma, asterix, etc
        kit.setProperty("Style.REGEX", toHex(foreground,0));
        kit.setProperty("Style.STRING", toHex(green,0)); //string literals
        kit.setProperty("Style.STRING2", toHex(Color.PINK,0));
        kit.setProperty("Style.TYPE", toHex(green,0)); //void, int
        kit.setProperty("Style.TYPE2", toHex(Color.PINK,0));
        kit.setProperty("Style.TYPE3", toHex(Color.PINK,0));
        kit.setProperty("Style.WARNING", toHex(Color.PINK,0));
        
        kit.setProperty("CaretColor", toHex(red));
        kit.setProperty("SelectionColor", toHex(selection));
        kit.setProperty("PairMarker.Color",toHex(yellow));
        kit.setProperty("TokenMarker.Color",toHex(yellow));
        kit.setProperty("LineNumbers.Background",toHex(currentLine));
        kit.setProperty("LineNumbers.CurrentBack",toHex(selection));
        kit.setProperty("LineNumbers.Foreground",toHex(Color.BLACK));
        kit.setProperty("LineNumbers.RightMargin","7");
        
        /*
        Style.COMMENT 0x339933, 2
Style.COMMENT2 0x339933, 3
Style.DEFAULT 0x000000, 0
Style.DELIMITER 0x000000, 1
Style.ERROR 0xCC0000, 3
Style.IDENTIFIER 0x000000, 0
Style.KEYWORD 0x3333ee, 0
Style.KEYWORD2 0x3333ee, 3
Style.NUMBER 0x999933, 1
Style.OPERATOR 0x000000, 0
Style.REGEX 0xcc6600, 0
Style.STRING 0xcc6600, 0
Style.STRING2 0xcc6600, 1
Style.TYPE 0x000000, 2
Style.TYPE2 0x000000, 1
Style.TYPE3 0x000000, 3
Style.WARNING 0xCC0000, 0
*/
        editorKit.install(pane);
        pane.setFont(customFont.deriveFont(14f));
    }
    
    private String toHex(Color color, int i) {
        return "0x" + Integer.toHexString(color.getRGB()).substring(2)+", "+i;
    }
    private String toHex(Color color) {
        return "0x" + Integer.toHexString(color.getRGB()).substring(2);
    }
    
    

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
    private javax.swing.JComboBox boardDropdown;
    private javax.swing.JButton checkButton;
    private javax.swing.JMenuItem checkMenuItem;
    private javax.swing.JTextArea console;
    private javax.swing.JTabbedPane consoleTabPane;
    private javax.swing.JToggleButton consoleToggle;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JMenuItem cutItem;
    private javax.swing.JMenuItem darkThemeItem;
    private javax.swing.JSplitPane editorSplit;
    private javax.swing.JEditorPane exampleDescription;
    private javax.swing.JList examplesList;
    private javax.swing.JEditorPane helpPane;
    private javax.swing.JScrollPane helpScroll;
    private javax.swing.JMenuItem indentMenuItem;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem lightThemeItem;
    private javax.swing.JSplitPane masterSplit;
    private javax.swing.JMenuItem newSketchItem;
    private javax.swing.JMenuItem openSketchItem;
    private javax.swing.JMenuItem pasteItem;
    private javax.swing.JComboBox portDropdown;
    private javax.swing.JMenuItem quitMenu;
    private javax.swing.JMenu recentSketchesMenu;
    private javax.swing.JMenuItem redoItem;
    private javax.swing.JToggleButton rightToggle;
    private javax.swing.JButton runButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JTextField searchField;
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
