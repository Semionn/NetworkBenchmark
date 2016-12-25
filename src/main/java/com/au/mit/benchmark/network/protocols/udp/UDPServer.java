package com.au.mit.benchmark.network.protocols.udp;

import com.au.mit.benchmark.network.common.AbstractClientHandler;
import com.au.mit.benchmark.network.common.AbstractServer;
import com.au.mit.benchmark.network.common.ServerParams;

import java.io.IOException;

public abstract class UDPServer extends AbstractServer {
    public UDPServer(ServerParams params) {
        super(params);
    }

    public abstract void detachHandler(AbstractClientHandler udpClientHandler);

    public abstract void logException(IOException e);
}
