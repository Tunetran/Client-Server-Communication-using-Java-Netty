/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SubdomainScan {

    public static class ScanStats {
        public List<String> foundList;
        public int notFoundCount;

        public ScanStats(List<String> foundList, int notFoundCount) {
            this.foundList = foundList;
            this.notFoundCount = notFoundCount;
        }
    }

     public static ScanStats scan(String domain, String wordlistFile) {
        MetricsCollector.getInstance().setSubdomainStats(0, 0);

        List<String> foundList = new ArrayList<>();
        int notFoundCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(wordlistFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String subdomain = line.trim() + "." + domain;
                try {
                    InetAddress.getByName(subdomain);
                    System.out.println("Found: " + subdomain);
                    LogBroadcaster.getInstance().addLog("Found: " + subdomain);
                    foundList.add(subdomain);
                    MetricsCollector.getInstance().incrementFound();
                } catch (UnknownHostException e) {
                    System.out.println("Not found: " + subdomain);
                    LogBroadcaster.getInstance().addLog("Not found: " + subdomain);
                    notFoundCount++;
                    MetricsCollector.getInstance().incrementNotFound();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            LogBroadcaster.getInstance().addLog("Error reading wordlist: " + e.getMessage());
        }

        return new ScanStats(foundList, notFoundCount);
    }
}