/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2022, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.knapsack.innernode;

import org.chocosolver.solver.constraints.nary.knapsack.KPItem;

public class InnerNodeMinWeight implements InnerNode {
    private int minWeight;

    public InnerNodeMinWeight() {
        setup();
    }

    public void setup() {
        this.minWeight = Integer.MAX_VALUE;
    }

    public void updateValue(KPItem item) {
        if (item.isActive()) {
            this.minWeight = Math.min(item.getWeight(), minWeight);
        }
    }

    public int getWeight() {
        return minWeight;
    }

    public void updateValue(InnerNode node) {
        try {
            InnerNodeMinWeight nodeMaxWeight = (InnerNodeMinWeight) node;
            this.minWeight = Math.min(nodeMaxWeight.getWeight(), minWeight);
        } catch (Exception e) {
            throw new RuntimeException("updateValue of InnerNode used with another type ");
        }
    }

    public boolean isActive() {
        return minWeight != Integer.MAX_VALUE;
    }

    public String toString() {
        return "w=" + minWeight;
    }

}
