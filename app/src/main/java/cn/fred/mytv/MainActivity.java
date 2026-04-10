package cn.fred.mytv;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.fred.mytv.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SurfaceView surfaceView;
    private MediaPlayer mediaPlayer;
    List<ChannelInfo> channelInfos;
    private RelativeLayout transparentLayout; // 全局变量，用于控制透明层的显示/隐藏
    private int channelPos = 0;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("appConfig", Context.MODE_PRIVATE);
        channelPos = sharedPreferences.getInt("CUR_CHANNEL_POS", 0); // 获取默认值"default_value"如果"key"不存在
        // 设置横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // 隐藏状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 全屏沉浸式模式
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            // 旧版本系统
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(binding.getRoot());
        surfaceView = binding.getRoot().findViewById(R.id.surface_view);
        try {
            List<String> content = readIPTVData();
            channelInfos = ChannelApi.parse(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initChannelLayout();
        // 设置点击监听器，单击屏幕显示/隐藏频道列表
        surfaceView.setOnClickListener(v -> toggleChannelList());
    }

    private void initMediaPlayer() {
        // 创建MediaPlayer实例
        mediaPlayer = new MediaPlayer();
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // 设置SurfaceView的holder
                mediaPlayer.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 销毁SurfaceHolder的时候记录当前的播放位置并停止播放
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            }
        });
        // 设置新的数据源
        try {
            String url = channelInfos.get(channelPos).getUrl();
            mediaPlayer.setDataSource(this, Uri.parse(url));
            mediaPlayer.prepareAsync();
            // 设置准备完成监听器
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            // 设置错误监听器
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("MediaPlayer", "Error: " + what + ", " + extra);
                //  hideProgressBar();
                return false;
            });
            // 设置缓冲更新监听器
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
                Log.d("Buffering", "Buffering: " + percent + "%");
            });
        } catch (Exception e) {
            //throw new RuntimeException(e);
            Toast.makeText(this, "线路维护中,请切换其它频道", Toast.LENGTH_SHORT).show();
        }

    }

    private void toggleChannelList() {
        if (transparentLayout.getVisibility() == View.GONE) {
            showChannelList();
        } else {
            hideChannelList();
        }
    }


    // 重写按键事件处理
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 按返回键关闭列表
        if (keyCode == KeyEvent.KEYCODE_BACK && transparentLayout.getVisibility() == View.VISIBLE) {
            hideChannelList();
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                toggleChannelList();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (channelPos > 0) {
                    channelPos--;
                    switchToChannel();

                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (channelPos < channelInfos.size() - 1) {
                    channelPos++;
                    switchToChannel();

                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showChannelList() {
        transparentLayout.setVisibility(View.VISIBLE);
        transparentLayout.getFocusedChild();
    }

    private void hideChannelList() {
        transparentLayout.setVisibility(View.GONE);
    }

    private void initChannelLayout() {
        // 创建一个透明背景的布局
        transparentLayout = new RelativeLayout(this);
        transparentLayout.setBackgroundColor(Color.argb(128, 0, 0, 0)); // 半透明黑色背景

        // 创建ListView显示频道列表
        ListView listView = new ListView(this);
        ArrayAdapter<ChannelInfo> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, channelInfos) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1); // 获取TextView以便可以自定义显示内容
                textView.setBackgroundResource(R.drawable.list_item_selector);
                ChannelInfo tv = channelInfos.get(position); // 获取当前项的数据模型对象
                if (tv != null) {
                    // 你可以在这里自定义显示的内容，例如只显示名字或者名字和年龄等。例如，这里只显示名字：
                    textView.setText(tv.getName()); // 设置TextView的文本为当前用户的名字
                }

                return view; // 返回更新后的view对象
            }

        };

        listView.setAdapter(adapter);
        // 设置ListView的布局参数
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.3); // 宽度为屏幕30%
        params.height = getResources().getDisplayMetrics().heightPixels; // 高度为屏幕100%
        listView.setLayoutParams(params);
        // 设置ListView点击事件
        listView.setOnItemClickListener((parent, view, position, id) -> {
            channelPos = position;
            // 切换播放源
            switchToChannel();
            // 关闭列表
            transparentLayout.setVisibility(View.GONE);
        });

        transparentLayout.addView(listView);
        // 添加到根布局
        // RelativeLayout rootLayout = new RelativeLayout(this);
        binding.getRoot().addView(transparentLayout);
        // 获取Activity的根布局并添加透明层
       /* View rootView = findViewById(android.R.id.content);
        ((ViewGroup) rootView.getParent()).addView(layout);*/
        // 点击背景关闭列表
        transparentLayout.setOnClickListener(v -> transparentLayout.setVisibility(View.GONE));
        // 显示透明列表
        transparentLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initMediaPlayer();
    }

    @Override
    protected void onStop() {
        // 保存最后的频道信息
        SharedPreferences sharedPreferences = getSharedPreferences("appConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("CUR_CHANNEL_POS", channelPos); // 获取默认值"default_value"如果"key"不存在
        editor.apply();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        super.onStop();
    }


    private void switchToChannel() {
        // 停止当前播放
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        try {
            if (channelPos > channelInfos.size() - 1 || channelPos < 0) {
                channelPos = 0;
            }
            String url = channelInfos.get(channelPos).getUrl();
            mediaPlayer.setDataSource(this, Uri.parse(url));
            // 异步准备播放器
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e("cn.fred.mytv", "url:" + channelInfos.get(channelPos).getUrl());
            Toast.makeText(this, "线路维护中,请切换其它频道", Toast.LENGTH_SHORT).show();
            // throw new RuntimeException(e);
        }
    }


    private List<String> readIPTVData() throws IOException {
        AssetManager assetManager = getAssets();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open("iptv.m3u")));
        List<String> list = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            list.add(line);
        }
        return list;
    }

}