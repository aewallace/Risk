package Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Map.Country;
import Map.RiskMap;
import Player.Player;

/**
 * Class to save games at a given point in time.
 * Relies heavily on GameMaster being able to bring itself
 * 		 to a relatively similar state using limited information.
 */
public class SavePoint implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2015022600000001L;
	public static final String versionInfo = "FXUI-RISK-Master\nVersion REL00-GH01\nStamp Y2015.M02.D26.HM1830\nType:MNT(00)";
	public HashMap<String, String> countriesPlusOwners;
	public HashMap<String, Integer> countriesPlusArmies;
	

	public HashMap<String, ArrayList<String>> playersPlusCards;
	public HashMap<String, String> activePlayersAndTheirTypes;
	
	public ArrayList<String> logCache;

	public HashMap<String, Boolean> playerIsEliminatedMap;

	public int roundsPlayed; //save the round; roundNew > roundOld for it to be a newer save
	public Date dateOriginallySaved = null; //save the original date that the current working game save was created
	public Date latestGameSaveDate = null;
	//use the round info to get fine detail info

	public SavePoint() {
		countriesPlusOwners = new HashMap<String, String>();
		countriesPlusArmies = new HashMap<String, Integer>();

		playersPlusCards = new HashMap<String, ArrayList<String>>();
		activePlayersAndTheirTypes = new HashMap<String, String>();

		playerIsEliminatedMap = new HashMap<String, Boolean>();

		int roundsPlayed = 0;
		
		logCache = new ArrayList<String>();
		// TODO Auto-generated constructor stub
	}
	
	public HashMap<String, String> getCountriesAndOwners() {return countriesPlusOwners;}
	public HashMap<String, Integer> getCountriesAndArmyCount() {return countriesPlusArmies;}

	public HashMap<String, ArrayList<String>> getPlayersAndTheirCards() {return playersPlusCards;}
	public HashMap<String, String> getActivePlayersAndTheirTypes() {return activePlayersAndTheirTypes;}

	public HashMap<String, Boolean> getPlayerIsEliminatedMap(){return playerIsEliminatedMap;}

	public int getRoundsPlayed(){return roundsPlayed;}
	
	public ArrayList<String> getLogCache(){return logCache;}
	
	/**
	 * 
	 * @param originalGameSaveDate the date for the game that originally used this save
	 * @param currentSaveDate the date on which this save was updated
	 * @param roundIn the round of the game at which point this save was made
	 * @return TRUE if the game signature was updated, FALSE if you attempted to save invalid info
	 */
	public boolean updateSaveIdentificationInfo(Date originalGameSaveDate, Date currentSaveDate, int roundIn)
	{
		boolean updateOccurredWithoutError = true;
		
		//game save date must never be null after first setup, and cannot be overwritten once set
		if (originalGameSaveDate != null && dateOriginallySaved == null){
			dateOriginallySaved = originalGameSaveDate;
		}
		else{
			updateOccurredWithoutError = false;
		}
		
		//we can update the info if the
		//  latest date in the save file is the same or earlier than what's being put in as the new date
		//  or if the latest date in the save file had never been set.
		// otherwise, we cannot update the save date
		if (latestGameSaveDate == null || latestGameSaveDate.compareTo(currentSaveDate) <= 0){
			latestGameSaveDate = currentSaveDate;
		}
		else{
			updateOccurredWithoutError = false;
		}
		
		if(roundIn >= roundsPlayed)
		{
			roundsPlayed = roundIn;
		}
		else{
			updateOccurredWithoutError = false;
		}
		
		
		return updateOccurredWithoutError;
	}
	
	public Date getOriginalSaveDate(){
		return dateOriginallySaved;
	}
	
	public void prepAllCountryDetails(RiskMap map){
		for (Country country : Country.values()) {
			country.init();
			countriesPlusArmies.put(country.getName(), (Integer) map.getCountryArmies(country));
			countriesPlusOwners.put(country.getName(), map.getCountryOwner(country));
		}
	}
	
	public void prepAllPlayerDetails(HashMap<String, Player> currentPlayers, List<String> originalPlayers){
		for (String playerName : originalPlayers){
			if(currentPlayers.containsKey(playerName)){
				activePlayersAndTheirTypes.put(playerName, currentPlayers.get(playerName).getClass().toString());
				playerIsEliminatedMap.put(playerName, false);
			}
			else{
				playerIsEliminatedMap.put(playerName, true);
			}
		}
	}
	
	public void prepCardsForGivenPlayer(String player, Collection<Card> cardsForPlayer){
		ArrayList<String> cards = new ArrayList<String>();
		if (cardsForPlayer == null || cardsForPlayer.size() < 1){
			playersPlusCards.put(player, null);
			return;
		}
		else{
			for (Card card : cardsForPlayer){
				String cName;
				if (card.getCountry() != null){
					cName = card.getCountry().getName();
				}
				else{
					cName = "null";
				}
				cards.add(card.getType()+","+cName);
			}
			playersPlusCards.put(player, cards);
			return;
		}
	}
	
	public void prepRoundsCompleted(int roundsCompleted){
		roundsPlayed = roundsCompleted;
	}
	
	public void prepLogCache(ArrayList<String> logCacheIn){
		logCache = logCacheIn;
	}
}
