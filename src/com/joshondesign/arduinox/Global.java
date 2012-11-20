package com.joshondesign.arduinox;

import com.joshondesign.arduino.common.Device;
import com.joshondesign.arduino.common.Util;
import com.joshondesign.xml.Doc;
import com.joshondesign.xml.Elem;
import com.joshondesign.xml.XMLParser;
import gnu.io.CommPortIdentifier;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author josh
 */
public class Global {
    private static Global _global;
    private List<Sketch> sketches = new ArrayList<>();
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private Map<Sketch,EditorWindow> windows = new HashMap<>();
    private final List<SerialPort> ports;
    private final List<Device> devices;
    private Device extracore;
    private File toolchainDir;
    private static final String TOOLCHAIN_DIR = "TOOLCHAIN_DIR";
    private static final String RECENT_SKETCHES = "RECENT_SKETCHES";
    private static final String OPEN_SKETCHES = "OPEN_SKETCHES";
    private static final String COMPILER_COMMANDS_SHOWN = "COMPILER_COMMANDS_SHOWN";
    
    public static void main(String ... args) throws IOException {
        Properties props = new Properties();
        props.setProperty("asdf", "asdfasdf");
        Util.p("===");
        for(Entry e : props.entrySet()) {
            Util.p("key = " + e.getKey() + " " + e.getValue() );
        }
        props.storeToXML(new FileOutputStream("foo.xml"), "foo");
        props.clear();
        props = new Properties();
        Util.p("===");
        for(Entry e : props.entrySet()) {
            Util.p("key = " + e.getKey() + " " + e.getValue() );
        }
        props.loadFromXML(new FileInputStream("foo.xml"));
        Util.p("===");
        for(Entry e : props.entrySet()) {
            Util.p("key = " + e.getKey() + " " + e.getValue() );
        }
    }
    
    public static final int SERIAL_RATE_INTS[] = {
        300,1200,2400,4800,9600,14400,
        19200,28800,38400,57600,115200
    };
    
    public static final String SERIAL_RATE_STRINGS[] = {
        "300","1200","2400","4800","9600","14400",
        "19200","28800","38400","57600","115200"
    };
    private List<String> recentSketches;
    private Set<String> recentUniqueSketches = new HashSet<>();
    private final List<Example> examples;
    private boolean compilerCommandsShown;


    private Global() {
        setupToolchainPath();
        recentSketches = new ArrayList<>();
        this.ports = scanForSerialPorts();
        this.devices = scanForDevices();
        this.examples = scanForExamples();
    }
    private void init() {
        loadSettings();
    }
    
    

    static Global getGlobal() {
        if(_global == null) {
            _global = new Global();
            _global.init();
        }
        return _global;
    }

    void addSketch(Sketch sketch) {
        this.sketches.add(sketch);
        String path = sketch.getDirectory().getAbsolutePath();
        if(!recentUniqueSketches.contains(path)) {
            this.recentSketches.add(path);
            this.recentUniqueSketches.add(path);
        }
        pcs.firePropertyChange("sketches", sketches, sketch);
    }
    
    void removeSketch(Sketch sketch) {
        this.sketches.remove(sketch);
        pcs.firePropertyChange("sketches", sketches, null);
    }

    Iterable<Sketch> getSketches() {
        return sketches;
    }

    EditorWindow getWindowForSketch(Sketch sketch) {
        return windows.get(sketch);
    }

    void setWindowForSketch(Sketch sketch, EditorWindow frame) {
        this.windows.put(sketch,frame);
    }

    void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(property,listener);
    }
    
    private List<SerialPort> scanForSerialPorts() {
        
        List<SerialPort> ports = new ArrayList<>();
        try {
        
        //get all ports
        for (Enumeration enumeration = CommPortIdentifier.getPortIdentifiers(); enumeration.hasMoreElements();) {
            CommPortIdentifier port = (CommPortIdentifier) enumeration.nextElement();
            SerialPort pt = new SerialPort();
            pt.portName = port.getName();
            pt.shortName = pt.portName;
            ports.add(pt);
        }
        
        //filter out dupes
        if(Util.isMacOSX()) {
            Map<String,SerialPort> map = new HashMap<>();
            for(SerialPort port : ports) {
                String shortName = port.portName;
                if(port.portName.startsWith("/dev/tty")) {
                    shortName = port.portName.replaceFirst("/dev/tty.", "");
                }
                if(port.portName.startsWith("/dev/cu")) {
                    shortName = port.portName.replaceFirst("/dev/cu.", "");
                }
                port.shortName = shortName;
                map.put(shortName,port);
            }
            
            ports.clear();
            ports.addAll(map.values());
        }
        
        //remove manually blocked items (such as bluetooth)
        Iterator<SerialPort> it = ports.iterator();
        while(it.hasNext()) {
            SerialPort port = it.next();
            if(port.shortName.toLowerCase().contains("bluetooth")) it.remove();
        }
        
        for(SerialPort port : ports) {
            Util.p("final port = " + port.portName + " short = " + port.shortName);
        }
        
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        //sort by name
        //if only one, use it.
        return ports;
    }

    List<SerialPort> getPorts() {
        return this.ports;
    }

    SerialPort getPortForPath(String portName) {
        for(SerialPort port : this.ports) {
            if(port.portName.equals(portName)) {
                return port;
            }
        }
        return null;
    }

    private List<Example> scanForExamples() {
        List<Example> examples = new ArrayList<>();
        try {
            File exdir = new File(getResourcesDir(),"examples/");
            Util.p("basedir = " + exdir.getCanonicalPath());
            scanForExamples(exdir,examples);
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return examples;
    }
    
    
    private List<Device> scanForDevices() {
        List<Device> devices  = new ArrayList<>();
        try {
            File basedir = getResourcesDir();
            if(basedir == null) {
                Util.p("resources dir is null!");
                return devices;
            }
            Util.p("resources dir = " + basedir.getCanonicalPath());
            if(!basedir.exists()) {
                Util.p("resources dir doesn't exist!");
                return devices;
            }
            for(File xml : new File(basedir,"hardware/boards/").listFiles()) {
                Util.p("parsing: " + xml.getCanonicalPath());
                try {
                    Doc doc = XMLParser.parse(xml);
                    Elem e = doc.xpathElement("/board");
                    Device d = new Device();
                    d.name = e.attr("name");
                    d.protocol = e.attr("protocol");
                    d.maximum_size = Integer.parseInt(e.attr("maximum-size"));
                    d.upload_speed = Integer.parseInt(e.attr("upload-speed"));
                    d.low_fuses = parseHex(e.attr("low-fuses"));
                    d.high_fuses = parseHex(e.attr("high-fuses"));
                    d.extended_fuses = parseHex(e.attr("extended-fuses"));
                    d.path = e.attr("path");
                    d.file = e.attr("file");
                    d.unlock_bits = parseHex(e.attr("unlock-bits"));
                    d.lock_bits = parseHex(e.attr("lock-bits"));
                    d.mcu = e.attr("mcu");
                    d.f_cpu = e.attr("f-cpu");
                    d.core = e.attr("core");
                    d.variant = e.attr("variant");
                    devices.add(d);
                } catch (Exception ex) {
                    Logger.getLogger(Global.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Throwable ex2) {
            ex2.printStackTrace();
        }
        
        return devices;
    }
    
    List<Device> getDevices() {
        return this.devices;
    }
    
    File getToolchainDir() {
        return toolchainDir;
    }

    void setToolchainDir(File arduinoPath) {
        this.toolchainDir = arduinoPath;
    }
    
    File getDocumentsDir() {
        return new File(System.getProperty("user.home"),"Documents/Arduino");
    }

    public void saveSettings() {
        try {
            Util.p("========= saving settings");
            Properties props = new Properties();
            
            StringBuffer sb2 = new StringBuffer();
            for(Sketch sketch : getSketches()) {
                sb2.append(sketch.getDirectory().getAbsolutePath());
                sb2.append(",");
            }
            props.setProperty(OPEN_SKETCHES, sb2.toString());
            
            
            StringBuffer sb = new StringBuffer();
            for(String s : recentSketches) {
                sb.append(s);
                sb.append(",");
            }
            props.setProperty(RECENT_SKETCHES, sb.toString());
            props.setProperty(COMPILER_COMMANDS_SHOWN, Boolean.toString(isCompilerCommandsShown()));
            
            
            props.store(new FileOutputStream("settings.props"), "foo");            
            props.storeToXML(new FileOutputStream("settings.xml"), "ArduinoX Settings", "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadSettings() {
        try {
            Properties props = new Properties();
            props.loadFromXML(new FileInputStream("settings.xml"));
            Util.p("Loaded settings: " + props.keySet().size());

            recentUniqueSketches.clear();
            if(props.containsKey(RECENT_SKETCHES)) {
                String[] s = props.getProperty(RECENT_SKETCHES).split(",");
                for(String ss : s) {
                    //first, skip the dupes
                    if(recentUniqueSketches.contains(ss)) continue;
                    
                    recentUniqueSketches.add(ss);
                    recentSketches.add(ss);
                }
            }
            
            Set<String> os = new HashSet<>();
            if(props.containsKey(OPEN_SKETCHES)) {
                String[] s = props.getProperty(OPEN_SKETCHES).split(",");
                for(String ss : s) {
                    if(!os.contains(ss)) {
                        os.add(ss);
                        Actions.openNewSketch(new File(ss));
                    }
                }
            }
            
            if(props.containsKey(COMPILER_COMMANDS_SHOWN)) {
                compilerCommandsShown = Boolean.parseBoolean(props.getProperty(COMPILER_COMMANDS_SHOWN, "true"));
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    Device getDeviceForName(String deviceName) {
        for(Device d : devices) {
            if(d.getName().equals(deviceName)) return d;
        }
        return null;
    }

    Iterable<File> getRecentSketches() {
        List<File> files = new ArrayList<>();
        for(String skdir : recentSketches) {
            files.add(new File(skdir));
        }
        return files;
    }

    private int parseHex(String attr) {
        if(attr == null || attr.trim().equals("")) {
            throw new IllegalArgumentException("The value '" + attr + "' is not a valid hex number");
        }
        if(attr.toLowerCase().startsWith("0x")) {
            attr = attr.substring(2);
        }
        return Integer.parseInt(attr,16);
    }

    private void scanForExamples(File basedir, List<Example> examples) {
        for(File file : basedir.listFiles()) {
            if(file.getName().toLowerCase().equals("example.xml")) {
                examples.add(parseExample(file));
            }
            if(file.isDirectory()) {
                scanForExamples(file,examples);
            }
        }
    }

    private Example parseExample(File file) {
        try {
            Doc doc = XMLParser.parse(file);
            Elem e = doc.xpathElement("/example");
            Example ex = new Example();
            ex.name = e.attr("name");
            for(Elem k : e.xpath("keyword")) {
                ex.keywords.add(k.text().toLowerCase());
                //Util.p("added keyword: " + k.text().toLowerCase());
            }
            ex.description = e.xpathString("description/text()");
            ex.directory = file.getParentFile();
            //Util.p("parsed example: " + ex.name);
            return ex;
        } catch (Exception ex) {
            Logger.getLogger(Global.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    List<Example> findExamplesByText(String text) {
        List<Example> results = new ArrayList<>();
        for(Example ex : examples) {
            if(ex.name.toLowerCase().contains(text.toLowerCase())) {
                results.add(ex);
                continue;
            }
            if(ex.keywords.contains(text.toLowerCase())) {
                results.add(ex);
                continue;
            }
        }
        return results;
    }

    List<Example> getExamples() {
        return this.examples;
    }

    private File getResourcesDir() {
        String resourcesprop = System.getProperty("com.joshondesign.arduinox.resourcespath");
        if(resourcesprop != null) {
            return new File(resourcesprop);
        }
        return new File(toolchainDir,"resources");
    }

    int getOpenSketchCount() {
        return this.sketches.size();
    }


    private void p(Properties props) {
        for(Entry e : props.entrySet()) {
            Util.p("key = " + e.getKey() + " value = " + e.getValue());
        }
    }

    boolean isCompilerCommandsShown() {
        return compilerCommandsShown;
    }

    void setCompilerCommandsShown(boolean selected) {
        this.compilerCommandsShown = selected;
    }

    private void setupToolchainPath() {
        Util.p("the toolchain path = " + System.getProperty("com.joshondesign.arduinox.toolchainpath"));
        String toolchainPath = System.getProperty("com.joshondesign.arduinox.toolchainpath");
        if("uselibrary".equals(toolchainPath)) {
            String librarypath = System.getProperty("java.library.path");
            File contentsDir = new File(librarypath).getParentFile();
            toolchainPath = new File(contentsDir,"toolchain").getAbsolutePath();
        }
        if(toolchainPath == null) {
            toolchainPath = System.getProperty("user.dir");
        }
        //Util.p("final toochain path = " + toolchainPath);
        this.setToolchainDir(new File(toolchainPath));
    }

}
