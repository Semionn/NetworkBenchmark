package com.au.mit.benchmark.network.common;

/**
 * Created by Semionn on 15.12.2016.
 */
public class Sorter {
    public static void sort(Integer[] array) {
        int N = array.length;

        for (int i = 0; i < N-1; i++) {
            for (int j = 1; j < N-i; j++) {
                if (array[j-1] > array[j]) {
                    int temp = array[j];
                    array[j] = array[j-1];
                    array[j-1] = temp;
                }
            }
        }
    }
}
