/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public class VerifyServerNetty {

    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            try {
                new VerifyServerNetty().start(23456);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // HTTP metrics API
        MetricsHttpServer.start(9090);
        StatsApi.start(MetricsCollector.getInstance());

    }

    public void start(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // Pipeline xử lý dữ liệu theo frame: length + data
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
                            p.addLast(new LengthFieldPrepender(4));
                            p.addLast(new ByteArrayDecoder());
                            p.addLast(new ByteArrayEncoder());
                            p.addLast(new ChannelTrafficShapingHandler(2000)); // 2s refresh
                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new VerifyServerHandler());
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started at port " + port);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // Handler xử lý chính (log)
    static class VerifyServerHandler extends SimpleChannelInboundHandler<byte[]> {

        private final PrivateKey serverPrivateKey;
        private final PublicKey clientPublicKey;

        public VerifyServerHandler() {
            try {
                serverPrivateKey = loadPrivateKey("private_key_server.pem");
                clientPublicKey = loadPublicKey("public_key_client.pem");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load keys", e);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
            String content = new String(msg, "UTF-8");
            String[] lines = content.split("\n");
            if (lines.length < 4) {
                ctx.writeAndFlush(("ERROR: Missing parts\n").getBytes("UTF-8"));
                return;
            }

            byte[] encryptedMessage = Base64.getDecoder().decode(lines[0]);
            byte[] signature = Base64.getDecoder().decode(lines[1]);
            byte[] encryptedAesKey = Base64.getDecoder().decode(lines[2]);
            byte[] ivBytes = Base64.getDecoder().decode(lines[3]);

            // Giải mã AES key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            // Giải mã nội dung - message
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, iv);
            byte[] rawMsg;
            try {
                rawMsg = aesCipher.doFinal(encryptedMessage);
            } catch (Exception e) {
                ctx.writeAndFlush("ERROR: AES decrypt failed\n".getBytes("UTF-8"));
                return;
            }

            // Verify signature - Xác thực chữ ký
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(clientPublicKey);
            sig.update(rawMsg);

            long timestamp = System.currentTimeMillis();
            String readableTime = formatTimestamp(timestamp);
            String remote = ctx.channel().remoteAddress().toString();

            if (sig.verify(signature)) {
                MetricsCollector.getInstance().incrementSuccess();
                MetricsCollector.getInstance().logRequest("SUCCESS from " + remote + " at " + readableTime);
                LogBroadcaster.getInstance().addLog("SUCCESS from " + remote + " at " + readableTime);

                // Quét subdomain
                SubdomainScan.ScanStats stats = SubdomainScan.scan("huflit.edu.vn", "subdomains-test.txt");

                List<String> found = stats.foundList;
                int notFoundCount = stats.notFoundCount;

                System.out.println("Scan result: " + found);
                System.out.println("Found: " + found.size() + ", Not found: " + notFoundCount);

                // Ghi vào Metrics
                MetricsCollector.getInstance().setSubdomainStats(found.size(), notFoundCount);

                // Ghi log từng cái
                for (String sub : found) {
                    LogBroadcaster.getInstance().addLog("Found: " + sub);
                }

                // Database save
                KeyDatabase db = new KeyDatabase();
                db.saveKey(
                        Base64.getEncoder().encodeToString(clientPublicKey.getEncoded()),
                        Base64.getEncoder().encodeToString(aesKeyBytes),
                        Base64.getEncoder().encodeToString(ivBytes)
                );
                LogBroadcaster.getInstance().addLog("Saved key to DB at " + timestamp);

                String result = "FOUND: " + found.toString() + "\n";
                ctx.writeAndFlush(result.getBytes("UTF-8"));
            } else {
                MetricsCollector.getInstance().incrementFailed();
                MetricsCollector.getInstance().logRequest("FAILED from " + remote + " at " + timestamp);
                ctx.writeAndFlush("VERIFICATION_FAILED\n".getBytes("UTF-8"));
                LogBroadcaster.getInstance().addLog("FAILED from " + remote + " at " + timestamp);
            }
        }
    }

    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        String key = new String(java.nio.file.Files.readAllBytes(new java.io.File(filename).toPath()));
        key = key.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public static PublicKey loadPublicKey(String filename) throws Exception {
        String key = new String(java.nio.file.Files.readAllBytes(new java.io.File(filename).toPath()));
        key = key.replaceAll("-----\\w+ PUBLIC KEY-----", "").replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    public static String formatTimestamp(long tsMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new Date(tsMillis));
    }
}
 
