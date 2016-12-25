package com.au.mit.benchmark.network.protocols.tcp.asynchronous;

import com.au.mit.benchmark.network.common.AbstractClientHandler;
import com.au.mit.benchmark.network.common.AbstractServer;
import com.au.mit.benchmark.network.common.ProtobufMessage;
import com.au.mit.benchmark.network.common.ServerParams;
import com.au.mit.benchmark.network.common.exceptions.RepeatedServerStartException;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerAsynchronous extends AbstractServer {
    private static final Logger logger = Logger.getLogger(ServerAsynchronous.class.getName());
    private Thread mainThread = null;
    private AsynchronousServerSocketChannel listener;

    public ServerAsynchronous(ServerParams params) {
        super(params);
    }

    @Override
    public void start() {
        if (mainThread != null) {
            throw new RepeatedServerStartException("Stop the server before starting again.");
        }
        mainThread = new Thread(() -> {
            try {
                listener = AsynchronousServerSocketChannel.open();
                listener.bind(new InetSocketAddress(port));
                logger.log(Level.INFO, String.format("Server started at port %d", port));

                listener.accept(listener, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
                    @Override
                    public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel listener) {
                        listener.accept(listener, this);

                        ByteBuffer readBuffer = ByteBuffer.allocate(Integer.BYTES);
                        final long startTime = System.currentTimeMillis();
                        channel.read(readBuffer, readBuffer, new ClientHandler(null,
                                ServerAsynchronous.this, X, channel, startTime));
                    }

                    @Override
                    public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
                    }
                });
            } catch (IOException e) {
                logger.log(Level.WARNING, "Opening server socket failed", e);
            }
        });
        mainThread.start();
    }

    @Override
    public void stop() {
        if (listener != null) {
            try {
                listener.close();
                listener = null;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Server socket closing failed", e);
            }
        }

        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
    }
    private void logException(IOException e) {
        logger.log(Level.WARNING, "Exception occurred during communication with client", e);
    }

    private static class ClientHandler extends AbstractClientHandler implements CompletionHandler<Integer, ByteBuffer> {
        private final ServerAsynchronous server;
        private final int X;
        private final AsynchronousSocketChannel channel;
        private final long startTime;
        private volatile int totalRequestProcessingTime = 0;
        private volatile int stepNum;

        ClientHandler(Socket socket, ServerAsynchronous server, int X, AsynchronousSocketChannel channel, long startTime) {
            super(socket, server);
            this.server = server;
            this.X = X;
            this.channel = channel;
            this.startTime = startTime;
            this.stepNum = 1;
        }

        @Override
        protected void processClient() {
        }

        @Override
        protected void processClient(boolean arg) {

        }

        @Override
        public void completed(Integer result, ByteBuffer messageSizeBuffer) {
            if (messageSizeBuffer.hasRemaining()) {
                channel.read(messageSizeBuffer, messageSizeBuffer, this);
                return;
            }

            messageSizeBuffer.flip();
            final int messageSize = messageSizeBuffer.getInt();
            final ByteBuffer messageBuffer = ByteBuffer.allocate(messageSize);

            channel.read(messageBuffer, messageBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer messageBuffer) {
                    if (messageBuffer.hasRemaining()) {
                        channel.read(messageBuffer, messageBuffer, this);
                        return;
                    }

                    try {
                        messageBuffer.flip();
                        byte[] messageBytes = messageBuffer.array();
                        ProtobufMessage.Message message = ProtobufMessage.Message.parseFrom(messageBytes);

                        final ProtobufMessage.Message response = processMessage(message);

                        final byte[] responseBytes = response.toByteArray();

                        ByteBuffer writeBuffer = ByteBuffer.allocate(messageSize + Integer.BYTES);
                        writeBuffer.putInt(messageSize);
                        writeBuffer.put(responseBytes);
                        writeBuffer.flip();
                        channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer responseBuffer) {
                                if (responseBuffer.hasRemaining()) {
                                    channel.write(responseBuffer, responseBuffer, this);
                                    return;
                                }

                                if (stepNum < X) {
                                    stepNum++;
                                    messageSizeBuffer.flip();
                                    messageSizeBuffer.limit(Integer.BYTES);
                                    channel.read(messageSizeBuffer, messageSizeBuffer, ClientHandler.this);
                                } else {
                                    long requestProcessingTime = System.currentTimeMillis() - startTime;
                                    totalRequestProcessingTime += requestProcessingTime;
                                    server.setRequestProcessingTime(totalRequestProcessingTime);
                                }
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {

                            }
                        });
                    } catch (InvalidProtocolBufferException e) {
                        server.logException(e);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    server.logException(new IOException(exc));
                }
            });
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            server.logException(new IOException(exc));
        }
    }

}
