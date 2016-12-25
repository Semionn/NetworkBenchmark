package com.au.mit.benchmark.network.common;

import com.au.mit.benchmark.network.NetworkBenchmark;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkResult {
    private List<Long> requestProcTime = new ArrayList<>();
    private List<Long> clientProcTime = new ArrayList<>();
    private List<Long> clientWorkTime = new ArrayList<>();
    private List<Integer> variableParamValues = new ArrayList<>();
    private NetworkBenchmark.VariableParam variableParam = null;
    private Architecture architecture = null;
    private Integer clientsCount;
    private ClientParams clientParams;
    private boolean completed = false;

    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    public Integer getClientsCount() {
        return clientsCount;
    }

    public void setClientsCount(Integer clientsCount) {
        this.clientsCount = clientsCount;
    }

    public ClientParams getClientParams() {
        return clientParams;
    }

    public void setClientParams(ClientParams clientParams) {
        this.clientParams = clientParams;
    }

    public List<Long> getRequestProcTime() {
        return requestProcTime;
    }

    public void addRequestProcTime(long value) {
        requestProcTime.add(value);
    }

    public List<Long> getClientProcTime() {
        return clientProcTime;
    }

    public void addClientProcTime(long value) {
        clientProcTime.add(value);
    }

    public List<Long> getClientWorkTime() {
        return clientWorkTime;
    }

    public void addClientWorkTime(long value) {
        clientWorkTime.add(value);
    }

    public List<Integer> getVariableParamValues() {
        return variableParamValues;
    }

    public void addVariableParamValues(int value) {
        variableParamValues.add(value);
    }

    public String getVariableParamName() {
        return variableParam.name();
    }

    public void setVariableParam(NetworkBenchmark.VariableParam variableParam) {
        this.variableParam = variableParam;
    }

    public void setCompleted() {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getParamsDescription() {
        if (clientParams == null || architecture == null || clientsCount == null || variableParam == null) {
            return "Not all params set";
        }
        StringBuilder result = new StringBuilder();
        result.append("Arch=").append(architecture.name()).append(System.lineSeparator());
        if (variableParam != NetworkBenchmark.VariableParam.Delta) {
            result.append("Delta=").append(Integer.toString(clientParams.getDelta())).append(System.lineSeparator());
        }
        if (variableParam != NetworkBenchmark.VariableParam.M) {
            result.append("M=").append(clientsCount).append(System.lineSeparator());
        }
        if (variableParam != NetworkBenchmark.VariableParam.N) {
            result.append("N=").append(clientsCount).append(System.lineSeparator());
        }
        switch (variableParam) {
            case M:
                result.append("M=");
                break;
            case N:
                result.append("N=");
                break;
            case Delta:
                result.append("Delta=");
                break;
        }
        for (Integer value: variableParamValues) {
            result.append(value).append(",");
        }
        return result.toString();
    }

}