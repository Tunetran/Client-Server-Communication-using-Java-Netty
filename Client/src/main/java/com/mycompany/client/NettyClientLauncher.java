/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.client;

/**
 *
 * @author VT
 */
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.function.Consumer;

public class NettyClientLauncher {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public NettyClientLauncher(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public void sendMessage(String host, int port,
                            String message,
                            Consumer<String> onResponse,
                            Consumer<Throwable> onError) {
        new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup(1);
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(new LengthFieldBasedFrameDecoder(10 * 1024 * 1024, 0, 4, 0, 4));
                                p.addLast(new LengthFieldPrepender(4));
                                p.addLast(new ByteArrayDecoder());
                                p.addLast(new ByteArrayEncoder());

                                VerifyClientHandler handler = new VerifyClientHandler(privateKey, publicKey, onResponse);
                                p.addLast(handler);

                                // Đặt message gửi khi channel active
                                handler.setMessageToSend(message.getBytes(StandardCharsets.UTF_8));
                            }
                        });

                ChannelFuture future = bootstrap.connect(host, port).sync();
                future.channel().closeFuture().sync();
            } catch (Exception ex) {
                if (onError != null) {
                    onError.accept(ex);
                }
            } finally {
                group.shutdownGracefully();
            }
        }, "NettyClientThread").start();
    }
}