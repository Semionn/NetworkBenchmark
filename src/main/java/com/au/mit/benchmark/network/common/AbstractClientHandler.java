package com.au.mit.benchmark.network.common;

import java.util.Arrays;

public abstract class AbstractClientHandler<T> {
    protected final T socket;
    private final AbstractServer server;

    public AbstractClientHandler(T socket, AbstractServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void handle() {
        long startTime = System.currentTimeMillis();
        processClient();
        long requestProcessingTime = System.currentTimeMillis() - startTime;
        server.updateRequestProcessingTime(requestProcessingTime);
    }

    public void handle(boolean arg) {
        long startTime = System.currentTimeMillis();
        processClient(arg);
        long requestProcessingTime = System.currentTimeMillis() - startTime;
        server.updateRequestProcessingTime(requestProcessingTime);
    }

    protected abstract void processClient();

    protected abstract void processClient(boolean arg);

    protected ProtobufMessage.Message processMessage(ProtobufMessage.Message message) {
        long startTime = System.currentTimeMillis();
        Integer[] array = message.getBodyList().toArray(new Integer[message.getMessageSize() - 1]);
        Sorter.sort(array);
        final ProtobufMessage.Message response = generateMessage(array);
        long clientProcessingTime = System.currentTimeMillis() - startTime;
        server.updateClientProcessingTime(clientProcessingTime);
        return response;
    }

    private ProtobufMessage.Message generateMessage(Integer[] array) {
        ProtobufMessage.Message.Builder builder = ProtobufMessage.Message.newBuilder()
                .setMessageSize(array.length + 1);
        builder.addAllBody(Arrays.asList(array));
        return builder.build();
    }
}
