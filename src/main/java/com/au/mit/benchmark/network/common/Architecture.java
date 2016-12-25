package com.au.mit.benchmark.network.common;

import com.au.mit.benchmark.network.protocols.tcp.ClientTCPSingleRequest;
import com.au.mit.benchmark.network.protocols.tcp.ClientTCPSingleSession;
import com.au.mit.benchmark.network.protocols.tcp.asynchronous.ServerAsynchronous;
import com.au.mit.benchmark.network.protocols.tcp.nonblocking.ServerNonblocking;
import com.au.mit.benchmark.network.protocols.tcp.threadpooled.ServerThreadPool;
import com.au.mit.benchmark.network.protocols.tcp.threadsingle.ServerSingleThread;
import com.au.mit.benchmark.network.protocols.tcp.threadunique.ServerThreadUnique;
import com.au.mit.benchmark.network.protocols.udp.ClientUDPSingleSession;
import com.au.mit.benchmark.network.protocols.udp.threadpooled.ServerUDPThreadPool;
import com.au.mit.benchmark.network.protocols.udp.threadunique.ServerUDPThreadUnique;

import java.util.function.Function;

public enum Architecture {
    TCPThreadUnique(ClientTCPSingleSession::new, ServerThreadUnique::new),
    TCPSingleThread(ClientTCPSingleRequest::new, ServerSingleThread::new),
    TCPThreadPool(ClientTCPSingleSession::new, ServerThreadPool::new),
    TCPAsynchronous(ClientTCPSingleSession::new, ServerAsynchronous::new),
    TCPNonblocking(ClientTCPSingleSession::new, ServerNonblocking::new),
    UDPThreadUnique(ClientUDPSingleSession::new, ServerUDPThreadUnique::new),
    UDPThreadPool(ClientUDPSingleSession::new, ServerUDPThreadPool::new);

    private final Function<ClientParams, AbstractClient> clientGenerator;
    private final Function<ServerParams, AbstractServer> serverGenerator;

    Architecture(Function<ClientParams, AbstractClient> clientGenerator,
                 Function<ServerParams, AbstractServer> serverGenerator) {
        this.clientGenerator = clientGenerator;
        this.serverGenerator = serverGenerator;
    }

    public AbstractClient generateClient(ClientParams params) {
        return clientGenerator.apply(params);
    }

    public AbstractServer generateServer(ServerParams params) {
        return serverGenerator.apply(params);
    }
}
