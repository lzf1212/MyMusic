package com.example.mymusic;


import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MusicActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Test";
    public static int icons = R.drawable.iuyuantu;
    private ImageView btn_music;
    RecyclerView musicRv;
    //数据源
    private List<LocalMusicBean> mDatas;
    private LocalMusicAdapter adapter;
    private MediaPlayer mediaPlayer;
    public int id = -1;
    private int num = -2;
    private TextView t1, t2;
    private MusicPlayService.MusicControl musicControl;
    MyServiceConn conn;
    Intent intent;
    private boolean isUnbind = false;//记录服务是否被解绑


    private int mStartY = 0;
    private int mDiffY = 0;
    private int mDiffY2 = 0;
    private VelocityTracker mVelocityTracker;
    private int initialVelocity;

    private PopupWindow mPop;

    private static SeekBar sb;
    private ObjectAnimator animator;
    private ImageView iv_music;
    private static TextView tv_progress, tv_total, song_name, songer_name;
    private ImageView play, last, next, pull_down, btn_xuhuang;
    private final int mRequestCode = 100;
    //需要申请两个权限，SD读写
    //1、首先声明一个数组permissions，将需要的权限都放在里面
    String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //2、创建一个mPermissionList，逐个判断哪些权限未授予，未授予的权限存储到mPerrrmissionList中
    List<String> mPermissionlist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_list);

        //一般情况下在触发某个事件的时候再请求动态权限，比如点击事件！
        if (Build.VERSION.SDK_INT >= 23) {//6.0才用动态权限
            initPermission();
        }

        intent = new Intent(this, MusicPlayService.class);//创建意图对象
        conn = new MyServiceConn();                       //创建服务连接对象
        bindService(intent, conn, BIND_AUTO_CREATE);  //绑定服务
        // 可能初始化一个新实例的默认值的成员
        musicRv = findViewById(R.id.local_music_rv1);
        mediaPlayer = new MediaPlayer();
        mDatas = new ArrayList<>();
        t1 = findViewById(R.id.local_music_bottom_tv_song);
        t2 = findViewById(R.id.local_music_bottom_tv_singer);
        btn_music = findViewById(R.id.local_music_icon);
        btn_music.setOnClickListener(this);
        //创建适配器对象
        adapter = new LocalMusicAdapter(MusicActivity.this, mDatas);
        musicRv.setAdapter(adapter);
        //设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(MusicActivity.this, LinearLayoutManager.VERTICAL, false);
        musicRv.setLayoutManager(layoutManager);
        //加载本地数据源
        loadLocalMusicData();
        //设置每一项的点击事件
        setEventListener();


    }


    //权限判断和申请
    private void initPermission() {
        mPermissionlist.clear();//清空没有通过的权限

        //逐个判断你要的权限是否已经通过
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionlist.add(permissions[i]);//添加还未授予的权限
            }
        }

        //申请权限
        if (mPermissionlist.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, permissions, mRequestCode);
        }

    }

    //请求权限后回调的方法
    //参数： requestCode  是我们自己定义的权限请求码
    //参数： permissions  是我们请求的权限名称数组
    //参数： grantResults 是我们在弹出页面后是否允许权限的标识数组，数组的长度对应的是权限名称数组的长度，数组的数据0表示允许权限，-1表示我们点击了禁止权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (mRequestCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                showPermissionDialog();//跳转到系统设置权限页面，或者直接关闭页面，不让他继续访问
            }
        }
    }

    /**
     * 不再提示权限时的展示对话框
     */
    AlertDialog mPermissionDialog;

    private void showPermissionDialog() {
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(this)
                    .setMessage("已禁用权限，请手动授予")
                    .setCancelable(false)
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();

                            //跳转到设置权限界面
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", MusicActivity.this.getPackageName(), null);
                            intent.setData(uri);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关闭页面或者做其他操作
                            finish();
                        }
                    })
                    .create();
        }
        mPermissionDialog.show();
    }

    //关闭对话框
    private void cancelPermissionDialog() {
        mPermissionDialog.cancel();
    }


    @SuppressLint({"ClickableViewAccessibility", "SuspiciousIndentation"})
    private void initPopupWindow() {
        mPop = new PopupWindow();
        mPop.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        mPop.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        mPop.setBackgroundDrawable(null);
        View mContentView = LayoutInflater.from(this).inflate(R.layout.touchable_pop, null);
        ConstraintLayout mIvTop = mContentView.findViewById(R.id.ct_ly);
        mPop.setAnimationStyle(R.style.popwin_anim_style);
        mPop.setContentView(mContentView);
        mPop.setClippingEnabled(false);
        mPop.setFocusable(true);
        mPop.showAtLocation(
                getWindow().getDecorView(), Gravity.BOTTOM, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);

        play = mContentView.findViewById(R.id.btn_play);
        last = mContentView.findViewById(R.id.btn_last);
        next = mContentView.findViewById(R.id.btn_next);
        song_name = mContentView.findViewById(R.id.song_name);
        songer_name = mContentView.findViewById(R.id.songer_name);
        btn_xuhuang = mContentView.findViewById(R.id.btn_xuhuang);

        tv_progress = mContentView.findViewById(R.id.tv_progress);
        tv_total = mContentView.findViewById(R.id.tv_total);
        pull_down = mContentView.findViewById(R.id.pull_down);

        iv_music = mContentView.findViewById(R.id.iv_music);
        iv_music.setImageResource(MusicActivity.icons); //设置专辑图片

        animator = ObjectAnimator.ofFloat(iv_music, "rotation", 0f, 360.0f);
        animator.setDuration(10000);//动画旋转一周的时间为10秒
        animator.setInterpolator(new LinearInterpolator());//匀速
        animator.setRepeatCount(-1);//-1表示设置动画无限循环;
        musicControl.setLooping(false);
        if (musicControl.isPlay()) {
            play.setImageResource(R.drawable.play1);
            song_name.setText(mDatas.get(id).getSong());
            songer_name.setText(mDatas.get(id).getSinger());
            animator.start();
        } else {
            animator.pause();
        }

        btn_xuhuang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicControl.isLooping()) {
                    btn_xuhuang.setImageResource(R.drawable.bofan1);
                    musicControl.setLooping(false);
                } else {
                    btn_xuhuang.setImageResource(R.drawable.single);
                    musicControl.setLooping(true);
                }
            }
        });

        pull_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPop.dismiss();
                animator.pause();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (id == -1) {
                    if (num >= 0) {
                        id = 0;
                        play.setImageResource(R.drawable.play1);
                        song_name.setText(mDatas.get(id).getSong());
                        songer_name.setText(mDatas.get(id).getSinger());
                        t1.setText(mDatas.get(id).getSong());
                        t2.setText(mDatas.get(id).getSinger());
                        musicControl.play(mDatas.get(id).getPath());
                        animator.start();


                    } else {
                        Toast.makeText(MusicActivity.this, "没有可播放的歌曲！", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    if (musicControl.isPlay()) {
                        play.setImageResource(R.drawable.pause1);
                        musicControl.pausePlay();
                        animator.pause();
                    } else {
                        play.setImageResource(R.drawable.play1);
                        musicControl.continuePlay();
                        if (animator.isPaused())
                            animator.resume();
                        else {
                            animator.start();
                        }

                    }
                }
            }
        });

        last.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (id == 0) {
                    Toast.makeText(MusicActivity.this, "已经是第一首了，没有上一曲！", Toast.LENGTH_SHORT).show();
                } else if (id < 0) {
                    Toast.makeText(MusicActivity.this, "没有可播放的歌曲！", Toast.LENGTH_SHORT).show();
                } else {
                    if (musicControl.isPlay()) {
                        animator.pause();
                        animator.clone();
                        id--;
                        song_name.setText(mDatas.get(id).getSong());
                        songer_name.setText(mDatas.get(id).getSinger());
                        t1.setText(mDatas.get(id).getSong());
                        t2.setText(mDatas.get(id).getSinger());
                        musicControl.play(mDatas.get(id).getPath());
                        animator.start();
                    } else {
                        animator.pause();
                        animator.clone();
                        play.setImageResource(R.drawable.play1);
                        id--;
                        song_name.setText(mDatas.get(id).getSong());
                        songer_name.setText(mDatas.get(id).getSinger());
                        t1.setText(mDatas.get(id).getSong());
                        t2.setText(mDatas.get(id).getSinger());
                        musicControl.play(mDatas.get(id).getPath());
                        animator.start();

                    }
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (id >= 0 && id >= num) {
                    Toast.makeText(MusicActivity.this, "已经是最后一首了，没有下一曲！", Toast.LENGTH_SHORT).show();
                } else if (id < 0) {
                    Toast.makeText(MusicActivity.this, "没有可播放的歌曲！", Toast.LENGTH_SHORT).show();
                } else {
                    if (musicControl.isPlay()) {
                        animator.pause();
                        animator.clone();
                        id++;
                        song_name.setText(mDatas.get(id).getSong());
                        songer_name.setText(mDatas.get(id).getSinger());
                        t1.setText(mDatas.get(id).getSong());
                        t2.setText(mDatas.get(id).getSinger());
                        musicControl.play(mDatas.get(id).getPath());
                        animator.start();
                    } else {
                        animator.pause();
                        animator.clone();
                        id++;
                        song_name.setText(mDatas.get(id).getSong());
                        songer_name.setText(mDatas.get(id).getSinger());
                        t1.setText(mDatas.get(id).getSong());
                        t2.setText(mDatas.get(id).getSinger());
                        play.setImageResource(R.drawable.play1);
                        musicControl.play(mDatas.get(id).getPath());
                        animator.start();
                    }
                }
            }
        });

        sb = mContentView.findViewById(R.id.sb);
        //为滑动条添加事件监听
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //进度条改变时，会调用此方法
                if (progress == seekBar.getMax()) {//当滑动条到末端时，结束动画
                    animator.pause();//停止播放动画
                }

                if (progress == seekBar.getMax() && !musicControl.isLooping()) {
                    animator.start();
                    Log.e("Test", id + " ======== " + num);
                    if (id >= 0 && id >= num) {
                        id = 0;
                        play.setImageResource(R.drawable.play1);
                        song_name.setText(mDatas.get(id).getSong());
                        songer_name.setText(mDatas.get(id).getSinger());
                        t1.setText(mDatas.get(id).getSong());
                        t2.setText(mDatas.get(id).getSinger());
                        musicControl.play(mDatas.get(id).getPath());
                    } else if (id < 0) {
                        Toast.makeText(MusicActivity.this, "没有可播放的歌曲！", Toast.LENGTH_SHORT).show();
                    } else if (id != 0) {
                        if (musicControl.isPlay()) {
                            id++;
                            song_name.setText(mDatas.get(id).getSong());
                            songer_name.setText(mDatas.get(id).getSinger());
                            t1.setText(mDatas.get(id).getSong());
                            t2.setText(mDatas.get(id).getSinger());
                            musicControl.play(mDatas.get(id).getPath());

                        } else {
                            id++;
                            song_name.setText(mDatas.get(id).getSong());
                            songer_name.setText(mDatas.get(id).getSinger());
                            t1.setText(mDatas.get(id).getSong());
                            t2.setText(mDatas.get(id).getSinger());
                            play.setImageResource(R.drawable.play1);
                            musicControl.play(mDatas.get(id).getPath());

                        }
                    }
                } else if (progress == seekBar.getMax() && musicControl.isPlay()) {
                    play.setImageResource(R.drawable.play1);
                    song_name.setText(mDatas.get(id).getSong());
                    songer_name.setText(mDatas.get(id).getSinger());
                    t1.setText(mDatas.get(id).getSong());
                    t2.setText(mDatas.get(id).getSinger());
                    animator.start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {//滑动条开始滑动时调用
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {//滑动条停止滑动时调用
                //根据拖动的进度改变音乐播放进度
                int progress = seekBar.getProgress();//获取seekBar的进度
                musicControl.seekTo(progress);//改变播放进度
            }
        });

        mIvTop.setOnTouchListener((v, event) -> {
            int sign = event.getAction();

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(event);

            switch (sign) {
                case MotionEvent.ACTION_DOWN:
                    mStartY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mDiffY = (int) event.getRawY() - mStartY;
                    mDiffY2 = mDiffY;


                    // 限制方向
                    if (mDiffY < 0) mDiffY = 0;
                    mPop.update(0, -mDiffY, -1, -1, true);
                    break;
                case MotionEvent.ACTION_UP:
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, ViewConfiguration.get(MusicActivity.this).getScaledMaximumFlingVelocity());
                    initialVelocity = Math.abs((int) velocityTracker.getYVelocity());

                    Log.e("Test", (int) event.getRawY() + "");
                    Log.e("Test", mStartY + "");
                    Log.e("Test", Math.abs((int) event.getRawY() - mStartY) + "");
                    Log.e("Test", mDiffY2 + "aaaaa");
                    Log.e("Test", initialVelocity + "fffff");
                    // click
                    if (Math.abs((int) event.getRawY() - mStartY) < 1000 && initialVelocity < 100) {
                        Toast.makeText(MusicActivity.this, "clicked", Toast.LENGTH_SHORT).show();
                        mPop.update(0, 0, -1, -1, true);
                    } else {
                        if (mDiffY2 > 0)
                            mPop.dismiss();
                    }
                    break;
            }
            return true;
        });
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadLocalMusicData() {
        /* 加载本地存储当中的音乐mp3文件到集合当中*/
        //1.获取ContentResolver对象
        ContentResolver resolver = MusicActivity.this.getContentResolver();
        //2.获取本地音乐存储的Uri地址
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(TAG, "loadLocalMusicData: " + uri);
        //3 开始查询地址
        Cursor cursor = resolver.query(uri, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        Log.i(TAG, "loadLocalMusicData: " + cursor);
        //4.遍历Cursor
        int id = 0;
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            @SuppressLint("Range") String type = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
            Log.i(TAG, "loadLocalMusicData:123   " + type);

            @SuppressLint("Range") String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            @SuppressLint("Range") String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            id++;
            String sid = String.valueOf(id);
            @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            Log.i(TAG, "loadLocalMusicData:456   " + path);
            @SuppressLint("Range") long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            String time = sdf.format(new Date(duration));
            //将一行当中的数据封装到对象当中
            LocalMusicBean bean = new LocalMusicBean(sid, song, singer, album, time, path, "");
            mDatas.add(bean);
        }
        num = id > 0 ? id - 1 : -2;
        Log.i(TAG, "loadLocalMusicData: " + mDatas);
        //数据源变化，提示适配器更新
        adapter.notifyDataSetChanged();
    }


    private void setEventListener() {
        /* 设置每一项的点击事件*/
        adapter.setOnItemClickListener(new LocalMusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                id = position;
                t1.setText(mDatas.get(position).getSong());
                t2.setText(mDatas.get(position).getSinger());
                musicControl.play(mDatas.get(id).getPath());
                initPopupWindow();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.local_music_icon) {
            initPopupWindow();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbind(isUnbind); //解绑服务
    }


    class MyServiceConn implements ServiceConnection { //用于实现连接服务
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicControl = (MusicPlayService.MusicControl) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    private void unbind(boolean isUnbind) {
        if (!isUnbind) {                  //判断服务是否被解绑
            musicControl.pausePlay();//暂停播放音乐
            unbindService(conn);      //解绑服务
            stopService(intent);      //停止服务
        }
    }

    @SuppressLint("HandlerLeak")
    public static Handler handler = new Handler() {//创建消息处理器对象
        //在主线程中处理从子线程发送过来的消息
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();//获取从子线程发送过来的音乐播放进度
            int duration = bundle.getInt("duration");
            int currentPosition = bundle.getInt("currentPosition");
            sb.setMax(duration);
            sb.setProgress(currentPosition);
            //歌曲总时长
            int minute = duration / 1000 / 60;
            int second = duration / 1000 % 60;
            String strMinute = null;
            String strSecond = null;
            if (minute < 10) {//如果歌曲的时间中的分钟小于10
                strMinute = "0" + minute;//在分钟的前面加一个0
            } else {
                strMinute = minute + "";
            }
            if (second < 10) {//如果歌曲中的秒钟小于10
                strSecond = "0" + second;//在秒钟前面加一个0
            } else {
                strSecond = second + "";
            }
            tv_total.setText(strMinute + ":" + strSecond);
            //歌曲当前播放时长
            minute = currentPosition / 1000 / 60;
            second = currentPosition / 1000 % 60;
            if (minute < 10) {//如果歌曲的时间中的分钟小于10
                strMinute = "0" + minute;//在分钟的前面加一个0
            } else {
                strMinute = minute + " ";
            }
            if (second < 10) {//如果歌曲中的秒钟小于10
                strSecond = "0" + second;//在秒钟前面加一个0
            } else {
                strSecond = second + " ";
            }
            tv_progress.setText(strMinute + ":" + strSecond);
        }
    };


}



