package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Locale;

import com.kamjae.coiote.Problem.Coherence;
import com.kamjae.coiote.Problem.Solution;
import com.kamjae.coiote.Problem.Feasibility;

public class Main {

	public static void main(String[] args) {
		// Needed to have dots instead of commas in decimal numberss
		Locale.setDefault(new Locale("en", "US"));

		Problem p;
		String input = null;
		String output = null;
		String solution = null;
		String optimal = null;
		boolean test = false;
		
		// Analyze input arguments
		for (int i = 0  ; i < args.length ; i++) {
			if (args[i].equals("-i")) {
				input = args[++i];
			}
			if (args[i].equals("-o")) {
				output = args[++i];
			}
			else if (args[i].equals("-s")) {
				solution = args[++i];
			}
			else if (args[i].equals("-os")) {
				optimal = args[++i];
			}
			else if (args[i].equals("-test")) {
				test = true;
			}
		}
		
		if (input == null) {
			printHelp();
			return;
		} else {
			p = new Problem(input);
		}

		String[] sourcePath = input.split("/");
		String fileName = sourcePath[sourcePath.length - 1];
		String instanceName = fileName.split("\\.")[0];
		
		Solution sol = p.solveProblem();
		Coherence inc = sol.isCoherent();
		
		if (inc != Coherence.COHERENT) {
			System.err.println("Warning " + fileName + ": incoherent solution. " + inc);
		}

		if(!test){
			if(output != null){
				// Only print summary
				try(FileWriter fw = new FileWriter(output, true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter out = new PrintWriter(bw)){
					// instance;Cost;Exec_time;Type0_Assigned;Type1_Assigned;Type2_Assigned
					String formattedLine = String.format("%s;%d;%.3f;%d;%d;%d", 
														fileName,
														sol.getTotalCost(),
														sol.getElapsedMillis() / 1000f,
														sol.getCountOfType(0),
														sol.getCountOfType(1),
														sol.getCountOfType(2));
					out.println(formattedLine);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} 
			if(solution != null) {
				// Print complete solution
				try(FileWriter fw = new FileWriter(solution, true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter out = new PrintWriter(bw)){
					
					String formattedLine = sol.toVerbose();
					out.print(formattedLine);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			if (optimal != null) {
				try (BufferedReader in = new BufferedReader(new FileReader(optimal))) {				
					DecimalFormat formatter = new DecimalFormat("0.000");
					String line;		
					boolean found = false;
					while ((line = in.readLine()) != null) {
						// Remove any separator
						String[] vals = line.split(String.format("[\";\t]"));
						
						// Filter empty strings. THANKS JAVA.
						String[] filteredVals = new String[vals.length];
						int i = 0;
						
						for (String val : vals) {
							if (!val.isEmpty())
								filteredVals[i++] = val;
						}

						if (i == 0)
							continue;
						if (instanceName.equals(filteredVals[0])) {
							found = true;
							float opCost = Float.parseFloat(filteredVals[2]);
							float opGap = (sol.getTotalCost() - opCost) / opCost * 100;
							System.out.println(fileName + ": " + formatter.format(opGap) + " percent");

							break;
						}
					}
					if(!found){
						// Edit in second upload: missing final parenthesis prevented successful compiling
						System.out.println(fileName + " Cost: " + sol.getTotalCost());
						// -------------------------------------------------------------------------------
					}
					
					in.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					System.exit(1);
				}
			}		
		} else {
			Feasibility feas = p.checkFeasibility(sol);
			switch(feas){
				case FEASIBLE:
					System.out.println(instanceName + ": Solution is feasible");
					break;
				case UNF_DEMAND:
					System.out.println(instanceName + ": Solution is not feasible: demand not satisfied");
					break;
				case UNF_CUSTOMERS:
					System.out.println(instanceName + ": Solution is not feasible: exceeded number of available users");
					break;
			}
		}
	}
	
	public static void printHelp() {
		System.out.println("Usage: java -jar coiote.jar -i inputFile [-o outputSummaryFile] [-s outputSolutionFile] [-test] [-os optimalSolution]");
	}
}
