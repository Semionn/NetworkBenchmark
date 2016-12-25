package com.au.mit.benchmark.network.client;

import com.au.mit.benchmark.network.NetworkBenchmark;
import com.au.mit.benchmark.network.common.Architecture;
import com.au.mit.benchmark.network.common.BenchmarkResult;
import com.au.mit.benchmark.network.common.exceptions.BenchmarkException;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GUIController {
    private static final Logger logger = Logger.getLogger(GUIController.class.getName());
    private static final int DEFAULT_SERVER_PORT = 8082;
    private static final Path resultsFolder = Paths.get("results");
    private static final String additionalResultsFilePath = resultsFolder.resolve("benchmarkResults.csv").toString();

    public TextField textM;
    public TextField textX;
    public TextField textDelta;
    public TextField textN;

    public TextField textParamStart;
    public TextField textParamStep;
    public TextField textParamEnd;
    public ToggleGroup toggleGroup;

    public HBox resultsPane;
    public TabPane resultsTabPane;
    public ComboBox comboBoxArchitecture;
    public TextField textServerIP;
    public Label textLog;

    public void start(javafx.event.ActionEvent actionEvent) {
        try {
            final int M = Integer.parseInt(textM.getText());
            final int N = Integer.parseInt(textN.getText());
            final int Delta = Integer.parseInt(textDelta.getText());
            final int X = Integer.parseInt(textX.getText());
            final int paramStart = Integer.parseInt(textParamStart.getText());
            final int paramStep = Integer.parseInt(textParamStep.getText());
            final int paramEnd = Integer.parseInt(textParamEnd.getText());
            final NetworkBenchmark.VariableParam variableParam = (NetworkBenchmark.VariableParam) toggleGroup.getSelectedToggle().getUserData();
            final Architecture architecture = (Architecture) comboBoxArchitecture.getSelectionModel().getSelectedItem();

            try {
                textLog.setText("Benchmark started...");
                final BenchmarkResult results = new NetworkBenchmark(architecture, N, X, M, Delta,
                        variableParam, paramStart, paramStep, paramEnd, textServerIP.getText(), DEFAULT_SERVER_PORT).start();
                drawCharts(results);
                resultsTabPane.getSelectionModel().select(1);
                storeResultsAdditional(results);
                storeResults(results);
            } catch (BenchmarkException e) {
                showException(e);
            }

        } catch (NumberFormatException e) {
            showNumberFormatException(e);
        }
    }

    private void storeResults(BenchmarkResult results) {
        new File(resultsFolder.toString()).mkdirs();
        final String requestFilePath = resultsFolder.resolve("RequestTime.csv").toString();
        try (PrintWriter printWriter = new PrintWriter(requestFilePath)) {
            final List<Long> requestProcTime = results.getRequestProcTime();
            for (int i = 0; i < requestProcTime.size(); i++) {
                printWriter.println(requestProcTime.get(i));
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Benchmark results weren't stored", e);
            textLog.setText("Exception occurred during saving results to the file: " + e.getMessage());
        }

        final String clientProcFilePath = resultsFolder.resolve("ClientProcessingTime.csv").toString();
        try (PrintWriter printWriter = new PrintWriter(clientProcFilePath)) {
            final List<Long> clientProcTime = results.getClientProcTime();
            for (int i = 0; i < clientProcTime.size(); i++) {
                printWriter.println(clientProcTime.get(i));
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Benchmark results weren't stored", e);
            textLog.setText("Exception occurred during saving results to the file: " + e.getMessage());
        }

        final String clientWorkFilePath = resultsFolder.resolve("ClientWorkTime.csv").toString();
        try (PrintWriter printWriter = new PrintWriter(clientWorkFilePath)) {
            final List<Long> clientWorkTime = results.getClientWorkTime();
            for (int i = 0; i < clientWorkTime.size(); i++) {
                printWriter.println(clientWorkTime.get(i));
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Benchmark results weren't stored", e);
            textLog.setText("Exception occurred during saving results to the file: " + e.getMessage());
        }

        final String paramsFilePath = resultsFolder.resolve("ParamsValues.csv").toString();
        try (PrintWriter printWriter = new PrintWriter(paramsFilePath)) {
            printWriter.print(results.getParamsDescription());
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Benchmark results weren't stored", e);
            textLog.setText("Exception occurred during saving results to the file: " + e.getMessage());
        }
    }

    private void showNumberFormatException(NumberFormatException e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Warning!");
        alert.setHeaderText(null);
        alert.setContentText(String.format("Wrong number format: %s", e.getMessage()));
        alert.showAndWait();
    }

    private void showException(BenchmarkException e) {
        logger.log(Level.WARNING, "Something gone wrong during the benchmarking", e);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Warning!");
        alert.setHeaderText(null);
        String exceptionMessage = e.getMessage();
        Exception curException = (Exception) e.getCause();
        while (curException != null) {
            exceptionMessage += ": " + curException.getMessage();
            curException = (Exception) curException.getCause();
        }
        alert.setContentText(exceptionMessage);
        alert.showAndWait();
        textLog.setText("Status: not started");
    }

    private void storeResultsAdditional(BenchmarkResult results) {
        new File(resultsFolder.toString()).mkdirs();
        try (PrintWriter printWriter = new PrintWriter(additionalResultsFilePath)) {
            printWriter.println("ParamType=" + results.getVariableParamName());
            printWriter.println("ParamValue;RequestTime;ClientProcTime;ClientWorkTime;");
            final List<Integer> paramValues = results.getVariableParamValues();
            final List<Long> requestProcTime = results.getRequestProcTime();
            final List<Long> clientProcTime = results.getClientProcTime();
            final List<Long> clientWorkTime = results.getClientWorkTime();
            for (int i = 0; i < paramValues.size(); i++) {
                final StringBuilder builder = new StringBuilder();
                builder.append(paramValues.get(i));
                builder.append(";");
                if (i < requestProcTime.size()) {
                    builder.append(requestProcTime.get(i));
                    builder.append(";");
                }
                if (i < clientProcTime.size()) {
                    builder.append(clientProcTime.get(i));
                    builder.append(";");
                }
                if (i < clientWorkTime.size()) {
                    builder.append(clientWorkTime.get(i));
                    builder.append(";");
                }
                printWriter.println(builder.toString());
            }
            textLog.setText("Benchmark results stored in the '" + additionalResultsFilePath + "' file");
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Benchmark results weren't stored", e);
            textLog.setText("Exception occurred during saving results to the file: " + e.getMessage());
        }
    }

    private void drawCharts(BenchmarkResult results) {
        resultsPane.getChildren().clear();
        final String paramName = results.getVariableParamName();
        final List<Integer> paramValues = results.getVariableParamValues();
        drawChart(results.getRequestProcTime(), "Request processing time", paramName, paramValues);
        drawChart(results.getClientProcTime(), "Client processing time", paramName, paramValues);
        drawChart(results.getClientWorkTime(), "Client work time", paramName, paramValues);
    }

    private void drawChart(List<Long> data, String title, String xAxisLabel, List<Integer> paramValues) {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Time, ms");
        xAxis.setLabel(xAxisLabel);
        final LineChart<Number,Number> lineChart = new LineChart<>(xAxis,yAxis);
        lineChart.setTitle(title);
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Measurements");
        for (int i = 0; i < data.size(); i++) {
            series.getData().add(new XYChart.Data<>(paramValues.get(i), data.get(i)));
        }
        lineChart.getData().add(series);
        resultsPane.getChildren().add(lineChart);
    }
}