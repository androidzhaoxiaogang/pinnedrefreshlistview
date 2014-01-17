package com.android.pinnedrefreshlist;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;

public class PullListView extends ListView implements OnScrollListener {
	static final float FRICTION = 2.0f;
	static final int SMOOTH_SCROLL_DURATION_MS = 200;
	private LoadingViewWrapper mTopLoadingWrapper;
	private boolean mIsBeingDragged = false;
	private float mLastMotionY;
	private float mInitialMotionY;
	private State mState = State.RESET;
	private SmoothScrollRunnable mCurrentSmoothScrollRunnable = null;
	private Interpolator mScrollAnimationInterpolator = null;
	private OnRefreshListener mOnRefreshListener = null;
	private boolean mIsReseting = false;

	private OnScrollListener mOnScrollListener;

	private  static PinnedSectionedHeaderAdapter mAdapter;
	private View mCurrentHeader;
	private int mCurrentHeaderViewType = 0;
	private float mHeaderOffset;
	private boolean mShouldPin = true;
	private int  mCurrentSection = 0;

	public void setOnRefreshListener(OnRefreshListener listener) {
		mOnRefreshListener = listener;
	}

	public PullListView(Context context) {
		super(context);
		super.setOnScrollListener(this);
		init(context, null);
	}

	public PullListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setOnScrollListener(this);
		init(context, attrs);
	}

	public PullListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		super.setOnScrollListener(this);
		init(context, attrs);
	}

	/**
	 * 设置loading的背景色
	 * 
	 * @param color
	 */
	public final void setTopLoadingBackColor(int color) {
		mTopLoadingWrapper.setRefreshBackgroundColor(color);
	}

	protected LoadingViewWrapper createLoadingLayout(Context context) {
		LoadingViewWrapper layout = new LoadingViewWrapper(context);
		return layout;
	}

	/**
	 * 处理AttributSet样式
	 * 
	 * @param attrs
	 */
	protected void handleStyledAttributes(AttributeSet attrs) {
	}

	/**
	 * 初始化
	 * 
	 * @param context
	 * @param attrs
	 */
	private void init(Context context, AttributeSet attrs) {
		mTopLoadingWrapper = createLoadingLayout(context);
		handleStyledAttributes(attrs);
		super.addHeaderView(mTopLoadingWrapper, null, false);
	}

	@Override
	public void addHeaderView(View v) {
		mTopLoadingWrapper.addHeadView(v);
		refreshLoadingSize();
	}

	public final void setTopLoadingLayout(ILoadingLayout loadinglayout) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mTopLoadingWrapper.setLoadingInner(loadinglayout, params);
		refreshLoadingSize();
	}

	/**
	 * 取Scroll的最大距离
	 * 
	 * @return
	 */
	protected int getMaximumPullScroll() {
		return Math.round(getHeight() / FRICTION);
	}

	/**
	 * loading 的大小重新刷新一下
	 */
	protected final void refreshLoadingSize() {
		int loadingHeight = mTopLoadingWrapper.getLoadingHeight();
		mTopLoadingWrapper.layoutLoading(-loadingHeight);
	}

	protected boolean isReadyForPullStart() {
		return isFirstItemVisible();
	}

	private boolean isFirstItemVisible() {
		final Adapter adapter = getAdapter();
		if (null == adapter || adapter.isEmpty()) {
			return true;
		} else {
			if (getFirstVisiblePosition() < 1) {
				final View firstVisibleChild = getChildAt(0);
				if (firstVisibleChild != null) {
					return firstVisibleChild.getTop() >= 0;
				}
			}
		}
		return false;
	}

	public final boolean isRefreshing() {
		return mState == State.REFRESHING || mState == State.MANUAL_REFRESHING;
	}

	public final boolean isReset() {
		return mState == State.RESET;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastMotionY = mInitialMotionY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			mLastMotionY = event.getY();
			if (mIsBeingDragged || isReadyForPullStart()) {
				mIsBeingDragged = true;
				pullEvent();
			}
			mInitialMotionY = mLastMotionY;
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mIsBeingDragged = false;
			checkScroll();
			break;
		}
		return super.dispatchTouchEvent(event);
	}

	final void checkScroll() {
		if (mState == State.RELEASE_TO_REFRESH) {
			setState(State.REFRESHING, true);
		} else {
			int loadingPadding = mTopLoadingWrapper.getLoadingPaddingTop();
			if (isRefreshing() && loadingPadding > 0) {
				if (!mIsReseting)
					smoothScrollTo(0, null);
			} else if (isRefreshing()) {
				// 不动
			} else {
				setState(State.RESET);
			}
		}
	}

	final void setState(State state, final boolean... params) {
		switch (state) {
		case RESET:
			onReset();
			break;
		case PULL_TO_REFRESH:
			mState = state;
			onPullToRefresh();
			break;
		case RELEASE_TO_REFRESH:
			mState = state;
			onReleaseToRefresh();
			break;
		case REFRESHING:
		case MANUAL_REFRESHING:
			onRefreshing(params[0], state);
			break;
		case OVERSCROLLING:
			// NO-OP
			mState = state;
			break;
		}
	}

	/**
	 * refresh完成
	 */
	public final void onRefreshComplete() {
		if (isRefreshing()) {
			setState(State.RESET);
		}
	}

	public final void setRefreshing(boolean doScroll) {
		if (mState == State.RESET) {
			setState(State.MANUAL_REFRESHING, doScroll);
		}
	}

	private void setScrollPadding(int toPadding) {
		mTopLoadingWrapper.layoutLoading(toPadding);
	}

	@Override
	protected final void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		refreshLoadingSize();
		post(new Runnable() {
			@Override
			public void run() {
				requestLayout();
			}
		});
	}

	/**
	 * 处理事件
	 */
	private void pullEvent() {
		final int delayY;
		delayY = Math.round((mLastMotionY - mInitialMotionY) / FRICTION);
		if (delayY != 0) {
			mTopLoadingWrapper.moveDY(delayY);
			if (!isRefreshing()) {
				// 不是loading中
				int loadingPadding = mTopLoadingWrapper.getLoadingPaddingTop();
				if (mState != State.PULL_TO_REFRESH
						&& loadingPadding > -mTopLoadingWrapper
								.getLoadingHeight() && loadingPadding <= 0) {
					setState(State.PULL_TO_REFRESH);
				} else if (mState == State.PULL_TO_REFRESH
						&& loadingPadding > 0) {
					setState(State.RELEASE_TO_REFRESH);
				}
			}
		}
	}

	protected void onReset() {
		if (mIsReseting)
			return;
		mIsReseting = true;
		final OnSmoothScrollFinishedListener finishListener = new OnSmoothScrollFinishedListener() {
			@Override
			public void onSmoothScrollFinished() {
				mState = State.RESET;
				mTopLoadingWrapper.reset();
				mIsReseting = false;
			}
		};
		smoothScrollTo(-mTopLoadingWrapper.getLoadingHeight(), finishListener);
	}

	protected void onPullToRefresh() {
		mTopLoadingWrapper.pullToRefresh();
	}

	protected void onReleaseToRefresh() {
		mTopLoadingWrapper.releaseToRefresh();
	}

	protected void onRefreshing(final boolean doScroll, final State state) {
		if (mIsReseting)
			return;
		if (doScroll) {
			OnSmoothScrollFinishedListener listener = new OnSmoothScrollFinishedListener() {
				@Override
				public void onSmoothScrollFinished() {
					callRefreshListener(state);
				}
			};
			smoothScrollTo(0, listener);
		} else {
			callRefreshListener(state);
		}
	}

	private void callRefreshListener(final State state) {
		mState = state;
		mTopLoadingWrapper.refreshing();
		if (null != mOnRefreshListener) {
			mOnRefreshListener.onRefresh();
		}
	}

	/**
	 * 
	 * @param newScrollValue
	 * @param listener
	 */
	private final void smoothScrollTo(int toPadding,
			OnSmoothScrollFinishedListener listener) {
		if (null != mCurrentSmoothScrollRunnable) {
			mCurrentSmoothScrollRunnable.stop();
		}
		final int oldScrollValue = mTopLoadingWrapper.getLoadingPaddingTop();
		if (oldScrollValue != toPadding) {
			if (null == mScrollAnimationInterpolator) {
				mScrollAnimationInterpolator = new DecelerateInterpolator();
			}
			mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(
					oldScrollValue, toPadding, SMOOTH_SCROLL_DURATION_MS,
					listener);
			post(mCurrentSmoothScrollRunnable);
		} else {
			if (listener != null)
				listener.onSmoothScrollFinished();
		}
	}

	final class SmoothScrollRunnable implements Runnable {
		private final Interpolator mInterpolator;
		private final int mScrollToY;
		private final int mScrollFromY;
		private final long mDuration;
		private OnSmoothScrollFinishedListener mListener;

		private boolean mContinueRunning = true;
		private long mStartTime = -1;
		private int mCurrentY = -1;

		public SmoothScrollRunnable(int fromY, int toY, long duration,
				OnSmoothScrollFinishedListener listener) {
			mScrollFromY = fromY;
			mScrollToY = toY;
			mInterpolator = mScrollAnimationInterpolator;
			mDuration = duration;
			mListener = listener;
		}

		@Override
		public void run() {
			/**
			 * Only set mStartTime if this is the first time we're starting,
			 * else actually calculate the Y delta
			 */
			if (mStartTime == -1) {
				mStartTime = System.currentTimeMillis();
			} else {
				/**
				 * We do do all calculations in long to reduce software float
				 * calculations. We use 1000 as it gives us good accuracy and
				 * small rounding errors
				 */
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime))
						/ mDuration;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);
				final int deltaY = Math.round((mScrollFromY - mScrollToY)
						* mInterpolator
								.getInterpolation(normalizedTime / 1000f));
				mCurrentY = mScrollFromY - deltaY;
				setScrollPadding(mCurrentY);
			}
			// If we're not at the target Y, keep going...
			if (mContinueRunning && mScrollToY != mCurrentY) {
				ViewCompat.postOnAnimation(PullListView.this, this);
			} else {
				if (null != mListener) {
					mListener.onSmoothScrollFinished();
				}
			}
		}

		public void stop() {
			mContinueRunning = false;
			removeCallbacks(this);
		}

		public boolean isRunning() {
			return mContinueRunning;
		}
	}

	public static enum State {

		/**
		 * When the UI is in a state which means that user is not interacting
		 * with the Pull-to-Refresh function.
		 */
		RESET(0x0),

		/**
		 * When the UI is being pulled by the user, but has not been pulled far
		 * enough so that it refreshes when released.
		 */
		PULL_TO_REFRESH(0x1),

		/**
		 * When the UI is being pulled by the user, and <strong>has</strong>
		 * been pulled far enough so that it will refresh when released.
		 */
		RELEASE_TO_REFRESH(0x2),

		/**
		 * When the UI is currently refreshing, caused by a pull gesture.
		 */
		REFRESHING(0x8),

		/**
		 * When the UI is currently refreshing, caused by a call to
		 * {@link PullToRefreshBase#setRefreshing() setRefreshing()}.
		 */
		MANUAL_REFRESHING(0x9),

		/**
		 * When the UI is currently overscrolling, caused by a fling on the
		 * Refreshable View.
		 */
		OVERSCROLLING(0x10);

		/**
		 * Maps an int to a specific state. This is needed when saving state.
		 * 
		 * @param stateInt
		 *            - int to map a State to
		 * @return State that stateInt maps to
		 */
		static State mapIntToValue(final int stateInt) {
			for (State value : State.values()) {
				if (stateInt == value.getIntValue()) {
					return value;
				}
			}

			// If not, return default
			return RESET;
		}

		private int mIntValue;

		State(int intValue) {
			mIntValue = intValue;
		}

		int getIntValue() {
			return mIntValue;
		}
	}

	interface OnSmoothScrollFinishedListener {
		void onSmoothScrollFinished();
	}

	public interface OnRefreshListener {

		/**
		 * onRefresh will be called for both a Pull from start, and Pull from
		 * end
		 */
		public void onRefresh();

	}

	public void setPinHeaders(boolean shouldPin) {
		mShouldPin = shouldPin;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		mAdapter = (PinnedSectionedHeaderAdapter) adapter;
		super.setAdapter(adapter);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(view, firstVisibleItem,
					visibleItemCount, totalItemCount);
		}

		if (mAdapter == null || mAdapter.getCount() == 0 || !mShouldPin
				|| (firstVisibleItem < getHeaderViewsCount())) {
			mCurrentHeader = null;
			mHeaderOffset = 0.0f;
			for (int i = firstVisibleItem; i < firstVisibleItem
					+ visibleItemCount; i++) {
				View header = getChildAt(i);
				if (header != null) {
					header.setVisibility(VISIBLE);
				}
			}
			return;
		}

		firstVisibleItem -= getHeaderViewsCount();

		int section = mAdapter.getSectionForPosition(firstVisibleItem);
		
		int viewType = mAdapter.getSectionHeaderViewType(section);
		mCurrentHeader = getSectionHeaderView(section,
				mCurrentHeaderViewType != viewType ? null : mCurrentHeader);
		ensurePinnedHeaderLayout(mCurrentHeader);
		mCurrentHeaderViewType = viewType;

		mHeaderOffset = 0.0f;

		for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
			if (mAdapter.isSectionHeader(i)) {
				View header = getChildAt(i - firstVisibleItem);
				float headerTop = header.getTop();
				float pinnedHeaderHeight = mCurrentHeader.getMeasuredHeight();
				header.setVisibility(VISIBLE);
				if (pinnedHeaderHeight >= headerTop && headerTop > 0) {
					mHeaderOffset = headerTop - header.getHeight();
				} else if (headerTop <= 0) {
					header.setVisibility(INVISIBLE);
				}
			}
		}

		invalidate();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mOnScrollListener != null) {
			mOnScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	private View getSectionHeaderView(int section, View oldView) {
		boolean shouldLayout = section != mCurrentSection || oldView == null;

		View view = mAdapter.getSectionHeaderView(section, oldView, this);
		if (shouldLayout) {
			// a new section, thus a new header. We should lay it out again
			ensurePinnedHeaderLayout(view);
			mCurrentSection = section;
		}
		return view;
	}

	private void ensurePinnedHeaderLayout(View header) {
		if (header.isLayoutRequested()) {
			int widthSpec = MeasureSpec.makeMeasureSpec(getWidth(),
					MeasureSpec.EXACTLY);
			int heightSpec;
			ViewGroup.LayoutParams layoutParams = header.getLayoutParams();
			if (layoutParams != null && layoutParams.height > 0) {
				heightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height,
						MeasureSpec.EXACTLY);
			} else {
				heightSpec = MeasureSpec.makeMeasureSpec(0,
						MeasureSpec.UNSPECIFIED);
			}
			header.measure(widthSpec, heightSpec);
			int height = header.getMeasuredHeight();
			header.layout(0, 0, getWidth(), height);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mAdapter == null || !mShouldPin || mCurrentHeader == null)
			return;
		int saveCount = canvas.save();
		canvas.translate(0, mHeaderOffset);
		canvas.clipRect(0, 0, getWidth(), mCurrentHeader.getMeasuredHeight()); // needed
		// for
		// <
		// HONEYCOMB
		mCurrentHeader.draw(canvas);
		canvas.restoreToCount(saveCount);
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		mOnScrollListener = l;
	}

	public void setOnItemClickListener(
			PullListView.OnItemClickListener listener) {
		super.setOnItemClickListener(listener);
	}
}
