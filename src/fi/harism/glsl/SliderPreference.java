package fi.harism.glsl;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class SliderPreference extends DialogPreference {
	
	private int mValue;
	private int mMinValue;
	private int mMaxValue;
	private int mFloatDivider;
	
	private String mTitle;
	
	public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
		mMinValue = ta.getInt(R.styleable.SliderPreference_minValue, 0);
		mMaxValue = ta.getInt(R.styleable.SliderPreference_maxValue, 100);
		mFloatDivider = ta.getInt(R.styleable.SliderPreference_floatDivider, 0);
		
		mTitle = getTitle().toString();
		setDialogLayoutResource(R.layout.slider);
	}
	
	public SliderPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	@Override
	public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		if (mFloatDivider == 0) {
			mValue = getPersistedInt(0);
		} else {
			mValue = (int)(getPersistedFloat(0) * mFloatDivider);
		}
		updateTitle();
	}
	
	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		SeekBar seekBar = (SeekBar)v.findViewById(R.id.slider);
		seekBar.setMax(mMaxValue - mMinValue);
		seekBar.setProgress(mValue - mMinValue);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar sb) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar sb) {
			}
			@Override
			public void onProgressChanged(SeekBar sb, int newValue, boolean fromUser) {
				mValue = newValue + mMinValue;
				updateTitle();
			}
		});
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (mFloatDivider == 0) {
			persistInt(mValue);
		} else {
			persistFloat((float)mValue / mFloatDivider);
		}
	}
	
	private void updateTitle() {
		String title;
		if (mFloatDivider == 0) {
			title = String.format(mTitle, mValue);
		} else {
			title = String.format(mTitle, (float)mValue / mFloatDivider);
		}
		setTitle(title);
		setDialogTitle(title);
		if (getDialog() != null) {
			getDialog().setTitle(title);
		}
	}
	
}
