# CS220 Synthesis of Digital Systems

The files uploaded are some of the deliverables I had created for the two projects that I have completed for CS220 Synthesis of Digital Systems at UC Riverside.

The assignment details are found in each of the project's directories in a PDF file. 

## Assignment 2: Integer Linear Programming (ILP) using Gurobi Optimization
In this assignment, we create a program that is able to solve the Graph Coloring Problem using ILP. For my implementation, I used the Gurobi Optimization libraries, which can be found here: https://www.gurobi.com/. 

## Assignment 3: NSGA-II (Non-dominated Sorting Genetic Algorithm II)
In this assignment, we go back to the Graph Coloring Program and we now solve a multi-objective optimization problem of minimizing the number of colors used to color the graph and maximimizing the number of satisfied affinity edges. For this implementation, I used MOEA Framework, an open-source Java library that is able to perform multiple different algorithms, including NSGA-II (http://moeaframework.org/). 

## Assignment 4: SAT Solver
In this assignment, we solve a Boolean Decision (Yes,No) problem that is a variant of the Graph Coloring Problem where the algorithm tries to solve the constraints:
- The color assignment uses at most K1 colors
- The color assignment satisfies at least K2 affinity edges
  
For my implementation, I used z3 in Python. The original files, which are uploaded to this Github, can be found in this Google Drive folder:
https://drive.google.com/drive/folders/1WZq3BvtoivE2iWYym8T6p0UVy-EZrpCj?usp=sharing
