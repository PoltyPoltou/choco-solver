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

    private List<KPItem> genItemsWith0() {
        return Arrays.asList(new KPItem[] {
                new KPItem(3, 0),
                new KPItem(10, 3),
                new KPItem(10, 5),
                new KPItem(8, 4),
                new KPItem(16, 10),
                new KPItem(19, 13),
                new KPItem(16, 12),
                new KPItem(16, 12),
                new KPItem(2, 2),
                new KPItem(3, 9),
                new KPItem(5, 18),
                new KPItem(0, 3),
        });
    }

    /**
     * @param tree
     * @param dantzig
     * @param index       to test
     * @param equilibrium value of the optimal fractionnal solution if index is
     *                    removed
     */
    private void internMandatoryTest(ComputingLossWeightTree tree, Info dantzig, int index, double equilibrium) {
        Assert.assertEquals(
                tree.isMandatory(dantzig, equilibrium - 5, index),
                false);
        Assert.assertEquals(
                tree.isMandatory(dantzig, equilibrium, index),
                false);
        Assert.assertEquals(
                tree.isMandatory(dantzig, equilibrium + 0.1, index),
                true);
        Assert.assertEquals(
                tree.isMandatory(dantzig, equilibrium + 5, index),
                true);
    }

    /**
     * @param tree
     * @param dantzig
     * @param index       to test
     * @param equilibrium value of the optimal fractionnal solution if index is
     *                    removed
     */
    private void internForbiddenTest(ComputingLossWeightTree tree, Info dantzig, int index, double equilibrium) {
        Assert.assertEquals(
                tree.isForbidden(dantzig, equilibrium - 1, index),
                false);
        Assert.assertEquals(
                tree.isForbidden(dantzig, equilibrium, index),
                false);
        Assert.assertEquals(
                tree.isForbidden(dantzig, equilibrium + 0.1, index),
                true);
        Assert.assertEquals(
                tree.isForbidden(dantzig, equilibrium + 1, index),
                true);
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

    @Test
    public void testIsMandatory() {
        ComputingLossWeightTree tree = new ComputingLossWeightTree(genItems());
        int capacity = 15;
        Info dantzig = tree.findCriticalItem(capacity);
        // dantzig = (18,32.8,15,12)
        Assert.assertEquals(tree.isMandatory(dantzig, 27.7, 15), true);
        Assert.assertEquals(tree.isMandatory(dantzig, 27.6, 15), false);
        Assert.assertEquals(tree.isMandatory(dantzig, 27.5, 15), false);
        // if 16 is not mandatory, then 17 should not as w16 > w17
        Assert.assertEquals(tree.isMandatory(dantzig, 30, 16), false);
        Assert.assertEquals(tree.isMandatory(dantzig, 30, 17), false);
        for (int i = dantzig.index + 1; i < tree.getNumberNodes(); ++i) {
            // for indexes after criticalIndex we should always return false
            Assert.assertEquals(tree.isMandatory(dantzig, 30, i), false);
        }
        tree = new ComputingLossWeightTree(genItemsWith0());
        dantzig = tree.findCriticalItem(2);
        Assert.assertTrue(tree.isMandatory(dantzig, 3, 15));
        Assert.assertTrue(tree.isMandatory(dantzig, 4, 15));
        Assert.assertTrue(tree.isForbidden(dantzig, 4, 17));

    }

    @Test
    public void testIsMandatoryWithDeletedItems() {
        ComputingLossWeightTree tree = new ComputingLossWeightTree(genItems());
        tree.removeLeaf(18);
        tree.removeLeaf(19);
        Info dantzig = tree.findCriticalItem(15);
        // dantzig = (20, 32, 15, 12)
        Assert.assertEquals(tree.isMandatory(dantzig, dantzig.profit, 20), false);
        tree.removeLeaf(21);
        for (int i = tree.getNumberNodes() - tree.getNumberLeaves(); i < dantzig.index; i++) {
            // for all leaves in the fractionnal optimum, it must be mandatory if
            // profit LB = fractionnal optimum, except critical item if there is another
            // item with the same efficiency
            Assert.assertEquals(
                    tree.isMandatory(dantzig, dantzig.profit, i),
                    tree.getLeaf(i).isActive());
        }
        tree.activateLeaf(21);
        // best profit if we remove the item (here 15)
        internMandatoryTest(tree, dantzig, 15, 26);
        internMandatoryTest(tree, dantzig, 16, 32 - 10. / 3);

    }

    @Test
    public void testIsForbidden() {
        ComputingLossWeightTree tree = new ComputingLossWeightTree(genItems());
        int capacity = 20;
        Info dantzig = tree.findCriticalItem(capacity);
        // dantzig = (18, 40.8, 20, 12)
        Assert.assertEquals(tree.isForbidden(dantzig, 30, 24), true);
        internForbiddenTest(tree, dantzig, 22, 39.6);
        // if 22 is not forbidden, then 21 should not as w21 > w22
        Assert.assertEquals(tree.isForbidden(dantzig, 30, 22), false);
        Assert.assertEquals(tree.isForbidden(dantzig, 30, 21), false);
        Assert.assertEquals(tree.isForbidden(dantzig, 0, 17), false);
        // for indexes before or equal to criticalIndex we should always return false
        for (int i = tree.getNumberNodes() - tree.getNumberLeaves(); i < dantzig.index; ++i) {
            Assert.assertEquals(tree.isForbidden(dantzig, 30, i), false);
        }
    }

    @Test
    public void testIsForbiddenWithDeletedItems() {
        ComputingLossWeightTree tree = new ComputingLossWeightTree(genItems());
        tree.removeLeaf(18);
        tree.removeLeaf(19);
        Info dantzig = tree.findCriticalItem(15);
        // dantzig = (20, 32, 15, 12)
        Assert.assertEquals(tree.isForbidden(dantzig, dantzig.profit, 20), true);
        Assert.assertEquals(tree.isForbidden(dantzig, dantzig.profit, 21), true);
        Assert.assertEquals(tree.isForbidden(dantzig, dantzig.profit, 24), true);
        internForbiddenTest(tree, dantzig, 20, 26);
        tree.removeLeaf(21);
        for (int i = dantzig.index; i < tree.getNumberNodes(); i++) {
            // for all leaves NOT in the fractionnal optimum, it must be forbidden if
            // profit LB = fractionnal optimum, except critical item if there is another
            // item with the same efficiency and higher weight in the fractionnal optimum
            Assert.assertEquals(tree.isForbidden(dantzig, dantzig.profit, i),
                    tree.getLeaf(i).isActive());
        }
        tree.activateLeaf(21);
        // best profit if we remove the item (here 15)
        internForbiddenTest(tree, dantzig, 22, dantzig.profit - 2 / 3.);
        internForbiddenTest(tree, dantzig, 23, dantzig.profit - 13);

    }

}
