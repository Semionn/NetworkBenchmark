package com.au.mit.benchmark.network.common;

public class ClientParams {
    private final int N; //array size
    private final int Delta; //time response delay
    private final int X; // requests count;

    public ClientParams(int n, int delta, int x) {
        N = n;
        Delta = delta;
        X = x;
    }

    public int getN() {
        return N;
    }

    public int getDelta() {
        return Delta;
    }

    public int getX() {
        return X;
    }
}
