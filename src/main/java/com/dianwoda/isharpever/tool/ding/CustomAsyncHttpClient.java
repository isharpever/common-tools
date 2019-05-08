package com.dianwoda.isharpever.tool.ding;

import java.nio.charset.CodingErrorAction;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 异步http客户端
 *
 * @author yinxiaolin
 * @since 2018/5/29
 */
@Slf4j
public class CustomAsyncHttpClient {
    /**
     * 异步httpclient
     */
    private CloseableHttpAsyncClient asyncHttpClient;

    public CustomAsyncHttpClient(CloseableHttpAsyncClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    /**
     * 异步执行
     */
    public void execute(HttpUriRequest request, FutureCallback<HttpResponse> callback) {
        if (asyncHttpClient == null) {
            log.error("--- asyncHttpClient is null");
            return;
        }
        asyncHttpClient.execute(request, callback);
    }

    /**
     * 同步执行
     */
    public HttpResponse execute(HttpUriRequest request, long timeout, TimeUnit unit)
            throws Exception {
        if (asyncHttpClient == null) {
            log.error("--- asyncHttpClient is null");
            return null;
        }
        Future<HttpResponse> responseFuture = asyncHttpClient.execute(request, null);
        return responseFuture.get(timeout, unit);
    }

    public static class Builder {
        /**
         * 设置等待数据超时时间5秒钟 根据业务调整
         */
        private int socketTimeout = 1000;

        /**
         * 连接超时
         */
        private int connectTimeout = 1000;

        /**
         * 从连接池中获取到连接的最长时间
         */
        private int connectionRequestTimeout = 1000;

        /**
         * 连接池最大连接数
         */
        private int poolSize = 3000;

        /**
         * 每个主机的并发最多只有1500
         */
        private int maxPerRoute = 1500;

        /**
         * 是否使用代理 0:否 1:是
         */
        private int useProxy = 0;

        /**
         * http代理:host
         */
        private String proxyHost;

        /**
         * http代理:port
         */
        private Integer proxyPort;

        /**
         * 服务器认证:用户
         */
        private String username = "";

        /**
         * 服务器认证:密码
         */
        private String password = "";

        public Builder setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setConnectionRequestTimeout(int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        public Builder setPoolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder setMaxPerRoute(int maxPerRoute) {
            this.maxPerRoute = maxPerRoute;
            return this;
        }

        public Builder setUseProxy(int useProxy) {
            this.useProxy = useProxy;
            return this;
        }

        public Builder setProxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public Builder setProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Integer getSocketTimeout() {
            return socketTimeout;
        }

        public Integer getConnectTimeout() {
            return connectTimeout;
        }

        public Integer getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        public Integer getPoolSize() {
            return poolSize;
        }

        public Integer getMaxPerRoute() {
            return maxPerRoute;
        }

        public Integer getUseProxy() {
            return useProxy;
        }

        public String getProxyHost() {
            return proxyHost;
        }

        public Integer getProxyPort() {
            return proxyPort;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public CustomAsyncHttpClient build() {
            CloseableHttpAsyncClient asyncHttpClient;
            try {
                asyncHttpClient = this.createAsyncClient();
            } catch (IOReactorException e) {
                log.error("--- 创建异步httpclient异常", e);
                return null;
            }
            return new CustomAsyncHttpClient(asyncHttpClient);
        }

        /**
         * 初始化
         */
        private CloseableHttpAsyncClient createAsyncClient() throws IOReactorException {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(this.getConnectTimeout())
                    .setSocketTimeout(this.getSocketTimeout())
                    .setConnectionRequestTimeout(this.getConnectionRequestTimeout())
                    .build();

            SSLContext sslcontext = SSLContexts.createDefault();

            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                    this.getUsername(), this.getPassword());

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);

            // 设置协议http和https对应的处理socket链接工厂的对象
            Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder
                    .<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    .register("https", new SSLIOSessionStrategy(sslcontext))
                    .build();

            // 配置io线程
            IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                    .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                    .setConnectTimeout(this.getConnectTimeout())
                    .build();
            // 设置连接池大小
            ConnectingIOReactor ioReactor;
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
            PoolingNHttpClientConnectionManager conMgr = new PoolingNHttpClientConnectionManager(
                    ioReactor, null, sessionStrategyRegistry, null);

            if (this.getPoolSize() > 0) {
                conMgr.setMaxTotal(this.getPoolSize());
            }

            if (this.getMaxPerRoute() > 0) {
                conMgr.setDefaultMaxPerRoute(this.getMaxPerRoute());
            } else {
                conMgr.setDefaultMaxPerRoute(10);
            }

            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setMalformedInputAction(CodingErrorAction.IGNORE)
                    .setUnmappableInputAction(CodingErrorAction.IGNORE)
                    .setCharset(Consts.UTF_8).build();

            Lookup<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder
                    .<AuthSchemeProvider>create()
                    .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                    .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                    .register(AuthSchemes.NTLM, new NTLMSchemeFactory())
                    .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
                    .register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
                    .build();
            conMgr.setDefaultConnectionConfig(connectionConfig);

            CloseableHttpAsyncClient asyncHttpClient = null;
            if (this.getUseProxy() == 1) {
                asyncHttpClient = HttpAsyncClients.custom().setConnectionManager(conMgr)
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                        .setProxy(new HttpHost(this.getProxyHost(),
                                this.getProxyPort()))
                        .setDefaultCookieStore(new BasicCookieStore())
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            } else {
                asyncHttpClient = HttpAsyncClients.custom().setConnectionManager(conMgr)
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                        .setDefaultCookieStore(new BasicCookieStore())
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            }

            asyncHttpClient.start();
            return asyncHttpClient;
        }
    }
}
