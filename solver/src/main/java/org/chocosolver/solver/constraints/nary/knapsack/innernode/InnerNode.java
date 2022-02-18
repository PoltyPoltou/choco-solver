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
import org.chocosolver.solver.constraints.nary.knapsack.WeightInterface;

public interface InnerNode extends WeightInterface {

    public void setup();

    public void updateValue(KPItem item);

    public void updateValue(InnerNode item);

    public default void setValue(KPItem item1, KPItem item2) {
        setup();
        updateValue(item1);
        updateValue(item2);
    }

    public default void setValue(InnerNode item1, InnerNode item2) {
        setup();
        updateValue(item1);
        updateValue(item2);
    }

    public boolean isActive();
}
