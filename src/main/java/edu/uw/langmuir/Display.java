/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uw.langmuir;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 *
 * @author Quinn
 */
public class Display extends VBox {
    TextField positionField;
    TextField rotationField;
    
    int canvasWidth = 600;
    int canvasHeight = 600;
    Canvas canvas = new Canvas(canvasWidth,canvasHeight);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    Color backgroundColor = Color.LIGHTGRAY;
    
    int vposition = canvasHeight-150; //vertical position of gantry
    int gantryLength = 550;
    int range = 400; //total range the physical gantry can travel
    
    public Display(TextField positionField, TextField rotationField) {
        this.positionField = positionField;
        this.rotationField = rotationField;
        
        
        positionField.setMinWidth(50);
        rotationField.setMinWidth(50);
        positionField.setMaxWidth(50);
        rotationField.setMaxWidth(50);
        
        
        getChildren().addAll(canvas);
        update(0,0);
        
        //debug
        setStyle("-fx-background-color: blue;");
    }
    
    public void update(double position, double rotation) {
        positionField.setText(Double.toString( position ));
        rotationField.setText(Double.toString( rotation ));
        
        render(position, rotation);
    }
    
    public void render(double position, double rotation) {
        clearCanvas();
        
        
        drawCarriage(gc);
        
        gc.save();
        
        
        applyPosition(gc, position);
        applyRotation(gc, rotation);
        drawSusan(gc);
        
        gc.restore();
        
        drawThruster(gc);
        
    }
    
    public void clearCanvas() {
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
    }
    public void drawCarriage(GraphicsContext gc) {
        int width = gantryLength;
        int height = 40;
        
        int thick = 5;
        
        gc.setFill(Color.BLACK);
        gc.fillRect((canvasWidth - width)/2, vposition - height/2, width, height);
        gc.setFill(backgroundColor);
        gc.fillRect((canvasWidth - width)/2 + thick, vposition + thick - height/2, width - thick*2, height - thick*2);
    }
    public void drawSusan(GraphicsContext gc) { //centered on local point 0,0
        //gc.translate(canvasWidth/2, vposition);
        
        drawCircle(gc, 0, 0, 200, Color.BLUE);
        drawCircle(gc, 0, 0, 185, backgroundColor);
        
        //langmuirs
        int r = 75;
        int size = 15;
        drawCircle(gc, r, 0, size, Color.BLACK);
        drawCircle(gc, -r, 0, size, Color.BLACK);
        drawCircle(gc, 0, r, size, Color.BLACK);
        drawCircle(gc, 0, -r, size, Color.BLACK);
    }
    public void drawCircle(GraphicsContext gc, int x, int y, int d, Color c) {
        int r = d/2;
        gc.setFill(c);
        gc.fillOval(x-r, y-r, d, d);
    }
    
    public void drawThruster(GraphicsContext gc) {
        int width = 50;
        int height = 200;
        
        //plasma
        gc.setFill(Color.ORANGE);
        gc.setGlobalAlpha(0.5);
        
        int out = 20;
        int overflow = 10;
        double angle = Math.PI / 7; //radians
        int spread = (int)((canvasHeight + overflow - height) * Math.tan(angle));
        
        double[] xpoints = {canvasWidth/2-out, canvasWidth/2+out, canvasWidth/2+spread, canvasWidth/2-spread};
        double[] ypoints = {height, height, canvasHeight + overflow, canvasHeight + overflow};
        
        gc.fillPolygon(xpoints, ypoints, xpoints.length);
        gc.setGlobalAlpha(1);
        
        //thruster
        gc.setFill(Color.BLACK);
        gc.fillRect((canvasWidth - width)/2, 0, width, height);
    }
    
    public void applyRotation(GraphicsContext gc, double rotation) {
        gc.rotate(rotation * 360 / (2*Math.PI));
    }
    
    public void applyPosition(GraphicsContext gc, double position) {
        gc.translate(position * gantryLength/range + canvasWidth/2,vposition);
    }
}
