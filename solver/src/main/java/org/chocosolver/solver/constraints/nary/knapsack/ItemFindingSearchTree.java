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

import org.chocosolver.solver.constraints.nary.knapsack.innernode.factory.InnerNodeMaxWeightFactory;
import org.chocosolver.solver.constraints.nary.knapsack.innernode.factory.InnerNodeMinWeightFactory;

public class ItemFindingSearchTree {
    private BinarySearchFingerTree maxTree;
    private BinarySearchFingerTree minTree;

    public ItemFindingSearchTree(List<KPItem> sortedItems) {
        maxTree = new BinarySearchFingerTree(sortedItems, new InnerNodeMaxWeightFactory());
        minTree = new BinarySearchFingerTree(sortedItems, new InnerNodeMinWeightFactory());
    }

    public int findNextRightItem(int startingIndex, int criticalIndex, int weight) {
        return maxTree.binarySearch(startingIndex, criticalIndex, i -> weight < maxTree.getNodeWeight(i), true);
    }

    public int findNextLeftItem(int startingIndex, int criticalIndex, int weight) {
        return minTree.binarySearch(startingIndex, criticalIndex, i -> weight > minTree.getNodeWeight(i), false);
    }

    public void activateLeaf(int index) {
        maxTree.activateLeaf(index);
        minTree.activateLeaf(index);
    }

    public void removeLeaf(int index) {
        maxTree.removeLeaf(index);
        minTree.removeLeaf(index);
    }

    protected int getLeafWeight(int globalLeafIndex) {
        return maxTree.getLeaf(globalLeafIndex).getWeight();
    }

    protected int getNumberNodes() {
        return maxTree.getNumberNodes();
    }
}
