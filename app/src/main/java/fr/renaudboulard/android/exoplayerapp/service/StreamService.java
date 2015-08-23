package fr.renaudboulard.android.exoplayerapp.service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.util.DebugTextViewHelper;
import com.google.android.exoplayer.util.Util;

import java.util.List;
import java.util.Map;

import fr.renaudboulard.android.exoplayerapp.player.DemoPlayer;
import fr.renaudboulard.android.exoplayerapp.player.ExtractorRendererBuilder;

/**
 * Created by renaud on 22/08/15.
 */
public class StreamService extends Service implements
        DemoPlayer.Listener, DemoPlayer.CaptionListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener {

    public static final int TYPE_MP3 = 4;
    public static final int TYPE_AAC = 9;

    private final IBinder mBinder = new LocalBinder();
    Callbacks activity;

    private DemoPlayer player;
    private boolean playerNeedsPrepare;

    private Uri contentUri;
    private String url;
    private int contentType;
    private long playerPosition;
    private EventLogger eventLogger;

    private AudioCapabilities audioCapabilities;


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d("DEBUG", "onCreate Stream Service");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DEBUG", "Start Stream Service");
        if(intent!=null){
            url = intent.getStringExtra("URL");
            preparePlayer();
        }
        return START_NOT_STICKY;
    }

    public void onDestroy() {
        Log.d("DEBUG", "Stop Stream Service");
        releasePlayer();
    }



    private void preparePlayer() {
        contentType = TYPE_AAC;
        contentUri = Uri.parse(url);
        if (player == null) {
            player = new DemoPlayer(getRendererBuilder());
            player.addListener(this);
            player.setCaptionListener(this);
            player.setMetadataListener(this);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            eventLogger = new EventLogger();
            eventLogger.startSession();

            eventLogger.mCallback = new EventLogger.OnPlayerFailedListener() {
                @Override
                public void onPlayerFailed() {
                    //preparePlayer();
                }
            };

            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {

        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
    }

    @Override
    public void onError(Exception e) {
        playerNeedsPrepare = true;
        preparePlayer();
    }

    // Internal methods

    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        switch (contentType) {
            case TYPE_MP3:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, new Mp3Extractor());
            case TYPE_AAC:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, new AdtsExtractor());
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    @Override
    public void onCues(List<Cue> cues) {

    }

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {

    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {

    }

    public void pause() {
        if(player.getPlayerControl().isPlaying()){
            player.getPlayerControl().pause();
        }else{
            player.getPlayerControl().start();
        }

    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public StreamService getServiceInstance(){
            return StreamService.this;
        }
    }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;
    }

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        public void updateTitle(String title);
    }
}