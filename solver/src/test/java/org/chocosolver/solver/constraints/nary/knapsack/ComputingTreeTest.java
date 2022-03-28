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

import org.testng.Assert;
import org.testng.annotations.Test;

public class ComputingTreeTest {

    private List<KPItem> genItems() {
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

    @Test
    public void testCriticalItem() {
        ComputingLossWeightTree tree = new ComputingLossWeightTree(genItems());
        Info info = tree.findCriticalItem(-10);
        Assert.assertEquals(info.index, 15);
        Assert.assertEquals(info.profit, 0);
        Assert.assertEquals(info.weight, 0);
        info = tree.findCriticalItem(0);
        Assert.assertEquals(info.index, 15);
        Assert.assertEquals(info.profit, 0);
        Assert.assertEquals(info.weight, 0);
        info = tree.findCriticalItem(150);
        Assert.assertEquals(info.index, tree.getNumberNodes());
        Assert.assertEquals(info.profit, 105);
        Assert.assertEquals(info.weight, 88);
        info = tree.findCriticalItem(20);
        Assert.assertEquals(info.index, 18);
        Assert.assertEquals(info.profit, 40.8);
        Assert.assertEquals(info.weight, 20);
    }

    @Test
    public void testCriticalItemWithDeletedItems() {
        ComputingLossWeightTree tree = new ComputingLossWeightTree(genItems());
        // removing item in the solution
        tree.removeLeaf(16);
        Info info = tree.findCriticalItem(20);
        Assert.assertEquals(info.index, 19);
        Assert.assertEquals(info.profit, 34 + 3 * 19.0 / 13.0);
        Assert.assertEquals(info.weight, 20);

        // removing critical item
        tree.activateLeaf(16);
        tree.removeLeaf(18);
        info = tree.findCriticalItem(20);
        Assert.assertEquals(info.index, 19);
        Assert.assertEquals(info.profit, 28 + 8 * 19 / 13.0);
        Assert.assertEquals(info.weight, 20);

        // removing item not in the solution
        tree.activateLeaf(18);
        tree.removeLeaf(19);
        info = tree.findCriticalItem(20);
        Assert.assertEquals(info.index, 18);
        Assert.assertEquals(info.profit, 40.8);
        Assert.assertEquals(info.weight, 20);
    }

}
