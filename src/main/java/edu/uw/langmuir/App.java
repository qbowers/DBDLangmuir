package edu.uw.langmuir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {
    public static App instance;
    ArduinoCommunicator comm = new ArduinoCommunicator();
    
    
    //UI elements
    TextField positionField = new TextField();
    TextField rotationField = new TextField();
    
    Controls controls = new Controls();
    Display display = new Display(positionField, rotationField);
    
    
    @Override
    public void start(Stage stage) {
        instance = this;
        controls.init(positionField, rotationField);
        var scene = new Scene( new HBox(display, controls));
        stage.setScene(scene);
        stage.show();
        
        comm.begin();
        comm.startConnectionTester();
    }
    
    public void setConnectionStatus(String status) {
        Platform.runLater( () -> { controls.setConnectionStatus(status);});
    }
    
    
    public void controlUpdate(Event e) {
        comm.setPosition( controls.getPosition() );
        comm.setRotation( controls.getRotation() );
    }
    public void homeState(Event e) {
        comm.setPosition(0);
        comm.setRotation(0);
    }
    public void reset(Event e) {
        comm.reset();
    }
    
    public void nPosUp(Event e) { comm.nudgePositionUp(); }
    public void nPosDown(Event e) { comm.nudgePositionDown(); }
    public void nRotUp(Event e) { comm.nudgeRotationUp(); }
    public void nRotDown(Event e) { comm.nudgeRotationDown(); }
    
    public void refreshDisplay() {
        display.update(comm.getPosition(), comm.getRotation());
    }
    
    
    @Override
    public void stop() {
        homeState(new Event(EventType.ROOT)); //ugly but whatever
        comm.close();
    }
    
    
    public static void main(String[] args) { launch(); }
}