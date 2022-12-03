package com.frogmind.hypehype.hh_screen_recorder;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Objects;

public class ScreenCaptureService extends Service {
    public final static String ERROR_KEY = "error";

    private MediaRecorder m_mediaRecorder;
    private MediaProjection m_mediaProjection;
    private VirtualDisplay m_virtualDisplay;
    private int m_screenWidth = 0;
    private int m_screenHeight = 0;
    private int m_screenDensity = 0;
    private int m_mediaProjCode = 0;
    private Intent m_mediaProjData;
    private String m_outputPath = "";
    private static final String TAG = "ScreenRecordService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        System.out.println("HHRecorder: start command received successfully.");


        if(intent != null && intent.getAction() != null)
        {
            if(intent.getAction().equals("pause"))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    m_mediaRecorder.pause();
                    HhScreenRecorderPlugin._instance.onPausedRecording();
                }
            }
            else if(intent.getAction().equals("resume"))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    m_mediaRecorder.resume();
                    HhScreenRecorderPlugin._instance.onResumedRecording();
                }
            }
        }
        else
        {
            // CREATE NOTIFICATION && START FOREGROUND SERVICE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "001";
                String channelName = "RecordChannel";
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
                channel.setLightColor(Color.BLUE);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Notification notification;

                    Intent myIntent = new Intent(this, NotificationReceiver.class);
                    PendingIntent pendingIntent;

                    if (Build.VERSION.SDK_INT >= 31){
                        pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_IMMUTABLE);
                    }else{
                        pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, 0);

                    }

                    String notificationButtonText = "Button";
                    String notificationTitle = "Recording your screen.";
                    String notificationMessage = "Drag down to stop the recording.";

                    Notification.Action action = new Notification.Action.Builder(
                            Icon.createWithResource(this, android.R.drawable.presence_video_online),
                            notificationButtonText,
                            pendingIntent).build();

                    notification = new Notification.Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(R.drawable.icon).setContentTitle(notificationTitle).setContentText(notificationMessage).addAction(action).build();
                    startForeground(101, notification);
                }
            }
            else {
                startForeground(101, new Notification());
            }

            // Retrieve params
            m_screenWidth = intent.getIntExtra("width", 0);
            m_screenHeight = intent.getIntExtra("height", 0);
            if(m_screenWidth == 0 || m_screenHeight == 0)
            {
                HhScreenRecorderPlugin._instance.onFailedToStartCapture( "Screen width or height is zero!");
                return Service.START_STICKY;
            }
            m_screenDensity = intent.getIntExtra("density", 1);
            m_mediaProjCode = intent.getIntExtra("mediaProjCode", -1);
            m_mediaProjData = intent.getParcelableExtra("mediaProjData");

            String filename = intent.getStringExtra("filename");
            String directory = intent.getStringExtra("directory");
            m_outputPath = directory + "/" + filename + ".mp4";

            // INIT
            try{
                initRecorder();
            }
            catch (Exception e)
            {
                HhScreenRecorderPlugin._instance.onFailedToStartCapture( "Failed to init media recorder: " + Log.getStackTraceString(e));
                return Service.START_STICKY;
            }

            try{
                initMediaProjection();
            }
            catch (Exception e)
            {
                HhScreenRecorderPlugin._instance.onFailedToStartCapture( "Failed to init media projection: " + Log.getStackTraceString(e));
                return Service.START_STICKY;
            }

            try{
                initVirtualDisplay();
            }
            catch (Exception e)
            {
                HhScreenRecorderPlugin._instance.onFailedToStartCapture( "Failed to init virtual display: " + Log.getStackTraceString(e));
                return Service.START_STICKY;
            }

            m_mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                    HhScreenRecorderPlugin._instance.onMediaRecorderError(i, i1);
                }
            });

            m_mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {
                    HhScreenRecorderPlugin._instance.onMediaRecorderInfo(i, i1);
                }
            });

            try
            {
                m_mediaRecorder.start();
                HhScreenRecorderPlugin._instance.onStartedCapture();
            }
            catch (Exception e)
            {
                HhScreenRecorderPlugin._instance.onFailedToStartCapture("Failed to start media recorder: " + Log.getStackTraceString(e));
                return Service.START_STICKY;
            }

            HhScreenRecorderPlugin._instance.onStartedCapture();
        }

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        HhScreenRecorderPlugin._instance.onServiceDestroyed();
        stopForeground(true);
        stopSelf();

        if(m_virtualDisplay != null)
        {
            m_virtualDisplay.release();
            m_virtualDisplay = null;
        }

        if(m_mediaRecorder != null)
        {
            m_mediaRecorder.setOnErrorListener(null);
            m_mediaRecorder.reset();
        }

        if(m_mediaProjection != null)
        {
            m_mediaProjection.stop();
            m_mediaProjection = null;
        }

        super.onDestroy();
    }

    private void initRecorder() throws IOException {
        m_mediaRecorder = new MediaRecorder();
        // m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        m_mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        m_mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        m_mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        m_mediaRecorder.setVideoFrameRate(30);
        m_mediaRecorder.setVideoSize(m_screenWidth, m_screenHeight);
        m_mediaRecorder.setOutputFile(m_outputPath);
        m_mediaRecorder.prepare();
    }

    private void initMediaProjection()
    {
        m_mediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(m_mediaProjCode, m_mediaProjData);
    }

    private void initVirtualDisplay()
    {
        m_virtualDisplay = m_mediaProjection.createVirtualDisplay(TAG, m_screenWidth, m_screenHeight, m_screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, m_mediaRecorder.getSurface(), null, null);
    }
}