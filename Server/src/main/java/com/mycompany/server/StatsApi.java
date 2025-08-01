/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import static spark.Spark.*;


public class StatsApi {
    public static void start(MetricsCollector metricsCollector) {
        port(4567); // hoặc port bạn đang dùng
        get("/stats", (req, res) -> {
            res.type("application/json");
            return metricsCollector.toJson();
        });
    }
}