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

/**
 * KPItem
 */
public class KPItem implements Comparable<KPItem>, WeightInterface, ProfitInterface {

    private int profit;
    private int weight;
    private boolean active;

    public KPItem(int profit, int weight) {
        this.profit = profit;
        this.weight = weight;
        this.active = true;
    }

    public void desactivate() {
        active = false;
    }

    public void activate() {
        active = true;
    }

    public int getProfit() {
        return active ? profit : 0;
    }

    public void setProfit(int profit) {
        this.profit = profit;
    }

    public int getWeight() {
        return active ? weight : 0;
    }

    public boolean isActive() {
        return active;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public double getEfficiency() {
        return active ? (double) getProfit() / getWeight() : 0;
    }

    public int compareTo(KPItem item) {
        // this.compareTo(item) > 0 iff this.efficiency > item.efficiency
        int comparaison = getProfit() * item.getWeight() - item.getProfit() * getWeight();
        if (comparaison == 0) {
            // breaking ties in favor of smaller weights
            return item.getWeight() - getWeight();
        } else {
            return comparaison;
        }
    }
}