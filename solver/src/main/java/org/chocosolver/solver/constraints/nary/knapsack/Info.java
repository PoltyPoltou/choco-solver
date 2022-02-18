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

public class Info {
    public int index;
    public double profit;
    public double weight;

    public Info(int index, double profit, double weight) {
        this.index = index;
        this.profit = profit;
        this.weight = weight;
    }
}
