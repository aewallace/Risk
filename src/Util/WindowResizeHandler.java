package Util;

import Master.FXUIGameMaster;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Handles target window resizing duties for a given JavaFX Stage object. The
 * Stage object should have been populated and put up for display BEFORE making
 * use of this class and its functions. (i.e., stage.isShowing() should return
 * true at this point, and stage.getScene() should return a valid Scene object.)
 * 18 October 2015, 18:45
 */
public class WindowResizeHandler {
    /**Controls whether or not diagnostic information is printed through the
     * associated {@link #diagnosticPrintln(string)} method. True: print
     * diagnostic info. False: suppress display of diagnostic info.
     * (Every call to diagnosticPrintln checks this, currently.) */
	private static final boolean diagnosticModeEnabled = false;
	
	/*Remaining convenient class-level variables.*/
    private static final int TRIG_BY_WIDTH = 4, TRIG_BY_HEIGHT = 8;
    private Stage activeStage = null;
    private Scene activeScene = null;
    private double desiredAspectRatio = 0.0;
    private double windowDecorationWidth = 0.0, windowDecorationHeight = 0.0,
            activeScreenWidth, activeScreenHeight;
    private ChangeListener<Number> widthPropertyListener = null;
    private ChangeListener<Number> heightPropertyListener = null;
    private AtomicBoolean resizeBlocked = new AtomicBoolean(true);
    //private AtomicBoolean stillRunningResize = new AtomicBoolean(false);
    private AtomicBoolean resizeThreadIsActive = new AtomicBoolean(false);
    private double widthHistory = 0.0, heightHistory = 0.0;
    private double idealContentWidth = 0, idealContentHeight = 0;
    private boolean assumeCallerIsActive = false;
	//attach a slider object to receive feedback from resize operations (percentages only)
    // also dynamically enables/disables during resize operations
    private Slider sliderToAdjust = null;

    /**
     * Class constructor which accepts fewer parameters. Creates an object
     * responsible for intelligent resizing of a single linked window (Stage).
     * Assumes no window decorations are present. (If window decorations are
     * present, please use the alternate constructor). (In other words,
     * "windowDecorationWidth" & "windowDecorationHeight" parameter of
     * {@link #ResizeHandler(Stage, double, double, double, double, double)} is
     * set to 0).
     *
     * @param desiredStage the stage which may or may not require resizing
     * @param aspectRatio target ratio of the content of the window (not
     * necessarily of the window itself!)
     * @param contentWidth ideal width of the content (Scene) of the window
     * (Stage)
     */
    public WindowResizeHandler(Stage desiredStage, double aspectRatio, double contentWidth, double contentHeight) {
        diagnosticPrintln("ResizeHandler constructor A");
        constructorHelper(desiredStage, aspectRatio, 0.0, 0.0, contentWidth, contentHeight);
    }

    /**
     * Class constructor which accepts more parameters. Creates an object
     * responsible for intelligent resizing of a single linked window (Stage),
     * taking window decorations into account. Compared to
     * {@link #ResizeHandler(Stage, double)}, allows you to inform the object of
     * the width and height of the window decorations. (The height of the window
     * decorations makes the height of a window/Stage technically be greater
     * than the height of the window contents/Scene.) (The same applies to the
     * width of the window decorations, if there is a detectable width.)
     * (Without this information being ascertained correctly, may result in
     * resizing errors!)
     *
     * @param desiredStage the stage which may or may not require resizing.
     * Contains the Scene which is treated as the "window contents".
     * @param aspectRatio target ratio of the content of the window (not
     * necessarily of the window itself!)
     * @param windowDecorationHeight the amount of space taken up vertically by
     * the OS's window decoration (window title, minimize/maximize/close
     * buttons, etc)
     * @param contentWidth the ideal default width of the content (Scene),
     * assuming unlimited screen real estate.
     * @param contentHeight the ideal default height of the content (Scene),
     * assuming unlimited screen real estate.
     */
    public WindowResizeHandler(Stage desiredStage, double aspectRatio, double windowDecorationWidth, double windowDecorationHeight, double contentWidth, double contentHeight) {
        diagnosticPrintln("ResizeHandler constructor B");
        constructorHelper(desiredStage, aspectRatio, windowDecorationWidth, windowDecorationHeight, contentWidth, contentHeight);
    }

    /**
     * Allows for multiple forms of the class constructor without having to
     * repeat code.
     *
     * @param desiredStage [CHECKED: must not be null] the stage which may or
     * may not require resizing
     * @param aspectRatio [CHECKED: must be > 0] target ratio of the content of
     * the window (not necessarily of the window itself!)
     * @param windowDecorationWidth [CHECKED: must be > 0] the amount of space
     * taken up horizontally by the OS's window decoration (window title,
     * minimize/maximize/close buttons, borders, etc)
     * @param windowDecorationHeight [CHECKED: must be > 0] the amount of space
     * taken up vertically by the OS's window decoration (window title,
     * minimize/maximize/close buttons, borders, etc)
     * @param contentWidth [UNCHECKED (will be auto-set)] the ideal default
     * width of the content (Scene), assuming unlimited screen real estate. If
     * an invalid value (below 1) is supplied, the ideal size will be determined
     * using the primary screen & the aspect ratio. 
     * @param contentHeight [UNCHECKED (will be auto-set)] the ideal default 
     * height of the content (Scene), assuming unlimited screen real estate. If 
     * an invalid value (below 1) is supplied, the ideal size will be determined 
     * using the primary screen & the aspect ratio.
     */
    private void constructorHelper(Stage desiredStage, double aspectRatio, double windowDecorationWidth, double windowDecorationHeight, double contentWidth, double contentHeight) {
        verifyIsFXThread();
        if (desiredStage == null || aspectRatio <= 0) {
            throw new UnsupportedOperationException("Select parameters detected as invalid. Status of potentially invalid parameters: desiredStage = "
                    + desiredStage == null ? "null (invalid)" : "not null (valid)" + " aspectRatio = " + aspectRatio + "(should be > 0 to be valid)");
        } else if (!desiredStage.isShowing()) {
            throw new UnsupportedOperationException("Requires a technically visible Stage object. Aborting creation.");
        } else if (windowDecorationWidth < 0 || windowDecorationHeight < 0) {
            throw new UnsupportedOperationException("Window decoration dimension parameters detected as potentially invalid. Status of parameters: windowDecorationWidth = "
                    + windowDecorationWidth + (windowDecorationWidth < 0 ? "(invalid)" : "(valid)")
                    + " && windowDecorationHeight = "
                    + windowDecorationHeight + (windowDecorationHeight < 0 ? "(invalid)" : "(valid)"));
        } else {
            this.activeStage = desiredStage;
            if ((this.activeScene = desiredStage.getScene()) == null) {
                throw new UnsupportedOperationException("Couldn't get scene from stage (attempt to setup resize capability failed).");
            }
            setCallerActive();
            this.desiredAspectRatio = aspectRatio;
            this.windowDecorationHeight = windowDecorationHeight;
            storeIdealContentDimensions(1, 1, contentWidth, contentHeight);
            attachResizeListeners();
            fitToScreen();
        }
    }
    
    /**
     * Determines the bounds of the active screen (the screen currently
     * displaying the majority of the application).
     * Necessary in multi-screen (multi-monitor) environments to assist with
     * proper automatic resizing.
     * 
     * If no screens are detected due to a bug, the bounds are set to the
     * default width and height of the app's content.
     */
    private void determineActiveScreenBounds(){
        double currentCenterX = activeStage.getX() + (activeStage.getWidth()/2);
        double currentCenterY = activeStage.getY() + (activeStage.getHeight()/2);
        boolean screenFound = false;
        Rectangle2D givenBounds;
        ObservableList<Screen> screensIn = Screen.getScreens();
        
        for(int screenNo = 0; screenNo < screensIn.size() && !screenFound;
                screenNo++)
        {
            givenBounds = screensIn.get(screenNo).getVisualBounds();
            if(currentCenterX < givenBounds.getMaxX()
                    && currentCenterY < givenBounds.getMaxY()
                    && currentCenterX > givenBounds.getMinX()
                    && currentCenterY > givenBounds.getMinY())
            {
                //this screen being tested is the screen to use
                this.activeScreenWidth = givenBounds.getWidth();
                this.activeScreenHeight = givenBounds.getHeight();
                screenFound = true;
                diagnosticPrintln("Rendering on screen " + screenNo);
            }
            diagnosticPrintln("Screen: " + screenNo);
            diagnosticPrintln(screensIn.get(screenNo).getVisualBounds().toString());
        }
        if(!screenFound){
            this.activeScreenWidth = FXUIGameMaster.DEFAULT_CONTENT_WIDTH;
            this.activeScreenHeight = FXUIGameMaster.DEFAULT_CONTENT_HEIGHT;
            diagnosticPrintln("Using pixel-matched dimensions (DEFAULT CONTENT)");
        }
    }

    /**
     * Alerts this WindowResize object that we can expect the window (Stage) to
     * be active/open/non-null, allowing auto-resizing. This is only a hint;
     * internal checks are used to verify that the window/Stage is still active,
     * to avoid major catastrophe. Pair with with {@link #setCallerInactive()}
     * to enable/disable automatic resizing for this object on-the-fly.
     *
     * @return state of the associated internal check, which should be "true"
     * after calling this function.
     */
    public boolean setCallerActive() {
        return this.assumeCallerIsActive = true;
    }

    /**
     * Alerts this WindowResize object that we can expect the window (Stage) to
     * be inactive/hidden/closed/null, blocking auto-resizing.\n The associated
     * flag indicating "active" or "inactive" will be ignored if the associated
     * window/Stage has become inactive, so you are not required to explicitly
     * called this function. However, this flag (when set as "inactive" after
     * calling this function) will block any attempts to make use of this
     * WindowResize object until the flag is cleared/set back to "active" with
     * {@link #setCallerActive()}, should such utility prove useful (e.g.,
     * should you want to block & unblock automatic resizing at a certain point
     * of the project flow, or if we are shutting down the program and want to
     * avoid any further resource usage).
     *
     * @return state of the associated internal check, which should be "false"
     * after calling this function.
     */
    public boolean setCallerInactive() {
        return this.assumeCallerIsActive = false;
    }

    /**
     * Forces a check of the internal window object state. Should the associated
     * Stage or Scene (window and its contents) be null/inactive, tells this
     * object that the caller is "inactive"-- i.e., automatically blocks any
     * further attempts at auto-resizing until either (a) the WindowResize
     * object is refreshed or (b) this object is removed from memory.
     *
     * @return "true" if the window object is relatively safe to operative on,
     * "false" otherwise.
     */
    private boolean getWindowObjectState() {
        if (this.activeScene == null || this.activeStage == null) {
            diagnosticPrintln("There was either a faulty stage or faulty scene submitted for resizing. Please be wary...");
            return setCallerInactive();
        } else {
            return this.assumeCallerIsActive;
        }
    }

    /**
     * Verifies that the caller method is being run on a JavaFX thread. If not,
     * throws an exception. (Necessary as JFX objects generally require the JFX
     * thread for modification).
     */
    private void verifyIsFXThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new UnsupportedOperationException("Use of this ResizeHandler object requires a JavaFX thread for instantiation, modification, or use! "
                    + "(Did you implement me in the wrong place?)");
        }
    }

    /**
     * Sets the ideal dimensions of the contents (Scene) for this window
     * (Stage). Given an input of the minimum dimensions allowed for an object,
     * validates the "ideal dimensions" stored for the current window. If valid,
     * sets the desired dimensions directly with no issue. If invalid,
     * calculates a better set of content (Scene) dimensions for this window
     * (the active Stage). When calculation is required, uses the dimensions of
     * the primary screen as a seed to set the "ideal" values. Requires a valid
     * window decoration height to avoid chopping off content!
     *
     * @param minWidth the minimum width allowed for the content.
     * @param minHeight the minimum height allowed for the content.
     */
    private void storeIdealContentDimensions(double minWidth, double minHeight, double idealWidth, double idealHeight) {
        if (!getWindowObjectState()) {
            return;
        }
        
        //if we have some invalid input, we must do some calculation...
        if (idealWidth < minWidth || idealHeight < minHeight) {
            diagnosticPrintln("Detecting ideal window size..." + idealWidth + " <<W ::: H>> " + idealHeight);
            determineActiveScreenBounds();
            double maxWidth = activeScreenWidth; //TODO get rid of temp variable
            double maxHeight = activeScreenHeight;

            double usableScreenAspectRatio = maxWidth / maxHeight;
            /* if the desired aspect ratio for the given window is greater than the usable aspect ratio for the screen,
             * then the desired aspect ratio is too wide. We will have to calculate the applicable height
             * using the content's desired aspect ratio.
             */
            if (this.desiredAspectRatio > usableScreenAspectRatio) {
                this.idealContentWidth = maxWidth;
                this.idealContentHeight = maxWidth / this.desiredAspectRatio;
            } /*	else, we need to use the maximum available height, then determine the width to apply.*/ else {
                this.idealContentHeight = maxHeight;
                this.idealContentWidth = maxHeight * this.desiredAspectRatio;
            }
        } //else, we just set the values directly.
        else {
            this.idealContentHeight = idealHeight;
            this.idealContentWidth = idealWidth;
        }
    }

    /**
     * Performs a singular operation to resize the window to fit within the
     * bounds of the screen. Will block then unblock the window's associated
     * resize listeners. Makes use of the ideal dimensions. NOT FOR USE IN
     * FULLSCREEN.
     * 
     * Please ensure you run this on the JavaFX thread.
     */
    private void fitToScreen() {
        verifyIsFXThread();
        if (!getWindowObjectState()) {
            return;
        }
        diagnosticPrintln("FitToScreen started."
        		+ "\nDetermining optimal window dimensions.");
        determineActiveScreenBounds();
        double maxWidth = activeScreenWidth; //TODO get rid of temp variable
        double maxHeight = activeScreenHeight;
        
        double newContentWidth = 0, newContentHeight = 0;

        double usableScreenAspectRatio = maxWidth / maxHeight;
        /* if the desired aspect ratio for the given window is greater than the usable aspect ratio for the screen,
         * then the desired aspect ratio is too wide. We will have to fit the window to the applicable height
         * using the content's desired aspect ratio.
         */
        if (this.desiredAspectRatio > usableScreenAspectRatio) {
            newContentWidth = maxWidth - this.windowDecorationWidth;
            newContentHeight = maxWidth / this.desiredAspectRatio;
        } /*	else, we need to use the maximum available height, 
        then determine the width to which we should resize the window.*/ 
        else {
            newContentHeight = maxHeight - this.windowDecorationHeight;
            newContentWidth = maxHeight * this.desiredAspectRatio;
        }

        double scalePercentageHeight = newContentHeight / this.idealContentHeight;
        double scalePercentageWidth = newContentWidth / this.idealContentWidth;

        diagnosticPrintln("Scales w vs h: " + scalePercentageWidth + " :: " + scalePercentageHeight);
        Scale scale = new Scale(scalePercentageWidth, scalePercentageHeight);

        //final double xPositionToUse = (maxWidth - newContentWidth) / 2;
        //the above negated by use of built-in function to center on screen.
        final double newWidthCopy = newContentWidth;
        final double newHeightCopy = newContentHeight;
        /*finally, we actually set the size of the window*/
        detachResizeListeners();
        diagnosticPrintln("Snapping to dimensions...");
        activeScene.getRoot().getTransforms().setAll(scale);
        activeScene.getRoot().setLayoutX(0);
        activeScene.getRoot().setLayoutY(0);
        activeStage.setWidth(newWidthCopy + windowDecorationWidth);
        activeStage.setHeight(newHeightCopy + windowDecorationHeight);
        activeStage.centerOnScreen();
        diagnosticPrintln("FitToScreen complete.");
        attachResizeListeners();
            
    }
    
    /**
     * Automate the process of snapping contents to a fullscreen setup.
     * Please ensure you run this on the JavaFX thread.
     */
    private void fitToFullScreen() {
        verifyIsFXThread();
        if (!getWindowObjectState()) {
            return;
        }
        diagnosticPrintln("\n\nDetermining optimal window dimensions."
                + " (ENTERING FULLSCREEN SCALING MODE)");
        determineActiveScreenBounds();
        
        double maxWidth = activeStage.getWidth(); //TODO get rid of temp variable
        double maxHeight = activeStage.getHeight();
        
        double usableScreenAspectRatio = maxWidth / maxHeight;
        double newContentHeight, newContentWidth, scalePercentageHeight,
        		scalePercentageWidth, xTranslate, yTranslate;
        
        /* if the desired aspect ratio for the given window is greater than the 
         * usable aspect ratio for the screen, then the desired aspect ratio is 
         * too wide. There will be bars at the top and/or bottom. We need to use
         * the maximum available width, and figure out a better height to set.
         */
        if (this.desiredAspectRatio > usableScreenAspectRatio) {
            newContentWidth = maxWidth;
            newContentHeight = newContentWidth / this.desiredAspectRatio;
            diagnosticPrintln("(A-type) newHeightDetermined: "
            		+ newContentHeight + "\nIdeal content w vs h: " 
            		+ this.idealContentWidth + " :: " + this.idealContentHeight);
        } /*	else, we need to use the maximum available height, 
        then determine the width to which we should resize the window.*/ 
        else {
            newContentHeight = maxHeight/* + windowDecorationHeight*/;
            newContentWidth = newContentHeight * this.desiredAspectRatio;
            diagnosticPrintln("(B-type) newWidthDetermined: " 
            		+ newContentWidth + "\nIdeal content w vs h: " 
            		+ this.idealContentWidth + " :: " + this.idealContentHeight);
        }
        scalePercentageHeight = newContentHeight / this.idealContentHeight;
        scalePercentageWidth = newContentWidth / this.idealContentWidth;
        diagnosticPrintln("Scales w vs h: " + scalePercentageWidth 
        		+ " :: " + scalePercentageHeight);
        if((xTranslate = (maxWidth - newContentWidth) / 2) < 0){
        	xTranslate = 0;}
        if((yTranslate = (maxHeight - newContentHeight) / 2) < 0){
        	yTranslate = 0;}
        activeScene.getRoot().getTransforms()
        	.setAll(new Scale(scalePercentageWidth, scalePercentageHeight));
        activeScene.getRoot().setLayoutX(xTranslate);
        activeScene.getRoot().setLayoutY(yTranslate);
        diagnosticPrintln("xTranslate and yTranslate? " + xTranslate + " ::: " +
        		yTranslate);
        diagnosticPrintln("Scaling should be complete."
                + " (EXITING FULLSCREEN SCALING MODE)\n\n");
    }

    /**
     * Used to enable intelligent resizing of the main window (aka "Stage
     * primaryStage") This enables assistance in resizing of the primary window
     * at launch as well as after launch. Attempts to keep the proper aspect
     * ratio of the contents of the main window. Has no effect on secondary
     * dialogs. (Note: use of this method after launch does not snap the OS
     * window bounds to the contents of the window when the aspect ratio is
     * different). Applicable to the Stage defined in this ResizeHandler
     * instance!
     * 
     * If the app suddenly stops responding to any attempts to resize, ensure
     * you do not have a rogue call to the companion detachResizeListeners()
     * {@link #detachResizeListeners()} method & have forgotten to call this!
     */
    private void attachResizeListeners() {
        verifyIsFXThread();
        if (this.resizeBlocked.get() == false) {
            diagnosticPrintln("Resizing listeners already on!");
            return;
        }
        this.widthPropertyListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldStageWidth, Number newStageWidth) {
                detachResizeListeners();
                resize(TRIG_BY_WIDTH);
            }
        };

        this.heightPropertyListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldStageHeight, Number newStageHeight) {
                detachResizeListeners();
                resize(TRIG_BY_HEIGHT);
            }
        };
        this.activeScene.widthProperty().addListener(this.widthPropertyListener);
        this.activeScene.heightProperty().addListener(this.heightPropertyListener);
        this.resizeBlocked.set(false);
        diagnosticPrintln("Resizing listeners: ON!");
    }

    /**
     * Fully disable the resize listeners for the window. (there will be no
     * attempt at intelligent reaction unless an earlier intelligent resize
     * operation is active) Applicable to the Stage defined in this
     * ResizeHandler instance!
     * 
     * If the app suddenly stops responding to any attempts to resize,
     * ensure you are not missing a call to the companion attachResizeListeners()
     * {@link #attachResizeListeners()} method.
     */
    private void detachResizeListeners() {
        if (this.resizeBlocked.get() == true) {
            diagnosticPrintln("Resizing listeners already off!");
            return;
        }

        diagnosticPrintln("Resizing listeners: OFF!");
        verifyIsFXThread();
        this.resizeBlocked.set(true);
        this.activeScene.widthProperty().removeListener(this.widthPropertyListener);
        this.activeScene.heightProperty().removeListener(this.heightPropertyListener);
    }

    /**
     * Should a Slider be delegated to control (or display) the size of the
     * window, tell the Resize Handler about it so the value may be
     * automatically updated. (Value updates based on percentage of window size,
     * from 0 to 1) The slider may control the size of the window without using
     * this; it will simply receive no automatic feedback, nor will it be
     * dynamically enabled/disabled during resize operations.
     *
     * @param sliderAttached the slider object to modify over time
     */
    public void attachResizeSlider(Slider sliderAttached) {
        this.sliderToAdjust = sliderAttached;
    }

    /**
     * Allows setting the window size based on a percentage of the determined
     * ideal window size (determined when the program launches).
     * 
     * Please run on the JavaFX thread.
     *
     * @param percentage : percentage of ideal window size to apply, in a range
     * from 0.00 not inclusive to 1.00 inclusive (representing 0% and 100%).
     */
    public void resizeByPercentage(double percentage) {
        verifyIsFXThread();
        if (percentage <= 0) {
            return;
        }
        //detachResizeListeners();
        if(!activeStage.isFullScreen())
        {
            activeStage.setWidth(percentage * (this.idealContentWidth +
                    this.windowDecorationWidth));
            activeStage.setHeight(percentage * (this.idealContentHeight +
                    this.windowDecorationHeight));
        }
        else{
            activeStage.setWidth(percentage * this.idealContentWidth);
            activeStage.setHeight(percentage * this.idealContentHeight);
        }
        //attachResizeListeners();
    }

    /**
     * Used to assist in resizing of the primary window at launch as well as
     * after launch. Attempts to keep the proper aspect ratio of the contents of
     * the main window. Affects only the main window. (Note: use of this method
     * after launch does not snap the OS window bounds to the contents of the
     * window when the aspect ratio is different).
     *
     * @param triggerType whether triggered by a change in WIDTH or a change in
     * HEIGHT.
     */
    private void resize(int triggerType) {
        if (this.activeStage == null || this.activeScene == null) {
            diagnosticPrintln("skipping resize...(invalid Stage or invalid Scene)");
            return;
        } else {
            diagnosticPrintln("making resize thread.");
            this.widthHistory = this.activeStage.getWidth();
            this.heightHistory = this.activeStage.getHeight();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    resizeHelper(triggerType);
                }
            }).start();
        }
    }

    private void resizeHelper(int triggerType) {
        diagnosticPrintln("Initial resizeHelper call stats "
        	+ "(W:H:windowDecorationHeight): " + this.widthHistory 
        	+ ":::" + this.heightHistory + ":::" + this.windowDecorationHeight);
        if (!getWindowObjectState()) {
            return; }

        //remember whether the window was fullscreen when this method was invoked
        final boolean stageIsFullscreen = activeStage.isFullScreen();
        long waitTime = 200;
        diagnosticPrintln("The app was set to fullscreen at the time of this "
                + "method call: " + stageIsFullscreen);

        //disable any attached slider to prevent accidents/overrides
        if (this.sliderToAdjust != null) {
            this.sliderToAdjust.setDisable(true); }

        /*
         * Wait for the user to stop tweaking the window; monitor the dimensions
         * and when there are no more changes, proceed with resize checks.
         */

        do {
            diagnosticPrintln("Waiting for user to stop altering dimensions.");
            RiskUtils.sleep(2 * waitTime);
            this.widthHistory = this.activeStage.getWidth();
            this.heightHistory = this.activeStage.getHeight();
            RiskUtils.sleep(2 * waitTime);
        } while (this.widthHistory != this.activeStage.getWidth() && this.heightHistory != this.activeStage.getHeight());

        /*Used in the future to determine what steps of the resizing process must occur.*/
        AtomicBoolean stillRunningResize = new AtomicBoolean(false);
        stillRunningResize.set(true);

        double newContentWidth = 0.0d, newContentHeight = 0.0d;
        double scalePercentageWidth = 1.0d;
        double scalePercentageHeight = 1.0d;
        double expectedAspectRatio = 0.0d;

        //if we modified the height manually, keep that height property and calculate the new width that must be set
        if (triggerType == TRIG_BY_HEIGHT) {
            diagnosticPrintln("Trigger type: height (so will calc and fix width)");
            newContentHeight = this.heightHistory - this.windowDecorationHeight;
            newContentWidth = newContentHeight * this.desiredAspectRatio;
            scalePercentageHeight = newContentHeight / this.idealContentHeight;
            scalePercentageWidth = newContentWidth / this.idealContentWidth;
            expectedAspectRatio = newContentWidth / newContentHeight;
        } //else if the width, keep that width and programmatically set the height
        else if (triggerType == TRIG_BY_WIDTH) {
            diagnosticPrintln("Trigger type: width (so will calc and fix height)");
            newContentWidth = this.widthHistory - this.windowDecorationWidth;
            newContentHeight = newContentWidth / this.desiredAspectRatio;
            scalePercentageHeight = newContentHeight / this.idealContentHeight;
            scalePercentageWidth = newContentWidth / this.idealContentWidth;
            expectedAspectRatio = newContentWidth / newContentHeight;
        } else {
            diagnosticPrintln("How did you get here? Nobody's s'pose to be here.");
        }

        diagnosticPrintln("calculated target aspect ratio: " + expectedAspectRatio + " ...versus expected: " + this.desiredAspectRatio);
        /*Determine the relative amount by which we must scale the content -- the scene -- within the window (stage).
         This makes the scene come close to appropriately fitting within the stage with a limited unsightly gap.*/
        diagnosticPrintln("Scales w vs h: " + scalePercentageWidth + " :: " + scalePercentageHeight);
        Scale scale = new Scale(scalePercentageWidth, scalePercentageHeight);
        diagnosticPrintln("acceptable new width:: " + newContentWidth);
        diagnosticPrintln("acceptable new height:: " + newContentHeight);

        /*Store the new width and/or height in variables that can be accessed in a separate runnable/thread.*/
        final double newWidthCopy = newContentWidth;
        final double newHeightCopy = newContentHeight;
        final double attemptedAspectRatio = newContentWidth / newContentHeight;

        //If we attempted to resize our content to a dimension beyond the screen's bounds, we just snap back to the max dimensions available.
        determineActiveScreenBounds(); //TODO get rid of commented code below if this works
        if ((newWidthCopy >= activeScreenWidth ||
            newHeightCopy >= activeScreenHeight)
            && !stageIsFullscreen)
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    diagnosticPrintln("Choosing the \"fitToScreen\" path.");
                    fitToScreen();
                }
            });
        } /*now handle the size calulation being outside acceptable bounds but
        the screen IS indeed fullscreen
        */
        else if ((newWidthCopy >= activeScreenWidth ||
            newHeightCopy >= activeScreenHeight)
            && stageIsFullscreen)
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    diagnosticPrintln("Choosing the \"fitToFullscreen\" path.");
                    fitToFullScreen();
                }
            });
        } 
        else {
            /*Perform the actual changes using the JavaFX thread. At a bare minimum, content scaling is performed.*/
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    diagnosticPrintln("Target scene height  ::: target scene width");
                    diagnosticPrintln(newHeightCopy + ";;;" + newWidthCopy);
                    //activeStage.sizeToScene();
                    diagnosticPrintln("Attempting to scale.\nCan this stage be resized? (t if yes, f is no): " + activeStage.isResizable());
                    activeScene.getRoot().getTransforms().setAll(scale);
                    stillRunningResize.set(false);
                }
            });

            do {
                RiskUtils.sleep(waitTime);
                diagnosticPrintln("Pausing for SCALE to finish.");
            } while (stillRunningResize.get() == true);

            /*Now resize the window to fit the new scaling
             * ...yes, we must make this thread wait on other actions to complete. Again.*/
            stillRunningResize.set(true);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	activeScene.getRoot().setLayoutX(0);
                    activeScene.getRoot().setLayoutY(0);
                    if (!stageIsFullscreen) {
                        activeStage.setWidth(newWidthCopy+windowDecorationWidth);
                        activeStage.setHeight(newHeightCopy+windowDecorationHeight);
                    }
                    else{
                    activeStage.setWidth(newWidthCopy);
                    activeStage.setHeight(newHeightCopy);
                    }
                    if (sliderToAdjust != null) {
                        sliderToAdjust.setValue(activeStage.getWidth() / idealContentWidth);
                        sliderToAdjust.getTooltip().setText("Current percentage of max screen size: " + String.format("%.2f", activeStage.getWidth() / idealContentWidth));
                    }
                    diagnosticPrintln("new!!! " + desiredAspectRatio + " ...actually set to... " + (double) (activeScene.getWidth() / activeScene.getHeight()) + "...attempted to set: " + attemptedAspectRatio);
                    diagnosticPrintln("new2!!! ...effective dimens set to... " + activeScene.getWidth() + "::<W ::: H>:: " + activeScene.getHeight());
                    stillRunningResize.set(false);
                }
            });
            do {
                RiskUtils.sleep(waitTime);
                diagnosticPrintln("Waiting at EXIT for RESIZE to finish.");
            } while (stillRunningResize.get() == true);
        }
        //enable the resize listeners
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                attachResizeListeners();
            }
        });
        /*	Indicate that the thread to resize the window is no longer active.
         *	The app allows the thread to terminate to avoid permanent excess resource usage...in this case, at least.
         *	Any further use should re-fire the thread elsewhere.*/
        resizeThreadIsActive.set(false);
        if (this.sliderToAdjust != null && !this.activeStage.isFullScreen()) {
            //this.sliderToAdjust.setValue(this.activeStage.getWidth()/this.idealContentWidth);
            this.sliderToAdjust.setDisable(false);
        }
        diagnosticPrintln("Exit resize thread.");
    }
    
    /**
     * Enables a user to, with the flip of a particular boolean, control whether
     * verbose diagnostic information is printed. Calls to this method with the
     * affected boolean set to "false" will result in the message being
     * suppressed. Affected boolean: {@link #diagnosticModeEnabled}.
     * @param contentOut the content to be conditionally displayed.
     */
    public static void diagnosticPrintln(String contentOut) {
        if (WindowResizeHandler.diagnosticModeEnabled) {
            System.out.println(contentOut);
        }
    }

    /**
     * Create a non-JavaFX thread (if necessary) to build & display size options
     * for the associated window (Stage) Tries to run the dialog's code on a
     * non-JFX thread as much as possible.
     */
    public int showSizeOptions() {
        //represents the dialog; true: the dialog is visible (& code is waiting), false: window isn't showing.
        AtomicBoolean dialogIsShowing = new AtomicBoolean(true);

        if (!this.assumeCallerIsActive) {
            return -1;
        }

        if (Platform.isFxApplicationThread()) { //if this is the FX thread, make it all happen, and use showAndWait
            sizeOptionsHelper(dialogIsShowing);
        } else { //if this isn't the FX thread, we can pause logic with a call to RiskUtils.sleep()
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    sizeOptionsHelper(dialogIsShowing);
                }
            });

            do {
                RiskUtils.sleep(100);
            } while (dialogIsShowing.get());
        }
        return 0; // TODO decide on better return type
    }

    /**
     * Build & show the dialog.
     *
     * @param fxThread pass "true" if the method is being run on the JavaFX app
     * thread (please avoid doing so)
     * @param dialogIsShowing used to control the flow of code; will be set to
     * "false" when dialog is closed.
     */
    private void sizeOptionsHelper(AtomicBoolean dialogIsShowing) {
        Window owner = this.activeStage.getScene().getWindow();
        try {
            final Stage dialog = new Stage();
            dialog.setTitle("Window Options");
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY() + 50);

            final VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: pink; -fx-padding: 30");

            final Text queryText = new Text("     Alter window settings?     ");
            queryText.setTextAlignment(TextAlignment.CENTER);

            final Text querySymbol = new Text("[[[ --- ]]] ");
            querySymbol.setTextAlignment(TextAlignment.CENTER);
            querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));

            Text spaceBuffer = new Text("\n");
            spaceBuffer.setTextAlignment(TextAlignment.CENTER);
            spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 16));

            final Button yeah = new Button("Accept Changes");
            final Button nah = new Button("Revert Changes");

            yeah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {

                    dialogIsShowing.set(false);
                    dialog.close();
                }
            });

            nah.setDefaultButton(true);
            nah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dialogIsShowing.set(false);
                    dialog.close();
                }
            });

            Text windowSizeSliderLabel = new Text("Window size [%]");
            windowSizeSliderLabel.setFont(Font.font("Verdana", FontWeight.LIGHT, 16));
            windowSizeSliderLabel.setFill(Color.WHITE);

            Slider windowSizeSlider = new Slider(0.1f, 1.5f, 0.75f);
            windowSizeSlider.setSnapToTicks(false);
            windowSizeSlider.setShowTickMarks(true);
            windowSizeSlider.setMajorTickUnit(0.25f);
            windowSizeSlider.setMinorTickCount(0);
            windowSizeSlider.setTooltip(new Tooltip("Window Size (percentage -- 10% to 150%)"));
            windowSizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> ov,
                        Number old_val, Number new_val) {
                    if (!windowSizeSlider.isDisabled()) {
                        resizeByPercentage(new_val.doubleValue());
                    }
                }
            });
            attachResizeSlider(windowSizeSlider);

            CheckBox doFullScreen = new CheckBox("Display in fullscreen?");
            doFullScreen.setTooltip(new Tooltip("Enable or Disable fullscreen mode (enabled ignores screen sizing requests)"));
            doFullScreen.setTextFill(Color.ANTIQUEWHITE);
            doFullScreen.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    if (doFullScreen.isSelected()) {
                    	detachResizeListeners();
                        windowSizeSlider.setDisable(true);
                        activeStage.setFullScreen(true);
                        fitToFullScreen();
                        attachResizeListeners();
                    } else if (!doFullScreen.isSelected()) {
                        activeStage.setFullScreen(false);
                        windowSizeSlider.setDisable(false);
                    } else {
                        //do nothing
                    }
                }
            });
            doFullScreen.setSelected(activeStage.isFullScreen());

            layout.getChildren().setAll(
                    querySymbol, queryText, doFullScreen, windowSizeSliderLabel, windowSizeSlider, nah, yeah, spaceBuffer
            );

            dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    dialogIsShowing.set(false);
                }
            });

            dialog.setScene(new Scene(layout));
            dialog.show();
        } catch (Exception e) {
            System.out.println("ERROR: resize dialog display failed:: " + e);
        }
        attachResizeSlider(null);
    }
}
