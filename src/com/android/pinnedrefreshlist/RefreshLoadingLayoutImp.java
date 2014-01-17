package com.android.pinnedrefreshlist;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RefreshLoadingLayoutImp implements ILoadingLayout {

	private View mHead = null;
	private ImageView head_arrow = null;
	private ProgressBar head_progress_bar;
	private TextView head_tips_tv;
	private TextView head_last_time_tv;
	private RelativeLayout head_contentLayout;

	private RotateAnimation mRotateAnimation, mResetRotateAnimation;
	private SharedPreferences mPreferences;
	private String mKey;
	private boolean isTriggerRelease = false;

	public RefreshLoadingLayoutImp(Context context, String key ) {
		mHead = View.inflate(context, R.layout.refresh_header, null);
		head_arrow = (ImageView) mHead.findViewById(R.id.head_arrow);
		head_progress_bar = (ProgressBar) mHead.findViewById(R.id.head_progress_bar);
		head_tips_tv = (TextView) mHead.findViewById(R.id.head_tips_tv);
		head_last_time_tv = (TextView) mHead.findViewById(R.id.head_last_time_tv);
		int widthMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.MATCH_PARENT, MeasureSpec.AT_MOST);
		int heightMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
		mHead.measure(widthMeasureSpec, heightMeasureSpec);

		final int rotateAngle = -180;

		mRotateAnimation = new RotateAnimation(0, rotateAngle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mRotateAnimation.setInterpolator(new LinearInterpolator());
		mRotateAnimation.setDuration(150);
		mRotateAnimation.setFillAfter(true);

		mResetRotateAnimation = new RotateAnimation(rotateAngle, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mResetRotateAnimation.setInterpolator(new LinearInterpolator());
		mResetRotateAnimation.setDuration(150);
		mResetRotateAnimation.setFillAfter(true);

		mPreferences = context.getSharedPreferences("refresh_time", Context.MODE_PRIVATE);
		mKey = key;
	}
	
	public RefreshLoadingLayoutImp(Context context, String key ,int color) {
		mHead = View.inflate(context, R.layout.refresh_header, null);
		head_arrow = (ImageView) mHead.findViewById(R.id.head_arrow);
		head_progress_bar = (ProgressBar) mHead.findViewById(R.id.head_progress_bar);
		head_tips_tv = (TextView) mHead.findViewById(R.id.head_tips_tv);
		head_last_time_tv = (TextView) mHead.findViewById(R.id.head_last_time_tv);
		head_contentLayout = (RelativeLayout) mHead.findViewById(R.id.head_contentLayout);
		head_contentLayout.setBackgroundColor(color);
		int widthMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.MATCH_PARENT, MeasureSpec.AT_MOST);
		int heightMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
		mHead.measure(widthMeasureSpec, heightMeasureSpec);
		
		final int rotateAngle = -180;

		mRotateAnimation = new RotateAnimation(0, rotateAngle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mRotateAnimation.setInterpolator(new LinearInterpolator());
		mRotateAnimation.setDuration(150);
		mRotateAnimation.setFillAfter(true);

		mResetRotateAnimation = new RotateAnimation(rotateAngle, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mResetRotateAnimation.setInterpolator(new LinearInterpolator());
		mResetRotateAnimation.setDuration(150);
		mResetRotateAnimation.setFillAfter(true);

		mPreferences = context.getSharedPreferences("refresh_time", Context.MODE_PRIVATE);
		mKey = key;
	}

	private String getRefreshTime() {
		String lastTime;
		lastTime = mPreferences.getString(mKey, "");
		if (!TextUtils.isEmpty(lastTime)) {
			lastTime = "最近更新:  " + lastTime;
		}
		return lastTime;
	}

	@Override
	public int getLayoutSize() {
		return mHead.getMeasuredHeight();
	}

	@Override
	public View getContentView() {
		return mHead;
	}

	@Override
	public void onPull(float scaleOfLayout) {
	}

	@Override
	public void onReset() {
		isTriggerRelease = false;
		head_arrow.clearAnimation();
		mHead.setVisibility(View.GONE);
		head_progress_bar.setVisibility(View.GONE);
		head_last_time_tv.setVisibility(View.GONE);
		head_arrow.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPullToRefresh() {
		if (mHead.getVisibility() != View.VISIBLE) {
			mHead.setVisibility(View.VISIBLE);
		}

		if (isTriggerRelease) {
			head_arrow.startAnimation(mResetRotateAnimation);
			isTriggerRelease = false;
		}
		head_tips_tv.setText("下拉刷新");
		String lastTime = getRefreshTime();
		if (!TextUtils.isEmpty(lastTime)) {
			head_last_time_tv.setVisibility(View.VISIBLE);
			head_last_time_tv.setText(lastTime);
		}
	}

	@Override
	public void onReleaseToRefresh() {
		isTriggerRelease = true;
		head_arrow.startAnimation(mRotateAnimation);
		head_tips_tv.setText("释放立即刷新");
	}

	@Override
	public void onRefreshing() {
		if (mHead.getVisibility() != View.VISIBLE) {
			mHead.setVisibility(View.VISIBLE);
		}
		head_arrow.clearAnimation();
		head_arrow.setVisibility(View.GONE);
		head_progress_bar.setVisibility(View.VISIBLE);
		head_tips_tv.setText("正在刷新...");
		String lastTime = getRefreshTime();
		if (!TextUtils.isEmpty(lastTime)) {
			head_last_time_tv.setVisibility(View.VISIBLE);
			head_last_time_tv.setText(lastTime);
		}
	}

}
