import java.util.Arrays;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryIntegerVariable;
import org.moeaframework.problem.AbstractProblem;

/**
 * <b><u>CS220 Assignment #3 - NGSAII</b></u>
 * <br>
 * This is the main class which creates the problems to send to MOEA to 
 * evaluate for creating solutions using the NGSAII algorithm.
 * 
 * This code was created with the help of the following website:
 * http://keyboardscientist.weebly.com/blog/moea-framework-defining-new-problems
 * 
 * @author Lisa Chen
 * @since Nov 24, 2019
 * @version 1.0
 */
public class GraphColorWithAffinityProblem extends AbstractProblem {

	private static final int NUM_OBJECTIVES = 2;
	private final int NUM_CONSTRAINTS;
	private int maxNumColors;
	private int numConstraints;
	private final int NUM_VERTS;
	private int[][] edgeMatrix;	
	private int[] colorAssignment;
	private int constraintNotSatisfied = -1;
	private int constraintSatisfied = 0;
	//to denote which edge is which in the edge matrix
	private final int INTERFERENCE_EDGE_MARKER = 1;
	private final int AFFINITY_EDGE_MARKER = 2;
	private int minColorsObjectiveIndex = 0;
	private int affinityObjectiveIndex = 1;
	
	/**
	 * This constructor is called automatically when using an Executor to solve 
	 * this problem class. The constructor initiates variables necessary for 
	 * the objectives and constraints as well as setting the number of vertices, 
	 * number of objectives, and number of constraints for the problem.
	 */
	public GraphColorWithAffinityProblem() {
		super(GraphColoringTest.getNumVertices(), NUM_OBJECTIVES, 
				GraphColoringTest.getTotalConstraints());
		
		NUM_CONSTRAINTS = GraphColoringTest.getTotalConstraints();
		maxNumColors = GraphColoringTest.getMaxNumColors();
		NUM_VERTS = GraphColoringTest.getNumVertices();
		edgeMatrix = GraphColoringTest.getEdgeMatrix();
		colorAssignment = new int[NUM_VERTS];
	}


	/**
	 * Evaluates the problem and stores the solutions created by multi-objective 
	 * algorithm based on set objectives and constraints for the problem.
	 * @param solution The solution object to store the solutions 
	 */
	@Override
	public void evaluate(Solution solution) {
		assignVariablesToColorAssignmentArray(solution);
		setObjectives(solution);
		setConstraints(solution);
	}
	
	/**
	 * Assigns the variables from the problem (linked to the solution object) 
	 * to the color assignment array to work with the objectives and 
	 * constraints.
	 * @param solution The solution where the variables are connected to
	 */
	private void assignVariablesToColorAssignmentArray(Solution solution) {
		for (int vert = 0; vert < NUM_VERTS; vert++) {
			colorAssignment[vert] = ((BinaryIntegerVariable)solution.
					getVariable(vert)).getValue();
		}
	}
	
	/**
	 * Sets the objectives for the problem that the solution must try to 
	 * optimize to meet.
	 * @param solution The solution object to add the objective for the results
	 */
	private void setObjectives(Solution solution) {
		int currentMax = 0;
		int affinitySatisfied = 0;
		
		//minimizing colors used objective by detecting highest color # used
		for (int vert = 0; vert < NUM_VERTS; vert++) {
			currentMax = currentMax > colorAssignment[vert]? currentMax : 
				colorAssignment[vert];
		}
		solution.setObjective(minColorsObjectiveIndex, currentMax);
				
		//objective of maxxing affinitySatisfied (minimize: -affinitySatisfied)
		for (int vert1 = 0; vert1 < NUM_VERTS; vert1++) {
			for (int vert2 = vert1 + 1; vert2 < NUM_VERTS; vert2++) {
				if (edgeMatrix[vert1][vert2] == AFFINITY_EDGE_MARKER) {
					if(colorAssignment[vert1] == colorAssignment[vert2])
						affinitySatisfied++;
				}
			}
		}
		solution.setObjective(affinityObjectiveIndex, -affinitySatisfied);
	}
	
	/**
	 * Sets the interference edge and colors assigned consecutively constraints 
	 * for the problem that the solution must obey.
	 * @param solution The solution object to constrain the results
	 */
	private void setConstraints(Solution solution) {
		int constraintIndex = 0;
		//interference edge constraint
		for (int vert1 = 0; vert1 < NUM_VERTS; vert1++) {
			for (int vert2 = vert1 + 1; vert2 < NUM_VERTS; vert2++) {
				if (edgeMatrix[vert1][vert2] == INTERFERENCE_EDGE_MARKER) {
					solution.setConstraint(constraintIndex++, 
							colorAssignment[vert1] != colorAssignment[vert2] ? 
							constraintSatisfied : constraintNotSatisfied);
				}
			}
		}
		
		//color numbers must be used consecutively constraint
		int[] sortedColors = colorAssignment;
		Arrays.sort(sortedColors);
		for (int vert = 1; vert < NUM_VERTS; vert++) {
			solution.setConstraint(constraintIndex++, colorAssignment[vert] - 
					colorAssignment[vert-1] <= 1 ? constraintSatisfied : 
						constraintNotSatisfied);
		}
	}
	


	/**
	 * Creates a new solution object with set variables, the number of 
	 * objectives, and number of constraints.
	 * @return A new empty solution object with variables set
	 */
	@Override
	public Solution newSolution() {
		Solution solution = new Solution(NUM_VERTS, NUM_OBJECTIVES, 
				NUM_CONSTRAINTS);
		for (int i = 0; i < NUM_VERTS; i++)
			solution.setVariable(i, new BinaryIntegerVariable(1,maxNumColors));
		return solution;
	}

}