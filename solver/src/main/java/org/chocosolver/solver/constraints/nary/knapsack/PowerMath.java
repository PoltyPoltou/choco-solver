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

public abstract class PowerMath {

    public static boolean isPowerOfTwo(int n) {
        double ratio = Math.log(n) / Math.log(2);
        return (int) (Math.ceil(ratio)) == (int) (Math.floor(ratio));
    }

    public static int power2(double exponant) {
        return (int) Math.pow(2, exponant);
    }

}
