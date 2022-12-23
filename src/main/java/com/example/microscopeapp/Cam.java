package com.example.microscopeapp;

import com.example.microscopeapp.Frames.Frames;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;

public class Cam extends Application
{
    private static final int FRAME_WIDTH  = 640;
    private static final int FRAME_HEIGHT = 480;
    private static final int OFFSET_X = 30;
    private static final int OFFSET_Y = 30;
    private static final int MENU_OFFSET = 60;
    private static final int BUTTON_NUMBER = 7;
    private static final double BUTTON_OFFSET = (FRAME_WIDTH + OFFSET_X*2 - MENU_OFFSET)/(1.0*BUTTON_NUMBER);
    private static final int BUFFER_SIZE = FRAME_WIDTH*FRAME_HEIGHT*Integer.BYTES;


    private boolean isDrawing = false;
    private double[] clickedXY = new double[2];
    private PowerButton[] powerButtons = new PowerButton[BUTTON_NUMBER];
    private byte[] bufferCurrent;
    private byte[] bufferPrevious;
    private static final byte[] bufferBlank = new byte[FRAME_WIDTH*FRAME_HEIGHT*Integer.BYTES];
    private byte[] bufferArea;

    GraphicsContext gc;
    Canvas canvas;
    byte[] buffer;
    PixelWriter pixelWriter;
    PixelFormat<ByteBuffer> pixelFormat;
    int image_width;
    int image_height;
    Stage mainStage;
    boolean[] toErase = new boolean[3];
    int[] toSub = new int[2];
    double markShapeBuffer = 0;
    double[] areaEdges = new double[4];
    double[][] areaBorderPoints;
    int areaI;
    byte[][] shapeCollection = new byte[10][];
    int[][][] shapeCollectionCoords = new int[10][][];
    int shapeCounter = 0;
    int shapeCurrent;

    Frames frames;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        mainStage = primaryStage;
        int result;

        Timeline timeline;

        //frames = new Frames();

        //result = Frames.open_shm("/frames");

        primaryStage.setTitle("Camera");
        Scene scene;

        //Group root = new Group();
        AnchorPane root = new AnchorPane();
        canvas     = new Canvas(FRAME_WIDTH + OFFSET_X*2, FRAME_HEIGHT + OFFSET_Y*2);
        gc         = canvas.getGraphicsContext2D();


        timeline = new Timeline(new KeyFrame(Duration.millis(130), e-> {
            try {
                disp_frame();
            } catch (FileNotFoundException ex) {

            }
        }));

        //timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setCycleCount(1);

        timeline.play();

        root.getChildren().add(canvas);

        //add draw button
        PowerButton pb1 = new PowerButton("");
        pb1.setChosenColor(Color.DEEPSKYBLUE);
        powerButtons[pb1.getId()] = pb1;
        ToggleButton bd = new ToggleButton("Draw");
        AnchorPane.setRightAnchor(bd, BUTTON_OFFSET*2);
        AnchorPane.setTopAnchor(bd, 2d);
        EventHandler<ActionEvent> eventDraw = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonDraw(pb1.getId());
            }
        };
        bd.setOnAction(eventDraw);

        //add change colour button
        PowerButton pb2 = new PowerButton("");
        powerButtons[pb2.getId()] = pb2;
        ToggleButton bcc = new ToggleButton("Change / remove colour");
        AnchorPane.setRightAnchor(bcc, BUTTON_OFFSET*3);
        AnchorPane.setTopAnchor(bcc, 2d);
        EventHandler<ActionEvent> eventDraw2 = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonColour(pb2.getId());
            }
        };
        bcc.setOnAction(eventDraw2);

        //add shape button
        PowerButton pb3 = new PowerButton("");
        pb3.setChosenColor(Color.YELLOW);
        powerButtons[pb3.getId()] = pb3;
        ToggleButton bs = new ToggleButton("Mark shape");
        AnchorPane.setRightAnchor(bs, BUTTON_OFFSET*4);
        AnchorPane.setTopAnchor(bs, 2d);
        EventHandler<ActionEvent> eventDraw3 = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonShape(pb3.getId());
            }
        };
        bs.setOnAction(eventDraw3);

        //add size/rotate button
        ToggleButton bsr = new ToggleButton("Change size / rotate");
        AnchorPane.setRightAnchor(bsr, BUTTON_OFFSET*5);
        AnchorPane.setTopAnchor(bsr, 2d);
        EventHandler<ActionEvent> eventDraw4 = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonSize();
            }
        };
        bsr.setOnAction(eventDraw4);
        PowerButton pb4 = new PowerButton("");
        powerButtons[pb4.getId()] = pb4;

        //add edit button
        ToggleButton be = new ToggleButton("Edit image");
        AnchorPane.setRightAnchor(be, BUTTON_OFFSET*6);
        AnchorPane.setTopAnchor(be, 2d);
        EventHandler<ActionEvent> eventDraw5 = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonEdit();
            }
        };
        be.setOnAction(eventDraw5);
        PowerButton pb5 = new PowerButton("");
        powerButtons[pb5.getId()] = pb5;

        //add properties button
        ToggleButton bp = new ToggleButton("Properties");
        AnchorPane.setRightAnchor(bp, BUTTON_OFFSET*7);
        AnchorPane.setTopAnchor(bp, 2d);
        EventHandler<ActionEvent> eventDraw6 = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonProperties();
            }
        };
        bp.setOnAction(eventDraw6);
        PowerButton pb6 = new PowerButton("");
        powerButtons[pb6.getId()] = pb6;

        //add undo button
        Button bu = new Button("Undo");
        AnchorPane.setRightAnchor(bu, BUTTON_OFFSET);
        AnchorPane.setTopAnchor(bu, 2d);
        EventHandler<ActionEvent> eventDraw7 = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                buttonUndo();
            }
        };
        bu.setOnAction(eventDraw7);
        PowerButton pb7 = new PowerButton("");
        powerButtons[pb7.getId()] = pb7;

        root.getChildren().add(bd);
        root.getChildren().add(bcc);
        root.getChildren().add(bs);
        root.getChildren().add(bsr);
        root.getChildren().add(be);
        root.getChildren().add(bp);
        root.getChildren().add(bu);

        scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void buttonDraw(int id)
    {
        EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (!powerButtons[id].isClicked())
                {
                    canvas.removeEventHandler(MouseEvent.ANY, this);
                }
                if (e.getEventType() == MouseEvent.MOUSE_PRESSED)
                {
                    clickedXY[0] = e.getX();
                    clickedXY[1] = e.getY();
                    isDrawing = true;
                }

                if (e.getEventType() == MouseEvent.MOUSE_RELEASED)
                {
                    isDrawing = false;
                    saveState();
                }

                if (e.getEventType() == MouseEvent.MOUSE_DRAGGED && isDrawing)
                {
                    gc.setStroke(powerButtons[id].getChosenColor());
                    gc.setLineWidth(2);
                    //check box borders
                    if (e.getX() < FRAME_WIDTH + OFFSET_X && e.getY() < FRAME_HEIGHT+OFFSET_Y &&
                    e.getX() > OFFSET_X && e.getY() > OFFSET_Y)
                    {
                        gc.strokeLine(clickedXY[0], clickedXY[1], e.getX(), e.getY());
                        clickedXY[0] = e.getX();
                        clickedXY[1] = e.getY();
                    }
                }
            }
        };

        if (powerButtons[id].isClicked())
        {
            //canvas.setOnMousePressed(this::nothing);
            powerButtons[id].setClicked(false);
            powerButtons[id].curtainCall();
        }
        else
        {
            //canvas.setOnMousePressed(this::mouse);
            //turn on drawing mode
            canvas.addEventHandler(MouseEvent.ANY, mouseHandler);
            powerButtons[id].setClicked(true);

            //create new dialogue window
            Text colorsText = new Text("Choose your colour:");

            ColorPicker cp = new ColorPicker(powerButtons[id].getChosenColor());
            cp.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    powerButtons[id].setChosenColor(cp.getValue());
                }
            });
            AnchorPane layout = new AnchorPane();
            AnchorPane.setTopAnchor(colorsText, 20.0);
            AnchorPane.setTopAnchor(cp, 50.0);
            AnchorPane.setLeftAnchor(colorsText, 5.0);
            AnchorPane.setLeftAnchor(cp, 5.0);
            layout.getChildren().addAll(colorsText, cp);


            powerButtons[id].optionsWindow(layout, mainStage);
        }
    }

    private void buttonColour(int id)
    {
        EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {

            }
        };

        if (powerButtons[id].isClicked())
        {
            //canvas.setOnMousePressed(this::nothing);
            powerButtons[id].setClicked(false);
            powerButtons[id].curtainCall();
        }
        else
        {
            //canvas.setOnMousePressed(this::mouse);
            //turn on drawing mode
            canvas.addEventHandler(MouseEvent.ANY, mouseHandler);
            powerButtons[id].setClicked(true);

            //create new dialogue window
            Text colorsText = new Text("Choose colour to be sacked:");

            toErase = new boolean[]{false, false, false};
            RadioButton rb1 = new RadioButton("Blue");
            RadioButton rb2 = new RadioButton("Green");
            RadioButton rb3 = new RadioButton("Red");

            rb1.addEventHandler(ActionEvent.ACTION, e -> {
                    if (rb1.isSelected())
                    {
                        toErase[0] = true;
                    }
                    else
                    {
                        toErase[0] = false;
                    }
                    eraseColourButton(bufferCurrent, toErase);
            });

            rb2.addEventHandler(ActionEvent.ACTION, e -> {
                if (rb2.isSelected())
                {
                    toErase[1] = true;
                }
                else
                {
                    toErase[1] = false;
                }
                eraseColourButton(bufferCurrent, toErase);
            });

            rb3.addEventHandler(ActionEvent.ACTION, e -> {
                if (rb3.isSelected())
                {
                    toErase[2] = true;
                }
                else
                {
                    toErase[2] = false;
                }
                eraseColourButton(bufferCurrent, toErase);
            });

            toSub[0]=-1;
            toSub[1]=-1;

            Text colorsText2 = new Text("Choose colour to substitute:");
            Text subtext1 = new Text("Delete:");
            Text subtext2 = new Text("Replace with:");
            RadioButton rb4 = new RadioButton("Blue");
            RadioButton rb5 = new RadioButton("Green");
            RadioButton rb6 = new RadioButton("Red");
            ToggleGroup tg1 = new ToggleGroup();
            rb4.setToggleGroup(tg1);
            rb4.setUserData(0);
            rb5.setToggleGroup(tg1);
            rb5.setUserData(1);
            rb6.setToggleGroup(tg1);
            rb6.setUserData(2);

            tg1.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue<? extends Toggle> observableValue, Toggle old_toggle, Toggle new_toggle) {
                    if (tg1.getSelectedToggle() != null)
                    {
                        toSub[0] = (int) tg1.getSelectedToggle().getUserData();
                    }
                }
            });

            RadioButton rb7 = new RadioButton("Blue");
            RadioButton rb8 = new RadioButton("Green");
            RadioButton rb9 = new RadioButton("Red");
            ToggleGroup tg2 = new ToggleGroup();

            rb7.setToggleGroup(tg2);
            rb7.setUserData(0);
            rb8.setToggleGroup(tg2);
            rb8.setUserData(1);
            rb9.setToggleGroup(tg2);
            rb9.setUserData(2);

            tg2.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue<? extends Toggle> observableValue, Toggle old_toggle, Toggle new_toggle) {
                    if (tg2.getSelectedToggle() != null)
                    {
                        toSub[1] = (int) tg2.getSelectedToggle().getUserData();
                    }
                }
            });

            Button replaceButton = new Button("Replace");

            EventHandler<ActionEvent> replaceColourHandler = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (toSub[0] == -1 || toSub[1] == -1)
                    {
                        return;
                    }
                    substituteColour(bufferCurrent, toSub);
                }
            };
            replaceButton.setOnAction(replaceColourHandler);


            AnchorPane layout = new AnchorPane();
            AnchorPane.setTopAnchor(colorsText, 20.0);
            AnchorPane.setTopAnchor(rb1, 40.0);
            AnchorPane.setTopAnchor(rb2, 60.0);
            AnchorPane.setTopAnchor(rb3, 80.0);
            AnchorPane.setTopAnchor(colorsText2, 105.0);
            AnchorPane.setTopAnchor(subtext1, 120.0);
            AnchorPane.setTopAnchor(rb4, 140.0);
            AnchorPane.setTopAnchor(rb5, 160.0);
            AnchorPane.setTopAnchor(rb6, 180.0);
            AnchorPane.setTopAnchor(subtext2, 200.0);
            AnchorPane.setTopAnchor(rb7, 220.0);
            AnchorPane.setTopAnchor(rb8, 240.0);
            AnchorPane.setTopAnchor(rb9, 260.0);
            AnchorPane.setTopAnchor(replaceButton, 290.0);

            AnchorPane.setLeftAnchor(colorsText, 5.0);
            AnchorPane.setLeftAnchor(rb1, 5.0);
            AnchorPane.setLeftAnchor(rb2, 5.0);
            AnchorPane.setLeftAnchor(rb3, 5.0);
            AnchorPane.setLeftAnchor(colorsText2, 5.0);
            AnchorPane.setLeftAnchor(subtext1, 10.0);
            AnchorPane.setLeftAnchor(rb4, 10.0);
            AnchorPane.setLeftAnchor(rb5, 10.0);
            AnchorPane.setLeftAnchor(rb6, 10.0);
            AnchorPane.setLeftAnchor(subtext2, 10.0);
            AnchorPane.setLeftAnchor(rb7, 10.0);
            AnchorPane.setLeftAnchor(rb8, 10.0);
            AnchorPane.setLeftAnchor(rb9, 10.0);
            AnchorPane.setLeftAnchor(replaceButton, 10.0);

            layout.getChildren().addAll(colorsText, rb1, rb2, rb3, colorsText2, subtext1, rb4,
                    rb5, rb6, subtext2, rb7, rb8, rb9, replaceButton);


            powerButtons[id].optionsWindow(layout, mainStage);
        }
    }

    private void buttonShape(int id)
    {
        EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (!powerButtons[id].isClicked())
                {
                    canvas.removeEventHandler(MouseEvent.ANY, this);
                }
                if (e.getEventType() == MouseEvent.MOUSE_PRESSED)
                {
                    areaBorderPoints = new double[2][BUFFER_SIZE];
                    areaI = 0;
                    areaBorderPoints[0][areaI] = e.getX();
                    areaBorderPoints[1][areaI] = e.getY();
                    areaI++;
                    clickedXY[0] = e.getX();
                    clickedXY[1] = e.getY();
                    isDrawing = true;
                    areaEdges[0] = e.getX();
                    areaEdges[1] = e.getY();
                }

                if (e.getEventType() == MouseEvent.MOUSE_RELEASED)
                {
                    isDrawing = false;
                    saveState();
                    areaEdges[2] = e.getX();
                    areaEdges[3] = e.getY();
                    //manhattan metric
                    if (Math.abs(areaEdges[2] - areaEdges[0]) + Math.abs(areaEdges[3] - areaEdges[1]) < 20)
                    {
                        gc.strokeLine(areaEdges[2], areaEdges[3], areaEdges[0], areaEdges[1]);


                        double difX = areaEdges[2] - areaEdges[0];
                        double difY = areaEdges[3] - areaEdges[1];
                        areaBorderPoints[0][areaI] = areaEdges[0] + 1 * difX / 3;
                        areaBorderPoints[1][areaI] = areaEdges[1] + 1 * difY / 3;

                        areaBorderPoints[0][areaI+1] = areaEdges[0] + 2 * difX / 3;
                        areaBorderPoints[1][areaI+1] = areaEdges[1] + 2 * difY / 3;

                        areaBorderPoints[0][areaI+2] = areaEdges[0] + 3 * difX / 3;
                        areaBorderPoints[1][areaI+2] = areaEdges[1] + 3 * difY / 3;
                        areaI += 3;
                        double[][] areaPointsToSend = new double[2][areaI];
                        System.arraycopy(areaBorderPoints[0], 0, areaPointsToSend[0], 0, areaI);
                        System.arraycopy(areaBorderPoints[1], 0, areaPointsToSend[1], 0, areaI);
                        cutTheShapeOut(areaPointsToSend, areaI);
                    }
                    else
                    {
                        buttonUndo();
                    }
                }

                if (e.getEventType() == MouseEvent.MOUSE_DRAGGED && isDrawing)
                {
                    gc.setStroke(powerButtons[id].getChosenColor());
                    if (markShapeBuffer > 16.0)
                    {
                        markShapeBuffer = 0;
                        if (powerButtons[id].getChosenColor() == Color.YELLOW)
                        {
                            powerButtons[id].setChosenColor(Color.BLACK);
                        }
                        else
                        {
                            powerButtons[id].setChosenColor(Color.YELLOW);
                        }
                    }
                    gc.setLineWidth(2);
                    //gc.setLineDashes(6);
                    //gc.setLineDashOffset(0);
                    if (e.getX() < FRAME_WIDTH + OFFSET_X && e.getY() < FRAME_HEIGHT+OFFSET_Y &&
                            e.getX() > OFFSET_X && e.getY() > OFFSET_Y)
                    {
                        gc.strokeLine(clickedXY[0], clickedXY[1], e.getX(), e.getY());
                        markShapeBuffer += (e.getX() - clickedXY[0]) * (e.getX() - clickedXY[0]) + (e.getY() - clickedXY[1]) * (e.getY() - clickedXY[1]);

                        double difX = e.getX() - clickedXY[0];
                        double difY = e.getY() - clickedXY[1];
                        int pointNumber = 20;

                        for (int i=0; i<pointNumber; i++)
                        {
                            areaBorderPoints[0][areaI] = clickedXY[0] + i * difX / pointNumber;
                            areaBorderPoints[1][areaI] = clickedXY[1] + i * difY / pointNumber;
                            areaI++;
                        }

                        clickedXY[0] = e.getX();
                        clickedXY[1] = e.getY();
                        areaBorderPoints[0][areaI] = e.getX();
                        areaBorderPoints[1][areaI] = e.getY();
                        areaI++;
                    }
                }
            }
        };

        if (powerButtons[id].isClicked())
        {
            //canvas.setOnMousePressed(this::nothing);
            powerButtons[id].setClicked(false);
            powerButtons[id].curtainCall();
        }
        else
        {
            //canvas.setOnMousePressed(this::mouse);
            //turn on drawing mode
            canvas.addEventHandler(MouseEvent.ANY, mouseHandler);
            powerButtons[id].setClicked(true);

            //create new dialogue window
            Text colorsText = new Text("Choose your colour:");

            ColorPicker cp = new ColorPicker(powerButtons[id].getChosenColor());
            cp.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    powerButtons[id].setChosenColor(cp.getValue());
                }
            });

            Button deleteShapeButton = new Button("Delete");

            EventHandler<ActionEvent> deleteShapeHandler = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (shapeCounter != 0)
                    {
                        deleteShape(shapeCounter-1);
                    }
                }
            };
            deleteShapeButton.setOnAction(deleteShapeHandler);

            AnchorPane layout = new AnchorPane();
            AnchorPane.setTopAnchor(colorsText, 20.0);
            AnchorPane.setTopAnchor(cp, 50.0);
            AnchorPane.setTopAnchor(deleteShapeButton, 100.0);
            AnchorPane.setLeftAnchor(colorsText, 5.0);
            AnchorPane.setLeftAnchor(cp, 5.0);
            AnchorPane.setLeftAnchor(deleteShapeButton, 5.0);
            layout.getChildren().addAll(colorsText, cp, deleteShapeButton);


            powerButtons[id].optionsWindow(layout, mainStage);
        }
    }

    private void buttonSize()
    {

    }

    private void buttonEdit()
    {

    }

    private void buttonProperties()
    {

    }

    private void buttonUndo()
    {
        refreshImage(bufferPrevious);
        saveState();
    }

    private void disp_frame() throws FileNotFoundException {

        pixelWriter = gc.getPixelWriter();
        pixelFormat = PixelFormat.getByteRgbInstance();
        FileInputStream inputStream = new FileInputStream("D:\\IntelliJ\\Projects\\Microscope\\src\\main\\resources\\frames\\Frame1.png");
        Image img = new Image(inputStream);
        PixelReader pixelReader = img.getPixelReader();
        image_width = (int)img.getWidth();
        image_height = (int)img.getHeight();

        //buffer = Frames.get_frame();
        buffer = new byte[image_width*image_height*Integer.BYTES];
        pixelReader.getPixels(0, 0, image_width, image_height, PixelFormat.getByteBgraInstance(), buffer, 0, image_width*4);
        //pixelWriter.setPixels(5, 5, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, buffer, 0, FRAME_WIDTH*3);

        refreshImage(buffer);
        bufferCurrent = copyBytes(buffer);
        saveState();

        int color_count[] = {0,0,0};
        color_count = color_stats(buffer);
        for (int i=0; i<3; i++) {
            System.out.println(color_count[i]);
        }
    }

    private byte[] eraseColour(byte[] buffer, int colour)
    {
        int COLOR_NUMBER = 4;
        byte[] buffer3 = new byte[image_width*image_height*Integer.BYTES];
        for (int i=0; i< buffer.length; i+=COLOR_NUMBER)
        {
            for (int j=0; j<COLOR_NUMBER; j++)
            {
                buffer3[i+j] = buffer[i+j];
            }
            buffer3[i+colour] = 0;
        }
        return buffer3;
    }

    private void eraseColourButton(byte[] buffer, boolean[] toErase)
    {
        byte[] buffer2 = copyBytes(buffer);

        for (int i=0; i<3; i++)
        {
            if (toErase[i])
            {
                buffer2 = eraseColour(buffer2, i);
            }
        }
        refreshImage(buffer2);
        saveState();
    }

    private int[] color_stats(byte[] buffer)
    {
        int COLOR_NUMBER = 4;
        int color_count[] = new int[3];
        for (int i=0; i< buffer.length; i+=COLOR_NUMBER)
        {
            if (buffer[i] > buffer[i+1])
            {
                if (buffer[i] > buffer[i+2])
                {
                    color_count[0]++;
                }
                else
                {
                    color_count[2]++;
                }
            }
            else
            {
                if (buffer[i+1] > buffer[i+2])
                {
                    color_count[1]++;
                }
                else
                {
                    color_count[2]++;
                }
            }
        }
        return color_count;
    }

    private void nothing(MouseEvent e)
    {

    }

    public static byte[] copyBytes(byte[] board) {
        byte[] boardCopy = new byte[board.length];
        for (int i = 0; i < board.length; i++) {
            boardCopy[i] = board[i];
        }
        return boardCopy;
    }

    public void substituteColour(byte[] buffer, int[] toSub)
    {
        int COLOR_NUMBER = 4;
        byte[] buffer2 = copyBytes(buffer);

        for (int i=0; i< buffer.length; i+=COLOR_NUMBER)
        {
            double sqrts1 = Math.sqrt(buffer[i]) + Math.sqrt(buffer[i+1]) + Math.sqrt(buffer[i+2]) - Math.sqrt(buffer[i+toSub[0]]);
            double sqrts2 = Math.sqrt(buffer[i+toSub[0]]);

            if (sqrts2 < sqrts1)
            {
                continue;
            }

            for(int j=0; j<3; j++)
            {
                if (j != toSub[1])
                {
                    buffer2[i+j] = 0;
                }
            }
        }

        refreshImage(buffer2);
        saveState();
    }

    public void cutTheShapeOut(double[][] areaBorderPoints, int areaI)
    {
        double[] extremes = new double[4]; // dół lewo góra prawo

        extremes[0] = Arrays.stream(areaBorderPoints[1]).max().getAsDouble() + 2;
        extremes[1] = Arrays.stream(areaBorderPoints[0]).min().getAsDouble() - 2;
        extremes[2] = Arrays.stream(areaBorderPoints[1]).min().getAsDouble() - 2;
        extremes[3] = Arrays.stream(areaBorderPoints[0]).max().getAsDouble() + 2;

        int img_width = (int) (extremes[3]) - (int)(extremes[1]);
        int img_height = (int) (extremes[0]) - (int)(extremes[2]);
        SnapshotParameters params = new SnapshotParameters();
        WritableImage snapshotImage = new WritableImage(FRAME_WIDTH + 2*OFFSET_X, FRAME_HEIGHT + 2*OFFSET_Y);
        WritableImage wi = canvas.snapshot(params, snapshotImage);

        bufferArea = new byte[img_width * img_height * Integer.BYTES];
        //bufferArea = new byte[BUFFER_SIZE];

        snapshotImage.getPixelReader().getPixels((int) extremes[1]+1, (int) extremes[2], img_width, img_height, PixelFormat.getByteBgraInstance(), bufferArea, 0, img_width*4);

        extremes[1] -= OFFSET_X;
        extremes[2] -= OFFSET_Y;

        byte[] shape = new byte[img_width * img_height * Integer.BYTES];   // b g r a
        int [][] shapeCoords = new int[2][img_width * img_height];
        int shapeNumber = 0;

        int[][] mask = new int[img_width+1][img_height+1];

        for (int i=0; i<areaI; i++)
        {
            mask[(int)(areaBorderPoints[0][i] - OFFSET_X-1) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y) - (int) extremes[2]] = 1;

            //left
            if ((int)(areaBorderPoints[0][i] - OFFSET_X-2) - ((int) extremes[1]) >= 0)
            {
                mask[(int)(areaBorderPoints[0][i] - OFFSET_X-2) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y) - (int) extremes[2]] = 1;
            }
            if ((int)(areaBorderPoints[0][i] - OFFSET_X-3) - ((int) extremes[1]) >= 0)
            {
                mask[(int)(areaBorderPoints[0][i] - OFFSET_X-3) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y) - (int) extremes[2]] = 1;
            }
            //right
            mask[(int)(areaBorderPoints[0][i] - OFFSET_X) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y) - (int) extremes[2]] = 1;
            if ((int)(areaBorderPoints[0][i] - OFFSET_X+1) - ((int) extremes[1]) < img_width+1)
            {
                mask[(int)(areaBorderPoints[0][i] - OFFSET_X+1) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y) - (int) extremes[2]] = 1;
            }

            //up
            if (((int)areaBorderPoints[1][i] - OFFSET_Y - 1) - (int) extremes[2] >= 0)
            {
                mask[(int)(areaBorderPoints[0][i] - OFFSET_X-1) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y - 1) - (int) extremes[2]] = 1;
            }
            if (((int)areaBorderPoints[1][i] - OFFSET_Y - 2) - (int) extremes[2] >= 0)
            {
                mask[(int)(areaBorderPoints[0][i] - OFFSET_X-1) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y - 2) - (int) extremes[2]] = 1;
            }
            //down
            mask[(int)(areaBorderPoints[0][i] - OFFSET_X-1) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y + 1) - (int) extremes[2]] = 1;
            if (((int)areaBorderPoints[1][i] - OFFSET_Y + 2) - (int) extremes[2] < img_height+1)
            {
                mask[(int)(areaBorderPoints[0][i] - OFFSET_X-1) - ((int) extremes[1])][((int)areaBorderPoints[1][i] - OFFSET_Y + 2) - (int) extremes[2]] = 1;
            }
        }

        //sufix sum
        int[][] sufixSums = new int[img_width+2][img_height+1];
        for (int j=img_height; j>=0; j--)
        {
            for (int i=img_width; i>0; i--)
            {
                sufixSums[i][j] = sufixSums[i+1][j] + mask[i][j];
            }
        }

        boolean isInside = false;
        for (int j=0; j<img_height; j++)
        {
            for (int i=0; i<img_width; i++)
            {
                int coord_x = (int) (extremes[1]+i);
                int coord_y = (int) (extremes[2]+j);
                if (mask[i][j] == 1)
                {
                    isInside = false;

                    if (sufixSums[i+1][j] != 0)
                    {
                        isInside = !isInside;
                    }
                    else
                    {
                        isInside = false;
                    }
                }
                else
                {
                    if (mask[i+1][j] == 1)
                    {
                        isInside = true;
                    }
                    else
                    {
                        if (i-1 >= 0 && mask[i-1][j] == 1)
                        {
                            shapeCoords[0][shapeNumber/4] = coord_x;
                            shapeCoords[1][shapeNumber/4] = coord_y;
                            shape[shapeNumber] = bufferArea[j*img_width*4 + i*4];
                            shape[shapeNumber + 1] = bufferArea[j*img_width*4 + i*4 + 1];
                            shape[shapeNumber + 2] = bufferArea[j*img_width*4 + i*4 + 2];
                            shape[shapeNumber + 3] = bufferArea[j*img_width*4 + i*4 + 3];
                            shapeNumber+=4;
                        }
                    }
                }

                if (isInside)
                {
                    shapeCoords[0][shapeNumber/4] = coord_x;
                    shapeCoords[1][shapeNumber/4] = coord_y;
                    shape[shapeNumber] = bufferArea[j*img_width*4 + i*4];
                    shape[shapeNumber + 1] = bufferArea[j*img_width*4 + i*4 + 1];
                    shape[shapeNumber + 2] = bufferArea[j*img_width*4 + i*4 + 2];
                    shape[shapeNumber + 3] = bufferArea[j*img_width*4 + i*4 + 3];
                    shapeNumber+=4;
                }
            }
            isInside = false;
        }

        shapeCollection[shapeCounter] = shape;
        shapeCollectionCoords[shapeCounter] = shapeCoords;
        shapeCounter++;

    /*
        for (int i=0; i<shape.length; i+=4)
        {
            shape[i+3] = 127;
        }
    */

        bufferPrevious = copyBytes(bufferCurrent);
        updateBufferWithShape(shapeCounter-1);
        //updateBufferCurrent(bufferArea, img_width, img_height, (int)extremes[1], (int)extremes[2]);

        refreshImage(bufferCurrent);
    }

    private void deleteShape(int index)
    {
        shapeCounter--;
        byte[] shape = shapeCollection[index];
        for (int i=0; i<shape.length; i+=4)
        {
            shape[i] = 0;
            shape[i+1] = 0;
            shape[i+2] = 0;
            shape[i+3] = -1;
        }
        updateBufferWithShape(index);
        refreshImage(bufferCurrent);
        saveState();
    }

    private void updateBufferWithShape(int index)
    {
        byte[] shape = shapeCollection[index];
        int[][] shapeCoords = shapeCollectionCoords[index];

        for (int i=0; i<shape.length; i+=4)
        {
            bufferCurrent[ (shapeCoords[1][i/4])*FRAME_WIDTH*4 + (shapeCoords[0][i/4]+1)*4] = shape[i];
            bufferCurrent[ (shapeCoords[1][i/4])*FRAME_WIDTH*4 + (shapeCoords[0][i/4]+1)*4 + 1] = shape[i + 1];
            bufferCurrent[ (shapeCoords[1][i/4])*FRAME_WIDTH*4 + (shapeCoords[0][i/4]+1)*4 + 2] = shape[i + 2];
            bufferCurrent[ (shapeCoords[1][i/4])*FRAME_WIDTH*4 + (shapeCoords[0][i/4]+1)*4 + 3] = shape[i + 3];
        }
    }

    private void updateBufferCurrent(byte[] bufferArea, int img_width, int img_height, int extremesX, int extremesY)
    {
        // WIDTH * HEIGHT * 4
        for (int j=0; j<img_height; j++)
        {
            for(int i = 0; i<img_width; i++)
            {
                bufferCurrent[ (extremesY + j)*FRAME_WIDTH*4 + (extremesX+1+i)*4] = bufferArea[j*img_width*4 + i*4];
                bufferCurrent[ (extremesY + j)*FRAME_WIDTH*4 + (extremesX+1+i)*4 +1] = bufferArea[j*img_width*4 + i*4 + 1];
                bufferCurrent[ (extremesY + j)*FRAME_WIDTH*4 + (extremesX+1+i)*4 +2] = bufferArea[j*img_width*4 + i*4 + 2];
                bufferCurrent[ (extremesY + j)*FRAME_WIDTH*4 + (extremesX+1+i)*4 +3] = bufferArea[j*img_width*4 + i*4 + 3];
            }
        }
        System.out.println("Hello");
    }

    public void saveState()
    {
        SnapshotParameters params = new SnapshotParameters();
        WritableImage snapshotImage = new WritableImage(FRAME_WIDTH + 2*OFFSET_X, FRAME_HEIGHT + 2*OFFSET_Y);
        WritableImage wi = canvas.snapshot(params, snapshotImage);

        //save previous buffer
        bufferPrevious = copyBytes(bufferCurrent);

        snapshotImage.getPixelReader().getPixels(OFFSET_X, OFFSET_Y, FRAME_WIDTH, FRAME_HEIGHT, PixelFormat.getByteBgraInstance(), bufferCurrent, 0, FRAME_WIDTH*4);
        //File temp = new File("temp.png");


        //PixelReader pixelReader = img.getPixelReader();
        //int image_width2 = FRAME_WIDTH;
        //int image_height2 = FRAME_HEIGHT;

        //pixelReader.getPixels(0, 0, image_width2, image_height2, PixelFormat.getByteBgraInstance(), bufferCurrent, 0, image_width2*4);
        //bufferCurrent = copyBytes(buffer);
    }

    private void refreshImage(byte[] buffer)
    {
        //pixelWriter.setPixels(OFFSET_X, OFFSET_Y, image_width, image_height, PixelFormat.getByteBgraInstance(), bufferBlank, 0, image_width*4);
        pixelWriter.setPixels(OFFSET_X, OFFSET_Y, image_width, image_height, PixelFormat.getByteBgraInstance(), buffer, 0, image_width*4);
    }

}