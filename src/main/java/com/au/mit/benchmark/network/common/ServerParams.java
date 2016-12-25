package com.au.mit.benchmark.network.common;

public class ServerParams {
    private final int port;
    private final int M; // max count of clients
    private final int X; // requests count per client

    public ServerParams(int port, int m, int x) {
        this.port = port;
        M = m;
        X = x;
    }

    public int getPort() {
        return port;
    }

    public int getM() {
        return M;
    }

    public int getX() {
        return X;
    }
}
