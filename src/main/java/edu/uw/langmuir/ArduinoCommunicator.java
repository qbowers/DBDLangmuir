/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uw.langmuir;


import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import javafx.application.Platform;
/**
 *
 * @author Quinn
 */
public class ArduinoCommunicator {
    static Thread commThread;
    static enum Command {
        LANGMUIRID("LangmuirBot"),
        UPDATE("UPDATE"),
        
        IDENTIFY("IDR"), //ID request
        //STOP("STP"),
        //CLOSE("CLS"),
        DELINEATOR("END"),
        REBOOT("REBOOT");
        //setposition, rotation

        //useful nonsense, you can safely ignore this
        String val;
        Command(String s) { val = s; }
        
        @Override
        public String toString() { return val; }
    }
    
    
    SerialPort langmuir;
    OutputStream langmuirOS;
    private double position; //perhaps needs to be atomic
    private double rotation; //concurrent things happening and all
    
    
    private void processSerialMessage(String message, SerialPort port) { //message excludes delineator
        //System.out.println(message + " from " + port.getDescriptivePortName());
        if (message.startsWith( Command.IDENTIFY.toString() )) registerArduino( port );
        else if ( port == langmuir) {
            if (message.startsWith( Command.UPDATE.toString() )) {
                int i;
                if ((i = message.indexOf("pos")) != -1) position = Float.parseFloat(message.substring(i+3));
                if ((i = message.indexOf("rot")) != -1) rotation = Float.parseFloat(message.substring(i+3));
                
                Platform.runLater(() -> { App.instance.refreshDisplay(); });
            }
        }
        
        
    }
    
    private void registerArduino( SerialPort port ) {
        langmuir = port;
        langmuirOS = port.getOutputStream();
        
        App.instance.setConnectionStatus("Connected on port " + langmuir.getSystemPortName());
    }
    private void disconnect() {
        langmuir = null;
        langmuirOS = null;
        
        App.instance.setConnectionStatus("DISCONNECTED");
    }
    
    
    
    
    
    
    private void findArduino() { //polls all the serial ports
        SerialPort[] ports = SerialPort.getCommPorts();
        for( SerialPort port:ports) {
            port.openPort();
            System.out.println(port.getDescriptivePortName());
            port.addDataListener(listener);
            
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) { }
            
            try {
                write(Command.IDENTIFY, port.getOutputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    private void closeAllPorts() { //polls all the serial ports
        SerialPort[] ports = SerialPort.getCommPorts();
        for( SerialPort port:ports) if (port.isOpen()) port.closePort();
    }
    
    
    private void write(Command c) throws IOException { write( c, langmuirOS); }
    private void write(String data) throws IOException { write( data, langmuirOS); }
    private void write(Command c, OutputStream os) throws IOException { write(c.toString(), os); }
    private void write(String data, OutputStream os) throws IOException {
        os.write( (data + Command.DELINEATOR).getBytes() );
    }
    SerialPortDataListener listener = new SerialPortDataListener() {
        @Override
        public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
        
        
        HashMap<SerialPort, String> bufferState = new HashMap<>();
        @Override
        public void serialEvent(SerialPortEvent e) {
            if (e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;
            
            
            SerialPort port = e.getSerialPort();
            byte[] data = new byte[port.bytesAvailable()];
            port.readBytes(data, data.length);

            //System.out.println("Read " + count + " bytes from arduino");
            String buffer = new String(data);
            if (bufferState.containsKey(port)) {
                buffer = bufferState.get(port) + buffer;
            }
            String[] split;
            while ((split = parse(buffer))[0] != null) { //loop breaks when there is NOT a complete message-delineator string
                processSerialMessage(split[0], port);
                buffer = split[1]; //the remainder of the buffer
            }

            bufferState.put(port, buffer);
        }
    };
    public static String[] parse(String s) {
        int location = s.indexOf(Command.DELINEATOR.toString());
        if (location == -1) return new String[]{null, s};
        
        return new String[]{
            s.substring(0, location),
            s.substring(location + Command.DELINEATOR.toString().length())
        };  
    }
    
    
    
    
    
    
    private Thread testerThread = new Thread(()-> {
        while( true ) {
            protect(()-> { write(Command.IDENTIFY);});
            try{
                Thread.sleep(5000);
            } catch (InterruptedException ex) {}
        }
    });
    public void startConnectionTester() {
        testerThread.start();
    }
    //public methods called by UI thread
    public void begin() {
        commThread = new Thread( () -> { //spin up new thread
            System.out.println("Thread spun");
            
            while(langmuir == null) {
                findArduino();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) { }
            }
        });
        commThread.start();
    }
    public double getPosition() { return position; }
    public double getRotation() { return rotation; }
    
    public void nudgePositionUp() {
        protect(()-> {write("NDGposup");});
    }
    public void nudgePositionDown() {
        protect(()-> {write("NDGposdown");});
    }
    public void setPosition(double x) {
        protect(()-> {write("SETpos"+x);});
    }
    
    public void nudgeRotationUp() {
        protect(()-> {write("NDGrotup");});
    }
    public void nudgeRotationDown() {
        protect(()-> {write("NDGrotdown");});
    }
    public void setRotation(double theta) {
        protect(()-> {write("SETrot"+theta);});
    }
    public void close() { //shutdown all the serial stuff
        //we dont really need protection, gonna die anyway
        reset();
        closeAllPorts();
        System.exit(0);
    }
    /*public void stop() { //doesnt work yet
        protect(()-> {write(Command.STOP);});
    }*/
    public void reset() {
        protect(()-> {write(Command.REBOOT);});
    }
    
    //handy util, screams if arduino missing
    class ArduinoDisconnectedException extends RuntimeException {}
    private void protect(Lambda lambda) {
        //if ( langmuir == null ) throw new ArduinoDisconnectedException();
        try {
            lambda.run();
        } catch( NullPointerException | IOException ex ) {
            disconnect();
            if (!commThread.isAlive()) begin(); //try to pick up the arduino again
        }
    }
}
