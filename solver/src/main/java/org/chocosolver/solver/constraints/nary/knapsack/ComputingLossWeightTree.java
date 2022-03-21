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
    // used to make mandatory and forbidden test return false on equality
    static final double OFFSET = 1e-4;

    /**
     * @param sortedWeights sorted items by efficiency !
     * @param sortedEnergy  sorted items by efficiency !
     */
    public ComputingLossWeightTree(int[] sortedWeights, int[] sortedEnergy) {
        super(sortedWeights, sortedEnergy, new InnerNodeSumFactory());
    }

    public ComputingLossWeightTree(List<KPItem> sortedItems) {
        super(sortedItems, new InnerNodeSumFactory());
    }

    /**
     * @param capacity capacity of the KP to consider
     * @return true iff the optimal solution is to take every items
     */
    public boolean isFullSolution(int capacity) {
        return getNodeWeight(0) <= capacity;
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
     * Computes the index of the critical item and the solutions informations
     * 
     * @param capacity capacity of the KP to consider
     * @return Info object with informations of the solution
     */
    public Info findCriticalItem(int capacity) {
        int remainingCapacity = capacity;
        double criticalProfit = 0;
        int criticalIdx = 0;
        if (capacity < 0) {
            return new Info(getNumberNodes() - getNumberLeaves(), 0, 0, 0);
        }
        if (isTrivial(capacity)) {
            return new Info(getNumberNodes(), getNodeProfit(0), getNodeProfit(0), getNodeWeight(0));
        }
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
        return new Info(criticalIdx, criticalProfit, capacity - remainingCapacity, capacity);
    }

    /**
     * Test if the item is mandatory in the KP constraint
     *
     * @param criticalInfos    Info about the current KP (re)formulation
     * @param profitLowerBound LB of the KP constraint
     * @param itemIndex        global index of the item to test
     */
    public boolean isMandatory(Info criticalInfos, double profitLowerBound, int itemIndex) {
        // a mandatory item cannot be after the critical item
        // we don't consider inactive items
        // item with 0 profit can be removed anyway
        if (itemIndex > criticalInfos.index || !getLeaf(itemIndex).isActive() || getNodeProfit(itemIndex) == 0) {
            return false;
        }
        double energyDeficitPossible = criticalInfos.profit - profitLowerBound;
        // special cases
        if (criticalInfos.index == getNumberNodes()) {
            // if the KP optimal solution is trivial
            // we can have every item in the solution
            return getNodeProfit(itemIndex) > energyDeficitPossible + ComputingLossWeightTree.OFFSET;
        }
        if (getNodeWeight(itemIndex) == 0) {
            // we just see if removing the profit of the item is allowed,
            // then it is NOT mandatory
            return getNodeProfit(itemIndex) > energyDeficitPossible + ComputingLossWeightTree.OFFSET;
        }
        if (getNodeWeight(criticalInfos.index) == 0) {
            throw new RuntimeException("Critical item has 0 weight, should NEVER happen");
        }
        // end special cases
        double itemEfficiency = getLeaf(itemIndex).getEfficiency();
        int itemWeight = 0;
        // Weight and profit accumulated by the search
        int weight = 0;
        double profit = 0;
        // index of the current node examinated
        int index = criticalInfos.index;

        int weightWithoutCriticalItem = criticalInfos.weightWithoutCriticalItem;
        // Weight and profit that are examinated to know if we dive in the tree
        int nextWeight = 0;
        double nextProfit = 0;
        if (itemIndex != criticalInfos.index) {
            // If we are checking the critical item we can't use this
            // we use the remaining of the critical item as init
            nextWeight = getNodeWeight(criticalInfos.index) - criticalInfos.weight + weightWithoutCriticalItem;
            nextProfit = nextWeight * getLeaf(criticalInfos.index).getEfficiency();
            // weight to consider is the whole item
            itemWeight = getNodeWeight(itemIndex);
        } else {
            // weight to consider is only the part in the solution
            itemWeight = criticalInfos.weight - weightWithoutCriticalItem;
        }

        // we are looking for the node that contains the "exceeding" item
        while (profit + nextProfit - (weight + nextWeight) * itemEfficiency >= -energyDeficitPossible) {
            weight += nextWeight;
            profit += nextProfit;
            // speedup as we only need to know if weight >= itemWeight
            if (weight >= itemWeight) {
                return false;
            }
            index = getNextNode(index, true);
            // there is no node left and we know that weight < itemWeight
            if (index == -1) {
                // we must give up all of the item without additionnal profit
                return itemWeight * itemEfficiency - profit > energyDeficitPossible
                        + ComputingLossWeightTree.OFFSET;
            }
            nextProfit = getNodeProfit(index);
            nextWeight = getNodeWeight(index);
        }
        // speedup
        if (weight + nextWeight + ComputingLossWeightTree.OFFSET < itemWeight) {
            return true;
        }
        // now we dive into the subtree to find the "exceeding" item
        while (isInnerNode(index)) {
            int leftChild = getLeftChild(index);
            nextProfit = getNodeProfit(leftChild);
            nextWeight = getNodeWeight(leftChild);
            if (profit + nextProfit - (weight + nextWeight) * itemEfficiency <= -energyDeficitPossible) {
                if (weight + nextWeight + ComputingLossWeightTree.OFFSET < itemWeight) {
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
        // exists, thus we must give up the rest without additionnal profit
        if (!isLeaf(index)) {
            return itemWeight * itemEfficiency - profit + ComputingLossWeightTree.OFFSET < energyDeficitPossible;
        } else {
            // we have to compute the exact part of this item that can be used
            double portionWeight = (weight * itemEfficiency - profit - energyDeficitPossible)
                    / (getLeaf(index).getEfficiency() - itemEfficiency);
            return weight + portionWeight + ComputingLossWeightTree.OFFSET < itemWeight;
        }
    }

    /**
     * Test if the item is forbidden in the KP constraint
     *
     * @param capacity          capacity of the KP
     * @param criticalItemIndex for the given capacity, index of the critical item
     * @param profitLowerBound  LB of the KP constraint
     * @param itemIndex         global index of the item to test
     */
    public boolean isForbidden(Info criticalInfos, double profitLowerBound, int itemIndex) {
        if (!getLeaf(itemIndex).isActive() || criticalInfos.index > itemIndex || getNodeWeight(itemIndex) == 0) {
            return false;
        }
        double itemEfficiency = getLeaf(itemIndex).getEfficiency();
        int itemWeight = getLeaf(itemIndex).getWeight();
        // we compute the new critical item if we take itemIndex in the solution
        // we know that the item is after the original critical item, thus we don't need
        // to remove items in the tree
        double energyDeficitPossible = criticalInfos.profit - profitLowerBound;
        // Weight and profit accumulated by the search
        int weight = 0;
        double profit = 0;
        // index of the current node examinated
        int index = criticalInfos.index;
        int weightWithoutCriticalItem = criticalInfos.weightWithoutCriticalItem;
        // Weight and profit that are examinated to know if we dive in the tree
        // we use the remaining of the critical item as init
        int nextWeight = 0;
        double nextProfit = 0;
        if (itemIndex != criticalInfos.index) {
            // If we are checking the critical item we can't use this
            // we use the part of the critical item in the solution as init
            nextWeight = criticalInfos.weight - weightWithoutCriticalItem;
            nextProfit = nextWeight * getLeaf(criticalInfos.index).getEfficiency();
            // weight to consider is the whole item
            itemWeight = getLeaf(itemIndex).getWeight();
        } else {
            // weight to consider is only the part NOT in the solution
            itemWeight = getLeaf(itemIndex).getWeight() - (criticalInfos.weight - weightWithoutCriticalItem);
        }

        // we are looking for the node that contains the "exceeding" item
        while ((weight + nextWeight) * itemEfficiency - profit - nextProfit >= -energyDeficitPossible) {
            weight += nextWeight;
            profit += nextProfit;
            // speedup as we only need to know if weight >= itemWeight
            if (weight >= itemWeight) {
                return false;
            }
            index = getNextNode(index, false);
            // there is no node left and we know that weight < itemWeight
            if (index == -1) {
                return itemWeight * itemEfficiency - profit + ComputingLossWeightTree.OFFSET < energyDeficitPossible;
            }
            nextProfit = getNodeProfit(index);
            nextWeight = getNodeWeight(index);
        }
        // speedup
        if (weight + nextWeight + ComputingLossWeightTree.OFFSET < itemWeight) {
            return true;
        }
        // now we dive into the subtree to find the "exceeding" item
        while (isInnerNode(index)) {
            int rightChild = getRightChild(index);
            nextProfit = getNodeProfit(rightChild);
            nextWeight = getNodeWeight(rightChild);
            if ((weight + nextWeight) * itemEfficiency - profit - nextProfit <= -energyDeficitPossible) {
                if (weight + nextWeight + ComputingLossWeightTree.OFFSET < itemWeight) {
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
            return itemWeight * itemEfficiency - profit + ComputingLossWeightTree.OFFSET < energyDeficitPossible;
        } else {
            // computes the exact part of the last item that can be used
            double portionWeight = (profit - energyDeficitPossible - weight * itemEfficiency)
                    / (itemEfficiency - getLeaf(index).getEfficiency());
            return weight + portionWeight + ComputingLossWeightTree.OFFSET < itemWeight;
        }
    }
}
