package com.androidMILP.milp;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationResponse;
import com.joptimizer.optimizers.PrimalDualMethod;

public class MixedIntegerLinearProblem implements Runnable {

	/*
	 * final TreeSet<IntegerProblemNode> stack = new
	 * TreeSet<IntegerProblemNode>( new IntegerProblemNodeComparator());
	 */
	final LinkedList<IntegerProblemNode> stack = new LinkedList<IntegerProblemNode>();

	private final LinkedList<LinearMultivariateRealFunction> currIneq = new LinkedList<LinearMultivariateRealFunction>();

	public final LinearMultivariateRealFunction objectiveFcn;

	public final int numVars;
	public final int[] intVarIndx;

	public final double tol;

	protected double[] bestSolution;
	protected double bestValue = Double.POSITIVE_INFINITY;

	/***
	 * argmin(x): cTx
	 * 
	 * st.
	 * 
	 * Gx <= h Ax = b
	 * 
	 * @param tol
	 * @param intVarIndx
	 * @param binaryVarIndx
	 * @param objectiveFcn
	 * @param G
	 * @param h
	 * @param A
	 * @param b
	 */
	public MixedIntegerLinearProblem(double tol, int[] intVarIndx, double[] c, double[][] G, double[] h) {

		if (G.length != h.length) {
			throw new IllegalArgumentException("G and h must have same length");
		}

		this.numVars = c.length;
		this.tol = tol;

		this.objectiveFcn = new LinearMultivariateRealFunction(c, 0);

		// Create functions for the ineqCons
		LinkedList<LinearMultivariateRealFunction> ineqCon = new LinkedList<LinearMultivariateRealFunction>();
		for (int i = 0; i < G.length; i++) {
			final LinearMultivariateRealFunction fcn = new LinearMultivariateRealFunction(
					G[i], -h[i]);
			ineqCon.add(fcn);
		}

		// Combine and transfer
		final LinkedHashSet<Integer> intVars = new LinkedHashSet<Integer>();
		for (Integer i : intVarIndx) {
			intVars.add(i);
		}

		this.intVarIndx = new int[intVars.size()];
		int i = 0;
		for (int indx : intVars) {
			this.intVarIndx[i] = indx;
			i++;
		}

		IntegerProblemNode root = new IntegerProblemNode(null, ineqCon);

		stack.add(root);
	}

	public int getNumVars() {
		return this.numVars;
	}

	public double[] getSolution() {
		return this.bestSolution;
	}

	public double getSolutionValue() {
		return this.bestValue;
	}

	protected IntegerProblemNode[] branch(IntegerProblemNode parent,
			int varNum, double value) {
		final double[] leftVector = new double[getNumVars()];
		leftVector[varNum] = 1;
		final LinearMultivariateRealFunction leftBound = new LinearMultivariateRealFunction(
				leftVector, -Math.floor(value));
		IntegerProblemNode leftNode = new IntegerProblemNode(parent, leftBound,
				null);

		final double[] rightVector = new double[getNumVars()];
		rightVector[varNum] = -1;
		final LinearMultivariateRealFunction rightBound = new LinearMultivariateRealFunction(
				rightVector, Math.ceil(value));
		IntegerProblemNode rightNode = new IntegerProblemNode(parent,
				rightBound, null);

		parent.leftChd = leftNode;
		parent.rightChd = rightNode;

		return new IntegerProblemNode[] { leftNode, rightNode };
	}

	public void solve() {
		while (!stack.isEmpty()) {
			IntegerProblemNode top = stack.pollFirst();

			if (top.lowerBnd > this.bestValue) {
				prune(top);
			} else {
				solveNode(top);
			}
		}

		// Clear stack to free up memory
		stack.clear();
	}

	public void solveNode(IntegerProblemNode node) {

		// Using cache, to retrieve the full MILP specification
		currIneq.clear();
		node.getIneqConstraints(currIneq);
		final LinearMultivariateRealFunction[] inEq = new LinearMultivariateRealFunction[currIneq
				.size()];
		currIneq.toArray(inEq);

		// Create the optimation request
		final OptimizationRequest or = new OptimizationRequest();
		or.setF0(this.objectiveFcn);
		or.setFi(inEq); //
		
		// Set stopping tolerances
		or.setToleranceFeas(tol / 32);
		or.setTolerance(tol / 32);

		// Create the optimizer and set the request
		final PrimalDualMethod opt = new PrimalDualMethod();
		opt.setOptimizationRequest(or);

		try {
			int code = opt.optimize();
			if (code == OptimizationResponse.FAILED) {
				// This is an unfeasible node return
				return;
			}
		} catch (Exception e) {
			if (!e.getMessage().equals("Infeasible problem")) {
				throw new IllegalStateException(e);
			}
			prune(node);
			return;
		}

		double[] solution = opt.getOptimizationResponse().getSolution();
		double solVal = this.objectiveFcn.value(solution);

		node.solution = solution;
		node.solVal = solVal;

		final HashSet<Integer> nonIntIndx = new HashSet<Integer>();
		isExactSol(node, nonIntIndx);

		if (node.solVal < bestValue) {
			if (node.exactSol) {
				// Update values if this is best solution
				bestSolution = node.solution;
				bestValue = node.solVal;

				// Since this is an exact solution, we can prune and remove from
				// memory
				prune(node);
			} else {
				// Since the iterator has no guarenteed iteration order this is
				// equivalent to random branching
				int i = nonIntIndx.iterator().next();

				IntegerProblemNode[] branches = branch(node, i,
						node.solution[i]);

				// Add left
				stack.add(branches[0]);

				// Add right
				stack.add(branches[1]);
			}
		}
	}

	public void isExactSol(IntegerProblemNode node, HashSet<Integer> indxes) {
		boolean isExact = true;

		for (int indx : this.intVarIndx) {
			double val = node.solution[indx];
			if (Math.abs(Math.round(val) - val) > this.tol) {
				isExact = false;
				indxes.add(indx);
			}
		}

		node.exactSol = isExact;
	}

	private void prune(IntegerProblemNode node) {
		IntegerProblemNode pruneNode = node;
		while (pruneNode != null) {
			if (pruneNode.parent != null && pruneNode.leftChd == null
					&& pruneNode.rightChd == null) {
				if (pruneNode.parent.leftChd == pruneNode) {
					pruneNode.parent.leftChd = null;
				} else if (node.parent.rightChd == pruneNode) {
					pruneNode.parent.rightChd = null;
				}

				pruneNode = pruneNode.parent;
			} else {
				// If there are still child nodes to be solved
				break;
			}
		}

	}

	@Override
	public void run() {
		this.solve();
	}
}
