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

import java.util.List;

import org.chocosolver.solver.constraints.nary.knapsack.innernode.factory.InnerNodeSumFactory;

public class ComputingLossWeightTree extends BinarySearchFingerTree {
    /**
     * @param sortedWeights sorted items by efficiency !
     * @param sortedEnergy sorted items by efficiency !
     */
    public ComputingLossWeightTree(int[] sortedWeights,int[] sortedEnergy) {
        super(sortedWeights, sortedEnergy, new InnerNodeSumFactory());
    }
    public ComputingLossWeightTree(List<KPItem> sortedItems) {
        super(sortedItems, new InnerNodeSumFactory());
    }

    private ProfitInterface getNodeProfitInterface(int index) {
        if (isLeaf(index)) {
            return getLeaf(index);
        } else if (isInnerNode(index)) {
            // the constructor ensure that every inner node is type InnerNodeSum,
            // which extends ProfitInterface
            return (ProfitInterface) getInnerNode(index);
        } else {
            throw new IndexOutOfBoundsException(
                    "Looking for an index that corresponds to nothing in the tree (leaf outside of range)");
        }
    }

    public int getNodeProfit(int index) {
        return getNodeProfitInterface(index).getProfit();
    }

    public boolean isTrivial(int capacity) {
        // detect the trivial case where we can put every item in the KP
        return getNodeWeight(0) < capacity;
    }

    /**
     * @param capacity capacity of the KP to consider
     * @return the index of the critical item and the solutions informations
     */
    public Info findCriticalItem(int capacity) {
        int remainingCapacity = capacity;
        double criticalProfit = 0;
        int criticalIdx = 0;
        while (isInnerNode(criticalIdx)) {
            int leftChild = getLeftChild(criticalIdx);
            int rightChild = getRightChild(criticalIdx);
            if (isLeaf(leftChild) || isInnerNode(leftChild)) {
                if (getNodeWeight(leftChild) >= remainingCapacity) {
                    criticalIdx = leftChild;
                } else {
                    criticalIdx = rightChild;
                    remainingCapacity -= getNodeWeight(leftChild);
                    criticalProfit += getNodeProfit(leftChild);
                }
            } else {
                throw new RuntimeException("Finding a critical item led to an empty Item");
            }
        }
        criticalProfit += getLeaf(criticalIdx).getEfficiency() * remainingCapacity;
        return new Info(criticalIdx, criticalProfit, capacity);
    }

    /**
     * Test if the item is mandatory in the KP constraint
     *
     * @param capacity         capacity of the KP
     * @param dantzigProfit    profit of the fractionnal solution
     * @param criticalIndex    global index of the critical item
     * @param profitLowerBound LB of the KP constraint
     * @param itemIndex        global index of the item to test
     */
    public boolean isMandatory(int capacity, double dantzigProfit, int criticalIndex,
            double profitLowerBound, int itemIndex) {
        if (itemIndex > criticalIndex) {
            return false;
        }
        double efficiency = getLeaf(itemIndex).getEfficiency();
        double itemWeight = getLeaf(itemIndex).getWeight();
        // Weight and profit accumulated by the search
        int weight = 0;
        double profit = 0;
        // index of the current node examinated
        int index = criticalIndex;
        //
        int weightWithoutCriticalItem = getLeafTreeList().stream().limit(globalToLeaf(criticalIndex))
                .mapToInt(item -> item.getWeight()).sum();
        // Weight and profit that are examinated to know if we dive in the tree
        int nextWeight = 0;
        double nextProfit = 0;
        if (itemIndex != criticalIndex) {
            // If we are checking the critical item we can't use this
            // we use the remaining of the critical item as init
            nextWeight = getLeaf(criticalIndex).getWeight() - capacity + weightWithoutCriticalItem;
            nextProfit = nextWeight * getLeaf(criticalIndex).getEfficiency();
        }
        // we are looking for the node that contains the "exceeding" item
        while (profit + nextProfit - (weight + nextWeight) * efficiency >= profitLowerBound - dantzigProfit) {
            index = getNextNode(index, true);
            weight += nextWeight;
            profit += nextProfit;
            // speedup as we only need to know if weight >= itemWeight
            if (weight >= itemWeight) {
                return false;
            }
            nextProfit = getNodeProfit(index);
            nextWeight = getNodeWeight(index);
        }
        // speedup
        if (weight + nextWeight < itemWeight) {
            return true;
        }
        // now we dive into the subtree to find the "exceeding" item
        while (isInnerNode(index)) {
            int leftChild = getLeftChild(index);
            nextProfit = getNodeProfit(leftChild);
            nextWeight = getNodeWeight(leftChild);
            if (profit + nextProfit - (weight + nextWeight) * efficiency <= profitLowerBound - dantzigProfit) {
                if (weight + nextWeight < itemWeight) {
                    return true;
                }
                index = leftChild;
            } else {
                weight += nextWeight;
                profit += nextProfit;
                // speedup as we only need to know if weight >= itemWeight
                if (weight >= itemWeight) {
                    return false;
                }
                index = getRightChild(index);
            }
        }
        // Special case where we went to the end of the tree and the leaf does not
        // exists
        if (!isLeaf(index)) {
            return weight < itemWeight;
        } else {
            // we have to compute the exact part of this item that can be used
            double portionWeight = (profitLowerBound - dantzigProfit - profit + weight * efficiency)
                    / (getLeaf(index).getEfficiency() - efficiency);
            return weight + portionWeight < itemWeight;
        }
    }

    /**
     * Test if the item is forbidden in the KP constraint
     *
     * @param capacity         capacity of the KP
     * @param dantzigProfit    profit of the fractionnal solution
     * @param criticalIndex    global index of the critical item
     * @param profitLowerBound LB of the KP constraint
     * @param itemIndex        global index of the item to test
     */
    public boolean isForbidden(int capacity, double dantzigProfit, int criticalIndex,
            double profitLowerBound, int itemIndex) {
        if (itemIndex <= criticalIndex) {
            return false;
        }
        double efficiency = getLeaf(itemIndex).getEfficiency();
        double itemWeight = getLeaf(itemIndex).getWeight();
        // Weight and profit accumulated by the search
        int weight = 0;
        double profit = 0;
        // index of the current node examinated
        int index = criticalIndex;
        //
        int weightWithoutCriticalItem = getLeafTreeList().stream().limit(globalToLeaf(criticalIndex))
                .mapToInt(item -> item.getWeight()).sum();
        // Weight and profit that are examinated to know if we dive in the tree
        // we use the remaining of the critical item as init
        int nextWeight = capacity - weightWithoutCriticalItem;
        double nextProfit = nextWeight * getLeaf(criticalIndex).getEfficiency();
        // we are looking for the node that contains the "exceeding" item
        while ((weight + nextWeight) * efficiency - profit - nextProfit >= profitLowerBound - dantzigProfit) {
            index = getNextNode(index, false);
            weight += nextWeight;
            profit += nextProfit;
            // speedup as we only need to know if weight >= itemWeight
            if (weight >= itemWeight) {
                return false;
            }
            nextProfit = getNodeProfit(index);
            nextWeight = getNodeWeight(index);
        }
        // speedup
        if (weight + nextWeight < itemWeight) {
            return true;
        }
        // now we dive into the subtree to find the "exceeding" item
        while (isInnerNode(index)) {
            int rightChild = getRightChild(index);
            nextProfit = getNodeProfit(rightChild);
            nextWeight = getNodeWeight(rightChild);
            if ((weight + nextWeight) * efficiency - profit - nextProfit <= profitLowerBound - dantzigProfit) {
                if (weight + nextWeight < itemWeight) {
                    return true;
                }
                index = rightChild;
            } else {
                weight += nextWeight;
                profit += nextProfit;
                // speedup as we only need to know if weight >= itemWeight
                if (weight >= itemWeight) {
                    return false;
                }
                index = getLeftChild(index);
            }
        }
        // Special case where we went to the end of the tree and the leaf does not
        // exists
        if (!isLeaf(index)) {
            return weight < itemWeight;
        } else {
            // we have to compute the exact part of this item that can be used
            double portionWeight = (profitLowerBound - dantzigProfit + profit - weight * efficiency)
                    / (efficiency - getLeaf(index).getEfficiency());
            return weight + portionWeight < itemWeight;
        }
    }
}
