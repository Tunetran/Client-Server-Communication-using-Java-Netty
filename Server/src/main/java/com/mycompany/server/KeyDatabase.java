/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KeyDatabase {
    private static final String DB_URL = "jdbc:sqlite:keys.db";

    public KeyDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS keys (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pubkey TEXT,
                    aeskey TEXT,
                    iv TEXT,
                    timestamp TEXT
                )
            """);
        } catch (Exception e) {
            System.err.println("Error initializing database:");
            e.printStackTrace();
        }
    }

    public void saveKey(String pubkeyBase64, String aesKeyBase64, String ivBase64) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO keys(pubkey, aeskey, iv, timestamp) VALUES (?, ?, ?, ?)"
            );
            ps.setString(1, pubkeyBase64);
            ps.setString(2, aesKeyBase64);
            ps.setString(3, ivBase64);
            ps.setString(4, timestamp);
            ps.executeUpdate();
            System.out.println("Saved key to DB at " + timestamp);
        } catch (Exception e) {
            System.err.println("Error saving to DB:");
            e.printStackTrace();
        }
    }
}
