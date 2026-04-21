package com.tianhai.warn.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

//@Configuration
public class ElasticsearchConfig {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

//    @Value("${elasticsearch.host}")
//    private String host;
//
//    @Value("${elasticsearch.port}")
//    private int port;
//
//    @Value("${elasticsearch.username}")
//    private String username;
//
//    @Value("${elasticsearch.password}")
//    private String password;
//
//    @Value("${elasticsearch.scheme}")
//    private String scheme;

//    private String host = "127.0.0.1";
private String host = "8.148.184.250";
    private int port = 9200;
    private String username = null; // 生产环境要使用https
    private String password = null;
    private String scheme = "http";
    private String caCertPathStr = "F:/ElasticSearch/elasticsearch-8.11.0/config/certs/http_ca.crt";

    // 简化业务 放弃使用ES
//    @Bean
//    public ElasticsearchClient elasticsearchClient()  {
//        // 生产环境下的https + 用户认证 + 证书验证
//        // 1 加载CA根证书（http_ca.cart）用于验证服务端身份
////        SSLContext sslContext = null;
////        try {
////            Path caCertPath = Paths.get(caCertPathStr);
////            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
////            Certificate trustedCa = certificateFactory.generateCertificate(Files.newInputStream(caCertPath));
////            KeyStore trustStore = KeyStore.getInstance("pkcs12");
////            trustStore.load(null, null);
////            trustStore.setCertificateEntry("ca", trustedCa);
////
////            sslContext = SSLContexts.custom()
////                    .loadTrustMaterial(trustStore, null)
////                    .build();
////        } catch (Exception e) {
////            log.error("加载CA证书失败，证书路径：{}", caCertPathStr, e);
////        }
////
////        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
////        credentialsProvider.setCredentials(AuthScope.ANY,
////                new UsernamePasswordCredentials(username, password));
////
////        SSLContext finalSslContext = sslContext;
////        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
////                .setHttpClientConfigCallback(httpAsyncClientBuilder -> {
////                    httpAsyncClientBuilder.setSSLContext(finalSslContext);
////                    httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
////                    httpAsyncClientBuilder.setKeepAliveStrategy(((httpResponse, httpContext) -> 30 * 1000));
////
////                    return httpAsyncClientBuilder;
////                });
////        RestClient restClient = builder.build();
////        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
////
////        return new ElasticsearchClient(transport);
//
//
//        // 简化业务 放弃使用ES
//        //开发环境下的http + 无用户认证 + 无证书验证
//        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
//                .setHttpClientConfigCallback(httpClientBuilder -> {
//            // 配置连接保持活跃
//            httpClientBuilder.setKeepAliveStrategy((response, context) -> 30 * 1000); // 30秒
//
//            return httpClientBuilder;
//        });
//
//        // 配置用户名和密码
//        if (username != null && !username.isEmpty()) {
//            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//            credentialsProvider.setCredentials(AuthScope.ANY,
//                    new UsernamePasswordCredentials(username, password));
//            builder.setHttpClientConfigCallback(httpClientBuilder ->
//                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
//        }
//
//        RestClient restClient = builder.build();
//        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
//
//        return new ElasticsearchClient(transport);
//    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳，使用字符串格式
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        // 忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
