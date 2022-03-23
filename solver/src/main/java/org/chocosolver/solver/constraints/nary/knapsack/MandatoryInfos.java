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

public class MandatoryInfos {
    public final boolean decision;
    public final int endItem;
    public final double profitAccumulated;
    public final double weightAccumulated;
    public final double remainingWeightEndItem;

    public MandatoryInfos(boolean decision, int endItem, double profitAccumulated, double weightAccumulated,
            double remainingWeightEndItem) {
        this.decision = decision;
        this.endItem = endItem;
        this.profitAccumulated = profitAccumulated;
        this.weightAccumulated = weightAccumulated;
        this.remainingWeightEndItem = remainingWeightEndItem;
    }

}
