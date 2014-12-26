package me.wheam.mediaplayertest;

/**
 * @author wheam@wandoujia.com (Qi Zhang)
 */
public interface MediaPlayerController {
  void start();

  void pause();

  int getDuration();

  int getCurrentPosition();

  void seekTo(int pos);

  boolean isPlaying();

  int getBufferPercentage();

  boolean canPause();

  boolean canSeekBackward();

  boolean canSeekForward();

  void hideLoading();

  void showLoading();
}
