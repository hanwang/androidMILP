package com.androidMILP.milp;

import java.util.Collection;
import java.util.LinkedList;

import com.joptimizer.functions.LinearMultivariateRealFunction;

public class IntegerProblemNode {

	public final IntegerProblemNode parent;

	public final LinkedList<LinearMultivariateRealFunction> inEqCon = new LinkedList<LinearMultivariateRealFunction>();

	protected double[] solution = null;
	protected boolean exactSol = false;
	protected double solVal = Double.NaN;

	protected IntegerProblemNode leftChd = null;
	protected IntegerProblemNode rightChd = null;

	public final double lowerBnd;

	public IntegerProblemNode(IntegerProblemNode parent,
			Collection<LinearMultivariateRealFunction> inEqCon) {
		this.parent = parent;
		if (inEqCon != null) {
			this.inEqCon.addAll(inEqCon);
		}

		if (this.parent == null) {
			this.lowerBnd = Double.NEGATIVE_INFINITY;
		} else {
			this.lowerBnd = parent.solVal;
		}

	}

	public IntegerProblemNode(IntegerProblemNode parent, LinearMultivariateRealFunction inEqCon,
			LinearMultivariateRealFunction eqCon) {
		this.parent = parent;
		if (inEqCon != null) {
			this.inEqCon.add(inEqCon);
		}

		if (this.parent == null) {
			this.lowerBnd = Double.NEGATIVE_INFINITY;
		} else {
			this.lowerBnd = parent.solVal;
		}
	}

	public void getIneqConstraints(
			LinkedList<LinearMultivariateRealFunction> container) {

		if (parent != null) {
			parent.getIneqConstraints(container);
		}
		container.addAll(inEqCon);
	}
}
