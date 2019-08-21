/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uw.langmuir;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author Quinn
 */
public class Controls extends VBox {
    
    TextField positionField;
    TextField rotationField;
    Label connectionStatus;

    
    
    public Controls() { }
    public void init(TextField positionField, TextField rotationField) {
        setPadding(new Insets(20,20,20,20));
        setSpacing(20);
        
        this.positionField = positionField;
        this.rotationField = rotationField;
        
        //NudgeControls
        Button pDownButton = new Button("<");
        pDownButton.setOnMouseClicked(App.instance::nPosDown);
        Button pUpButton = new Button(">");
        pUpButton.setOnMouseClicked(App.instance::nPosUp);
        Label pnl = new Label("Position: ");
        pnl.setMinWidth(50);
        HBox pNudgeRow = new HBox(pnl, pDownButton, positionField, pUpButton);
        pNudgeRow.setSpacing(10);
        
        Button rDownButton = new Button("<");
        rDownButton.setOnMouseClicked(App.instance::nRotDown);
        Button rUpButton = new Button(">");
        rUpButton.setOnMouseClicked(App.instance::nRotUp);
        Label rnl = new Label("Rotation: ");
        rnl.setMinWidth(50);
        HBox rNudgeRow = new HBox( rnl, rDownButton, rotationField, rUpButton);
        rNudgeRow.setSpacing(10);
        
        
        //reset and set buttons
        Button setButton = new Button("Set");
        setButton.setOnMouseClicked(App.instance::controlUpdate);
        Button homeButton = new Button("Home");
        homeButton.setOnMouseClicked(App.instance::homeState);
        Button resetButton = new Button("Reset");
        homeButton.setOnMouseClicked(App.instance::reset);
        
        HBox buttons = new HBox(setButton, homeButton, resetButton);
        buttons.setSpacing(50);
        
        connectionStatus = new Label();
        
        getChildren().addAll(pNudgeRow, rNudgeRow, buttons, connectionStatus);
        
    }
    
    public float getPosition() { return parseFloat( positionField.getText() ); }
    public float getRotation() { return parseFloat( rotationField.getText() ); }
    
    public void setConnectionStatus(String status) { connectionStatus.setText(status); }
    
    public float parseFloat( String s ) {
        try {
            return Float.parseFloat(s);
        } catch( Exception e ) {
            return 0;
        }
    }
}
