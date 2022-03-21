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

import org.chocosolver.util.sort.IntComparator;

public class ItemComparator implements IntComparator {

    private final int[] weights;
    private final int[] profits;

    public ItemComparator(int[] weights, int[] profits) {
        this.weights = weights;
        this.profits = profits;
    }

    @Override
    public int compare(int i1, int i2) {
        // Compares efficiencies decreasingly
        int comparaison = profits[i2] * weights[i1] - profits[i1] * weights[i2];
        if (comparaison == 0) {
            if (weights[i1] * weights[i2] == 0) {
                return profits[i2] - profits[i1];
            } else {
                // breaking ties in favor of larger weights
                return weights[i2] - weights[i1];
            }
        } else {
            return comparaison;
        }
    }

}
