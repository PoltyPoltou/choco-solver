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

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TestingStuff {
    public static void runCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder().command(command);
        try {
            Process process = processBuilder.start();
            // wait for the process to complete
            process.waitFor();
            // close the resources
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static KPItem genItem(Random r) {
        return new KPItem(r.nextInt(20), r.nextInt(20));
    }

    public static void testingStuff(String[] args) {
        List<KPItem> list = new LinkedList<>();
        Random r = new Random(42);
        for (int i = 0; i < 10; i++) {
            list.add(genItem(r));
        }
        KnapsackConstraint cstrt = new KnapsackConstraint(20, 15, list);
        ComputingLossWeightTree computTree = cstrt.getComputingTree();
        String dir = "/home/polty/document/IMT2022/sandbox/print";
        try {
            FileWriter writer = new FileWriter(dir + "/comput.txt", false);
            writer.write(computTree.toString());
            writer.close();
            runCommand("dot", "-T", "svg", dir + "/comput.txt", "-O");
        } catch (IOException e) {
            throw new RuntimeException("File comput.txt could not be opened");
        }
        System.out.println(computTree.findCriticalItem(15).index);

        ItemFindingSearchTree findingTree = cstrt.getFindingTree();
        try {
            FileWriter writer = new FileWriter(dir + "/finding.txt", false);
            writer.write(findingTree.toString());
            writer.close();
            runCommand("dot", "-T", "svg", dir + "/finding.txt", "-O");
        } catch (IOException e) {
            throw new RuntimeException("File finding.txt could not be opened");
        }
    }
}
