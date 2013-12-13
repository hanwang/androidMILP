package com.androidMILP.test;

import org.junit.Before;

import com.androidMILP.milp.MixedIntegerLinearProblem;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

import junit.framework.TestCase;

public class MILPTest extends TestCase {
	double tol = 1e-7;

	public void test4() {
		double tol = 1e-7;
		
		double[][] A = new double[][] { { 2.0, 1 }, { 3., 4. }};
		double[] b = new double[] { 4., 12. };

		double[] c = new double[] { -1., -1. };
		
		int[] intVarIndx = new int[] { 0, 1 };

		MixedIntegerLinearProblem milp = new MixedIntegerLinearProblem(tol,
				intVarIndx, c, A, b);

		milp.run();

		double[] sol = milp.getSolution();

		System.out.println(sol[0] + "|" + sol[1]);

	}
	
	public void test1() {
		double[][] G = new double[][] { { 4. / 3., -1 }, { -1. / 2., 1. },
				{ -2., -1. }, { 1. / 3., 1. } };

		double[] h = new double[] { 2., 1. / 2., 2., 1. / 2. };

		double[] c = new double[] { -1., -1. };

		MixedIntegerLinearProblem milp = new MixedIntegerLinearProblem(tol,
				new int[] { 0, 1 }, c, G, h);

		milp.run();

		double[] sol = milp.getSolution();

		assertEquals(1.0, sol[0], tol);
		assertEquals(0.0, sol[1], tol);

	}

	public void test3() {
		double[] c = new double[] { 0., -1. };

		double[][] G = new double[][] { { -1.0, 1.0 }, { 3.0, 2.0 },
				{ 2.0, 3.0 }, { -1.0, 0.0 }, { 0.0, -1.0 } };

		double[] h = new double[] { 1.0, 12.0, 12.0, 0.0, 0.0 };

		MixedIntegerLinearProblem milp = new MixedIntegerLinearProblem(tol,
				new int[] { 0, 1 }, c, G, h);

		milp.run();

		double[] sol = milp.getSolution();

		assertEquals(1.0, sol[0], tol);
		assertEquals(2.0, sol[1], tol);
	}

	public void test2() throws Exception {
		// Objective function (plane)
		LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
				new double[] { -1., -1. }, 4);

		// inequalities (polyhedral feasible set G.X<H )
		ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
		double[][] G = new double[][] { { 4. / 3., -1 }, { -1. / 2., 1. },
				{ -2., -1. }, { 1. / 3., 1. } };
		double[] H = new double[] { 2., 1. / 2., 2., 1. / 2. };
		inequalities[0] = new LinearMultivariateRealFunction(G[0], -H[0]);
		inequalities[1] = new LinearMultivariateRealFunction(G[1], -H[1]);
		inequalities[2] = new LinearMultivariateRealFunction(G[2], -H[2]);
		inequalities[3] = new LinearMultivariateRealFunction(G[3], -H[3]);

		// optimization problem
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setFi(inequalities);
		// or.setInitialPoint(new double[] {0.0, 0.0});//initial feasible point,
		// not mandatory
		or.setToleranceFeas(1.E-9);
		or.setTolerance(1.E-9);

		// optimization
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);
		int returnCode = opt.optimize();

		double[] sol = opt.getOptimizationResponse().getSolution();

		System.out.println(sol[0] + "|" + sol[1]);

	}

}
