/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.client;

/**
 *
 * @author VT
 */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.scene.chart.*;
import javafx.concurrent.Task;
import com.google.gson.Gson;
import java.net.*;
import java.io.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ClientUI extends Application {

    private TextField privateKeyPathField, publicKeyPathField;
    private TextArea messageInputField, logOutputArea;
    private Button sendButton;

    private File privateKeyFile, publicKeyFile;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Verify Client - UI");

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(10));

        Label privateKeyLabel = new Label("Client Private Key:");
        privateKeyPathField = new TextField("private_key_client.pem");
        privateKeyPathField.setEditable(false);
        privateKeyPathField.setPrefWidth(360);
        Button btnSelectPrivKey = new Button("Browse");
        btnSelectPrivKey.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Client Private Key");
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected != null) {
                privateKeyFile = selected;
                privateKeyPathField.setText(selected.getAbsolutePath());
            }
        });

        Label publicKeyLabel = new Label("Server Public Key:");
        publicKeyPathField = new TextField("public_key_server.pem");
        publicKeyPathField.setEditable(false);
        publicKeyPathField.setPrefWidth(360);
        Button btnSelectPubKey = new Button("Browse");
        btnSelectPubKey.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Server Public Key");
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected != null) {
                publicKeyFile = selected;
                publicKeyPathField.setText(selected.getAbsolutePath());
            }
        });

        Label messageLabel = new Label("Message:");
        messageInputField = new TextArea("<Messages>");
        messageInputField.setWrapText(true);
        messageInputField.setPrefRowCount(6);

        sendButton = new Button("Send Message");
        sendButton.setOnAction(e -> sendMessage());

        logOutputArea = new TextArea();
        logOutputArea.setWrapText(true);
        logOutputArea.setEditable(false);
        logOutputArea.setPrefRowCount(15);

        // Layout grid positions
        grid.add(privateKeyLabel, 0, 0);
        grid.add(privateKeyPathField, 1, 0);
        grid.add(btnSelectPrivKey, 2, 0);

        grid.add(publicKeyLabel, 0, 1);
        grid.add(publicKeyPathField, 1, 1);
        grid.add(btnSelectPubKey, 2, 1);

        grid.add(messageLabel, 0, 2);
        grid.add(messageInputField, 1, 2, 2, 1);

        TabPane tabPane = new TabPane();

        Tab mainTab = new Tab("Main");
        mainTab.setClosable(false);
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(10));
        mainBox.getChildren().addAll(grid, sendButton, new Label("Log Output:"), logOutputArea);
        mainTab.setContent(mainBox);

        tabPane.getTabs().add(mainTab);
        tabPane.getTabs().add(createDashboardTab());

        Scene scene = new Scene(tabPane, 700, 650);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Load default keys file at start
        privateKeyFile = new File(privateKeyPathField.getText());
        publicKeyFile = new File(publicKeyPathField.getText());
    }

    private void sendMessage() {
        if (privateKeyFile == null || !privateKeyFile.exists()) {
            log("[ERROR] Client private key file not found: " + privateKeyPathField.getText());
            return;
        }
        if (publicKeyFile == null || !publicKeyFile.exists()) {
            log("[ERROR] Server public key file not found: " + publicKeyPathField.getText());
            return;
        }

        String msg = messageInputField.getText();
        if (msg == null || msg.trim().isEmpty()) {
            log("[ERROR] Message cannot be empty");
            return;
        }

        sendButton.setDisable(true);
        log("[INFO] Sending message...");

        executor.submit(() -> {
            try {
                PrivateKey privKey = VerifyClientHandler.loadPrivateKey(privateKeyFile.getAbsolutePath());
                PublicKey pubKey = VerifyClientHandler.loadPublicKey(publicKeyFile.getAbsolutePath());

                NettyClientLauncher launcher = new NettyClientLauncher(privKey, pubKey);
                launcher.sendMessage(
                        "localhost",
                        23456,
                        msg,
                        response -> Platform.runLater(() -> {
                            log("[SERVER] - " + response);
                            sendButton.setDisable(false);
                        }),
                        error -> Platform.runLater(() -> {
                            log("[ERROR] " + error.getMessage());
                            sendButton.setDisable(false);
                        })
                );
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    log("[ERROR] Failed to load keys: " + ex.getMessage());
                    sendButton.setDisable(false);
                });
            }
        });
    }

    private void log(String message) {
        logOutputArea.appendText(message + "\n");
        logOutputArea.setScrollTop(Double.MAX_VALUE);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }

private Tab createDashboardTab() {
    Tab tab = new Tab("Dashboard");
    tab.setClosable(false);

    CategoryAxis xAxis = new CategoryAxis();
    xAxis.setLabel("Kết quả subdomain");
    xAxis.setCategories(javafx.collections.FXCollections.observableArrayList("Tồn tại", "Không tồn tại"));
    NumberAxis yAxis = new NumberAxis();
    yAxis.setLabel("Số lượng");

    BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
    barChart.setTitle("Kết quả quét subdomain (theo lần quét gần nhất)");
    barChart.setLegendVisible(false);

    XYChart.Series<String, Number> series = new XYChart.Series<>();
    XYChart.Data<String, Number> foundData = new XYChart.Data<>("Tồn tại", 0);
    XYChart.Data<String, Number> notFoundData = new XYChart.Data<>("Không tồn tại", 0);
    series.getData().addAll(foundData, notFoundData);
    barChart.getData().add(series);

    // Chú thích màu
    Label foundLegend = new Label("  ");
    foundLegend.setStyle("-fx-background-color: #2ecc40; -fx-min-width: 20px; -fx-min-height: 20px; -fx-border-color: black; -fx-border-width: 1;");
    Label foundText = new Label("Tồn tại (Found)");

    Label notFoundLegend = new Label("  ");
    notFoundLegend.setStyle("-fx-background-color: #ff4136; -fx-min-width: 20px; -fx-min-height: 20px; -fx-border-color: black; -fx-border-width: 1;");
    Label notFoundText = new Label("Không tồn tại (Not found)");

    HBox legendBox = new HBox(15, foundLegend, foundText, notFoundLegend, notFoundText);
    legendBox.setPadding(new Insets(0, 0, 10, 0));

    Runnable updateChart = () -> {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    URL url = new URL("http://localhost:4567/stats");
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    String json = in.readLine();
                    in.close();
                    Gson gson = new Gson();
                    Stats stats = gson.fromJson(json, Stats.class);

                    Platform.runLater(() -> {
                        foundData.setYValue(stats.found);
                        notFoundData.setYValue(stats.notFound);

                        // Đặt màu cho từng cột
                        if (foundData.getNode() != null) {
                            foundData.getNode().setStyle("-fx-bar-fill: #2ecc40;");
                        }
                        if (notFoundData.getNode() != null) {
                            notFoundData.getNode().setStyle("-fx-bar-fill: #ff4136;");
                        }

                        // Hiển thị số lượng trên đầu cột
                        for (XYChart.Data<String, Number> data : series.getData()) {
                            StackPane stackPane = (StackPane) data.getNode();
                            stackPane.getChildren().removeIf(n -> n instanceof Label);
                            Label label = new Label(data.getYValue().toString());
                            label.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
                            stackPane.getChildren().add(label);
                            StackPane.setAlignment(label, javafx.geometry.Pos.TOP_CENTER);
                            label.setTranslateY(-20);
                        }
                    });
                } catch (Exception ex) {
                    // Không cập nhật nếu lỗi
                }
                return null;
            }
        };
        new Thread(task).start();
    };

    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> updateChart.run()));
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();

    Button refreshBtn = new Button("Refresh");
    refreshBtn.setOnAction(e -> updateChart.run());

    VBox vbox = new VBox(legendBox, barChart, refreshBtn);
    vbox.setSpacing(15);
    vbox.setPadding(new Insets(20));
    tab.setContent(vbox);

    updateChart.run();

    tab.setOnClosed(e -> timeline.stop());

    return tab;
}
}
