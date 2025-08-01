/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import reactor.netty.http.server.HttpServer;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Paths;

public class MetricsHttpServer {

    private static final String INDEX_HTML_PATH = "D:\\Server\\src\\main\\java\\dasboard\\index.html";

    public static void start(int port) {
        HttpServer.create()
            .port(port)
            .route(routes -> routes
                .get("/metrics", (req, res) ->
                    res.header("Content-Type", "application/json")
                       .sendString(Mono.just(MetricsCollector.getInstance().toJson())))
                .get("/log", (req, res) ->
                    res.header("Content-Type", "application/json")
                       .sendString(Mono.just(LogBroadcaster.getInstance().getLogsAsJson())))
                .get("/ui", (req, res) -> {
                    try {
                        byte[] bytes = Files.readAllBytes(Paths.get(INDEX_HTML_PATH));
                        String content = new String(bytes);
                        return res.header("Content-Type", "text/html; charset=UTF-8")
                                  .sendString(Mono.just(content));
                    } catch (Exception e) {
                        return res.status(500).send();
                    }
                })
            )
            .bindNow();

        System.out.println("Metrics HTTP server started on port " + port);
    }
}
