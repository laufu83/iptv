package cn.fred.mytv;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.ui.PlayerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer mediaPlayer;
    List<ChannelInfo> channelInfos;
    private RelativeLayout transparentLayout; // 全局变量，用于控制透明层的显示/隐藏


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("HomeActivity", "onCreate");
        EdgeToEdge.enable(this);
      /*  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/
        // 设置横屏
        //  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 隐藏状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 全屏沉浸式模式
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            // 旧版本系统
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_home);
        playerView = findViewById(R.id.player_view);
        try {
            List<String> content = readIPTVData();
            channelInfos = ChannelApi.parse(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initChannelLayout();
        initMediaPlayer();
        // 设置点击监听器，单击屏幕显示/隐藏频道列表
        playerView.setOnClickListener(v -> toggleChannelList());
    }

    private void initMediaPlayer() {
        // 创建MediaPlayer实例
        mediaPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(mediaPlayer);
        // 设置新的数据源
        switchToChannel(0);
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
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            // 显示频道列表
            toggleChannelList();
            return true;
        }

        // 按返回键关闭列表
        if (keyCode == KeyEvent.KEYCODE_BACK && transparentLayout.getVisibility() == View.VISIBLE) {
            hideChannelList();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showChannelList() {
        transparentLayout.setVisibility(View.VISIBLE);
    }

    private void hideChannelList() {
        transparentLayout.setVisibility(View.GONE);
    }

    private void initChannelLayout() {
        Log.i("HomeActivity", "init Channel Layout");
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
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.4); // 宽度为屏幕30%
        params.height = getResources().getDisplayMetrics().heightPixels; // 高度为屏幕100%
        listView.setLayoutParams(params);
        // 设置ListView点击事件
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // 切换播放源
            switchToChannel(position);
            // 关闭列表
            transparentLayout.setVisibility(View.GONE);
        });
        transparentLayout.addView(listView);
        // 添加到根布局
        // RelativeLayout rootLayout = new RelativeLayout(this);
        // 添加到根布局
        View rootView = findViewById(android.R.id.content);
        ((ViewGroup) rootView.getParent()).addView(transparentLayout);
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
        Log.i("HomeActivity", "onStart");
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("HomeActivity", "onStop");
        releasePlayer();
    }


    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void switchToChannel(int index) {
        Log.i("HomeActivity", "switchToChannel " + index);
        if (index > channelInfos.size() - 1) {
            return;
        }
        String url = channelInfos.get(index).getUrl();
        // 针对 HLS 直播流，可以使用 HlsMediaSource 以获得更好的自适应支持
        // 虽然 player.setMediaItem 也可以，但显式构建 Source 更稳健
        MediaItem videoItem = MediaItem.fromUri(url);
        // 停止当前播放，准备新的源
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(videoItem);
        mediaPlayer.setMediaSource(hlsMediaSource);
        //mediaPlayer.setMediaItem(videoItem);
        mediaPlayer.prepare();
        mediaPlayer.setPlayWhenReady(true); // 自动开始播放
        Toast.makeText(this, "正在播放频道: " + channelInfos.get(index).getName(), Toast.LENGTH_SHORT).show();
    }

    private List<String> readIPTVData() throws IOException {
        Log.i("HomeActivity", "read IPTV Data");
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