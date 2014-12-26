package me.wheam.mediaplayertest;

import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;

/**
 * @author wheam@wandoujia.com (Qi Zhang)
 */
public class VideoPlayActivity extends ActionBarActivity {

  private VideoPlayFragment playFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getSupportActionBar().hide();
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    if (savedInstanceState == null) {
      playFragment = new VideoPlayFragment();
      getSupportFragmentManager().beginTransaction()
          .replace(android.R.id.content, playFragment, "play").commitAllowingStateLoss();

    }
  }
}
