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

	// Music player instance.
	private MusicPlayer mMusicPlayer;
	// GlslRenderer Instance.
	private GlslRenderer mRenderer;
	// Default GLSurfaceView instance.
	private GLSurfaceView mSurfaceView;

	// Timer for updating FPS value.
	private Timer mFpsTimer;
	// Runnable for updating FPS text withing UI thread.
	private Runnable mFpsRunnable;

	// Application name to which FPS value is appended.
	private String mAppName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Try to save some classes from previous instance.
		if (getLastNonConfigurationInstance() != null) {
			GlslActivity self = (GlslActivity) getLastNonConfigurationInstance();
			mRenderer = self.mRenderer;
			mMusicPlayer = self.mMusicPlayer;
		} else {
			mRenderer = new GlslRenderer();
			mMusicPlayer = new MusicPlayer(500, 50);
		}
		// Mandatory call, GlslRenderer uses Context.getString(..) to read
		// shaders etc.
		mRenderer.setOwnerActivity(this);

		// Initiate GLSurfaceView for rendering.
		mSurfaceView = new GLSurfaceView(this);
		mSurfaceView.setEGLContextClientVersion(2);
		mSurfaceView.setRenderer(mRenderer);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		mSurfaceView.setOnTouchListener(mRenderer);

		// Read application name.
		mAppName = getString(R.string.app_name);
		// Initiate FPS runnable.
		mFpsRunnable = new FpsRunnable();

		// Setup default preference values, setting readAgain to true adds new
		// preferences given their default values into saved preferences.
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		// We have our own title, must be called before content view is being
		// set.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		// Add GLSurfaceView into content view.
		ViewGroup container = (ViewGroup) findViewById(R.id.layout_container);
		container.addView(mSurfaceView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Generate options menu.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Show about dialog.
		// TODO: maybe it was better to use 'proper' dialog showing methods.
		case R.id.menu_about:
			Dialog dlg = new Dialog(this);
			dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dlg.setContentView(R.layout.about);
			dlg.show();
			return true;
			// Start preferences Activity.
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
		// GLSurfaceView.onPause must be called.
		mSurfaceView.onPause();
		// Stop FPS timer.
		mFpsTimer.cancel();
		mFpsTimer = null;
		// Stop playing music and release underlying MediaPlayer instance
		// eventually. I'm not exactly sure is it safe to leave a thread running
		// even after this Activity has been killed. Haven't faced problems yet
		// though.
		mMusicPlayer.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		// Must be called on onResume.
		mSurfaceView.onResume();

		// Fetch shared preferences.
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Initiate FPS timer.
		mFpsTimer = new Timer();
		String key = getString(R.string.key_show_title);
		if (prefs.getBoolean(key, true)) {
			// If title is visible, assure it really is and start FPS timer.
			findViewById(R.id.layout_header).setVisibility(View.VISIBLE);
			mFpsTimer.scheduleAtFixedRate(new FpsTimerTask(), 0, 500);
		} else {
			// Otherwise hide title.
			findViewById(R.id.layout_header).setVisibility(View.GONE);
		}

		key = getString(R.string.key_play_music);
		if (prefs.getBoolean(key, true)) {
			// If music should be played try to start streaming, in error cases
			// simply release underlying MediaPlayer.
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
	private final class FpsRunnable implements Runnable {
		@Override
		public void run() {
			// Get FPS value from renderer.
			String fps = Float.toString(mRenderer.getFps());
			// Try to locate ".", if its being used as fraction separator.
			int separator = fps.indexOf('.');
			// In case it was not found, try "," instead.
			if (separator == -1) {
				separator = fps.indexOf(',');
			}
			// If separator was found, strip
			if (separator != -1) {
				fps = fps.substring(0,
						Math.min(fps.length() - 1, separator + 2));
			}
			// Locate title TextView and update its content.
			TextView tv = (TextView) findViewById(R.id.layout_title);
			tv.setText(mAppName + " (" + fps + "fps)");
		}
	}

	/**
	 * Timer task which triggers FPS updating periodically.
	 */
	private final class FpsTimerTask extends TimerTask {
		@Override
		public void run() {
			// Simply forward callback to UI thread.
			runOnUiThread(mFpsRunnable);
		}
	}

	/**
	 * Helper class for handling music playback.
	 */
	private final class MusicPlayer implements MediaPlayer.OnPreparedListener {

		// MediaPlayer instance.
		private MediaPlayer mMediaPlayer;
		// Current playing position.
		private int mMediaPlayerPosition;

		// Fade in/out time.
		private int mFadeTime;
		// Fade in/out update interval.
		private int mFadeInterval;

		/**
		 * Constructor takes fade in/out time, and fade interval, as a
		 * parameter. These values are used for starting and stopping playback
		 * as music fades in on start and fades out, before releasing
		 * MediaPlayer, on stop.
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
			// Just in case playback was stopped before being prepared.
			if (mMediaPlayer != null) {
				// Set playback values before starting it.
				mMediaPlayer.setVolume(0f, 0f);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.seekTo(mMediaPlayerPosition);
				mMediaPlayer.start();
				// Timer for ramping up volume.
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
			// If there's existing MediaPlayer, remove it first.
			if (mMediaPlayer != null) {
				// Instead of releasing MediaPlayer, ramp down volume first.
				new FadeOutTimer(mMediaPlayer, mFadeTime, mFadeInterval)
						.start();
			}
			// Instantiate new MediaPlayer object.
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
			// In case stop is called twice or so on.
			if (mMediaPlayer != null) {
				// Store current position for restarting purposes.
				mMediaPlayerPosition = mMediaPlayer.getCurrentPosition()
						+ mFadeTime;
				// Trigger a fade out timer which will take ownership of
				// MediaPlayer and release it eventually.
				new FadeOutTimer(mMediaPlayer, mFadeTime, mFadeInterval)
						.start();
				mMediaPlayer = null;
			}
		}

		/**
		 * Helper class for handling music fade in.
		 */
		private final class FadeInTimer extends CountDownTimer {

			// MediaPlayer object for applying volume adjustment into.
			private MediaPlayer mMediaPlayerIn;
			// How long fade in should last.
			private long mFadeInTime;

			public FadeInTimer(MediaPlayer mediaPlayerIn, long fadeInTime,
					long fadeInInterval) {
				super(fadeInTime, fadeInInterval);
				mMediaPlayerIn = mediaPlayerIn;
				mFadeInTime = fadeInTime;
			}

			@Override
			public void onFinish() {
				// On finish set volume to max.
				mMediaPlayerIn.setVolume(1f, 1f);
				mMediaPlayerIn = null;
			}

			@Override
			public void onTick(long millisUntilFinish) {
				// On ticks increase volume.
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
				// Once finished, release MediaPlayer and set it to null.
				mMediaPlayerOut.release();
				mMediaPlayerOut = null;
			}

			@Override
			public void onTick(long millisUntilFinish) {
				// On tick decrease volume.
				float v = (float) millisUntilFinish / mFadeOutTime;
				mMediaPlayerOut.setVolume(v, v);
			}

		}
	}

}
