package com.isharpever.tool;

import com.alibaba.fastjson.JSON;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class CrawlerTest2 {

    @Test
    public void getUserBanlance() {
        HttpResponse<String> response = Unirest.post("https://m.qm66999.com:6524/tools/_ajax/getUserBanlance")
                .header("cookie", "abc9e5438fd2f85431b6ec5016a01483=fddfcd2d30b52cf8ec2d6efde0e70450; 8211029ccea95855ba086abfe8592d94=d71d4ba54d245d7204bd32402b969c83; 373cd5a1cfd2680766c2c8ffbcf4af1d=b8cc37bf68d30459d873984068757b8e; ca573890acbcded0e0afa1ccef8fefee=d4d4456e4c281c9009e4f84624f6624c; JSESSIONID=50AAE43C05A845AA6FC5B1A540873F0D; 0db9a3b9073d239b03c0466c19df5415=e58347373c98d97dedfb5498a957eebf; random=73216109388752060")
                .header("content-type", "application/json")
                .body("{\"userName\":\"zhou273827\",\"device\":1,\"encryKey\":\"1610792453213677752906438050\",\"encryValue\":\"f99f4b4a9176480c9d1aef30bfe94fd1\"}")
                .asString();
        log.info("response={} {}", response.getStatusText(), response.getBody());
    }

    @Test
    public void betSingle() {
        String issue = "20764700";
        BetVO betVO = new BetVO(System.currentTimeMillis(), issue, new BetItem(issue));
        String body = JSON.toJSONString(betVO);
        log.info("body={}", body);

        HttpResponse<String> response = Unirest.post("https://qm66999.com:6524/tools/_ajax/CTAUXY10/betSingle")
                .header("cookie", "abc9e5438fd2f85431b6ec5016a01483=fddfcd2d30b52cf8ec2d6efde0e70450; 8211029ccea95855ba086abfe8592d94=d71d4ba54d245d7204bd32402b969c83; 373cd5a1cfd2680766c2c8ffbcf4af1d=b8cc37bf68d30459d873984068757b8e; ca573890acbcded0e0afa1ccef8fefee=d4d4456e4c281c9009e4f84624f6624c; JSESSIONID=50AAE43C05A845AA6FC5B1A540873F0D; 0db9a3b9073d239b03c0466c19df5415=e58347373c98d97dedfb5498a957eebf; random=73216109388752060")
                .header("content-type", "application/json")
                .body(body)
                .asString();
        log.info("response={} {}", response.getStatusText(), response.getBody());
    }

    @Test
    public void test1685100() {
        HttpResponse<String> response = Unirest.get("https://1685100.com/view/aozxy10/pk10kai.html")
                .asString();
        log.info("response={}-{}\n{}", response.getStatus(), response.getStatusText(), response.getBody());


        response = Unirest.get("https://api.apiose122.com/pks/getPksHistoryList.do?lotCode=10012")
                .asString();
        log.info("response={}-{}\n{}", response.getStatus(), response.getStatusText(), response.getBody());
    }
}

@Getter @Setter
class BetVO {
    private String accountId = "404396601";
    private long clientTime;
    private String encryKey = "1610792453213677752906439050";
    private String encryValue = "f99f4b4a9176480c9d1aef30bfe94f84";
    private String gameId = "CTAUXY10";
    private String issue;
    private String[] item;

    public BetVO(long clientTime, String issue, BetItem betItem) {
        this.clientTime = clientTime;
        this.issue = issue;
        this.item = new String[] {JSON.toJSONString(betItem)};
    }
}

@Getter @Setter
class BetItem {
    private String codes = "小";
    private String issueNo;
    private String methodid = "CTBSC001001007";
    private int mode = 1;
    private int money = 1;
    private int nums = 1;
    private String rebate = "0.00";
    private int times = 1;

    public BetItem(String issueNo) {
        this.issueNo = issueNo;
    }
}