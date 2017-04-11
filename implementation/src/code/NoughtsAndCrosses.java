package code;

import java.util.Map;
import java.util.Stack;
import java.util.Vector;

class Agent {
	
	protected int learningFactor;	//change to alter degree to which game results propagate down chosen states
	protected Map<String, Integer> states;	//all state,value pairs known
	protected Stack<String> chosenMoves;	//all states chosen in a game
	
	protected Agent() {
		
	}
}

class NoughtAgent extends Agent {
	
}

class CrossAgent extends Agent {
	
	public CrossAgent() {
		//read from .csv of states
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
	
	private boolean checkForCompletion(String gameState) {
		if (!gameState.contains(".")) {	//if board is full
			return true;
		}
		for (int i = 0; i < 8; i++) {
			if (gameState.charAt(allWinStates[i][0]) == gameState.charAt(allWinStates[i][1]) && gameState.charAt(allWinStates[i][1]) == gameState.charAt(allWinStates[i][2])) {	//if player has won
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		System.out.println("Hello Tic Tac Toe!");
	}
}