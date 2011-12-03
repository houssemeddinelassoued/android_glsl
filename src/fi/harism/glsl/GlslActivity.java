/*
   Copyright 2011 Harri Smått

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.glsl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

/**
 * Main Activity class.
 */
public final class GlslActivity extends Activity {

	private MusicPlayer mMusicPlayer;
	private GlslRenderer mRenderer;
	private GLSurfaceView mSurfaceView;

	private Timer mFpsTimer;
	private Runnable mFpsRunnable;

	private String mAppName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getLastNonConfigurationInstance() != null) {
			GlslActivity self = (GlslActivity) getLastNonConfigurationInstance();
			mRenderer = self.mRenderer;
			mMusicPlayer = self.mMusicPlayer;
		} else {
			mRenderer = new GlslRenderer();
			mMusicPlayer = new MusicPlayer(500, 50);
		}
		mRenderer.setOwnerActivity(this);

		mSurfaceView = new GLSurfaceView(this);
		mSurfaceView.setEGLContextClientVersion(2);
		mSurfaceView.setRenderer(mRenderer);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		mSurfaceView.setOnTouchListener(mRenderer);

		mAppName = getString(R.string.app_name);
		mFpsRunnable = new FpsRunnable();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		ViewGroup container = (ViewGroup) findViewById(R.id.layout_container);
		container.addView(mSurfaceView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			Dialog dlg = new Dialog(this);
			dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dlg.setContentView(R.layout.about);
			dlg.show();
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this,
					fi.harism.glsl.prefs.GlslPreferenceActivity.class));
			return true;
		}
		return false;
	}

	@Override
	public void onPause() {
		super.onPause();
		mSurfaceView.onPause();
		mFpsTimer.cancel();
		mFpsTimer = null;

		mMusicPlayer.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		mSurfaceView.onResume();
		mFpsTimer = new Timer();

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		String key = getString(R.string.key_show_title);
		if (prefs.getBoolean(key, true)) {
			findViewById(R.id.layout_header).setVisibility(View.VISIBLE);
			mFpsTimer.scheduleAtFixedRate(new FpsTimerTask(), 0, 500);
		} else {
			findViewById(R.id.layout_header).setVisibility(View.GONE);
		}

		key = getString(R.string.key_play_music);
		if (prefs.getBoolean(key, true)) {
			try {
				mMusicPlayer.start(getResources().openRawResourceFd(
						R.raw.mosaik_01_leandi).getFileDescriptor());
			} catch (IOException ex) {
				mMusicPlayer.stop();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return this;
	}

	/**
	 * Helper class for handling FPS related Activity.runOnUiThread() calls.
	 */
	private class FpsRunnable implements Runnable {
		@Override
		public void run() {
			String fps = Float.toString(mRenderer.getFps());
			int separator = fps.indexOf('.');
			if (separator == -1) {
				separator = fps.indexOf(',');
			}
			if (separator != -1) {
				fps = fps.substring(0, separator + 2);
			}
			TextView tv = (TextView) findViewById(R.id.layout_title);
			tv.setText(mAppName + " (" + fps + "fps)");
		}
	}

	/**
	 * Timer task which triggers FPS updating periodically.
	 */
	private class FpsTimerTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(mFpsRunnable);
		}
	}

	/**
	 * Helper class for handling music playback.
	 */
	private final class MusicPlayer implements MediaPlayer.OnPreparedListener {

		private MediaPlayer mMediaPlayer;
		private int mMediaPlayerPosition;

		private int mFadeTime;
		private int mFadeInterval;

		/**
		 * Constructor takes fade in/out time, and fade interval, as a
		 * parameter.
		 * 
		 * @param fadeTime
		 *            Fade in and fade out time
		 * @param fadeInterval
		 *            Fade 'accuracy'
		 */
		public MusicPlayer(int fadeTime, int fadeInterval) {
			mFadeTime = fadeTime;
			mFadeInterval = fadeInterval;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			if (mMediaPlayer != null) {
				mMediaPlayer.setVolume(0f, 0f);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.seekTo(mMediaPlayerPosition);
				mMediaPlayer.start();
				new FadeInTimer(mMediaPlayer, mFadeTime, mFadeInterval).start();
			}
		}

		/**
		 * Starts music playback. Music is faded in once MediaPlayer has been
		 * prepared for given FileDescriptor. If there is music playing already,
		 * it will be stopped before starting new one.
		 * 
		 * @param fileDescriptor
		 *            Music file to play
		 * @throws IOException
		 */
		public void start(FileDescriptor fileDescriptor) throws IOException {
			if (mMediaPlayer != null) {
				new FadeOutTimer(mMediaPlayer, mFadeTime, mFadeInterval)
						.start();
			}
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setDataSource(fileDescriptor);
			mMediaPlayer.prepareAsync();
		}

		/**
		 * Stops playing music. Music is first faded out and MediaPlayer
		 * destroyed once fade out has been finished. Calling this method does
		 * not require previous call to start().
		 */
		public void stop() {
			if (mMediaPlayer != null) {
				mMediaPlayerPosition = mMediaPlayer.getCurrentPosition()
						+ mFadeTime;
				new FadeOutTimer(mMediaPlayer, mFadeTime, mFadeInterval)
						.start();
				mMediaPlayer = null;
			}
		}

		/**
		 * Helper class for handling music fade in.
		 */
		private class FadeInTimer extends CountDownTimer {

			private MediaPlayer mMediaPlayerIn;
			private long mFadeInTime;

			public FadeInTimer(MediaPlayer mediaPlayerIn, long fadeInTime,
					long fadeInInterval) {
				super(fadeInTime, fadeInInterval);
				mMediaPlayerIn = mediaPlayerIn;
				mFadeInTime = fadeInTime;
			}

			@Override
			public void onFinish() {
				mMediaPlayerIn.setVolume(1f, 1f);
			}

			@Override
			public void onTick(long millisUntilFinish) {
				float v = (float) millisUntilFinish / mFadeInTime;
				mMediaPlayerIn.setVolume(1f - v, 1f - v);
			}

		}

		/**
		 * Helper class for handling music fade out / stopping.
		 */
		private class FadeOutTimer extends CountDownTimer {

			private MediaPlayer mMediaPlayerOut;
			private long mFadeOutTime;

			/**
			 * Constructor for MediaPlayer fade out class. FadeOutTimer releases
			 * given MediaPlayer instance once underlying timer has finished.
			 * 
			 * @param mediaPlayerOut
			 *            MediaPlayer instance to be released.
			 * @param fadeOutTime
			 *            Fade out time
			 * @param fadeOutInterval
			 *            Fade out interval
			 */
			public FadeOutTimer(MediaPlayer mediaPlayerOut, long fadeOutTime,
					long fadeOutInterval) {
				super(fadeOutTime, fadeOutInterval);
				mMediaPlayerOut = mediaPlayerOut;
				mFadeOutTime = fadeOutTime;
			}

			@Override
			public void onFinish() {
				mMediaPlayerOut.release();
				mMediaPlayerOut = null;
			}

			@Override
			public void onTick(long millisUntilFinish) {
				float v = (float) millisUntilFinish / mFadeOutTime;
				mMediaPlayerOut.setVolume(v, v);
			}

		}
	}

}
