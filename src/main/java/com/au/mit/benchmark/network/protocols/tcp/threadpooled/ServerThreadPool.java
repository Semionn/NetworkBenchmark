package com.au.mit.benchmark.network.protocols.tcp.threadpooled;

import com.au.mit.benchmark.network.common.AbstractClientHandler;
import com.au.mit.benchmark.network.common.AbstractServer;
import com.au.mit.benchmark.network.common.ProtobufMessage;
import com.au.mit.benchmark.network.common.ServerParams;
import com.au.mit.benchmark.network.common.exceptions.RepeatedServerStartException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThreadPool extends AbstractServer {
    private Logger logger = Logger.getLogger(ServerThreadPool.class.getName());
    private volatile ExecutorService threadPool = Executors.newCachedThreadPool();
    private volatile boolean closing = false;
    private Thread mainThread = null;
    private ServerSocket serverSocket = null;

    public ServerThreadPool(ServerParams params) {
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
                serverSocket = new ServerSocket(port);
                logger.log(Level.INFO, String.format("Server started at port %d", port));
                while (!Thread.interrupted()) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket, this, X);
                        threadPool.execute(handler::handle);
                    } catch (IOException e) {
                        if (!closing) {
                            logger.log(Level.WARNING, "Connection to client failed", e);
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Opening server socket failed", e);
            }
        });
        mainThread.start();
    }

    @Override
    public void stop() {
        closing = true;
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Server socket closing failed", e);
            }
        }
        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
        threadPool.shutdownNow();
    }

    private void logException(IOException e) {
        logger.log(Level.WARNING, "Exception occurred during communication with client", e);
    }

    private static class ClientHandler extends AbstractClientHandler<Socket> {
        private final ServerThreadPool server;
        private final int X;

        ClientHandler(Socket socket, ServerThreadPool server, int X) {
            super(socket, server);
            this.server = server;
            this.X = X;
        }

        @Override
        protected void processClient() {
            try {
                final DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                final DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                for (int k = 0; k < X; k++) {
                    int messageSize = inputStream.readInt();
                    byte[] messageBytes = new byte[messageSize];
                    for (int i = 0; i < messageSize; ++i) {
                        messageBytes[i] = inputStream.readByte();
                    }
                    ProtobufMessage.Message message = ProtobufMessage.Message.parseFrom(messageBytes);

                    final ProtobufMessage.Message response = processMessage(message);
                    final byte[] responseBytes = response.toByteArray();
                    outputStream.writeInt(responseBytes.length);
                    outputStream.write(responseBytes);
                }
                socket.shutdownOutput();
            } catch (IOException e) {
                server.logException(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    server.logException(e);
                }
            }
        }

        @Override
        protected void processClient(boolean arg) {

        }
    }
}
