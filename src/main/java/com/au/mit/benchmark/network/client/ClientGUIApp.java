package com.au.mit.benchmark.network.client;

import com.au.mit.benchmark.network.NetworkBenchmark;
import com.au.mit.benchmark.network.common.Architecture;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Toggle;
import javafx.stage.Stage;

public class ClientGUIApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("client-gui.fxml"));
        Parent root = loader.load();
        componentsInitialization(root);
        primaryStage.setTitle("Benchmark client");
        primaryStage.setScene(new Scene(root, 800, 350));
        primaryStage.show();
    }

    private void componentsInitialization(Parent root) {
        final ComboBox archComboBox = (ComboBox) root.lookup("#comboBoxArchitecture");
        archComboBox.getItems().setAll(Architecture.values());
        archComboBox.getSelectionModel().select(0);

        final Toggle toggleM = (Toggle) root.lookup("#toggleM");
        toggleM.setUserData(NetworkBenchmark.VariableParam.M);

        final Toggle toggleN = (Toggle) root.lookup("#toggleN");
        toggleN.setUserData(NetworkBenchmark.VariableParam.N);
        toggleN.setSelected(true);

        final Toggle toggleDelta = (Toggle) root.lookup("#toggleDelta");
        toggleDelta.setUserData(NetworkBenchmark.VariableParam.Delta);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
