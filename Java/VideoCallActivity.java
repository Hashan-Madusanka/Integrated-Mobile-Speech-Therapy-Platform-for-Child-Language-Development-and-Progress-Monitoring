package com.example.speechtherapy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class VideoCallActivity extends AppCompatActivity {
    private RtcEngine mRtcEngine;
    private static final String APP_ID = "de4d8efc859947179b791826b15834ef";
    private static final String CHANNEL_NAME = "Speech Therapy";
    ImageView endcall;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_call);
        endcall = findViewById(R.id.endcall);
        initializeAgoraEngine();
        if (mRtcEngine != null) {
            // Join the channel
            mRtcEngine.joinChannel(null, CHANNEL_NAME, null, 0);

            // Set up the local video view
            SurfaceView localView =  new SurfaceView(getApplicationContext());
            localView.setZOrderMediaOverlay(true);
            mRtcEngine.setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0));

            // Add local video view to your layout
            FrameLayout localContainer = findViewById(R.id.local_video_container);
            localContainer.addView(localView);

            SurfaceView remoteView = new SurfaceView(getApplicationContext());
            mRtcEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, 1));
            FrameLayout remoteContainer = findViewById(R.id.remote_video_container);
            remoteContainer.addView(remoteView);
        }
        endcall.setOnClickListener(view -> {
            // Leave the Agora channel
            if (mRtcEngine != null) {
                mRtcEngine.leaveChannel();
            }

            // Reference to the videoCall node in Firebase
            DatabaseReference videoCallRef = FirebaseDatabase.getInstance().getReference("videoCall");

            // Remove the videoCall node
            videoCallRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Set the result to indicate success
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("refreshRequired", true);
                    setResult(RESULT_OK, resultIntent);
                    Log.d("VideoCall", "Video call node deleted successfully");
                    finish(); // Close the current activity and navigate to the previous page
                } else {
                    Log.e("VideoCall", "Failed to delete video call node", task.getException());
                }
            });
        });

    }
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), APP_ID, new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    Log.d("Agora", "Join channel success, uid: " + uid);
                }
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    Log.d("Agora", "Remote user joined: " + uid);

                    runOnUiThread(() -> {
                        // Set up the remote video view
                        SurfaceView remoteView = new SurfaceView(getApplicationContext());
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                        FrameLayout remoteContainer = findViewById(R.id.remote_video_container);
                        remoteContainer.removeAllViews(); // Clear previous views, if any
                        remoteContainer.addView(remoteView);
                    });
                }
                @Override
                public void onError(int err) {
                    super.onError(err);
                    Log.e("Agora", "Error: " + err);
                }
            });
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    new VideoEncoderConfiguration.VideoDimensions(1280, 720),
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE));

            mRtcEngine.enableVideo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            mRtcEngine.destroy();
        }
    }


}