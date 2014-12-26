package me.wheam.mediaplayertest;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * @author wheam@wandoujia.com (Qi Zhang)
 */
public class VideoPlayFragment extends Fragment
    implements
    MediaPlayerController,
    SurfaceHolder.Callback {

  public static final String PLAY_TYPE = "play_type";
  public static final String PLAY_PATH = "play_path";
  private PlayType playType = PlayType.URL;
  private String playPath =
      "http://data.vod.itc.cn/?prod=ad&new=/46/215/tbw9IfeJouLMiv8xuz6c5H.mp4&vid=2117206&uid=1419571212147890&plat=17&pt=5&prod=h5&pg=1&eye=0&cateCode=106&advEFId=adv_id_0";
  private static final int START_TIMEOUT = 5000;
  private static final int SEEK_POS = 5000;
  private boolean isFirstStart = true;
  private AudioManager audioManager;
  private SurfaceView videoSurface;
  private FrameLayout videoSurfaceContainer;
  private MediaPlayer player;
  private VideoControllerView controller;
  private ViewGroup loadingContainer;
  private int lastPercent;
  private int currentPosition = 0;
  private SurfaceHolder surfaceHolder;
  private boolean isSurfaceHolderReady;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
    controller = new VideoControllerView(getActivity());
    controller.setMediaPlayer(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View root = LayoutInflater.from(container.getContext()).inflate(R.layout.fragment_player, null);
    controller.setAnchorView((ViewGroup) root.findViewById(
        R.id.videoSurfaceContainer));
    videoSurface = (SurfaceView) root.findViewById(R.id.videoSurface);
    videoSurfaceContainer = (FrameLayout) root.findViewById(R.id.videoSurfaceContainer);
    player = new MediaPlayer();

    loadingContainer = (ViewGroup) root.findViewById(R.id.loading_container);
    SurfaceHolder videoHolder = videoSurface.getHolder();
    videoHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    videoHolder.addCallback(this);
    isSurfaceHolderReady = true;
    return root;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    controller.setBackListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getActivity().onBackPressed();
      }
    });
    view.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          if (controller.isShowing()) {
            hide();
          } else {
            show();
          }
        }
        return false;
      }
    });
    controller.setTitleText("测试");
    playVideo();
  }

  private void playVideo() {
    try {
      // if (player != null) {
      // player.release();
      // player = null;
      // }
      // player = new MediaPlayer();
      player.reset();
      player.setDataSource(playPath);
      player.setDisplay(surfaceHolder);
      initMediaPlayer();
      player.prepareAsync();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void initMediaPlayer() {
    player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

      @Override
      public void onBufferingUpdate(MediaPlayer mp, int percent) {
        lastPercent = percent;
      }
    });
    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        hideLoading();
        try {
          if (!player.isPlaying()) {
            player.start();
          }
        } catch (IllegalStateException e) {
          e.printStackTrace();
        }

        if (controller.isShowing()) {
          controller.sendHandlerMessage(START_TIMEOUT);
        }
        hide();
        controller.setProgress();
        controller.setPlayButtonPause();
        controller.setButtonsEnable(true);
        controller.setProgressEnable(true);
      }
    });

    player.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

      @Override
      public void onSeekComplete(MediaPlayer mp) {
        hideLoading();
        try {
          if (!player.isPlaying()) {
            player.start();
          }
        } catch (IllegalStateException e) {
          e.printStackTrace();
        }
        if (controller.isShowing()) {
          controller.sendHandlerMessage(START_TIMEOUT);
        }
        controller.setPlayButtonPause();
        controller.setButtonsEnable(true);
      }
    });
    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        if (!isFirstStart) {
          playVideo();
        }
      }
    });
    showLoading();
  }

  private void show() {
    controller.show();
  }

  private void hide() {
    controller.hide();
  }

  @Override
  public void start() {
    try {
      player.start();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
    controller.setPlayButtonPause();
  }

  @Override
  public void pause() {
    currentPosition = getCurrentPosition();
    try {
      player.pause();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
    controller.setPlayButtonPlay();

  }

  @Override
  public int getDuration() {
    if (player == null) {
      return 0;
    } else {
      try {
        return player.getDuration();
      } catch (IllegalStateException e) {
        e.printStackTrace();
        return 0;
      }
    }
  }

  @Override
  public int getCurrentPosition() {
    if (player == null) {
      return 0;
    } else {
      try {
        return player.getCurrentPosition();
      } catch (IllegalStateException e) {
        return 0;
      }
    }
  }

  @Override
  public void seekTo(int pos) {
    if (player == null) {
      return;
    }
    if (pos <= 0 || pos >= getDuration()) {
      return;
    }
    showLoading();
    try {
      player.seekTo(pos);
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
    controller.setButtonsEnable(false);
  }

  @Override
  public boolean isPlaying() {
    if (player != null && !isFirstStart) {
      boolean isPlaying = false;
      try {
        isPlaying = player.isPlaying();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      }
      return isPlaying;
    } else {
      return false;
    }
  }

  @Override
  public int getBufferPercentage() {
    return lastPercent;
  }

  @Override
  public boolean canPause() {
    return true;
  }

  @Override
  public boolean canSeekBackward() {
    return true;
  }

  @Override
  public boolean canSeekForward() {
    return true;
  }

  @Override
  public void hideLoading() {
    if (loadingContainer == null) {
      return;
    }
    if (isFirstStart) {
      isFirstStart = false;
    }
    loadingContainer.setVisibility(View.GONE);
  }

  @Override
  public void showLoading() {
    if (loadingContainer == null) {
      return;
    }
    loadingContainer.setVisibility(View.VISIBLE);
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    this.surfaceHolder = surfaceHolder;

    /*
     * android bug, see
     * http://stackoverflow.com/questions/18451854/the-surface-has-been-released-inside-surfacecreated
     */
    final Surface surface = surfaceHolder.getSurface();
    if (surface == null) {
      return;
    }
    if (isSurfaceHolderReady) {
      player.setDisplay(surfaceHolder);
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {

  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

  }

  @Override
  public void onPause() {
    super.onPause();
    pause();
  }

  @Override
  public void onStop() {
    super.onStop();
    if (player != null) {
      player.stop();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!controller.isShowing() && !isFirstStart) {
      show();
    }
  }

}
