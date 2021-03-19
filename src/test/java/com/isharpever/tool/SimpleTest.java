package com.isharpever.tool;

import com.isharpever.tool.executor.ExecutorServiceUtil;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.isharpever.tool.utils.EnDecryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.crypto.Cipher;

@Slf4j
public class SimpleTest {

    @Test
    public void testExecutorServiceUtil() throws Exception {
        ScheduledExecutorService scheduledExecutorService = ExecutorServiceUtil
                .buildScheduledThreadPool(1, "test2-");
        scheduledExecutorService.schedule(() -> {
            System.out.println("just a test2");
        }, 5, TimeUnit.SECONDS);

        ExecutorService executorService = ExecutorServiceUtil.buildExecutorService(1, "test1-");
        executorService.submit(() -> {
            System.out.println("just a test1");
        });

        synchronized (this) {
            this.wait();
        }
    }

    @Test
    public void testRSA() throws Exception {
        Map<Integer, String> keyMap = EnDecryptUtil.genKeyPair();
        RSAPublicKey publicKey = EnDecryptUtil.toRSAPublicKey(keyMap.get(0));
        RSAPrivateKey privateKey = EnDecryptUtil.toRSAPrivateKey(keyMap.get(1));

        String plainText = "hello,rsa";
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes;

        // 公钥加密
        cipherBytes = EnDecryptUtil.enDecryptWithRSA(plainBytes, publicKey, Cipher.ENCRYPT_MODE);
        log.info("公钥加密 密文={}", Base64.encodeBase64String(cipherBytes));

        // 私钥解密
        log.info("私钥解密 明文={}", new String(EnDecryptUtil.enDecryptWithRSA(cipherBytes, privateKey, Cipher.DECRYPT_MODE)));

        // 私钥加密
        cipherBytes = EnDecryptUtil.enDecryptWithRSA(plainBytes, privateKey, Cipher.ENCRYPT_MODE);
        log.info("私钥加密 密文={}", Base64.encodeBase64String(cipherBytes));

        // 公钥解密
        log.info("公钥解密 明文={}", new String(EnDecryptUtil.enDecryptWithRSA(cipherBytes, publicKey, Cipher.DECRYPT_MODE)));

        String sign;
        // 签名
        sign = EnDecryptUtil.sign(plainText, privateKey);
        log.info("私钥签名={}", sign);

        // 验签
        log.info("公钥验签={}", EnDecryptUtil.verify(plainText, publicKey, sign));
    }
}
