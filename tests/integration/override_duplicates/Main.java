package com.jvm.external.override_duplicates;

import com.netflix.ndbench.core.NdBenchDriver;

public class Main {

    public static void main(String[] args) {
        System.out.printf(
            "Timeout is: %s\n",
            NdBenchDriver.TIMEOUT);
    }
}
