package com.smartwear.xzfit.utils.manager;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.VolumeUtils;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.MusicInfoBean;
import com.zhapp.ble.parsing.SendCmdState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备音乐管理类
 */
public class MusicSyncManager {

    private static final String TAG = MusicSyncManager.class.getSimpleName();
    private static MusicSyncManager musicSyncManager;
    //通知栏音乐相关辅助获取歌曲名称
    private static List<String> musicPackages = new ArrayList<>();
    //辅助获取音乐标题map
    private ConcurrentHashMap<String, String> mAuxiliaryMap;

    //NotificationListenerService实现类
    private NotificationListenerService mService;
    //媒体控制器会话管理类
    private MediaSessionManager mMediaSessionManager;

    //媒体控制器 - 控制器回调
    private ConcurrentHashMap<MediaController, MediaControllerCallBack> mControllerMap = null;
    //获取当前音乐信息，监听当前媒体控制器音乐变换
    private MediaController mMediaController;
    //控制上下一首，播放暂停
    private MediaController.TransportControls mMediaControllerCntrl;

    private AudioManager mAudioManager;

    //设备 0：未播放 2：播放， 3：暂停
    private int mMusicState = 0;
    //当前播放歌曲标题
    private String mMusicTitle = "";
    //是否设备主动获取
    private boolean isDeviceGet = true;
    //获取音乐音量最大值
    int mMaxVolume = 0;
    //设备是否退出音乐界面
    private boolean isDeivceQuitMusic = true;

    private Handler mHandler;
    private Runnable mDelayedRefRunnable;

    public static MusicSyncManager getInstance() {
        if (musicSyncManager == null) {
            synchronized (MusicSyncManager.class) {
                if (musicSyncManager == null) {
                    musicSyncManager = new MusicSyncManager();
                }
            }
        }
        return musicSyncManager;
    }

    private MusicSyncManager() {
        mMaxVolume = VolumeUtils.getMaxVolume(AudioManager.STREAM_MUSIC);
        //增加常用第三方音乐控制器的包名，用于辅助MediaController获取音乐信息为空的情况
        musicPackages.add("com.tencent.qqmusic");    //QQ
        musicPackages.add("com.netease.cloudmusic"); //网易云
        musicPackages.add("com.kugou.android");      //酷狗
        musicPackages.add("com.android.bbkmusic");   //Vivo I音乐
        musicPackages.add("cn.kuwo.player");         //酷我
    }

    /**
     * 激活管理类
     *
     * @param service
     */
    public void attachedNotificationListenerService(NotificationListenerService service) {
        if (service == null) {
            LogUtils.i(TAG, "NotificationListenerService = null");
            return;
        }
        mService = service;
        mMediaSessionManager = (MediaSessionManager) mService.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
    }

    public interface MusicSyncListener {
        void onPlaybackStateChanged(PlaybackState state);

        void onMetadataChanged(String title);
    }

    private MusicSyncListener musicSyncListener;

    public void setMusicListener(MusicSyncListener musicSyncListener) {
        LogUtils.i(TAG, "setMusicListener init");
        this.musicSyncListener = musicSyncListener;
        if (mService == null) {
            LogUtils.i(TAG, "NotificationListenerService  = null");
            return;
        }
        if (!isEnabled(mService)) {
            LogUtils.i(TAG, "NotificationListenerService is not start");
            return;
        }
        if (mControllerMap != null) {

            for (MediaController mediaController : mControllerMap.keySet()) {
                MediaControllerCallBack callBack = mControllerMap.get(mediaController);
                if (mediaController != null && callBack != null) {
                    mediaController.unregisterCallback(callBack);
                    mControllerMap.remove(mediaController);
                    callBack = null;
                    LogUtils.i(TAG, mediaController.getPackageName() + " -> unregisterCallback");
                }
            }
            mControllerMap.clear();
        }
        if (mMediaSessionManager == null) {
            mMediaSessionManager = (MediaSessionManager) mService.getSystemService(Context.MEDIA_SESSION_SERVICE);
        }
        mMusicState = 0;
        mMusicTitle = "";
        this.mMediaController = null;
        this.mMediaControllerCntrl = null;
        //获取所有媒体控制器
        List<MediaController> controllers = mMediaSessionManager.getActiveSessions(new ComponentName(mService, mService.getClass()));
        if (controllers.size() == 0) {
            LogUtils.i(TAG, "暂无媒体控制器存在");
            sendMusicData();
            return;
        }
        //注册所有音乐媒体控制器相关回调
        for (int i = 0; i < controllers.size(); i++) {
            LogUtils.i(TAG, "注册监听的媒体控制器 packname=" + controllers.get(i).getPackageName() + "  index=" + i);
            //注册回调，并发送获取当前的音乐信息
            setCallBack(controllers.get(i));
        }
        //有控制器没数据，可能是没有音乐数据，或者未播放
        if (mMusicState == 0 && TextUtils.equals(mMusicTitle, "")) {
            //获取最新的播放控制器
            this.mMediaController = controllers.get(0);
            this.mMediaControllerCntrl = mMediaController.getTransportControls();
            //获取标题，播放状态
            MediaMetadata metadata = mMediaController.getMetadata();
            if (metadata != null) {
                //媒体控制元数据获取
                String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                mMusicTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                //存在数据 清空辅助数据
                if (mAuxiliaryMap != null) {
                    mAuxiliaryMap.remove(mMediaController.getPackageName());
                }
            } else {
                //通知栏音乐标题辅助获取音乐名称
                auxiliaryMusicTitle(mMediaController.getPackageName());
            }
            mMusicState = 3; //改实时值
            if (mMediaController.getPlaybackState() != null) {
                int state = mMediaController.getPlaybackState().getState();
                if (state == PlaybackState.STATE_PLAYING) {
                    mMusicState = 2;
                } else {
                    mMusicState = 3;
                }
            }
            LogUtils.d(TAG, "未播放的音乐信息 --> " + mMusicTitle + "," + mMusicState);
            this.musicSyncListener.onMetadataChanged(mMusicTitle);
        }
    }

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    /**
     * 是否获取访问通知权限
     *
     * @return true
     */
    public static boolean isEnabled(Context context) {
        String pkgName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setCallBack(final MediaController mediaController) {
        if (mediaController == null) {
            return;
        }
        if (mediaController.getPlaybackState() != null &&
                mediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
            //获取当前播放控制器
            this.mMediaController = mediaController;
            this.mMediaControllerCntrl = mediaController.getTransportControls();
            //获取标题，播放状态
            MediaMetadata metadata = mediaController.getMetadata();
            if (metadata != null) {
                //媒体控制元数据获取
                String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                mMusicTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                //存在数据 清空辅助数据
                if (mAuxiliaryMap != null) {
                    mAuxiliaryMap.remove(mediaController.getPackageName());
                }
            } else {
                //通知栏音乐标题辅助获取音乐名称
                auxiliaryMusicTitle(mediaController.getPackageName());
            }
            mMusicState = 2; //改实时值
            if (mediaController.getPlaybackState() != null) {
                int state = mediaController.getPlaybackState().getState();
                if (state == PlaybackState.STATE_PLAYING) {
                    mMusicState = 2;
                } else {
                    mMusicState = 3;
                }
            }
            LogUtils.d(TAG, "播放中音乐 --> " + mMusicTitle + "," + mMusicState);
            if (this.musicSyncListener != null) {
                this.musicSyncListener.onMetadataChanged(mMusicTitle);
            }
        }
        MediaControllerCallBack callBack = new MediaControllerCallBack();
        mediaController.registerCallback(callBack);
        if (mControllerMap == null) mControllerMap = new ConcurrentHashMap<>();
        mControllerMap.put(mediaController, callBack);
    }

    /**
     * 辅助获取音乐标题
     *
     * @param packageName
     */
    private void auxiliaryMusicTitle(String packageName) {
        String aName = "";
        if (mAuxiliaryMap != null) {
            aName = mAuxiliaryMap.get(packageName);
            LogUtils.d(TAG, "auxiliaryMusicTitle =" + aName);
        }
        if (TextUtils.isEmpty(aName)) {
            //日志记录到文件
            com.smartwear.xzfit.utils.LogUtils.i(TAG, "系统当前的媒体控制器获取音乐信息失败 --->packname=" + packageName + ",Metadata==null&Notification_title==null");
        }
        if (!TextUtils.isEmpty(aName)) {
            mMusicTitle = aName;
        }
    }


    /**
     * 媒体控制器回调
     */
    class MediaControllerCallBack extends MediaController.Callback {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            LogUtils.d(TAG, "onSessionDestroyed");
            if (mControllerMap != null) {
                MediaController controller = null;
                MediaControllerCallBack callBack = null;
                for (MediaController c : mControllerMap.keySet()) {
                    callBack = mControllerMap.get(c);
                    if (callBack == this) {
                        controller = c;
                    }
                }
                if (controller == mMediaController && callBack != null) {
                    mMediaController.unregisterCallback(callBack);
                    mControllerMap.remove(controller);
                    callBack = null;
                    mMediaController = null;
                    mMediaControllerCntrl = null;
                }
            }
            //刷新音乐
            getMusicInfo();
        }

        @Override
        public void onSessionEvent(@NonNull String event, @Nullable Bundle extras) {
            super.onSessionEvent(event, extras);
            LogUtils.d(TAG, "onSessionEvent extras=" + extras);
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state == null) return;
            LogUtils.i(TAG, "onPlaybackStateChanged state=" + state.getState() + " speed=" + state.getPlaybackSpeed());
            //musicSyncListener.onPlaybackStateChanged(mediaController.getPlaybackState());
            try {
                if (mControllerMap != null) {
                    MediaController controller = null;
                    for (MediaController c : mControllerMap.keySet()) {
                        MediaControllerCallBack callBack = mControllerMap.get(c);
                        if (callBack == this) {
                            controller = c;
                        }
                    }
                    if (controller != null) {
                        if (controller == mMediaController) {
                            if (mMediaController.getPlaybackState() != null) {
                                if (mMediaController.getPlaybackState().getState() == PlaybackState.STATE_STOPPED
                                        || mMediaController.getPlaybackState().getState() == PlaybackState.STATE_PAUSED
                                        || mMediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                                    musicSyncListener.onPlaybackStateChanged(mMediaController.getPlaybackState());
                                    if (mService != null) {
                                        //当前音乐已经暂停或者停止 && 系统存在音乐播放，表示别的媒体控制器抢占当前播放通道，继续注册监听
                                        if (mMediaController.getPlaybackState().getState() == PlaybackState.STATE_STOPPED
                                                || mMediaController.getPlaybackState().getState() == PlaybackState.STATE_PAUSED) {
                                            //音乐播放器停止播放有延时
                                            mHandler.postDelayed(() -> {
                                                //部分app打开未播放也会让别的播放控制器暂停
                                                //if (isPlaying(mService)) {
                                                delayedGetMusicInfo();
                                                //}
                                            }, 500);

                                        }
                                    }
                                }
                            }
                        } else {
                            delayedGetMusicInfo();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                com.smartwear.xzfit.utils.LogUtils.e(TAG, "onPlaybackStateChanged:" + e);
            }
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                return;
            }
            if (mControllerMap != null) {
                MediaController controller = null;
                for (MediaController c : mControllerMap.keySet()) {
                    MediaControllerCallBack callBack = mControllerMap.get(c);
                    if (callBack == this) {
                        controller = c;
                    }
                }
                if (controller != null) {
                    if (controller == mMediaController) {
                        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                        LogUtils.d(TAG, "onMetadataChanged ============================== title =" + title);
                        if (title == null) {
                            return;
                        }
                        //歌曲名称切换
                        if (musicSyncListener != null) {
                            musicSyncListener.onMetadataChanged(title);
                        }
                    } else {
                        delayedGetMusicInfo();
                    }
                }
            }


        }

        @Override
        public void onQueueChanged(@Nullable List<MediaSession.QueueItem> queue) {
            super.onQueueChanged(queue);
            LogUtils.d(TAG, "onQueueChanged");
        }

        @Override
        public void onQueueTitleChanged(@Nullable CharSequence title) {
            super.onQueueTitleChanged(title);
            LogUtils.d(TAG, "onQueueTitleChanged " + title);
        }

        @Override
        public void onExtrasChanged(@Nullable Bundle extras) {
            super.onExtrasChanged(extras);
            LogUtils.d(TAG, "onExtrasChanged");
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            super.onAudioInfoChanged(info);
            LogUtils.d(TAG, "onAudioInfoChanged");
        }
    }

    public void delayedGetMusicInfo() {
        if (mDelayedRefRunnable == null) {
            mDelayedRefRunnable = () -> getMusicInfo();
        }
        mHandler.removeCallbacks(mDelayedRefRunnable);
        mHandler.postDelayed(mDelayedRefRunnable, 500);
    }


    int mCurrent = 0;

    public int getmCurrent() {
        return mCurrent;
    }

    /**
     * 设备获取音乐信息
     */
    public void getMusicInfo() {
        synchronized (MusicSyncManager.this) {
            LogUtils.i(TAG, "音乐 getMusicInfo ");
            isDeviceGet = true;
            //设置音乐变换监听
            setMusicListener(new MusicSyncListener() {
                @SuppressLint("WrongConstant")
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    //设备 2 暂停  3 播放
                    if (state != null) {
                        LogUtils.d("音乐-播放状态 Music onPlaybackStateChanged: " + state.getState());
                        int curstate;
                        //正在播放
                        if (state.getState() == PlaybackState.STATE_PLAYING) {
                            curstate = 2;
                        }
                        //未播放
                        else {
                            curstate = 3;
                        }

                        if (mMusicState != curstate) {
                            mMusicState = curstate;
                            if (TextUtils.isEmpty(mMusicTitle)) {
                                delayedGetMusicInfo();
                            } else {
                                sendMusicData();
                            }
                        }
                    }
                }

                @Override
                public void onMetadataChanged(String title) {
                    if (title == null) return;
                    LogUtils.d("音乐-歌曲名称 Music onMetadataChanged: " + title + " isDeviceGet = " + isDeviceGet);

                    //是否是设备获取的，如果是设备获取，则不用判断歌曲名时候改变。
                    if (isDeviceGet) {
                        isDeviceGet = false;
                        mMusicTitle = title;
                        sendMusicData();
                    } else {
                        if (!title.equals(mMusicTitle)) {
                            mMusicTitle = title;
                            sendMusicData();
                        }
                    }
                }
            });
        }
    }

    //上次发送至设备的数据
    private int mLastState = -1;
    private String mLastTitle = null;
    private int mLastVolume = -1;

    /**
     * 发送音乐信息
     */
    public void sendMusicData() {
        if (isDeivceQuitMusic) return;
        LogUtils.d(TAG, "发送音乐信息 sendMusicData start");

        mCurrent = VolumeUtils.getVolume(AudioManager.STREAM_MUSIC);
        //mMusicState == 0无歌曲播放 ,1 无权限，2 播放中 ,3 已暂停 , 4已停止
        MusicInfoBean musiceInfoBean = new MusicInfoBean(mMusicState, mMusicTitle, mCurrent, mMaxVolume);
        if (mService == null) {
            LogUtils.d(TAG, mService + " is null");
            musiceInfoBean.state = 0;
            musiceInfoBean.songTitle = "";
        }
        if (TextUtils.isEmpty(musiceInfoBean.songTitle)) {
            LogUtils.d(TAG, "mMusicTitle isEmpty");
            musiceInfoBean.state = 0;
            musiceInfoBean.songTitle = "";
        }
        if (TextUtils.equals(musiceInfoBean.songTitle, mLastTitle)
                && musiceInfoBean.state == mLastState
                && musiceInfoBean.currentVolume == mLastVolume) {
            LogUtils.d(TAG, "过滤重复音乐信息 = " + musiceInfoBean.toString());
            return;
        }
        mLastTitle = musiceInfoBean.songTitle;
        mLastState = musiceInfoBean.state;
        mLastVolume = musiceInfoBean.currentVolume;
        LogUtils.d(TAG, "发送音乐信息 = " + musiceInfoBean.toString());
        //发送数据给设备
        ControlBleTools.getInstance().syncMusicInfo(musiceInfoBean, new ParsingStateManager.SendCmdStateListener(null) {
            @Override
            public void onState(@NonNull SendCmdState state) {
                if (state == SendCmdState.SUCCEED) {
                    LogUtils.d("发送音乐信息成功");
                }
            }
        });
    }

    public void clearLastData() {
        mLastState = -1;
        mLastTitle = null;
        mLastVolume = -1;
    }

    /**
     * 音量控制指令
     *
     * @param context
     * @param keyCode
     */
    public boolean controlVolume(Context context, int keyCode) {
        try {
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
            }
            if (mAudioManager != null) {
                int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int step = max / 15;
                if (step < 1) step = 1;
                switch (keyCode) {
                    case AudioManager.ADJUST_RAISE:
                        if (volume + step < max) {
                            volume += step;
                        } else {
                            volume = max;
                        }
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                        break;
                    case AudioManager.ADJUST_LOWER:
                        if (volume - step > 0) {
                            volume -= step;
                        } else {
                            volume = 0;
                        }
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                        break;
                }
                if (volume == mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                    LogUtils.d("setStreamVolume volume ：" + volume);
                    return true;
                }
                LogUtils.d("setStreamVolume失效，使用adjustStreamVolume : ");
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, keyCode, AudioManager.FLAG_SHOW_UI);
                if (volume == mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                    LogUtils.d("adjustStreamVolume volume ：" + volume);
                    return true;
                }
            }
            LogUtils.d("adjustStreamVolume失效");
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deviceMusicQuit(boolean isquit) {
        isDeivceQuitMusic = isquit;
    }

    /**
     * 播放
     *
     * @return
     */
    public boolean play() {
        /*if (mMediaController != null && mMediaControllerCntrl != null) {
            mMediaControllerCntrl.play();
        }*/
        return dispatchKeyCode(mMediaController, KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    /**
     * 暂停
     *
     * @return
     */
    public boolean pause() {
        /*if (mMediaController != null && mMediaControllerCntrl != null) {
            mMediaControllerCntrl.pause();
        }*/
        return dispatchKeyCode(mMediaController, KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    /**
     * 下一首
     *
     * @return
     */
    public boolean next() {
        /*if (mMediaController != null && mMediaControllerCntrl != null) {
            mMediaControllerCntrl.skipToNext();
        }*/
        return dispatchKeyCode(mMediaController, KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    /**
     * 上一首
     *
     * @return
     */
    public boolean previous() {
        /*if (mMediaController != null && mMediaControllerCntrl != null) {
            mMediaControllerCntrl.skipToPrevious();
        }*/
        return dispatchKeyCode(mMediaController, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    /**
     * 停止
     *
     * @return
     */
    public boolean stop() {
        /*if (mMediaController != null && mMediaControllerCntrl != null) {
            mMediaControllerCntrl.stop();
        }*/
        return dispatchKeyCode(mMediaController, KeyEvent.KEYCODE_MEDIA_STOP);
    }

    /**
     * 将指定的媒体按钮事件发送到会话。只有媒体键可以通过此方法发送
     *
     * @param mController
     * @param keyCode     {@link KeyEvent#isMediaSessionKey(int)}
     * @return
     */
    private boolean dispatchKeyCode(MediaController mController, int keyCode) {
        final long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
        try {
            if (mController != null) {
                mController.dispatchMediaButtonEvent(down);
                mController.dispatchMediaButtonEvent(up);
            } else {
                if (mAudioManager == null) {
                    if (mService != null) {
                        mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
                    }
                }
                if (mAudioManager != null) {
                    mAudioManager.dispatchMediaKeyEvent(down);
                    mAudioManager.dispatchMediaKeyEvent(up);
                }
            }
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 检查播放状态
     */
    public boolean isPlaying(Context context) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioManager != null) {
            if (mAudioManager.isMusicActive()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isMusicPackageName(String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            for (String name : musicPackages) {
                if (packageName.equals(name)) {
                    return true;
                }
            }
            return packageName.contains("music");
        }
        return false;
    }

    public void notificationAncillaryMusicTitle(String packageNam, String title) {
        LogUtils.d(TAG, "notificationAncillaryMusicTitle packageNam:" + packageNam + ", title:" + title);
        if (!TextUtils.isEmpty(packageNam) && !TextUtils.isEmpty(title)) {
            if (mAuxiliaryMap == null) {
                mAuxiliaryMap = new ConcurrentHashMap<>();
            }
            mAuxiliaryMap.put(packageNam, title);

            //------------- 通知需要触发刷新
            delayedGetMusicInfo();

            /*if (mMediaController != null && TextUtils.equals(mMediaController.getPackageName(), packageNam)) {
                delayedGetMusicInfo();
            } else {
                if (mService != null && mMediaSessionManager != null) {
                    List<MediaController> controllers = mMediaSessionManager.getActiveSessions(new ComponentName(mService, mService.getClass()));
                    if (controllers.size() > 0) {
                        delayedGetMusicInfo();
                    }
                }
            }*/
        }
    }
}
