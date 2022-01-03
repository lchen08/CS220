import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import gurobi.*;

/**
 * <b><u>CS220 Assignment #2 - ILP</b></u>
 * <br>
 * This class creates the model and parameters necessary to interact with the 
 * Gurobi optimizer. 
 * The goal is to use the optimizer to solve a variant of the Graph Coloring 
 * Problem using ILP. 
 * 
 * The inputs are 3 .txt files with inputs of a graph and 2 disjoint set of 
 * edges: interference edges and affinity edges. Interference edges connect 
 * vertices with different colors while affinity edges are "satisfied" if it 
 * connects vertices of the same color. The goal is to maximize the number of 
 * affinity edges while using only the amount of colors = chromatic number.
 * 
 * Output is a text file containing:
 * Chromatic number of G, ignoring affinity edges.
 * The number of satisfied affinity edges in the optimal solution
 * The color assigned to each vertex
 *
 * @author Lisa Chen
 * @since Nov 08, 2019
 * @version 2.0
 */
public class Gurobi_Main {
	private int qtyVert;
	private int qtyMaxColors;
	private int qtyInterferenceEdge;
	private int qtyAffinityEdge;
	private int[][] edgeMatrix;
	
	private GRBEnv env; 
	private GRBModel model;
	private GRBVar[][][] satisfiedAffMatrix;
	private GRBVar[][] colorAssignment;
	private GRBVar[] colorsUsed;
	private GRBLinExpr chromaticNumber;
	private GRBLinExpr satisfiedAffinityEdges;
	
	//to denote which edge is which in the edge matrix
	private final int INTERFERENCE_EDGE_MARKER = 1;
	private final int AFFINITY_EDGE_MARKER = 2;
	
	private final File FILE_1 = new File("sample_1.txt");
	private final File FILE_2 = new File("sample_2.txt");
	private final File FILE_3 = new File("sample_3.txt");
	private final File FILE_4 = new File("ImageProbeSynthesis.txt");
	private final String ASSIGNMENT_NAME_PREFIX = "colorassign";
	private final String COLOR_NAME_PREFIX = "color";
	private final String OUTPUT_FILENAME = "Demo";
	
	public static void main(String[] args) {
		try {
			int fileNum = 4;
			Gurobi_Main gurobi = new Gurobi_Main(fileNum);
		} catch (GRBException e) {
		      System.out.println("Error code: " + e.getErrorCode() + ". " + 
		    		  e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Runs the full program, which creates the constraints and expressions to 
	 * feed into Gurobi to solve the graph coloring problem using ILP. This 
	 * solves both standard graph coloring with no affinity edges and also 
	 * affinity edges added. 
	 * @param fileNum The number associated with the sample filename.
	 * @throws GRBException
	 * @throws FileNotFoundException
	 */
	public Gurobi_Main(int fileNum) throws GRBException, FileNotFoundException {
		env = new GRBEnv(true);
		env.set("logFile", "mip1.log");
		env.start();
		
		File selectedFile;
		
		switch (fileNum) {
			case 1: selectedFile = FILE_1; break;
			case 2: selectedFile = FILE_2; break;
			case 3: selectedFile = FILE_3; break;
			case 4: selectedFile = FILE_4; break;
			default: selectedFile = null;
		}
		processGraphDataFile(selectedFile);
		solveStdGraphColoring();
		solveAffinityColor();
		printOutputs();
		createOutputFile(fileNum);
	}
	
	/**
	 * Creates the filename per the requirements (one line each):
	 * One number associated with the chromatic number of the sample graph
	 * One number associated with the number of satisfied affinity edges
	 * Total of V (# of vertices) lines with one number per line associated with
	 * the color assigned to that vertex.
	 * @param fileNum The number associated with the input filename
	 * @throws FileNotFoundException
	 * @throws GRBException
	 */
	private void createOutputFile(int fileNum) throws FileNotFoundException, 
	GRBException {
		String filename = OUTPUT_FILENAME + fileNum + ".txt";
		File output = new File(filename);
		PrintWriter printer = new PrintWriter(output);
		
		printer.println((int) chromaticNumber.getValue());
		printer.println((int) satisfiedAffinityEdges.getValue());
		
		for (int i = 0; i < qtyVert; i++) {
			for (int j = 0; j < qtyMaxColors; j++) {
				double assignment = colorAssignment[i][j].get(GRB.DoubleAttr.X);
				if (assignment > 0) {
					printer.println(j+1);
					j = qtyMaxColors;
				}
			}
		}
		printer.close();
	}
	
	/**
	 * Solves the standard graph coloring program using ILP and feeding into 
	 * the Gurobi solver. Adds all variables, constrains, and objective needed 
	 * for Gurobi. 
	 * @throws GRBException
	 */
	private void solveStdGraphColoring() throws GRBException {
		createGurobiModel();
		initializeStdColoringVars();
		setStdGraphColorObjective();
		addInterferenceColorConstraint();
		addOneColorPerVertexConstraint();
		addColoringConsecutiveConstraint();
		model.optimize();
	}
	
	/**
	 * Solves the graph coloring program with maximizing satisfying affinity 
	 * edges by using ILP and feeding into the Gurobi solver. Adds all 
	 * variables, constrains, and objective needed for Gurobi. 
	 * @throws GRBException
	 */
	private void solveAffinityColor() throws GRBException {
		createGurobiModel();
		setMaxColorsEqualChromaticNum();
		initializeAffinityColoringVars();
		setAffinityEdgeObjective();
		addInterferenceColorConstraint();
		addOneColorPerVertexConstraint();
		addColoringConsecutiveConstraint();
		addAffinitySatisfiedLinkageConstraint();
		model.optimize();
	}
	
	/**
	 * Adds to the Gurobi model the constraint requiring satisfied affinity 
	 * edges marked as satisfied only when connected vertices are the same 
	 * color. All other vertices that are not connected by affinity edges are 
	 * marked as unsatisfied as default. 
	 * @throws GRBException
	 */
	private void addAffinitySatisfiedLinkageConstraint() throws GRBException {
		double stdCoef = 1.0;
		double affinityCoeff = 2.0;
		double noAff = 0;
		for (int vert1 = 0; vert1 < qtyVert; vert1++) {
			for (int vert2 = 0; vert2 < qtyVert; vert2++) {
				if (edgeMatrix[vert1][vert2] == AFFINITY_EDGE_MARKER) {
					for (int color = 0; color < qtyMaxColors; color++) {
						GRBLinExpr affColor = new GRBLinExpr();
						GRBLinExpr constraint = new GRBLinExpr();
						affColor.addTerm(stdCoef, colorAssignment[vert1][color]);
						affColor.addTerm(stdCoef, colorAssignment[vert2][color]);
						constraint.addTerm(affinityCoeff, 
								satisfiedAffMatrix[vert1][vert2][color]);
						model.addConstr(constraint, GRB.LESS_EQUAL, affColor, 
								"LinkSatisfiedAffinity" + color);
					}
				}
				/*
				don't really need this - just makes it easier to see for the user 
				and satisfies the logic behind it, but the optimizer does not 
				look at these cells to maximize affinity satisfaction */
				else {
					for (int color = 0; color < qtyMaxColors; color++) {
						model.addConstr(satisfiedAffMatrix[vert1][vert2][color], 
								GRB.EQUAL, noAff, "noAff" + vert1 + "_" + vert2);
					}
				}
			}
		}
	}
	
	/**
	 * This is to adds the requirement that the Gurobi model can only use 
	 * the number of colors = chromatic number (minimal number to satisfy
	 * interference edges). This is only for the affinity edge satisfaction 
	 * optimization problem only.
	 */
	private void setMaxColorsEqualChromaticNum() throws GRBException {
		qtyMaxColors = (int) chromaticNumber.getValue();
	}
	
	/**
	 * Adds to the Gurobi model the constraint where interference edges must 
	 * be obeyed when coloring vertices. Also handles that if a color is 
	 * marked as used in the color assignment matrix, the color is marked as 
	 * used in colorUsed (Note: It does not check the reverse: if color in 
	 * colorUsed is marked, there is no check that there exists a vertex colored 
	 * in that color. For standard graph coloring, there is no issue since 
	 * the minimizing of colors used prevents this from happening.).  
	 * @throws GRBException
	 */
	private void addInterferenceColorConstraint() throws GRBException {
		double coeff = 1.0;
		for (int vert1 = 0; vert1 < qtyVert; vert1++) {
			for (int vert2 = 0; vert2 < qtyVert; vert2++) {
				if (edgeMatrix[vert1][vert2] == INTERFERENCE_EDGE_MARKER) {
					for (int color = 0; color < qtyMaxColors; color++) {
						GRBLinExpr constraint = new GRBLinExpr();
						constraint.addTerm(coeff, colorAssignment[vert1][color]);
						constraint.addTerm(coeff, colorAssignment[vert2][color]);
						model.addConstr(constraint, GRB.LESS_EQUAL, 
								colorsUsed[color], "InterferenceConstr" + color);
					}
				}
			}
		}
	}
	
	/**
	 * Adds to the model the constraint that colors are used sequentially. 
	 * @throws GRBException
	 */
	private void addColoringConsecutiveConstraint() throws GRBException {
		for (int i = 1; i < qtyMaxColors; i++)
			model.addConstr(colorsUsed[i], GRB.LESS_EQUAL, colorsUsed[i-1], 
					"ConsecutiveConstr" + i);
	}
	
	/**
	 * Adds to the model the constraint that vertices can only be colored one 
	 * color; no more or less. 
	 * @throws GRBException
	 */
	
	private void addOneColorPerVertexConstraint() throws GRBException {
		double colorIsUsed = 1.0;
		for (int vertex = 0; vertex < qtyVert; vertex++) {
			GRBLinExpr constraintExpr = new GRBLinExpr();	
			addArrayTermsToExpr(constraintExpr, colorAssignment[vertex]);
			model.addConstr(constraintExpr, GRB.EQUAL, colorIsUsed, 
					"VertexColorConstr" + vertex);
		}
			
	}
	
	/**
	 * Adds all the terms in an array to a Gurobi expression. 
	 * @param expr The expression to add the terms
	 * @param termArray The array holding the terms to add
	 * @param arrayLength The length of the array
	 */
	private void addArrayTermsToExpr(GRBLinExpr expr, GRBVar[] termArray) {
		double coeff = 1.0;
		for (int i = 0; i < termArray.length; i++)
			expr.addTerm(coeff, termArray[i]);
	}

	/** 
	 * Initializes the variables used for optimizing in Gurobi for the standard 
	 * graph coloring (no affinity edges). All Gurobi variables are binary. 
	 */
	private void initializeStdColoringVars() throws GRBException {
		colorsUsed = new GRBVar[qtyMaxColors];
		for (int i = 0; i < qtyMaxColors; i++) {
			String colorName = COLOR_NAME_PREFIX + (i + 1);
			colorsUsed[i] = model.addVar(0, 1, 0, GRB.BINARY, colorName);
		}
		
		colorAssignment = new GRBVar[qtyVert][qtyMaxColors];
		for (int i = 0; i < qtyVert; i++) {
			for (int j = 0; j < qtyMaxColors; j++) {
				String assigmentName = ASSIGNMENT_NAME_PREFIX + (i+1) + (j+1);
				colorAssignment[i][j] = model.addVar(0, 1, 0, GRB.BINARY, 
						assigmentName);
			}
		}
	}
	
	/**
	 * Initializes the variables used for optimizing in Gurobi for including 
	 * affinity edges when handling graph coloring. All Gurobi variables are 
	 * binary.
	 * @throws GRBException
	 */
	private void initializeAffinityColoringVars() throws GRBException {
		initializeStdColoringVars();

		satisfiedAffMatrix = new GRBVar[qtyVert][qtyVert][qtyMaxColors];
		for (int i = 0; i < qtyVert; i++) {
			for (int j = 0; j < qtyVert; j++) {
				for (int k = 0; k < qtyMaxColors; k++) {
					String name = "affinity" + (i+1) + (j+1) + (k+1);
					satisfiedAffMatrix[i][j][k] = model.addVar(0, 1, 0, 
							GRB.BINARY, name);
				}
			}
		}
	}
	
	/** 
	 * Sets the objective to maximizing the number of satisfied affinity edges 
	 * (edges connecting vertices that are the same color). 
	 * @throws GRBException 
	 */
	private void setAffinityEdgeObjective() throws GRBException {
		double coeff = 1.0;
	    satisfiedAffinityEdges = new GRBLinExpr();
	    
	    for (int vert1 = 0; vert1 < qtyVert; vert1++) {
	    	for (int vert2 = vert1+1; vert2 < qtyVert; vert2++) {
	    		if (edgeMatrix[vert1][vert2] == AFFINITY_EDGE_MARKER) {
	    			for (int color = 0; color < qtyMaxColors; color++) {
		    			satisfiedAffinityEdges.addTerm(coeff, 
		    					satisfiedAffMatrix[vert1][vert2][color]);
	    			}
	    		}
	    	}
	    }
	    model.setObjective(satisfiedAffinityEdges, GRB.MAXIMIZE);
	}
	
	/**
	 * Sets the standard graph coloring objective: minimizing the number of 
	 * colors used to color the graph.
	 * @throws GRBException 
	 */
	private void setStdGraphColorObjective() throws GRBException {
		chromaticNumber = new GRBLinExpr();
		addArrayTermsToExpr(chromaticNumber, colorsUsed);
		model.setObjective(chromaticNumber, GRB.MINIMIZE);
	}
	
	/**
	 * Creates or resets to a new Gurobi model.  
	 * @throws GRBException 
	 */
	private void createGurobiModel() throws GRBException {
		model = new GRBModel(env);
	}
	
	/**
	 * Process the sample text file containing information on the graph and 
	 * its interference and affinity edges. 
	 * @param txtFile Sample text file containing graph information.
	 * @throws FileNotFoundException
	 * @throws GRBException 
	 */
	public void processGraphDataFile(File txtFile) throws FileNotFoundException {
		Scanner in = new Scanner(txtFile);
		
		//get the values for the vertices and edges from file's first line
		qtyVert = Integer.parseInt(in.next());
		qtyMaxColors = qtyVert;
		qtyInterferenceEdge = Integer.parseInt(in.next());
		qtyAffinityEdge = Integer.parseInt(in.next());
		in.nextLine();
		
		populateEdgeMatrix(in);
		in.close();
	}
	
	/**
	 * Populates the edge matrix from a given text file, marking the edges as 
	 * interference or affinity edge based on defined markers. Graph is 
	 * undirected, so edges are added for both 5 to 1 and 1 to 5 the same way.
	 * @param txtScanner The scanner reading the text file.
	 */
	private void populateEdgeMatrix(Scanner txtScanner) {
		edgeMatrix = new int[qtyVert][qtyVert];
		int interferenceCount = 0;
		int affinityCount = 0;
		int currentEdgeType = INTERFERENCE_EDGE_MARKER;
		
		while (txtScanner.hasNextLine()) {
			if (interferenceCount < qtyInterferenceEdge)
				interferenceCount++;
			else if (interferenceCount == qtyInterferenceEdge) {
				currentEdgeType = AFFINITY_EDGE_MARKER;
				affinityCount++;
			}
			
			String edgeText = txtScanner.nextLine();
			addEdge(edgeText,currentEdgeType);
		}
		
		//verify no issues with populating all edges and noting edge types
		if (affinityCount != qtyAffinityEdge) 
			throw new RuntimeException("Affinity edge count mismatch");
	}
	
	/**
	 * Adds an edge to the edge matrix. The matrix is offset by 1. For example,
	 * vertex 1 connected to vertex 2 is set marked at the [0][1] position of 
	 * the matrix. 
	 * @param edgeText The text containing information on the edge to create
	 * @param currentEdgeType The type of edge to designate in the matrix
	 */
	private void addEdge(String edgeText, int currentEdgeType) {
		Scanner edgeScanner = new Scanner(edgeText);
		int vertex1 = Integer.parseInt(edgeScanner.next());
		int vertex2 = Integer.parseInt(edgeScanner.next());
		edgeMatrix[vertex1 - 1][vertex2 - 1] = currentEdgeType;
		edgeMatrix[vertex2 - 1][vertex1 - 1] = currentEdgeType;
		edgeScanner.close();
	}
	
	/**
	 * Prints affinity edge outputs to console for use in debugging purposes. 
	 * @throws GRBException
	 */
	private void printAffinityOutputs() throws GRBException {
		printOutputs();
		
		for (int i = 0; i < qtyVert; i++) {
			System.out.printf("%5s", "V" + (i+1) + ": ");
			for (int j = 0; j < qtyMaxColors; j++) {
				System.out.print((int) colorAssignment[i][j].
						get(GRB.DoubleAttr.X) + " ");
			}
			System.out.println();
		}
		System.out.println("\nSatisfied Affinity Edges: " + 
		satisfiedAffinityEdges.getValue());
	}
	
	/**
	 * Prints outputs to console for use in debugging purposes. 
	 * @throws GRBException
	 */
	private void printOutputs() throws GRBException {
		System.out.println();
		for (int vert = 1; vert <= qtyVert; vert++) 
			System.out.format("%3s ","V" + vert);
		System.out.println();
		for (int vert = 0; vert < qtyVert; vert++) {
			for (int color = 0; color < qtyMaxColors; color++) {
				double assignment = colorAssignment[vert][color].
						get(GRB.DoubleAttr.X);
				if (assignment > 0) { 
					System.out.format("%3s ",color+1);
					color = qtyMaxColors;
				}
			}
		}
		System.out.println();
		
		System.out.println("\nChromatic Number: " + chromaticNumber.getValue());
		System.out.println("Affinity Edges Satisfied: " + 
			satisfiedAffinityEdges.getValue());
	}
}
