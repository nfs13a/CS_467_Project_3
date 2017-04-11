package code;

class Agent {
	
	protected int learningFactor;
	
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
	
	private boolean checkForCompletion(String gameState) {
		return false;
	}

	public static void main(String[] args) {
		System.out.println("Hello Tic Tac Toe!");
	}
}