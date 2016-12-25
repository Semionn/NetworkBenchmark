package com.au.mit.benchmark.network.client;

import com.au.mit.benchmark.network.common.Architecture;
import com.au.mit.benchmark.network.common.exceptions.BenchmarkException;
import com.au.mit.benchmark.network.common.exceptions.CommunicationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BenchmarkClient {
    private static final Logger logger = Logger.getLogger(BenchmarkClient.class.getName());
    private static final int CONNECTION_RETRIES = 3;
    private static final int RETRY_TIME_MS = 1000;

    private final Architecture architecture;
    private final int paramsCount;

    private long requestProcessingTime;
    private long clientProcessingTime;

    private SocketChannel socketChannel = null;

    public BenchmarkClient(Architecture architecture, int paramsCount) {
        this.paramsCount = paramsCount;
        this.architecture = architecture;
    }

    public long getRequestProcessingTime() {
        return requestProcessingTime;
    }

    public long getClientProcessingTime() {
        return clientProcessingTime;
    }

    public void connect(String hostname, int port) {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(hostname, port);
            socketChannel = null;
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    socketChannel = SocketChannel.open(hostAddress);
                    break;
                } catch (IOException e) {
                    logger.info(String.format("Attempt %d: Connection opening failed due to error: %s", i, e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
            if (socketChannel == null) {
                throw new CommunicationException(String.format("Connection refused after %d attempts", CONNECTION_RETRIES));
            }
            DataInputStream inputStream = new DataInputStream(socketChannel.socket().getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socketChannel.socket().getOutputStream());
            outputStream.writeInt(paramsCount);
            outputStream.writeUTF(architecture.name());
            if (!inputStream.readBoolean()) {
                throw new CommunicationException("Something gone wrong with connection to the benchmark server");
            }
        } catch (IOException e) {
            throw new CommunicationException(e);
        } catch (InterruptedException ignored) { }
    }

    public void startServer(int X, int M) {
        if (socketChannel == null) {
            throw new BenchmarkException("Connection to the server wasn't established");
        }
        try {
            DataInputStream inputStream = new DataInputStream(socketChannel.socket().getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socketChannel.socket().getOutputStream());
            outputStream.writeInt(X);
            outputStream.writeInt(M);
            inputStream.readBoolean();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Communication exception occurred", e);
            throw new CommunicationException(e);
        }
    }

    public void stopServer() {
        if (socketChannel == null) {
            throw new BenchmarkException("Connection to the server wasn't established");
        }
        try {
            DataInputStream inputStream = new DataInputStream(socketChannel.socket().getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socketChannel.socket().getOutputStream());
            outputStream.writeBoolean(true);
            requestProcessingTime = inputStream.readLong();
            clientProcessingTime = inputStream.readLong();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Communication exception occurred", e);
            throw new CommunicationException(e);
        }
    }
}
