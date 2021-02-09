package com.isharpever.tool;

import com.isharpever.tool.ding.CustomAsyncHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CrawlerTest {

    public static final CustomAsyncHttpClient httpClient = CustomAsyncHttpClient.createBuilder()
            .setConnectTimeout(10000)
            .setSocketTimeout(10000)
            .build();

    @Test
    public void test1685100() throws Exception {
        HttpPost post = new HttpPost("https://m.qm66999.com:6524/tools/_ajax/getUserBanlance");
        post.addHeader("cookie", "abc9e5438fd2f85431b6ec5016a01483=fddfcd2d30b52cf8ec2d6efde0e70450; 8211029ccea95855ba086abfe8592d94=d71d4ba54d245d7204bd32402b969c83; 373cd5a1cfd2680766c2c8ffbcf4af1d=b8cc37bf68d30459d873984068757b8e; ca573890acbcded0e0afa1ccef8fefee=d4d4456e4c281c9009e4f84624f6624c; JSESSIONID=50AAE43C05A845AA6FC5B1A540873F0D; 0db9a3b9073d239b03c0466c19df5415=e58347373c98d97dedfb5498a957eebf; random=73216109388752060");
        post.addHeader("content-type", "application/json");
        post.setEntity(new StringEntity("{\"userName\":\"zhou273827\",\"device\":1,\"encryKey\":\"1610792453213677752906438050\",\"encryValue\":\"f99f4b4a9176480c9d1aef30bfe94fd1\"}"));
        HttpResponse response = httpClient.execute(post, 10, TimeUnit.SECONDS);
        System.out.println(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
    }
}
