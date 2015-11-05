/*FXUI GameMaster Class
 *Albert Wallace, 2015. Version info now found in class definition.
 *for Seth Denney's RISK, JavaFX UI-capable version
 *
 *Base build from original GameMaster class implementation, by Seth Denney, Feb 20 2015 
 */
package Master;

import LogPlayer.LogPlayer;
import Map.Country;
import Map.RiskMap;
import Player.EasyDefaultPlayer;
import Player.FXUIPlayer;
import Player.HardDefaultPlayer;
import Player.NormalDefaultPlayer;
import Player.Player;
import Player.PlayerFactory;
import Player.Seth;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.About;
import Util.Card;
import Util.DiceRoller;
import Util.FXUIAudio;
import Util.FXUI_Crossbar;
import Util.PlayerEliminatedException;
import Util.RiskConstants;
import Util.RiskUtils;
import Util.RollOutcome;
import Util.SavePoint;
import Util.SavePoint.SaveStatus;
import Util.TextNodes;
import Util.WindowResizeHandler;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Represents the primary game controller for a game of Risk. Asks for necessary
 * responses and makes necessary decisions, acting as a trigger for any Player
 * objects added.
 * 
* Compatible with original CPU opponents (Player), as well as a human player
 * through FXUI Player (FXUIPlayer) type.
 * 
*
 * UI elements are JavaFX, done with Java JDK 8. (By extension, elements were
 * done under JavaFX 8). Build requires JDK8.
 * (source files/build with "Stamp" -- aka date/time stamp -- of Feb 21
 * 2015, 6:00 PM -- aka Y2015.M02.D21.HM1800 -- supported JDK7, but successive
 * versions gradually made use of more JDK8 features).
 * 
*/
public class FXUIGameMaster extends Application {
	/**Controls whether or not diagnostic information is printed through the
     * associated {@link #diagnosticPrintln(string)} method. True: print
     * diagnostic info. False: suppress display of diagnostic info.
     * (Every call to diagnosticPrintln checks this, currently.) */
    public static final boolean DIAGNOSTIC_MODE = false;
    
    /*
     *Continue on with remaining variables and constants as normal... 
     */
    public static final String VERSION_INFO = "FXUI-RISK-Master\nVersion 01x16h\nStamp 2015.11.04, 20:20\nStability:Beta(02)"; // TODO implement safeguards on all run-once methods
    public static final String ERROR = "(ERROR!!)", INFO = "(info:)", WARN = "(warning-)";
    private static final String MAP_BACKGROUND_IMG = "RiskBoard.jpg";
    private static final String DEFAULT_CHKPNT_FILE_NAME = "fxuigm_save.s2r";
    private static String loadfrom_filename = DEFAULT_CHKPNT_FILE_NAME;
    private static String saveto_filename = DEFAULT_CHKPNT_FILE_NAME;
    private static final long AUTO_CLOSE_TIMEOUT = 5500;
    public static final int DEFAULT_CONTENT_WIDTH = 1600;
    public static final int DEFAULT_CONTENT_HEIGHT = 1062;
    public static final int DEFAULT_DIALOG_OFFSET = 300;
    private static final int IDLE_MODE = 0, NEW_GAME_MODE = 1, LOADED_GAME_MODE = 2;
    private static int workingMode = IDLE_MODE;
    protected static final String LOGFILE = "LOG.txt";
    protected static final String STATSFILE = "STATS.txt";
    protected static final String EVENT_DELIM = "...";
    protected static final boolean LOGGING_OFF = false, LOGGING_ON = true;
    protected static boolean forceEnableLogging = false, forceLoggingIsIndeterminate = true;
    protected static boolean loggingEnabled = true; //this is the one that has the final say as to whether the log file is created
	protected static AtomicBoolean logDialogIsShowing = new AtomicBoolean(false);
    protected static boolean runBotsOnly = false;
    protected static FXUI_Crossbar crossbar = new FXUI_Crossbar();
    protected static FXUIAudio audioManager = null;
    protected RiskMap map;
    protected Deque<Card> deck;
    protected static String desiredPlayersForGame = null;
    protected List<String> players;
    protected Map<String, Player> playerNameToPlayerObjHMap;
    protected Map<String, Collection<Card>> playerToCardDeckHMap;
    protected Map<String, Node[]> playerToIndicatorHMap;
    private static final int DEFAULT_PLAYER_NAME_FONT_SIZE = 22;
    protected static final long DEFAULT_DELAY_BETWEEN_MOVES = 750;
    protected static long delayTimeBetweenBots = DEFAULT_DELAY_BETWEEN_MOVES;

    protected static RiskMap starterMap = null;
    protected static Random rand;
    protected static int allocationIdx = 0;

    protected FileWriter log, stats;
    protected static List<String> internalLogCache = Collections.synchronizedList(new ArrayList<String>());
    protected List<String> allPlayers;
    protected int round, turnCount;

    private Scene scene;
    private static Stage mainStage;
    private static Pane mainWindowPane;
    private static Text subStatusTextElement;
    private static Text mainStatusTextElement;
    private static Text extendedMessageDisplay;
    private static ArrayList<String> extendedMessageCache;
    private static int extendedMessageCachePos;
    private HBox playerDisplay = null;
    private HashMap<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;
    private static boolean fullAppExit = false;
    private static List<Country> countriesToUpdateOwner = Collections.synchronizedList(new LinkedList<Country>());
    private static List<Country> countriesToUpdateCount = Collections.synchronizedList(new LinkedList<Country>());
    private static Date gameStartTime = new Date();

    private static WindowResizeHandler mainWindowResizeHandler = null;
    
    

    //to handle recovering a prior session or help with launching a new game session
    private static SavePoint activeSaveData = new SavePoint();
    private static SavePoint loadedSaveData = null;
    private static String loadFailureReason = "";
    private HashMap<String, Country> stringCountryRepresentation = new HashMap<String, Country>();
    private static ArrayList<Node> buttonCache = null;

    private enum ButtonPosEnum {
        BTN_START,
        BTN_SAVE,
        BTN_HIGHLIGHT,
        CKBX_LOGGING,
        BTN_LOG_PLAYBACK
    }
    private static boolean endGame = false;
    private static Player currentPlayer = null;
    private static boolean skipExitConfirmation = false;
    private static Thread priGameLogicThread = null;
    private static Node gameRunningIndicator = null;
	private static boolean indicatorAnimatedAlready;
    private static final boolean LOAD_ALT_SAVE = false, LOAD_DEFAULT_SAVE = true;

    
    
    
    /**
     * If the app detects a call from the system to exit the program, and it's
     * from a dialog window, handle the call by...asking if we really want to
     * exit.
     * @param shutAppDownOnAccept upon clicking the affirmation button
     * ("yes", etc) in the resultant dialog, this controls whether we want to 
     * close the entire app (true) or merely end the game (false) 
     * @param currentAttempts the number of attempts made to close the application
     * or end the game, as remembered by the caller. The return value can be used
     * to update whatever variable the calling method passed in as this parameter.
     * (Or, if hard-coded to a specific int, can tell you whether you get an affirming
     * or negative response: negative is 1 less than input, affirm is not)
     * @return returns {@link RiskConstants #MAX_ATTEMPTS} if affirmed (go ahead
     * and exit or stop the game), or "{@code currentAttempts - 1}" if negative.
     */
    public static int doYouWantToMakeAnExit(boolean shutAppDownOnAccept, int currentAttempts) {
        AtomicBoolean dialogIsShowing = new AtomicBoolean(true); //represents the EXIT dialog; true: the code displaying the dialog is still running, false otherwise.
        AtomicBoolean allowAppToExit = new AtomicBoolean(false);
        if (!FXUIGameMaster.skipExitConfirmation && Platform.isFxApplicationThread()) { //if this is the FX thread, make it all happen, and use showAndWait
            exitDialogHelper(shutAppDownOnAccept || FXUIGameMaster.fullAppExit, true, allowAppToExit, dialogIsShowing);
        } else if (!FXUIGameMaster.skipExitConfirmation && !Platform.isFxApplicationThread()) { //if this isn't the FX thread, we can pause logic with a call to RiskUtils.sleep()
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    exitDialogHelper(shutAppDownOnAccept || FXUIGameMaster.fullAppExit, false, allowAppToExit, dialogIsShowing);
                }
            });
            if (!shutAppDownOnAccept) {
                do {
                    RiskUtils.sleep(100);
                } while (dialogIsShowing.get() && !FXUIGameMaster.fullAppExit);
                if (FXUIGameMaster.fullAppExit) {
                    allowAppToExit.set(true);
                }
            } else {
                do {
                    RiskUtils.sleep(100);
                } while (dialogIsShowing.get());
            }
        } else {
            dialogIsShowing.set(false);
        }
        if (allowAppToExit.get() || FXUIGameMaster.skipExitConfirmation) {
            FXUIGameMaster.skipExitConfirmation = false;
            return RiskConstants.MAX_ATTEMPTS;
        } else {
            return currentAttempts - 1;
        }
    }

    private static void exitDialogHelper(final boolean shutAppDownOnAccept, final boolean fxThread, AtomicBoolean allowAppToExit, AtomicBoolean dialogIsShowing) {
        Window owner = mainWindowPane.getScene().getWindow();
        try {
            final Stage dialog = new Stage();
            dialog.setTitle("bye bye?");
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY());

            final VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: pink; -fx-padding: 30");

            final Text queryText = new Text("     Did you want to end the game?     ");
            queryText.setTextAlignment(TextAlignment.CENTER);

            final Text querySymbol = new Text("(o.o ?)");
            querySymbol.setTextAlignment(TextAlignment.CENTER);
            querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));

            Text spaceBuffer = new Text("\nIf enabled, your most recent\ncheckpoint will auto-save,\nor you may manually save");
            spaceBuffer.setTextAlignment(TextAlignment.CENTER);
            spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 16));

            final Button yeah = new Button("Yes");
            final Button nah = new Button("No");
            final Button saveMe = new Button("save last checkpoint to...");

            yeah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    crossbar.signalHumanEndingGame();
                    allowAppToExit.set(true);
                    dialogIsShowing.set(false);
                    FXUIGameMaster.skipExitConfirmation = shutAppDownOnAccept;
                    if (!shutAppDownOnAccept) {
                        FXUIGameMaster.endGame = true;
                        mainStatusTextElement.setText("I D L E");
                    }
                    crossbar.tryCloseCurrentPlayerDialog();
                    dialog.close();
                }
            });

            nah.setDefaultButton(true);
            nah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dialogIsShowing.set(false);
                    allowAppToExit.set(false);
                    dialog.close();
                }
            });

            saveMe.setTooltip(new Tooltip("Changes the location where your game is being auto-saved"
                    + "\nAND IMMEDIATELY saves to that new location!"));
            saveMe.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    if (performSave(true)) {
                        saveMe.getTooltip().setText("saved. autosave set to " + saveto_filename
                                + ".\n\nChanges the location where your game is being auto-saved"
                                + "\nAND IMMEDIATELY saves to that new location!");
                        spaceBuffer.setText("checkpoint saved. autosaving there\nuntil app restart");
                    } else {
                        saveMe.getTooltip().setText("save failed; try again???"
                                + "\n\nChanges the location where your game is being auto-saved"
                                + "\nAND IMMEDIATELY saves to that new location!");
                        spaceBuffer.setText("manual save failed.\n(checkpoint may be auto-saved)");
                    }
                }
            });

            if (!FXUIGameMaster.activeSaveData.getIsReady()) {
                saveMe.setDisable(true);
                saveMe.setText("save checkpoint (N/A)");
            }

            if (shutAppDownOnAccept) {
                yeah.setText("[continue]");
                layout.setStyle("-fx-background-color: black; -fx-padding: 30");
                queryText.setText("\"Good night, sweet prince.\"\n-Shakespeare. \"Hamlet\".\n\n - \n\n[window will close soon]");
                queryText.setFill(Color.WHEAT);
                querySymbol.setText("zZz (u_u?) zZz");
                querySymbol.setFill(Color.WHEAT);
                spaceBuffer.setFill(Color.WHEAT);
                spaceBuffer.setText("+\n+\n+");
                nah.setVisible(false);
                saveMe.setVisible(false);
                autoCloseDialog(dialog, spaceBuffer, new AtomicBoolean(false));
            }

            layout.getChildren().setAll(
                    querySymbol, queryText, saveMe, nah, yeah, spaceBuffer
            );

            dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    dialogIsShowing.set(false);
                    allowAppToExit.set(false);
                }
            });

            dialog.setScene(new Scene(layout));
            if (fxThread) {
                dialog.showAndWait();
            } else {
                dialog.show();
            }
        } catch (Exception e) {
            System.out.println(ERROR + "attempted exit failed:: " + e);
        }
    }

    /**
     * Allow the player to decide if they want to start a new game, or launch an
     * old game. SHOULD NOT BE RUN ON JAVAFX THREAD. 
     * @return Will return -1 in two scenarios: if not on the JavaFX thread, or 
     * if the game is not idle (if working mode doesn't equal idle). 
     * Else, returns the working mode (whether new game or loaded game)
     */
    private int displayGameSelector() {
        if (Platform.isFxApplicationThread() || FXUIGameMaster.workingMode != IDLE_MODE) {
            return -1;
        }
        AtomicBoolean gameSelectorIsShowing = new AtomicBoolean(true);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                gameSelectorHelper(gameSelectorIsShowing);
            }
        });

        do {
            RiskUtils.sleep(100);
        } while (gameSelectorIsShowing.get());
        FXUIGameMaster.crossbar.setCurrentPlayerDialog(null);

        return FXUIGameMaster.workingMode;
    }

    private void gameSelectorHelper(AtomicBoolean gameSelectorIsShowing) {
        Window owner = mainWindowPane.getScene().getWindow();
        final Stage dialog = new Stage();
        dialog.setTitle("new? restore?");
        dialog.initOwner(owner);
        dialog.setX(owner.getX());
        dialog.setY(owner.getY() + 300);

        final Pane miniPane = new Pane();

        final VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: coral;");
        
        final String infoStripCSSFormatting = "-fx-background-color: darkred; -fx-padding: 5";
        final VBox topInfoStrip = new VBox(10);
        topInfoStrip.setAlignment(Pos.CENTER);
        topInfoStrip.setStyle(infoStripCSSFormatting);
        final VBox bottomInfoStrip = new VBox(10);
        bottomInfoStrip.setAlignment(Pos.CENTER);
        bottomInfoStrip.setStyle(infoStripCSSFormatting);

        final Text queryText = new Text("     Greetings, Human!     \nWhat would you like to do?\n-\n");
        queryText.setTextAlignment(TextAlignment.CENTER);
        queryText.setFill(Color.WHEAT);

        final Text querySymbol = new Text("\\(OxO ?)");
        querySymbol.setTextAlignment(TextAlignment.CENTER);
        querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        querySymbol.setFill(Color.WHEAT);

        final Button newGameBtn = new Button("Launch a NEW game.");
        final Text newGameText = new Text("start a brand new game.");
        newGameText.setFont(Font.font("Arial", FontWeight.LIGHT, FontPosture.ITALIC, 12));
        newGameText.setTextAlignment(TextAlignment.CENTER);
        newGameBtn.setTooltip(new Tooltip("Launch a brand new game, with the "
        		+ "potential to overwrite\nprevious game saves."));
        
        final CheckBox botsOnly = new CheckBox("Bots only? (non-interactive play?)");
        botsOnly.setTooltip(new Tooltip("Effective for new games only.\n"
        		+ "CHECKED: the bots will automatically play without a human\n"
                + "UNCHECKED: ...a human player will be set up to play against bots\n"
                + "If enabling (CHECKED), remember the prior log will be overwritten!"));
        botsOnly.setTextFill(Color.BLACK);
        botsOnly.setFont(Font.font("Arial", FontWeight.LIGHT, FontPosture.ITALIC, 10));
        botsOnly.setSelected(FXUIGameMaster.runBotsOnly);
        
        double widthOfLines = 250d;
        double strokeThicknessOfLines = 5.0d;
        Color colorOfLines = Color.TOMATO;
        Line bufferLineOne = new Line(0,0,widthOfLines,0);
        Line bufferLineTwo = new Line(0,0,widthOfLines,0);
        Line bufferLineThree = new Line(0,0,widthOfLines,0);
        Line bufferLineFour = new Line(0,0,widthOfLines,0);
        bufferLineOne.setStrokeWidth(strokeThicknessOfLines);
        bufferLineTwo.setStrokeWidth(strokeThicknessOfLines);
        bufferLineThree.setStrokeWidth(strokeThicknessOfLines);
        bufferLineFour.setStrokeWidth(strokeThicknessOfLines);
        bufferLineOne.setStroke(colorOfLines);
        bufferLineTwo.setStroke(colorOfLines);
        bufferLineThree.setStroke(colorOfLines);
        bufferLineFour.setStroke(colorOfLines);

        final Text loadGameText = new Text(); //text set conditionally before insertion into layout
        final Button loadGameBtn = new Button("LOAD previous save.");
        final Text loadGameSubText = new Text("-");
        final String startingTooltipContents = "Load from the default save file!"
        		+ "\nCurrently set to load from " + loadfrom_filename;
        final Tooltip ldToolTip = new Tooltip(startingTooltipContents);
        loadGameText.setFont(Font.font("Arial", FontWeight.LIGHT, FontPosture.ITALIC, 12));
        loadGameText.setTextAlignment(TextAlignment.CENTER);
        loadGameSubText.setTextAlignment(TextAlignment.CENTER);
        loadGameSubText.setOpacity(0.5d);
        loadGameBtn.setTooltip(ldToolTip);
        
        final String delaySliderTextDefault = "Bot delay (none to 1.75 sec)";
        final Text delaySliderText = new Text(delaySliderTextDefault);
        delaySliderText.setTextAlignment(TextAlignment.CENTER);
        delaySliderText.setFont(Font.font("Arial", FontWeight.LIGHT, FontPosture.ITALIC, 12));
        
        final Slider botDelay = new Slider(0.0f,1.75f,0.75f);
        botDelay.setSnapToTicks(false);
        botDelay.setShowTickMarks(true);
        botDelay.setMajorTickUnit(0.25f);
        botDelay.setMinorTickCount(0);
        botDelay.setTooltip(new Tooltip("Insert a delay between turns taken by bots.\n"
        		+ "ranges from no delay (0x1sec) to max delay (1.75x1sec)"));
        

        final Button cnclBtn = new Button("[cancel]");
        cnclBtn.setTooltip(new Tooltip("Return to the main menu screen without "
        		+ "launching a game of either type."));

        /*
         * Different event handlers, 
         * to be used depending on states/keys&buttons to make available.
         */
        EventHandler<ActionEvent> defaultLdBtnHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                FXUIGameMaster.workingMode = LOADED_GAME_MODE;
                dialog.close();
            }
        };

        EventHandler<ActionEvent> loadAltFileHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                final FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(new Stage());
                if (file != null) {
                    //active_checkpoint_file_name = file.getPath();
                    if (loadFromSave(true, file.getAbsolutePath())) {
                        loadfrom_filename = file.getAbsolutePath();
                        FXUIGameMaster.workingMode = LOADED_GAME_MODE;
                        dialog.close();
                    } else {
                        loadGameText.setText("load failed.");
                        ldToolTip.setText(
                                (FXUIGameMaster.loadFailureReason.contains("FileNotFound")
                                        ? //If true, set text to...
                                        "Can't load: "
                                        + "OS dependent path error;\nplace the file in the same dir as the app,"
                                        + "\nrename as " + DEFAULT_CHKPNT_FILE_NAME + "& relaunch app"
                                        : //Or if false, set text to...
                                        "Can't load save file!\nReason:")
                                + loadFailureReason
                                + "\n\nCLICK to load alt save file"
                        );
                    }
                }
            }
        };

        AtomicBoolean loadButtonState = new AtomicBoolean(false);
        EventHandler<KeyEvent> altKeyEventHandler = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getEventType() == KeyEvent.KEY_PRESSED) {
                    if (event.getCode() == KeyCode.ALT || event.getCode() == KeyCode.SHIFT) {
                        loadButtonState.set(!loadButtonState.get());
                        if (loadButtonState.get() == LOAD_DEFAULT_SAVE) {
                            loadGameBtn.setOnAction(loadAltFileHandler);
                            loadGameBtn.setText("SELECT OTHER save file...");
                            ldToolTip.setText("Select a different checkpoint/save file!\n(Opens \"Locate File...\" dialog)");
                        } else if (loadButtonState.get() == LOAD_ALT_SAVE) {
                            loadGameBtn.setOnAction(defaultLdBtnHandler);
                            loadGameBtn.setText("LOAD from known checkpoint...");
                            ldToolTip.setText(startingTooltipContents);
                        }
                    }
                }
            }
        };

        newGameBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
            	/*
            	 * Check to see whether we are using bots or not...
            	 * Look at the state of the checkbox.
            	 */
            	if (botsOnly.isSelected()) {
                    //tell it to enable logging
                	FXUIGameMaster.runBotsOnly = true;
                } else if (!botsOnly.isSelected()) {
                    //tell it to forcefully disable logging
                	FXUIGameMaster.runBotsOnly = false;
                }
            	/*
            	 * Set us up to be in the new game mode, then close the dialog.
            	 */
                FXUIGameMaster.workingMode = NEW_GAME_MODE;
                dialog.close();
            }
        });
        newGameBtn.setDefaultButton(true);

        loadGameBtn.setOnAction(defaultLdBtnHandler);
        
        botDelay.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov,
                    Number old_val, Number new_val) {
            		FXUIGameMaster.delayTimeBetweenBots = (long) (FXUIGameMaster.DEFAULT_DELAY_BETWEEN_MOVES * new_val.doubleValue());
            		delaySliderText.setText(delaySliderTextDefault + " (now: " 
            				+ String.format("%.2f", new_val.doubleValue()) + ")");
            }
        });
        delaySliderText.setText(delaySliderTextDefault + " (now: " 
				+ String.format("%.2f", botDelay.getValue()) + ")");
        FXUIGameMaster.delayTimeBetweenBots = 
        		(long) (FXUIGameMaster.DEFAULT_DELAY_BETWEEN_MOVES * botDelay.getValue());

        cnclBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                dialog.close();
            }
        });

        if (!loadFromSave(true, DEFAULT_CHKPNT_FILE_NAME)) {
            loadGameBtn.setOpacity(0.5d);
            loadGameText.setOpacity(0.5d);
            loadGameBtn.setOnAction(loadAltFileHandler);
            loadGameBtn.setFocusTraversable(false);
            queryText.setText("Would you like to...\n"
            		+ "Make a new game?\n"
            		+ "Find an old save file?\n");
            ldToolTip.setText("Tried to load default save file...\n"
                    + (FXUIGameMaster.loadFailureReason.contains("FileNotFound")
                            ? "No checkpoint/save file found!\n" : "Couldn't load it!\nReason:")
                    + loadFailureReason
                    + "\n\nCLICK to load alt save file");
            loadGameText.setText("load alternate save file");
            loadGameBtn.setText("FIND previous save file...");
        } else {
            loadGameText.setText("load game from save file!");
            loadGameSubText.setText("(ALT/SHIFT switches loading types.)\n\n(hover over buttons to see more info.)");
            miniPane.setOnKeyPressed(altKeyEventHandler);
        }
        
        /*
         * Set up the layout...
         * Insert select elements into top and bottom coloured strips, with 
         * remaining elements to be inserted directly into the layout otherwise.
         */
        topInfoStrip.getChildren().setAll(querySymbol, queryText);
        bottomInfoStrip.getChildren().setAll(cnclBtn);

        layout.getChildren().setAll(
                topInfoStrip, 
                bufferLineOne, newGameText, newGameBtn, botsOnly, 
                bufferLineTwo, loadGameText, loadGameBtn, loadGameSubText, 
                bufferLineThree, delaySliderText, botDelay, 
                bufferLineFour, bottomInfoStrip
        );
        miniPane.getChildren().add(layout);

        dialog.setScene(new Scene(miniPane));

        /*Alter the boolean indicating whether the window is showing; without this, the logic will not continue
         * if the caller method depends on said atomic boolean to determine when to wait versus when to continue.
         */
        dialog.setOnHiding(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                gameSelectorIsShowing.set(false);

            }
        });
        dialog.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                gameSelectorIsShowing.set(true);

            }
        });

        /*Tell the crossbar what the current dialog is
         * (we are treating it as a human player dialog, for the sake of consistency)
         */
        FXUIGameMaster.crossbar.setCurrentPlayerDialog(dialog);
        dialog.show();
        /*
         *Don't forget to tell the crossbar there's no dialog
         * when the window is closed!
         * (In other words, .setCurrentPlayerDialog(NULL))
         */
    }

    /**
     * Used to auto-close the final dialog when exiting the app. Also gives a
     * sort of ...ASCII animation to indicate closing process, if supported by
     * dialog. Previously known as "deathKnell".
     *
     * @param dialog the dialog window to be closed (which permits the logic to
     * end)
     * @param animatedRegion the Text object to animate using ASCII text defined
     * in the method itself
     * @param delayClose used to signal whether the window is still open
     * (is set to false once the method closes the dialog window)
     */
    public static void autoCloseDialog(Stage dialog, Text animatedRegion, AtomicBoolean delayClose) {
        try {
            if (dialog == null) {
                System.out.println(INFO + "autoCloseDialogM: \"dialog\" reported as null. window will not automatically close");
                return;
            }
        	final AtomicBoolean dialogIsShowing = new AtomicBoolean(true);
        	dialog.showingProperty().addListener(new ChangeListener<Boolean>(){
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
						Boolean newValue) {
					dialogIsShowing.set(newValue.booleanValue());
					
				}
        	});
            
            if (animatedRegion == null) {
                Thread acDialog = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RiskUtils.sleep(AUTO_CLOSE_TIMEOUT);
                        if(delayClose != null && delayClose.get()){
                        	RiskUtils.sleep(2*AUTO_CLOSE_TIMEOUT);
                        }
                        if(dialogIsShowing.get()){
	                        Platform.runLater(new Runnable() {
	                            @Override
	                            public void run() {
	                                dialog.close();
	                            }
	                        });
                        }
                    }
                });
                acDialog.setDaemon(true);
                acDialog.setName("autoCloseDialog short");
                acDialog.start();
                return;
            } 
            else {
                final ArrayList<String> messagesOut = new ArrayList<String>();
                messagesOut.addAll(Arrays.asList("wait", "no", "don't leave", 
                		"come back!", "...I LIKE YOU", "BE MY FRIEND", "...", "uh"));
                final int discreteAnimSteps = 10 < (messagesOut.size() - 1) ? 10 : (messagesOut.size() - 1);
                Thread aCloser = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = discreteAnimSteps; i > -1 && dialogIsShowing.get(); --i) {
                            final int stepNo = i;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    String output = messagesOut.get(stepNo);
                                    animatedRegion.setText(output);
                                }
                            });
                            RiskUtils.sleep(AUTO_CLOSE_TIMEOUT / discreteAnimSteps);
                        }
                        if(delayClose != null && delayClose.get()){
                        	RiskUtils.sleep(2*AUTO_CLOSE_TIMEOUT);
                        }
                        if(dialog.isShowing()){
	                        Platform.runLater(new Runnable() {
	                            @Override
	                            public void run() {
	                                dialog.close();
	                            }
	                        });
                        }
                    }
                });
                aCloser.setName("autoCloseDialog long");
                aCloser.setDaemon(true);
                aCloser.start();
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    /**
     * Prepares save info. Use at the beginning of each new round, as a
     * checkpoint, to avoid any one player getting an advantage over any other
     * player upon later resume. Doesn't guarantee perfect state save, but
     * helps.
     */
    private boolean prepareSave() {
        if (activeSaveData == null) {
            activeSaveData = new SavePoint();
        }
        disableSaveButton();
        boolean saveIsReady = false;
        HashMap<String, Collection<Card>> playerCardsetMap = new HashMap<>();
        for (String player : this.players) {
            playerCardsetMap.put(player, createCardSetCopy(player));
        }
        /*
         * If loadedSaveData != null, we're working with an old save, so transplant the original save date
         * from that old game save. Else, we're working with a new game, and a new save date for the game.
         * Successive saves of a new game should only receive an updated latestSaveDate while 
         * keeping the original originalSaveDate.
         */
        Date originalSaveDate = (loadedSaveData != null && loadedSaveData.getOriginalSaveDate() != null)
                ? loadedSaveData.getOriginalSaveDate()
                : activeSaveData.getOriginalSaveDate();
        saveIsReady = activeSaveData.prepareOverallSave(
                originalSaveDate, new Date(), this.round,
                this.map,
                (Map<String, Player>) playerNameToPlayerObjHMap, this.allPlayers,
                FXUIGameMaster.internalLogCache,
                this.players,
                playerCardsetMap
        );
        System.out.println(INFO + "Checkpoint reached.");
        enableSaveButton();
        return saveIsReady;
    }

    /**
     * Performs actual write to disc using most recent checkpoint available.
     * Checkpoints are acquired with prepareSave(), automatically performed
     * after initial player allocation. Write to secondary storage is triggered
     * either automatically at each new round, or manually with the "Save"
     * button (with no discernible difference between the two).
     *
     * @return returns true on successful save, or false when a show-stopping
     * exception was thrown.
     */
    private static boolean performSave(boolean customSave) {
        disableSaveButton();
        // TODO add informative error messages
        boolean succeeded = activeSaveData.performSave(customSave, saveto_filename);
        if(succeeded){
            displayExtendedMessage("checkpoint saved\n" 
            		+ activeSaveData.getWriteStatus().getCustomDescription());
        }
        if (!succeeded) {
            setSubStatus("save failed");
            displayExtendedMessage(activeSaveData.getWriteStatus().getCustomDescription());
        }
        enableSaveButton();
        
        return succeeded;
    }

    /**
     * Restores a prior game session. Loads the contents of a serialized
     * SavePoint object into memory & rebuilds the game session from that
     * limited info. (Relies on some basic initialization elsewhere). Loading
     * should occur on the game logic thread (whichever thread prompts for
     * responses). Should *not* be run on threads which may delay
     * reading/processing (can cause concurrency issues, as it does not force a
     * lock of any necessary resources).
     *
     * @param testing To test the ability to read the save file & skip actually
     * loading data, "true". To do a full load of previous save data, "false".
     * @param potentialLocation location of the save file. Ignored unless
     * testing = true.
     * @return returns true if the load succeeded, or false if a show-stopping
     * exception was encountered
     */
    private boolean loadFromSave(boolean testing, String potentialLocation) {
        boolean loadSucceeded = true;
        try {
            InputStream file = new FileInputStream(testing ? potentialLocation : FXUIGameMaster.loadfrom_filename);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            SavePoint loadedSave = (SavePoint) input.readObject();
            loadedSaveData = loadedSave;
            if (!testing) {
                if (!loadPlayersFromSave(loadedSave)) {
                    loadFailureReason.concat("\n!LF::: "
                            + "Couldn't load prior player information from the given save file!");
                    loadSucceeded = false;
                } else {
                    loadSucceeded &= true;
                }
                if (!restoreCountryInfo(loadedSave)) {
                    loadFailureReason.concat("\n!LF::: "
                            + "Couldn't reset status information for all Countries.");
                    loadSucceeded = false;
                } else {
                    loadSucceeded &= true;
                }

                if (!restorePreviousLogInfo(loadedSave)) {
                    loadFailureReason.concat("\n!LF::: "
                            + "Couldn't restore the log for the prior game session!");
                    loadSucceeded = false;
                } else {
                    loadSucceeded &= true;
                }
                representPlayersOnUI();
                refreshUIElements(true);
            }
            input.close();
            buffer.close();
            file.close();
            loadFailureReason = "";
        } catch (Exception e) {
            if (!testing) {
                System.out.println(ERROR + "Load failed. ::: " + e);
                e.printStackTrace();
            }
            loadFailureReason = "\n!LF::: "
                    + "exception occurred: " + e;
            loadSucceeded = false;
        }
        return loadSucceeded;
    }

    /**
     * Loads the log from the prior game -- up to the checkpoint -- so we can
     * update the actual physical log file properly.
     *
     * @return "False" if no log info could be found in the previous save, or
     * "True" otherwise.
     */
    private boolean restorePreviousLogInfo(SavePoint loadedSave) {
        internalLogCache = loadedSaveData.getLogCache();
        if (loggingEnabled == LOGGING_ON) {
            try {
                if (this.log != null && this.stats != null) {
                    this.log.close();
                    this.stats.close();
                }
                this.log = new FileWriter(LOGFILE);
                this.stats = new FileWriter(STATSFILE);
            } catch (IOException e) {
            	System.out.println("Failed to recreate log file");
            	e.printStackTrace();
            }
        }
        for (String cacheLine : internalLogCache) {
            writeLogLn(false, cacheLine);
        }
        return loadedSave != null && loadedSave.getLogCache() != null;
    }

    /**
     * Pulls the players and their cards from a given SavePoint object.
     *
     * @param loadedSave the save from which we get player info
     * @return returns false if the amount of *active* players is outside the
     * bounds of the Risk rules, or true otherwise
     */
    protected boolean loadPlayersFromSave(SavePoint loadedSave) {
        boolean success = true;
        //clear the player list...just in case.
        writeLogLn(true, "Loading players...");
        try {
            this.playerNameToPlayerObjHMap.clear();
            this.allPlayers.clear();
            this.players.clear();
        } catch (Exception e) {
            this.playerNameToPlayerObjHMap = Collections.synchronizedMap(new HashMap<String, Player>());
            this.allPlayers = new ArrayList<String>();
            this.players = new ArrayList<String>();
        }
        final String FXP = FXUIPlayer.class.toString();
        final String EDP = EasyDefaultPlayer.class.toString();
        final String HDP = HardDefaultPlayer.class.toString();
        final String NDP = NormalDefaultPlayer.class.toString();
        final String S_P = Seth.class.toString();
        for (Entry<String, Boolean> playerIn : loadedSave.getPlayerIsEliminatedMap().entrySet()) {
            this.allPlayers.add(playerIn.getKey());
            Player playerObjectToCast = null;

			//Check player type if not eliminated
            //and recreate player for this session
            if (playerIn.getValue() == false)//if player isn't eliminated...
            {
                String switcher = loadedSave.getActivePlayersAndTheirTypes().get(playerIn.getKey());
                if (switcher.equals(FXP)) {
                    playerObjectToCast = new FXUIPlayer(playerIn.getKey());
                    FXUIPlayer.setCrossbar(FXUIGameMaster.crossbar);
                } else if (switcher.equals(EDP)) {
                    playerObjectToCast = new EasyDefaultPlayer(playerIn.getKey());
                } else if (switcher.equals(HDP)) {
                    playerObjectToCast = new HardDefaultPlayer(playerIn.getKey());
                } else if (switcher.equals(NDP)) {
                    playerObjectToCast = new NormalDefaultPlayer(playerIn.getKey());
                } else if (switcher.equals(S_P)) {
                    playerObjectToCast = new Seth(playerIn.getKey());
                }

                if (playerObjectToCast == null) {
                    System.out.println(ERROR + "Failed to cast/load " + playerIn.getKey()
                            + " as a valid player. (Attempted type: " + loadedSave.getActivePlayersAndTheirTypes().get(playerIn.getKey()) + "...Is the Player type know to the GameMaster?)");
                    success = false;
                } else {
                    this.playerNameToPlayerObjHMap.put(playerIn.getKey(), playerObjectToCast);
                    this.players.add(playerIn.getKey());
                }
            }
        }

        //rebuild the card deck the players had at the time of the save
        this.playerToCardDeckHMap = Collections.synchronizedMap(new HashMap<String, Collection<Card>>());
        for (Player playerM : this.playerNameToPlayerObjHMap.values()) {
            ArrayList<Card> newCards = new ArrayList<Card>();
            if (loadedSave.getPlayersAndTheirCards().get(playerM.getName()) != null) {
                for (String cardRepresentation : loadedSave.getPlayersAndTheirCards().get(playerM.getName())) {
                    if (cardRepresentation.contains(RiskConstants.WILD_CARD)) {
                        newCards.add(new Card(RiskConstants.WILD_CARD, null));
                    } else {
                        String[] cardDetails = cardRepresentation.split(",");
                        Card cdOut = new Card(cardDetails[0], stringCountryRepresentation.get(cardDetails[1]));
                        newCards.add(cdOut);
                    }
                }
            }
            this.playerToCardDeckHMap.put(playerM.getName(), newCards);
        }

        //Make sure we have the right number of players and there was no issue.
        if (this.players.size() < RiskConstants.MIN_PLAYERS || this.players.size() > RiskConstants.MAX_PLAYERS || !success) {
            return false; //there was a failure of some sort
        } else {
            writeLogLn(true, "Players:");
            for (String playerName : this.players) {
                writeLogLn(true, playerName);
            }
            writeLogLn(true, EVENT_DELIM);
            return true;
        }
    }

    /**
     * Takes country info (including owners + army count) from a SavePoint
     * object, and updates the internal data of the map with said info.
     * Refreshing the map is done elsewhere.
     *
     * @param loadedSave the SavePoint object from which we source our data
     */
    public boolean restoreCountryInfo(SavePoint loadedSave) {
        for (Entry<String, Integer> entryOutArmy : loadedSave.getCountriesAndArmyCount().entrySet()) {
            mapSetCountryArmyCount(stringCountryRepresentation.get(entryOutArmy.getKey()), entryOutArmy.getValue());
        }
        for (Entry<String, String> entryOutOwner : loadedSave.getCountriesAndOwners().entrySet()) {
            mapSetCountryOwner(stringCountryRepresentation.get(entryOutOwner.getKey()), entryOutOwner.getValue());
        }
        this.round = loadedSave.getRoundsPlayed();

        return loadedSave.getCountriesAndArmyCount().entrySet().size() == loadedSave.getCountriesAndOwners().entrySet().size()
                && loadedSave.getCountriesAndOwners().entrySet().size() > 0;
    }

    /**
     * This method is used as a way to have the new game, load game and save
     * game buttons disabled at select critical points/when the option is not
     * pertinent, and re-enable them when the game is not in any critical
     * sections. Does not handle all fine-tuned disables/enables; only initial
     * startup and game play. Based on states that might otherwise be easily
     * compromised.
     */
    public void setButtonAvailability() {
        if (buttonCache.size() != ButtonPosEnum.values().length) {
            System.out.println(WARN + "I can't determine if I'm able to access some of"
                    + " the UI buttons to disable/enable them. Weird, huh?");
            return;
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (workingMode == IDLE_MODE) {
                    buttonCache.get(ButtonPosEnum.BTN_START.ordinal()).setDisable(false); //we can start a new game
                    disableSaveButton(); //we cannot use the save button
                    buttonCache.get(ButtonPosEnum.BTN_HIGHLIGHT.ordinal()).setVisible(false); //we cannot highlight the countries owner by a given player...
                    buttonCache.get(ButtonPosEnum.CKBX_LOGGING.ordinal()).setDisable(false); //we are allowed to enable/disable logging
                    buttonCache.get(ButtonPosEnum.BTN_LOG_PLAYBACK.ordinal()).setDisable(false); //disable the log playback button
                    //the checkbox shouldn't have changed during play, but we update its text
                    ((CheckBox) buttonCache.get(ButtonPosEnum.CKBX_LOGGING.ordinal())).setText("Enable logging?\n(State: " + (FXUIGameMaster.loggingEnabled == LOGGING_ON ? "Yes" : "No") + ")");
                    setPlayStatus("I D L E"); //set the status to "IDLE"
                } else {
                    buttonCache.get(ButtonPosEnum.BTN_START.ordinal()).setDisable(true); //we cannot start a new game...at this point.
                    //save button is set dynamically while the game is in play, so do not concern yourself with it
                    buttonCache.get(ButtonPosEnum.BTN_HIGHLIGHT.ordinal()).setVisible(true); //we CAN highlight the countries owner by a given player.
                    buttonCache.get(ButtonPosEnum.CKBX_LOGGING.ordinal()).setDisable(true); //we are not allowed to enable/disable logging
                    buttonCache.get(ButtonPosEnum.BTN_LOG_PLAYBACK.ordinal()).setDisable(true); //disable the log playback button
                    ((CheckBox) buttonCache.get(ButtonPosEnum.CKBX_LOGGING.ordinal())).setIndeterminate(false);
                    ((CheckBox) buttonCache.get(ButtonPosEnum.CKBX_LOGGING.ordinal())).setSelected(FXUIGameMaster.loggingEnabled);
                    ((CheckBox) buttonCache.get(ButtonPosEnum.CKBX_LOGGING.ordinal())).setText("Enable logging?\n(State: " + (FXUIGameMaster.loggingEnabled == LOGGING_ON ? "Yes" : "No") + " -- Locked during play)");
                    setPlayStatus("in play."); //set the status to "in play"; will be overwritten with an error if need be
                }
            }
        });
    }

    /**
     * Companion method to disable the save button; ensures actions happen on FX
     * thread
     */
    public static void disableSaveButton() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                buttonCache.get(ButtonPosEnum.BTN_SAVE.ordinal()).setDisable(true); //disable the save button
            }
        });
    }

    /**
     * Companion method to enable the save button; ensures actions happen on FX
     * thread
     */
    public static void enableSaveButton() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                buttonCache.get(ButtonPosEnum.BTN_SAVE.ordinal()).setDisable(false); //enable the save button
            }
        });
    }

    /**
     * Used to set the text of the "currentPlayStatus" UI element, even from
     * unsafe threads (unsafe: "not the FX thread")
     *
     * @param status text to be displayed
     * @return "true" if called from the JavaFX thread, "false" otherwise
     */
    private boolean setPlayStatus(String status) {
        if (Platform.isFxApplicationThread()) //if already on FX thread, can directly set
        {
            mainStatusTextElement.setText(status);
            return true;
        } else //place in event queue for running on the FX thread
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    mainStatusTextElement.setText(status);
                }
            });
            return false;
        }
    }

    /**
     * Used to set the text of the "subStatusTextElement" UI element, even from
     * unsafe threads (unsafe: "not the FX thread"). As implied, intended use is
     * to indicate some sort of error -- without replacing the main status. TODO
     * add the ability to push these messages to diagnostics dialog & secondary
     * storage
     *
     * @param error text to be displayed (or default/alternative text, if no
     * error occurred)
     * @return "true" if called from the JavaFX thread, "false" otherwise
     */
    private static boolean setSubStatus(String status) {
        if (Platform.isFxApplicationThread()) //if already on FX thread, can directly set
        {
            subStatusTextElement.setText(status);
            return true;
        } else //place in event queue for running on the FX thread
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    subStatusTextElement.setText(status);
                }
            });
            return false;
        }
    }

    /**
     * Once button logic has been handled, and it is verified that no other game
     * has been started, this method is run to trigger the various states of
     * attack/defense/etc, trigger checkpoints, and check for any outstanding
     * calls to prematurely end the game.
     *
     * @return name of the winner if the game has an ideal termination, null
     * otherwise.
     */
    private String begin() {
        boolean initiationGood = false;
        if (workingMode == NEW_GAME_MODE) {
            if (!(initiationGood = loadPlayers(FXUIGameMaster.desiredPlayersForGame))) {
                System.out.println(ERROR + "Invalid number of players. 2-6 Players allowed.");
                setPlayStatus("creation of new game failed; 2-6 Players allowed");
            } else {
                representPlayersOnUI();
                allocateMap();
                initiationGood = initializeForces() && initiationGood;
                if (!initiationGood) {
                    setPlayStatus("creation of new game failed");
                } else {
                    prepareSave();
                }
            }
        } else if (workingMode == LOADED_GAME_MODE) {
            initiationGood = loadFromSave(false, null);
            if (!initiationGood) {
                setPlayStatus("load failed!!");
            }
        }
        if (initiationGood) {
            crossbar.resetEndGameSignal();
            //play round-robin until there is only one player left
            int turn = 0;
            while (this.players.size() > 1 && !FXUIGameMaster.endGame) {
                if (turn == 0) {
                    this.round++;
                    writeLogLn(true, "Beginning Round " + round + "!");
                    if (this.round > RiskConstants.MAX_ROUNDS) {
                        return "Stalemate!";
                    }
                }
                FXUIAudio.playNextNote();
                FXUIGameMaster.currentPlayer = this.playerNameToPlayerObjHMap.get(this.players.get(turn));
                boolean canUpdateUIAndSave = FXUIGameMaster.currentPlayer.getClass().toString().equals(FXUIPlayer.class.toString());
                writeLogLn(true, FXUIGameMaster.currentPlayer.getName() + " is starting their turn.");
                writeStatsLn();
                this.turnCount++;
                try {
                	highlightCurrentPlayer(FXUIGameMaster.currentPlayer);
                    for (int gameStep = 4; gameStep > -1 && !(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame); gameStep--) {
                        refreshUIElements(canUpdateUIAndSave || FXUIGameMaster.runBotsOnly);
                        if(FXUIGameMaster.runBotsOnly || !canUpdateUIAndSave){
                        	RiskUtils.sleep(FXUIGameMaster.delayTimeBetweenBots);
                        }
                        switch (gameStep) {
                            case 4:
                                reinforce(FXUIGameMaster.currentPlayer, true);
                                break;
                            case 3:
                                attack(FXUIGameMaster.currentPlayer);
                                break;
                            case 2:
                                fortify(FXUIGameMaster.currentPlayer);
                                break;
                            case 1:
                                turn = (this.players.indexOf(FXUIGameMaster.currentPlayer.getName()) + 1) % this.players.size();
                                break;
                            case 0:
                                if (canUpdateUIAndSave && !FXUIGameMaster.runBotsOnly) {
                                    prepareSave();
                                    performSave(false);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } catch (PlayerEliminatedException e) {
					//If an elimination exception is thrown up to this level,
                    //then it was currentPlayer who was eliminated.
                    turn %= this.players.size();
                }
            }
            FXUIGameMaster.currentPlayer = null;
            highlightCurrentPlayer(null);
            if (!FXUIGameMaster.fullAppExit && !FXUIGameMaster.endGame && this.players.size() > 0) {
                refreshUIElements(true);
                FXUIAudio.playEndJingle();
                writeStatsLn();
                System.out.println(this.players.get(0) + " is the victor!");
                writeLogLn(true, this.players.get(0) + " is the victor!");
                displayExtendedMessage(this.players.get(0) + " is the victor!");
				winnerScreen(this.players.get(0));
                FXUIGameMaster.workingMode = IDLE_MODE;
                //flashPlayerCountries(this.players.get(0));
            } else if (!FXUIGameMaster.fullAppExit) {
                displayExtendedMessage("Thanks for playing!");
                System.out.println(WARN + "Early game end by UI player "
                		+ "(not a full exit); sorry 'bout it!");
            } else {
                System.out.println(WARN + "Game forced to exit by UI player; sorry 'bout it!");
            }
        }

        try {
            if (this.log != null && this.stats != null) {
                log.close();
                stats.close();
            }
        } catch (IOException e) {
        	System.out.println("Failed to finalize log file");
        	e.printStackTrace();
        }
        FXUIGameMaster.workingMode = IDLE_MODE;
        setButtonAvailability();
        crossbar.resetEndGameSignal();
        if (this.players.size() > 0 && !FXUIGameMaster.endGame) {
            return this.players.get(0);
        } else {
            FXUIGameMaster.endGame = false;
            return null;
        }
    }

    /**
     * Triggers each Player type to invoke initial troop allocation on countries
     * assigned to each player. Used only for new games, as saved games can
     * restore their state fairly well.
     *
     * @return returns true if at least one player succeeded, false otherwise
     */
    protected boolean initializeForces() {
        boolean valid;
        int attempts;
        int playerIndex = 0;
        //get initial troop allocation
        while (playerIndex < this.players.size() && !crossbar.isHumanEndingGame()) {
            Player player = this.playerNameToPlayerObjHMap.get(this.players.get(playerIndex));
            writeLogLn(true, "Getting initial troop allocation from " + player.getName() + "...");
            int reinforcements;
            valid = false;
            attempts = 0;
            /* If we reach a point where there is only one player remaining,
             * given that we are just doing the initial player setup, the game
             * is no longer valid
             **/
            if (this.players.size() < RiskConstants.MIN_PLAYERS) {
                try {
                    eliminate(player, null, "Player count: " + this.players.size() + "; required players: " + RiskConstants.MIN_PLAYERS);
                } catch (PlayerEliminatedException e) {
                    writeLogLn(false, "WARNING: Invalid game initialization. Invalid game state reached!");
                }
            } else {
                while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit && !crossbar.isHumanEndingGame()) {
                    attempts++;
                    reinforcements = RiskConstants.INIT_ARMIES / this.players.size();
                    ReinforcementResponse rsp = tryInitialAllocation(player, reinforcements);
                    if (crossbar.isHumanEndingGame()) {
                        break;
                    }
                    if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, player.getName(), reinforcements)
                            && validateInitialAllocation(rsp.getAllocation(), player.getName(), reinforcements)) {
                        allocateArmies(player.getName(), rsp.getAllocation(), reinforcements);
                        playerIndex++;
                        writeLogLn(true, "Troops successfully allocated for " + player.getName() + "...");
                    }
                }
                if (!valid || crossbar.isHumanEndingGame()) {
                    try {
                        if (crossbar.isHumanEndingGame()) {
                            eliminate(player, null, "Ending game prematurely; ejecting player from game!");
                        } else {
                            eliminate(player, null, "You failed to provide a valid initial army allocation.");
                        }
                    } catch (PlayerEliminatedException e) {
                        playerIndex = 0;
                    }
                }
            }
        }
        return this.players.size() > 0;
    }

    /**
     * Attempts to ensure that any Player is indeed registered as a known player
     * and looking up the name of the player points to the correct Player. If
     * the Player is not found in the list (due to some goof up), triggers
     * a player elimination to remove the Player from the roster.
     * @param player
     * @throws PlayerEliminatedException
     */
    protected void validatePlayerName(Player player) throws PlayerEliminatedException {
        if (!(this.playerNameToPlayerObjHMap.containsKey(player.getName()) && this.playerNameToPlayerObjHMap.get(player.getName()) == player)) {
            eliminate(player, null, "Players who hide their true identity are not welcome here. BEGONE!");
        }
    }

    /**
     * Replicates the {@link RiskMap #setCountryOwner(Country, String)} function
     * with the side-effect of keeping track of which countries have changed,
     * to assist with UI refresh.
     * Sets the owner (where the owner is the name of a given Player) 
     * of a given country.
     * @param country
     * @param owner
     */
    private void mapSetCountryOwner(Country country, String owner){
    	if(country == null){
    		System.out.println("Null country being added to list. What? mSCO");
    	}
    	this.map.setCountryOwner(country, owner);
    	FXUIGameMaster.countriesToUpdateOwner.add(country);
    }
    
    /**
     * Replicates the {@link RiskMap #setCountryArmies(Country, int)} function
     * with the side-effect of keeping track of which countries have changed
     * to assist with UI refresh.
     * Sets the number of army members in a given Country on the RiskMap.
     * @param country
     * @param numArmies
     */
    private void mapSetCountryArmyCount(Country country, int numArmies){
    	if(country == null){
    		System.out.println("Null country being added to list. What? mSCAC");
    	}
    	this.map.setCountryArmies(country, numArmies);
    	FXUIGameMaster.countriesToUpdateCount.add(country);
    }
    
    /**
     * Replicates the {@link RiskMap #addCountryArmies(Country, int)} function
     * with the side-effect of keeping track of which countries have changed
     * to assist with UI refresh.
     * Adds to (subtracts from) the number of army members in a given Country.
     * @param country
     * @param numArmies the number of add (or subtract, if negative)
     */
    private void mapAddToCountryArmyCount(Country country, int numArmies){
    	if(country == null){
    		System.out.println("Null country being added to list. What? mATCAC");
    	}
    	this.map.addCountryArmies(country, numArmies);
    	FXUIGameMaster.countriesToUpdateCount.add(country);
    }
    
    protected void reinforce(Player currentPlayer, boolean withCountryBonus) throws PlayerEliminatedException {
        int reinforcements = 0;
        int attempts = 0;
        boolean valid = false;
        reinforcements += getCardTurnIn(currentPlayer, getPlayerCardCounts());
        if (FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame) {
            return;
        }
        Map<String, Integer> oppCards = getPlayerCardCounts();
        if (withCountryBonus) {
            reinforcements += RiskUtils.calculateReinforcements(this.map, currentPlayer.getName());
        }
        writeLogLn(true, currentPlayer.getName() + " reinforcing with " + reinforcements + " armies.");
        while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
            attempts++;
            ReinforcementResponse rsp = tryReinforce(currentPlayer, oppCards, reinforcements);
            if (FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame) {
                return;
            }
            this.refreshUIElements(true);
            if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, currentPlayer.getName(), reinforcements)) {
                for (Map.Entry<Country, Integer> entry : rsp.getAllocation().entrySet()) {
                    this.mapAddToCountryArmyCount(entry.getKey(), entry.getValue());
                    writeLogLn(true, entry.getValue() + " " + entry.getKey().getName());
                }
            }
        }
        if (!valid) {
            eliminate(currentPlayer, null, "You failed to provide a valid reinforcement allocation.");
        } else if (crossbar.isHumanEndingGame()) {
            eliminate(currentPlayer, null, "Player decided to leave. Come back any time, friend!");
        }
        writeLogLn(true, EVENT_DELIM);
    }

    protected void attack(Player currentPlayer) throws PlayerEliminatedException {
        int attempts = 0;
        boolean resetTurn;
        boolean hasGottenCard = false;
        while (attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
            attempts++;
            resetTurn = false;
            AttackResponse atkRsp = tryAttack(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
            this.refreshUIElements(true);
            if (atkRsp != null) {
                if (AttackResponse.isValidResponse(atkRsp, this.map, currentPlayer.getName())) {
                    writeLogLn(true, currentPlayer.getName() + " is attacking "
                            + atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
                            + ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
                    System.out.println(currentPlayer.getName() + " is attacking "
                            + atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
                            + ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
                    attempts = 0;
                    Player defender = getOwnerObject(atkRsp.getDfdCountry());
                    DefendResponse dfdRsp = null;
                    try {
                        //this is guaranteed to either be valid or throw a PlayerEliminatedException
                        dfdRsp = defend(defender, getPlayerCardCounts(), atkRsp);
                    } catch (PlayerEliminatedException e) {
                        //defender messed up and was auto-eliminated
                        resetTurn = true;
                    }
                    if (!resetTurn) {
                        carryOutAttack(atkRsp, dfdRsp);
                        hasGottenCard = checkForTakeover(currentPlayer, atkRsp, hasGottenCard) || hasGottenCard;
                    }
                }
            } else {
                //because an attack is not required, a null response is taken to mean that the player declines the opportunity
                attempts = RiskConstants.MAX_ATTEMPTS;
            }
        }
    }

    protected DefendResponse defend(Player defender, Map<String, Integer> oppCards, AttackResponse atkRsp) throws PlayerEliminatedException {
        int attempts = 0;
        boolean valid = false;
        DefendResponse rsp = null;
        while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
            attempts++;
            rsp = tryDefend(defender, createCardSetCopy(defender.getName()), oppCards, new AttackResponse(atkRsp));
            this.refreshUIElements(true);
            valid = DefendResponse.isValidResponse(rsp, this.map, atkRsp.getDfdCountry());
        }
        if (!valid) {
            eliminate(defender, null, "You failed to provide a valid defense response.");
        } else if (crossbar.isHumanEndingGame()) {
            eliminate(defender, null, "This defender wants a break. Go ahead, friend. You deserve it.");
        }
        return rsp;
    }

    protected void carryOutAttack(AttackResponse atk, DefendResponse dfd) {
        RollOutcome result = DiceRoller.roll(atk.getNumDice(), dfd.getNumDice());
        this.mapAddToCountryArmyCount(atk.getAtkCountry(), -1 * result.getAtkLosses());
        this.mapAddToCountryArmyCount(atk.getDfdCountry(), -1 * result.getDfdLosses());
        writeLogLn(true, "\tAttacker lost: " + result.getAtkLosses() + "; Defender lost: " + result.getDfdLosses());
    }

    protected boolean checkForTakeover(Player attacker, AttackResponse atkRsp, boolean hasGottenCard) throws PlayerEliminatedException {
        if (this.map.getCountryArmies(atkRsp.getDfdCountry()) == 0) {
            String loserName = this.map.getCountryOwner(atkRsp.getDfdCountry());
            writeLogLn(true, attacker.getName() + " has taken " + atkRsp.getDfdCountry() + " from " + loserName + "!");
            this.mapSetCountryOwner(atkRsp.getDfdCountry(), attacker.getName());
            if (!hasGottenCard) {
                awardCard(attacker.getName());
            }
            boolean allowReinforce = false;
            try {
                advanceArmies(attacker, atkRsp);
                allowReinforce = true;
            } catch (PlayerEliminatedException attackerException) {
                throw attackerException;
            } finally {
                checkForElimination(attacker, loserName, atkRsp.getDfdCountry(), allowReinforce);
            }
            return true;
        } else {
            return false;
        }
    }

    protected void advanceArmies(Player attacker, AttackResponse atkRsp) throws PlayerEliminatedException {
        int attempts = 0;
        boolean valid = false;
        while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
            attempts++;
            AdvanceResponse advRsp = tryAdvance(attacker, createCardSetCopy(attacker.getName()), getPlayerCardCounts(), atkRsp);
            if (valid = AdvanceResponse.isValidResponse(advRsp, atkRsp, this.map)) {
                writeLogLn(true, attacker.getName() + " advanced " + advRsp.getNumArmies() + " into " + atkRsp.getDfdCountry() + " from " + atkRsp.getAtkCountry() + ".");
                this.mapAddToCountryArmyCount(atkRsp.getAtkCountry(), -1 * advRsp.getNumArmies());
                this.mapAddToCountryArmyCount(atkRsp.getDfdCountry(), advRsp.getNumArmies());
            }
        }
        if (crossbar.isHumanEndingGame()) {
            eliminate(attacker, null, "The advancer decided to take a break. 'S OK. Get some cookies. Or hot cocoa.");
        } else if (!valid) {
            eliminate(attacker, null, "You failed to provide a valid advance response.");
        }
    }

    protected void awardCard(String playerName) {
        writeLogLn(true, "Awarding " + playerName + " one card.");
        if (this.deck.size() > 0) {
            this.playerToCardDeckHMap.get(playerName).add(this.deck.removeFirst());
        }
    }

    /**
     * Check for & perform player elimination if an elimination condition is
     * detected. (Specifically, if the player reigns over 0 countries, performs
     * elimination.)
     *
     * @param attacker
     * @param loserName
     * @param takenCountry
     * @param allowReinforce
     * @throws PlayerEliminatedException
     */
    protected void checkForElimination(Player attacker, String loserName, Country takenCountry, boolean allowReinforce) throws PlayerEliminatedException {
        try {
            if (RiskUtils.getPlayerCountries(this.map, loserName).size() == 0) {
                eliminate(getPlayerObject(loserName), attacker, "You were eliminated by " + attacker.getName() + " at " + takenCountry.getName() + ".");
            }
        } catch (PlayerEliminatedException defenderException) {
			//this ensures that attacker will not be allowed to reinforce if (s)he was auto-eliminated during the advanceArmies() call or the game ended.
            //also, player can only reinforce after eliminating another player if (s)he is forced to turn in cards
            if (allowReinforce && this.playerToCardDeckHMap.get(attacker.getName()).size() >= RiskConstants.FORCE_TURN_IN && this.players.size() > 1) {
                reinforce(attacker, false);//note that if the current player fails to reinforce, the player can be eliminated here and an exception thrown back up to begin()
            }
        }
    }

    /**
     * Allows a given player to fortify his/her/their troop count during each
     * round, if possible.
     *
     * @param currentPlayer
     */
    protected void fortify(Player currentPlayer) {
        int attempts = 0;
        boolean valid = false;
        while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
            attempts++;
            FortifyResponse rsp = tryFortify(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
            this.refreshUIElements(true);
            if (rsp != null) {
                if (valid = FortifyResponse.isValidResponse(rsp, this.map, currentPlayer.getName())) {
                    writeLogLn(true, currentPlayer.getName() + " is transferring " + rsp.getNumArmies() + " from " + rsp.getFromCountry() + " to " + rsp.getToCountry() + ".");
                    this.mapAddToCountryArmyCount(rsp.getFromCountry(), -1 * rsp.getNumArmies());
                    this.mapAddToCountryArmyCount(rsp.getToCountry(), rsp.getNumArmies());
                }
            } else {
                //because fortification is not required, a null response is taken to mean that the player declines the opportunity
                attempts = RiskConstants.MAX_ATTEMPTS;
            }
        }
    }

    /**
     * Allows a Player the chance to perform initial allocation of troops at the
     * beginning of a game.
     *
     * @param player
     * @param reinforcements the number of troops allowed to the player for
     * allocation
     * @return the layout of the Player's countries + how many troops are
     * stationed at each country.
     */
    protected ReinforcementResponse tryInitialAllocation(Player player, int reinforcements) {
        try {
            ReinforcementResponse rsp = player.getInitialAllocation(this.map.getReadOnlyCopy(), reinforcements);
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            //e.printStackTrace();
            return null;
        }
    }

    protected CardTurnInResponse tryTurnIn(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, boolean turnInRequired) {
        try {
            CardTurnInResponse rsp = player.proposeTurnIn(this.map.getReadOnlyCopy(), cardSet, oppCards, turnInRequired);
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            //e.printStackTrace();
            return null;
        }
    }

    protected ReinforcementResponse tryReinforce(Player player, Map<String, Integer> oppCards, int reinforcements) {
        try {
            ReinforcementResponse rsp = player.reinforce(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements);
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            //e.printStackTrace();
            return null;
        }
    }

    protected AttackResponse tryAttack(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
        try {
            AttackResponse rsp = player.attack(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            e.printStackTrace();
            System.out.println(ERROR + "tryAttack " + e);
            return null;
        }
    }

    protected DefendResponse tryDefend(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
        try {
            DefendResponse rsp = player.defend(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            //e.printStackTrace();
            return null;
        }
    }

    protected AdvanceResponse tryAdvance(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
        try {
            AdvanceResponse rsp = player.advance(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            //e.printStackTrace();
            return null;
        }
    }

    protected FortifyResponse tryFortify(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards) {
        try {
            FortifyResponse rsp = player.fortify(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
            validatePlayerName(player);
            return rsp;
        } catch (RuntimeException | PlayerEliminatedException e) {
            //e.printStackTrace();
            return null;
        }
    }

    protected boolean validateInitialAllocation(Map<Country, Integer> allocation, String playerName, int armies) {
        Map<Country, Boolean> allocatedCheck = new HashMap<Country, Boolean>();
        for (Country country : RiskUtils.getPlayerCountries(this.map, playerName)) {
            allocatedCheck.put(country, false);
        }
        for (Country country : allocation.keySet()) {
            if (!allocatedCheck.containsKey(country)) {
                return false;
            } else if (allocation.get(country) < 1) {
                return false;
            } else {
                allocatedCheck.put(country, true);
                armies -= allocation.get(country);
            }
        }
        for (Boolean check : allocatedCheck.values()) {
            if (!check) {
                return false;
            }
        }
        return armies == 0;
    }

    protected void allocateArmies(String playerName, Map<Country, Integer> allocation, int reinforcements) {
        writeLogLn(true, playerName + " reinforcing with " + reinforcements + " armies.");
        for (Map.Entry<Country, Integer> entry : allocation.entrySet()) {
            this.mapSetCountryArmyCount(entry.getKey(), entry.getValue());
            writeLogLn(true, entry.getValue() + " " + entry.getKey().getName());
        }
        writeLogLn(true, EVENT_DELIM);
    }

    protected int getCardTurnIn(Player currentPlayer, Map<String, Integer> oppCards) throws PlayerEliminatedException {
        int cardBonus = 0;
        int attempts = 0;
        boolean valid = false;
        boolean turnInRequired = oppCards.get(currentPlayer.getName()) >= RiskConstants.FORCE_TURN_IN;
        while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
            CardTurnInResponse rsp = tryTurnIn(currentPlayer, createCardSetCopy(currentPlayer.getName()), oppCards, turnInRequired);
            if (rsp != null) {
                if (valid = CardTurnInResponse.isValidResponse(rsp, this.playerToCardDeckHMap.get(currentPlayer.getName()))) {
                    cardBonus = RiskConstants.advanceTurnIn();
                    writeLogLn(true, currentPlayer.getName() + " turned in cards for " + cardBonus + " additional reinforcements!");
                    if (rsp.getBonusCountry() != null) {
                        if (this.map.getCountryOwner(rsp.getBonusCountry()).equals(currentPlayer.getName())) {
                            this.mapAddToCountryArmyCount(rsp.getBonusCountry(), RiskConstants.BONUS_COUNTRY_ARMIES);
                        }
                    }
                    for (Card card : rsp.getCards()) {
                        this.playerToCardDeckHMap.get(currentPlayer.getName()).remove(card);
                        this.deck.addLast(card);
                    }
                }
            } else {
                //if a turn-in is not required, a null response is taken as the player declining
                valid = !turnInRequired;
            }
            attempts++;
        }
        if (!valid && turnInRequired) {
            eliminate(currentPlayer, null, "You were required to turn in cards this turn, and you failed to do so.");
        } else if (crossbar.isHumanEndingGame()) {
            eliminate(currentPlayer, null, "The player is opting out of the game altogether. Have a good day, buddy.");
        }
        return cardBonus;
    }

    protected Collection<Card> createCardSetCopy(String playerName) {
        Collection<Card> copy = new ArrayList<Card>();
        for (Card card : this.playerToCardDeckHMap.get(playerName)) {
            copy.add(new Card(card.getType(), card.getCountry()));
        }
        return copy;
    }

    protected Player getOwnerObject(Country country) {
        String playerName = this.map.getCountryOwner(country);
        return getPlayerObject(playerName);
    }

    protected Player getPlayerObject(String playerName) {
        for (Player player : this.playerNameToPlayerObjHMap.values()) {
            if (player.getName().equals(playerName)) {
                return player;
            }
        }
        return null;
    }

    protected Map<String, Integer> getPlayerCardCounts() {
        Map<String, Integer> playerCardCounts = new HashMap<String, Integer>();
        for (String playerName : this.playerNameToPlayerObjHMap.keySet()) {
            playerCardCounts.put(playerName, this.playerToCardDeckHMap.get(playerName).size());
        }
        return playerCardCounts;
    }

    protected void loadDeck() {
        writeLogLn(true, "Building deck...");
        List<Card> newDeck = new ArrayList<Card>();
        int i = 0;
        for (Country country : Country.values()) {
            newDeck.add(new Card(RiskConstants.REG_CARD_TYPES[i % RiskConstants.REG_CARD_TYPES.length], country));
            i++;
        }
        for (i = 0; i < RiskConstants.NUM_WILD_CARDS; i++) {
            newDeck.add(new Card(RiskConstants.WILD_CARD, null));
        }
        shuffleCards(newDeck);
        this.deck = new LinkedList<Card>(newDeck);
    }

    protected void shuffleCards(List<Card> cardList) {
        int j;
        Card temp;
        for (int i = 0; i < 2 * cardList.size(); i++) {
            j = rand.nextInt(cardList.size());
            temp = cardList.get(i % cardList.size());
            cardList.set(i % cardList.size(), cardList.get(j));
            cardList.set(j, temp);
        }
    }

    protected boolean loadPlayers(String players) {
        if (workingMode == LOADED_GAME_MODE) {
            return false;
        }
        writeLogLn(true, "Loading players...");
        this.playerNameToPlayerObjHMap = Collections.synchronizedMap(new HashMap<String, Player>());
        if (players == null) {
            players = RiskConstants.DEFAULT_PLAYERS;
        }

        List<Player> playerList = PlayerFactory.getPlayersFromString(players);

        FXUIPlayer.setCrossbar(FXUIGameMaster.crossbar);

        for (Player player : playerList) {
            this.playerNameToPlayerObjHMap.put(player.getName(), player);
        }

        this.players = new ArrayList<String>(this.playerNameToPlayerObjHMap.keySet());
        this.allPlayers = new ArrayList<String>(this.playerNameToPlayerObjHMap.keySet());

        shufflePlayers(this.players);//choose a random turn order

        this.playerToCardDeckHMap = Collections.synchronizedMap(new HashMap<String, Collection<Card>>());

        for (Player player : this.playerNameToPlayerObjHMap.values()) {
            this.playerToCardDeckHMap.put(player.getName(), new ArrayList<Card>());
        }

        if (this.players.size() < RiskConstants.MIN_PLAYERS || this.players.size() > RiskConstants.MAX_PLAYERS) {
            return false;
        } else {
            writeLogLn(true, "Players:");
            for (String playerName : this.players) {
                writeLogLn(true, playerName);
            }
            writeLogLn(true, EVENT_DELIM);
            return true;
        }
    }

    protected void shufflePlayers(List<String> playerList) {
        int j;
        String temp;
        for (int i = 0; i < 2 * playerList.size(); i++) {
            j = rand.nextInt(playerList.size());
            temp = playerList.get(i % playerList.size());
            playerList.set(i % playerList.size(), playerList.get(j));
            playerList.set(j, temp);
        }
    }

    //only allocates unowned countries
    protected void allocateUnownedCountries() {
        if (this.players.size() > 0) {
            writeLogLn(true, "Re-allocating eliminated player's countries...");
            for (Country country : Country.values()) {
                if (map.getCountryOwner(country) == null) {
                    mapSetCountryOwner(country, this.playerNameToPlayerObjHMap.get(this.players.get(allocationIdx % this.players.size())).getName());
                    if (this.round > 0) {
						//If these countries are being eliminated during a game,
                        //it is due to a player being eliminated by the Master,
                        //and so the re-allocated countries must be occupied.
                        mapSetCountryArmyCount(country, 1);
                    }
                    allocationIdx++;
                }
            }
        }
    }

    //allocates ALL countries on map
    protected void allocateMap() {
        if (this.players.size() > 0) {
            writeLogLn(true, "Allocating countries...");
            for (Card card : this.deck) {
                if (!card.getType().equals(RiskConstants.WILD_CARD)) {
                    mapSetCountryOwner(card.getCountry(), this.playerNameToPlayerObjHMap.get(this.players.get(allocationIdx % this.players.size())).getName());
                    allocationIdx++;
                }
            }
        }
    }

    protected void eliminate(Player loser, Player eliminator, String reason) throws PlayerEliminatedException {
        if (this.playerNameToPlayerObjHMap.containsKey(loser.getName())) {
            writeLogLn(true, loser.getName() + " Eliminated! " + reason);
            System.out.println(loser.getName() + " Eliminated! " + reason);
            displayExtendedMessage(loser.getName() + " Eliminated! " + reason);
            for (Country country : Country.values()) {
                if (map.getCountryOwner(country).equals(loser.getName())) {
                    if (eliminator != null) {
                        mapSetCountryOwner(country, eliminator.getName());
                    } else {
                        mapSetCountryOwner(country, null);
                    }
                }
            }
            if (eliminator != null) {
                for (Card card : this.playerToCardDeckHMap.get(loser.getName())) {
                    this.playerToCardDeckHMap.get(eliminator.getName()).add(card);
                }
                this.playerToCardDeckHMap.get(loser.getName()).clear();
            }
            this.players.remove(loser.getName());
            this.playerNameToPlayerObjHMap.remove(loser.getName());
            allocateUnownedCountries();
            throw new PlayerEliminatedException(loser.getName() + " Eliminated! " + reason);
        } else {
            System.out.println(WARN + "eliminate() :: this.playerMap does not contain so-called 'loser'");
        }
    }

    /**
     * The game, by default, creates a log file containing all the moves made
     * by participants. This log file can be used at a later time to review how
     * the game progressed using the associated LogPlayer mini-app. This is the
     * method used to insert each status line into the log, assuming logging
     * has been enabled. 
     * 
     * If logging has been enabled, the log file will already
     * have been opened, and this method will write directly to the file. Else,
     * this method assumed logging has been disabled and bypasses attempts to 
     * write to this file. However, there is also an ability to write these
     * lines to an internal log cache, which can be written as part of game saves
     * (SavePoints), which can be set at each call.
     * This log cache is also used by the game to display the game's status
     * in the built-in log text viewer.
     * @param mirrorToInternalCache "true": save the line to the internal cache
     * 		in case the line of text must be used to redisplay/populate the log.
     * @param line the line of text to add to the log and/or cache.
     */
    protected void writeLogLn(boolean mirrorToInternalCache, String line) {
        if (mirrorToInternalCache) {
            internalLogCache.add(line);
        }
        if (this.log != null) {
            try {
                this.log.write(line + "\r\n");
                this.log.flush();
            } catch (IOException e) {
                System.out.println(ERROR + "Error writing log: " + e);
            }
        }
    }
    
    /**
     * As part of the logging process, select logged lines are also stored
     * in an internal cache, to allow game saves to restore log files
     * from scratch (and to ensure some sort of log is kept even if logging
     * is disabled). However, it is necessary to clear this internal cache
     * when, say, starting a new game without restarting the application.
     * (Restoring a save overwrites the contents of the internal log cache).
     * This is the method to use to do that.
     */
    protected static void resetInternalLogCache(){
    	if(FXUIGameMaster.internalLogCache != null){
    		FXUIGameMaster.internalLogCache.clear();
    	}
    }

    protected void writeStatsLn() {
        if (this.stats != null) {
            try {
                stats.write(this.turnCount + " " + this.round + " ");
                for (String playerName : this.players) {
                    //count player's countries
                    stats.write(RiskUtils.getPlayerCountries(this.map, playerName).size() + " ");
                    //count player's armies
                    stats.write(RiskUtils.countPlayerArmies(this.map, playerName, null) + " ");
                }
                stats.write("\r\n");
                this.stats.flush();
            } catch (IOException e) {
                System.out.println(ERROR + "Error writing statistics: " + e);
            }
        }
    }

    /**
     * Used to show list of players at top edge of UI. Due to threading
     * requirements, currently wraps up a secondary method to accomplish this
     * task.
     * 
     * Please ensure players exist BEFORE calling this method.
     */
    private void representPlayersOnUI() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                representPlayersSubroutine();
            }
        });
    }

    /**
     * Helper method for representPlayersOnUI.
     */
    private void representPlayersSubroutine() {
        try {
            //clears the old display of players
            if (this.playerDisplay != null) {
                FXUIGameMaster.mainWindowPane.getChildren().remove(this.playerDisplay);
            }
            if(this.playerToIndicatorHMap == null){
            	this.playerToIndicatorHMap = Collections.synchronizedMap(new HashMap<String, Node[]>());
            }
            else{
            	this.playerToIndicatorHMap.clear();
            }

            ArrayList<Color> colors = new ArrayList<Color>();
            colors.add(Color.WHITE);
            colors.add(Color.AQUA);
            colors.add(Color.RED);
            colors.add(Color.GREENYELLOW);
            colors.add(Color.CORAL);
            colors.add(Color.VIOLET);
            this.playerColorMap = Collections.synchronizedMap(new HashMap<String, Color>());
            int i = -1;

            HBox namesOfPlayers = new HBox(40);
            namesOfPlayers.setLayoutX(50);
            namesOfPlayers.setLayoutY(5);

            if (this.playerNameToPlayerObjHMap == null || this.playerNameToPlayerObjHMap.size() == 0) {
                System.out.println(ERROR + "Player map not populated; please fix logic!");
            }
            for (String playerName : this.playerNameToPlayerObjHMap.keySet()) {
            	VBox playerBox = new VBox();
            	playerBox.setAlignment(Pos.CENTER);
                this.playerColorMap.put(playerName, colors.get(++i % colors.size()));
                Text txt = new Text("#" + playerName.toLowerCase());
                txt.setFont(Font.font("Verdana", FontWeight.THIN, FXUIGameMaster.DEFAULT_PLAYER_NAME_FONT_SIZE));
                txt.setFill(colors.get((i) % colors.size()));
                txt.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {
                        if (currentPlayer != null) {
                            flashPlayerCountries(playerName);
                        }
                    }
                });
                
                Color colorOfArc = this.playerColorMap.get(playerName);
                
                Arc arcIndicator = new Arc();
                arcIndicator.setCenterX(50.0f);
                arcIndicator.setCenterY(50.0f);
                arcIndicator.setRadiusX(25.0f);
                arcIndicator.setRadiusY(25.0f);
                arcIndicator.setStartAngle(55.0f);
                arcIndicator.setLength(70.0f);
                arcIndicator.setType(ArcType.CHORD);
                arcIndicator.setStroke(colorOfArc);
                arcIndicator.setFill(colorOfArc);
                GaussianBlur gBlur = new GaussianBlur(4);
                Glow gGlow = new Glow(1.0d);
                gGlow.setInput(gBlur);
            	arcIndicator.setEffect(gGlow);
                
                
                playerBox.getChildren().addAll(txt, arcIndicator);
                namesOfPlayers.getChildren().add(playerBox);
                this.playerToIndicatorHMap.put(playerName, new Node[] {arcIndicator, txt});
            }
            FXUIGameMaster.mainWindowPane.getChildren().add(namesOfPlayers);
            this.playerDisplay = namesOfPlayers;
        } catch (RuntimeException e) {
        	System.out.println(e.toString() + " _RPSub_ " + e.getMessage());
        	e.printStackTrace();
        }
    }
    
    /**
     * Applies an indicator to the UI to indicate the current player, or 
     * removes the indicator from all (in the event of a game ending, etc)
     * @param playerToHilite NULL if you want to remove indicator from all players, or
     * the applicable Player which is the current player.
     * @return -1 if there was a failure (inability to set), 0 if old indicators
     * were cleared and new indicator was successfully applied, 
     * 1 if all indicators cleared but no new indicator applied.
     */
    private int highlightCurrentPlayer(Player playerToHilite){
    	if(this.playerToIndicatorHMap == null || 
    			this.playerToIndicatorHMap.size() == 0)
    	{
    		return -1;
    	}
    	final AtomicBoolean doneUpdating = new AtomicBoolean(false);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try{
					for(Entry<String, Node[]> entry : playerToIndicatorHMap.entrySet()) {
						if(playerToHilite != null && entry.getKey() == playerToHilite.getName()){
							entry.getValue()[0].setVisible(true);
							Text playerText = (Text) entry.getValue()[1];
							playerText.setFont(Font.font("Verdana", FontWeight.THIN, FXUIGameMaster.DEFAULT_PLAYER_NAME_FONT_SIZE));
						}
						else{
							entry.getValue()[0].setVisible(false);
							Text playerText = (Text) entry.getValue()[1];
							playerText.setFont(Font.font("Verdana", FontWeight.THIN, 16));
						}
					}
				}
				catch(Exception e){
					e.printStackTrace();
					/*
					* Try again...
					*/
					highlightCurrentPlayer(playerToHilite);
				}
				finally{
					doneUpdating.set(true);
				}
			}
		});
    	
		while(!doneUpdating.get()){
			RiskUtils.sleep(50);
		}
    	if(playerToHilite != null){
    		return 0;
    	}
    	return 1;
    }

    /**
     * The requisite main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) {
    	launch(FXUIGameMaster.class, args);
        if (FXUIGameMaster.priGameLogicThread != null) {
            try {
                System.out.println("Full exit?");
                FXUIGameMaster.priGameLogicThread.join(5000);
                System.out.println("Full exit.");
            } catch (InterruptedException e) {
            	System.out.println("Someone somewhere along the way held up the "
            			+ "exit process.");
            	e.printStackTrace();
            } catch (Exception e){
                System.out.println("Exception propagated to main():");
                e.printStackTrace();
            }
        }

    }

    /**
     * Creates a secondary thread to run game logic (to allow blocking without
     * blocking the UI. Relies on the caller to detect if a game is already in
     * progress.
     *
     * @return returns "true" so long as the game thread was started
     * successfully; "false" otherwise.
     */
    public boolean createGameLogicThread() {
        if(FXUIGameMaster.priGameLogicThread != null){
            System.out.println("Primary logic thread invoked, but is already"
                    + " running. Skipping duplicate invocation.");
            return false;
        }
        Runnable gameCode = new Runnable() {
            @Override
            public void run() {
            	/*
            	 * Set the default to "BOTS vs HUMAN"
            	 */
            	FXUIGameMaster.runBotsOnly = false;
            	
            	/*
            	 * Set the date and time of this newly running game.
            	 */
            	FXUIGameMaster.gameStartTime = new Date();
            	
            	/*
            	 * Allow the user to select game details...
            	 */
                int stateOut = displayGameSelector();
                
                //Depending on what's been selected there, run or don't run.
                if (stateOut != IDLE_MODE) {
                	resetInternalLogCache();
                	setButtonAvailability();
                    runGameAndDisplayVictor();
                }
                FXUIGameMaster.priGameLogicThread = null;
            }
        };
        Thread gameThread = new Thread(gameCode);
        gameThread.setName("createGameLogicThread");
        gameThread.setDaemon(true);
        gameThread.start();
        if (gameThread.isAlive()) {
            FXUIGameMaster.priGameLogicThread = gameThread;
            animateGameRunningIndicator();
            return true;
        } else {
            return false;
        }
    }

    public void runGameAndDisplayVictor() {
        try {
            // TODO add support for selecting types of players, which is set during call to initializeFXGMClass
            HashMap<String, Integer> winLog = new HashMap<String, Integer>();
            RiskConstants.SEED = 1;
            int numGames = 1;
            for (int i = 0; i < numGames; i++) {
                RiskConstants.resetTurnIn();
                PlayerFactory.resetPlayerCounts();
                boolean doWeLog = (!forceLoggingIsIndeterminate && forceEnableLogging) || (forceLoggingIsIndeterminate && i == numGames - 1 ? LOGGING_ON : LOGGING_OFF);
                /**
                 * Be aware: "DEFAULT_PLAYERS" means various types of bots ONLY!
                 * If there is ever a future version where this is untrue,
                 * then this method of setting up players will be invalid!
                 */
                if(FXUIGameMaster.runBotsOnly){
                	initializeFXGMClass("Countries.txt", RiskConstants.DEFAULT_PLAYERS, doWeLog);
                }
                else{
                	initializeFXGMClass("Countries.txt", RiskConstants.DEFAULT_PLAYERS + "," + PlayerFactory.FXUIAsk, doWeLog);
                }

                System.out.print((i + 1) + " - ");
                
                /*
                 * Let the player/bots actually play the game!
                 */
                String victor = begin();

                System.out.println(INFO + "Hi user!!! game execute was successfully!!!!"); //yes very successfully!!!!
                if (victor != null) {
                    setPlayStatus("I D L E (game ended due to victory)");
                    if (!winLog.containsKey(victor)) {
                        winLog.put(victor, 0);
                    }
                    winLog.put(victor, winLog.get(victor) + 1);
                }
            }
            if (!FXUIGameMaster.fullAppExit) {
                for (Map.Entry<String, Integer> entry : winLog.entrySet()) {
                    System.out.println(entry.getKey() + " had a win percentage of " + 100.0 * entry.getValue() / numGames + "%");
                }
            }
        } catch (RuntimeException e) {
            System.out.println(ERROR + "low-level game runtime error:: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Does a tiny bit of initialization on the map's internal structures,
     * without setting up/displaying players or other user-facing info. Replaces
     * GameMaster() from the original GameMaster class, since JavaFX would
     * otherwise leave it unused.
     *
     * @param mapFile
     * @param playerFile
     * @param logSwitch
     * @throws IOException
     */
    public void initializeFXGMClass(String mapFile, String players, boolean logSwitch) {
        for (Country country : Country.values()) {
            stringCountryRepresentation.put(country.getName(), country);
        }

        this.round = 0;
        this.turnCount = 0;
        if (rand == null) {
            rand = new Random(RiskConstants.SEED);
        }

        FXUIGameMaster.activeSaveData = new SavePoint();

        FXUIGameMaster.loggingEnabled = logSwitch;
        if (FXUIGameMaster.loggingEnabled == LOGGING_ON) {
            System.out.println(INFO + "Trying to enable logging...");
            try {
                this.stats = new FileWriter(STATSFILE);
                this.log = new FileWriter(LOGFILE);
                System.out.println(INFO + "Logging enabled!");
            } catch (IOException e) {
                FXUIGameMaster.loggingEnabled = LOGGING_OFF;
                setSubStatus("log disabled (I/O error)");
                System.out.println(WARN + "Logging DISABLED!");
            }
        } else {
            System.out.println(INFO + "Logging disabled.");
            this.log = null;
            this.stats = null;
        }

        writeLogLn(true, "Loading map from " + mapFile + "...");
        if (starterMap == null) {
            starterMap = new RiskMap();
        }

        this.map = starterMap.getCopy();
        loadDeck();
        FXUIGameMaster.desiredPlayersForGame = players;
    }

    /**
     * Populates the UI with the countries using predefined window locations.
     *
     * @param nodeFile
     */
    private void loadCountryNodesForUIDisplay(String nodeFile) {
        try {
            if (nodeFile != null) {
                this.textNodeMap = new HashMap<String, Text>();
                File fileRepresentation = new File(nodeFile);
                //basic check for existence of country list file
                if (!fileRepresentation.exists()) {
                    Scanner reader = new Scanner(TextNodes.nodes);
                    while (reader.hasNext()) {
                        int nextX = reader.nextInt();
                        int nextY = reader.nextInt();
                        String nextCountry = reader.nextLine().trim();
                        Text txt = new Text(nextX, nextY, nextCountry + "\n0");
                        txt.setFill(Color.BLUEVIOLET);
                        txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                        this.textNodeMap.put(nextCountry, txt);
                        FXUIGameMaster.mainWindowPane.getChildren().add(txt);
                    }
                    reader.close();
                } else {
                    Scanner reader = new Scanner(fileRepresentation);
                    while (reader.hasNext()) {
                        int nextX = reader.nextInt();
                        int nextY = reader.nextInt();
                        String nextCountry = reader.nextLine().trim();
                        Text txt = new Text(nextX, nextY, nextCountry + "\n0");
                                                //enable clicking/tapping on the country.
                        //beta feature added 2015-10-10 as a TODO
                        txt.setOnMouseClicked(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent t) {
                                System.out.println(nextCountry);
                            }
                        });
                        txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
                        txt.setStroke(Color.BLACK);
                        txt.setFill(Color.ANTIQUEWHITE);
                        //txt.setStrokeType(StrokeType.OUTSIDE);
                        //txt.setStrokeLineCap(StrokeLineCap.SQUARE);
                        //txt.setSmooth(true);
                        txt.setEffect(new Glow(1.0d));
                        this.textNodeMap.put(nextCountry, txt);
                        FXUIGameMaster.mainWindowPane.getChildren().add(txt);
                    }
                    reader.close();
                }
            }
        } catch (RuntimeException e) {
            setSubStatus("generic runtime error...");
            e.printStackTrace();
            FXUIGameMaster.displayExtendedMessage(e.getStackTrace().toString());
        } catch (FileNotFoundException e) {
            setSubStatus("couldn't open file...");
            e.printStackTrace();
            FXUIGameMaster.displayExtendedMessage(e.getStackTrace().toString());
        }
    }

    /*
	 * And so begins the complex setup to create a stepping refresh cycle that 
	 * updates elements one by one. It's to be on a clock (with each element
	 * update being one cycle, and the clock ending when the entire map 
	 * refresh is complete).
     * 
     */
    /**
     * Main entry method to prompt a refresh of the countries and their counts
     * in the main FXUI window. Has no bearing on the secondary dialogs. If the
     * game is being "exited", or the current player is not a "human" (and the 
     * game is not bots only), the refresh is as instantaneous as possible. 
     * Else, internal decision logic will request a slower refresh, to provide 
     * for more easily detected changes.
     *
     * @param guaranteeRefresh if "true", will refresh (with a dynamic decision
     * on if it's instant or staggered updating). If "false", will skip updating
     * altogether for the majority of the time.
     */
    public void refreshUIElements(boolean guaranteeRefresh) {
        if (this.players == null || this.players.size() == 0) {
            System.out.println(INFO + "FXUIGM - Player count mismatch; Delaying country refresh...");
            return;
        }
        if (this.playerColorMap == null || this.playerColorMap.size() == 0) {
            System.out.println(INFO + "FXUIGM - PlayerColorMap size mismatch; Delaying country refresh...");
            return;
        }
        final int timeToWaitBetweenElements = 14;
        final long blinkDelay = 100;
            
    	Thread clockedUIRefreshThreadA = new Thread(null, new Runnable() {
            @Override
            public void run() {
            	while (FXUIGameMaster.countriesToUpdateCount.size() > 0 && !FXUIGameMaster.endGame){
            		try{
	            		performStepOfRefreshProcess(FXUIGameMaster.countriesToUpdateCount.remove(0),true,blinkDelay);
	            		if (FXUIGameMaster.endGame) {
	                        break;
	                    }
            		}
            		catch(Exception nsee){
            			System.out.println(FXUIGameMaster.ERROR + "TA.NSEE.list size? : " + FXUIGameMaster.countriesToUpdateCount.size());
            			break;
            		}
            		//and the sleep to create the pause between updates...
                    if (timeToWaitBetweenElements > 0) {
                        RiskUtils.sleep(timeToWaitBetweenElements);
                    }
            	}
            }
	    }, "refreshUIElements clockedRefreshA");
        
    	Thread clockedUIRefreshThreadB = new Thread(null, new Runnable() {
            @Override
            public void run() {
            	while (FXUIGameMaster.countriesToUpdateOwner.size() > 0 && !FXUIGameMaster.endGame){
            		try{
	            		performStepOfRefreshProcess(FXUIGameMaster.countriesToUpdateOwner.remove(0),false,blinkDelay);
	            		if (FXUIGameMaster.endGame) {
	                        break;
	                    }
	            	}
	        		catch(Exception nsee){
	        			System.out.println(FXUIGameMaster.ERROR + "TB.NSEE.list size? : " + FXUIGameMaster.countriesToUpdateOwner.size());
	        			break;
	        		}
            		//and the sleep to create the pause between updates...
                    if (timeToWaitBetweenElements > 0) {
                        RiskUtils.sleep(timeToWaitBetweenElements);
                    }
                }
            }
	    }, "refreshUIElements clockedRefreshB");
        clockedUIRefreshThreadB.setDaemon(true);
        clockedUIRefreshThreadA.setDaemon(true);
        clockedUIRefreshThreadA.start();
        clockedUIRefreshThreadB.start();
    }
    

    /**
     * As part of the clocked refresh cycle, updates the visual state of a
     * singular country, passed in as a param.
     *
     * @param country the country whose data is to be refreshed on the main map.
     */
    private void performStepOfRefreshProcess(Country country, boolean updateCount, long blinkDelay) {
    	if(country == null){
    		System.out.println("Country reported as NULL. Help?");
    		return;
    	}
    	//If update the count, update the text. And blink, where possible.
    	if(updateCount){
    		Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	textNodeMap.get(country.getName()).setText(country.getName());
                }});
    		RiskUtils.runLaterWithDelay(blinkDelay, new Runnable() {
                @Override
                public void run() {
                	textNodeMap.get(country.getName()).setText(country.getName() + "\n" + map.getCountryArmies(country));
                }
            });
    	}
    	//If update Owner, update Fill...No blink necessary.
    	else{
    		Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	textNodeMap.get(country.getName()).setFill(playerColorMap.get(map.getCountryOwner(country)));
            }});
    	}
        
    }
    
    /**
     * Used to help highlight a given player's countries on the main UI. Does
     * each country individually, then all countries at once. Repeats ??? times.
     *
     * @param playerName the String representation of the player in question.
     */
    private void flashPlayerCountries(String playerName) {
        final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, playerName);
        final int gameModeWhenStarted = FXUIGameMaster.workingMode;
        final long timeDeltaMS = 220;
        final int totalCycles = 2;
        Runnable clockedBlinkTask = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < totalCycles && !FXUIGameMaster.endGame; i++) {
                    /*
                     * Do all countries at once. Relatively speaking, anyway.
                     */
                    for (int kLoop = 0; kLoop < totalCycles && gameModeWhenStarted == FXUIGameMaster.workingMode; kLoop++) { //all countries at once
                        singleBlinkTypeB(0.25d, timeDeltaMS, myCountries);
                        if (FXUIGameMaster.endGame) {
                            break;
                        }
                        singleBlinkTypeB(0.5d, timeDeltaMS, myCountries);
                        if (FXUIGameMaster.endGame) {
                            break;
                        }
                        singleBlinkTypeB(0.75d, timeDeltaMS, myCountries);
                        if (FXUIGameMaster.endGame) {
                            break;
                        }
                        singleBlinkTypeB(1.0d, timeDeltaMS, myCountries);
                        if (FXUIGameMaster.endGame) {
                            break;
                        }
                        singleBlinkTypeB(0.75d, timeDeltaMS, myCountries);
                    }

                    /*
                     * Reset the display, and if we must cancel the animation,
                     * break out of the loop.
                     */
                    singleBlinkTypeB(1.0d, 0, myCountries);
                    if (FXUIGameMaster.endGame || gameModeWhenStarted != FXUIGameMaster.workingMode) {
                        break;
                    }

                    /*
                     * Then do each country individually...
                     */
                    for (Country country : myCountries) { //each country separately
                    	if(gameModeWhenStarted != FXUIGameMaster.workingMode){
                    		break;
                    	}
                        for (int nLoop = 0; nLoop < totalCycles - 1 
                        		&& !FXUIGameMaster.endGame 
                        		&& gameModeWhenStarted != FXUIGameMaster.workingMode;
                        		nLoop++)
                        {
                            singleBlinkTypeA(0.1d, timeDeltaMS, country);
                            singleBlinkTypeA(0.5d, timeDeltaMS, country);
                            singleBlinkTypeA(1.0d, timeDeltaMS, country);
                        }
                    }
                    /*
                     * If we should stop the animation: break out of loop after
                     * resetting the display.
                     */
                    singleBlinkTypeB(1.0d, 0, myCountries);
                    if (FXUIGameMaster.endGame || gameModeWhenStarted != FXUIGameMaster.workingMode) {
                        break;
                    }

                }
            }
        };
        Thread clockedBlinkThread = new Thread(null, clockedBlinkTask, "flashPlayerCountry clockedBlinkTask");
        clockedBlinkThread.setDaemon(true);
        clockedBlinkThread.start();
    }

    /**
     * Sets the opacity and outline color for one (1) given country to the given
     * input values (1 opacity setting, one outline color) after a sleep of some
     * milliseconds.
     *
     * @param opacitySetting target opacity setting, as a double. Range: from
     * 0.0 to 1.0.
     * @param timeDeltaMS time to wait before switching opacity and outline
     * values, in milliseconds.
     * @param country single country whose opacity and outline color is to be
     * changed.
     * @param colorValue target color value for the single country
     */
    private void singleBlinkTypeA(double opacitySetting, long timeDeltaMS, Country country) {
        RiskUtils.sleep(timeDeltaMS);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textNodeMap.get(country.getName()).setOpacity(opacitySetting);
            }
        });
    }

    /**
     * Sets the opacity value for multiple (>=1) countries to one (1) input
     * value after a wait/sleep of some milliseconds.
     *
     * @param opacitySetting target opacity setting for the countries' labels,
     * as a double. Range: from 0.0 to 1.0
     * @param timeDeltaMS time to wait before switching opacity values, in
     * milliseconds
     * @param myCountries countries on the UI to update
     */
    private void singleBlinkTypeB(double opacitySetting, long timeDeltaMS, Set<Country> myCountries) {
        RiskUtils.sleep(timeDeltaMS);
        for (Country country : myCountries) {
            if (FXUIGameMaster.endGame) {
                break;
            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    textNodeMap.get(country.getName()).setOpacity(opacitySetting);
                }
            });
        }
    }

    /**
     * Get your life in the form of a game! This method is the point at which
     * the JavaFX items are populated on the main map screen.
     *
     * Extra dialogs are further prompted elsewhere, depending on their
     * needs/uses. (In this file is the method to present an exit confirmation
     * dialog, as is the class representing the About dialog.)
     * 
     * This is the method required by JavaFX to initiate display of content.
     * JavaFX provides a Stage (colloquially equivalent to a window), and we
     * must place content in that Stage. JavaFX will continually check the state
     * of this Stage, and if it is closed, by default JavaFX will close the app.
     */
    @Override
    public void start(final Stage primaryStage) throws Exception {
        System.out.println("Preparing window...");
        final About nAbout = new About();
        mainWindowPane = new Pane();
        mainWindowPane.setPrefSize(FXUIGameMaster.DEFAULT_CONTENT_WIDTH, FXUIGameMaster.DEFAULT_CONTENT_HEIGHT);
        mainWindowPane.setStyle("-fx-background-color: black");
        /*
         * The pulse line for the bottom of the screen.
         */
        double strokeThicknessOfLines = 4d;
        Color colorOfLines = Color.WHITE;
        Line musicPulseIndicator = new Line(0,DEFAULT_CONTENT_HEIGHT-10,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT-10);
        //Rectangle accentLine = new Rectangle(4,4,DEFAULT_CONTENT_WIDTH-8,DEFAULT_CONTENT_HEIGHT-8);
        musicPulseIndicator.setStrokeWidth(strokeThicknessOfLines);
        musicPulseIndicator.setStroke(colorOfLines);
        musicPulseIndicator.setFill(Color.TRANSPARENT);
        FXUIAudio.setVisualIndicator(musicPulseIndicator);
        
        Line musicPulseIndicatorS = new Line(0,0,0,40);
        musicPulseIndicatorS.setStrokeWidth(strokeThicknessOfLines);
        musicPulseIndicatorS.setStroke(colorOfLines);
        musicPulseIndicatorS.setFill(colorOfLines);
        musicPulseIndicator.opacityProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				// TODO Auto-generated method stub
				musicPulseIndicatorS.setOpacity(newValue.doubleValue());
			}
        });

        
        Line activeGameIndic = new Line(0,4,DEFAULT_CONTENT_WIDTH,4);
        activeGameIndic.setStrokeWidth(strokeThicknessOfLines);
        activeGameIndic.setStroke(colorOfLines);
        activeGameIndic.setFill(colorOfLines);
        this.setGameRunningIndicator(activeGameIndic);
        
        Line activeGameIndicS = new Line(0,0,0,40);
        activeGameIndicS.setStrokeWidth(strokeThicknessOfLines);
        activeGameIndicS.setStroke(colorOfLines);
        activeGameIndicS.setFill(colorOfLines);
        activeGameIndic.opacityProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				// TODO Auto-generated method stub
				activeGameIndicS.setOpacity(newValue.doubleValue());
			}
        });
        
        /*
         * We set the image in the pane based on whether there was an error or not.
         * If there was an error, it'll be changed later.
         */
        //Facilitate checking for errors...
        String errorTextInitialContents = "currently";

        /*
         * Attempt to load the background image (the map).
         * If fail, encourage user to exit the app since it won't work too well.
         */
        ImageView backgroundImg = new ImageView();
        try {
            Image imageOK = new Image(MAP_BACKGROUND_IMG, true);
            backgroundImg.setImage(imageOK);
            backgroundImg.setOpacity(0.7d);
            mainWindowPane.setPrefSize(imageOK.getWidth(), imageOK.getHeight());
            mainWindowPane.getChildren().add(backgroundImg);
            //FXUIAudio.setVisualIndicator(backgroundImg);
        } 
        catch (Exception e) 
        {
            errorTextInitialContents = ERROR + "Couldn't load background map image " + MAP_BACKGROUND_IMG
                    + ".\n(Maybe bad compilation?)\n If you compiled this, check the build source"
                    + " for all required resources, mind any errors encountered while building,"
                    + " and please attempt to rebuild from scratch. "
                    + "\nElse, please report this error.\nDisabling application features...";
            System.out.println(errorTextInitialContents);
            fullAppExit = true;
        }
        /*
         * And the icon...which we don't mind if it fails to load.
         */
        try{
        	ImageView iconIn = new ImageView(new Image("Icon.jpg", 75, 75, true, true, false));
        	iconIn.setLayoutX(DEFAULT_CONTENT_WIDTH-100);
        	iconIn.setLayoutY(DEFAULT_CONTENT_HEIGHT-100);
        	iconIn.setOpacity(0.5d);
        	mainWindowPane.getChildren().add(iconIn);
        }
        catch(Exception e){
        	System.out.println("Logo failed to load. It's OK, tho.");
        }

        //Populate the display with the countries
        loadCountryNodesForUIDisplay("TextNodes.txt");
        

        //now display elements -- status and buttons!
        subStatusTextElement = new Text(errorTextInitialContents);
        subStatusTextElement.setFill(Color.WHITE);
        subStatusTextElement.setFont(Font.font("Verdana", FontWeight.THIN, 20));
        
        mainStatusTextElement = new Text("ready to play!");
        mainStatusTextElement.setFont(Font.font("Verdana", FontWeight.NORMAL, 24));
        mainStatusTextElement.setFill(Color.WHITE);

        //The vertical box to contain the major buttons and status.
        VBox primaryStatusButtonPanel = new VBox(10);
        HBox startButtonPanel = new HBox(0);

        primaryStatusButtonPanel.setAlignment(Pos.CENTER_LEFT);
        primaryStatusButtonPanel.setLayoutX(29);
        primaryStatusButtonPanel.setLayoutY(495);
        
        startButtonPanel.setAlignment(Pos.CENTER_LEFT);
        
        //allow display of extended messages in the main window.
        FXUIGameMaster.extendedMessageDisplay = new Text("-");
        FXUIGameMaster.extendedMessageDisplay.setFont(Font.font("Verdana", FontWeight.NORMAL, 16));
        FXUIGameMaster.extendedMessageDisplay.setFill(Color.WHITE);

        //cache prior messages so we can display in a non-interactive dialog
        extendedMessageCache = new ArrayList<String>();
        extendedMessageCachePos = 0;


        //Button to show the window size options
        Button windowOptions = new Button("Window Options");
        windowOptions.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                mainWindowResizeHandler.showSizeOptions();
            }
        });
        
        //Button to show audio options (volume, etc)
        Button audioOptions = new Button("Audio Options");
        audioOptions.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                audioManager.showAudioOptions(primaryStage.getScene().getWindow());
            }
        });

        //End the current game, but don't close the program.
        Button stopGameBtn = new Button("Bow out.\n(End current game)");
        stopGameBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        //crossbar.signalHumanEndingGame();
                        if(!crossbar.tryCloseCurrentPlayerDialog()){
                        	if(FXUIGameMaster.doYouWantToMakeAnExit(false,0) > 0)
                        		FXUIGameMaster.endGame = true;
                        }
                    }
                });
            }
        });

        //Button to actually start the game
        Button startBtn = new Button("Let's go!!\n(Start/Load game)");
        startBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (workingMode == IDLE_MODE) {
                    createGameLogicThread();
                }
            }

        });
        startButtonPanel.getChildren().addAll(musicPulseIndicatorS,startBtn,activeGameIndicS);

        //Mirrors the functionality of the start button.
        mainWindowPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (workingMode == IDLE_MODE) {
                    createGameLogicThread();
                }
            }
        });

        //your standard About buttons...
        Button tellMe = new Button("About (Basic Info)");
        tellMe.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                nAbout.launch(mainWindowPane.getScene().getWindow(), false);
            }
        });

        //...I said "About buttons". Plural. Yep.	
        Button tellMe2 = new Button("More About (Ver.)");
        tellMe2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                nAbout.more(mainWindowPane.getScene().getWindow());
            }
        });

        //Button allowing you to attempt to force a manual game save, when allowed
        Button saveMe = new Button("save game as...");
        saveMe.setTooltip(new Tooltip("Changes the location where your game is being auto-saved"
                + "\nAND IMMEDIATELY saves to that new location!"));
        saveMe.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (performSave(true)) {
                    saveMe.getTooltip().setText("saved. autosave set to " + saveto_filename
                            + ".\n\nChanges the location where your game is being auto-saved"
                            + "\nAND IMMEDIATELY saves to that new location!");
                } else {
                    saveMe.getTooltip().setText("save failed; try again???"
                            + "\n\nChanges the location where your game is being auto-saved"
                            + "\nAND IMMEDIATELY saves to that new location!");
                }
            }
        });
        saveMe.setDisable(true);

        //Button to launch the logplayer (assuming a previous game has actually taken place)
        Button logPlayback = new Button("open log player.");
        logPlayback.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        LogPlayer.setAsLaunchedFromFXUIGM();
                        new LogPlayer().start(new Stage());
                    }
                });
            }
        });

        //Button to exit the application entirely
        Button exitApp = new Button("Lights out!\n(Exit to desktop)");
        exitApp.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                tryToExit(primaryStage);
            }
        });

		//Checkbox to allow you to set whether you want to have the log file created
        //(Note: the logfile allows you to review the actions taken during the game)
        CheckBox doLogging = new CheckBox("Enable logging?\nauto (yes)");
        doLogging.setTooltip(new Tooltip("YES: Always log (each game overwrites the log of the last game for normal games)\n"
                + "NO: Never log (whatever log file exists will remain untouched)\n"
                + "INDETERMINATE/AUTO: Effectively YES, unless redefined elsewhere.\n"
                + "For game simulations (when available): enabling (setting to YES) may result in a flood of logs!"));
        doLogging.setTextFill(Color.ANTIQUEWHITE);
        doLogging.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (doLogging.isIndeterminate()) {
                    //tell it to do whatever it wants by default
                    forceEnableLogging = false;
                    forceLoggingIsIndeterminate = true;
                    doLogging.setText("Enable logging?\nauto (yes)");
                } else if (doLogging.isSelected()) {
                    //tell it to enable logging
                    forceEnableLogging = true;
                    forceLoggingIsIndeterminate = false;
                    doLogging.setText("Enable logging?\n" + (FXUIGameMaster.loggingEnabled ? "Yes" : "No"));
                } else if (!doLogging.isSelected()) {
                    //tell it to forcefully disable logging
                    forceEnableLogging = false;
                    forceLoggingIsIndeterminate = false;
                    doLogging.setText("Enable logging?\n" + (FXUIGameMaster.loggingEnabled ? "Yes" : "No"));
                }
            }
        });
        doLogging.setIndeterminate(true);

        Button flashCurrCountries = new Button("HIGHLIGHT current player's countries");
        flashCurrCountries.setVisible(false);
        flashCurrCountries.setLayoutX(25);
        flashCurrCountries.setLayoutY(45);
        flashCurrCountries.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (currentPlayer != null) {
                    flashPlayerCountries(currentPlayer.getName());
                }
            }
        });
        
        Button showLogBtn = new Button("Show log contents.");
        showLogBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                showLogContents();
            }
        });
        
        Button showAboutBtn = new Button("Options/About.");
        showAboutBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
            	showOptionsAndAbout(primaryStage.getScene().getWindow(),
                	new Button[]{tellMe, tellMe2, windowOptions, audioOptions});
            }
        });

        /*
         * Add buttons to panel. Tweak these additions depending on whether there was an error
         */
        
        
        if (!fullAppExit) { //if we had no error
            primaryStatusButtonPanel.getChildren().addAll(subStatusTextElement,
            	mainStatusTextElement, startButtonPanel, stopGameBtn, exitApp,
            	showAboutBtn, showLogBtn, saveMe, doLogging, logPlayback,
            	FXUIGameMaster.extendedMessageDisplay);
            mainWindowPane.getChildren().addAll(activeGameIndic, musicPulseIndicator, 
            		flashCurrCountries, primaryStatusButtonPanel);
        } else { //if we had an error
            primaryStatusButtonPanel.getChildren().addAll(mainStatusTextElement, 
            	exitApp);
            mainWindowPane.getChildren().addAll(primaryStatusButtonPanel);
            mainWindowPane.setOnKeyPressed(null);
        }
		//****layout of text & buttons displayed upon launch ends here.***

		/*
		 * Add the layout (and, by extension, its contents) to the Scene.
		 * We'll add the Scene to the Stage (the main window) later.
		 */
        scene = new Scene(mainWindowPane, Color.BLACK);

		//Add buttons to an array, to allow easy enable/disable depending on state.
        //Use the ENUM table "ButtonIndex" to access elements in the array -- and set the targeted capacity.***
        buttonCache = new ArrayList<Node>(ButtonPosEnum.values().length);
        for (int loopIdx = 0; loopIdx < ButtonPosEnum.values().length; ++loopIdx) {
            buttonCache.add(null); //necessary to actually create the slots in the array
        }
		//this presentation preferred to indicate importance of enumeration in ButtonIndex
        //in alternative setups, you could merely do "buttonCache.add(element)" for each individual object.
        buttonCache.set(ButtonPosEnum.BTN_START.ordinal(), startBtn);
        buttonCache.set(ButtonPosEnum.BTN_SAVE.ordinal(), saveMe);
        buttonCache.set(ButtonPosEnum.BTN_HIGHLIGHT.ordinal(), flashCurrCountries);
        buttonCache.set(ButtonPosEnum.CKBX_LOGGING.ordinal(), doLogging);
        buttonCache.set(ButtonPosEnum.BTN_LOG_PLAYBACK.ordinal(), logPlayback);

        //Get the primary window showin', already! (and set the initial size appropriately)
        primaryStage.setTitle("RISK!");
        primaryStage.setScene(scene);
        
        //Get the splash screen queued up, then show the window.
        bootSplashScreen();
        primaryStage.show();

        //enable the game to gracefully support resizing of the window
        enableAutomaticResizingFunctionality(primaryStage);

        //Save this so we can make use of the Stage's information elsewhere.
        FXUIGameMaster.mainStage = primaryStage;

        //In case FXUIPlayer dialogs need help with positioning, tell them where to get it.
        FXUIPlayer.setOwnerWindow(mainWindowPane.getScene().getWindow()); //applies to all human player(s), so now made static.

		/*
		 * Help control what happens when the user tries to exit by telling the app...what...to do.
         * In this case, we're telling it "Yes, we're trying to exit from the main window, so display the appropriate dialog.
         */
        scene.getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                tryToExit(primaryStage);
            }
        });
        
        /*
         * Do other "welcome" things: About, audio chime.
         */
        audioManager = new FXUIAudio();
		nAbout.launch(mainWindowPane.getScene().getWindow(), true);

        /*
         * Print to output that we're ready. This is the end of the process.
         * The buttons shown on the UI take over from this point.
         */
        System.out.println("RISK is ready.\n" + FXUIGameMaster.VERSION_INFO);
    }
    
    /**
     * Upon launch, show & animate a splash screen over the regular screen contents.
     */
    private void bootSplashScreen(){
    	final Rectangle bkgnd = new Rectangle(0,0,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT);
    	bkgnd.setFill(Color.ALICEBLUE);
    	final Text tName = new Text(DEFAULT_CONTENT_WIDTH/1.75, DEFAULT_CONTENT_HEIGHT/1.75, "RISK");
    	tName.setTextAlignment(TextAlignment.CENTER);
        tName.setFont(Font.font("Arial", FontWeight.BOLD, 256));
        tName.setFill(Color.BLACK);
        tName.setStroke(Color.CORAL);
        GaussianBlur gBlur = new GaussianBlur(4);
        Glow gGlow = new Glow(1.0d);
        gGlow.setInput(gBlur);
    	tName.setEffect(gGlow);
        mainWindowPane.getChildren().addAll(bkgnd,tName);

        Thread pulse = new Thread(null, new Runnable() {
            @Override
            public void run() {
            	bootSplashHelper(tName, bkgnd);
            }
	    }, "bootSplashScreen");
	    pulse.setDaemon(true);
	    pulse.start();
    }
    
	
	private static void bootSplashHelper(Text splashText, Rectangle splashBackground){
		final int discreteSteps = 75;
		final AtomicBoolean complete = new AtomicBoolean(false);
        RiskUtils.sleep(2400);
		for (int i = discreteSteps; i > (int)(discreteSteps/2); i--){
			complete.set(false);
			final int input = i;
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	splashText.setOpacity((double)input/discreteSteps);
    	        	splashBackground.setOpacity((double)input/discreteSteps);
    	        	complete.set(true);
                }
            });
			while(!complete.get() && !FXUIGameMaster.fullAppExit){
				RiskUtils.sleep(5);
			}
		}
		Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	mainWindowPane.getChildren().removeAll(splashText, splashBackground);
            }
        });
		
	}
        
        
    /**
     * Upon exit, fade into a splash screen over the regular screen contents.
     */
    private void exitSplashScreen(){
    	final Rectangle backgroundRectangle = new Rectangle(0,0,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT);
    	backgroundRectangle.setFill(Color.BLACK);
    	final Text textGoodbye = new Text(DEFAULT_CONTENT_WIDTH/5, DEFAULT_CONTENT_HEIGHT/1.75, "G O O D B Y E");
    	textGoodbye.setTextAlignment(TextAlignment.CENTER);
        textGoodbye.setFont(Font.font("Arial", FontWeight.BOLD, 128));
        textGoodbye.setFill(Color.ALICEBLUE);
        textGoodbye.setStroke(Color.CORAL);
    	final Rectangle foregroundRectangle = new Rectangle(0,0,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT);
    	foregroundRectangle.setFill(Color.CHOCOLATE);
    	final Text textRisk = new Text(DEFAULT_CONTENT_WIDTH/1.75, DEFAULT_CONTENT_HEIGHT/1.75, "RISK");
    	textRisk.setTextAlignment(TextAlignment.CENTER);
        textRisk.setFont(Font.font("Arial", FontWeight.BOLD, 256));
        textRisk.setFill(Color.ALICEBLUE);
        textRisk.setStroke(Color.CORAL);
        GaussianBlur gBlur = new GaussianBlur(5);
        Glow gGlow = new Glow(1.0d);
        gGlow.setInput(gBlur);
    	textRisk.setEffect(gGlow);
        textRisk.setOpacity(0);
        foregroundRectangle.setOpacity(0);
        mainWindowPane.getChildren().addAll(backgroundRectangle, textGoodbye, foregroundRectangle,textRisk);

        Thread pulse = new Thread(null, new Runnable() {
            @Override
            public void run() {
            	exitSplashHelper(textRisk, foregroundRectangle);
            }
	    }, "exitSplashScreen");
	    pulse.setDaemon(true);
	    pulse.start();
    }
    
	
	private static void exitSplashHelper(Text splashText, Rectangle splashBackground){
		final int discreteSteps = 75;
		final AtomicBoolean complete = new AtomicBoolean(false);
		for (int i = (int)(discreteSteps/2); i < discreteSteps && FXUIGameMaster.mainStage.isShowing(); i++){
			complete.set(false);
			final int input = i;
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	splashText.setOpacity((double)input/discreteSteps);
    	        	splashBackground.setOpacity((double)input/discreteSteps);
    	        	complete.set(true);
                }
            });
			while(!complete.get() && !FXUIGameMaster.fullAppExit){
				RiskUtils.sleep(5);
			}
		}
		
	}
	
	/**
     * Upon win, show a splash screen over the regular screen contents.
     */
    private void winnerScreen(String winnerName){
		if(winnerName == null)
		{
			FXUIGameMaster.diagnosticPrintln("Cannot display winner splash screen");
		}
    	final Rectangle backgroundRectangle = new Rectangle(0,0,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT);
    	backgroundRectangle.setFill(Color.GREY);
		backgroundRectangle.setOpacity(0.3d);
    	final Text textGoodbye = new Text(DEFAULT_CONTENT_WIDTH/5, DEFAULT_CONTENT_HEIGHT/1.75, "v i c t o r y");
    	textGoodbye.setTextAlignment(TextAlignment.CENTER);
        textGoodbye.setFont(Font.font("Arial", FontWeight.BOLD, 128));
        textGoodbye.setFill(Color.ALICEBLUE);
        textGoodbye.setStroke(Color.CORAL);
		final Text textInstructions= new Text(DEFAULT_CONTENT_WIDTH/4, DEFAULT_CONTENT_HEIGHT/1.9, "[click to close]");
    	textInstructions.setTextAlignment(TextAlignment.CENTER);
        textInstructions.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        textInstructions.setFill(Color.BLACK);
        textInstructions.setStroke(Color.ALICEBLUE);
		textInstructions.setEffect(new GaussianBlur(5));
    	final Rectangle foregroundRectangle = new Rectangle(0,DEFAULT_CONTENT_HEIGHT/4,DEFAULT_CONTENT_WIDTH,DEFAULT_CONTENT_HEIGHT/2.6);
		try{
			foregroundRectangle.setFill(playerColorMap.get(winnerName));
		}
		catch (Exception e){
			foregroundRectangle.setFill(Color.BLACK);
		}
    	final Text textRisk = new Text(0, DEFAULT_CONTENT_HEIGHT/2.5, winnerName + "\nis victorious");
    	textRisk.setTextAlignment(TextAlignment.LEFT);
        textRisk.setFont(Font.font("Arial", FontWeight.BOLD, 128));
        textRisk.setFill(Color.ALICEBLUE);
        textRisk.setStroke(Color.BLACK);
        GaussianBlur gBlur = new GaussianBlur(2);
        Glow gGlow = new Glow(1.0d);
        gGlow.setInput(gBlur);
    	textRisk.setEffect(gGlow);
        textRisk.setOpacity(0);
        foregroundRectangle.setOpacity(0);
		
		EventHandler<MouseEvent> clickToClose = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent t) {
                    mainWindowPane.getChildren().removeAll(backgroundRectangle, textGoodbye, foregroundRectangle, textRisk, textInstructions);
                }
        };
		backgroundRectangle.setOnMouseClicked(clickToClose);
		textGoodbye.setOnMouseClicked(clickToClose);
		foregroundRectangle.setOnMouseClicked(clickToClose);
		textRisk.setOnMouseClicked(clickToClose);
		textInstructions.setOnMouseClicked(clickToClose);
		
		Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	mainWindowPane.getChildren().addAll(backgroundRectangle, textGoodbye, foregroundRectangle, textRisk, textInstructions);
                }
            });
        //mainWindowPane.getChildren().addAll(backgroundRectangle, textGoodbye, foregroundRectangle,textRisk);

        Thread pulse = new Thread(null, new Runnable() {
            @Override
            public void run() {
            	winnerScreenHelper(textRisk, foregroundRectangle);
            }
	    }, "exitSplashScreen");
	    pulse.setDaemon(true);
	    pulse.start();
    }
    
	
	private static void winnerScreenHelper(Text splashText, Rectangle splashBackground){
		final int discreteSteps = 75;
		final AtomicBoolean complete = new AtomicBoolean(false);
		for (int i = (int)(discreteSteps/2); i < discreteSteps && FXUIGameMaster.mainStage.isShowing(); i++){
			complete.set(false);
			final int input = i;
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	splashText.setOpacity((double)input/discreteSteps);
    	        	splashBackground.setOpacity((double)input/discreteSteps);
    	        	complete.set(true);
                }
            });
			while(!complete.get() && !FXUIGameMaster.fullAppExit){
				RiskUtils.sleep(5);
			}
		}
		
	}
	
	
	private void setGameRunningIndicator(Node visualIndicator){
		if(visualIndicator == null){
			return;
		}
		else{
			visualIndicator.setOpacity(0.5d);
			FXUIGameMaster.gameRunningIndicator = visualIndicator;
		}
	}
	
	private void animateGameRunningIndicator(){
		if(FXUIGameMaster.gameRunningIndicator == null || FXUIGameMaster.indicatorAnimatedAlready){
			return;
		}
		
		Thread pulse = new Thread(new Runnable() {
            @Override
            public void run() {
            	gameRunningVisualIndicatorHelper(FXUIGameMaster.gameRunningIndicator);
            	FXUIGameMaster.indicatorAnimatedAlready = false;
            }
	    });
		pulse.setName("animateGameRunningIndicator");
	    pulse.setDaemon(true);
	    pulse.start();
	}
	
	private static void gameRunningVisualIndicatorHelper(Node visualIndicator){
		int discreteSteps = 40, startingStep = 1;
		final Date runToAnimate = FXUIGameMaster.gameStartTime;
		final long sleepTime = 55;
		final long timeBetweenPulses = 2000;
		final AtomicBoolean returnSoon = new AtomicBoolean(false);
		final AtomicBoolean proceed = new AtomicBoolean(true);
		while(FXUIGameMaster.priGameLogicThread != null 
				&& !FXUIGameMaster.endGame 
				&& !FXUIGameMaster.fullAppExit
				&& runToAnimate == FXUIGameMaster.gameStartTime){
			for (int i = startingStep; i < discreteSteps; i++){
				do{
					RiskUtils.sleep(sleepTime);
				}
				while(!proceed.get());
				final int input = i;
				if(FXUIGameMaster.fullAppExit){
					return;
				}
				Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                	try{
	                		proceed.set(false);
	        				visualIndicator.setOpacity((double)input/discreteSteps);
	        				visualIndicator.setEffect(new Glow((double)input/discreteSteps));
	        				proceed.set(true);
	        			}
	        			catch(Exception e){
	        				e.printStackTrace();
	        				returnSoon.set(true);
	        				proceed.set(true);
	        			}
	                }
	            });
				if(returnSoon.get()){ return; }
			}
			for (int i = (int)(discreteSteps*1.5); i > startingStep; i--){
				do{
					RiskUtils.sleep(sleepTime);
				}
				while(!proceed.get());
				final int input = i;
				if(FXUIGameMaster.fullAppExit){
					return;
				}
				Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                	try{
	                		proceed.set(false);
	        				visualIndicator.setOpacity((double)input/discreteSteps);
	        				visualIndicator.setEffect(new Glow((double)input/discreteSteps));
	        				proceed.set(true);
	        			}
	        			catch(Exception e){
	        				e.printStackTrace();
	        				returnSoon.set(true);
	        				proceed.set(true);
	        			}
	                }
	            });
				if(returnSoon.get()){ return; }
			}
			do{
				RiskUtils.sleep(timeBetweenPulses);
			}
			while(!proceed.get());
		}
	}
	

    private void enableAutomaticResizingFunctionality(Stage stageIn) {
        /*GET INFORMATION NECESSARY FOR WINDOW RESIZING...
         ...which means get the height of the window decoration.
         Otherwise, a border happens.*/
        Stage testStage = new Stage();
        Pane testPane = new Pane();
        testPane.setMaxSize(0, 0);
        Scene testScene = new Scene(testPane, 1, 1);
        testStage.setScene(testScene);
        testStage.show();
        FXUIGameMaster.diagnosticPrintln("TestStage size:" + testStage.getWidth() + ":::" + testStage.getHeight());
        FXUIGameMaster.diagnosticPrintln("TestScene height:: " + testScene.getHeight());
        double windowDecorationHeight = testStage.getHeight() - testScene.getHeight();
        double windowDecorationWidth = testStage.getWidth() - testScene.getWidth();
        FXUIGameMaster.diagnosticPrintln("WindowDecorationHeight < ::: > WindowDecorationWidth " + windowDecorationHeight + " <:::> " + windowDecorationWidth);
        testStage.close();
        testScene = null;
        testPane = null;
        testStage = null;
        /*End part where we get extra window size information*/
        FXUIGameMaster.mainWindowResizeHandler = new WindowResizeHandler(stageIn, (double) DEFAULT_CONTENT_WIDTH / DEFAULT_CONTENT_HEIGHT, windowDecorationWidth, windowDecorationHeight, DEFAULT_CONTENT_WIDTH, DEFAULT_CONTENT_HEIGHT);
        FXUIGameMaster.mainWindowResizeHandler.setCallerActive();
    }

    /**
     * Used to attempt a clean exit of the application. As of right now, we
     * *must* show the confirmation dialog, as it triggers a cleanup of active
     * windows and such to allow the game logic thread to end gracefully (i.e.,
     * not interrupt them).
     */
    private void tryToExit(Stage primaryStage) {
        Thread ttExit = new Thread(new Runnable() {
            @Override
            public void run() {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                        	FXUIAudio.playEndJingle();
                        	setSubStatus("xo");
                        	mainStatusTextElement.setText("G O O D B Y E");
                            exitSplashScreen();
                        }
                    });
            	if(FXUIGameMaster.mainStage.isShowing()){
            		FXUIGameMaster.diagnosticPrintln("waiting for splash screen...");
            		RiskUtils.sleep(5000);
            	}
            	else{
            		FXUIGameMaster.diagnosticPrintln("not waiting for splash screen.");
            	}
                Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    FXUIGameMaster.fullAppExit = true;
                    FXUIGameMaster.mainWindowResizeHandler.setCallerInactive();
                    crossbar.signalHumanEndingGame();
                    crossbar.tryCloseCurrentPlayerDialog();
                    FXUIGameMaster.endGame = true;
                    RiskUtils.sleep(500); //Singular use on the FX thread.
                    primaryStage.hide();
                    doYouWantToMakeAnExit(true, 0);
                    primaryStage.close();
                }
                }
                );
            }
	    });
        ttExit.setName("tryToExit");
	    ttExit.setDaemon(true);
	    ttExit.start();
        
    }
    
    /**
    * Show a message on the screen in a nice, safe place. (At this point, it's
    * shown in the lower left).
    * Clicking on that message will open it in full in a dialog.
    * @param message the message to be shown (potentially truncated)
    */
    private static void displayExtendedMessage(String message){
    	int strLenToDisplay = 100;
    	if (message == null){
    		return;
    	}
    	else if (message.length() < 1){
    		return;
    	}
    	else if (message.length() < strLenToDisplay){
    		strLenToDisplay = message.length();
    	}
    	final String lineToShow = message.substring(0, strLenToDisplay);
    	Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	FXUIGameMaster.extendedMessageDisplay.setText(lineToShow);
            	FXUIGameMaster.extendedMessageDisplay.setOnMouseClicked(
            		new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent t) {
                            showPassiveDialog(message);
                            FXUIGameMaster.extendedMessageDisplay.setText("---");
                        }
                });
            }
        });
    	FXUIGameMaster.extendedMessageCache.add(message);
    }

    /**
     * Create a non-JavaFX thread (if necessary) to build & display a passive
     * dialog window (to inform a user of something). Tries to run the dialog's
     * code on a non-JFX thread as much as possible.
     * @param textToShow the text...to be shown
     * @return for now, returns >=0 upon thread creation success, ??? otherwise
     */
    public static int showPassiveDialog(String textToShow) {
		//represents the dialog; true: the dialog is visible (& code is waiting), false: window isn't showing.
		AtomicBoolean dialogIsShowing = new AtomicBoolean(true);
		if(Platform.isFxApplicationThread()){ //if this is the FX thread, make it all happen!
			passiveDialogHelper(textToShow, dialogIsShowing);
			return 1;
		}
		else{ //if this isn't the FX thread, make it happen there!
			Platform.runLater(new Runnable(){
				@Override public void run(){
					passiveDialogHelper(textToShow, dialogIsShowing);
				}
			});
		}
		return 0;
    }

    /**
     * Build & show the dialog.
     *
     * @param textToShow the info text to show in the passive dialog
     * @param dialogIsShowing used to control the flow of code; will be set to
     * "false" when dialog is closed.
     */
    private static void passiveDialogHelper(String textIn, AtomicBoolean dialogIsShowing) {
        Window owner = FXUIGameMaster.mainStage.getScene().getWindow();
        try {
            final Stage dialog = new Stage();
            dialog.setTitle(""); //TODO make use of this in the future
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY() + 50);

            final VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-padding: 30");

            final Text headerText = new Text(""); //TODO make use of this in the future
            headerText.setTextAlignment(TextAlignment.CENTER);
            headerText.setFont(Font.font("Arial", FontWeight.BOLD, 24));

            final Text bodyText = new Text("     " 
            		+ delineateAfter180Chars(textIn) 
            		+ "     ");
            bodyText.setTextAlignment(TextAlignment.CENTER);

            Text spaceBuffer = new Text("\n");
            spaceBuffer.setTextAlignment(TextAlignment.CENTER);
            spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 16));

            final Button yeah = new Button("OK");
            yeah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dialogIsShowing.set(false);
                    dialog.close();
                }
            });

            layout.getChildren().setAll(
                    headerText, bodyText, yeah, spaceBuffer
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
            System.out.println(ERROR + " passive dialog display failed:: " + e);
        }
    }
    
    /**
     * Create a non-JavaFX thread (if necessary) to build & display a passive
     * dialog window containing the contents of the log.
     * A bit easier than opening the log file directly elsewhere.
     * @param textToShow the text...to be shown
     * @return returns -1 if log dialog is already being displayed,
     *  0 if run immediately & synchronously, or 1 if run asynchronously.
     *  (Synchronously: dialog will be displayed before other code
     * is run. Asynch: dialog MAY be displayed before other code gets executed)
     */
    public static int showLogContents() {
    	if(FXUIGameMaster.logDialogIsShowing == null){
    		FXUIGameMaster.logDialogIsShowing = new AtomicBoolean(false);
    	}
    	if(FXUIGameMaster.logDialogIsShowing.get() == true){
    		return -1;
    	}

		//represents the dialog; true: the dialog is visible (& code is waiting), false: window isn't showing.
		if(Platform.isFxApplicationThread()){ //if this is the FX thread...
			passiveLogDisplayHelper(FXUIGameMaster.logDialogIsShowing);
			return 1;
		}
		else{ //if this isn't the FX thread, communicate with it to display dialog!
			Platform.runLater(new Runnable(){
				@Override public void run(){
					passiveLogDisplayHelper(FXUIGameMaster.logDialogIsShowing);
				}
			});
		}
		return 0;
    }

    /**
     * Build & show the dialog.
     *
     * @param textToShow the info text to show in the passive dialog
     * @param dialogIsShowing used to control the flow of code; will be set to
     * "false" when dialog is closed.
     */
    private static void passiveLogDisplayHelper(AtomicBoolean dialogIsShowing) {
        Window owner = FXUIGameMaster.mainStage.getScene().getWindow();
        try {
        	dialogIsShowing.set(true);
        	
            final Stage dialog = new Stage();
            dialog.setTitle("-log-");
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY() + 50);

            /*	This will contain all the log text ("bodyText")
             *	to adjust the positioning of the text in the ScrollPane,
             *	edit here, as the ScrollPane directly contains this VBox
             */
            final VBox innerLayout = new VBox(1);
            innerLayout.setAlignment(Pos.TOP_LEFT);
            /*affect how far the text is from a given edge
             * top,right,bottom,left*/
            innerLayout.setPadding(new Insets(10,0,0,10));
            innerLayout.setPrefSize(300, 350);

            final Text headerText = new Text("----");
            headerText.setTextAlignment(TextAlignment.CENTER);
            headerText.setFont(Font.font("Arial", FontWeight.LIGHT, 14));
            headerText.setFill(Color.WHEAT);
           
            final Text bodyText = new Text("");
            bodyText.setTextAlignment(TextAlignment.LEFT);
            bodyText.setFont(Font.font("Arial", FontWeight.LIGHT, 12));

            String spaceBufferContents = FXUIGameMaster.internalLogCache.size() == 0 ? 
            		"[No content logged yet]" : "[Populating log content...]";
            Text spaceBuffer = new Text(spaceBufferContents);
            spaceBuffer.setTextAlignment(TextAlignment.CENTER);
            spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
            
            final AtomicBoolean pauseText = new AtomicBoolean(false);
            final Button pauseBtn = new Button("pause auto-update");
            pauseBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    pauseText.set(!pauseText.get());
                    if(pauseText.get()){
                    	pauseBtn.setText("resume auto-update");
                    }
                    else{
                    	pauseBtn.setText("pause auto-update");
                    }
                }
            });

            final Button closeBtn = new Button("hide log");
            closeBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dialogIsShowing.set(false);
                    dialog.close();
                }
            });
            

            innerLayout.getChildren().setAll(
            		bodyText, spaceBuffer
            );

            dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    dialogIsShowing.set(false);
                }
            });
            
            ScrollPane siPane = new ScrollPane();
            siPane.setLayoutX(0);
            siPane.setContent(innerLayout);
            
            HBox lowButtonsHBox = new HBox(10);
            lowButtonsHBox.setAlignment(Pos.CENTER);
            lowButtonsHBox.getChildren().setAll(
            		pauseBtn, closeBtn
            );
            lowButtonsHBox.setPadding(new Insets(0,0,20,0));
            
            VBox outerVBox = new VBox(10);
            outerVBox.setAlignment(Pos.TOP_CENTER);
            outerVBox.getChildren().setAll(
            		headerText, siPane, lowButtonsHBox
            );
            outerVBox.setStyle("-fx-background-color: grey");
            
            dialog.setScene(new Scene(outerVBox));
            dialog.show();
            //refresh the contents so long as the window is open
            //also, exit the while loop if the application is going to exit...
            //otherwise the thread might try to stay alive and it's a big mess.
            Thread refreshTextThread = 
            new Thread(new Runnable() {
                @Override
                public void run() {
                	final AtomicInteger cachePosition = new AtomicInteger(0);
                	while(dialogIsShowing.get() && !FXUIGameMaster.fullAppExit){
                		int curLocation = cachePosition.get();
                		String newTextOut = "";
                		RiskUtils.sleep(1000);
                		for (; curLocation < internalLogCache.size(); curLocation++){
                			newTextOut += 
                					delineateAfter180Chars(internalLogCache.get(curLocation));
                		}
                		if(!dialogIsShowing.get() || cachePosition.get() + 1 >=  internalLogCache.size() || pauseText.get()){
                			RiskUtils.sleep(1000);
                		}
                		else{
	                        cachePosition.set(curLocation);
	                        final String newTextShare = newTextOut;
	                        Platform.runLater(new Runnable() {
	                            @Override
	                            public void run() {
	                            	spaceBuffer.setText("");
	                            	bodyText.setText(bodyText.getText() + "\n" 
	                            			+ newTextShare);
	                            	siPane.setVvalue(1);
	                            	//make sure to always display the last lines
	                            	//of the log if we are already at the bottom
	                            }
	                        });
                		}
                	}
                	FXUIGameMaster.diagnosticPrintln("log window thread done.");
                	FXUIGameMaster.diagnosticPrintln("log window boolean: " + dialogIsShowing.get());
                }
            });
            refreshTextThread.setName("passiveLogDisplayHelper");
            refreshTextThread.setDaemon(true);
            refreshTextThread.start();
        } catch (Exception e) {
            System.out.println(ERROR + " display of log contents failed:: " + e);
        }
    }
    
    /**
     * Break a string of text up onto lines, separated by the newline character,
     * of 180 (or fewer) characters.
     * @param textIn The text to potentially be split
     * @return the String of text, with extra newlines added where necessary.
     */
    private static String delineateAfter180Chars(String textIn){
        String textToShow = "";
        for (int strIndex = 0; strIndex < textIn.length(); strIndex += 180){
        	int charsToDo = 180;
        	if (textIn.length() - strIndex < charsToDo){
        		charsToDo = textIn.length() - strIndex;
        	}
        	textToShow += textIn.substring(strIndex,strIndex+charsToDo);
        	textToShow+= "\n";
        }
        return textToShow;
    }
    
    /**
     * 
     * @param owner The Window of the Stage calling this method
     * @param autoExit whether this window should automatically close after
     * a set amount of time (around 5 seconds)
     */
    private void showOptionsAndAbout(Window owner, Button[] buttonsToShow) {
    	if(buttonsToShow == null){
    		return;
    	}
    	
        final Stage dialog = new Stage();

        dialog.setTitle("Options & About");
        if(buttonsToShow.length < 1){
        	dialog.setTitle("No content provided!");
        }
        dialog.initOwner(owner);
		//dialog.initStyle(StageStyle.UTILITY);
        //dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setX(owner.getX());
        dialog.setY(owner.getY() + 100);

        final Button closeButton = new Button("[close]");
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                dialog.close();
            }
        });

        final VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 5;");
        for(int i = 0; i < buttonsToShow.length; i++){
        	layout.getChildren().add(buttonsToShow[i]);
        }
        
        double widthOfLines = 250d;
        double strokeThicknessOfLines = 3.0d;
        Color colorOfLines = Color.WHEAT;
        Line bufferLine = new Line(0,0,widthOfLines,0);
        bufferLine.setStrokeWidth(strokeThicknessOfLines);
        bufferLine.setStroke(colorOfLines);
        
        layout.getChildren().addAll(bufferLine,closeButton);

        ScrollPane internalScrollPane = new ScrollPane(layout);

        dialog.setScene(new Scene(internalScrollPane));
        dialog.show();
    }
    
    /**
	 * Enables a user to, with the flip of a particular boolean, control whether
	 * verbose diagnostic information is printed. Calls to this method with the
	 * affected boolean set to "false" will result in the message being
	 * suppressed. Affected boolean: {@link WindowResizeHandler#DIAGNOSTIC_MODE}.
	 * @param contentOut the content to be conditionally displayed.
	 */
	public static void diagnosticPrintln(String contentOut) {
        if (FXUIGameMaster.DIAGNOSTIC_MODE) {
            System.out.println(contentOut);
        }
    }
}
