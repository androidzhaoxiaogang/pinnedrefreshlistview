/**
 * 
 */
package com.android.pinnedrefreshlist;

import android.view.View;

public interface ILoadingLayout {

	public int getLayoutSize();

	public View getContentView();

	public void onPull(float scaleOfLayout);

	public void onReset();

	public void onPullToRefresh();

	public void onReleaseToRefresh();

	public void onRefreshing();
}
