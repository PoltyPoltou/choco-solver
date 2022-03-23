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

import java.util.Arrays;
import java.util.List;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.MathUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class KPPropagTest {

    private static List<KPItem> genItems() {
        return Arrays.asList(new KPItem[] {
                new KPItem(3, 0),
                new KPItem(10, 3),
                new KPItem(10, 5),
                new KPItem(8, 4),
                new KPItem(10, 0),
                new KPItem(16, 10),
                new KPItem(19, 13),
                new KPItem(0, 150),
                new KPItem(16, 12),
                new KPItem(0, 3),
                new KPItem(2, 2),
                new KPItem(32, 0),
                new KPItem(3, 9),
                new KPItem(5, 18),
        });
    }

    @Test
    public void KpTest2() {
        int wLB = 10;
        int wUB = 20;
        int pLB = 25;
        int pUB = 35;
        Model m = new Model();
        int n = genItems().size();
        BoolVar[] x = m.boolVarArray(n);
        int[] w = new int[n];
        int[] p = new int[n];
        List<KPItem> lst = genItems();
        for (int i = 0; i < n; ++i) {
            w[i] = lst.get(i).getWeight();
            p[i] = lst.get(i).getProfit();
        }
        IntVar profit = m.intVar(pLB, pUB);
        IntVar weight = m.intVar(wLB, wUB);
        m.knapsackOld(x, weight, profit, w, p).post();
        Solver sol = m.getSolver();
        sol.setSearch(Search.inputOrderLBSearch(x), Search.defaultSearch(m));
        while (sol.solve())
            ;
        long nbSol = sol.getSolutionCount();

        m = new Model();
        x = m.boolVarArray(n);
        w = new int[n];
        p = new int[n];
        lst = genItems();
        for (int i = 0; i < n; ++i) {
            w[i] = lst.get(i).getWeight();
            p[i] = lst.get(i).getProfit();
        }
        profit = m.intVar(pLB, pUB);
        weight = m.intVar(wLB, wUB);
        m.knapsack(x, weight, profit, w, p).post();
        sol = m.getSolver();
        sol.setSearch(Search.inputOrderLBSearch(x), Search.defaultSearch(m));
        while (sol.solve())
            ;
        Assert.assertEquals(sol.getSolutionCount(), nbSol);
    }

}
