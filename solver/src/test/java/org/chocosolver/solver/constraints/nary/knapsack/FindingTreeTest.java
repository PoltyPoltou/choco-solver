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

import java.util.Arrays;
import java.util.List;

import org.chocosolver.solver.constraints.nary.knapsack.innernode.factory.InnerNodeMaxWeightFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FindingTreeTest {

    public List<KPItem> genItems() {
        return Arrays.asList(new KPItem[] {
                new KPItem(10, 3),
                new KPItem(10, 5),
                new KPItem(8, 4),
                new KPItem(16, 10),
                new KPItem(19, 13),
                new KPItem(16, 12),
                new KPItem(16, 12),
                new KPItem(2, 2),
                new KPItem(3, 9),
                new KPItem(5, 18) });
    }

    public void testSearch(ItemFindingSearchTree tree, int start, int expectendEnd, int bound,
            boolean right) {
        int indexSearch = 0;
        if (right) {
            indexSearch = tree.findNextRightItem(start, bound, tree.getLeafWeight(start));
        } else {
            indexSearch = tree.findNextLeftItem(start, bound, tree.getLeafWeight(start));
        }
        Assert.assertEquals(indexSearch, expectendEnd);
    }

    @Test(groups = "10s")
    public void testRemoveLeaf() {
        BinarySearchFingerTree tree = new BinarySearchFingerTree(genItems(), new InnerNodeMaxWeightFactory());
        tree.removeLeaf(19);
        Assert.assertEquals(12, tree.getNodeWeight(9));
        Assert.assertEquals(12, tree.getNodeWeight(4));
        Assert.assertEquals(18, tree.getNodeWeight(0));
    }

    @Test(groups = "10s")
    public void testActivateLeaf() {
        BinarySearchFingerTree tree = new BinarySearchFingerTree(genItems(), new InnerNodeMaxWeightFactory());
        tree.removeLeaf(19);
        Assert.assertEquals(12, tree.getNodeWeight(9));
        Assert.assertEquals(12, tree.getNodeWeight(4));
        Assert.assertEquals(18, tree.getNodeWeight(0));
        tree.activateLeaf(19);
        Assert.assertEquals(13, tree.getNodeWeight(9));
        Assert.assertEquals(13, tree.getNodeWeight(4));
        Assert.assertEquals(18, tree.getNodeWeight(0));
    }

    @Test(groups = "10s")
    public void testSimpleBinarySearch() {
        ItemFindingSearchTree tree = new ItemFindingSearchTree(genItems());
        // test maxTree
        testSearch(tree, 15, 16, tree.getNumberNodes(), true);
        testSearch(tree, 18, 19, tree.getNumberNodes(), true);
        testSearch(tree, 19, 24, tree.getNumberNodes(), true);
        testSearch(tree, 20, 24, tree.getNumberNodes(), true);
        testSearch(tree, 20, -1, 21, true);
        // test minTree
        testSearch(tree, 19, 18, 15, false);
        testSearch(tree, 20, 18, 15, false);
        testSearch(tree, 22, -1, 15, false);
        testSearch(tree, 23, 22, 15, false);
        testSearch(tree, 21, -1, 19, false);
    }

    @Test(groups = "10s")
    public void testBinarySearchWithRemovedElements() {
        ItemFindingSearchTree tree = new ItemFindingSearchTree(genItems());
        tree.removeLeaf(19);
        // test maxTree
        testSearch(tree, 18, 20, tree.getNumberNodes(), true);
        testSearch(tree, 18, -1, 19, true);
        testSearch(tree, 20, 24, tree.getNumberNodes(), true);
        // test minTree
        tree.removeLeaf(22);
        testSearch(tree, 23, 17, 15, false);
        testSearch(tree, 23, -1, 20, false);
        testSearch(tree, 24, 23, 15, false);
        testSearch(tree, 20, 18, 15, false);
    }
}
