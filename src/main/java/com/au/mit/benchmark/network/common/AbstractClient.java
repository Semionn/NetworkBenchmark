package com.au.mit.benchmark.network.common;

import com.au.mit.benchmark.network.common.exceptions.CommunicationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractClient {
    protected static final Logger logger = Logger.getLogger(AbstractClient.class.getName());
    private static int CONNECTION_RETRIES = 10;
    private static int RETRY_TIME_MS = 500;
    protected final int N;
    protected final int Delta;
    protected final int X;
    private long workTime = 0;

    public AbstractClient(ClientParams params) {
        N = params.getN();
        Delta = params.getDelta();
        X = params.getX();
    }

    public long getWorkTime() {
        return workTime;
    }

    public boolean connect(String hostname, int port, int clientNum) {
        long startTime = System.currentTimeMillis();
        boolean result = connectImpl(hostname, port, clientNum);
        workTime = System.currentTimeMillis() - startTime;
        return result;
    }

    protected abstract boolean connectImpl(String hostname, int port, int clientNum);

    public abstract void disconnect();

    protected int[] generateArray(int N) {
        final Random gen = new Random();
        int[] result = new int[N];
        for (int i = 0; i < N; i++) {
            result[i] = gen.nextInt();
        }
        return result;
    }

    protected ProtobufMessage.Message generateMessage(int[] array) {
        ProtobufMessage.Message.Builder builder = ProtobufMessage.Message.newBuilder()
                .setMessageSize(N + 1);
        final List<Integer> arrayList = Arrays.stream(array)
                .boxed()
                .collect(Collectors.toList());
        builder.addAllBody(arrayList);
        return builder.build();
    }

    protected <R> R sendRequest(String hostname, int port, Function<SocketChannel, R> request) throws CommunicationException {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(hostname, port);
            SocketChannel socketChannel = null;
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
            R res = request.apply(socketChannel);
            socketChannel.close();
            return res;
        } catch (IOException e) {
            throw new CommunicationException(e);
        } catch (InterruptedException ignored) { }
        return null;
    }

}
