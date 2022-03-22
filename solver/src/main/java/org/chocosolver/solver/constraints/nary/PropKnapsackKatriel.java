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
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.sort.ArraySort;
import org.chocosolver.util.tools.ArrayUtils;

import org.chocosolver.solver.constraints.nary.knapsack.*;

/**
 * Propagator for the Knapsack constraint
 * based on Dantzig-Wolfe relaxation trying
 * to find forbidden and mandatory items
 *
 * @author Nicolas PIERRE
 */
public class PropKnapsackKatriel extends Propagator<IntVar> {
    static final int ADDED = 1;
    static final int REMOVED = -1;
    static final int NOT_DEFINED = 0;
    // ***********************************************************************************
    // VARIABLES
    // ***********************************************************************************

    private final int[] order;
    private final int[] reverseOrder;
    private final int n;
    private final IntVar capacity;
    private final IntVar power;
    private final int[] itemState;
    private final ItemFindingSearchTree findingTree;
    private final ComputingLossWeightTree computingTree;
    private final ArrayList<KPItem> orderedItems;
    private Info criticalItemInfos;
    // variables to keep track of the change from the original constraint expression
    private int usedCapacity;
    private int powerCreated;

    // ***********************************************************************************
    // CONSTRUCTORS
    // ***********************************************************************************

    public PropKnapsackKatriel(IntVar[] itemOccurence, IntVar capacity, IntVar power,
            int[] weight, int[] energy) {
        super(ArrayUtils.append(itemOccurence, new IntVar[] { capacity, power }), PropagatorPriority.QUADRATIC, true);
        this.n = itemOccurence.length;
        this.itemState = new int[n];
        this.reverseOrder = new int[n];
        this.capacity = capacity;
        this.power = power;
        this.usedCapacity = 0;
        this.powerCreated = 0;
        Arrays.fill(this.itemState, 0);
        // we find the decreasing order of efficiency
        this.order = ArrayUtils.array(0, n - 1);
        ArraySort sorter = new ArraySort(n, false, true);
        sorter.sort(order, n, new ItemComparator(weight, energy));
        this.orderedItems = new ArrayList<>();
        orderedItems.ensureCapacity(n);
        for (int i = 0; i < n; ++i) {
            orderedItems.add(new KPItem(energy[order[i]], weight[order[i]]));
            reverseOrder[order[i]] = i;
        }
        this.findingTree = new ItemFindingSearchTree(orderedItems);
        this.computingTree = new ComputingLossWeightTree(orderedItems);
    }

    // ***********************************************************************************
    // METHODS
    // ***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vIdx < n) {
            // updates on items
            return IntEventType.boundAndInst();
        } else if (vIdx == n) {
            // updates on the max weight
            return IntEventType.DECUPP.getMask();
        } else /* vIdx == n + 1 */ {
            // updates on the energy variable
            return IntEventType.INCLOW.getMask();
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        propagateOnItems(PropagatorEventType.isFullPropagation(evtmask));
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        if (varIdx < n) {
            // item changed
            // we update the trees
            if (this.vars[varIdx].isInstantiatedTo(0)) {
                this.removeItemFromProblem(varIdx, false);
            } else if (this.vars[varIdx].isInstantiatedTo(1)) {
                this.addItemToSolution(varIdx, false);
            }
        }
        forcePropagate(PropagatorEventType.FULL_PROPAGATION);
    }

    @Override
    public ESat isEntailed() {
        if (powerCreated >= power.getUB() && usedCapacity <= capacity.getLB()) {
            return ESat.TRUE;
        } else if (powerCreated < power.getLB() || usedCapacity > capacity.getUB()) {
            return ESat.FALSE;
        } else {
            return ESat.UNDEFINED;
        }
    }

    /**
     * propagation on items given the power LB and the capacity UB
     * 
     * @param computeCriticalWeight
     */
    private void propagateOnItems(boolean computeCriticalWeight) throws ContradictionException {
        // findWeirdItems();
        if (computeCriticalWeight) {
            this.criticalItemInfos = this.computingTree.findCriticalItem(this.capacity.getUB() - usedCapacity);
        }
        // if power LB is not reachable so we can't filter (every item would be
        // mandatory)
        if (criticalItemInfos.profit + powerCreated >= power.getLB()) {
            List<Integer> mandatoryList = findMandatoryItems();
            List<Integer> forbiddenList = findForbiddenItems();
            for (int unorderedLeafIdx : mandatoryList) {
                addItemToSolution(unorderedLeafIdx, true);
            }
            for (int unorderedLeafIdx : forbiddenList) {
                removeItemFromProblem(unorderedLeafIdx, true);
            }
        }
    }

    /**
     * changes the constraint such that the item is in the solution
     * Informs backtrack environment what to do
     *
     * @param i index of an item in the list given in the constructor
     */
    private void addItemToSolution(int i, boolean removeVarValue) throws ContradictionException {
        if (this.itemState[i] == NOT_DEFINED) {
            int sortedGlobalIndex = computingTree.leafToGlobalIndex(this.reverseOrder[i]);
            this.itemState[i] = ADDED;
            computingTree.removeLeaf(sortedGlobalIndex);
            findingTree.removeLeaf(sortedGlobalIndex);
            if (removeVarValue) {
                vars[i].removeValue(0, this);
            }
            getEnvironment(i).save(() -> activateItemToProblem(i, ADDED));
            // we update intern values
            this.usedCapacity += computingTree.getLeaf(sortedGlobalIndex).getActivatedWeight();
            this.powerCreated += computingTree.getLeaf(sortedGlobalIndex).getActivatedProfit();
        }
    }

    /**
     * changes the constraint such that the item is NOT in the solution
     * Informs backtrack environment what to do
     *
     * @param i index of an item in the list given in the constructor
     */
    private void removeItemFromProblem(int i, boolean removeVarValue) throws ContradictionException {
        if (this.itemState[i] == NOT_DEFINED) {
            int sortedGlobalIndex = computingTree.leafToGlobalIndex(this.reverseOrder[i]);
            this.itemState[i] = REMOVED;
            computingTree.removeLeaf(sortedGlobalIndex);
            findingTree.removeLeaf(sortedGlobalIndex);
            if (removeVarValue) {
                vars[i].removeValue(1, this);
            }
            getEnvironment(i).save(() -> activateItemToProblem(i, REMOVED));
        }
    }

    /**
     * changes the propagator state such that the item is not determined anymore
     *
     * @param i             index of an item in the list given in the constructor
     * @param expectedState state of the item when reverting
     */
    private void activateItemToProblem(int i, int expectedState) {
        if (this.itemState[i] == expectedState) {
            int sortedGlobalIndex = computingTree.leafToGlobalIndex(this.reverseOrder[i]);
            computingTree.activateLeaf(sortedGlobalIndex);
            findingTree.activateLeaf(sortedGlobalIndex);
            if (this.itemState[i] == ADDED) {
                // we update intern values as the item was added to every solutions
                this.usedCapacity -= computingTree.getLeaf(sortedGlobalIndex).getActivatedWeight();
                this.powerCreated -= computingTree.getLeaf(sortedGlobalIndex).getActivatedProfit();
            }
            this.itemState[i] = NOT_DEFINED;
        } else if (this.itemState[i] != NOT_DEFINED) {
            throw new RuntimeException("???");
        }
    }

    private List<Integer> findMandatoryItems() {

        List<Integer> mandatoryList = new LinkedList<>();
        int maxWeight = 0;
        int index = computingTree.leafToGlobalIndex(0);
        // finding first active item
        if (!computingTree.getLeaf(index).isActive()) {
            index = findingTree.findNextRightItem(index, criticalItemInfos.index, maxWeight);
        }
        while (index != -1) {
            if (computingTree.isMandatory(criticalItemInfos, power.getLB() - powerCreated, index)) {
                mandatoryList.add(order[computingTree.globalToLeaf(index)]);
            } else {
                maxWeight = Math.max(maxWeight, computingTree.getNodeWeight(index));
            }
            index = findingTree.findNextRightItem(index, criticalItemInfos.index, maxWeight);
        }
        return mandatoryList;
    }

    private List<Integer> findForbiddenItems() {

        List<Integer> forbiddenList = new LinkedList<>();
        int minWeight = Integer.MAX_VALUE;
        int index = computingTree.getNumberNodes() - 1;
        // finding first active item
        if (!computingTree.getLeaf(index).isActive()) {
            index = findingTree.findNextLeftItem(index, criticalItemInfos.index, minWeight);
        }
        while (index != -1) {
            if (computingTree.isForbidden(criticalItemInfos, power.getLB() - powerCreated, index)) {
                forbiddenList.add(order[computingTree.globalToLeaf(index)]);
            } else {
                minWeight = Math.min(minWeight, computingTree.getNodeWeight(index));
            }
            index = findingTree.findNextLeftItem(index, criticalItemInfos.index, minWeight);
        }
        return forbiddenList;
    }

    private IEnvironment getEnvironment(int varIdx) {
        return this.vars[0].getEnvironment();
    }
}
