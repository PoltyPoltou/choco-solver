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
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final int[] order;
    private final double[] ratio;
    private final int n;
    private int deltaCapacity;
    private final int capacity;
    private int deltaLB;
    private final IntVar profitLB;
    private final int[] itemState;
    private final ItemFindingSearchTree findingTree;
    private final ComputingLossWeightTree computingTree;
    private final ArrayList<KPItem> orderedItems;
    private Info criticalItemInfos;
    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropKnapsack2(IntVar[] itemOccurence, int capacity, IntVar profitLB,
                        int[] weight, int[] energy) {
        super(ArrayUtils.append(itemOccurence, new IntVar[]{profitLB}), PropagatorPriority.LINEAR, false);
        this.n = itemOccurence.length;
        this.itemState = new int[n];
        Arrays.fill(this.itemState, 0);
        this.profitLB = vars[n];
        this.deltaCapacity = 0;
        this.capacity = capacity;
        this.deltaLB = 0;
        // ratio energy/weight of every item (not ordred)
        this.ratio = new double[n];
        for (int i = 0; i < n; i++) {
            ratio[i] = weight[i] == 0?Double.MAX_VALUE : ((double) (energy[i]) / (double) (weight[i]));
        }
        // we find the decreasing order of efficiency
        this.order = ArrayUtils.array(0,n-1);
        ArraySort sorter = new ArraySort(n,false,true);
        sorter.sort(order, n, (i1, i2) -> Double.compare(ratio[i2],ratio[i1]));
        this.orderedItems = new ArrayList<>();
        orderedItems.ensureCapacity(n);
        for (int i = 0; i < n; ++i) {
            orderedItems.set(i, new KPItem(energy[order[i]],weight[order[i]]));
        }
        this.findingTree = new ItemFindingSearchTree(orderedItems);
        this.computingTree = new ComputingLossWeightTree(orderedItems);
        this.criticalItemInfos = this.computingTree.findCriticalItem(this.capacity);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        /*
            TODO remove this comment
            This method is called once at initialization
            We will not call it afterwards
        */
        this.criticalItemInfos = computingTree.findCriticalItem(capacity);
    }
    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        /*
            TODO remove this comment
            This method is called when varIdx changes according to mask
        */
        if(varIdx < n){
            // item changed
            // we update the trees
            if(this.vars[varIdx].getValue() == 0){
                this.removeItemFromProblem(varIdx);
            }else if(this.vars[varIdx].getValue() == 1){
                this.addItemToSolution(varIdx);
            }
        }
    }

    @Override
    public ESat isEntailed() {
        return ESat.UNDEFINED;
    }
    /**
     * changes the constraint such that the item is in the solution
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
        }
    }

    /**
     * changes the constraint such that the item is NOT in the solution
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
            int globalLeafIndex = computingTree.leafToGlobalIndex(sortedIndex);
            computingTree.activateLeaf(globalLeafIndex);
            findingTree.activateLeaf(globalLeafIndex);
            if(this.itemState[i] == ADDED) {
                deltaCapacity += item.getWeight();
                deltaLB += item.getProfit();
            }
            this.itemState[i] = NOT_DEFINED;
        }
    }
    private List<Integer> findMandatoryItems() {
        List<Integer> mandatoryList = new LinkedList<>();
        int index = 0;
        int maxWeight = 0;
        do {
            if (computingTree.isMandatory(
                    criticalItemInfos.weight + deltaCapacity,
                    criticalItemInfos.profit,
                    criticalItemInfos.index,
                    profitLB.getUB() + deltaLB,
                    index)) {
                mandatoryList.add(index);
            } else {
                maxWeight = Math.max(maxWeight, findingTree.getNodeWeight(index));
            }
            index = findingTree.findNextRightItem(index, criticalItemInfos.index, maxWeight);
        } while (index != -1);
        return mandatoryList;
    }

    private List<Integer> findForbiddenItems() {
        List<Integer> forbiddenList = new LinkedList<>();
        int index = findingTree.getNumberNodes() - 1;
        int minWeight = 0;
        do {
            if (computingTree.isForbidden(
                    criticalItemInfos.weight + deltaCapacity,
                    criticalItemInfos.profit,
                    criticalItemInfos.index,
                    profitLB.getLB() + deltaLB,
                    index)) {
                forbiddenList.add(index);
            } else {
                minWeight = Math.min(minWeight, findingTree.getNodeWeight(index));
            }
            index = findingTree.findNextLeftItem(index, criticalItemInfos.index, minWeight);
        } while (index != -1);
        return forbiddenList;
    }
}
