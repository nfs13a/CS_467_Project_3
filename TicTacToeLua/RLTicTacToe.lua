math.randomseed( os.time() )


stateTable = {}  --holds table (string, float) representation of a state ex. X0_X0_X__
maxGameCount = 10000 

function deleteTable()

end

function printStateSpace(s)
	print(s.string, s.prob)
end

function printTable(t)
	for i = 1, #t, 1 do
		print(t[i])
	end
end

function printStateTable()
	for i = 1, #stateTable, 1 do
		print(i)
		printStateSpace(stateTable[i])
	end

	print("Number of items in Table:", #stateTable)
end

function isXTurn(s)

	local xCount = 0
	local oCount = 0

	for i = 1, s.string:len(), 1 do
		if s.string:sub(i,i) == "X" then
			xCount = xCount + 1
		elseif s.string:sub(i,i) == "O" then
			oCount = oCount + 1
		end
	end

	if xCount == 0 or xCount == oCount then
		return true
	else
		return false
	end
end

function calculateAllPossibleActions(s)

	local char

	if isXTurn(s) then
		char = "X"
	else
		char = "O"
	end

	for i = 1, s.string:len(), 1 do
		local tempString
		if s.string:sub(i,i) == "_" then
			tempString = s.string:sub(1,i-1) .. char .. s.string:sub(i+1, s.string:len())

			local tempStateSpace = {}
			tempStateSpace.string = tempString
			tempStateSpace.prob = .5

			checkIfExistsInStateTable(tempStateSpace)
		end
	end
	--push them all if they aren't in currently.
end

function chooseNextStateSpace(s) --there's a better way to do this, I'm just not thinking right now

	local tempPossibleActions = {}
	local losingOptions = {}

	local char

	if isXTurn(s) then
		char = "X"
	else
		char = "O"
	end

	for i = 1, s.string:len(), 1 do
		local tempString
		if s.string:sub(i,i) == "_" then
			tempString = s.string:sub(1,i-1) .. char .. s.string:sub(i+1, s.string:len())

			for j = 1, #stateTable, 1 do

				if tempString == stateTable[j].string then

					if stateTable[j].prob == 0 then
						table.insert( losingOptions, tempString )
					elseif stateTable[j].prob == 1 then
						return tempString
					end

					for k = 1, stateTable[j].prob * 10, 1 do 
						table.insert(tempPossibleActions, tempString)
					end
				end
			end
		end
	end

	if #tempPossibleActions ~= 0 then
		local a = tempPossibleActions[math.random(1, #tempPossibleActions)]
		for i = 1, #stateTable, 1 do
			if stateTable[i].string == a then
				return a, stateTable[i].prob
			end
		end
	else
		return losingOptions[1], 0 --a losing decision
	end

end

function checkIfExistsInStateTable(s)
	local tempStateSpace = {}
	tempStateSpace.string = s.string

	for i = 1, #stateTable, 1 do
		if stateTable[i].string == tempStateSpace.string then
			return true --it exists
		end
		tempStateSpace.string = rotateCounterclockwise(tempStateSpace)
	end

	table.insert(stateTable, s)
	return false --it does not currently exist
end

function pushStateToStateTable(s)
	table.insert(stateTable, s)
end

function rotateCounterclockwise(s)

	local a1 = s.string:sub(1,1)
	local b2 = s.string:sub(2,2)
	local c3 = s.string:sub(3,3)	
	local d4 = s.string:sub(4,4)	-- 	1 2 3        3 6 9
	local e5 = s.string:sub(5,5)	--	4 5 6  rc =  2 5 8
	local f6 = s.string:sub(6,6)	--	7 8 9        1 4 7
	local g7 = s.string:sub(7,7)
	local h8 = s.string:sub(8,8)
	local i9 = s.string:sub(9,9)

	return c3 .. f6 .. i9 .. b2 .. e5 .. h8 .. a1 .. d4 .. g7
end

function checkIfEndState(s)

	printStateSpace(s)

	if s.string:sub(1,1) ~= "_" and s.string:sub(1,1) == s.string:sub(4,4) and s.string:sub(4,4) == s.string:sub(7,7) then
		print("End Game", s.string:sub(1,1), "147")
		return true
	elseif s.string:sub(1,1) ~= "_" and s.string:sub(1,1) == s.string:sub(5,5) and s.string:sub(5,5) == s.string:sub(9,9) then
		print("End Game", s.string:sub(1,1), "159")
		return true
	elseif s.string:sub(1,1) ~= "_" and s.string:sub(1,1) == s.string:sub(2,2) and s.string:sub(2,2) == s.string:sub(3,3) then
		print("End Game", s.string:sub(1,1), "123")
		return true
	elseif s.string:sub(2,2) ~= "_" and s.string:sub(2,2) == s.string:sub(5,5) and s.string:sub(5,5) == s.string:sub(8,8) then
		print("End Game", s.string:sub(2,2), "258")
		return true
	elseif s.string:sub(3,3) ~= "_" and s.string:sub(3,3) == s.string:sub(5,5) and s.string:sub(7,7) == s.string:sub(7,7) then
		print("End Game", s.string:sub(3,3) ,"357")
		return true
	elseif s.string:sub(3,3) ~= "_" and s.string:sub(3,3) == s.string:sub(6,6) and s.string:sub(6,6) == s.string:sub(9,9) then
		print("End Game", s.string:sub(3,3), "369")
		return true
	elseif s.string:sub(4,4) ~= "_" and s.string:sub(4,4) == s.string:sub(5,5) and s.string:sub(5,5) == s.string:sub(6,6) then
		print("End Game", s.string:sub(4,4), "456")
		return true
	elseif s.string:sub(7,7) ~= "_" and s.string:sub(7,7) == s.string:sub(8,8) and s.string:sub(8,8) == s.string:sub(9,9) then
		print("End Game", s.string:sub(7,7), "789")
		return true
	elseif s.string:sub(1,1) ~= "_" and s.string:sub(2,2) ~= "_" and s.string:sub(3,3) ~= "_" and s.string:sub(4,4) ~= "_" and s.string:sub(5,5) ~= "_" and s.string:sub(6,6) ~= "_" and s.string:sub(7,7) ~= "_" and s.string:sub(8,8) ~= "_" and s.string:sub(9,9) ~= "_" then
		print("DRAW")
		return true
	else
		print("Not End Game")
		return false
	end
end

function startGame()

	local gameCount = 0

	local stateSpace = {}
	stateSpace.string = "_________"
	stateSpace.prob = .5
	table.insert( stateTable, stateSpace )

	while gameCount <= maxGameCount do
		print ("GameCount", gameCount)

		stateSpace.string = "_________" -- an empty playboard
		stateSpace.prob = .5

		local movesMade = {}
		table.insert( movesMade, stateSpace.string )
		movesMade.isOver = false

		--printStateTable()

		while not movesMade.isOver do

			calculateAllPossibleActions(stateSpace) -- calculate all possible next moves, add those to stateTable if doesn't exist
			stateSpace.string, stateSpace.prob = chooseNextStateSpace(stateSpace) -- make a valid move, depending on probability of next steps

			if stateSpace.string == nil then 
				break	--something seems to be wrong with chooseNext, such that it's returning nil in some cases..... temporary workaround
			end

			table.insert(movesMade, stateSpace.string) -- set old statespace to current, pass in new state to moves movesMade

			if checkIfEndState(stateSpace) then
				movesMade.isOver = true
				break
			end

			-- check to see if game is over
				--if is over
				--award points based on win/loss
				--set movesMade.isOver = true

				--if not over,
				--keep a-going
		end

		gameCount = gameCount + 1

		printTable(movesMade)
		print("")
	end
end

function main()
	startGame()
end


main()


--[[
1.) Make a Reinforcement Learning Agent/Program that learns to play Tic-Tac-Toe pseudo-intelligently.

2.) Create states and represent them. Create a new state when you see it.

3.) When creating a new state, calculate all possible actions. Assign each action a value of 0.5, because nothing is know about its real value yet.

4.) Evaluation function: has this state already been created?  No: state of the environment, who's turns is it?, has the game ended - is so give rewards to both agents, 
	winning agent last action-value set to 1.0, previous action-values for this game instance increased according to learning factor and decay; losing agent receives decreases for chose state-action pairs. create the new state, 
	generate all possible legal actions, assign 0.5 value to all actions 

5.) Choose action function: choose randomly taking values in to consideration, favor exploration (choosing lower values disproportionately), favor exploitation (choosing higher values disproportionately).

6.) When do we stop? Easy: set number of "games" or clock time; other options: reaching target actions values for states. For some domains, when a certain solution/overall value is reached.

7.) Demo/check-in: Your project should-

     - Allow a human to play against your agent/program

     - With a human opponent, each time the agent has a turn, show a list of possible actions and their values

     - The agent should be able to play well

     - Training data should be saved to a file, so it can be reused/improved

     - You should be able to demonstrate your agent "restarting" from scratch (no known values)  
 ]]