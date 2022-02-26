/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2022, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.structure.IOperation;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.sort.ArraySort;
import org.chocosolver.util.tools.ArrayUtils;

import org.chocosolver.solver.constraints.nary.knapsack.*;

/**
 * Propagator for the Knapsack constraint
 * based on Dantzig-Wolfe relaxation
 *
 * @author Jean-Guillaume Fages
 */
public class PropKnapsack2 extends Propagator<IntVar> {
    static final int ADDED = 1;
    static final int REMOVED = -1;
    static final int NOT_DEFINED = 0;
    // ***********************************************************************************
    // VARIABLES
    // ***********************************************************************************

    private final int[] order;
    private final int[] weight;
    private final int[] energy;
    private final double[] ratio;
    private final int n;
    private int deltaCapacity;
    private final IntVar capacity;
    private int deltaLB;
    private final IntVar power;
    private final int[] itemState;
    private final ItemFindingSearchTree findingTree;
    private final ComputingLossWeightTree computingTree;
    private final ArrayList<KPItem> orderedItems;
    private Info criticalItemInfosLower;
    private Info criticalItemInfosUpper;

    // ***********************************************************************************
    // CONSTRUCTORS
    // ***********************************************************************************

    public PropKnapsack2(IntVar[] itemOccurence, IntVar capacity, IntVar power,
            int[] weight, int[] energy) {
        super(ArrayUtils.append(itemOccurence, new IntVar[] { capacity, power }), PropagatorPriority.LINEAR, false);
        this.n = itemOccurence.length;
        this.itemState = new int[n];
        this.deltaCapacity = 0;
        this.deltaLB = 0;
        this.capacity = capacity;
        this.weight = weight;
        this.energy = energy;
        this.power = vars[n];
        Arrays.fill(this.itemState, 0);
        // ratio energy/weight of every item (not ordred)
        this.ratio = new double[n];
        for (int i = 0; i < n; i++) {
            ratio[i] = weight[i] == 0 ? Double.MAX_VALUE : ((double) (energy[i]) / (double) (weight[i]));
        }
        // we find the decreasing order of efficiency
        this.order = ArrayUtils.array(0, n - 1);
        ArraySort sorter = new ArraySort(n, false, true);
        sorter.sort(order, n, (i1, i2) -> Double.compare(ratio[i2], ratio[i1]));
        this.orderedItems = new ArrayList<>();
        orderedItems.ensureCapacity(n);
        for (int i = 0; i < n; ++i) {
            orderedItems.set(i, new KPItem(energy[order[i]], weight[order[i]]));
        }
        this.findingTree = new ItemFindingSearchTree(orderedItems);
        this.computingTree = new ComputingLossWeightTree(orderedItems);

    }

    // ***********************************************************************************
    // METHODS
    // ***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vIdx < n + 1) {
            // updates on the weight or on an item
            return IntEventType.boundAndInst();
        } else {
            // updates on the LB variable, we do nothing
            return IntEventType.VOID.getMask();
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        /*
         * TODO remove this comment
         * This method is called once at initialization
         * We will not call it afterwards
         */
        this.criticalItemInfosLower = this.computingTree.findCriticalItem(this.capacity.getLB());
        this.criticalItemInfosUpper = this.computingTree.findCriticalItem(this.capacity.getUB());
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        /*
         * TODO remove this comment
         * This method is called when varIdx changes according to mask
         */
        if (varIdx < n) {
            // item changed
            // we update the trees
            if (this.vars[varIdx].getValue() == 0) {
                this.removeItemFromProblem(varIdx);
            } else if (this.vars[varIdx].getValue() == 1) {
                this.addItemToSolution(varIdx);
            }
        } else if (varIdx == n && mask == IntEventType.DECUPP.getMask()) {
            this.criticalItemInfosUpper = this.computingTree.findCriticalItem(this.capacity.getUB());
        } else if (varIdx == n && mask == IntEventType.INCLOW.getMask()) {
            this.criticalItemInfosLower = this.computingTree.findCriticalItem(this.capacity.getLB());
        }
    }

    @Override
    public ESat isEntailed() {
        double camax = capacity.getUB();
        double pomin = 0;
        for (int i = 0; i < n; i++) {
            camax -= (long) weight[i] * vars[i].getLB(); // potential overflow
            pomin += (long) energy[i] * vars[i].getLB(); // potential overflow
        }
        if (camax < 0 || pomin > power.getUB()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            if (pomin == power.getValue()) {
                return ESat.TRUE;
            }
        }
        return ESat.UNDEFINED;
    }

    /**
     * changes the constraint such that the item is in the solution
     * Informs backtrack environment what to do
     * 
     * @param i index of an item in the list given in the constructor
     */
    private void addItemToSolution(int i) {
        KPItem item = orderedItems.get(order[i]);
        int sortedIndex = computingTree.leafToGlobalIndex(i);
        if (this.itemState[i] == NOT_DEFINED) {
            this.itemState[i] = ADDED;
            int globalLeafIndex = computingTree.leafToGlobalIndex(sortedIndex);
            computingTree.removeLeaf(globalLeafIndex);
            findingTree.removeLeaf(globalLeafIndex);
            deltaCapacity -= item.getWeight();
            deltaLB -= item.getProfit();
            getEnvironment(i).save(activateItemBacktrackOperation(i));
        }
    }

    /**
     * changes the constraint such that the item is NOT in the solution
     * Informs backtrack environment what to do
     * 
     * @param i index of an item in the list given in the constructor
     */
    private void removeItemFromProblem(int i) {
        int sortedIndex = computingTree.leafToGlobalIndex(this.order[i]);
        if (this.itemState[i] == NOT_DEFINED) {
            this.itemState[i] = REMOVED;
            int globalLeafIndex = computingTree.leafToGlobalIndex(sortedIndex);
            computingTree.removeLeaf(globalLeafIndex);
            findingTree.removeLeaf(globalLeafIndex);
            getEnvironment(i).save(activateItemBacktrackOperation(i));
        }
    }

    /**
     * changes the constraint such that the item is not determined anymore
     *
     * @param i index of an item in the list given in the constructor
     */
    private void activateItemToProblem(int i) {
        int sortedIndex = computingTree.leafToGlobalIndex(this.order[i]);
        if (this.itemState[i] != NOT_DEFINED) {
            KPItem item = computingTree.getLeaf(sortedIndex);
            int globalLeafIndex = computingTree.leafToGlobalIndex(sortedIndex);
            computingTree.activateLeaf(globalLeafIndex);
            findingTree.activateLeaf(globalLeafIndex);
            if (this.itemState[i] == ADDED) {
                deltaCapacity += item.getWeight();
                deltaLB += item.getProfit();
            }
            this.itemState[i] = NOT_DEFINED;
        }
    }

    private IOperation activateItemBacktrackOperation(int i) {
        return () -> activateItemToProblem(i);
    }

    private List<Integer> findMandatoryItems() {
        List<Integer> mandatoryList = new LinkedList<>();
        int index = computingTree.leafToGlobalIndex(0);
        int maxWeight = 0;
        do {
            if (computingTree.isMandatory(
                    criticalItemInfosUpper.weight + deltaCapacity,
                    criticalItemInfosUpper.profit,
                    criticalItemInfosUpper.index,
                    power.getUB() + deltaLB,
                    index)) {
                mandatoryList.add(index);
            } else {
                maxWeight = Math.max(maxWeight, findingTree.getNodeWeight(index));
            }
            index = findingTree.findNextRightItem(index, criticalItemInfosUpper.index, maxWeight);
        } while (index != -1);
        return mandatoryList;
    }

    private List<Integer> findForbiddenItems() {
        List<Integer> forbiddenList = new LinkedList<>();
        int index = findingTree.getNumberNodes() - 1;
        int minWeight = Integer.MAX_VALUE;
        do {
            if (computingTree.isForbidden(
                    criticalItemInfosLower.weight + deltaCapacity,
                    criticalItemInfosLower.profit,
                    criticalItemInfosLower.index,
                    power.getLB() + deltaLB,
                    index)) {
                forbiddenList.add(index);
            } else {
                minWeight = Math.min(minWeight, findingTree.getNodeWeight(index));
            }
            index = findingTree.findNextLeftItem(index, criticalItemInfosLower.index, minWeight);
        } while (index != -1);
        return forbiddenList;
    }

    private IEnvironment getEnvironment(int varIdx) {
        return this.vars[0].getEnvironment();
    }
}
