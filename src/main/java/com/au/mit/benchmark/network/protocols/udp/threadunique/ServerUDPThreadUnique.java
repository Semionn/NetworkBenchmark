package com.au.mit.benchmark.network.protocols.udp.threadunique;

import com.au.mit.benchmark.network.common.AbstractClientHandler;
import com.au.mit.benchmark.network.common.ServerParams;
import com.au.mit.benchmark.network.common.exceptions.CommunicationException;
import com.au.mit.benchmark.network.common.exceptions.RepeatedServerStartException;
import com.au.mit.benchmark.network.protocols.udp.UDPClientHandler;
import com.au.mit.benchmark.network.protocols.udp.UDPServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerUDPThreadUnique extends UDPServer {
    private Logger logger = Logger.getLogger(ServerUDPThreadUnique.class.getName());
    private final ConcurrentMap<AbstractClientHandler, Thread> clientThreads = new ConcurrentHashMap<>();
    private volatile DatagramSocket serverSocket = null;
    private volatile Thread mainThread;
    private volatile boolean closing = false;

    public ServerUDPThreadUnique(ServerParams params) {
        super(params);
    }

    @Override
    public void start() {
        if (mainThread != null) {
            throw new RepeatedServerStartException("Stop the server before starting again.");
        }
        closing = false;
        mainThread = new Thread(() -> {
            try {
                while (!Thread.interrupted() && !closing) {
                    for (int clientNum = 0; clientNum < M; clientNum++) {
                        if (available(port + clientNum)) {
                            serverSocket = new DatagramSocket(port + clientNum);
                            UDPClientHandler handler = new UDPClientHandler(serverSocket, this, X);
                            Thread clientThread = new Thread(handler::handle);
                            clientThreads.put(handler, clientThread);
                            clientThread.start();
                        }
                    }
                }
            } catch (IOException e) {
                if (!closing) {
                    throw new CommunicationException(e);
                }
            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }
            }
        });
        mainThread.start();
    }

    @Override
    public void stop() {
        closing = true;
        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
        synchronized (clientThreads) {
            clientThreads.values().forEach(Thread::interrupt);
            clientThreads.values().forEach(thread -> {
                try {
                    thread.join(100);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "InterruptedException occurred during joining client thread", e);
                }
            });
        }
    }

    public void logException(IOException e) {
        logger.log(Level.WARNING, "Exception occurred during communication with client", e);
    }

    public void detachHandler(AbstractClientHandler handler) {
        synchronized (clientThreads) {
            clientThreads.remove(handler);
        }
    }

    private static boolean available(int port) {
        try (DatagramSocket ignored = new DatagramSocket(port)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
