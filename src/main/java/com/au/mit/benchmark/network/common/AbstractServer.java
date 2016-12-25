package com.au.mit.benchmark.network.common;

public abstract class AbstractServer {
    protected final int port;
    protected final int M; //max count of clients
    protected final int X; // requests count per client
    protected volatile long requestProcessingTime;
    protected volatile long clientProcessingTime = 0;

    public AbstractServer(ServerParams params) {
        this.port = params.getPort();
        M = params.getM();
        X = params.getX();
    }

    public long getClientProcessingTime() {
        return clientProcessingTime;
    }

    public long getRequestProcessingTime() {
        return requestProcessingTime;
    }

    protected void setRequestProcessingTime(long time) {
        requestProcessingTime = time;
    }

    protected void updateRequestProcessingTime(long time) {
        requestProcessingTime += time;
    }

    synchronized void updateClientProcessingTime(long time) {
        clientProcessingTime += time;
    }

    public abstract void start();

    public abstract void stop();

}
