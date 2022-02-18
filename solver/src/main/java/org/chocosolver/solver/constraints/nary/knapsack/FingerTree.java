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

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

/**
 * FingerTree
 */
public class FingerTree<NodeType, LeafType> {
    private ArrayList<NodeType> innerNodeTreeList;
    private ArrayList<LeafType> leafTreeList;

    public ArrayList<NodeType> getInnerNodeTreeList() {
        return innerNodeTreeList;
    }

    public ArrayList<LeafType> getLeafTreeList() {
        return leafTreeList;
    }

    /**
     * @param sortedItems Knapsack items sorted by decreasing efficiency, if
     *                    equality higher weight first
     */
    public FingerTree(List<LeafType> sortedItems) {
        init(sortedItems);
    }
    protected FingerTree(){}
    protected void init(List<LeafType> sortedItems){
        leafTreeList = new ArrayList<>(sortedItems);
        innerNodeTreeList = new ArrayList<>();
        int innerNodeSize = PowerMath.power2(1 + (int) (Math.log(sortedItems.size()) / Math.log(2))) - 1;
        innerNodeTreeList.ensureCapacity(innerNodeSize);
        for (int i = 0; i < innerNodeSize; i++) {
            innerNodeTreeList.add(null);
        }
    }
    public int getLeafParentIndex(int leafIndex) {
        return getLeafParentIndex(leafIndex, true);
    }

    public int getLeafParentIndex(int leafIndex, boolean offset) {
        return Math.floorDiv((offset ? this.innerNodeTreeList.size() : 0) + leafIndex - 1, 2);
    }

    public int getParentIndex(int nodeIndex) {
        if (nodeIndex != 0 && (isLeaf(nodeIndex) || isInnerNode(nodeIndex))) {
            return Math.floorDiv(nodeIndex - 1, 2);
        } else {
            throw new IllegalArgumentException("Getting parent of an invalid index : " + Integer.toString(nodeIndex));
        }
    }

    /**
     * Finger tree specific search
     * 
     * @param nodeIndex
     * @param right     true iff going right
     * @return the node to the right/left at the same depth of nodeIndex
     */
    public int getFingerNeighboor(int nodeIndex, boolean right) {
        // checks if we are the last element to the border
        if (!PowerMath.isPowerOfTwo(right ? (nodeIndex + 2) : (nodeIndex + 1))) {
            return nodeIndex + (right ? 1 : -1);
        } else {
            throw new RuntimeException("Tried to get finger neigboor of the rightmost node");
        }
    }

    public int getLeftChild(int nodeIndex) {
        return 2 * nodeIndex + 1;
    }

    /**
     * @param nodeIndex
     * @return index of the node with the same parent as nodeIndex, if it does not
     *         exists returns nodeIndex
     */
    public int getBrother(int nodeIndex) {
        // return nodeIndex if there is no brother
        if (nodeIndex == 0) {
            return 0;
        } else if (nodeIndex % 2 == 0) {
            // right to left
            return nodeIndex - 1;
        } else if (nodeIndex == getInnerNodeTreeList().size() + getLeafTreeList().size() - 1) {
            // left to right but right does not exist (only happens on leaves)
            return nodeIndex;
        } else {
            // nodeIndex % 2 == 1
            // left to right otherwise
            return nodeIndex + 1;
        }
    }

    public int getRightChild(int nodeIndex) {
        return 2 * nodeIndex + 2;
    }

    public boolean isInnerNode(int index) {
        return index < innerNodeTreeList.size();
    }

    public NodeType getInnerNode(int index) {
        return innerNodeTreeList.get(index);
    }

    public boolean isLeaf(int index) {
        return index >= innerNodeTreeList.size() && index < innerNodeTreeList.size() + leafTreeList.size();
    }

    public LeafType getLeaf(int index) {
        return leafTreeList.get(index - innerNodeTreeList.size());
    }

    /**
     * @param startingIndex starting node index
     * @param right         true will search to the right, false to the left
     * @return index of the next node to explore
     */
    public int getNextNode(int startingIndex, boolean right) {
        int index = startingIndex;
        while (index != 0 && index % 2 == (right ? 0 : 1)) {
            index = getParentIndex(index);
        }
        if (index == 0) {
            throw new RuntimeException("getNextNode led to the end of the tree");
        } else {
            return getFingerNeighboor(index, right);
        }
    }

    public int leafToGlobalIndex(int index) {
        return index + getInnerNodeTreeList().size();
    }

    public int globalToLeaf(int index) {
        return index - getInnerNodeTreeList().size();
    }

    public int getNumberNodes() {
        return getLeafTreeList().size() + getInnerNodeTreeList().size();
    }
}