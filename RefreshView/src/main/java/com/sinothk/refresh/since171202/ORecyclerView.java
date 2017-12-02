package com.sinothk.refresh.since171202;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.sinothk.refresh.R;


/**
 * Created by limxing on 16/7/23.
 * <p>
 * https://github.com/limxing
 * Blog: http://www.leefeng.me
 */
public class ORecyclerView extends RecyclerView {
    private Scroller mScroller;
    private ORecyclerViewAdapter oAdapter;
    private boolean isLoadMore;
    private boolean isRefresh = true;
    private OnItemClickListener itemListener;
    private OnItemLongClickListener itemLongClickListener;

    private float mLastY;//上一次Y 值
    private ORecyclerViewFooter recyclerViewFooter;
    private ORecyclerViewHeader recyclerViewHeader;
    private boolean mPullRefreshing;//是否正在刷新
    private boolean mPullLoading;//是否正在加载更多
    private RelativeLayout mHeaderViewContent;
    private int mHeaderViewHeight;
    private GridLayoutManager layoutManager;//
    private final static float OFFSET_RADIO = 1.8f;
    private final static int PULL_LOAD_MORE_DELTA = 50;
    private final static int SCROLL_DURATION = 400;
    private int mScrollBack;
    private final static int SCROLLBACK_HEADER = 4;
    private final static int SCROLLBACK_FOOTER = 3;
    private ORecyclerViewListener mRecyclerViewListener;
    private boolean mPullLoad;
    private TextView mHeaderTimeView;
    private boolean isNoDateShow = false;
    private ORecyclerViewScrollChange scrollerListener;//滑动监听
    private boolean isAutoLoadMore;

    /*添加头*/
    private View headerView;
    private Adapter adapter;
    private oAdapterDataObserver observer;
    private View footView;

    public void setHeaderView(View headerView) {
        this.headerView = headerView;
        if (oAdapter != null) {
            oAdapter.setHeaderView(headerView);
        }
    }

    public void setFootView(View footView) {
        this.footView = footView;
        if (oAdapter != null) {
            oAdapter.setFootView(footView);
        }
    }

    public ORecyclerView(Context context) {
        super(context);
        initWithContext(context);
    }

    public ORecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWithContext(context);
    }

    public ORecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWithContext(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        if (adapter!=null){
//            adapter.unregisterAdapterDataObserver(observer);
//        }
//        mScroller=null;
//        oAdapter=null;
//        itemListener=null;
//        recyclerViewFooter=null;
//        recyclerViewHeader=null;
//        mHeaderViewContent=null;
//        layoutManager=null;
//        mRecyclerViewListener=null;
//        mHeaderTimeView=null;
//        scrollerListener=null;
//        headerView=null;
//        adapter=null;
//        observer=null;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        if (observer == null)
            observer = new oAdapterDataObserver();
        adapter.registerAdapterDataObserver(observer);
        oAdapter = new ORecyclerViewAdapter(getContext(), adapter);
        oAdapter.setRecyclerViewHeader(recyclerViewHeader);
        oAdapter.setRecyclerViewFooter(recyclerViewFooter);
        if (headerView != null) {
            oAdapter.setHeaderView(headerView);
        }
        if (footView != null) {
            oAdapter.setHeaderView(footView);
        }
        oAdapter.setLoadMore(isLoadMore);
        oAdapter.setRefresh(isRefresh);

        if (itemListener != null) {
            oAdapter.setOnItemClickListener(itemListener);
        }
        if (itemLongClickListener != null) {
            oAdapter.setOnItemLongClickListener(itemLongClickListener);
        }

        super.setAdapter(oAdapter);

    }

    /**
     * 更新刷新头高度
     *
     * @param delta
     */
    private void updateHeaderHeight(float delta) {
        recyclerViewHeader.setVisiableHeight((int) delta
                + recyclerViewHeader.getVisiableHeight());
        if (isRefresh && !mPullRefreshing) { // 未处于刷新状态，更新箭头
            if (recyclerViewHeader.getVisiableHeight() > mHeaderViewHeight) {
                recyclerViewHeader.setState(ORecyclerViewHeader.STATE_READY);
            } else {
                recyclerViewHeader.setState(ORecyclerViewHeader.STATE_NORMAL);
            }
        }
    }

    /**
     * reset header view's height.
     */
    private void resetHeaderHeight() {
        final int height = recyclerViewHeader.getVisiableHeight();
        if (height == 0)
            return;
        if (mPullRefreshing && height <= mHeaderViewHeight) {
            return;
        }
        int finalHeight = 0;
        if (mPullRefreshing && height > mHeaderViewHeight) {
            finalHeight = mHeaderViewHeight;
        }

        mScrollBack = SCROLLBACK_HEADER;
        mScroller.startScroll(0, height, 0, finalHeight - height,
                SCROLL_DURATION);
        invalidate();
    }


    /**
     * 用于重置头或尾的高度
     */
    @Override
    public void computeScroll() {
//        if (mScroller==null){
//            initWithContext(getContext());
//        }
        if (mScroller != null && mScroller.computeScrollOffset()) {
            if (mScrollBack == SCROLLBACK_HEADER) {
                recyclerViewHeader.setVisiableHeight(mScroller.getCurrY());
            } else {
                recyclerViewFooter.setBottomMargin(mScroller.getCurrY());
            }
            postInvalidate();
        }
        super.computeScroll();
    }


    public void stopRefresh(boolean isSuccess) {
//        oAdapter.notifyDataSetChanged();
        if (mPullRefreshing) {
            if (isSuccess) {
                recyclerViewHeader.setState(ORecyclerViewHeader.STATE_SUCCESS);
            } else {
                recyclerViewHeader.setState(ORecyclerViewHeader.STATE_FRESH_FAILT);
            }
            recyclerViewHeader.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullRefreshing = false;
                    resetHeaderHeight();
                }
            }, 1000);

        }
    }

    /**
     * 更新底部加载更多高度
     *
     * @param delta
     */
    private void updateFooterHeight(float delta) {
        int height = recyclerViewFooter.getBottomMargin() + (int) delta;
        if (isLoadMore) {
            if (height > PULL_LOAD_MORE_DELTA) {
                recyclerViewFooter.setState(ORecyclerViewFooter.STATE_READY);
                mPullLoading = true;
            } else {
                recyclerViewFooter.setState(ORecyclerViewFooter.STATE_NORMAL);
                mPullLoading = false;
                mPullLoad = false;
            }
        }
        recyclerViewFooter.setBottomMargin(height);
    }

    private void resetFooterHeight() {
        int bottomMargin = recyclerViewFooter.getBottomMargin();
        if (bottomMargin > 0) {
            mScrollBack = SCROLLBACK_FOOTER;
            mScroller.startScroll(0, bottomMargin, 0, -bottomMargin,
                    SCROLL_DURATION);
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLastY == -1 || mLastY == 0) {
            mLastY = ev.getRawY();
            if (!mPullRefreshing && layoutManager.findFirstVisibleItemPosition() <= 1) {
                recyclerViewHeader.refreshUpdatedAtValue();
            }
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();

                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = ev.getRawY() - mLastY;
                mLastY = ev.getRawY();
                if (isRefresh && !mPullLoad && layoutManager.findFirstVisibleItemPosition() <= 1 &&
                        (recyclerViewHeader.getVisiableHeight() > 0 || moveY > 0)) {
                    updateHeaderHeight(moveY / OFFSET_RADIO);
                } else if (isLoadMore && !mPullRefreshing && !mPullLoad &&
                        layoutManager.findLastVisibleItemPosition() == oAdapter.getItemCount() - 1 &&
                        (recyclerViewFooter.getBottomMargin() > 0 || moveY < 0) && adapter.getItemCount() > 0) {
                    updateFooterHeight(-moveY / OFFSET_RADIO);
                }
                break;
            case MotionEvent.ACTION_UP:
                mLastY = -1; // reset
                if (!mPullRefreshing && layoutManager.findFirstVisibleItemPosition() == 0) {
                    // invoke refresh
                    if (isRefresh
                            && recyclerViewHeader.getVisiableHeight() > mHeaderViewHeight) {
                        mPullRefreshing = true;
                        recyclerViewHeader.setState(ORecyclerViewHeader.STATE_REFRESHING);
                        if (mRecyclerViewListener != null) {
                            mRecyclerViewListener.onRefresh();
                        }
                    }

                }
                if (isLoadMore && mPullLoading && layoutManager.findLastVisibleItemPosition() == oAdapter.getItemCount() - 1
                        && recyclerViewFooter.getBottomMargin() > PULL_LOAD_MORE_DELTA
                        ) {
                    recyclerViewFooter.setState(ORecyclerViewFooter.STATE_LOADING);
                    mPullLoad = true;
                    startLoadMore();
                }
                resetHeaderHeight();
                resetFooterHeight();
                break;
        }

        return super.onTouchEvent(ev);
    }

    private void startLoadMore() {
        if (mRecyclerViewListener != null) {
            recyclerViewFooter.setState(ORecyclerViewFooter.STATE_LOADING);
            mRecyclerViewListener.onLoadMore();

        }
    }

    /**
     * stop load more, reset footer view.
     */
    public void stopLoadMore() {
//        oAdapter.notifyDataSetChanged();
        if (mPullLoading) {
            mPullLoad = false;
            mPullLoading = false;
            recyclerViewFooter.setState(ORecyclerViewFooter.STATE_NORMAL);
            resetFooterHeight();
        }
    }


    private void initWithContext(Context context) {
        mScroller = new Scroller(context, new DecelerateInterpolator());
        recyclerViewHeader = new ORecyclerViewHeader(context);
        recyclerViewFooter = new ORecyclerViewFooter(context);
        mHeaderTimeView = (TextView) recyclerViewHeader
                .findViewById(R.id.lfrecyclerview_header_time);
        mHeaderViewContent = (RelativeLayout) recyclerViewHeader
                .findViewById(R.id.lfrecyclerview_header_content);
        recyclerViewHeader.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mHeaderViewHeight = mHeaderViewContent.getHeight();
                        getViewTreeObserver()
                                .removeGlobalOnLayoutListener(this);
                    }
                });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 1);
        setLayoutManager(gridLayoutManager);
//        setOnScrollChangeListener(this);
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
                onScrollChange(recyclerView, dx, dy);
            }
        });
        observer = new oAdapterDataObserver();
    }

   private class oAdapterDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
//            super.onChanged();
            oAdapter.notifyDataSetChanged();
        }


        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
//            super.onItemRangeChanged(positionStart + oAdapter.getheaderViewCount(), itemCount);
            oAdapter.notifyItemRangeChanged(positionStart + oAdapter.getheaderViewCount(), itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
//            super.onItemRangeChanged(positionStart + oAdapter.getheaderViewCount(), itemCount, payload);
            oAdapter.notifyItemRangeChanged(positionStart + oAdapter.getheaderViewCount(), itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
//            super.onItemRangeInserted(positionStart + oAdapter.getheaderViewCount(), itemCount);
            oAdapter.notifyItemRangeInserted(positionStart + oAdapter.getheaderViewCount(), itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
//            super.onItemRangeRemoved(positionStart + oAdapter.getheaderViewCount(), itemCount);
            oAdapter.notifyItemRangeRemoved(positionStart + oAdapter.getheaderViewCount(), itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
//            super.onItemRangeMoved(fromPosition + oAdapter.getheaderViewCount(), toPosition + oAdapter.getheaderViewCount(), itemCount);
            oAdapter.notifyItemMoved(fromPosition + oAdapter.getheaderViewCount(), toPosition + oAdapter.getheaderViewCount());
        }
    }


    public void setLoadMore(boolean b) {
        this.isLoadMore = b;
        if (!isLoadMore) {
            recyclerViewFooter.hide();
        }

    }

    public void setRefresh(boolean b) {
        this.isRefresh = b;


    }

    public void setOnItemClickListener(OnItemClickListener itemListener) {
        this.itemListener = itemListener;
        if (this.itemListener != null && oAdapter != null) {
            oAdapter.setOnItemClickListener(this.itemListener);
        }
    }

    public void setOnItemLongClickListener(OnItemLongClickListener itemLongClickListener) {
        this.itemLongClickListener = itemLongClickListener;
        if (this.itemLongClickListener != null && oAdapter != null) {
            oAdapter.setOnItemLongClickListener(this.itemLongClickListener);
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layoutManager) {
        super.setLayoutManager(layoutManager);
        this.layoutManager = (GridLayoutManager) layoutManager;
    }


    public void setORecyclerViewListener(ORecyclerViewListener l) {
        mRecyclerViewListener = l;
    }

    public void setAutoLoadMore(boolean autoLoadMore) {
        isAutoLoadMore = autoLoadMore;
    }

    private int currentLastNum;//自动加载一次

    private int num;

    /**
     * @param view view
     * @param i    i
     * @param i1   ii
     */
    public void onScrollChange(View view, int i, int i1) {

        if (oAdapter.itemHeight > 0 && num == 0) {
            num = (int) Math.ceil(getHeight() / oAdapter.itemHeight);
        }
        if (isAutoLoadMore && (layoutManager.findLastVisibleItemPosition() == oAdapter.getItemCount()
                - 1)
                && currentLastNum != layoutManager.findLastVisibleItemPosition()
                && num > 0 && adapter.getItemCount() > num
                && !mPullLoading) {

            currentLastNum = layoutManager.findLastVisibleItemPosition();
            mPullLoading = true;
            startLoadMore();
        }
        if (scrollerListener != null) {
            scrollerListener.onORecyclerViewScrollChange(view, i, i1);
        }
    }

    public interface ORecyclerViewScrollChange {
        void onORecyclerViewScrollChange(View view, int i, int i1);
    }

    /**
     * 设置滑动监听
     *
     * @param listener jianting
     */
    public void setScrollChangeListener(ORecyclerViewScrollChange listener) {
        this.scrollerListener = listener;
    }


    /**
     * implements this interface to get refresh/load more event.
     */
    public interface ORecyclerViewListener {
        void onRefresh();

        void onLoadMore();
    }


    /**
     * Set hide time
     * 设置隐藏时间
     */
    public void hideTimeView() {
        mHeaderTimeView.setVisibility(View.GONE);
    }

    /**
     * 设置底部文字
     *
     * @param text wenzi
     */
    public void setFootText(String text) {
        recyclerViewFooter.getmHintView().setText(text);
    }

    /**
     * 设置头部文字
     *
     * @param header wenzi
     */
    public void setHeaderText(String header) {
        recyclerViewHeader.getmHintTextView().setText(header);
    }

    /**
     * 设置是否没有数据时显示底部提示
     */
    public void setNoDateShow() {
        this.isNoDateShow = true;
    }

    /**
     * notification的时候调用
     */
    @Override
    public void requestLayout() {
        super.requestLayout();
        if (recyclerViewFooter == null || oAdapter == null || !isNoDateShow) {
            return;
        }
        boolean b = oAdapter.getItemCount() <= (oAdapter.getHFCount());
        recyclerViewFooter.setNoneDataState(b);
        if (b) {
            recyclerViewFooter.hide();
        } else {
            recyclerViewFooter.show();
        }
        if (!isLoadMore) {
            recyclerViewFooter.hide();
        }
    }


}