/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2022, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.knapsack;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainBest;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.search.strategy.selectors.variables.Largest;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.MathUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Jean-Guillaume FAGES (cosling), Nicolas PIERRE
 * @since 05/04/2017 et 30/03/2022
 */
public class KnapsackTest {

	private void testMutliKpInstance(int M, int N, int[] c, int[] b, int[][] a, int z) {
		Model m = new Model();
		BoolVar[] x = m.boolVarArray(N);
		IntVar[] bVar = new IntVar[M];
		for (int i = 0; i < M; i++) {
			bVar[i] = m.intVar(0, b[i]);
		}
		IntVar objective = m.intVar(0, N * MathUtils.max(c));
		m.setObjective(Model.MAXIMIZE, objective);
		m.scalar(x, c, "=", objective).post();
		for (int i = 0; i < M; i++) {
			m.knapsack2(x, bVar[i], objective, a[i], c).post();
		}
		Solver s = m.getSolver();
		IntVar[] xCost = new IntVar[N];
		for (int i = 0; i < N; i++)
			xCost[i] = m.intScaleView(x[i], c[i]);
		s.setSearch(Search.intVarSearch(new Largest(), new IntDomainMax(), xCost));
		while (s.solve())
			;
		s.printShortStatistics();
		Assert.assertEquals(s.getBestSolutionValue(), z);
	}

	@Test // (groups = "10s", timeOut = 60000)
	public void knapsackTest() {
		KpInstance pb = KpInstance.getInstance1();
		testMutliKpInstance(pb.M, pb.N, pb.c, pb.b, pb.a, 16537);
	}

	@Test
	public void minizincTest() {
		KpInstance pb = KpInstance.getInstance2();
		testMutliKpInstance(pb.M, pb.N, pb.c, pb.b, pb.a, 11191);
	}

	@Test // (groups = "10s", timeOut = 60000)
	public void knapsackTestBestValue() {
		knapsackTestBestValue(true);
		knapsackTestBestValue(false);
	}

	public void knapsackTestBestValue(boolean original) {
		KpInstance pb = KpInstance.getInstance1();
		Model m = new Model();
		BoolVar[] x = m.boolVarArray(pb.N);
		IntVar[] bVar = new IntVar[pb.M];
		for (int i = 0; i < pb.M; i++) {
			bVar[i] = m.intVar(0, pb.b[i]);
		}
		IntVar objective = m.intVar(16000, pb.N * MathUtils.max(pb.c));
		m.setObjective(Model.MAXIMIZE, objective);
		m.scalar(x, pb.c, "=", objective).post();
		for (int i = 0; i < pb.M; i++) {
			if (original) {
				m.knapsack2(x, bVar[i], objective, pb.a[i], pb.c).post();
			} else {
				m.knapsack(x, bVar[i], objective, pb.a[i], pb.c).post();
			}
		}
		Solver s = m.getSolver();
		IntVar[] xCost = new IntVar[pb.N];
		for (int i = 0; i < pb.N; i++)
			xCost[i] = m.intScaleView(x[i], pb.c[i]);
		s.setSearch(Search.intVarSearch(new Largest(), new IntDomainBest(), xCost));
		while (s.solve()) {

		}
		s.printShortStatistics();
		Assert.assertEquals(s.getBestSolutionValue(), 16537);
	}
}
