/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector {

    private static MetricsCollector instance = new MetricsCollector();

    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failedCount = new AtomicInteger(0);
    private Queue<String> logQueue = new ConcurrentLinkedQueue<>();

    private final AtomicInteger foundCount = new AtomicInteger(0);
    private final AtomicInteger notFoundCount = new AtomicInteger(0);

    public void incrementFound() {
        foundCount.incrementAndGet();
    }

    public void incrementNotFound() {
        notFoundCount.incrementAndGet();
    }

    public void setSubdomainStats(int found, int notFound) {
        foundCount.set(found);
        notFoundCount.set(notFound);
    }

    private MetricsCollector() {
    }

    public static MetricsCollector getInstance() {
        return instance;
    }

    public void incrementSuccess() {
        successCount.incrementAndGet();
    }

    public void incrementFailed() {
        failedCount.incrementAndGet();
    }

    public void logRequest(String msg) {
        logQueue.add(msg);
        // chỉ giữ 100 log gần nhất
        while (logQueue.size() > 100) {
            logQueue.poll();
        }
    }

    public int getFoundCount() {
        return foundCount.get();
    }

    public int getNotFoundCount() {
        return notFoundCount.get();
    }

    public String toJson() {
        StringBuilder logs = new StringBuilder("[");
        boolean first = true;
        for (String log : logQueue) {
            if (!first) {
                logs.append(",");
            }
            logs.append("\"").append(log.replace("\"", "\\\"")).append("\"");
            first = false;
        }
        logs.append("]");

        return String.format(
                "{\"success\":%d,\"failed\":%d,\"found\":%d,\"notFound\":%d,\"logs\":%s}",
                successCount.get(),
                failedCount.get(),
                foundCount.get(),
                notFoundCount.get(),
                logs.toString()
        );
    }

    public String getLogsAsJson() {
        return "[\"" + String.join("\",\"", logQueue) + "\"]";
    }

}
