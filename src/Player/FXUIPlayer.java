/*FXUI Player Class
*Albert Wallace, 2015. Version number now found in Player class definition.
*for Seth Denney's RISK, JavaFX UI-capable version
*
*Base build from original "player" interface, 
*incorporating elements of nothing but http://stackoverflow.com/questions/16823644/java-fx-waiting-for-user-input
*so thanks stackoverflow!
**/

package Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;
import Map.Country;
import Map.RiskMap;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.FXUI_Crossbar;
import Util.OSExitException;
import Util.RiskConstants;
import Util.RiskUtils;

/**
 * Encapsulates the UI elements a human may use
 * 	to respond to the GameMaster as a valid Player.
 * 
 * Requires FXUI GameMaster. Not compatible with original GameMaster;
 * 	implemented UI elements require triggering from active JavaFX application.
 * 
 * UI elements are JavaFX, done with Java JDK 8. (By extension, elements were done under JavaFX 8)
 * Compatibility with JDK 7 / JRE1.7 was retroactively restored. 
 * (source files with "Stamp" -- aka date/time stamp -- of Feb 21 2015, 6:00 PM -- aka Y2015.M02.D21.HM1800 -- & later apply).
 * JDK 7/JRE 1.7 will be the target until further notified.
 *
 */
public class FXUIPlayer implements Player {
	public static final String versionInfo = "FXUI-RISK-Player\nVersion 00x0Ch\nStamp Y2015.M03.D05.HM2144\nType:Alpha(01)";

	private static boolean instanceAlreadyCreated = false;
	private static FXUI_Crossbar crossbar;
	
	private String name;
	private int reinforcementsApplied = 0;
	private boolean passTurn = false;
	private boolean turningInNoCards = true;
	private String attackTarget = "-----", attackSource = "------";
	private Window owner;
	//private String currentFocus = null;
	
	//to determine whether the user is still playing the game, or if the user initiated a normal program exit from the system
	class doWeExit{
		private boolean systemExitUsed = true;
		
		//get whether the program should attempt to exit back to the OS, or if the app should continue running after "dialog.close()" is called
		public boolean isSystemExit(){
			final boolean cExit = systemExitUsed;
			systemExitUsed = true;
			return cExit;
		}
		
		//tell the program to not attempt to exit; the user is interacting with the program as per normal use, so a dialog closing is OK
		public boolean setAsNonSystemClose()
		{
			systemExitUsed = false;
			return systemExitUsed;
		}
	}
	private final doWeExit exitDecider = new doWeExit();
	
	/*
	 * Have some class! ...methods. Class methods.
	 */
	
	public FXUIPlayer() {
		if (instanceAlreadyCreated)
		{
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else
		{
			this.name = "FXUIPlayer";
		}
	}
	
	public FXUIPlayer(String nameIn) {
		if (instanceAlreadyCreated){
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else{
			this.name = nameIn;
		}
	}
	
	public void setOwnerWindow(Window ownerIn) {
		this.owner = ownerIn;
	}
	
	public void setAsCrossbar(FXUI_Crossbar crossbar)
	{
		FXUIPlayer.setCrossbar(crossbar);
	}
	
	/**
	 * Specify an allocation of the player's initial reinforcements.
	 * RESPONSE REQUIRED
	 * @param map
	 * @param reinforcements
	 * @return initial allocation
	 */
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
		
		crossbar.setPlayerName(getName());
		final ReinforcementResponse rsp = new ReinforcementResponse();
		final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
		final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
		
	    final Stage dialog = new Stage();
	    dialog.setTitle("Initial Troop Allocation!");
	    //dialog.initOwner(owner);
	    //dialog.initStyle(StageStyle.UTILITY);
	    //dialog.initModality(Modality.WINDOW_MODAL);
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    
	    final VBox layout = new VBox(10);
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    /*layout.getChildren().setAll(
	    textField, 
	    submitButton
	    );*/
	    
	    //generic instructions for initial allocation
	    Text guideText = new Text();
	    guideText.setText("You have been assigned initial countries\nand " + reinforcements + " initial troops;"
	  		+ "\nplease allocate those troops now.\nOne troop per country minimum;\nMust use all available troops.");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().add(guideText);
	    
	    //status: total reinforcements available, reinf used, reinf available.
	    final Text statusText = new Text();
	    statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
	    
	    
	    //buttons for countries you own, and text to display *additional* units to deplor to each country
	    for (final Country ctIn : myCountries)
			{
	  	  	final HBox singleCountryDisp = new HBox(4);
	  	  	singleCountryDisp.setAlignment(Pos.CENTER);
				map.getCountryArmies(ctIn);
				countryUsedReinforcementCount.put(ctIn.getName(), 1);
				reinforcementsApplied++;
				countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 1"));
				singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));
				
				//button to increment reinforcement count for selected country
			  Button plus = new Button ("+");
			  	plus.setOnAction(new EventHandler<ActionEvent>() {
			  		@Override
			    	public void handle(ActionEvent event){
						final String countryAffected = ctIn.getName();
						if (reinforcementsApplied + 1 <= reinforcements){
							reinforcementsApplied++;
							//System.out.println(countryAffected);
							countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)+1);
						}
						updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
				  }
				});
			  //button to decrement reinforcement count for selected country
			  Button minus = new Button ("-");
			  minus.setOnAction(new EventHandler<ActionEvent>(){
			  	  @Override public void handle(ActionEvent t){
			  		final String countryAffected = ctIn.getName();
			  		if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 1/* TODO use variable here*/){
			  			reinforcementsApplied--;
			  			countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)-1);
			  		}
			  		updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			  	  }
			  });
				singleCountryDisp.getChildren().addAll(plus, minus);
				layout.getChildren().add(singleCountryDisp);						
			}
	    
	    updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
	  
	    //button to attempt to accept final reinforcement allocation
	    Button acceptIt = new Button ("Accept/OK");
	    acceptIt.setOnAction(new EventHandler<ActionEvent>(){
	  	  @Override public void handle(ActionEvent t){
	  		if (reinforcementsApplied == reinforcements)
	  		{
	  			for (Country country : myCountries){
	  				rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
	  			}
	  			exitDecider.setAsNonSystemClose();
	  			dialog.close();
	  		}
	  		else{
	  			updateReinforcementCountGIA(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
	  		}
	  	  }
	    });
	    
	    layout.getChildren().addAll(statusText, acceptIt);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    } 
	    reinforcementsApplied = 0;
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		//return result;
		return rsp;
	}
	
	private void updateReinforcementCountGIA(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements){
		for(String countryToUpdate : textElements.keySet()){
			textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
			statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
			if (isError){
				textElements.get(countryToUpdate).setFill(Color.RED);
				statusText.setFill(Color.RED);
			}
			else{
				textElements.get(countryToUpdate).setFill(Color.BLACK);
				statusText.setFill(Color.BLACK);
			}
		}
	}
	
	/**
	 * Propose a subset of the player's cards that can be redeemed for additional reinforcements.
	 * RESPONSE REQUIRED WHEN PLAYER HOLDS AT LEAST 5 CARDS, OTHERWISE OPTIONAL
	 * Dummy method for this class.
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @param turnInRequired
	 * @return subset of the player's cards
	 */
	@Override
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
				
		final CardTurnInResponse rsp = new CardTurnInResponse();  
		final HashMap<Integer, Card> cardsToTurnIn = new HashMap<Integer, Card>();
		final HashMap<Integer, Text> cardStatusMapping = new HashMap<Integer, Text>();
		turningInNoCards = true;
		
	    final Stage dialog = new Stage();
	    final String selected = "*SELECTED*";
	    final String deselected = "not selected";
	    dialog.setTitle(turnInRequired ? "Please Turn In Cards (required)" : "Turn In Cards? (optional)");
	    //dialog.initOwner(owner);
	    
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    
	    final VBox layout = new VBox(10);
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    
	    //generic instructions for initial allocation
	    Text guideText = new Text();
	    guideText.setText(turnInRequired ? "As you have " + myCards.size() + " cards,\nplease turn in a selection of 3 cards:\n3x same type\nOR\n3x different type\nOR\nWild+Any combo\n"
	    		+ "[This action is required for this round]" : "Turn In Cards?\nIf you can form a set of cards with...\n3x same type\nOR\n3x different type\nOR\nWild+Any two\n"
	    		+ "...You are allowed to do so at this point.\nOtherwise, you may review your cards for later use.");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().add(guideText);
	    
	    
	    
	    
	    //buttons for countries you own, and text to display *additional* units to deploy to each country
	    final HBox cardArrayDisplayRowA = new HBox(4);
	    final HBox cardArrayDisplayRowB = new HBox(4);
	    cardArrayDisplayRowA.setAlignment(Pos.CENTER);
	    cardArrayDisplayRowB.setAlignment(Pos.CENTER);
	    int indexInCards = 0;
	    for (final Card cdIn : myCards) {
	  	  	final int indexInCardsUM = indexInCards;
	  	  	final VBox cardWithStatus = new VBox(4);
	  	  	//cardsToTurnIn.put(indexInCards, cdIn);
	  	  	Text subText = new Text(deselected);
	  	  	cardStatusMapping.put(indexInCards, subText);
				//button to increment reinforcement count for selected country
	  	  	String ctySrced = cdIn.getType().equals(RiskConstants.WILD_CARD) ? "wild" : cdIn.getCountry().getName();
			  Button card = new Button ("******\n[type]\n" + cdIn.getType().toLowerCase() + "\n*****\n" + ctySrced.toLowerCase() + "\n[country]\n******");
			  card.setAlignment(Pos.CENTER);
			  card.setTextAlignment(TextAlignment.CENTER);
			  
			  card.setOnAction(new EventHandler<ActionEvent>(){
			  	  @Override public void handle(ActionEvent t){
			  		turningInNoCards =  false;
			  		final Integer cardAffected = (Integer)indexInCardsUM;
			  		if (cardsToTurnIn.containsKey(cardAffected)){
			  			cardsToTurnIn.remove(cardAffected); 
			  			cardStatusMapping.get(cardAffected).setText(deselected);
			  			cardStatusMapping.get(cardAffected).setFont(Font.getDefault());
			  			//selectedCardCount--;
			  		}
			  		else
			  		{
			  			cardsToTurnIn.put(cardAffected, cdIn);
			  			cardStatusMapping.get(cardAffected).setText(selected);
			  			cardStatusMapping.get(cardAffected).setFont(Font.font(null, FontWeight.BOLD, -1));
			  			//selectedCardCount++;
			  		}
			  		//updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			  	  }
			  	  
			  }
			  );
			  indexInCards++;
			  cardWithStatus.getChildren().addAll(card, subText);
			  if(indexInCards > 2){cardArrayDisplayRowB.getChildren().add(cardWithStatus);} else {cardArrayDisplayRowA.getChildren().add(cardWithStatus);}
									
			}

	    layout.getChildren().addAll(cardArrayDisplayRowA, cardArrayDisplayRowB);	
	    
	  //status text: used to indicate if an error occurred upon attempted submission
	    final Text statusText = new Text("----");
	    
	    Button acceptIt = new Button ("Accept/OK");
	    acceptIt.setOnAction(new EventHandler<ActionEvent>(){
	  	  @Override public void handle(ActionEvent t){
	  		if (cardsToTurnIn.size() == RiskConstants.NUM_CARD_TURN_IN){
	  			for (Card cdOut : cardsToTurnIn.values()){
	  				rsp.addCard(cdOut);
	  			}
	  			if (CardTurnInResponse.isValidResponse(rsp, myCards)){
	  				exitDecider.setAsNonSystemClose();
	  				dialog.close();
	  			}
	  		}
	  		else if(!turnInRequired)
	  		{
	  			exitDecider.setAsNonSystemClose();
	  			dialog.close();
	  			turningInNoCards = true;
	  		}
	  		else{
	  			statusText.setText("invalid selection.\n(cards not a valid set)");
	  			//updateReinforcementCountGIA(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
	  		}
	  	  }
	    });
	    
	    Button skipIt = new Button ("Skip Action");
	    acceptIt.setOnAction(new EventHandler<ActionEvent>(){
	  	  @Override public void handle(ActionEvent t){
	  		exitDecider.setAsNonSystemClose();
	  		dialog.close();
	  	  }
	    });
	    
	    if(turnInRequired){
	  	  skipIt.setDisable(true);
	  	  skipIt.setText("Skip [unavailable]");
	    }
	    
	    //add status and buttons to layout
	    layout.getChildren().addAll(statusText, acceptIt);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		if(turningInNoCards){
			return null;
		}
		else{
			return rsp;
		}
	}
	
	
	/**
	 * Specify an allocation of the player's reinforcements.
	 * RESPONSE REQUIRED
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @param reinforcements
	 * @return reinforcement allocation
	 */
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
		final ReinforcementResponse rsp = new ReinforcementResponse();
		final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
		final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
		
		//if, say, USA offered 5 reinforcements, with Mexico using 2 and Canada using 1, then <USA <Mexico, 2>> is the expected format
		//use in FORTIFY, not REINFORCE...
		//HashMap<String, HashMap<String, Integer>> countryNeighborReinforcementAllocation = new HashMap<String, HashMap<String, Integer>>();
		
	    final Stage dialog = new Stage();
	    dialog.setTitle("Reinforcement with new troops!");
	    //dialog.initOwner(owner);
	    //dialog.initStyle(StageStyle.UTILITY);
	    //dialog.initModality(Modality.WINDOW_MODAL);
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    
	    final VBox layout = new VBox(10);
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    /*layout.getChildren().setAll(
	    textField, 
	    submitButton
	    );*/
	    
	    //Generic instructions for reinforcement
	    Text guideText = new Text();
	    guideText.setText("Please place your reinforcements\nin the countries you own.");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().add(guideText);
	    
	    //status text: total reinforcements available, reinf used, reinf available.
	    final Text statusText = new Text();
	    statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
	    
	    
	    //buttons for countries you own, and text to display *additional* units to deplor to each country
	    for (final Country ctIn : myCountries)
			{
	  	  	final HBox singleCountryDisp = new HBox(4);
	  	  	singleCountryDisp.setAlignment(Pos.CENTER);
				map.getCountryArmies(ctIn);
				countryUsedReinforcementCount.put(ctIn.getName(), 0);
				countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 0"));
				singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));
				//button to increment reinforcement count for selected country
			  Button plus = new Button ("+");
			  plus.setOnAction(new EventHandler<ActionEvent>(){
			  	  @Override public void handle(ActionEvent t){
			  		final String countryAffected = ctIn.getName();
			  		if (reinforcementsApplied + 1 <= reinforcements){
			  			reinforcementsApplied++;
			  			countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)+1);
			  		}
			  		updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			  	  }
			  }
			  );
			  //button to decrement reinforcement count for selected country
			  Button minus = new Button ("-");
			    minus.setOnAction(new EventHandler<ActionEvent>(){
			  	  @Override public void handle(ActionEvent t){
			  		final String countryAffected = ctIn.getName();
			  		if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 0 /* TODO use variable here*/){
			  			reinforcementsApplied--;
			  			countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)-1);
			  		}
			  		updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			  	  }
			  }
			  );
				singleCountryDisp.getChildren().addAll(plus, minus);
				layout.getChildren().add(singleCountryDisp);						
			}
	    
	    updateReinforcementCountRIF(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements); //TODO: move this up..or down. or comment
	  
	    
	    //button to attempt to accept final reinforcement allocation
	    Button acceptIt = new Button ("Accept/OK");
	    acceptIt.setOnAction(new EventHandler<ActionEvent>() {
	  	  @Override
	    	public void handle(ActionEvent event){
				if (reinforcementsApplied == reinforcements)
				{
					for (Country country : myCountries){
						rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
					}
					exitDecider.setAsNonSystemClose();
					dialog.close();
				}
				else{
					updateReinforcementCountRIF(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
				}
	    	}
		});//end eventhandler actionevent
	    
	    //add status and buttons to layout
	    layout.getChildren().addAll(statusText, acceptIt);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();
	
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		reinforcementsApplied = 0;
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		//return result;
		return rsp;
	}
	
	private void updateReinforcementCountRIF(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements){
		for(String countryToUpdate : textElements.keySet()){
			textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
			statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
			if (isError){
				textElements.get(countryToUpdate).setFill(Color.RED);
				statusText.setFill(Color.RED);
			}
			else{
				textElements.get(countryToUpdate).setFill(Color.BLACK);
				statusText.setFill(Color.BLACK);
			}
		}
	}
	
	/**
	 * Specify how and where an attack should be mounted.
	 * RESPONSE OPTIONAL
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @return attack choice
	 */
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
		final AttackResponse rsp = new AttackResponse();
		final HashMap<String, Country> myCountries = new HashMap<String, Country>();
		final HashMap<String, HashMap<String, Country>> countryNeighbors = new HashMap<String, HashMap<String, Country>>();
		final HashMap<String, ArrayList<String>> countryNeighborsAsStrings = new HashMap<String, ArrayList<String>>();
		final ArrayList<String> myCountriesAsStrings = new ArrayList<String>();
		//get the countries in an easily sorted string array representation, and store them in a map for easy reference
		for (Country country : RiskUtils.getPlayerCountries(map, this.name))
		{
			myCountries.put(country.getName(), country);
			myCountriesAsStrings.add(country.getName());
			//countryNeighbors.put(country.getName(), (ArrayList<Country>) country.getNeighbors());
			ArrayList<String> stng = new ArrayList<String>();
			HashMap<String, Country> cyAn = new HashMap<String, Country>();
			//add neighbor to list of attackable countries, if the owner isn't me.
			for (Country tgtCt : country.getNeighbors()){
				if(map.getCountryOwner(tgtCt) != this.name)
				{
					stng.add(tgtCt.getName());
					cyAn.put(tgtCt.getName(), tgtCt);
				}
			}
			//stng.sort(null);
			Collections.sort(stng);
			countryNeighborsAsStrings.put(country.getName(), stng);
			countryNeighbors.put(country.getName(), cyAn);
		}
		//myCountriesAsStrings.sort(null);
		Collections.sort(myCountriesAsStrings);
		
		ScrollPane spane = new ScrollPane();
	    final Stage dialog = new Stage();
	    dialog.setTitle("Attack? [optional]");
	    //dialog.initOwner(owner);
	    
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    dialog.setWidth(500);
	    dialog.setHeight(650); // TODO ugly hardcoding of window size needs fixing
	    
	    final VBox layout = new VBox(10);
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    
	    
	    //Generic instructions for reinforcement
	    Text guideText = new Text();
	    guideText.setText("Select the country from which you want to attack [left],\nthen select the target of your attack [right].\n[attacking is optional; you may pass]");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().add(guideText);
	    
	    //status text: total reinforcements available, reinf used, reinf available.
	    final Text statusText = new Text();
	    statusText.setText("Current selection: [No Selection]");
	    statusText.setTextAlignment(TextAlignment.CENTER);
	    
	    final VBox sourceCountriesVBox = new VBox(10);
	    final VBox targetCountriesVBox = new VBox(10);
	    sourceCountriesVBox.setAlignment(Pos.CENTER);
	    targetCountriesVBox.setAlignment(Pos.CENTER);
	    sourceCountriesVBox.getChildren().add(new Text("Source:"));
	    
	    
	    //buttons for countries you own, and text to display *additional* units to deplor to each country
	    for (final String ctSource : myCountriesAsStrings)
			{
				final Button ctSrcBtn = new Button(ctSource);
				//button to increment reinforcement count for selected country
			  ctSrcBtn.setOnAction(new EventHandler<ActionEvent>(){
			  	  @Override public void handle(ActionEvent t){
			  		final String srcID = ctSource;
		  			attackSource = srcID;
		  			attackTarget = "-----"; /* TODO represent as variable*/
		  			statusText.setText("Current selection: Attacking\n" + attackTarget + "\nfrom\n" + attackSource + ".");
			  		targetCountriesVBox.getChildren().clear();
			  		targetCountriesVBox.getChildren().add(new Text("Target:"));
			  		for (final String ctTarget : countryNeighborsAsStrings.get(srcID))
			  		{
			  			final Button ctTgtBtn = new Button(ctTarget);
							ctTgtBtn.setOnAction(new EventHandler<ActionEvent>(){
							  	  @Override public void handle(ActionEvent t){
						  			final String tgtID = ctTarget;
						  			attackTarget = tgtID;
						  			statusText.setText("Current selection: Attacking\n" + attackTarget + "\nfrom\n" + attackSource + ".");
							  	  }//end of actionevent definition
							});
							targetCountriesVBox.getChildren().add(ctTgtBtn);
			  		}//end of outer button for loop
			  	  }
			  }
			  );
			  //button to decrement reinforcement count for selected country
			  sourceCountriesVBox.getChildren().add(ctSrcBtn);
			}
	    
	    final HBox bothCountryGroups = new HBox(10);
	    bothCountryGroups.getChildren().addAll(sourceCountriesVBox, targetCountriesVBox);
	    bothCountryGroups.setAlignment(Pos.CENTER);
	  
	    
	    //button to attempt to accept final reinforcement allocation
	    Button acceptIt = new Button ("Accept/OK");
	    acceptIt.setOnAction(new EventHandler<ActionEvent>(){
	  	  @Override public void handle(ActionEvent t){
	  		if (myCountriesAsStrings.contains(attackSource) && countryNeighborsAsStrings.get(attackSource).contains(attackTarget))
	  		{
	  			int maxDiceAvailable = map.getCountryArmies(myCountries.get(attackSource)) > RiskConstants.MAX_ATK_DICE ? 3 : map.getCountryArmies(myCountries.get(attackSource)) - 1;
	  			if (maxDiceAvailable > 0)
	  			{
	  				rsp.setAtkCountry(myCountries.get(attackSource));
	  				rsp.setDfdCountry(countryNeighbors.get(attackSource).get(attackTarget));
	  				rsp.setNumDice(maxDiceAvailable);
	  				if(!AttackResponse.isValidResponse(rsp, map, getName()))
	  				{
	  					statusText.setText("Not a valid response; try another combo.");
	  				}
	  				else{
	  					passTurn = false;
	  					exitDecider.setAsNonSystemClose();
	  					dialog.close();
	  				}
	  			}
	  			else
	  			{
	  				statusText.setText("Not a valid response;\nAttack from a country with 2+ troops.");
	  			} 
	  		}
	  		else
	  		{
	  			statusText.setText("Not a valid response; \nmake sure you select a target and source!.");
	  		}
	  		
	  	  }
	    });
	    
	    Button skipIt = new Button ("[skip/pass]");
	    skipIt.setOnAction(new EventHandler<ActionEvent>(){
	  	  @Override public void handle(ActionEvent t){
	  		passTurn = true;
	  		exitDecider.setAsNonSystemClose();
	  		dialog.close();
	  	  }
	    });
	    
	    //add status and buttons to layout
	    Text buttonBuffer = new Text("***********");
	    buttonBuffer.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().addAll(statusText, bothCountryGroups, buttonBuffer, acceptIt, skipIt);
	    layout.setAlignment(Pos.CENTER);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    spane.setContent(layout);
	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		
		attackSource = "-----";
		attackTarget = "-----";
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		if(passTurn){
			passTurn = !passTurn;
			return null;
		}
		return rsp;
	}
	
	/**
	 * Specify the number of armies that should be advanced into a conquered territory.
	 * RESPONSE REQUIRED
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @param fromCountry
	 * @param toCountry
	 * @param min
	 * @return advance choice
	 */
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int minAdv){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
		final int sourceArmies = map.getCountryArmies(fromCountry);
		final AdvanceResponse rsp = new AdvanceResponse(minAdv);
		//current advancement allocation can be found with rsp.getNumArmies(). effectively "int destArmies"
		//...well, sort of.
		
		final Stage dialog = new Stage();
	    dialog.setTitle("Advance armies into conquests");
	    //dialog.initOwner(owner);
	    
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    final Text sourceCount = new Text();
	    final Text destCount = new Text();
	    sourceCount.setTextAlignment(TextAlignment.CENTER);
	    destCount.setTextAlignment(TextAlignment.CENTER);
	    final Text acceptanceStatus = new Text("Minimum to advance: " + minAdv);
	    acceptanceStatus.setTextAlignment(TextAlignment.CENTER);
	    final class UpdateStatus{
	  	  boolean doubleCheck = false;
	  	  UpdateStatus(){
	  	  }
	  	  public void refreshStatus(){
				sourceCount.setText(fromCountry.getName() + "\n:::::\n" + (sourceArmies - rsp.getNumArmies()));
				destCount.setText(toCountry.getName() + "\n:::::\n" + rsp.getNumArmies());
				doubleCheck = false;
	  	  }
	  	  public void resetAcceptance()
	  	  {
	  		acceptanceStatus.setText("Minimum to advance: " + minAdv);
	  		doubleCheck = false;
	  	  }
	  	  public boolean verifyAcceptance()
	  	  {
	  		if (sourceArmies - rsp.getNumArmies() != 0 && rsp.getNumArmies() != 0)
	  		{
	  			return true;
	  		}
	  		else if (!doubleCheck){
	  			acceptanceStatus.setText("You cannot leave 0 army members in " + (rsp.getNumArmies() == 0 ? toCountry.getName():fromCountry.getName()) + "?");
	  			//doubleCheck = true;
	  			return false;
	  		}
	  		else{
	  			
	  			return true;
	  		}
	  	  }
	    }
	    HBox countryCounts = new HBox(4);
	    countryCounts.setAlignment(Pos.CENTER);
	    countryCounts.getChildren().addAll(sourceCount, destCount);
	    final UpdateStatus updater = new UpdateStatus();
	    updater.refreshStatus();
	    
	    
	    final Button plusle = new Button("Add/+");
	    plusle.setOnAction(new EventHandler<ActionEvent>() {
	  	  @Override
	    	public void handle(ActionEvent event){
		  	  updater.resetAcceptance();
				if (rsp.getNumArmies() < sourceArmies - 1)
				{
					//System.out.println(rsp.getNumArmies());
					rsp.setNumArmies(rsp.getNumArmies() + 1);
					//System.out.println("adv rsp +" + rsp.getNumArmies());
					updater.resetAcceptance();
					updater.refreshStatus();
				}
	  	  }
			});
	    
	    final Button minun = new Button("Recall/-");
	    minun.setOnAction(new EventHandler<ActionEvent>() {
	  	  @Override
	    	public void handle(ActionEvent event){
		  	  updater.resetAcceptance();
			    if (rsp.getNumArmies() > minAdv + 1 && sourceArmies - (rsp.getNumArmies() + 1) >= 0)
			    {
			  	  updater.resetAcceptance();
			  	  //System.out.println(rsp.getNumArmies());
			  	  rsp.setNumArmies(rsp.getNumArmies() - 1);
			  	  //System.out.println("adv rsp -" + rsp.getNumArmies());
			  	  updater.refreshStatus();
			    }
	  	  }
	    });
	    
	    final Button acceptance = new Button("Submit/OK");
	    //acceptance.setDefaultButton(true);
	    acceptance.setOnAction(new EventHandler<ActionEvent>() {
	  	  @Override
	    	public void handle(ActionEvent event){
				//we can't verify it with the official function, 
				//but we can check if we've actually put our soldiers somewhere
				//and if we so decide, it's possible to just skip proper allocation (wherein either src or dst has 0 troops
				if(updater.verifyAcceptance())
				{
					exitDecider.setAsNonSystemClose();
					dialog.close();
				}
	  	  }
	    });
	    
	    final HBox allocationButtons = new HBox(4);
	    allocationButtons.setAlignment(Pos.CENTER);
	    allocationButtons.getChildren().addAll(minun,plusle);
	    
	    
	    final VBox layout = new VBox(10);
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    layout.getChildren().setAll(
	    countryCounts, allocationButtons,acceptanceStatus, acceptance
	    );

	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();

	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		System.out.println("return adv rsp " + rsp.getNumArmies());
		return rsp;
	}
	
	
	/**
	 * Propose a fortification transfer.
	 * RESPONSE OPTIONAL
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @return fortification choice
	 */
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
		final FortifyResponse rsp = new FortifyResponse();
		final HashMap<String, Country> myCountries = new HashMap<String, Country>();
		HashMap<String, HashMap<String, Country>> countryNeighbors = new HashMap<String, HashMap<String, Country>>();
		final HashMap<String, ArrayList<String>> countryNeighborsAsStrings = new HashMap<String, ArrayList<String>>();
		ArrayList<String> myCountriesAsStrings = new ArrayList<String>();
		//get the countries in an easily sorted string array representation, and store them in a map for easy reference
		for (Country country : RiskUtils.getPlayerCountries(map, this.name))
		{
			myCountries.put(country.getName(), country);
			myCountriesAsStrings.add(country.getName());
			ArrayList<String> stng = new ArrayList<String>();
			HashMap<String, Country> cyAn = new HashMap<String, Country>();
			//add neighbor to list of attackable countries, if the owner isn't me.
			for (Country tgtCt : country.getNeighbors()){
				if(map.getCountryOwner(tgtCt) == this.name)
				{
					stng.add(tgtCt.getName());
					cyAn.put(tgtCt.getName(), tgtCt);
				}
			}
			//stng.sort();
			Collections.sort(stng);
			countryNeighborsAsStrings.put(country.getName(), stng);
			countryNeighbors.put(country.getName(), cyAn);
		}
		//myCountriesAsStrings.sort(null);
		Collections.sort(myCountriesAsStrings);
		
		ScrollPane spane = new ScrollPane();
		final Stage dialog = new Stage();
		dialog.setTitle("Fortify? [optional]");
		//dialog.initOwner(owner);
		
		//dialog.setX(owner.getX());
		//dialog.setY(owner.getY());
		dialog.setWidth(500);
		dialog.setHeight(690); // TODO ugly hardcoding of window size needs fixing
		
		final VBox layout = new VBox(10);
		layout.setAlignment(Pos.CENTER);
		layout.setStyle("-fx-padding: 20;");
		
		
		//Generic instructions for reinforcement
		Text guideText = new Text();
		guideText.setText("Select the country from which you want to fortify [left],\nthen select the destination for your troops [right].\n[fortification is optional; you can skip this]");
		guideText.setTextAlignment(TextAlignment.CENTER);
		layout.getChildren().add(guideText);
		
		//status text: total reinforcements available, reinf used, reinf available.
		final Text statusText = new Text();
		statusText.setText("Current selection: [No Selection]");
		statusText.setTextAlignment(TextAlignment.CENTER);
		
		final VBox sourceCountriesVBox = new VBox(10);
		final VBox targetCountriesVBox = new VBox(10);
		sourceCountriesVBox.setAlignment(Pos.CENTER);
		targetCountriesVBox.setAlignment(Pos.CENTER);
		sourceCountriesVBox.getChildren().add(new Text("Source:"));
		
		
		//buttons for the source and destination countries
		for (final String ctSource : myCountriesAsStrings)
			{
				final Button ctSrcBtn = new Button(ctSource);
				//button to increment reinforcement count for selected country
			  ctSrcBtn.setOnAction(new EventHandler<ActionEvent>() {
			  	  @Override
			    	public void handle(ActionEvent event){
						final String srcID = ctSource;
						rsp.setFromCountry(myCountries.get(srcID));
						rsp.setNumArmies(0);
						rsp.setToCountry(null);
						statusText.setText("Current selection:\nFortifying\n????\nusing ??? troops from\n" + rsp.getFromCountry().getName() + ".");
						targetCountriesVBox.getChildren().clear();
						targetCountriesVBox.getChildren().add(new Text("Target:"));
						for (String ctTarget : countryNeighborsAsStrings.get(srcID))
						{
							final Button ctTgtBtn = new Button(ctTarget);
							final String ctTargetUM = ctTarget;
							ctTgtBtn.setOnAction(new EventHandler<ActionEvent>(){
							  	  @Override public void handle(ActionEvent t){
						  			final String tgtID = ctTargetUM;
						  			rsp.setToCountry(myCountries.get(tgtID));
						  			statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing ??? troops from\n" + rsp.getFromCountry().getName() + ".");
							  	  }//end of actionevent definition
							});
							targetCountriesVBox.getChildren().add(ctTgtBtn);
						}//end of button loop
			  	}
				});
			  //button to decrement reinforcement count for selected country
			  sourceCountriesVBox.getChildren().add(ctSrcBtn);
			}
		
		final HBox bothCountryGroups = new HBox(10);
		bothCountryGroups.getChildren().addAll(sourceCountriesVBox, targetCountriesVBox);
		bothCountryGroups.setAlignment(Pos.CENTER);
		
		
		final Button plusle = new Button("Troops++");
		//plusle.setDefaultButton(true);
		plusle.setOnAction(new EventHandler<ActionEvent>() {
			@Override
		  	public void handle(ActionEvent event){
					int curArmies = rsp.getNumArmies();
						if (rsp.getToCountry() != null && curArmies < map.getCountryArmies(rsp.getFromCountry()) - 1)
						{
							rsp.setNumArmies(rsp.getNumArmies() + 1);
					  	  statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
						}
			}
		});
		
		final Button minun = new Button("Troops--");
		//minun.setDefaultButton(true);
		minun.setOnAction(new EventHandler<ActionEvent>() {
			@Override
		  	public void handle(ActionEvent event){
					int curArmies = rsp.getNumArmies();
				    if (rsp.getToCountry() != null && curArmies > 0)
				    {
				  	  rsp.setNumArmies(rsp.getNumArmies() - 1);
				  	  statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
				    }
			}
		});
		
		HBox plusMinusBtns = new HBox(4);
		plusMinusBtns.setAlignment(Pos.CENTER);
		plusMinusBtns.getChildren().addAll(minun,plusle);
		
		final String playaName = this.getName();
		//button to attempt to accept final reinforcement allocation
		Button acceptIt = new Button ("Accept/OK");
		acceptIt.setOnAction(new EventHandler<ActionEvent>() {
			@Override
		  	public void handle(ActionEvent event){
					if (FortifyResponse.isValidResponse(rsp, map, playaName))
					{
						exitDecider.setAsNonSystemClose();
						passTurn = false;
						dialog.close();
					}
					else
					{
						statusText.setText("Not a valid response; \nmake sure you select a target and source!.");
					}
			}
			
		});
		
		Button skipIt = new Button ("[skip/pass]");
		skipIt.setOnAction(new EventHandler<ActionEvent>() {
			@Override
		  	public void handle(ActionEvent event){
					passTurn = true;
					exitDecider.setAsNonSystemClose();
					dialog.close();
			}
		});
		
		//add status and buttons to layout
		Text buttonBufferTop = new Text("***********");
		buttonBufferTop.setTextAlignment(TextAlignment.CENTER);
		Text buttonBufferBottom = new Text("***********");
		buttonBufferBottom.setTextAlignment(TextAlignment.CENTER);
		layout.getChildren().addAll(statusText, bothCountryGroups, buttonBufferTop, plusMinusBtns, buttonBufferBottom, acceptIt, skipIt);
		layout.setAlignment(Pos.CENTER);
		
		//formally add linear layout to scene, and wait for the user to be done (click the OK button)
		//spane.setContent(layout);
		//dialog.setScene(new Scene(spane));
		dialog.setScene(new Scene(layout));
		FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
		dialog.showAndWait();
		if (exitDecider.isSystemExit())
		{
			 //throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
		}  
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		if(passTurn){
			passTurn = !passTurn;
			return null;
		}
		return rsp;
	}
	
	/**
	 * Specify how a territory should be defended.
	 * RESPONSE REQUIRED
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @param atkCountry
	 * @param dfdCountry
	 * @param numAtkDice
	 * @return defense choice
	 */
	public DefendResponse defend(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country atkCountry, Country dfdCountry, int numAtkDice){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isPlayerEndingGame()){
	    	return null;
	    }
		DefendResponse rsp = new DefendResponse();
		int numDice = map.getCountryArmies(dfdCountry);
		if (numDice > RiskConstants.MAX_DFD_DICE) {
			numDice = RiskConstants.MAX_DFD_DICE;
		}
		rsp.setNumDice(numDice);
		return rsp;
		// TODO this stolen from seth's cpu. must find a way to jiggle & juggle these bits later if necessary.
	}
	
	
	/**
	 * Getter for the player name.
	 * @return name
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * Getter for the FXUI GameMaster-Player Crossbar
	 * @return crossbar, the desired crossbar, static across all instances.
	 */
	public static FXUI_Crossbar getCrossbar() {
		return crossbar;
	}

	/**
	 * Setter for the FXUI GameMaster-Player Crossbar
	 * @param crossbar, the crossbar to use, static across all instances.
	 */
	public static void setCrossbar(FXUI_Crossbar crossbar) {
		FXUIPlayer.crossbar = crossbar;
	}
}