package me.wheam.mediaplayertest;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * @author wheam@wandoujia.com (Qi Zhang)
 */
public class VideoControllerView extends RelativeLayout {

  private static final int MAX_PROGRESS = 100;
  private static final long DEFAULT_TIMEOUT = 10 * 1000L;
  private static final long MAX_TIMEOUT = 3600 * 1000L;
  private static final long SPINNER_ENABLE_TIME = 2 * 1000L;
  private static final int SEEK_POS = 5000;
  private static final int SEEK_POS_LONG = 15000;
  private static final int FADE_OUT = 1;
  private static final int SHOW_PROGRESS = 2;
  private static final int SPINNER_ENABLE = 3;
  private static final int SECONDS_OF_HOUR = 60 * 60;
  private static final int SECONDS_OF_MINUTE = 60;
  private static final int MILLIS_OF_SECOND = 1000;

  private final AtomicBoolean bottomBarHideAnimRunning = new AtomicBoolean(false);
  private final Animation.AnimationListener animatorListener = new Animation.AnimationListener() {
    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationEnd(Animation animation) {
      if (mAnchor.indexOfChild(VideoControllerView.this) > 0) {
        mAnchor.removeView(VideoControllerView.this);
      }
      bottomBarHideAnimRunning.set(false);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {}
  };
  private final View.OnClickListener mPauseListener = new View.OnClickListener() {
    public void onClick(View v) {
      doPauseResume();
    }
  };
  private final SeekBar.OnSeekBarChangeListener mSeekListener =
      new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar bar) {
          show(MAX_TIMEOUT);
          mDragging = true;
          mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
          if (mPlayer == null || !fromuser) {
            return;
          }
          long duration = mPlayer.getDuration();
          long newposition = (duration * progress) / ((long) MAX_PROGRESS);
          mPlayer.seekTo((int) newposition);
          if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime((int) newposition));
          }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
          mDragging = false;
          setProgress();
          setPlayButtonPause();
          show(DEFAULT_TIMEOUT);
          mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
      };
  private final View.OnClickListener mRewListener = new View.OnClickListener() {
    public void onClick(View v) {
      if (mPlayer == null) {
        return;
      }

      int pos = mPlayer.getCurrentPosition();
      pos -= SEEK_POS;
      if (pos <= 0) {
        pos = 0;
      }
      mPlayer.seekTo(pos);
      setProgress();

      show(DEFAULT_TIMEOUT);
    }
  };
  private MediaPlayerController mPlayer;
  private Context mContext;
  private ViewGroup mAnchor;
  private View mRoot;
  private ProgressBar mProgress;
  private TextView mEndTime, mCurrentTime;
  private boolean mShowing;
  private boolean mDragging;
  private boolean mUseFastForward;
  private boolean mFromXml;
  private ImageButton mPauseButton;
  private ImageButton mFfwdButton;
  private ImageButton mRewButton;
  private Animation topBarShowAnim;
  private Animation topBarHideAnim;
  private Animation bottomBarShowAnim;
  private Animation bottomBarHideAnim;
  private TextView mTitle;
  private ImageView mBackButton;
  private Spinner mProviderSpinner;
  private TextView mOriginWeb;
  private RelativeLayout topBar;
  private RelativeLayout bottomBar;
  private int position = 0;
  private int duration = 0;
  private Handler mHandler = new MessageHandler(this);
  private View.OnClickListener mFfwdListener = new View.OnClickListener() {
    public void onClick(View v) {
      if (mPlayer == null) {
        return;
      }

      int pos = mPlayer.getCurrentPosition();
      pos += SEEK_POS_LONG;
      if (pos > mPlayer.getDuration()) {
        pos = mPlayer.getDuration();
      }
      mPlayer.seekTo(pos);
      setProgress();

      show(DEFAULT_TIMEOUT);
    }
  };

  public VideoControllerView(Context context) {
    super(context);
    mContext = context;
  }

  public VideoControllerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
  }

  public VideoControllerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mContext = context;
  }

  public void setTitleText(String title) {
    mTitle.setText(title);
  }

  @Override
  public void onFinishInflate() {
    if (mRoot != null) {
      initControllerView(mRoot);
    }
  }

  public void setMediaPlayer(MediaPlayerController player) {
    mPlayer = player;
    setPlayButtonPlay();
  }

  public void setAnchorView(ViewGroup view) {
    mAnchor = view;

    FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT);

    removeAllViews();
    View v = makeControllerView();
    addView(v, frameParams);
  }

  protected View makeControllerView() {
    LayoutInflater inflate =
        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mRoot = inflate.inflate(R.layout.controller_view, null);

    initControllerView(mRoot);

    return mRoot;
  }

  private void initControllerView(View v) {
    topBarHideAnim = AnimationUtils.loadAnimation(getContext(), R.anim.video_top_bar_up_anim);
    topBarShowAnim = AnimationUtils.loadAnimation(getContext(), R.anim.video_top_bar_down_anim);
    bottomBarShowAnim = AnimationUtils.loadAnimation(getContext(), R.anim.video_bottom_bar_up_anim);
    bottomBarHideAnim =
        AnimationUtils.loadAnimation(getContext(), R.anim.video_bottom_bar_down_anim);
    bottomBarHideAnim.setAnimationListener(animatorListener);
    mPauseButton = (ImageButton) v.findViewById(R.id.pause);
    if (mPauseButton != null) {
      mPauseButton.requestFocus();
      mPauseButton.setOnClickListener(mPauseListener);
    }

    mFfwdButton = (ImageButton) v.findViewById(R.id.ffwd);
    if (mFfwdButton != null) {
      mFfwdButton.setOnClickListener(mFfwdListener);
    }

    mRewButton = (ImageButton) v.findViewById(R.id.rew);
    if (mRewButton != null) {
      mRewButton.setOnClickListener(mRewListener);
    }
    setButtonsEnable(false);
    mProgress = (ProgressBar) v.findViewById(R.id.mediacontroller_progress);
    if (mProgress != null) {
      setProgressEnable(false);
      mProgress.setMax(MAX_PROGRESS);
    }

    mEndTime = (TextView) v.findViewById(R.id.time);
    mEndTime.setText(stringForTime(0));
    mCurrentTime = (TextView) v.findViewById(R.id.time_current);
    mCurrentTime.setText(stringForTime(0));

    mTitle = (TextView) v.findViewById(R.id.title);
    mBackButton = (ImageView) v.findViewById(R.id.back);

    // make the topbar and bottonbar catch the touchevent but do nothing
    // so it will reduce the wrong operation
    topBar = (RelativeLayout) v.findViewById(R.id.top_bar);
    topBar.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return true;
      }
    });
    bottomBar = (RelativeLayout) v.findViewById(R.id.bottom_bar);
    bottomBar.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return true;
      }
    });
  }

  /**
   * Show the controller on screen. It will go away
   * automatically after 3 seconds of inactivity.
   */
  public void show() {
    show(DEFAULT_TIMEOUT);
  }

  /**
   * Disable pause or seek buttons if the stream cannot be paused or seeked.
   * This requires the control interface to be a MediaPlayerControlExt
   */

  public void setButtonsEnable(boolean enable) {
    if (mPlayer == null) {
      return;
    } else if (mPauseButton == null || mRewButton == null
        || mFfwdButton == null || mProgress == null) {
      return;
    }
    mPauseButton.setEnabled(enable);
    mRewButton.setEnabled(enable);
    mFfwdButton.setEnabled(enable);
  }

  public void setProgressEnable(boolean enable) {
    if (mProgress == null) {
      return;
    }
    if (mProgress instanceof SeekBar) {
      SeekBar seekBar = (SeekBar) mProgress;
      seekBar.setEnabled(enable);
      seekBar.setOnSeekBarChangeListener(mSeekListener);
    }
  }

  /**
   * Show the controller on screen. It will go away
   * automatically after 'timeout' milliseconds of inactivity.
   *
   * @param timeout The timeout in milliseconds. Use 0 to show
   *          the controller until hide() is called.
   */
  public void show(long timeout) {
    if (bottomBarHideAnimRunning.get()) {
      // if the hiding animation has not yet ended, do nothing.
      return;
    }

    if (!mShowing && mAnchor != null) {
      setProgress();
      if (mPauseButton != null) {
        mPauseButton.requestFocus();
      }
      FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
          Gravity.BOTTOM);
      topBar.startAnimation(topBarShowAnim);
      bottomBar.startAnimation(bottomBarShowAnim);
      mAnchor.addView(this, tlp);
      mShowing = true;
      setPlayButtonPause();
      Message msg = mHandler.obtainMessage(FADE_OUT);
      if (timeout != 0 && mPlayer.isPlaying()) {
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendMessageDelayed(msg, timeout);
      }
    }
    mHandler.sendEmptyMessage(SHOW_PROGRESS);
  }

  public void sendHandlerMessage(long timeout) {
    Message msg = mHandler.obtainMessage(FADE_OUT);
    if (timeout != 0) {
      mHandler.removeMessages(FADE_OUT);
      mHandler.sendMessageDelayed(msg, timeout);
    }
  }

  public boolean isShowing() {
    return mShowing;
  }

  /**
   * Remove the controller from the screen.
   */
  public void hide() {
    if (mAnchor == null) {
      return;
    }
    if (mShowing) {
      try {
        topBar.startAnimation(topBarHideAnim);
        bottomBarHideAnimRunning.set(true);
        bottomBar.startAnimation(bottomBarHideAnim);
        mHandler.removeMessages(SHOW_PROGRESS);
      } catch (IllegalArgumentException ex) {
        Log.w("MediaController", "already removed");
      }
      mShowing = false;
    }
  }


  public int setProgress() {
    if (mPlayer == null || mDragging || !mPlayer.isPlaying()) {
      return 0;
    }
    if (mPlayer.isPlaying()) {
      position = mPlayer.getCurrentPosition();
      duration = mPlayer.getDuration();
    }
    if (position >= duration) {
      return 1;
    }
    if (mProgress != null) {
      if (duration > 0) {
        long pos = ((long) MAX_PROGRESS) * position / duration;
        mProgress.setProgress((int) pos);
      }
      int percent = mPlayer.getBufferPercentage();
      mProgress.setSecondaryProgress(percent);
    }

    if (mEndTime != null) {
      mEndTime.setText(stringForTime(duration));
    }
    if (mCurrentTime != null) {
      mCurrentTime.setText(stringForTime(position));
    }
    return position;
  }

  @Override
  public boolean onTrackballEvent(MotionEvent ev) {
    show(DEFAULT_TIMEOUT);
    return false;
  }

  public void setPlayButtonPlay() {
    if (mRoot == null || mPauseButton == null || mPlayer == null) {
      return;
    }
    if (!mPlayer.isPlaying()) {
      mPauseButton.setImageResource(R.drawable.aa_video_play_button_selector);
    }
  }

  public void setPlayButtonPause() {
    if (mRoot == null || mPauseButton == null || mPlayer == null) {
      return;
    }
    if (mPlayer.isPlaying()) {
      mPauseButton.setImageResource(R.drawable.aa_video_pause_button_selector);
    }
  }

  public void doPauseResume() {
    if (mPlayer == null) {
      return;
    }
    if (mPlayer.isPlaying()) {
      mPlayer.pause();
      setPlayButtonPlay();
      show();
    } else {
      mPlayer.start();
      setPlayButtonPause();
      sendHandlerMessage(DEFAULT_TIMEOUT);
    }
    setProgress();
    show();
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (mPauseButton != null) {
      mPauseButton.setEnabled(enabled);
    }
    if (mFfwdButton != null) {
      mFfwdButton.setEnabled(enabled);
    }
    if (mRewButton != null) {
      mRewButton.setEnabled(enabled);
    }
    if (mProgress != null) {
      mProgress.setEnabled(enabled);
    }
    super.setEnabled(enabled);
  }

  public void setBackListener(OnClickListener mBackListener) {
    mBackButton.setOnClickListener(mBackListener);
  }


  private static class MessageHandler extends Handler {
    private final WeakReference<VideoControllerView> mView;

    MessageHandler(VideoControllerView view) {
      mView = new WeakReference<VideoControllerView>(view);
    }

    @Override
    public void handleMessage(Message msg) {
      VideoControllerView view = mView.get();
      if (view == null || view.mPlayer == null) {
        return;
      }
      int pos;
      switch (msg.what) {
        case FADE_OUT:
          if (view.isShowing()) {
            view.hide();
          }
          break;
        case SHOW_PROGRESS:
          pos = view.setProgress();
          if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {
            msg = obtainMessage(SHOW_PROGRESS);
            sendMessageDelayed(msg, MAX_PROGRESS - (pos % MAX_PROGRESS));
          }
          break;
        default:
          break;
      }
    }
  }

  public String stringForTime(long timeMs) {
    StringBuilder formatBuilder = new StringBuilder();
    Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
    long totalSeconds = timeMs / MILLIS_OF_SECOND;
    long seconds = totalSeconds % SECONDS_OF_MINUTE;
    long minutes = (totalSeconds / SECONDS_OF_MINUTE) % SECONDS_OF_MINUTE;
    long hours = totalSeconds / SECONDS_OF_HOUR;
    formatBuilder.setLength(0);
    return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
  }
}
