/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.client;

/**
 *
 * @author VT
 */
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.function.Consumer;

@Sharable
public class VerifyClientHandler extends SimpleChannelInboundHandler<byte[]> {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Consumer<String> onResponse;

    private byte[] messageToSend;

    public VerifyClientHandler(PrivateKey privateKey,
                              PublicKey publicKey,
                              Consumer<String> onResponse) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.onResponse = onResponse;
    }

    public void setMessageToSend(byte[] message) {
        this.messageToSend = message;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (messageToSend == null)
            throw new IllegalStateException("Message to send is not set.");

        // Sign message
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(messageToSend);
        byte[] signatureBytes = sig.sign();

        // Generate AES key and IV
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        byte[] ivBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // Encrypt message using AES CBC
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] encryptedMessage = aesCipher.doFinal(messageToSend);

        // Encrypt AES key with server's RSA public key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        // Compose payload Base64 parts separated by \n
        String payload = Base64.getEncoder().encodeToString(encryptedMessage) + "\n"
                + Base64.getEncoder().encodeToString(signatureBytes) + "\n"
                + Base64.getEncoder().encodeToString(encryptedAesKey) + "\n"
                + Base64.getEncoder().encodeToString(ivBytes) + "\n";

        ctx.writeAndFlush(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        String resp = new String(msg, StandardCharsets.UTF_8);
        if (onResponse != null) onResponse.accept(resp);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (onResponse != null) onResponse.accept("Exception: " + cause.getMessage());
        ctx.close();
    }

    // Static helper methods to load keys from PEM files
    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        String key = new String(java.nio.file.Files.readAllBytes(new java.io.File(filename).toPath()));
        key = key.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        return java.security.KeyFactory.getInstance("RSA").generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(decoded));
    }

    public static PublicKey loadPublicKey(String filename) throws Exception {
        String key = new String(java.nio.file.Files.readAllBytes(new java.io.File(filename).toPath()));
        key = key.replaceAll("-----\\w+ PUBLIC KEY-----", "").replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        return java.security.KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(decoded));
    }
}

