/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;

public class LogBroadcaster {

    private static LogBroadcaster instance = new LogBroadcaster();

    // Lưu log message (giới hạn size)
    private static final int MAX_LOG_SIZE = 500;
    private final CircularBuffer<String> logs = new CircularBuffer<>(MAX_LOG_SIZE);

    // Các channel WebSocket client đang kết nối
    private final Set<Channel> subscribers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static LogBroadcaster getInstance() {
        return instance;
    }

    public void addLog(String log) {
        logs.add(log);
        broadcast(log);
    }

    public void addSubscriber(Channel ch) {
        subscribers.add(ch);
    }

    public void removeSubscriber(Channel ch) {
        subscribers.remove(ch);
    }

    public String[] getRecentLogs() {
        return logs.toArray(new String[0]);
    }

    private void broadcast(String msg) {
        for (Channel ch : subscribers) {
            if (ch.isActive()) {
                ch.writeAndFlush(msg + "\n");
            } else {
                subscribers.remove(ch);
            }
        }
    }

    public String getLogsAsJson() {
        synchronized (logs) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (String log : logs) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("\"").append(log.replace("\"", "\\\"")).append("\"");
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
    }

}

