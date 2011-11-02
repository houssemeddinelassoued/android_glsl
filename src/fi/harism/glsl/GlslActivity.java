package fi.harism.glsl;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class GlslActivity extends Activity {

	private GLSurfaceView mGLSurfaceView;
	private GlslRenderer mGlslRenderer;

	private Timer mTimer;
	private Runnable mUiRunnable;

	private String mAppName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGLSurfaceView = new GLSurfaceView(this);
		mGLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
				| GLSurfaceView.DEBUG_LOG_GL_CALLS);
		mGLSurfaceView.setEGLContextClientVersion(2);

		mGlslRenderer = new GlslRenderer(this);
		mGLSurfaceView.setRenderer(mGlslRenderer);

		mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		setContentView(mGLSurfaceView);

		mUiRunnable = new FPSUiRunnable();
		mAppName = getString(R.string.app_name);
	}

	@Override
	public void onPause() {
		super.onPause();
		mGLSurfaceView.onPause();
		mTimer.cancel();
		mTimer = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		mGLSurfaceView.onResume();
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new FPSTimerTask(), 0, 200);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return false;
	}

	private class FPSTimerTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(mUiRunnable);
		}
	}

	private class FPSUiRunnable implements Runnable {
		@Override
		public void run() {
			String fps = Float.toString(mGlslRenderer.getFPS());
			int separator = fps.indexOf('.');
			if (separator == -1) {
				separator = fps.indexOf(',');
			}
			if (separator != -1) {
				fps = fps.substring(0, separator + 2);
			}
			setTitle(mAppName + " (" + fps + "fps)");
		}
	}
}
