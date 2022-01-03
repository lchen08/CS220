import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


/**
 * <b><u>CS220 Assignment #3 - NGSAII</b></u>
 * <br>
 * This class reads an input file noting the details of the graphs.The input 
 * file format is:
 * First line: three positive integers: the number of vertices, the number of 
 * interference edges, and the number of affinity edges
 * Second set of lines: the interference edges (format: vert1 vert2)
 * Third set of lines: the interference edges (format: vert1 vert2)
 * 
 * Bulk of this code was copied from Assignment #2 with minor changes to adjust 
 * to the new algorithm.
 * 
 * @author Lisa Chen
 * @since Nov 24, 2019
 * @version 1.0
 */
public class GraphFileReader {

	private int qtyVert;
	private int qtyInterferenceEdge;
	private int qtyAffinityEdge;
	private int[][] edgeMatrix;
	private final static File FILE_1 = new File("sample_1.txt");
	private final static File FILE_2 = new File("sample_2.txt");
	private final static File FILE_3 = new File("sample_3.txt");
	
	//to denote which edge is which in the edge matrix
	private final int INTERFERENCE_EDGE_MARKER = 1;
	private final int AFFINITY_EDGE_MARKER = 2;
	
	/**
	 * Initializes the graph file reader with a given number associated with 
	 * the file to read.
	 * @param fileNum The file number associated with the file to read
	 * @throws FileNotFoundException
	 */
	public GraphFileReader(int fileNum) throws FileNotFoundException {
		File selectedFile;
		switch (fileNum) {
			case 1: selectedFile = FILE_1; break;
			case 2: selectedFile = FILE_2; break;
			case 3: selectedFile = FILE_3; break;
			default: selectedFile = null;
		}
		processGraphDataFile(selectedFile);
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
	 * Gets the number of vertices in the graph as noted by the graph file.
	 * @return Number of vertices in the graph.
	 */
	public int getNumVertices() { return qtyVert; }
	
	/**
	 * Gets the edge matrix created from the information from the graph file. 
	 * The edge matrix contains information for affinity and interference edges.
	 * @return The edge matrix
	 */
	public int[][] getEdgeMatrix() { return edgeMatrix; }
	
	/**
	 * Gets the maximum number of colors possible to color the graph per the 
	 * graph file information. This assumes each vertex gets its own color.
	 * @return Max number of colors possible to color the graph
	 */
	public int getMaxNumColors() { return qtyVert; }
	
	/**
	 * Gets the number of interference edges in the graph.
	 * @return The number of interference edges in the graph.
	 */
	public int getNumInterferenceEdges() { return qtyInterferenceEdge; }
}
