package com.example.microscopeapp;

import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PowerButton {
    private static final int FRAME_WIDTH  = 640;
    private static final int FRAME_HEIGHT = 480;
    private static final int OFFSET_X = 30;
    private static final int OFFSET_Y = 30;

    static int buttonNumber = 0;
    int id;
    String iconPath;
    boolean isClicked;
    Color chosenColor = Color.WHITE;
    Stage optionsStage;

    public PowerButton(String imagePath)
    {
        id = buttonNumber;
        buttonNumber++;
        iconPath = imagePath;
        isClicked = false;
    }

    public int getId() {
        return id;
    }

    public boolean isClicked() {
        return isClicked;
    }

    public void setClicked(boolean clicked) {
        isClicked = clicked;
    }

    public void optionsWindow(AnchorPane layout, Stage mainStage)
    {
        Scene scene = new Scene(layout, 250, FRAME_HEIGHT+2*OFFSET_Y);

        optionsStage = new Stage();
        optionsStage.setTitle("Options");
        optionsStage.setScene(scene);

        optionsStage.setX(mainStage.getX() + (FRAME_WIDTH + OFFSET_X*2) + 10);
        optionsStage.setY(mainStage.getY() + 0);

        optionsStage.show();
    }

    public void setChosenColor(Color chosenColor) {
        this.chosenColor = chosenColor;
    }

    public Color getChosenColor()
    {
        return chosenColor;
    }

    public void curtainCall()
    {
        optionsStage.close();
    }
}
