package code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import javafx.util.Pair;

class Bet {
	private int space;	//also Count
	private int number;
	private boolean isStar;
	private int person;
	
	public Bet() {
		space = 1;
		number = 0;
		isStar = false;
		person = -1;
	}
	
	public Bet(int s, int n, boolean iS) {
		space = s;
		number = n;
		isStar = iS;
	}
	
	public int getSpace() {
		return space;
	}
	
	public int getNumber() {
		return number;
	}
	
	public boolean isStar() {
		return isStar;
	}
	
	public void setPerson(int p) {
		 person = p;
	}
	
	public int getPerson() {
		return person;
	}
	
	public static int incrementSpace(int previous, int space) {
		if (previous >= space) {	//on star
			return space * 2;
		} else if (space % 2 == 0) {	//on post-star
			return space + 1;
		}
		return (space + 1) / 2;	//on pre-star
	}
}

class Player {
	private Vector<Integer> dice;
	private double learningFactor;
	private double decay;
	private Vector<Boolean> revealedDice;
	private static Map<String, Double[]> states = null;
	private Stack<Pair<String,Integer>> chosenMoves;	//list of chosen moves, mapping which move chosen {0..3} to state
	
	public Player(int numDice) {
		dice = new Vector<Integer>(numDice);
		revealedDice = new Vector<Boolean>(numDice);
		for (int i = 0; i < numDice; i++) {
			revealedDice.set(i, false);
		}
		if (states == null) {
			states = new HashMap<String, Double[]>();
			try {
				String currentDir = new File("").getAbsolutePath();
				BufferedReader br = new BufferedReader(new FileReader(currentDir + "\\bluffPlayer.csv"));
				String line = "";	//holds each new line
				while ((line = br.readLine()) != null) {	//loop through lines
					String[] itemInfo = line.split(","); 	//state,call bluff,call spot on,bet on probability,bet with bluff
					Double[] temp = {Double.parseDouble(itemInfo[1]),Double.parseDouble(itemInfo[2]),Double.parseDouble(itemInfo[3]),Double.parseDouble(itemInfo[4])};
					states.put(itemInfo[0], temp);
				}
				br.close();
			} catch (FileNotFoundException e) {	//file not found, oops
				e.printStackTrace();
			} catch (IOException e) {	//bad input from file
				e.printStackTrace();
			}
			learningFactor = .3;
			decay = .01;
		}
	}
	
	public int chooseAction(Bet currentBet, Vector<Integer> revealedDice) {
		//when evaluating a number, look at revealedDice, then look at own dice, ignoring that have already been revealed
		int decision = -1;
		
		//this needs to be changed
		chosenMoves.push(new Pair(revealedDice.toString(),decision));
		return decision;
	}
	
	public Bet betProb(Bet currentBet, Vector<Integer> revealedDice) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Bet betBluff(Bet currentBet, Vector<Integer> revealedDice) {
		return null;
	}
	
	int getDiceNum() {
		return dice.size();
	}
	
	public int getNumDiceNum(int val) {
		int count = 0;
		for (int i : dice) {
			if (i == val)
				count++;
		}
		return count;
	}
	
	void loseDice(int num) {
		for (int i = 0; i < num; i++) {
			dice.remove(i);
		}
	}
	
	void roll() {
		for (int i = 0; i < dice.size(); i++) {
			dice.set(i, (int) (Math.random() * 6));
		}
	}
	
	void reroll(Vector<Integer> keep) {
		for (int i = 0; i < dice.size(); i++) {
			if (keep.contains(dice.get(i))) {
				revealedDice.set(i, true);
			} else {
				dice.set(i, (int) (Math.random() * 6));
			}
		}
	}
	
	public void learn(boolean won) {
		//modify taken paths
		
		decay = (chosenMoves.size() * 1.0) / 100;
		
		int i = 0;
		Pair<String,Integer> move;
		while (!chosenMoves.isEmpty()) {
			move = chosenMoves.pop();
			Double[] newOdds = states.get(move.getKey());
			if (won) {
				Double[] diff = new Double[]{0.0,0.0,0.0,0.0};
				for (int j = 0; j < 4; j++) {
					diff[j] = 1 - newOdds[j];
					if (move.getValue() == j)
						newOdds[j] = newOdds[j] + (diff[j] * ( learningFactor - decay * i) );
				}
			} else {
				Double[] diff = new Double[]{0.0,0.0,0.0,0.0};
				for (int j = 0; j < 4; j++) {
					diff[j] = 1 - newOdds[j];
					if (move.getValue() == j)
						newOdds[j] = newOdds[j] - (diff[j] * ( learningFactor - decay * i) );
				}
			}
			states.put(move.getKey(), newOdds);
			i++;
		}
	}

	public void writeToFile() {
		try {
			String currentDir = new File("").getAbsolutePath();
			BufferedWriter bw = new BufferedWriter(new FileWriter(currentDir + "\\bluffPlayer.csv"));
			Iterator it = states.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        String write = (String) pair.getKey();
		        Double[] values = (Double[]) pair.getValue();
		        for (int i = 0; i < 4; i++) {
		        	write += "," + values[i];
		        }
		        bw.write(write + "\n");
		        it.remove(); // avoids a ConcurrentModificationException
		    }
			bw.close();
		} catch (FileNotFoundException e) {	//file not found, oops
			e.printStackTrace();
		} catch (IOException e) {	//bad input from file
			e.printStackTrace();
		}
	}
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
	private Bet previousBet;
	private Bet currentBet;
	private Vector<Integer> revealedDice;
	private int winner;
	
	public callMyBluff() {
		for (int i = 0; i < 6; i++) {
			players.add(new Player(5));
		}
		
		previousBet = new Bet();		
		currentBet = new Bet();
		
		revealedDice = new Vector<Integer>();
	}
	
	/**
	 * @param caller
	 * @return player identifier that should start the next round
	 */
	private int handleCall(int caller) {
		int count = 0;
		for (Player p : players) {
			count += p.getNumDiceNum(currentBet.getNumber());
		}
		
		if (count == currentBet.getSpace()) {
			//not sure what to do
			return -1;	//not sure who to return here
		} else if (count > currentBet.getSpace()) {
			players.elementAt(caller).loseDice(count - currentBet.getSpace());
			return caller;
		} else {
			players.elementAt(currentBet.getPerson()).loseDice(currentBet.getSpace() - count);
			return currentBet.getPerson();
		}
	}
	
	/**
	 * @param caller
	 * @return player identifier that should start the next round
	 */
	private int handleSpotOn(int caller) {
		int count = 0;
		for (Player p : players) {
			count += p.getNumDiceNum(currentBet.getNumber());
		}
		
		if (count == currentBet.getSpace()) {
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).getDiceNum() > 0 && i != caller) {
					players.get(i).loseDice(1);
				}
			}
			return caller;
		} else {
			players.elementAt(caller).loseDice(Math.abs(count - currentBet.getSpace()));
			return currentBet.getPerson();
		}
	}
	
	private boolean checkForCompletion() {
		int playersRemainingCount = 0;
		int i = 0;
		for (Player p : players) {
			if (p.getDiceNum() > 0) {
				winner = i; 
				playersRemainingCount++;
			}
			i++;
		}
		
		return playersRemainingCount == 1;
	}
	
	void train() {
		int pTurn = 0;
		
		while (!checkForCompletion()) {
			while (true) {
				if (players.get(pTurn).getDiceNum() > 0) {
					int choice = players.get(pTurn).chooseAction(currentBet, revealedDice);
					Bet nextBet = null;
					switch (choice) {
						case 0: pTurn = handleCall(pTurn); break;
						case 1: pTurn = handleSpotOn(pTurn); break;
						case 2: nextBet = players.get(pTurn).betProb(currentBet, revealedDice);
								nextBet.setPerson(pTurn);
								break;
						case 3: nextBet = players.get(pTurn).betBluff(currentBet, revealedDice);
								nextBet.setPerson(pTurn);
								break;
					}
					if (nextBet != null) {
						previousBet = currentBet;
						currentBet = nextBet;
					}
					
					if (choice < 2) {
						break;
					}
				}
				pTurn = (pTurn + 1) % players.size(); 
			}
		}
		
		for (int i = 0; i < players.size(); i++) {
			players.get(pTurn).learn(players.get(pTurn).getDiceNum() > 0);
		}
		
		players.get(0).writeToFile();
		
		System.out.println("Player " + winner + "wins!");
	}

	public static void main(String[] args) {
		callMyBluff cMB = new callMyBluff();
		cMB.train();
	}
}