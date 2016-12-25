package com.au.mit.benchmark.network;

import com.au.mit.benchmark.network.client.BenchmarkClient;
import com.au.mit.benchmark.network.common.AbstractClient;
import com.au.mit.benchmark.network.common.Architecture;
import com.au.mit.benchmark.network.common.BenchmarkResult;
import com.au.mit.benchmark.network.common.ClientParams;
import com.au.mit.benchmark.network.common.exceptions.BenchmarkException;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkBenchmark {
    public enum VariableParam {
        M, N, Delta
    }

    private static final Logger logger = Logger.getLogger(NetworkBenchmark.class.getName());
    private static final int DEFAULT_BENCHMARK_PORT = 8081;
    private final Architecture architecture;
    private final int X;
    private int N;
    private int M;
    private int Delta;
    private final VariableParam variableParam;
    private final int paramStart, paramEnd, paramStep;
    private String serverHostname;
    private int serverPort;

    public NetworkBenchmark(Architecture architecture, int n, int x, int m, int delta, VariableParam variableParam,
                            int paramStart, int paramStep, int paramEnd, String serverHostname, int serverPort) {
        this.architecture = architecture;
        N = n;
        X = x;
        M = m;
        Delta = delta;
        this.variableParam = variableParam;
        this.paramStart = paramStart;
        this.paramStep = paramStep;
        this.paramEnd = paramEnd;
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public BenchmarkResult start() {
        final BenchmarkResult benchmarkResult = new BenchmarkResult();
        ObjectWrapper<Throwable> uncaughtEx = new ObjectWrapper<>();
        final Thread benchmarkThread = new Thread(() -> {
            try {
                final int benchmarkPort = DEFAULT_BENCHMARK_PORT;
                resetVariableParam();
                benchmarkResult.setVariableParam(variableParam);
                benchmarkResult.setClientsCount(M);
                benchmarkResult.setArchitecture(architecture);
                BenchmarkClient benchmarkClient = new BenchmarkClient(architecture, calcParamsCount());
                benchmarkClient.connect(serverHostname, benchmarkPort);
                do {
                    logger.log(Level.INFO, String.format("Parameter value: %d", getCurrentVariableParam()));
                    final ClientParams clientParams = new ClientParams(N, Delta, X);
                    benchmarkResult.setClientParams(clientParams);
                    benchmarkClient.startServer(X, M);

                    List<Thread> clientThreads = new ArrayList<>();
                    List<AbstractClient> clients = new ArrayList<>();
                    for (int i = 0; i < M; i++) {
                        final AbstractClient client = architecture.generateClient(clientParams);
                        clients.add(client);
                        final int clientNum = i;
                        final Thread clientThread = new Thread(() -> {
                            if (client.connect(serverHostname, serverPort, clientNum)) {
                                logger.info("Communication success");
                            }
                        });
                        clientThreads.add(clientThread);
                        clientThread.setUncaughtExceptionHandler((th, ex) -> {
                            uncaughtEx.setObj(ex);
                        });
                    }
                    clientThreads.forEach(Thread::start);
                    for (int i = 0; i < M; i++) {
                        clientThreads.get(i).join();
                    }
                    benchmarkClient.stopServer();
                    if (uncaughtEx.getObj() != null) {
                        break;
                    }
                    final OptionalDouble avgClientWorkTime = clients.stream().mapToLong(AbstractClient::getWorkTime).average();
                    benchmarkResult.addClientWorkTime((long) avgClientWorkTime.orElse(0));
                    benchmarkResult.addRequestProcTime(benchmarkClient.getRequestProcessingTime());
                    benchmarkResult.addClientProcTime(benchmarkClient.getClientProcessingTime());
                    benchmarkResult.addVariableParamValues(getCurrentVariableParam());
                } while (nextVariableParam());
                benchmarkResult.setCompleted();
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Benchmark was interrupted", e);
            }
        });
        benchmarkThread.setUncaughtExceptionHandler((th, ex) -> {
            uncaughtEx.setObj(ex);
        });
        benchmarkThread.start();
        int expectedCommunicationTime = ((Delta + 500) * X + 5000) * M + 60000;
        try {
            benchmarkThread.join(expectedCommunicationTime);
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "benchmark thread was interrupted");
        }
        final Throwable cause = uncaughtEx.getObj();
        if (!benchmarkResult.isCompleted() || cause != null) {
            final String message = "Benchmark results were not obtained";
            if (cause != null) {
                throw new BenchmarkException(message, cause);
            } else {
                throw new BenchmarkException(message);
            }
        }
        logger.log(Level.INFO, "Benchmarking completed");
        return benchmarkResult;
    }

    private int calcParamsCount() {
        return 1 + Math.round((float) Math.ceil((paramEnd - paramStart) * 1.0 / paramStep));
    }

    private void resetVariableParam() {
        switch (variableParam) {
            case N:
                this.N = paramStart;
                break;
            case M:
                this.M = paramStart;
                break;
            case Delta:
                this.Delta = paramStart;
                break;
        }
    }

    private int getCurrentVariableParam() {
        switch (variableParam) {
            case N:
                return this.N;
            case M:
                return this.M;
            case Delta:
                return this.Delta;
            default:
                return -1;
        }
    }

    private boolean nextVariableParam() {
        switch (variableParam) {
            case N:
                if (this.N < paramEnd) {
                    this.N += paramStep;
                    return true;
                } else {
                    return false;
                }
            case M:
                if (this.M < paramEnd) {
                    this.M += paramStep;
                    return true;
                } else {
                    return false;
                }
            case Delta:
                if (this.Delta < paramEnd) {
                    this.Delta += paramStep;
                    return true;
                } else {
                    return false;
                }
        }
        return false;
    }

    private static class ObjectWrapper<T> {
        private volatile T obj;

        private T getObj() {
            return obj;
        }

        private void setObj(T obj) {
            this.obj = obj;
        }
    }
}