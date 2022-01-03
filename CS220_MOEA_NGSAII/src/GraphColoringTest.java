import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

/**
 * <b><u>CS220 Assignment #3 - NGSAII</b></u>
 * <br>
 * This class creates the model and parameters necessary to interact with 
 * NGSAII using MOEA Framework.
 * The goal is to use the optimizer to solve a variant of the Graph Coloring 
 * Problem with two objectives: minimizing colors used and maximizing affinity 
 * edges.
 * 
 * The inputs are 3 .txt files with inputs of a graph and 2 disjoint set of 
 * edges: interference edges and affinity edges. Interference edges connect 
 * vertices with different colors while affinity edges are "satisfied" if it 
 * connects vertices of the same color. 
 * 
 * Output is a text file containing:
 * All pareto solutions with each solutions's number of colors used, 
 * affinity edges satisfied, and the resulting assignment of colors for each 
 * vertex.
 * 
 * This code was created with the help of the following websites:
 * http://keyboardscientist.weebly.com/blog/moea-framework-defining-new-problems
 * http://moeaframework.sourceforge.net/javadoc/org/moeaframework/core/spi/OperatorFactory.html
 *
 * @author Lisa Chen
 * @since Nov 24, 2019
 * @version 1.0
 */
public class GraphColoringTest {
	private static final String OUTPUT_FILENAME = "Assignment3_Demo";
	private static int minColorsObjectiveIndex = 0;
	private static int affinityObjectiveIndex = 1;
	private static GraphFileReader gfr;
	private static int totalConstraints;
	private static int numVert;
	private static int maxNumColors;
	

	public static void main(String[] args) throws FileNotFoundException {
		int fileNum = 1;
		gfr = new GraphFileReader(fileNum);
		numVert = gfr.getNumVertices();
		double rate = 1/numVert;
		
		//runs NGSAII algorithm with given parameters
		NondominatedPopulation result = new Executor()
				.withAlgorithm("NSGAII")
				.withProblemClass(GraphColorWithAffinityProblem.class)
				.withMaxEvaluations(10000)
			     .withProperty("sbx.rate", rate) //simulated binary crossover
			     .withProperty("sbx.distributionIndex", 15.0)
			     .withProperty("pm.rate", rate) //polynomial mutation
			     .withProperty("pm.distributionIndex", 15.0)
			     .withProperty("ux.rate", 0.2) //uniform crossover
			     .withProperty("populationSize", 100)
				.run();
		
		System.out.println("Pareto Front size: " + getNumValidSolutions(result));
		for (Solution solution : result) {
			if (!solution.violatesConstraints()) {
				System.out.format("Colors used:%3.0f   Affinity Edges "
						+ "Satisfied:%3.0f%n",
						solution.getObjective(minColorsObjectiveIndex),
						-solution.getObjective(affinityObjectiveIndex));

				for (int vert = 1; vert <= numVert; vert++) 
					System.out.format("%3s ","V" + vert);
				System.out.println();
				for (int vert = 0; vert < numVert; vert++)
					System.out.format("%3s ",solution.getVariable(vert));
				System.out.println("\n");
			}
		}
		
		createOutputFile(fileNum, result);
	}
	
	/**
	 * Creates the filename for all the pareto solutions found by the NGSAII 
	 * algorithm per the requirements (one line each):
	 * One number (N) associated with the number of solutions
	 * N lines where each line has the number of colors used and number of 
	 * satisfied affinity edges.
	 * N|V| lines which lists the color assignment for each of the solutions
	 * @param fileNum The number associated with the input filename
	 * @param result The result found by the NGSAII algorithm
	 * @throws FileNotFoundException
	 */
	private static void createOutputFile(int fileNum, NondominatedPopulation 
			result) throws FileNotFoundException {
		String filename = OUTPUT_FILENAME + fileNum + ".txt";
		File output = new File(filename);
		PrintWriter printer = new PrintWriter(output);
		
		int numSolutions = getNumValidSolutions(result);
		printer.println(numSolutions + "\n");
		
		if (numSolutions > 0) {
			//print the N lines for colors used and satisfied affinity edges
			for (Solution solution : result) {
				printer.format ("%-1.0f %1.0f%n", 
						solution.getObjective(minColorsObjectiveIndex),
						-solution.getObjective(affinityObjectiveIndex));
			}
			printer.println();
			
			//print the N|V| lines for for the color assignments
			for (Solution solution : result) {
				for (int vert = 0; vert < numVert; vert++) {
					printer.println(solution.getVariable(vert));
				}
				printer.println();
			}
		}
		printer.close();
		
	}
	
	private static int getNumValidSolutions(NondominatedPopulation result) {
		Solution solution = result.get(0);
		return solution.violatesConstraints() ? 0 : result.size();
	}
	

	/**
	 * Retrieves the total number of constraints that the problem is bounded by: 
	 * interference edge constraints (equal to the number of interference edges) 
	 * and consecutive usage of color assignment (which is to number of vertices 
	 * minus one = total number of comparisons required for constraint).
	 * @return The total number of constraints for hte problem
	 */
	public static int getTotalConstraints() {
		return gfr.getNumInterferenceEdges() + numVert - 1; 
		}

	/**
	 * Retrieves the max number of colors that can be used to color the graph.
	 * @return The max number of colors
	 */
	public static int getMaxNumColors() { return  gfr.getMaxNumColors(); }
	
	/**
	 * Retrieves the number of vertices in the graph.
	 * @return The graph's number of vertices.
	 */
	public static int getNumVertices() { return numVert; }
	
	/**
	 * Gets the edge matrix created from the information from the graph file. 
	 * The edge matrix contains information for affinity and interference edges.
	 * @return The edge matrix
	 */
	public static int[][] getEdgeMatrix() { return gfr.getEdgeMatrix(); }
}