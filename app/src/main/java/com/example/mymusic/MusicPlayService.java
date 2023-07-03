package com.example.mymusic;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MusicPlayService extends Service {
    private MediaPlayer player;
    private Timer timer;

    public MusicPlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicControl();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();//创建音乐播放器对象
        Log.i("lzs", "456  " + player);
    }

    class MusicControl extends Binder {

        //播放歌曲
        public boolean play(String str) {//String path
            try {
                Uri uri = Uri.parse(str);
                player.reset();//重置音乐播放器
                //加载多媒体文件
                player = MediaPlayer.create(getApplicationContext(), uri);
                //player.prepare();
                player.start();//播放音乐
                addTimer();//添加计时器
            } catch (Exception e) {
                e.printStackTrace();
                player = new MediaPlayer();//创建音乐播放器对象
                Toast.makeText(getApplicationContext(), "解析该歌曲失败，请换一首歌曲！", Toast.LENGTH_SHORT).show();
            }

            return player != null;

        }

        public boolean isPlay() {
            try {
                return player.isPlaying();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public boolean isLooping() {
            try {
                return player.isLooping();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;

        }

        public void setLooping(boolean b) {
            try {
                player.setLooping(b);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        public void pausePlay() {
            try {
                player.pause();//暂停播放音乐
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public void continuePlay() {
            try {
                player.start();//继续播放音乐
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        public void seekTo(int progress) {
            try {
                player.seekTo(progress);//设置音乐的播放位置
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        public void addTimer() { //添加计时器用于设置音乐播放器中的播放进度条
            try {
                if (timer == null) {
                    timer = new Timer();//创建计时器对象
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            if (player == null) return;
                            int duration = player.getDuration();//获取歌曲总时长
                            int currentPosition = player.getCurrentPosition();//获取播放进度
                            Message msg = MusicActivity.handler.obtainMessage();//创建消息对象
                            //将音乐的总时长和播放进度封装至消息对象中
                            Bundle bundle = new Bundle();
                            bundle.putInt("duration", duration);
                            bundle.putInt("currentPosition", currentPosition);
                            msg.setData(bundle);
                            //将消息发送到主线程的消息队列
                            MusicActivity.handler.sendMessage(msg);
                        }
                    };
                    //开始计时任务后的5毫秒，第一次执行task任务，以后每500毫秒执行一次
                    timer.schedule(task, 5, 500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player == null) return;
        if (player.isPlaying()) player.stop();//停止播放音乐
        player.release();                         //释放占用的资源
        player = null;                            //将player置为空
    }
}