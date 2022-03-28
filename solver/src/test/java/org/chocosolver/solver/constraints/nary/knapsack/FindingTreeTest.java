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

    public void testSearch(ItemFindingSearchTree tree, int start, int expectendEnd, int bound) {
        int indexSearch = 0;
        indexSearch = tree.findNextRightItem(start, bound, tree.getNodeWeight(start));
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
        testSearch(tree, 15, 16, tree.getNumberNodes());
        testSearch(tree, 18, 19, tree.getNumberNodes());
        testSearch(tree, 19, 24, tree.getNumberNodes());
        testSearch(tree, 20, 24, tree.getNumberNodes());
        testSearch(tree, 20, -1, 21);

    }

    @Test(groups = "10s")
    public void testBinarySearchWithRemovedElements() {
        ItemFindingSearchTree tree = new ItemFindingSearchTree(genItems());
        tree.removeLeaf(19);
        // test maxTree
        testSearch(tree, 18, 20, tree.getNumberNodes());
        testSearch(tree, 18, -1, 19);
        testSearch(tree, 20, 24, tree.getNumberNodes());

    }
}
