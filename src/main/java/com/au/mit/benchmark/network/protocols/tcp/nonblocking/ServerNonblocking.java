package com.au.mit.benchmark.network.protocols.tcp.nonblocking;

import com.au.mit.benchmark.network.common.*;
import com.au.mit.benchmark.network.common.exceptions.*;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerNonblocking extends AbstractServer {
    private static final Logger logger = Logger.getLogger(ServerNonblocking.class.getName());
    private static final int WORKERS_COUNT = 5;
    private final ExecutorService workersPool = Executors.newFixedThreadPool(WORKERS_COUNT);
    private Selector selector;
    private final InetSocketAddress listenAddress;
    private volatile Thread mainThread;
    private volatile ServerSocketChannel serverChannel = null;

    public ServerNonblocking(ServerParams serverParams) {
        super(serverParams);
        listenAddress = new InetSocketAddress(port);
    }

    @Override
    public void start() {
        if (mainThread != null) {
            throw new RepeatedServerStartException("Stop the server before starting again.");
        }
        mainThread = new Thread(() -> {
            try {
                selector = Selector.open();
                serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);

                serverChannel.socket().bind(listenAddress);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                logger.log(Level.INFO, String.format("Server started at port %d", port));

                while (!Thread.interrupted()) {
                    if (selector.select(5) == 0) {
                        continue;
                    }
                    Iterator keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = (SelectionKey) keys.next();
                        keys.remove();

                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            process(key, true);
                        } else if (key.isWritable()) {
                            process(key, false);
                        }
                    }
                }
            } catch (IOException e) {
                throw new CommunicationException(e);
            } finally {
                try {
                    if (serverChannel != null) {
                        serverChannel.close();
                        serverChannel = null;
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Server socket closing failed", e);
                }
            }
        });
        mainThread.start();
    }

    @Override
    public void stop() {
        mainThread.interrupt();
        if (serverChannel != null) {
            try {
                serverChannel.close();
                serverChannel = null;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Server socket closing failed", e);
            }
        }
        workersPool.shutdownNow();
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();

        ClientHandler handler = new ClientHandler(socket, this, channel, workersPool);
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, handler);
    }

    private void process(SelectionKey key, boolean isReading) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.handle(isReading);
            if (handler.getStepNum() == X) {
                key.cancel();
            }
        } catch (EmptyChannelException e) {
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            logger.warning("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
        }
    }

    private void logException(IOException e) {
        logger.log(Level.WARNING, "Exception occurred during communication with client", e);
    }

    private static class ClientHandler extends AbstractClientHandler {
        private final ServerNonblocking server;
        private final SocketChannel channel;
        private final ExecutorService workersPool;
        private int stepNum = 0;

        private ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        private ByteBuffer messageBuffer = null;
        private Integer messageSize = null;
        private ProtobufMessage.Message response = null;
        private volatile boolean readyWrite = false;

        ClientHandler(Socket socket, ServerNonblocking server, SocketChannel channel, ExecutorService workersPool) {
            super(socket, server);
            this.server = server;
            this.channel = channel;
            this.workersPool = workersPool;
        }

        public int getStepNum() {
            return stepNum;
        }

        @Override
        protected void processClient() {
        }

        @Override
        protected void processClient(boolean isReading) {
            try {
                if (isReading) {
                    tryRead();
                } else {
                    tryWrite();
                }
            } catch (IOException e) {
                server.logException(e);
            }
        }

        private boolean tryRead() throws IOException {
            if (messageSize == null) {
                channel.read(sizeBuffer);
                if (sizeBuffer.hasRemaining()) {
                    return false;
                }
                sizeBuffer.flip();
                messageSize = sizeBuffer.getInt();
                messageBuffer = ByteBuffer.allocate(messageSize);
            }
            if (messageSize != null) {
                channel.read(messageBuffer);
                if (messageBuffer.hasRemaining()) {
                    return false;
                }
                workersPool.submit(() -> {
                    try {
                        messageBuffer.flip();
                        final ProtobufMessage.Message message = ProtobufMessage.Message.parseFrom(messageBuffer.array());
                        response = processMessage(message);
                        final byte[] responseBytes = response.toByteArray();
                        messageBuffer = ByteBuffer.allocate(responseBytes.length);
                        messageBuffer.put(responseBytes);
                        messageBuffer.flip();
                        sizeBuffer.clear();
                        sizeBuffer.putInt(responseBytes.length);
                        sizeBuffer.flip();
                        readyWrite = true;
                    } catch (InvalidProtocolBufferException e) {
                        server.logException(e);
                    }
                });
                return true;
            }
            return false;
        }

        private boolean tryWrite() throws IOException {
            if (!readyWrite) {
                return false;
            }
            if (sizeBuffer.hasRemaining()) {
                channel.write(sizeBuffer);
                return true;
            }
            channel.write(messageBuffer);
            boolean result = !messageBuffer.hasRemaining();
            if (result) {
                sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
                messageBuffer = null;
                messageSize = null;
                response = null;
                readyWrite = false;
                stepNum++;
            }
            return result;
        }
    }

}
