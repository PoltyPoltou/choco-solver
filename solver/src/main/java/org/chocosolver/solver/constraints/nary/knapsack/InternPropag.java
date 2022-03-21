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

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

public class InternPropag extends Propagator<IntVar> {
    private IStateInt[] values;
    private IStateInt totalValue;
    private int[] coefficients;

    public InternPropag(IntVar[] itemOccurence, int[] coefficients) {
        super(itemOccurence, PropagatorPriority.LINEAR, true);
        this.values = new IStateInt[this.vars.length];
        this.coefficients = coefficients;
        for (int i = 0; i < this.vars.length; ++i) {
            values[i] = this.vars[i].getEnvironment().makeInt(this.vars[i].getLB() * this.coefficients[i]);
        }
        totalValue = this.vars[0].getEnvironment().makeInt(0);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < this.vars.length; ++i) {
            values[i].set(this.vars[i].getLB() * this.coefficients[i]);
            totalValue.add(values[i].get());
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.INCLOW.getMask();
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        int oldVal = values[varIdx].get();
        values[varIdx].set(this.vars[varIdx].getLB() * this.coefficients[varIdx]);
        totalValue.add(values[varIdx].get() - oldVal);
    }

    @Override
    public ESat isEntailed() {
        return ESat.UNDEFINED;
    }

    public int getValue() {
        return totalValue.get();
    }

}
