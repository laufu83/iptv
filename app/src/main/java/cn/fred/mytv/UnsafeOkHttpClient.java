package cn.fred.mytv;

import okhttp3.OkHttpClient;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class UnsafeOkHttpClient {

    /**
     * 创建一个信任所有证书的 OkHttpClient
     * @return 配置好的 OkHttpClient 实例
     */
    public static OkHttpClient createUnsafeClient() {
        try {
            // 1. 创建一个信任所有证书的 TrustManager
            X509TrustManager trustAllCerts = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // 信任所有客户端证书
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // 信任所有服务端证书
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    // 返回空数组，表示接受所有证书颁发机构
                    return new X509Certificate[]{};
                }
            };
            // 2. 创建一个 SSLContext，并设置自定义的 TrustManager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{trustAllCerts}, new java.security.SecureRandom());
            // 3. 创建一个 HostnameVerifier，用于验证主机名
            // 这里设置为始终返回 true，即信任所有主机名
            HostnameVerifier hostnameVerifier = (hostname, session) -> true;
            // 4. 使用 SSLContext 和 HostnameVerifier 构建 OkHttpClient
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时 30 秒
                    .readTimeout(30, TimeUnit.SECONDS)     // 读取超时 30 秒
                    .writeTimeout(30, TimeUnit.SECONDS)    // 写入超时 30 秒
                    .sslSocketFactory(sslContext.getSocketFactory(), trustAllCerts)
                    .hostnameVerifier(hostnameVerifier)
                    .build();

        } catch (Exception e) {
            // 如果发生任何错误，抛出运行时异常
            throw new RuntimeException(e);
        }
    }
}