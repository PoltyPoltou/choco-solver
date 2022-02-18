/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2022, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.knapsack.innernode.factory;

import org.chocosolver.solver.constraints.nary.knapsack.innernode.InnerNode;
import org.chocosolver.solver.constraints.nary.knapsack.innernode.InnerNodeSum;

public class InnerNodeSumFactory extends InnerNodeFactory {

    public InnerNode createInnerNode() {
        return new InnerNodeSum();
    }

}
