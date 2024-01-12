package com.ie.hh_screen_recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//import androidx.core.content.FileProvider;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ScreenCaptureService extends Service {
    public static ScreenCaptureService activeService = null;
    public final static String ERROR_KEY = "error";
    private static final String TAG = "ScreenRecordService";
    public static MediaProjection m_mediaProjection;
    private VirtualDisplay m_virtualDisplay;
    private int m_screenWidth = 0;
    private int m_screenHeight = 0;
    private int m_screenDensity = 0;
    private int m_mediaProjCode = 0;
    private Intent m_mediaProjData;
    private String m_fullPath = "";
    private Uri m_uri = null;

    private int m_bitrate = 120000000;
    private int m_fps = 60;

    private static final Object sSync = new Object();
    private static MediaMuxerWrapper sMuxer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        System.out.println("HHRecorder: start command received successfully.");

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

                if (Build.VERSION.SDK_INT >= 31) {
                    pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_IMMUTABLE);
                } else {
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
        } else {
            startForeground(101, new Notification());
        }

        // Retrieve params
        m_screenWidth = intent.getIntExtra("width", 0);
        m_screenHeight = intent.getIntExtra("height", 0);

        if (m_screenWidth == 0 || m_screenHeight == 0) {
            HhScreenRecorderPlugin._instance.onFailedToStartCapture("Screen width or height is zero!");
            return Service.START_STICKY;
        }

        // Aspect fitting
     /*   if (m_screenWidth > m_screenHeight) {
            final float scale_x = m_screenWidth / 1920f;
            final float scale_y = m_screenHeight / 1080f;
            final float scale = Math.max(scale_x,  scale_y);
            m_screenWidth= (int)(m_screenWidth / scale);
            m_screenHeight = (int)(m_screenHeight / scale);
        } else {
            final float scale_x = m_screenWidth / 1080f;
            final float scale_y = m_screenHeight / 1920f;
            final float scale = Math.max(scale_x,  scale_y);
            m_screenWidth = (int)(m_screenWidth / scale);
            m_screenHeight = (int)(m_screenHeight / scale);
        }
*/
        // No odd resolution.
        if (m_screenWidth % 2 != 0)
            m_screenWidth--;
        if (m_screenHeight % 2 != 0)
            m_screenHeight--;

        m_screenDensity = intent.getIntExtra("density", 1);
        m_mediaProjCode = intent.getIntExtra("mediaProjCode", -1);
        m_mediaProjData = intent.getParcelableExtra("mediaProjData");
        m_bitrate = intent.getIntExtra("bitrate", 120000000);
        m_fps = intent.getIntExtra("fps", 60);
        m_uri = intent.hasExtra("uri") ? Uri.parse(intent.getStringExtra("uri")) : null;
        m_fullPath = intent.hasExtra("fullpath") ? intent.getStringExtra("fullpath") : "";

        // HhScreenRecorderPlugin._instance.onFailedToStartCapture("Failed to init media recorder: " + Log.getStackTraceString(e));
        // HhScreenRecorderPlugin._instance.onStartedCapture();

        try {
            m_mediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(m_mediaProjCode, m_mediaProjData);
        } catch (Exception e) {
            HhScreenRecorderPlugin._instance.onFailedToStartCapture("Failed to init media projection: " + Log.getStackTraceString(e));
            return Service.START_STICKY;
        }

        activeService = this;
        startRecording();
        return Service.START_STICKY;
    }

    private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            System.out.print("HHRecorder: Media Encoder prepared!");
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            System.out.print("HHRecorder: Media Encoder stopped!");
        }
    };

    private void startRecording()
    {
        synchronized (sSync) {
            if (sMuxer == null) {
                try {
                    sMuxer = new MediaMuxerWrapper(getApplicationContext(), m_uri, m_fullPath, getContentResolver());
                    // for screen capturing
                    new MediaScreenEncoder(sMuxer, mMediaEncoderListener,
                            m_mediaProjection, m_screenWidth, m_screenHeight, m_screenDensity, m_bitrate, m_fps);
                    new MediaAudioEncoder(sMuxer, mMediaEncoderListener);
                    sMuxer.prepare();
                    sMuxer.startRecording();
                    HhScreenRecorderPlugin._instance.onStartedCapture();
                } catch (final IOException e) {
                    HhScreenRecorderPlugin._instance.onFailedToStartCapture(e.toString());
                }
            }
        }

    }
    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        synchronized (sSync) {
            if(sMuxer != null)
            {
                sMuxer.stopRecording();
            }
            sMuxer = null;
        }
        activeService = null;
        HhScreenRecorderPlugin._instance.onServiceDestroyed();
        stopForeground(true);
        stopSelf();

        if (m_virtualDisplay != null) {
            m_virtualDisplay.release();
            m_virtualDisplay = null;
        }

        if (m_mediaProjection != null) {
            m_mediaProjection.stop();
            m_mediaProjection = null;
        }

        super.onDestroy();
    }
}