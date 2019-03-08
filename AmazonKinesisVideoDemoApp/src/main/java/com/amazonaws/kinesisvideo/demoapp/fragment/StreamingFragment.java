package com.amazonaws.kinesisvideo.demoapp.fragment;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.kinesisvideo.client.KinesisVideoClientConfiguration;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.ClipFragment;
import com.amazonaws.kinesisvideo.demoapp.KinesisVideoDemoApp;
import com.amazonaws.kinesisvideo.demoapp.R;
import com.amazonaws.kinesisvideo.demoapp.activity.SimpleNavActivity;
import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;
import com.amazonaws.regions.Regions;

import java.util.ArrayList;
import java.util.List;

import hackathon.ReportKVSClipClient;

public class StreamingFragment extends Fragment implements TextureView.SurfaceTextureListener {
    public static final String KEY_MEDIA_SOURCE_CONFIGURATION_1 = "mediaSourceConfiguration1";
    public static final String KEY_MEDIA_SOURCE_CONFIGURATION_2 = "mediaSourceConfiguration2";
    public static final String KEY_STREAM_NAME = "streamName";

    private static final String TAG = StreamingFragment.class.getSimpleName();

    private Button mStartStreamingButton;
    private Button mSearchClips;
    private KinesisVideoClient mKinesisVideoClient;
    private String mStreamName;
    private AndroidCameraMediaSourceConfiguration mConfiguration1;
    private AndroidCameraMediaSourceConfiguration mConfiguration2;
    private AndroidCameraMediaSource mCameraMediaSource1;
    private AndroidCameraMediaSource mCameraMediaSource2;

    private SimpleNavActivity navActivity;
    private Long streamingStartTime;
    private Long streamingEndTime;

    public static StreamingFragment newInstance(SimpleNavActivity navActivity) {
        StreamingFragment s = new StreamingFragment();
        s.navActivity = navActivity;
        return s;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        getArguments().setClassLoader(AndroidCameraMediaSourceConfiguration.class.getClassLoader());
        mStreamName = getArguments().getString(KEY_STREAM_NAME);
        mConfiguration1 = getArguments().getParcelable(KEY_MEDIA_SOURCE_CONFIGURATION_1);
        mConfiguration2 = getArguments().getParcelable(KEY_MEDIA_SOURCE_CONFIGURATION_2);

        final View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        TextureView textureView = (TextureView) view.findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(this);
        return view;
    }

    private void createClientAndStartStreaming(final SurfaceTexture previewTexture) {

        try {
            mKinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    getActivity(),
                    KinesisVideoDemoApp.KINESIS_VIDEO_REGION,
                    KinesisVideoDemoApp.getCredentialsProvider());


            mCameraMediaSource1 = (AndroidCameraMediaSource) mKinesisVideoClient
                    .createMediaSource(mStreamName + "-Back", mConfiguration1);
            mCameraMediaSource2 = (AndroidCameraMediaSource) mKinesisVideoClient
                    .createMediaSource(mStreamName + "-Front", mConfiguration2);
            mCameraMediaSource1.setPreviewSurfaces();
            mCameraMediaSource2.setPreviewSurfaces();

            resumeStreaming();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to start streaming");
            throw new RuntimeException("unable to start streaming", e);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mStartStreamingButton = (Button) view.findViewById(R.id.start_streaming);
        mStartStreamingButton.setOnClickListener(stopStreamingWhenClicked());
        mSearchClips = (Button) view.findViewById(R.id.search_clips);
        //mSearchClips.setOnClickListener(openSearchClipFragment());
    }

//    private View.OnClickListener openSearchClipFragment() {
//        final ClipFragment searchClipFragment = ClipFragment.newInstance(2);
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        fragmentManager.beginTransaction().replace(R.id.content_simple, fragment).commit();
//    }

    @Override
    public void onResume() {
        super.onResume();
        resumeStreaming();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseStreaming();
    }

    private View.OnClickListener stopStreamingWhenClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                pauseStreaming();
                navActivity.startConfigFragment();
            }
        };
    }

    private void resumeStreaming() {
        try {
            if (mCameraMediaSource1 == null || mCameraMediaSource2 == null) {
                return;
            }

            mCameraMediaSource1.start();
            mCameraMediaSource2.start();
            streamingStartTime = System.currentTimeMillis();
            Toast.makeText(getActivity(), "resumed streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.stop_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to resume streaming", e);
            Toast.makeText(getActivity(), "failed to resume streaming", Toast.LENGTH_SHORT).show();
        }
    }


    private void pauseStreaming() {
        try {
            if (mCameraMediaSource1 == null || mCameraMediaSource2 == null) {
                return;
            }

            mCameraMediaSource1.stop();
            mCameraMediaSource2.stop();
            streamingEndTime = System.currentTimeMillis();
            // TODO : Call ReportKVSClient here
            //ReportKVSClipClient reportKVSClipClient = //

            Toast.makeText(getActivity(), "stopped streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.start_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to pause streaming", e);
            Toast.makeText(getActivity(), "failed to pause streaming", Toast.LENGTH_SHORT).show();
        }
    }

    ////
    // TextureView.SurfaceTextureListener methods
    ////

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize(1280, 720);
        createClientAndStartStreaming(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        try {
            if (mCameraMediaSource1 != null)
                mCameraMediaSource1.stop();
            if (mCameraMediaSource2 != null)
                mCameraMediaSource2.stop();
            if (mKinesisVideoClient != null)
                mKinesisVideoClient.stopAllMediaSources();
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "failed to release kinesis video client", e);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
}
