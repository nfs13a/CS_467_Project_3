package code;

import java.util.Vector;

class Bet {
	
}

class Player {
	
}

/**
 * @author CPU8
 * game manager
 */
public class callMyBluff {
	
	/**
	 * The board can be simplified into:
	 * 		if on an odd space, next is space + 1 % 2 for stars
	 * 		if on an even space, next is space + 1
	 * 		if on a star space, next is space * 2
	 */
	private final String[] spaces = {"1", "1S", "2", "3", "2S", "4", "5", "3S", "6", "7", "4S", "8", "9", "5S", "10", "11", "6S", "12", "13", "7S", "14", "15", "8S", "16", "17", "9S", "18", "19", "10S", "20"};
	
	private Vector<Player> players;
	private Bet currentBet;
	
	public callMyBluff() {
		for (int i = 0; i < 6; i++) {
			players.add(new Player());
		}
		currentBet = new Bet();
	}
	
	void handleCall() {
		
	}
	
	void handleSpotOn() {
		
	}
	
	void train() {
		int pTurn = 0;
		
		while (!checkForCompletion()) {
			while (true) {
				if (players.get(pTurn).getDiceNum() > 0) {
					
				}
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}
}