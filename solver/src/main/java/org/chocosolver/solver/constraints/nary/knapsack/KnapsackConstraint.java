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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class KnapsackConstraint {
    private int lowerBoundObjective;
    private int capacity;
    private Info criticalInfos;
    private List<KPItem> unsortedItems;
    private List<KPItem> items;
    private ItemFindingSearchTree findingTree;
    private ComputingLossWeightTree computingTree;

    public KnapsackConstraint(int lowerBoundObjective, int capacity, List<KPItem> items) {
        this.lowerBoundObjective = lowerBoundObjective;
        this.capacity = capacity;
        this.unsortedItems = new ArrayList<>(items);
        this.items = new ArrayList<>(items);
        this.items.sort(null);
        Collections.reverse(this.items);
        this.findingTree = new ItemFindingSearchTree(this.items);
        this.computingTree = new ComputingLossWeightTree(this.items);
        this.criticalInfos = computingTree.findCriticalItem(capacity);
    }

    public void setLowerBoundObjective(int lowerBoundObjective) {
        this.lowerBoundObjective = lowerBoundObjective;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        criticalInfos = computingTree.findCriticalItem(capacity);
    }

    public ItemFindingSearchTree getFindingTree() {
        return findingTree;
    }

    public ComputingLossWeightTree getComputingTree() {
        return computingTree;
    }

    /**
     * changes the constraint such that the item is in the solution
     *
     * @param i index of an item in the list given in the constructor
     */
    public void addItemToSolution(int i) {
        KPItem item = unsortedItems.get(i);
        int sortedIndex = computingTree.leafToGlobalIndex(i);
        if (this.items.get(sortedIndex).isActive()) {
            this.items.get(sortedIndex).desactivate();
            int globalLeafIndex = computingTree.leafToGlobalIndex(sortedIndex);
            computingTree.removeLeaf(globalLeafIndex);
            findingTree.removeLeaf(globalLeafIndex);
            setCapacity(capacity - item.getWeight());
            setLowerBoundObjective(lowerBoundObjective - item.getProfit());
        }
    }

    /**
     * changes the constraint such that the item is NOT in the solution
     *
     * @param i index of an item in the list given in the constructor
     */
    public void removeItemFromProblem(int i) {
        int sortedIndex = computingTree.leafToGlobalIndex(i);
        if (this.items.get(sortedIndex).isActive()) {
            this.items.get(sortedIndex).desactivate();
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
    public void activateItemToProblem(int i) {
        int sortedIndex = computingTree.leafToGlobalIndex(i);
        if (!this.items.get(sortedIndex).isActive()) {
            this.items.get(sortedIndex).activate();
            int globalLeafIndex = computingTree.leafToGlobalIndex(sortedIndex);
            computingTree.activateLeaf(globalLeafIndex);
            findingTree.activateLeaf(globalLeafIndex);
        }
    }

    public List<Integer> findMandatoryItems() {
        List<Integer> mandatoryList = new LinkedList<>();
        int index = 0;
        int maxWeight = 0;
        do {
            if (computingTree.isMandatory(capacity, criticalInfos.profit, criticalInfos.index, lowerBoundObjective,
                    index)) {
                mandatoryList.add(index);
            } else {
                maxWeight = Math.max(maxWeight, findingTree.getNodeWeight(index));
            }
            index = findingTree.findNextRightItem(index, criticalInfos.index, maxWeight);
        } while (index != -1);
        return mandatoryList;
    }

    public List<Integer> findForbiddenItems() {
        List<Integer> forbiddenList = new LinkedList<>();
        int index = findingTree.getNumberNodes() - 1;
        int minWeight = 0;
        do {
            if (computingTree.isForbidden(capacity, criticalInfos.profit, criticalInfos.index, lowerBoundObjective,
                    index)) {
                forbiddenList.add(index);
            } else {
                minWeight = Math.min(minWeight, findingTree.getNodeWeight(index));
            }
            index = findingTree.findNextLeftItem(index, criticalInfos.index, minWeight);
        } while (index != -1);
        return forbiddenList;
    }

}
