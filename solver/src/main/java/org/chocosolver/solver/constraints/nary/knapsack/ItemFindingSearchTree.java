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
import org.chocosolver.solver.constraints.nary.knapsack.innernode.factory.InnerNodeSumFactory;

public class ItemFindingSearchTree extends BinarySearchFingerTree {

    public ItemFindingSearchTree(List<KPItem> sortedItems) {
        super(sortedItems, new InnerNodeMaxWeightFactory());
    }

    /**
    * @param sortedWeights sorted items by efficiency !
    * @param sortedEnergy sorted items by efficiency !
    */
   public ItemFindingSearchTree(int[] sortedWeights,int[] sortedEnergy) {
       super(sortedWeights, sortedEnergy, new InnerNodeSumFactory());
   }

    public int findNextRightItem(int startingIndex, int criticalIndex, int weight) {
        return binarySearch(startingIndex, criticalIndex, i -> weight < getNodeWeight(i), true);
    }

    public int findNextLeftItem(int startingIndex, int criticalIndex, int weight) {
        return binarySearch(startingIndex, criticalIndex, i -> weight > getNodeWeight(i), false);
    }
}
