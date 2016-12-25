package com.au.mit.benchmark.network;

import com.au.mit.benchmark.network.common.AbstractServer;
import com.au.mit.benchmark.network.common.Architecture;
import com.au.mit.benchmark.network.common.ServerParams;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerBenchmarkApp {
    private static final Logger logger = Logger.getLogger(ServerBenchmarkApp.class.getName());
    private static final int DEFAULT_BENCHMARK_PORT = 8081;
    private static final int DEFAULT_SERVER_PORT = 8082;

    public static void main(String[] args) {
        final int benchmarkPort = args.length < 1 ? DEFAULT_BENCHMARK_PORT : Integer.parseInt(args[0]);
        final int serverPort = args.length < 2 ? DEFAULT_SERVER_PORT : Integer.parseInt(args[1]);
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(benchmarkPort);
                logger.log(Level.INFO, String.format("Benchmark server started at port %d", benchmarkPort));
                try {
                    Socket socket = serverSocket.accept();
                    final DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    final DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    final int restartsCount = inputStream.readInt();
                    Architecture architecture = Architecture.valueOf(inputStream.readUTF());
                    outputStream.writeBoolean(true);

                    for (int i = 0; i < restartsCount; i++) {
                        final int X = inputStream.readInt();
                        final int M = inputStream.readInt();
                        outputStream.writeBoolean(true);
                        final ServerParams serverParams = new ServerParams(serverPort, M, X);
                        final AbstractServer server = architecture.generateServer(serverParams);
                        new Thread(server::start).start();
                        while (!inputStream.readBoolean()) {
                        }
                        server.stop();
                        outputStream.writeLong(server.getRequestProcessingTime());
                        outputStream.writeLong(server.getClientProcessingTime());
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Connection to client failed", e);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Opening server socket failed", e);
            }
        }).start();
    }
}
