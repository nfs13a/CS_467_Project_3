package code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;

import javafx.util.Pair;

class Bet {
	private int space;	//also Count; the number on the board that is bet
	private int number;	// the side of the die that is bet on the space
	private boolean isStar; //0 represents a star, but this flag is a more elegant way to handle it
	private int person;	//integer identifier of player that made the Bet
	
	public Bet() {	//set default that will be overridden anyway
		space = 0;
		number = 0;
		isStar = false;
		person = -1;
	}
	
	public Bet(int s, int n, boolean iS) {	//set the Bet space values
		space = s;
		number = n;
		isStar = iS;
	}
	
	public Bet(Bet original) {
		space = original.getSpace();
		number = original.getNumber();
		isStar = original.isStar();
	}
	
	/*getters and setters*/
	public int getSpace() {
		return space;
	}
	
	public void setNumber(int n) {
		number = n;
	}
	
	public int getNumber() {
		return number;
	}
	
	public boolean isStar() {
		return isStar;
	}
	
	public void setPerson(int p) {	//the person who makes the Bet is known slightly later in the game loop, so we set it after the constructor
		 person = p;
	}
	
	public int getPerson() {
		return person;
	}
	
	/**
	 * here because we might need it in callMyBluff and Player, and it fits here
	 * @param previous - old bet space, which we need to know to determine if we are on a Star or not
	 * @param space - current bet space
	 * @return
	 */
	public static int incrementSpace(int previous, int current) {
		if (previous >= current) {	//on star
			return current * 2;
		} else if (current % 2 == 0) {	//on post-star
			return current + 1;
		}
		return (current + 1) / 2;	//on pre-star
	}
	
	public static Bet incrementBet(Bet current) {
		if (current.getSpace() <= 0) {
			//System.out.println("Going past starting bet.");
			return new Bet(1, 0, false);
		}
		if (current.isStar()) {
			return new Bet(current.getSpace() * 2, 0, false);
		} else {
			if (current.getSpace() % 2 == 0) {
				return new Bet(current.getSpace() + 1, 0, false);
			} else {
				return new Bet((current.getSpace() + 1) / 2, 0, true);
			}
		}
	}

	public boolean greaterThan(Bet currentBet) {
		if (isStar == currentBet.isStar()) {
			if (space == currentBet.getSpace()) {
				return number > currentBet.getNumber();
			} else {
				return space > currentBet.space;
			}
		} else {	//not on same type of space (Star vs Non-Star), so cannot be on same space
			if (currentBet.isStar()) {
				return space >= currentBet.getSpace() * 2;
			} else {	//isStar
				return !(currentBet.getSpace() >= space * 2);	//checks to see that currentBet's space is not ahead of this space. Since the bets cannot be on the same space in this condition, this works.
			}
		}
	}
}

class Player {
	private Vector<Integer> dice;	//all dice that the user has
	private double learningFactor;	//degree to which we change values of actions taken
	private double decay;			//degree to which we degrade the value given to taken actions
	private Vector<Boolean> revealedDice;	//parallel with dice to state which dice have been revealed or not
	private static Map<String, Double[]> states = null;	//shared state-action pairs across all players
	private Stack<Pair<String,Integer>> chosenMoves;	//list of chosen moves, mapping which move chosen {0..3} to state
	private int explorationVsEvaluation; 	//-1 for exploitation, 0 for standard, 1 for exploration
	
	public Player(int numDice) {
		dice = new Vector<Integer>(numDice);
		revealedDice = new Vector<Boolean>(numDice);
		for (int i = 0; i < numDice; i++) {
			dice.add(0);
			revealedDice.add(i, false);
			//revealedDice.set(i, false);
		}
		if (states == null) {
			states = new HashMap<String, Double[]>();
			try {
				String currentDir = new File("").getAbsolutePath();
				BufferedReader br = new BufferedReader(new FileReader(currentDir + "\\bluffPlayer.csv"));
				String line = "";	//holds each new line
				while ((line = br.readLine()) != null) {	//loop through lines
					String[] itemInfo = line.split(","); 	//state,call bluff,call spot on,bet on probability,bet with bluff
					Double[] temp = {Double.parseDouble(itemInfo[1]),Double.parseDouble(itemInfo[2]),Double.parseDouble(itemInfo[3]),Double.parseDouble(itemInfo[4]),Double.parseDouble(itemInfo[5]),Double.parseDouble(itemInfo[6])};
					states.put(itemInfo[0], temp);
				}
				//System.out.println("Read " + states.size() + " states.");
				br.close();
			} catch (FileNotFoundException e) {	//file not found, oops
				e.printStackTrace();
			} catch (IOException e) {	//bad input from file
				e.printStackTrace();
			}	
		}
		
		chosenMoves = new Stack<>();
		learningFactor = .3;
		decay = .001;
		
		explorationVsEvaluation = 0;	//stay with default for now
	}
	
	private int chooseFirstAction(Bet currentBet, Vector<Integer> rD, int totalDice) {	//since player has the first action, they cannot call a bluff/spot-on
		String currentState = "";
		
		for (int i = 0; i < totalDice; i++) {
			currentState += "U";
		}
		
		if (!states.containsKey(currentState)) {
			Double[] temp = {0.0,0.0,.5,.5,.5,.5};	//on starting move, must bet
			states.put(currentState, temp);
		}
		
		double oddsSum = 0.0;
		Double[] currentOdds = states.get(currentState);
		//need to loop here to get the sum of the odds so that we can generate the random number to make the decision
		for (int i = 2; i < 4; i++) {
			if (explorationVsEvaluation == 0) {
				oddsSum += i;
			} else if (explorationVsEvaluation == 1) {	//exploration
				oddsSum += 1 - i;
			} else {	//exploitation
				oddsSum += 2 * i;
			}
		}
		
		Random generator = new Random();
		double stateChooser = generator.nextDouble() * oddsSum;
		
		//calculate decision
		int decision = -1;
		oddsSum = 0.0;
		for (int i = 2; i < 4; i++) {
			double nextOdds;
			if (explorationVsEvaluation == 0) {
				nextOdds = currentOdds[i];
			} else if (explorationVsEvaluation == 1) {	//exploration
				nextOdds = 1 - currentOdds[i];
			} else {	//exploitation
				nextOdds = 2 * currentOdds[i];
			}
			
			if (stateChooser >= oddsSum && stateChooser <= oddsSum + nextOdds) {
				decision = i;
				break;
			}
			
			oddsSum += nextOdds;
		}
		
		if (decision == -1) {
			decision = 3;
		}
		
		chosenMoves.push(new Pair<String, Integer>(currentState,decision));
		
		return decision;
	}
	
	public int chooseAction(Bet currentBet, Vector<Integer> rD, int totalDice) {	
		if (currentBet.getSpace() == 0) {	//player gets first bet
			return chooseFirstAction(currentBet, rD, totalDice);	//simplified version of code called in this method
		}
		
		//get current state from revealedDice and player's dice, only caring about what does and does not apply to the current bet
		String currentState = "";
		int countOfSeen[] = {0,0};	//B,N
		
		for (int i : rD) {
			if (i == currentBet.getNumber())
				countOfSeen[0]++;
			else
				countOfSeen[1]++;
		}
		
		int playersRevealedCount = 0;
		for (int i = 0; i < dice.size(); i++) {
			if (!revealedDice.get(i)) {
				if (i == currentBet.getSpace()) {
					countOfSeen[0]++;	
				} else {
					countOfSeen[1]++;
				}
			} else {
				playersRevealedCount++;
			}
		}
		
		for (int i = 0; i < countOfSeen[0]; i++) {
			currentState += "B";
		}
		for (int i = 0; i < countOfSeen[1]; i++) {
			currentState += "N";
		}
		
		for (int i = 0; i < totalDice - (rD.size() + (dice.size() - playersRevealedCount)); i++) {
			currentState += "U";
		}
		
		if (currentState.length() != totalDice) {
			System.out.println("Error: calculated the incorrect total dice for state: " + currentState);
		}
		
		//currentState is a string of number of (B)et dice, (N)ot bet dice, and (U)nknown dice
		
		//insert currentState with default .5 values if it has not been seen before
		if (!states.containsKey(currentState)) {
			Double[] temp = {.5,.5,.5,.5,.5,.5};
			states.put(currentState, temp);
		}
		
		//System.out.println("State " + currentState + " was seen.");
		
		double oddsSum = 0.0;
		Double[] currentOdds = states.get(currentState);
		//need to loop here to get the sum of the odds so that we can generate the random number to make the decision
		for (double i : currentOdds) {
			if (explorationVsEvaluation == 0) {
				oddsSum += i;
			} else if (explorationVsEvaluation == 1) {	//exploration
				oddsSum += 1 - i;
			} else {	//exploitation
				oddsSum += 2 * i;
			}
		}
		
		Random generator = new Random();
		double stateChooser = generator.nextDouble() * oddsSum;
		
		//calculate decision
		int decision = -1;
		oddsSum = 0.0;
		for (int i = 0; i < 4; i++) {
			double nextOdds;
			if (explorationVsEvaluation == 0) {
				nextOdds = currentOdds[i];
			} else if (explorationVsEvaluation == 1) {	//exploration
				nextOdds = 1 - currentOdds[i];
			} else {	//exploitation
				nextOdds = 2 * currentOdds[i];
			}
			
			if (stateChooser >= oddsSum && stateChooser <= oddsSum + nextOdds) {
				decision = i;
				break;
			}
			
			oddsSum += nextOdds;
		}
		
		if (decision == -1) {
			decision = 3;
		}
		
		chosenMoves.push(new Pair<String, Integer>(currentState,decision));
		return decision;
	}
	
	public Bet betProb(Bet currentBet, Vector<Integer> rD, int totalDice) {
		//System.out.println("Entered betProb");
		//bet on side with most occurrences, only increment count if needed 
		int largest = 1;
		
		int diceCount[] = {0,0,0,0,0,0};
		
		for (int i = 0; i < 6; i++) {
			//count number of sides of dice that have been revealed
			for (int j : rD) {
				//System.out.println("revealed: " + j);
				//if (j == 0 || j == currentBet.getSpace()) {
				if (j == 0 || j == i) {
					diceCount[i]++;
				}
			}
			//count number of sides of dice that player has and have not been revealed
			for (int j = 0; j < dice.size(); j++) {
				//if (!revealedDice.get(j) && (j == 0 || j == currentBet.getSpace())) {
				if (!revealedDice.get(j) && (j == 0 || j == i)) {
					diceCount[i]++;
				}
			}
			if (i > 0 && diceCount[i] >= diceCount[largest]) {
				largest = i;
			}
		}
		
		if (diceCount[0] >= diceCount[largest] * 2) {
			largest = 0;
		}
		
		Bet newBet = new Bet(currentBet);
		if (largest == 0) {
			while (newBet.getSpace() == 0 || !newBet.isStar() || newBet.getSpace() == currentBet.getSpace()) {
				newBet = Bet.incrementBet(newBet);
			}
		} else {
			while (newBet.getSpace() == 0 || newBet.isStar() || (newBet.getSpace() == currentBet.getSpace() && newBet.getNumber() <= currentBet.getNumber())) {
				newBet = Bet.incrementBet(newBet);
			}
			newBet.setNumber(largest);
		}
		
		//if more N dice than bet, 1-reroll chance to reroll; if less N dice than bet reroll chance to reroll
		double rand = Math.random();
		String currentState = chosenMoves.peek().getKey(); 
		
		if (currentBet.getSpace() > 0 && ((diceCount[largest] > newBet.getSpace() && 1 - states.get(currentState)[4] <= rand) || (diceCount[largest] <= newBet.getSpace() && states.get(currentState)[4] <= rand))) {
			chosenMoves.pop();
			Pair<String, Integer> changedAction = new Pair<String, Integer>(currentState, 4);
			chosenMoves.push(changedAction);
			System.out.println("Reroll on " + largest);
			reroll(largest);
		}
		
		return newBet;
	}
	
	public Bet betBluff(Bet currentBet, Vector<Integer> revealedDice, int totalDice) {
		Bet newBet = new Bet(currentBet);
		
		boolean numBetMaxed = currentBet.getNumber() == 5 || currentBet.isStar() || currentBet.getSpace() <= 0;
		int rand = numBetMaxed ? (int) (Math.random() * 5 + 1) : (int) (Math.random() * 6); 
		
		for (int i = 0; i < rand; i++) {
			newBet = Bet.incrementBet(newBet);
		}
		
		if (!numBetMaxed && rand == 0) {	//bluff between currentBet number and 5
			newBet.setNumber((int) (Math.random() * (5 - currentBet.getNumber()) + currentBet.getNumber()));	//attempting to generate a random integer on [currentBet.getNumber(), 5]
		} else {	//bluff between 1 and 5, or star
			if (newBet.isStar()) {
				newBet.setNumber(0);
			} else {
				newBet.setNumber((int) (Math.random() * 5 + 1));
			}
		}
		
		/*if (newBet.getSpace() == currentBet.getSpace() && newBet.getNumber() == currentBet.getNumber()) {
			System.out.println("##############\nError, new bet was the same as previous\n##############");
			throw new UnknownError();
		}*/
		
		double rerollRand = Math.random();
		String currentState = chosenMoves.peek().getKey(); 
		
		if (currentBet.getSpace() > 0 && states.get(currentState)[5] <= rerollRand) {
			chosenMoves.pop();
			Pair<String, Integer> changedAction = new Pair<String, Integer>(currentState, 5);
			chosenMoves.push(changedAction);
			System.out.println("Reroll on " + newBet.getNumber());
			reroll(newBet.getNumber());
		}
		
		return newBet;
	}
	
	int getDiceNum() {
		return dice.size();
	}
	
	public int getNumDiceNum(int val) {
		int count = 0;
		System.out.print("Player has |");
		for (int i : dice) {
			System.out.print(i + "|");
			if (i == val || i == 0)
				count++;
		}
		System.out.println("");
		return count;
	}
	
	public Vector<Integer> getRevealedDice() {
		Vector<Integer> toReturn = new Vector<Integer>();
		
		for (int i = 0; i < dice.size(); i++) {
			if (revealedDice.get(i)) {
				toReturn.add(dice.get(i));
			}
		}
		
		return toReturn;
	}
	
	public void loseDice(int num) {
		if (dice.isEmpty()) {
			System.out.println("Attempting to remove " + num + " dice from a player with no dice.");
			return;
		}
		while (dice.size() > 0 && num > 0) {
			dice.remove(0);
			revealedDice.remove(0);
			num--;
		}
	}
	
	public void gainDie() {
		dice.add(0);
		revealedDice.add(false);
	}
	
	void roll() {
		for (int i = 0; i < dice.size(); i++) {
			dice.set(i, (int) (Math.random() * 6));
			revealedDice.set(i, false);
		}
	}
	
	void reroll(Vector<Boolean> reveals) {
		for (int i = 0; i < dice.size(); i++) {
			if (reveals.get(i)) {
				revealedDice.set(i, true);
			} else {
				if (!revealedDice.get(i)) {	//this statement will never be necessary with good user input
					dice.set(i, (int) (Math.random() * 6));
				}
			}
		}
	}
	
	void reroll(int keep) {
		for (int i = 0; i < dice.size(); i++) {
			if (dice.get(i) == keep || dice.get(i) == 0) {
				revealedDice.set(i, true);
			} else {
				if (!revealedDice.get(i)) {
					dice.set(i, (int) (Math.random() * 6));
				}
			}
		}
	}
	
	public void learn(boolean won) {		
		//decay = (chosenMoves.size() * 1.0) / 100;
		decay = (chosenMoves.size() * 1.0) / 10000;
		
		int i = 0;
		Pair<String,Integer> move;
		while (!chosenMoves.isEmpty()) {
			move = chosenMoves.pop();
			Double[] newOdds = states.get(move.getKey());
			if (won) {
				Double[] diff = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
				for (int j = 0; j < 6; j++) {
					diff[j] = 1 - newOdds[j];
					if (move.getValue() == j) {
						newOdds[j] = newOdds[j] + (diff[j] * ( learningFactor - decay * i) );
						if (move.getValue() > 3) {	//rerolled in that turn, change basic decision value also
							newOdds[j - 2] = newOdds[j - 2] + (diff[j - 2] * ( learningFactor - decay * i) );
						}
					}
				}
			} else {
				Double[] diff = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
				for (int j = 0; j < 6; j++) {
					diff[j] = newOdds[j];
					if (move.getValue() == j) {
						newOdds[j] = newOdds[j] - (diff[j] * ( learningFactor - decay * i) );
						if (move.getValue() > 3) {	//rerolled in that turn, change basic decision value also
							newOdds[j - 2] = newOdds[j - 2] + (diff[j - 2] * ( learningFactor - decay * i) );
						}
					}
				}
			}
			states.put(move.getKey(), newOdds);
			i++;
		}
	}

	public void writeToFile() {
		//System.out.println("Entered writeToFile to write " + states.size() + " states.");
		try {
			String currentDir = new File("").getAbsolutePath();
			BufferedWriter bw = new BufferedWriter(new FileWriter(currentDir + "\\bluffPlayer.csv"));
			Iterator<Map.Entry<String,Double[]>> it = states.entrySet().iterator();
		    while (it.hasNext()) {
		        Entry<String, Double[]> pair = it.next();
		        String write = (String) pair.getKey();
		        Double[] values = (Double[]) pair.getValue();
		        for (int i = 0; i < 6; i++) {
		        	write += "," + values[i];
		        }
		        bw.write(write + "\n");
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		    states = null;
			bw.close();
		} catch (FileNotFoundException e) {	//file not found, oops
			e.printStackTrace();
		} catch (IOException e) {	//bad input from file
			e.printStackTrace();
		}
	}

	public void reset() {
		states = null;
	}

	public void printDice() {
		for (int i : dice) {
			System.out.print("|" + i);
		}
		System.out.println("|");
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
	 * but this is probably not a necessary variable
	 */
	//private final String[] spaces = {"1", "1S", "2", "3", "2S", "4", "5", "3S", "6", "7", "4S", "8", "9", "5S", "10", "11", "6S", "12", "13", "7S", "14", "15", "8S", "16", "17", "9S", "18", "19", "10S", "20"};
	
	private Vector<Player> players;	//all players that we have
	private Bet previousBet;	//TODO might consider storing the previous Bet as a state in the Bet object
	private Bet currentBet;
	private Vector<Integer> revealedDice;
	private int winner;
	private int totalDice;
	
	public callMyBluff() {
		players = new Vector<Player>();
		
		for (int i = 0; i < (int) (Math.random() * 4 + 2); i++) {
			players.add(new Player(5));
		}
		
		previousBet = new Bet();		
		currentBet = new Bet();
		
		revealedDice = new Vector<Integer>();
		
		totalDice = players.size() * players.get(0).getDiceNum();
	}
	
	/**
	 * @param caller
	 * @return player identifier that should start the next round
	 */
	private int handleCall(int caller) {
		int count = 0;	//number of dice with bet side
		for (Player p : players) {
			count += p.getNumDiceNum(currentBet.getNumber());
		}
		
		//System.out.println("Counted " + count + " dice of bet number.");
		
		if (count == currentBet.getSpace()) {
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).getDiceNum() > 0 && i != currentBet.getPerson()) {
					players.get(i).loseDice(1);
					totalDice--;
				}
			}
			System.out.println("Bet was exactly correct.");
			return currentBet.getPerson();
		} else if (count > currentBet.getSpace()) {
			players.elementAt(caller).loseDice(count - currentBet.getSpace());
			totalDice -= count - currentBet.getSpace();
			System.out.println("Bet was valid, Player " + caller + " loses " + (count - currentBet.getSpace()) + " dice.");
			return caller;
		} else {
			players.elementAt(currentBet.getPerson()).loseDice(currentBet.getSpace() - count);
			totalDice -= currentBet.getSpace() - count;
			System.out.println("Bet was wrong, " + currentBet.getPerson() + " loses " + (currentBet.getSpace() - count) + " dice.");
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
		
		//System.out.println("Counted " + count + " dice of bet number.");
		
		if (count == currentBet.getSpace()) {
			System.out.println("Spot-On was exactly correct.");
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).getDiceNum() > 0 && i != caller) {
					players.get(i).loseDice(1);
					totalDice--;
				} else if (i == caller) {
					players.get(i).gainDie();
					totalDice++;
				}
			}
			return caller;
		} else {
			System.out.println("Spot-On was incorrect, Player " + caller + " loses " + (Math.abs(count - currentBet.getSpace())) + " dice");
			players.elementAt(caller).loseDice(Math.abs(count - currentBet.getSpace()));
			totalDice -= Math.abs(count - currentBet.getSpace());
			return currentBet.getPerson();
		}
	}
	
	private void reevaluateReveals() {
		revealedDice = new Vector<Integer>();
		for (Player p : players) {
			revealedDice.addAll(p.getRevealedDice());
		}
	}
	
	private boolean checkForCompletion() {
		int playersRemainingCount = 0;
		int i = 0;
		for (Player p : players) {
			System.out.println("Player " + i + " has " + p.getDiceNum() + " dice.");
			if (p.getDiceNum() > 0) {
				winner = i; 
				playersRemainingCount++;
			}
			i++;
		}
		
		return playersRemainingCount == 1;
	}
	
	private void reset() {
		players.get(0).reset();
		
		players = new Vector<Player>();
		
		for (int i = 0; i < (int) (Math.random() * 4 + 2); i++) {
			players.add(new Player(5));
		}
		
		previousBet = new Bet();
		currentBet = new Bet();
		
		revealedDice = new Vector<Integer>();
		
		totalDice = players.size() * players.get(0).getDiceNum();
	}
	
	void human() {
		//remove last computer so that human can fit in game
		//players.remove(players.size() - 1);
		System.out.println("Remember: a 0 is a star, and so is wild.");
		int pTurn = 0;
		int counter = 0;
		Scanner input = new Scanner(System.in);
		while (!checkForCompletion()) {
			currentBet = new Bet();
			previousBet = new Bet();
			//System.out.println("Total Dice: " + totalDice);
			totalDice = 0;
			for (int i = 0; i < players.size(); i++) {
				players.get(i).roll();
				totalDice += players.get(i).getDiceNum();
			}
			while (true) {
				if (pTurn == 0) { // human's turn
					if (players.get(0).getDiceNum() <= 0) {
						pTurn++;
						System.out.println("You have no dice left.");
						continue;
					}
					System.out.println("Human's turn.");
					System.out.println("There are " + totalDice + " dice left in play.");
					/*System.out.print("Dice of sides: |");
					for (int i : revealedDice) {
						System.out.print(i + "|");
					}
					System.out.println(" have been revealed.");*/
					
					for (int i = 1; i < players.size(); i++) {
						if (players.get(i).getDiceNum() > 0 && players.get(i).getRevealedDice().size() > 0) {
							System.out.print("Player " + i + " has revealed |");
							for (int j : players.get(i).getRevealedDice()) {
								System.out.print(j + "|");
							}
							System.out.println(" dice.");
						}
					}
					System.out.print("Your dice are: ");
					players.get(pTurn).printDice();
					for (int i = 1; i < players.size(); i++) {
						System.out.println("Player " + i + " has " + players.get(i).getDiceNum() + " dice.");
					}
					System.out.println("The current bet is " + currentBet.getSpace() + " " + currentBet.getNumber() + "'s.");
					
					String choice = "0";
					if (currentBet.getSpace() == 0) {
						System.out.println("You must bet");
						choice = "3";
					} else {
						while ((choice.charAt(0) > 51 || choice.charAt(0) < 49) || choice.length() > 1) {
							System.out.println("Choose (1) to call the bet, (2) to call spot-on, or (3) to bet.");
							choice = input.nextLine();
						}
					}
					
					switch (choice) {
						case "1": pTurn = handleCall(pTurn);  break;
						case "2": pTurn = handleSpotOn(pTurn); break;
						case "3":
							int newSpace = 0;
							int newNumber = 0;
							Bet newBet = new Bet(newSpace, newNumber, false);
							while (!newBet.greaterThan(currentBet) || newBet.getSpace() <= 0 || newBet.getNumber() > 5 || newBet.getNumber() < 0) {
								System.out.println("Enter the space:");
								newSpace = input.nextInt();
								System.out.println("Enter the number:");
								newNumber = input.nextInt();
								newBet = new Bet(newSpace, newNumber, newNumber == 0);
							}
							previousBet = currentBet;
							currentBet = newBet;
							
							if (currentBet.getSpace() <= 0) {
								System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\nError\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
								System.out.println("Player " + pTurn + " make decision " + choice +  " and bet on " + currentBet.getSpace() + " " + currentBet.getNumber() + "'s.");
								System.exit(0);
							}
							
							System.out.println("Do you want to reroll (Y/N)?");
							String decision = input.nextLine();
							decision = input.nextLine();
							if (decision.equalsIgnoreCase("Y")) {
								System.out.println("Do you want to reveal any of your dice (Y/N)?");
								decision = input.nextLine();
								if (decision.equalsIgnoreCase("Y")) {
									System.out.println("For each of your dice, enter a (0) to reroll or a (1) to reveal");
									Vector<Boolean> reveals = new Vector<>();
									for (int i = 0; i < players.get(pTurn).getDiceNum(); i++) {
										int keepOrNot = input.nextInt();
										reveals.add(keepOrNot == 1);
									}
									players.get(pTurn).reroll(reveals);
								} else {
									players.get(pTurn).reroll(new Vector<Boolean>(0));
								}
							} //else do nothing
					}
					if (choice.charAt(0) < 51) {
						break;
					} else {
						reevaluateReveals();
					}
				} else {
					System.out.println("It is Player " + pTurn + "'s turn.");
					if (players.get(pTurn).getDiceNum() > 0) {
						int choice = players.get(pTurn).chooseAction(currentBet, revealedDice, totalDice);
						//System.out.println("choice: " + choice);
						Bet nextBet = null;
						/*
						 * 0 - the player called the bluff
						 * 1 - the player called Spot On
						 * 2 - the player decides to bet based on probability
						 * 3 - the player decides to bluff
						 */
						switch (choice) {
							case 0: System.out.println("Player " + pTurn + " calls the bluff!"); pTurn = handleCall(pTurn); break;
							case 1: System.out.println("Player " + pTurn + " calls spot-on!"); pTurn = handleSpotOn(pTurn); break;
							case 2: nextBet = players.get(pTurn).betProb(currentBet, revealedDice, totalDice);
									nextBet.setPerson(pTurn);
									break;
							case 3: nextBet = players.get(pTurn).betBluff(currentBet, revealedDice, totalDice);
									nextBet.setPerson(pTurn);
									break;
						}
						if (nextBet != null) {
							previousBet = currentBet;
							currentBet = nextBet;
							System.out.println("Player " + pTurn + " bets " + currentBet.getSpace() + " " + currentBet.getNumber() + "'s.");
						}
						
						if (currentBet.getSpace() <= 0) {
							System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\nError\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
							System.out.println("Player " + pTurn + " make decision " + choice +  " and bet on " + currentBet.getSpace() + " " + currentBet.getNumber() + "'s.");
							System.exit(0);
						}
						
						if (choice < 2) {
							break;
						} else {	//handle reroll
							reevaluateReveals();
						}
					} else {
						System.out.println("Player " + pTurn + " has no dice.");
					}
				}
				
				System.out.println("####################################\n");
				pTurn = (pTurn + 1) % (players.size());
				counter++;
			}
		}
		
		for (int i = 1; i < players.size(); i++) {
			players.get(pTurn).learn(players.get(pTurn).getDiceNum() > 0);
		}
		
		players.get(1).writeToFile();
		
		System.out.println("Player " + winner + " wins!");
	}
	
	void train() {
		int pTurn = 0;
		int counter = 0;
		while (!checkForCompletion()) {
			currentBet = new Bet();
			previousBet = new Bet();
			//System.out.println("Total Dice: " + totalDice);
			totalDice = 0;
			for (int i = 0; i < players.size(); i++) {
				players.get(i).roll();
				totalDice += players.get(i).getDiceNum();
			}
			while (true) {
				System.out.println("It is Player " + pTurn + "'s turn.");
				System.out.println("The current bet is " + currentBet.getSpace() + " " + currentBet.getNumber() + "'s.");
				if (players.get(pTurn).getDiceNum() > 0) {
					int choice = players.get(pTurn).chooseAction(currentBet, revealedDice, totalDice);
					//System.out.println("choice: " + choice);
					Bet nextBet = null;
					/*
					 * 0 - the player called the bluff
					 * 1 - the player called Spot On
					 * 2 - the player decides to bet based on probability
					 * 3 - the player decides to bluff
					 */
					switch (choice) {
						case 0: pTurn = handleCall(pTurn); break;
						case 1: pTurn = handleSpotOn(pTurn); break;
						case 2: nextBet = players.get(pTurn).betProb(currentBet, revealedDice, totalDice);
								nextBet.setPerson(pTurn);
								break;
						case 3: nextBet = players.get(pTurn).betBluff(currentBet, revealedDice, totalDice);
								nextBet.setPerson(pTurn);
								break;
					}
					if (nextBet != null) {
						previousBet = currentBet;
						currentBet = nextBet;
					}
					
					if (choice < 2) {
						break;
					} else {	//handle reroll
						reevaluateReveals();
					}
				}/* else {
					System.out.println("Player " + pTurn + " did not perform turn.");
				}*/
				
				System.out.println("####################################\n");
				pTurn = (pTurn + 1) % players.size();
				counter++;
			}
		}
		
		for (int i = 0; i < players.size(); i++) {
			players.get(pTurn).learn(players.get(pTurn).getDiceNum() > 0);
		}
		
		players.get(0).writeToFile();
		
		System.out.println("Player " + winner + " wins!");
	}
	
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		System.out.println("(P)lay a game with computers or (T)rain the agent?");
		String choice = input.nextLine();
		if (choice.equals("P")) {
			callMyBluff cMB = new callMyBluff();
			cMB.human();
		} else if (choice.equals("T")) {
			System.out.println("Program Start\n\n\n");
			//for (int i = 0; i < 1000; i++) {
			DateFormat df = new SimpleDateFormat("HH");
			DateFormat dfAlt = new SimpleDateFormat("HH:mm");
			int i = 0;
			for (Date dateobj = new Date(); !dfAlt.format(dateobj).equals("23:00"); dateobj = new Date()) {
				try {
					System.out.println("Game " + i);
					callMyBluff cMB = new callMyBluff();
					cMB.train();
					i++;
				} catch (Exception e) {
					System.out.println("Error occurred at " + dfAlt.format(dateobj) + " during game " + i + ".");
					e.printStackTrace();
				}
			}
			System.out.println("###########################################################################\nTraining Complete\n###########################################################################");
			System.out.println("Played " + i + " games.");
		} else {
			System.out.println("Bad input; restart.");
		}	
	}
}