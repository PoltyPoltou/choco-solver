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
    private boolean mustRecomputeCriticalInfos;
    private int lastComputedWorldIndex;

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
        this.lastComputedWorldIndex = 0;
        Arrays.fill(this.itemState, 0);
        // we find the decreasing order of efficiency
        this.order = ArrayUtils.array(0, n - 1);
        ArraySort<Integer> sorter = new ArraySort<>(n, false, true);
        sorter.sort(order, n, new ItemComparator(weight, energy));
        this.orderedItems = new ArrayList<>();
        orderedItems.ensureCapacity(n);
        for (int i = 0; i < n; ++i) {
            orderedItems.add(new KPItem(energy[order[i]], weight[order[i]]));
            reverseOrder[order[i]] = i;
        }
        this.findingTree = new ItemFindingSearchTree(orderedItems);
        this.computingTree = new ComputingLossWeightTree(orderedItems);
        this.mustRecomputeCriticalInfos = true;
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
        if (mustRecomputeCriticalInfos) {
            computeCriticalIndex();
        }
        // if power LB is not reachable so we can't filter
        // (every item would be mandatory and forbidden)
        if (criticalItemInfos.profit + powerCreated >= power.getLB()) {
            List<Integer> mandatoryList = findMandatoryItems();
            List<Integer> forbiddenList = findForbiddenItems();
            for (int unorderedLeafIdx : mandatoryList) {
                addItemToSolution(unorderedLeafIdx, true);
                mustRecomputeCriticalInfos = true;
            }
            for (int unorderedLeafIdx : forbiddenList) {
                removeItemFromProblem(unorderedLeafIdx, true);
                mustRecomputeCriticalInfos = true;
            }
        }
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
        /*
         * we recompute the Dantzig solution if :
         * - a backtrack occured
         * - capacity UB changed
         * - an item has been determined
         */
        mustRecomputeCriticalInfos = mustRecomputeCriticalInfos || varIdx < n + 1
                || this.getModel().getEnvironment().getWorldIndex() < lastComputedWorldIndex;
        forcePropagate(PropagatorEventType.FULL_PROPAGATION);

    }

    @Override
    public ESat isEntailed() {
        // sum propagators of the KP contraint define the entailment
        return ESat.UNDEFINED;
    }

    /**
     * compute the Dantzig solution and set members variables
     */
    private void computeCriticalIndex() {
        this.criticalItemInfos = this.computingTree.findCriticalItem(this.capacity.getUB() - usedCapacity);
        mustRecomputeCriticalInfos = false;
        lastComputedWorldIndex = this.getModel().getEnvironment().getWorldIndex();
    }

    /**
     * changes the constraint such that the item is in the solution
     * Informs backtrack environment what to do
     *
     * @param i              index of an item in the list given in the constructor
     * @param removeVarValue true to remove the value in the variable domain
     */
    private void addItemToSolution(int i, boolean removeVarValue) throws ContradictionException {
        if (this.itemState[i] == NOT_DEFINED) {
            this.itemState[i] = ADDED;
            int sortedGlobalIndex = computingTree.leafToGlobalIndex(this.reverseOrder[i]);
            computingTree.removeLeaf(sortedGlobalIndex);
            findingTree.removeLeaf(sortedGlobalIndex);
            if (removeVarValue) {
                vars[i].removeValue(0, this);
            }
            getEnvironment().save(() -> activateItemToProblem(i, ADDED));
            // we update intern values
            this.usedCapacity += computingTree.getLeaf(sortedGlobalIndex).getActivatedWeight();
            this.powerCreated += computingTree.getLeaf(sortedGlobalIndex).getActivatedProfit();
        }
    }

    /**
     * changes the constraint such that the item is NOT in the solution
     * Informs backtrack environment what to do
     *
     * @param i              index of an item in the list given in the constructor
     * @param removeVarValue true to remove the value in the variable domain
     */
    private void removeItemFromProblem(int i, boolean removeVarValue) throws ContradictionException {
        if (this.itemState[i] == NOT_DEFINED) {
            this.itemState[i] = REMOVED;
            int sortedGlobalIndex = computingTree.leafToGlobalIndex(this.reverseOrder[i]);
            computingTree.removeLeaf(sortedGlobalIndex);
            findingTree.removeLeaf(sortedGlobalIndex);
            if (removeVarValue) {
                vars[i].removeValue(1, this);
            }
            getEnvironment().save(() -> activateItemToProblem(i, REMOVED));
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
            throw new RuntimeException("the item reverted does not have the expected state");
        }
    }

    /**
     * exploits {@code computeLimitWeightMandatory} to find all mandatory items in a
     * linear scan
     * 
     * @return indexes (given by the constructor) of mandatory items
     */
    private List<Integer> findMandatoryItems() {

        List<Integer> mandatoryList = new LinkedList<>();
        double allowedProfitLoss = criticalItemInfos.profit + powerCreated - power.getLB();
        // finding first active item
        int index = computingTree.leafToGlobalIndex(0);
        if (index != -1) {
            if (!computingTree.getLeaf(index).isActive()) {
                index = findingTree.findNextRightItem(index, criticalItemInfos.index, 0);
            }
            // not a trivial KP
            int maxWeight = 0;
            double criticalItemWeightNotInDantzig = 0;
            if (computingTree.isLeaf(criticalItemInfos.index)) {
                criticalItemWeightNotInDantzig = computingTree.getNodeWeight(criticalItemInfos.index)
                        - (criticalItemInfos.weight - criticalItemInfos.weightWithoutCriticalItem);
            }
            SearchInfos infos = new SearchInfos(false, criticalItemInfos.index,
                    0, 0, criticalItemWeightNotInDantzig);

            while (index != -1) {
                infos = computingTree.computeEquivalentWeightMandatory(criticalItemInfos, index, infos.endItem,
                        infos.profitAccumulated, infos.weightAccumulated, allowedProfitLoss,
                        infos.remainingWeightEndItem);
                if (infos.decision) {
                    mandatoryList.add(order[computingTree.globalToLeaf(index)]);
                } else {
                    // strong skipping
                    maxWeight = Math.max(maxWeight, computingTree.getNodeWeight(index));
                    maxWeight = Math.max(maxWeight, (int) infos.weightAccumulated);
                }
                index = findingTree.findNextRightItem(index, criticalItemInfos.index, maxWeight);
            }
        }
        return mandatoryList;
    }

    /**
     * exploits {@code computeLimitWeightForbidden} to find all forbidden items in a
     * linear scan
     * 
     * @return indexes (given by the constructor) of forbidden items
     */
    private List<Integer> findForbiddenItems() {

        List<Integer> forbiddenList = new LinkedList<>();
        double allowedProfitLoss = criticalItemInfos.profit + powerCreated - power.getLB();
        // finding first active item
        int index = criticalItemInfos.index;
        if (index != -1 && criticalItemInfos.index != computingTree.getNumberNodes()) {
            int maxWeight = 0;
            double criticalItemWeightInDantzig = criticalItemInfos.weight - criticalItemInfos.weightWithoutCriticalItem;
            if (!computingTree.getLeaf(index).isActive()) {
                index = findingTree.findNextRightItem(index, computingTree.getNumberNodes() - 1, maxWeight);
            }
            SearchInfos infos = new SearchInfos(false, criticalItemInfos.index,
                    0, 0, criticalItemWeightInDantzig);
            while (index != -1) {
                infos = computingTree.computeEquivalentWeightForbidden(criticalItemInfos, index, infos.endItem,
                        infos.profitAccumulated, infos.weightAccumulated, allowedProfitLoss,
                        infos.remainingWeightEndItem);
                if (infos.decision) {
                    forbiddenList.add(order[computingTree.globalToLeaf(index)]);
                } else {
                    // strong skipping
                    maxWeight = Math.max(maxWeight, (int) infos.weightAccumulated);
                    maxWeight = Math.max(maxWeight, computingTree.getNodeWeight(index));
                }
                index = findingTree.findNextRightItem(index, computingTree.getNumberNodes() - 1, maxWeight);
            }
        }
        return forbiddenList;
    }

    private IEnvironment getEnvironment() {
        return this.getModel().getEnvironment();
    }
}
