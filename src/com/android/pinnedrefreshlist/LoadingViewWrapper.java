package com.android.pinnedrefreshlist;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;


public class LoadingViewWrapper extends LinearLayout {
	private ILoadingLayout mLoadingInner = null;
	private LinearLayout mLoadingContainer = null;
	private LinearLayout mHeadContainer = null;

	public LoadingViewWrapper(Context context) {
		super(context);
		setOrientation(VERTICAL);
		mLoadingContainer = new LinearLayout(context);
		mLoadingContainer.setOrientation(VERTICAL);
		addView(mLoadingContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mHeadContainer = new LinearLayout(context);
		mHeadContainer.setOrientation(VERTICAL);
		addView(mHeadContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	}

	public int getLoadingPaddingTop() {
		return mLoadingContainer.getPaddingTop();
	}

    public void setRefreshBackgroundColor(int color){
    	mLoadingContainer.setBackgroundColor(color);
    }

	public void moveDY(int delayY) {
		int destTop = mLoadingContainer.getPaddingTop() + delayY;
		if (destTop < -getLoadingHeight()) {
			layoutLoading(-getLoadingHeight());
		} else {
			layoutLoading(destTop);
		}
	}

	public void layoutLoading(int height) {
		mLoadingContainer.setPadding(0, height, 0, 0);
		requestLayout();
	}

	public int getHeadHeight() {
		return mHeadContainer.getMeasuredHeight();
	}

	public int getLoadingHeight() {
		if (mLoadingInner == null) {
			return 0;
		}
		return mLoadingInner.getLayoutSize();
	}

	public final int getContentSize() {
		if (mLoadingInner == null)
			return 0;
		return mLoadingInner.getLayoutSize();
	}

	public void setLoadingInner(ILoadingLayout loadingInner, LinearLayout.LayoutParams params) {
		this.mLoadingInner = loadingInner;
		mLoadingContainer.addView(loadingInner.getContentView(), params);
	}

	public void addHeadView(View head) {
		mHeadContainer.addView(head, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	}

	public final void onPull(float scaleOfLayout) {
		if (mLoadingInner != null) {
			mLoadingInner.onPull(scaleOfLayout);
		}
	}

	public final void reset() {
		if (mLoadingInner != null) {
			mLoadingInner.onReset();
		}
	}

	public final void pullToRefresh() {
		if (mLoadingInner != null) {
			mLoadingInner.onPullToRefresh();
		}
	}

	public final void releaseToRefresh() {
		if (mLoadingInner != null) {
			mLoadingInner.onReleaseToRefresh();
		}
	}

	public final void refreshing() {
		if (mLoadingInner != null) {
			mLoadingInner.onRefreshing();
		}
	}
}
