package cn.fred.mytv;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoadActivity extends AppCompatActivity {
    private static final String TAG = LoadActivity.class.getName();
    // 使用不安全的 Client
    OkHttpClient unsafeClient = UnsafeOkHttpClient.createUnsafeClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_load);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        makeHttpsRequest();
    }

    private void makeHttpsRequest() {
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/laufu83/test/refs/heads/main/jx_iptv_m3u.txt") // 替换为你的测试 HTTPS 地址
                .build();


        // 使用异步请求，避免阻塞主线程
        unsafeClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response: " + responseBody);
                    // 在这里处理成功的响应，例如更新 UI
                    // 注意：此回调在后台线程，若需更新 UI，需切换到主线程
                    runOnUiThread(() -> {
                        // 更新 UI 代码
                    });
                } else {
                    Log.e(TAG, "Request failed: " + response.code());
                }
                response.close();
            }
        });
    }

    @Override
    protected void onDestroy() {
        // 释放资源
        if (unsafeClient != null) {
            unsafeClient.dispatcher().executorService().shutdown();
        }
        super.onDestroy();
    }
}