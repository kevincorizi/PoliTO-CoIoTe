package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;

public class Problem {
	public static final long GREEDY_THRESHOLD = 4950000000L;

	private int nCells;
	private int nPeriods;
	private int nTypes;

	private int[] typeTasks;	// Tasks each customer type can do
	private int[][][][] costs;
	private int[] tasksToDo;
	private HashMap<User, Integer> customers;	// Initial count of users in their respective cells
	
	// Data to keep track of solution evolution
	private Solution bestSolution;
	private long start;
	
	/**
	 * The User class define an assignable user as a combination of:
	 * 
	 * <ul>
	 * 	<li>i : Its source cell.</li>
	 * 	<li>m : Its type.</li>
	 * 	<li>t : Its time slot of availability.</li>
	 * </ul>
	 * 
	 * In the problem's model users are usually denoted as &vartheta;[i][m][t].
	 * 
	 * @author Alessio
	 *
	 */
	public class User {
		public int i, m, t;
		
		public User(int i, int m, int t) {
			this.i = i;
			this.m = m;
			this.t = t;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d, %d)", i, m, t);
		}
		
		public boolean equals(User other) {
			return i == other.i &&
					m == other.m &&
					t == other.t;
		}
	}
	/**
	 * <p>
	 * A Cell represents a destination in the Problem to which Users must
	 * be assigned in order to fulfill tasks. Each cell is identified by
	 * a key-value set associating each user combination to the amount assigned
	 * to this cell. Additionally, an index is built with respect to the
	 * type of user to quickly list all assigned users of a certain type.
	 * </p>
	 * <p>
	 * The internal HashMap (assignments) should <u><i><b>NEVER</i></b></u> be accessed 
	 * (and if I did it it's because I'm lazy and I regret it).
	 * Instead the appropriate methods should be used to correctly 
	 * update the cell. 
	 * </p>
	 * 
	 * @author Alessio
	 *
	 */
	public class Cell {
		private HashMap<User, Integer> assignments;
		private LinkedList<User>[] typeIndex;
		private int tasksToDo, fulfilled;
		
		@SuppressWarnings("unchecked")
		public Cell(int tasksToDo) {
			this.tasksToDo = tasksToDo;
			assignments = new HashMap<User, Integer>();
			typeIndex = (LinkedList<User>[])new LinkedList[nTypes];
			
			for (int m = 0 ; m < nTypes ; m++)
				typeIndex[m] = new LinkedList<User>();
			
			fulfilled = 0;
		}
		
		/**
		 * Assigns n times User u to the current cell. If no
		 * previous assignment is found, a new one is created.
		 * <u>If a previous assignment exists, it will be replaced</u>.
		 * To instead <i>add</i> users to the current cell use {@link #add(User, int)}.
		 * 
		 * @param u The user to assign.
		 * @param n How many users will be assigned.
		 */
		public void assign(User u, int n) {
			Integer currentCount = assignments.get(u);
			
			assignments.put(u, n);
			
			if (currentCount != null) {
				fulfilled -= currentCount * typeTasks[u.m]; 
			} else {
				typeIndex[u.m].add(u);
			}
			
			fulfilled += n * typeTasks[u.m];
		}
		
		/**
		 * Adds n Users u to the current cell. If no previous
		 * assignment is found, a new one is created. If a previous
		 * assignment exists, the new number of users will be added to
		 * the current one.
		 * 
		 * @param u The user to add.
		 * @param n How many users will be added.
		 */
		public void add(User u, int n) {
			Integer currentCount = assignments.get(u);

			if (currentCount == null) {
				currentCount = 0;
				typeIndex[u.m].add(u);
			}
			
			assignments.put(u, currentCount + n);
			
			fulfilled += n * typeTasks[u.m];
		}
		
		/**
		 * Removes n Users u from the current cell. If n equals the
		 * current number of assigned Users u, any reference to u will be
		 * eliminated.
		 * 		
		 * @param u The user to remove.
		 * @param n How many user to remove.
		 * @throws RuntimeException If no User u is assigned to this cell or
		 * if trying to remove more users than those that are assigned.
		 */
		public void remove(User u, int n) {
			Integer currentCount = assignments.get(u);
			
			if (currentCount == null) {
				throw new RuntimeException("Tried to remove a user which is not in this cell!");
			} else if (currentCount < n){
				throw new RuntimeException("Tried to remove more users than there are in this cell!");
			}
			
			if (currentCount > n) {
				assignments.put(u, currentCount - n);
			} else {
				assignments.remove(u);
				typeIndex[u.m].remove(u);
			}
			
			fulfilled -= n * typeTasks[u.m];
		}
		
		/**
		 * Returns how many Users of the given kind have been assigned
		 * to this cell.
		 * 
		 * @param u The user to check.
		 * @return
		 */
		public int getUserCount(User u) {
			return assignments.get(u);
		}
		
		/**
		 * Returns how many users of the given type are assigned to
		 * this cell.
		 * 
		 * @param type The type to count
		 * @return How many users of the given type are assigned.
		 */
		public int getTypeCount(int type) {
			int count = 0;
			for (User u : typeIndex[type]) {
				count += getUserCount(u);
			}
			return count;
		}
		
		/**
		 * Returns an array where result[m] is the number of
		 * users of type m inside this cell.
		 * 
		 * @return The type count array.
		 */
		public int[] getAllTypesCount() {
			int[] counts = new int[nTypes];
			for (int m = 0 ; m < nTypes ; m++) {
				counts[m] = getTypeCount(m);
			}
			return counts;
		}
		
		/**
		 * Returns the {@link Set} of all Users in this cell.
		 * @return The User Set.
		 */
		public Set<User> getUserSet() {
			return assignments.keySet();
		}
		
		/**
		 * Add all users in the input map's key set by its associated integer value.
		 * 
		 * @param userCounts The user map to assign.
		 * @see #add(User, int)
		 */
		public void addAll(HashMap<User, Integer> userCounts) {
			for (User u : userCounts.keySet()) {
				add(u, userCounts.get(u));
			}
		}
		
		/**
		 * Removes all users in the input map's key set by its associated integer value.
		 * 
		 * @param userCounts The user map to remove.
		 * @see #remove(User, int)
		 */
		public void removeAll(HashMap<User, Integer> userCounts) {
			for (User u : userCounts.keySet()) {
				remove(u, userCounts.get(u));
			}
		}
		
		/**
		 * Evaluates which users and how many for each triplet extract from this
		 * cell in order to satisfy assignments from a given {@link Combination}.
		 * 
		 * @param c The combination to satisfy
		 * @return A HashMap which links each extracted user to the amount to extract.
		 * This Map is guaranteed to satisfy the input Combination.
		 */
		public HashMap<User, Integer> extractFromThis(Combination c) {
			HashMap<User, Integer> extraction = new HashMap<User, Integer>();
			int[] values = c.values();
			for (int m = 0 ; m < nTypes ; m++) {
				int q = values[m];
				int extracted = 0;
				for (User u : typeIndex[m]) {
					if (extracted >= q) {
						break;
					}					
					int howMany = Math.min(getUserCount(u), q - extracted);
					extraction.put(u, howMany);
					extracted += howMany;
				}
			}
			return extraction;
		}

		/**
		 * Manually computes how many tasks have been fulfilled. Used
		 * for coherence checks.
		 * 
		 * @return How many tasks have been fulfilled (manually computed).
		 */
		public int computeFulfilled() {
			int computedFullfilled = 0;
			for (User u : getUserSet()) {
				computedFullfilled += typeTasks[u.m] * assignments.get(u); 
			}
			return computedFullfilled;
		}
		
		/**
		 * Manually computes how many users of the given type have been assigned
		 * to this cell. Used for coherence checks.
		 * 
		 * @param type The type to analyze.
		 * @return How many users of the given type have been assigned.
		 */
		public int computeAssignedOfType(int type) {
			int assigned = 0;
			for (User u : typeIndex[type]) {
				assigned += getUserCount(u);
			}
			return assigned;
		}
	}
	/**
	 * A Solution for this Problem (D'uh). This class contains
	 * an array for all destinations cell called the Solution Matrix
	 * (or solMatrix). Each element is an instance of the {@link Cell}
	 * class. An additional "cell" is given which contains all Users
	 * that have not been assigned so far. This cell is not relevant
	 * in computing the total cost and in fact it is only exploited
	 * to build new solutions.
	 * 
	 * @author Alessio
	 *
	 */
	public class Solution{
		private ArrayList<Cell> solMatrix;
		private Cell unassignedUsers;
		private int[] totalCustomers;	// Contains how many customers of a given type (m) have been assigned
		private int totalCost;
		private float elapsedMillis;

		/**
		 * Builds a new, empty solution to this problem.
		 * All users are put in the Unassigned Cell. Hence
		 * this solution is unfeasible by definition.
		 * 
		 */
		private Solution() {
			solMatrix = new ArrayList<Cell>();
			unassignedUsers = new Cell(0);
			totalCustomers = new int[nTypes];
			totalCost = 0;
			elapsedMillis = 0;
			
			for (int i = 0 ; i < nCells ; i++) {
				solMatrix.add(new Cell(tasksToDo[i]));
			}
			
			for (User u : customers.keySet()) {
				unassignedUsers.add(u, customers.get(u));	
			}
		}

		public int getTotalCost() {
			return totalCost;
		}
		
		public float getElapsedMillis() {
			return elapsedMillis;
		}

		/**
		 * Outputs the solution for non-zero values of x[i][j][m][t].
		 * The result is ordered by destination cell.
		 */
		public String toVerbose(){
			String output = "" + nCells + "; " + nPeriods + "; " + nTypes + "\n";
			
			for (Cell j : solMatrix) {
				if (j.assignments.size() == 0)
					continue;
				for (User u : j.assignments.keySet()) {
					output += u.i + ";" + solMatrix.indexOf(j) + ";" + u.m + ";" + u.t + ";" + j.assignments.get(u) + "\n";
				}
			}
			return output;
		}
		
		public int getCountOfType(int type) {
			return totalCustomers[type];
		}
		
		/**
		 * Computes the actual cost of this solution. Used for integrity checks.
		 * 
		 * @return The (computed) cost of this solution
		 */
		public int computeCost() {
			int cost = 0;
			for (int j = 0 ; j < nCells ; j++) {
				Cell c = solMatrix.get(j);
				for (User u : c.getUserSet()) {
					cost += c.getUserCount(u) * costs[u.i][j][u.m][u.t];
				}
			}
			return cost;
		}
		
		/**
		 * Checks if this solution is coherent, that is if the dynamically computed values 
		 * inside this solution match those actually derived from the assignments.
		 * Checks are performed against fulfillment of tasks, total cost and number of
		 * users assigned per type. 
		 * 
		 * @return the coherence status of this solution.
		 * @see Coherence
		 */
		public Coherence isCoherent() {
			for (Cell c : solMatrix) {
				if (c.computeFulfilled() != c.fulfilled)
					return Coherence.INC_FULLFILL;
			}
			
			if (computeCost() != totalCost)
				return Coherence.INC_COST;
			
			for (int m = 0 ; m < nTypes ; m++) {
				int typeCount = 0;
				for (Cell c : solMatrix) {
					typeCount += c.getTypeCount(m);
				}
				if (typeCount != totalCustomers[m]) {
					return Coherence.INC_CUSTOMERS;
				}
			}
			
			return Coherence.COHERENT;
		}
	}
	
	/**
	 * Describe the coherence of a given solution.
	 * 
	 * <dl>
	 * 	<dt>INC_FULLFILL</dt>
	 * 		<dd>The value of {@link Cell#fulfilled} is incoherent for one or more cells.</dd>
	 * 	<dt>INC_COST</dt>
	 * 		<dd>The value of {@link Solution#totalCost} is incoherent.</dd>
	 * 	<dt>INC_CUSTOMERS</dt>
	 * 		<dd>The value of {@link Solution#totalCustomers} is incoherent for one or more values of m.</dd>
	 * 	<dt>COHERENT</dt>
	 * 		<dd>All values are coherent.</dd> 
	 * </ul>
	 * 
	 * @author Alessio
	 *
	 */
	public enum Coherence {
		INC_FULLFILL,
		INC_COST,
		INC_CUSTOMERS,
		COHERENT
	}
	
	/**
	 * A Combination is a possible solution for a certain amount of tasks.
	 * To perform N tasks, we can use different quantities of users of different types,
	 * so we use combinations to explore all the solutions.
	 * r, also called the residual, contains the difference between the tasks to perform and the 
	 * tasks performed by the solution, and it can be greater, equal or lower than zero.
	 * The penalty of a Combinations consists of the global number of users it uses.
	 * 
	 * @author Kevin
	 * 
	 */
	public class Combination implements Comparable<Combination> {
		public int q1;
		public int q2;
		public int q3;
		public int r;
		public int penalty;

		public Combination(int q1, int q2, int q3, int r) {
			this.q1 = q1;
			this.q2 = q2;
			this.q3 = q3;
			this.r = r;
			this.penalty = this.q1 + this.q2 + this.q3;
		}

		public String toString(){
			String s = this.q1 + "*" + typeTasks[0] 
				+ " + " + this.q2 + "*" + typeTasks[1] 
				+ " + " + this.q3 + "*" + typeTasks[2] 
				+ " + " + this.r 
				+ " Penalty: "  + this.penalty;
			return s;
		}

		/** 
		 * <p>
		 * Returns a negative value if THIS < C
		 * Returns zero if THIS = C
		 * Returns a positive value if THIS > C
		 * </p>
		 * 
		 * <p>
 		 * "Badness" of a combination takes into account:
		 * <ol>
		 * 	<li>The residual, which should be as low as possible</li>
		 *	<li>The number of used users, because we have to try to low it as well</li>
		 * </ol>
		 * </p>
		 * A Combination is worse than another if it wastes more tasks.
		 * If two combinations waste the same amount of tasks, the one
		 * which uses less users is better.
		 *	@param c
		 * 
		 */
		@Override
		public int compareTo(Combination c){
			int i = (int)Math.abs(r) - (int)Math.abs(c.r);
			if(i != 0)
				return i;
			return this.penalty - c.penalty;
		}
		
		public int[] values() {
			return new int[]{q1, q2, q3};
		}
	}
	/**
	 * Defines the feasibility of a Solution.
	 * 
	 * <ul>
	 * 	<li> FEASIBLE : denotes a feasible solution (d'uh).</li>
	 * 	<li> UNF_DEMAND : denotes an unfeasible solution that doesn't
	 * fulfil all required tasks</li>
	 * 	<li> UNF_CUSTOMERS : denotes an unfeasible solution that assigns
	 * more users than those available.</li>
	 * </ul>
	 * 
	 * @author Alessio
	 *
	 */
	public enum Feasibility {
		FEASIBLE,
		UNF_DEMAND,
		UNF_CUSTOMERS
	}

	/**
	 * Loads a new problem instance from the given source file.
	 * 
	 * @param inputFile
	 */
	public Problem(String inputFile) {
		BufferedReader in;

		try {
			in = new BufferedReader(new FileReader(inputFile));

			// Read and parse first line, containing the cardinalities of the problem
			String[] line = in.readLine().split(" ");
			nCells = Integer.parseInt(line[0]);
			nPeriods = Integer.parseInt(line[1]);
			nTypes = Integer.parseInt(line[2]);

			typeTasks = new int[nTypes];
			costs = new int[nCells][nCells][nTypes][nPeriods];
			tasksToDo = new int[nCells];
			customers = new HashMap<User, Integer>();

			in.readLine();

			line = in.readLine().split(" ");

			// Read and parse the second line, containing the tasks each user type can do
			for (int i = 0 ; i < nTypes ; i++)
				typeTasks[i] = Integer.parseInt(line[i]);

			in.readLine();

			// Read and parse the costs matrices
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				int m = Integer.parseInt(line[0]);	// Type
				int t = Integer.parseInt(line[1]);  // Period

				for (int i = 0; i < nCells ; i++) {
					line = in.readLine().split(" ");
					for (int j = 0 ; j < nCells ; j++) {
						costs[i][j][m][t] = (int)Math.floor(Double.parseDouble(line[j]));
					}
				}
			}

			in.readLine();

			// Read and parse the tasks to do
			line = in.readLine().split(" ");
			for (int i = 0 ; i < nCells ; i++)
				tasksToDo[i] = Integer.parseInt(line[i]);

			in.readLine();

			// Read and parse the available users
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				int m = Integer.parseInt(line[0]);
				int t = Integer.parseInt(line[1]);

				line = in.readLine().split(" ");

				for (int i = 0 ; i < nCells ; i++) {
					int count = Integer.parseInt(line[i]);
					if (count == 0) continue;
					customers.put(new User(i, m, t), count);
				}
			}

			in.close();			
		} catch (IOException ioe) {
			System.err.println("Unable to read file: " + inputFile);
			ioe.printStackTrace();
			System.exit(1);
		}
		
		bestSolution = null;
	}

	public Solution solveProblem() {
		// solveProblem is the only function called by Main
		// Inside we do all computation
		start = System.nanoTime();
		Solution s = null;
		int bestCost = Integer.MAX_VALUE;
		long oneIterationTime = 0;

		while((System.nanoTime() - start + oneIterationTime) < GREEDY_THRESHOLD){
			long preIteration = System.nanoTime();
			s = greedy();
			oneIterationTime = System.nanoTime() - preIteration;
			if(s.totalCost < bestCost){
				bestSolution = s;
				bestCost = bestSolution.totalCost;
			}
		}

		if(checkFeasibility(bestSolution) != Feasibility.FEASIBLE)
			bestSolution = recoverUnfeasible();
		
		bestSolution.elapsedMillis = (System.nanoTime() - start) / 1E+6f;
		return bestSolution;
	}
	/**
	 * <p>
	 * This method is able to compute a 'desperate' solution in case
	 * regular greedy algorithms failed in getting a feasible solution.
	 * While overall performance is bad, it is able to get a feasible
	 * solution even with the tightest instances.
	 * </p>
	 * 
	 * <p>
	 * It works by selecting for every cell to be fulfilled the 
	 * best combination of users to assign, trying to minimize wasted tasks and users assigned.
	 * To have a better understanding please check {@link #getCombinations(int, int, int, int)}.
	 * </p>
	 * 
	 * @return An (almost?) failproof solution for the problem.
	 */
	public Solution recoverUnfeasible(){
		Solution feasible = new Solution();

		int[] availableUsers = feasible.unassignedUsers.getAllTypesCount();
        int possibleTasks = 0;
        for(int m = 0 ; m < nTypes ; m++) {
            possibleTasks += availableUsers[m] * typeTasks[m];
        }

		LinkedList<Integer> listJ = new LinkedList<>();
		for(int i = 0; i < nCells; i++){
			if (feasible.solMatrix.get(i).tasksToDo != 0) {
                listJ.add(i);
            }
		}
		Collections.sort(listJ, (i1, i2) -> tasksToDo[i1] - tasksToDo[i2]);
		
		for(int j : listJ){
			Cell c = feasible.solMatrix.get(j);

			// Added in second upload: these very same lines are in greedy() too, it was an unfortunate oversight
			for(int m = 0; m < nTypes; m++) {
                Collections.sort(feasible.unassignedUsers.typeIndex[m], (o1, o2)
                        -> costs[o1.i][j][o1.m][o1.t] - costs[o2.i][j][o2.m][o2.t]);
            }
            //---------------------------------------------------------------------------------------------------
			if(possibleTasks >= c.tasksToDo){
				LinkedList<Combination> comb = getCombinations(c.tasksToDo, 
									availableUsers[0], 
									availableUsers[1], 
									availableUsers[2]);
				Collections.sort(comb);
				Combination bestCombination = comb.get(0);

				HashMap<User, Integer> bestExtraction = feasible.unassignedUsers.extractFromThis(bestCombination);
	            feasible.unassignedUsers.removeAll(bestExtraction);
	            c.addAll(bestExtraction);
	            
	            for (User u : bestExtraction.keySet()) {
	            	availableUsers[u.m] -= bestExtraction.get(u);
	            	possibleTasks -= bestExtraction.get(u) * typeTasks[u.m];
	            	feasible.totalCost += costs[u.i][j][u.m][u.t] * bestExtraction.get(u);
	            	feasible.totalCustomers[u.m] += bestExtraction.get(u);
	            }
			}			
		}
		return feasible;
	}
	/**
	 * This method calculates all the possible feasible combinations of different
	 * types of users to perform a certain number of tasks.
	 * We assume that there will always be only 3 types of users
	 * AvailX will be the number of users available for each type, identified
	 * in the greedy function as the best possible choice for that given type
	 * 
	 * @param toDo How many tasks to do
	 * @param avail1 How many users of type 0 available
	 * @param avail2 How many users of type 1 available
	 * @param avail3 How many users of type 2 available
	 * @return The best combination possible of users to solve the given number of tasks
	 * 
	 * @see Combination#compareTo(Combination)
	 */
	public LinkedList<Combination> getCombinations(int toDo, int avail1, int avail2, int avail3){
		// No combination is allowed to contain more users than those actually available for each type
		int max1 = (int)Math.min(avail1, Math.ceil((float)toDo / typeTasks[0]));
		int max2 = (int)Math.min(avail2, Math.ceil((float)toDo / typeTasks[1]));
		int max3 = (int)Math.min(avail3, Math.ceil((float)toDo / typeTasks[2]));

		int t1 = typeTasks[0];
		int t2 = typeTasks[1];
		int t3 = typeTasks[2];

		LinkedList<Combination> combinations = new LinkedList<>();

		int result, remainder;
		for (int v1 = 0; v1 <= max1; v1++) {
			for (int v2 = 0; v2 <= max2; v2++) {
				for (int v3 = 0; v3 <= max3; v3++){
					if (!(v1 == 0 && v2 == 0 && v3 == 0)) {
						result = v1 * t1 + v2 * t2 + v3 * t3;	
						if(result >= toDo){
							remainder = result - toDo;
							Combination c = new Combination(v1, v2, v3, remainder);
							combinations.add(c);
							break;
						}
					}
				}
			}
		}
		return combinations;
	}
	/**
	 * This method tries to solve the problem in the following way:
	 * 
	 * <ol>
	 * 	<li>For each cell, sort the unassigned users by increasing cost.</li>
	 * 	<li>After doing that, evaluate the best combination<u>s</u> of users
	 * by calling {@link #getCombinations(int, int, int, int)}</li>
	 *  <li>Evaluate the cheapest combination among those found</li>
	 *  <li>Apply said combination</li>
	 * <ol>
	 * </p>
	 * 
	 * @return A Solution for the problem, by evaluating the best combination
	 * of users to assign to each cell.
	 */
	public Solution greedy(){
        Solution greedy = new Solution();
        LinkedList<Integer> listJ = new LinkedList<>();
        for(int i = 0; i < nCells; i++) {
            if (greedy.solMatrix.get(i).tasksToDo != 0) {
                listJ.add(i);
            }
        }
        Collections.shuffle(listJ);

        int[] availableUsers = greedy.unassignedUsers.getAllTypesCount();
        int possibleTasks = 0;
        for(int m = 0 ; m < nTypes ; m++) {
            possibleTasks += availableUsers[m] * typeTasks[m];
        }

        for (Integer j : listJ) {
            Cell c = greedy.solMatrix.get(j);
            if(possibleTasks < c.tasksToDo) {
            	// The solution will be unfeasible, so we stop without wasting time
                break;
            }

            for(int m = 0; m < nTypes; m++) {
                Collections.sort(greedy.unassignedUsers.typeIndex[m], (o1, o2)
                        -> costs[o1.i][j][o1.m][o1.t] - costs[o2.i][j][o2.m][o2.t]);
            }
            int[] required = new int[nTypes];
            int[] dispatchable = new int[nTypes];

            for(int m = 0; m < nTypes; m++){
                required[m] = (int)Math.ceil((float)c.tasksToDo / typeTasks[m]);
                dispatchable[m] = (int)Math.min(required[m], availableUsers[m]);
            }
            LinkedList<Combination> combinations = getCombinations(c.tasksToDo,
                    dispatchable[0],
                    dispatchable[1],
                    dispatchable[2]);
            LinkedList<Combination> bestCombinations = new LinkedList<>();
            int bestCombinationCost = Integer.MAX_VALUE;
            for(Combination comb : combinations){
                int cost = 0;
                
                HashMap<User, Integer> extraction = greedy.unassignedUsers.extractFromThis(comb);
                for (User u : extraction.keySet()) {
                	cost += costs[u.i][j][u.m][u.t] * extraction.get(u);
                }

                if(cost < bestCombinationCost){
                	bestCombinations.clear();
                    bestCombinations.add(comb);
                    bestCombinationCost = cost;
                } else if(cost == bestCombinationCost){
                	bestCombinations.add(comb);
                }
            }

            // This gives the same situation as before: the final cost changes according
            // to how Combinations are evaluated. The idea is to keep all optimal combinations
            // in bestCombinations and find a way to fine-tune the final assignment
            Collections.sort(bestCombinations);
            Combination bestCombination = bestCombinations.get(0);
            
            HashMap<User, Integer> bestExtraction = greedy.unassignedUsers.extractFromThis(bestCombination);
            greedy.unassignedUsers.removeAll(bestExtraction);
            c.addAll(bestExtraction);
            
            for (User u : bestExtraction.keySet()) {
            	availableUsers[u.m] -= bestExtraction.get(u);
            	possibleTasks -= bestExtraction.get(u) * typeTasks[u.m];
            	greedy.totalCost += costs[u.i][j][u.m][u.t] * bestExtraction.get(u);
            	greedy.totalCustomers[u.m] += bestExtraction.get(u);
            }
        }
        return greedy;
    }
	
	/**
	 * This method evaluate whether a solution is feasible or not, and if not
	 * the cause of unfeasibility.
	 * 
	 * @param sol The solution to evaluate
	 * @return The Feasibility of the given Solution.
	 */
	public Feasibility checkFeasibility(Solution sol) {
		for (int i = 0 ; i < nCells ; i++) {
			// Check against Task Demand constraint
			if (sol.solMatrix.get(i).fulfilled < tasksToDo[i])
				return Feasibility.UNF_DEMAND;
		}
		
		HashMap<User, Integer> cumulativeAssignments = new HashMap<User, Integer>();
		
		for (int j = 0 ; j < nCells ; j++) {
			Cell c = sol.solMatrix.get(j);
			for (User u : c.getUserSet()) {
				Integer currentCount = cumulativeAssignments.getOrDefault(u, 0);
				cumulativeAssignments.put(u, currentCount + c.getUserCount(u));
			}
		}
		
		for (User u : cumulativeAssignments.keySet()) {
			int assigned = cumulativeAssignments.get(u);
			int available = customers.get(u);
			// Check against over assignment of users
			if (assigned > available)
				return Feasibility.UNF_CUSTOMERS;
		}

		return Feasibility.FEASIBLE;
	}
}