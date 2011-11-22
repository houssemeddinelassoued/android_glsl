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

package fi.harism.glsl.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import fi.harism.glsl.R;

public class GlslSliderPreference extends DialogPreference {

	private float mValue;
	private float mValueMin;
	private float mValueMax;
	private float mValuePrecision;

	private String mTitle;

	public GlslSliderPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GlslSliderPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attrs,
				R.styleable.SliderPreference);
		mValueMin = ta.getFloat(R.styleable.SliderPreference_valueMin, 0);
		mValueMax = ta.getFloat(R.styleable.SliderPreference_valueMax, 100);
		mValuePrecision = ta.getFloat(
				R.styleable.SliderPreference_valuePrecision, 1);

		mTitle = getTitle().toString();
		setDialogLayoutResource(R.layout.slider);
	}

	private void updateTitle() {
		String title = String.format(mTitle, mValue);
		setTitle(title);
		setDialogTitle(title);
		if (getDialog() != null) {
			getDialog().setTitle(title);
		}
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		SeekBar seekBar = (SeekBar) v.findViewById(R.id.slider);
		seekBar.setMax((int) ((mValueMax - mValueMin) / mValuePrecision));
		seekBar.setProgress((int) ((mValue - mValueMin) / mValuePrecision));
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar sb, int newValue,
					boolean fromUser) {
				mValue = (newValue * mValuePrecision) + mValueMin;
				updateTitle();
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
			}
		});
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		persistFloat(mValue);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray attrs, int index) {
		return attrs.getFloat(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		if (restorePersistedValue) {
			mValue = getPersistedFloat(0);
		} else {
			mValue = (Float) defaultValue;
			persistFloat(mValue);
		}
		updateTitle();
	}

}
