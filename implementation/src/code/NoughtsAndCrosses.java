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
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;

abstract class Agent {
	
	protected double learningFactor;	//change to alter degree to which game results propagate down chosen states
	protected double decay;
	protected Map<String, Double> states;	//all state,value pairs known
	protected Stack<String> chosenMoves;	//all states chosen in a game
	protected String filename;
	protected int explorationVsEvaluation;	//1 for exploration, -1 for evaluation, 0 for standard weighting
	private boolean shouldLearn;
	private boolean playingHuman;
	
	protected Agent(double lF, double dc, String filename) {
		learningFactor = lF;
		
		decay = dc;
		
		states = new HashMap<String, Double>();
		try {
			String currentDir = new File("").getAbsolutePath();
			BufferedReader br = new BufferedReader(new FileReader(currentDir + "\\" + filename + ".csv"));
			String line = "";	//holds each new line
			while ((line = br.readLine()) != null) {	//loop through lines
				String[] itemInfo = line.split(","); // name,cost,value
				states.put(itemInfo[0], Double.parseDouble(itemInfo[1]));
			}
			br.close();
		} catch (FileNotFoundException e) {	//file not found, oops
			e.printStackTrace();
		} catch (IOException e) {	//bad input from file
			e.printStackTrace();
		}
		
		chosenMoves = new Stack<String>();
		
		this.filename = filename;
		
		explorationVsEvaluation = 0;
		
		shouldLearn = true;
		
		playingHuman = true;
	}
	
	abstract protected Vector<String> getNextStates(String gameState);
	
	protected void addNewStates(Vector<String> newStates) {
		for (String str : newStates) {
			if (!states.containsKey(str)) {
				states.put(str, .5);
			}
		}
	}
	
	public String chooseNextState(String current) {
		Vector<String> options = getNextStates(current);
		
		if (playingHuman) {
			System.out.println("Computer Options: ");
			for (String str : options) {
				System.out.println(str);
			}
		}
		
		addNewStates(options);
		
		double oddsSum = 0.0;
		for (String str : options) {
			if (explorationVsEvaluation == 0) {
				oddsSum += states.get(str);
			} else if (explorationVsEvaluation == 1) {	//exploration
				oddsSum += 1 - states.get(str);
			} else {	//exploitation
				oddsSum += 2 * states.get(str);
			}
			
		}
		Random generator = new Random();
		double stateChooser = generator.nextDouble() * oddsSum;
		
		String chosenState = "";
		oddsSum = 0.0;
		for (String str : options) {
			double nextOdds;
			if (explorationVsEvaluation == 0) {
				nextOdds = states.get(str);
			} else if (explorationVsEvaluation == 1) {	//exploration
				nextOdds = 1 - states.get(str);
			} else {	//exploitation
				nextOdds = 2 * states.get(str);
			}
			
			if (stateChooser >= oddsSum && stateChooser <= oddsSum + nextOdds) {
				chosenState = str;
				break;
			}
			
			oddsSum += nextOdds;
		}
		
		if (chosenState.isEmpty()) {
			chosenState = options.lastElement();
		}
		
		chosenMoves.push(chosenState);
		
		return chosenState;
	}
	
	public void setDumb() {
		states = new HashMap<String, Double>();	//reset states to knowing nothing
		shouldLearn = false;
	}
	
	public void learn(int result) {
		if (!shouldLearn) return;
		
		//modify taken paths
		int i = 0;
		String move;
		while (!chosenMoves.isEmpty()) {
			move = chosenMoves.pop();
			double newOdds = states.get(move);
			if (result > 0) {
				double diff = 1 - newOdds;
				newOdds = newOdds + (diff * ( learningFactor - decay * i) );
				if (newOdds >= 1) {
					//newOdds = .9;
				}
			} else if (result < 0) {
				double diff = newOdds - 0;
				newOdds = newOdds - (diff * ( learningFactor - decay * i) );
				if (newOdds <= 0) {
					//newOdds = .1;
				}
			}
			if (newOdds >= 1 || newOdds <= 0) {
				System.out.println(move + "\t" + newOdds);
			}
			states.put(move, newOdds);
			i++;
		}
		
		//write to file
		try {
			String currentDir = new File("").getAbsolutePath();
			BufferedWriter bw = new BufferedWriter(new FileWriter(currentDir + "\\" + filename + ".csv"));
			Iterator it = states.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        bw.write(pair.getKey() + "," + pair.getValue() + "\n");
		        it.remove(); // avoids a ConcurrentModificationException
		    }
			bw.close();
		} catch (FileNotFoundException e) {	//file not found, oops
			e.printStackTrace();
		} catch (IOException e) {	//bad input from file
			e.printStackTrace();
		}
	}

	public void playingHuman() {
		playingHuman = true;
	}
}

class NoughtAgent extends Agent {
	
	public NoughtAgent() {
		super(.3, .05, "noughtsData");
	}
	
	public NoughtAgent(int i) {
		super(.3, .05, "noughtsData");
		explorationVsEvaluation = i;
	}

	protected Vector<String> getNextStates(String gameState) {
		Vector<String> toReturn = new Vector<String>();
		
		for (int i = 0; i < gameState.length(); i++) {
			if (gameState.charAt(i) == '.') {
				String nextFound = gameState.substring(0, i) + "O" + gameState.substring(i + 1);
				toReturn.add(nextFound);
			}
		}
		
		return toReturn;
	}
}

class CrossAgent extends Agent {
	
	public CrossAgent() {
		super(.3, .05, "crossesData");
	}
	
	public CrossAgent(int i) {
		super(.3, .05, "crossesData");
		explorationVsEvaluation = i;
	}
	
	protected Vector<String> getNextStates(String gameState) {
		Vector<String> toReturn = new Vector<String>();
		
		for (int i = 0; i < gameState.length(); i++) {
			if (gameState.charAt(i) == '.') {
				String nextFound = gameState.substring(0, i) + "X" + gameState.substring(i + 1);
				toReturn.add(nextFound);
			}
		}
		
		return toReturn;
	}
}

/**
 * @author CPU8
 * game manager
 */
public class NoughtsAndCrosses {
	
	private final int allWinStates[][] = {
			{0,1,2},
			{3,4,5},
			{6,7,8},
			{0,3,6},
			{1,4,7},
			{2,5,8},
			{0,4,8},
			{2,4,6}
	};
	
	private String currentState;
	public Scanner input;
	
	public NoughtsAndCrosses() {
		input = new Scanner(System.in);
		currentState = ".........";
	}
	
	private void reset() {
		currentState = ".........";
	}
	
	private void printBoard() {
		System.out.println("Current board: ");
		System.out.println(currentState.substring(0,3));
		System.out.println(currentState.substring(3,6));
		System.out.println(currentState.substring(6));
	}
	
	private boolean checkForCompletion() {
		if (!currentState.contains(".")) {	//if board is full
			return true;
		}
		for (int i = 0; i < 8; i++) {
			if (currentState.charAt(allWinStates[i][0]) == currentState.charAt(allWinStates[i][1]) && currentState.charAt(allWinStates[i][1]) == currentState.charAt(allWinStates[i][2]) && currentState.charAt(allWinStates[i][0]) != '.') {	//if player has won
				return true;
			}
		}
		return false;
	}
	
	private int announceResults() {
		for (int i = 0; i < 8; i++) {
			if (currentState.charAt(allWinStates[i][0]) == currentState.charAt(allWinStates[i][1]) && currentState.charAt(allWinStates[i][1]) == currentState.charAt(allWinStates[i][2])) {	//if player has won
				if (currentState.charAt(allWinStates[i][0]) == 'X') {
					System.out.println(currentState.charAt(allWinStates[i][0]) + " wins!");
					return 1;
				} else if (currentState.charAt(allWinStates[i][0]) == 'O') {
					System.out.println(currentState.charAt(allWinStates[i][0]) + " wins!");
					return -1;
				}
			}
		}
		System.out.println("Draw.");
		return 0;
	}
	
	private void humanGame(String humanPiece) {
		System.out.println("Select the setting of the X computer ( explo(R)ation : explo(I)tation : (S)tandard : (D)umb )");
		String difficulty = input.nextLine();
		int i;
		if (difficulty.equalsIgnoreCase("R")) {
			i = 1;
		} else if (difficulty.equalsIgnoreCase("I")) {
			i = -1;
		} else {	//difficulty == "S" | "D"
			i = 0;
		}
		boolean humanX = humanPiece.equalsIgnoreCase("X");
		
		Agent c = null;
		if (humanX) {
			c = new NoughtAgent(i);
		} else {
			c = new CrossAgent(i);
		}
		
		if (difficulty.equalsIgnoreCase("D")) {
			c.setDumb();
		}
		
		c.playingHuman();
		
		boolean XTurn = true;
		
		while (!checkForCompletion()) {
			if (humanX == XTurn) {
				printBoard();
				
				Integer move = null;
				do {
					if (move != null && move >= 0 && move <= 8)
						System.out.println("Space is already taken.");
					if (move != null && (move < 0 || move > 8))
						System.out.println("Out of board bounds.");
					System.out.println("What is your move (0-8)?");
					move = input.nextInt();
				} while ((move < 0 || move > 8) && currentState.charAt(move) != '.');
				
				if (humanX) {
					currentState = currentState.substring(0, move) + "X" + currentState.substring(move + 1);
				} else {
					currentState = currentState.substring(0, move) + "O" + currentState.substring(move + 1);
				}
			} else {
				currentState = c.chooseNextState(currentState);
			}
			XTurn = !XTurn;
		}
		
		int XResult = announceResults();
		
		if (humanX) {
			XResult *= -1;
		}
		
		c.learn(XResult);
	}
	
	private void computerGame() {
		System.out.println("Select the setting of the X computer ( explo(R)ation : explo(I)tation : (S)tandard )");
		String difficulty = input.nextLine();
		int i;
		if (difficulty.equalsIgnoreCase("R")) {
			i = 1;
		} else if (difficulty.equalsIgnoreCase("I")) {
			i = -1;
		} else {	//difficulty == "S"
			i = 0;
		}
		CrossAgent x = new CrossAgent(i);
		
		System.out.println("Select the setting of the O computer ( explo(R)ation : explo(I)tation : (S)tandard )");
		difficulty = input.nextLine();
		if (difficulty.equalsIgnoreCase("R")) {
			i = 1;
		} else if (difficulty.equalsIgnoreCase("I")) {
			i = -1;
		} else {	//difficulty == "S"
			i = 0;
		}
		NoughtAgent o = new NoughtAgent(i);
		
		boolean XTurn = true;
		
		while (!checkForCompletion()) {
			if (XTurn) {
				currentState = x.chooseNextState(currentState);
			} else {
				currentState = o.chooseNextState(currentState);
			}
			XTurn = !XTurn;
		}
		
		int XResult = announceResults();
		
		x.learn(XResult);
		o.learn(XResult * -1);
	}
	
	public void train() {
		for (int i = 0; i < 1000; i++) {
			int j;
			if (i % 5 == 0) {
				j = -1;
			} else if (i % 2 == 0) {
				j = 1;
			} else {
				j = 0;
			}
			CrossAgent x = new CrossAgent(j);
			NoughtAgent o = new NoughtAgent(j);
		
			boolean XTurn = true;
		
			while (!checkForCompletion()) {
				//printBoard();
				if (XTurn) {
					currentState = x.chooseNextState(currentState);
				} else {
					currentState = o.chooseNextState(currentState);
				}
				XTurn = !XTurn;
			}
			
			//printBoard();
		
			int XResult = announceResults();
		
			x.learn(XResult);
			o.learn(XResult * -1);
			
			reset();
		}
		
		System.out.println("###########################################################################\nTraining over\n###########################################################################");
	}

	public static void main(String[] args) {
		NoughtsAndCrosses TTT = new NoughtsAndCrosses();
		
		System.out.println("Select the type of game ( (H)uman : (C)omputer : (T)raining )?");
		String decision = TTT.input.nextLine();
		
		if (decision.equalsIgnoreCase("H")) {
			System.out.println("Does the human want noughts or crosses (O or X)?");
			decision = TTT.input.nextLine();
			TTT.humanGame(decision);
		} else if (decision.equalsIgnoreCase("C")){
			TTT.computerGame();
		} else {
			TTT.train();
		}
	}
}