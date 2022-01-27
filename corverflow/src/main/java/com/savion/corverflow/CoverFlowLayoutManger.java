package com.savion.corverflow;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Cover Flow布局类
 * <p>通过重写LayoutManger布局方法{@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)}
 * 对Item进行布局，并对超出屏幕的Item进行回收
 * <p>通过重写LayoutManger中的{@link #scrollHorizontallyBy(int, RecyclerView.Recycler, RecyclerView.State)}
 * 进行水平滚动处理
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version V1.1：
 * 增加循环滚动功能
 * @Datetime 2020-06-09
 */

public class CoverFlowLayoutManger extends RecyclerView.LayoutManager {

    /**
     * item orientation==HORIZONTAL时表示向右移动
     * orientation==VERTICAL时表示向下移动
     */
    private final static int SCROLL_TO_END = 1;

    /**
     * item orientation==HORIZONTAL时表示向左移动
     * orientation==VERTICAL时表示向上移动
     */
    private final static int SCROLL_TO_START = 2;

    /**
     * 最大存储item信息存储数量，
     * 超过设置数量，则动态计算来获取
     */
    private final static int MAX_RECT_COUNT = 100;

    /**
     * 滑动总偏移量
     */
    private int mOffsetAll = 0;

    /**
     * Item宽
     */
    private int mDecoratedChildWidth = 0;

    /**
     * Item高
     */
    private int mDecoratedChildHeight = 0;

    /**
     * Item间隔与item宽的比例
     */
    private float mIntervalRatio = 0.5f;

    /**
     * 起始ItemX坐标
     */
    private int mStartX = 0;

    /**
     * 起始Item Y坐标
     */
    private int mStartY = 0;

    /**
     * 保存所有的Item的上下左右的偏移量信息
     */
    private SparseArray<Rect> mAllItemFrames = new SparseArray<>();

    /**
     * 记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
     */
    private SparseBooleanArray mHasAttachedItems = new SparseBooleanArray();

    /**
     * RecyclerView的Item回收器
     */
    private RecyclerView.Recycler mRecycle;

    /**
     * RecyclerView的状态器
     */
    private RecyclerView.State mState;

    /**
     * 滚动动画
     */
    private ValueAnimator mAnimation;

    /**
     * 正显示在中间的Item
     */
    private int mSelectPosition = 0;
    private int mSelectPositionNature = 0;

    /**
     * 前一个正显示在中间的Item
     */
    private int mLastSelectPosition = 0;

    /**
     * 选中监听
     */
    private OnSelected mSelectedListener;

    /**
     * 是否为平面滚动，Item之间没有叠加，也没有缩放
     */
    private boolean mIsFlatFlow = false;

    /**
     * 是否启动Item灰度值渐变
     */
    private boolean mItemGradualGrey = false;

    /**
     * 是否启动Item半透渐变
     */
    private boolean mItemGradualAlpha = false;

    /**
     * 是否无限循环
     */
    private boolean mIsLoop = false;

    /**
     * 是否启动Item 3D 倾斜
     */
    private boolean mItem3D = false;
    private static final long smoothScrollDuration = 500;
    /**
     * @author savion
     * @date 2022/1/14
     * @desc 是否可以手势滑动
     **/
    private boolean enableGesture = true;
    /**
     * @author savion
     * @date 2022/1/14
     * @desc 方向
     **/
    private int orientation = OrientationHelper.VERTICAL;

    private CoverFlowLayoutManger(boolean isFlat, boolean isGreyItem,
                                  boolean isAlphaItem, float cstInterval,
                                  boolean isLoop, boolean is3DItem, int orientation) {
        mIsFlatFlow = isFlat;
        mItemGradualGrey = isGreyItem;
        mItemGradualAlpha = isAlphaItem;
        mIsLoop = isLoop;
        mItem3D = is3DItem;
        this.orientation = orientation;
        if (cstInterval >= 0) {
            mIntervalRatio = cstInterval;
        } else {
            if (mIsFlatFlow) {
                mIntervalRatio = 1.1f;
            }
        }
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public boolean isVertical() {
        return orientation == OrientationHelper.VERTICAL;
    }

    public boolean isHorizontal() {
        return orientation == OrientationHelper.HORIZONTAL;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //如果没有item，直接返回
        //跳过preLayout，preLayout主要用于支持动画
        if (getItemCount() <= 0 || state.isPreLayout()) {
            mOffsetAll = 0;
            return;
        }
        mAllItemFrames.clear();
        mHasAttachedItems.clear();

        //得到子view的宽和高，这边的item的宽高都是一样的，所以只需要进行一次测量
        View scrap = recycler.getViewForPosition(0);
        addView(scrap);
        measureChildWithMargins(scrap, 0, 0);
        //计算测量布局的宽高
        mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
        mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
        mStartX = Math.round((getHorizontalSpace() - mDecoratedChildWidth) * 1.0f / 2);
        mStartY = Math.round((getVerticalSpace() - mDecoratedChildHeight) * 1.0f / 2);

        float offset = isVertical() ? mStartY : mStartX;

        /**只存{@link MAX_RECT_COUNT}个item具体位置*/
        for (int i = 0; i < getItemCount() && i < MAX_RECT_COUNT; i++) {
            Rect frame = mAllItemFrames.get(i);
            if (frame == null) {
                frame = new Rect();
            }
            if (isVertical()) {
                //纵向布局
                frame.set(mStartX, Math.round(offset), mStartX + mDecoratedChildWidth, Math.round(offset + mDecoratedChildHeight));
            } else {
                //横向布局
                frame.set(Math.round(offset), mStartY, Math.round(offset + mDecoratedChildWidth), mStartY + mDecoratedChildHeight);
            }
            mAllItemFrames.put(i, frame);
            mHasAttachedItems.put(i, false);
            offset = offset + getIntervalDistance(); //原始位置累加，否则越后面误差越大
        }

        detachAndScrapAttachedViews(recycler); //在布局之前，将所有的子View先Detach掉，放入到Scrap缓存中
        if ((mRecycle == null || mState == null) && //在为初始化前调用smoothScrollToPosition 或者 scrollToPosition,只会记录位置
                mSelectPosition != 0) {                 //所以初始化时需要滚动到对应位置
            mOffsetAll = calculateOffsetForPosition(mSelectPosition);
            onSelectedCallBack();
        }

        layoutItems(recycler, state, SCROLL_TO_START);

        mRecycle = recycler;
        mState = state;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (isVertical()) {
            return scrollByGesture(dy, recycler, state);
        }
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (isHorizontal()) {
            return scrollByGesture(dx, recycler, state);
        }
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    private int scrollByGesture(int transDistance, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mAnimation != null && mAnimation.isRunning()) {
            mAnimation.cancel();
        }
        int travel = transDistance;
        if (!mIsLoop) {
            //非循环模式，限制滚动位置
            if (transDistance + mOffsetAll < 0) {
                travel = -mOffsetAll;
            } else if (transDistance + mOffsetAll > getMaxOffset()) {
                travel = (int) (getMaxOffset() - mOffsetAll);
            }
        }
        //累计偏移量
        mOffsetAll += travel;
        layoutItems(recycler, state, transDistance > 0 ? SCROLL_TO_START : SCROLL_TO_END);
        return travel;
    }

    /**
     * 布局Item
     *
     * <p>1，先清除已经超出屏幕的item
     * <p>2，再绘制可以显示在屏幕里面的item
     */
    private void layoutItems(RecyclerView.Recycler recycler,
                             RecyclerView.State state, int scrollDirection) {
        if (state == null || state.isPreLayout()) {
            return;
        }

        Rect displayFrame = null;
        if (isVertical()) {
            //纵向
            displayFrame = new Rect(0, mOffsetAll, getHorizontalSpace(), mOffsetAll + getVerticalSpace());
        } else {
            //横向
            displayFrame = new Rect(mOffsetAll, 0, mOffsetAll + getHorizontalSpace(), getVerticalSpace());
        }

        int position = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getTag() != null) {
                TAG tag = checkTag(child.getTag());
                position = tag.pos;
            } else {
                position = getPosition(child);
            }

            Rect rect = getFrame(position);

            if (!Rect.intersects(displayFrame, rect)) {//Item没有在显示区域，就说明需要回收
                removeAndRecycleView(child, recycler); //回收滑出屏幕的View
                mHasAttachedItems.delete(position);
            } else { //Item还在显示区域内，更新滑动后Item的位置
                layoutItem(child, rect); //更新Item位置
                mHasAttachedItems.put(position, true);
            }
        }

        if (position == 0) {
            position = getCenterPosition();
        }


        // 检查前后 20 个 item 是否需要绘制
        int min = position - 20;
        int max = position + 20;

        if (!mIsLoop) {
            if (min < 0) {
                min = 0;
            }
            if (max > getItemCount()) {
                max = getItemCount();
            }
        }

        for (int i = min; i < max; i++) {
            Rect rect = getFrame(i);
            if (Rect.intersects(displayFrame, rect) &&
                    !mHasAttachedItems.get(i)) { //重新加载可见范围内的Item
                // 循环滚动时，计算实际的 item 位置
                int actualPos = i % getItemCount();
                // 循环滚动时，位置可能是负值，需要将其转换为对应的 item 的值
                if (actualPos < 0) {
                    actualPos = getItemCount() + actualPos;
                }

                View scrap = recycler.getViewForPosition(actualPos);
                checkTag(scrap.getTag());
                scrap.setTag(new TAG(i));

                measureChildWithMargins(scrap, 0, 0);
//                if (scrollDirection == SCROLL_TO_END || mIsFlatFlow) { //item 向右滚动，新增的Item需要添加在最前面
                if (scrollDirection == SCROLL_TO_END) { //item 向右滚动，新增的Item需要添加在最前面
                    addView(scrap, 0);
                } else { //item 向左滚动，新增的item要添加在最后面
                    addView(scrap);
                }
                layoutItem(scrap, rect); //将这个Item布局出来
                mHasAttachedItems.put(i, true);
            }
        }
    }

    /**
     * 布局Item位置
     *
     * @param child 要布局的Item
     * @param frame 位置信息
     */
    private void layoutItem(View child, Rect frame) {
        if (isVertical()) {
            //纵向
            layoutDecorated(child,
                    frame.left,
                    frame.top - mOffsetAll,
                    frame.right,
                    frame.bottom - mOffsetAll);
        } else {
            //横向
            layoutDecorated(child,
                    frame.left - mOffsetAll,
                    frame.top,
                    frame.right - mOffsetAll,
                    frame.bottom);
        }
        if (!mIsFlatFlow) { //不是平面普通滚动的情况下才进行缩放
            child.setScaleX(computeScale(frame)); //缩放
            child.setScaleY(computeScale(frame)); //缩放
        }

        if (mItemGradualAlpha) {
            child.setAlpha(computeAlpha(frame));
        }

        if (mItemGradualGrey) {
            greyItem(child, frame);
        }

        if (mItem3D) {
            item3D(child, frame);
        }
    }

    /**
     * 动态获取Item的位置信息
     *
     * @param index item位置
     * @return item的Rect信息
     */
    private Rect getFrame(int index) {
        Rect frame = mAllItemFrames.get(index);
        if (frame == null) {
            if (isVertical()) {
                //纵向
                frame = new Rect();
                float offset = mStartY + getIntervalDistance() * index; //原始位置累加（即累计间隔距离）
                frame.set(mStartX, Math.round(offset), mStartX + mDecoratedChildWidth, Math.round(offset + mDecoratedChildHeight));
            } else {
                //横向
                frame = new Rect();
                float offset = mStartX + getIntervalDistance() * index; //原始位置累加（即累计间隔距离）
                frame.set(Math.round(offset), mStartY, Math.round(offset + mDecoratedChildWidth), mStartY + mDecoratedChildHeight);
            }
        }

        return frame;
    }

    /**
     * 变化Item的灰度值
     *
     * @param child 需要设置灰度值的Item
     * @param frame 位置信息
     */
    private void greyItem(View child, Rect frame) {
        float value = 1f;
        if (isHorizontal()) {
            value = computeGreyScale(frame.left - mOffsetAll);
        } else {
            value = computeGreyScale(frame.top - mOffsetAll);
        }
        ColorMatrix cm = new ColorMatrix(new float[]{
                value, 0, 0, 0, 120 * (1 - value),
                0, value, 0, 0, 120 * (1 - value),
                0, 0, value, 0, 120 * (1 - value),
                0, 0, 0, 1, 250 * (1 - value),
        });
//        cm.setSaturation(0.9f);

        // Create a paint object with color matrix
        Paint greyPaint = new Paint();
        greyPaint.setColorFilter(new ColorMatrixColorFilter(cm));

        // Create a hardware layer with the grey paint
        child.setLayerType(View.LAYER_TYPE_HARDWARE, greyPaint);
        if (value >= 1) {
            // Remove the hardware layer
            child.setLayerType(View.LAYER_TYPE_NONE, null);
        }

    }

    private void item3D(View child, Rect frame) {
        if (isHorizontal()) {
            float center = (frame.left + frame.right - 2 * mOffsetAll) / 2f;
            float value = (center - (mStartX + mDecoratedChildWidth / 2f)) * 1f / (getItemCount() * getIntervalDistance());
            value = (float) Math.sqrt(Math.abs(value));
            float symbol = center > (mStartX + mDecoratedChildWidth / 2f) ? -1 : 1;
            child.setRotationY(symbol * 50f * value);
        } else {
            float center = (frame.top + frame.bottom - 2 * mOffsetAll) / 2f;
            float value = (center - (mStartY + mDecoratedChildHeight / 2f)) * 1f / (getItemCount() * getIntervalDistance());
            value = (float) Math.sqrt(Math.abs(value));
            float symbol = center > (mStartY + mDecoratedChildHeight / 2f) ? -1 : 1;
            child.setRotationX(symbol * -50f * value);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        switch (state) {
            case RecyclerView.SCROLL_STATE_IDLE:
                //滚动停止时
                fixOffsetWhenFinishScroll();
                break;
            case RecyclerView.SCROLL_STATE_DRAGGING:
                //拖拽滚动时
                if (mSelectedListener != null) {
                    mSelectedListener.onItemSelectStart();
                }
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                //动画滚动时
                break;
            default:
                break;
        }
    }

    @Override
    public void scrollToPosition(int position) {
        if (position < 0 || position > getItemCount() - 1) {
            return;
        }
        mOffsetAll = calculateOffsetForPosition(position);
        if (mRecycle == null || mState == null) {//如果RecyclerView还没初始化完，先记录下要滚动的位置
            mSelectPosition = position;
        } else {
            layoutItems(mRecycle, mState, position > mSelectPosition ? SCROLL_TO_START : SCROLL_TO_END);
            onSelectedCallBack();
            if (mSelectedListener != null) {
                mSelectedListener.onItemSelectEnd();
            }
        }
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        // TODO 循环模式已支持平滑滚动
        int finalOffset = calculateOffsetForPosition(position);
        if (mRecycle == null || mState == null) {//如果RecyclerView还没初始化完，先记录下要滚动的位置
            mSelectPosition = position;
        } else {
            startScroll(mOffsetAll, finalOffset, smoothScrollDuration);
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        if (isHorizontal()) {
            return enableGesture;
        }
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        if (isVertical()) {
            return enableGesture;
        }
        return false;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
        mRecycle = null;
        mState = null;
        mOffsetAll = 0;
        mSelectPosition = 0;
        mLastSelectPosition = 0;
        mHasAttachedItems.clear();
        mAllItemFrames.clear();
    }

    /**
     * 获取整个布局的水平空间大小
     */
    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    /**
     * 获取整个布局的垂直空间大小
     */
    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    /**
     * 获取最大偏移量
     */
    private float getMaxOffset() {
        return (getItemCount() - 1) * getIntervalDistance();
    }

    /**
     * 计算Item缩放系数
     *
     * @param r Item的偏移量
     * @return 缩放系数
     */
    private float computeScale(Rect r) {
        if (r == null) {
            return 1f;
        }
        float scale = 0;
        if (isHorizontal()) {
            scale = 1 - Math.abs(r.left - mOffsetAll - mStartX) * 1.0f / Math.abs(mStartX + mDecoratedChildWidth / mIntervalRatio);
        } else {
            scale = 1 - Math.abs(r.top - mOffsetAll - mStartY) * 1.0f / Math.abs(mStartY + mDecoratedChildHeight / mIntervalRatio);
        }
        if (scale < 0) {
            scale = 0;
        }
        if (scale > 1) {
            scale = 1;
        }
        return scale;
    }

    /**
     * 计算Item的灰度值
     *
     * @param x Item的偏移量
     * @return 灰度系数
     */
    private float computeGreyScale(int x) {
        if (isHorizontal()) {
            float itemMidPos = x + mDecoratedChildWidth / 2f; //item中点x坐标
            float itemDx2Mid = Math.abs(itemMidPos - getHorizontalSpace() / 2f); //item中点距离控件中点距离
            float value = 1 - itemDx2Mid * 1.0f / (getHorizontalSpace() / 2f);
            if (value < 0.1) {
                value = 0.1f;
            }
            if (value > 1) {
                value = 1;
            }
            value = (float) Math.pow(value, .8);
            return value;
        } else {
            float itemMidPos = x + mDecoratedChildHeight / 2f; //item中点x坐标
            float itemDx2Mid = Math.abs(itemMidPos - getVerticalSpace() / 2f); //item中点距离控件中点距离
            float value = 1 - itemDx2Mid * 1.0f / (getVerticalSpace() / 2f);
            if (value < 0.1) {
                value = 0.1f;
            }
            if (value > 1) {
                value = 1;
            }
            value = (float) Math.pow(value, .8);
            return value;
        }
    }

    /**
     * 计算Item半透值
     *
     * @param r Item的偏移量
     * @return 缩放系数
     */
    private float computeAlpha(Rect r) {
        if (r == null) {
            return 1f;
        }
        float alpha = 1f;
        if (isHorizontal()) {
            alpha = 1 - Math.abs(r.left - mOffsetAll - mStartX) * 1.0f / Math.abs(mStartX + mDecoratedChildWidth / mIntervalRatio);
        } else {
            alpha = 1 - Math.abs(r.top - mOffsetAll - mStartY) * 1.0f / Math.abs(mStartY + mDecoratedChildHeight / mIntervalRatio);
        }
        if (alpha < 0.3f) {
            alpha = 0.3f;
        }
        if (alpha > 1) {
            alpha = 1.0f;
        }
        return alpha;
    }

    /**
     * 计算Item所在的位置偏移
     *
     * @param position 要计算Item位置
     */
    private int calculateOffsetForPosition(int position) {
        return Math.round(getIntervalDistance() * position);
    }

    /**
     * 修正停止滚动后，Item滚动到中间位置
     */
    private void fixOffsetWhenFinishScroll() {
        if (getIntervalDistance() != 0) { // 判断非 0 ，否则除 0 会导致异常
            int scrollN = (int) (mOffsetAll * 1.0f / getIntervalDistance());
            float moreDx = (mOffsetAll % getIntervalDistance());
            if (Math.abs(moreDx) > (getIntervalDistance() * 0.5)) {
                if (moreDx > 0) {
                    scrollN++;
                } else {
                    scrollN--;
                }
            }
            int finalOffset = scrollN * getIntervalDistance();
            startScroll(mOffsetAll, finalOffset, smoothScrollDuration);
            mSelectPosition = Math.abs(Math.round(finalOffset * 1.0f / getIntervalDistance())) % getItemCount();
        } else {
            if (mSelectedListener != null) {
                mSelectedListener.onItemSelectEnd();
            }
        }
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 随机滚动到随机位置，指定时长4000ms
     **/
    public boolean randomSmoothScrollToPosition() {
        return randomSmoothScrollToPosition(4000L);
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 随机滚动到随机位置，指定动画时长
     **/
    public boolean randomSmoothScrollToPosition(long duration) {
        int random = (int) (Math.random() * 50);
        //圈数总个数
        int lapCount = 50;
        int pos = mSelectPositionNature + lapCount + random;
        return randomSmoothScrollToPosition(duration, pos);
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 随机滚动到随机位置，指定动画时长与位置
     **/
    public boolean randomSmoothScrollToPosition(long duration, int pos) {
        //在所有项目中随机一个
        if (enableGesture) {
            int offset = calculateOffsetForPosition(pos);
            startScroll(mOffsetAll, offset, duration, new AccelerateDecelerateInterpolator(), true);
            return true;
        }
        return false;
    }

    /**
     * 滚动到指定X轴位置
     *
     * @param from X轴方向起始点的偏移量
     * @param to   X轴方向终点的偏移量
     */
    private void startScroll(int from, int to, long duration) {
        startScroll(from, to, duration, null, false);
    }

    private void startScroll(int from, int to, long duration, Interpolator interpolator, final boolean lockGesture) {
        if (mAnimation != null && mAnimation.isRunning()) {
            mAnimation.cancel();
        }
        final int direction = from < to ? SCROLL_TO_START : SCROLL_TO_END;
        mAnimation = ValueAnimator.ofFloat(from, to);
        mAnimation.setDuration(duration <= 0 ? 500 : duration);
        mAnimation.setInterpolator(interpolator != null ? interpolator : new DecelerateInterpolator());
        mAnimation.addUpdateListener(animation -> {
            Log.e("savion","coverFlowLayout render:"+destoryed);
            if (!destoryed) {
                mOffsetAll = Math.round((float) animation.getAnimatedValue());
                layoutItems(mRecycle, mState, direction);
            }
        });
        mAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (lockGesture) {
                    enableGesture = false;
                }
                if (mSelectedListener != null) {
                    mSelectedListener.onItemSelectStart();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onSelectedCallBack();
                if (mSelectedListener != null) {
                    mSelectedListener.onItemSelectEnd();
                }
                if (lockGesture) {
                    enableGesture = true;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAnimation.start();
    }

    /**
     * 获取Item间隔
     */
    private int getIntervalDistance() {
        if (isHorizontal()) {
            return Math.round(mDecoratedChildWidth * mIntervalRatio);
        } else {
            return Math.round(mDecoratedChildHeight * mIntervalRatio);
        }
    }

    /**
     * 计算当前选中位置，并回调
     */
    private void onSelectedCallBack() {
        mSelectPositionNature = Math.round(mOffsetAll / getIntervalDistance());
        mSelectPosition = mSelectPositionNature;
        mSelectPosition = mSelectPosition < 0 ? (getItemCount() + (mSelectPosition % getItemCount())) : Math.abs(mSelectPosition % getItemCount());
        if (mSelectedListener != null && mSelectPosition != mLastSelectPosition) {
            mSelectedListener.onItemSelected(mSelectPosition);
        }
        mLastSelectPosition = mSelectPosition;
    }

    private TAG checkTag(Object tag) {
        if (tag != null) {
            if (tag instanceof TAG) {
                return ((TAG) tag);
            } else {
                throw new IllegalArgumentException("You should not use View#setTag(Object tag), use View#setTag(int key, Object tag) instead!");
            }
        } else {
            return null;
        }
    }

    /**
     * 获取第一个可见的Item位置
     * <p>Note:该Item为绘制在可见区域的第一个Item，有可能被第二个Item遮挡
     */
    public int getFirstVisiblePosition() {
        Rect displayFrame = new Rect(mOffsetAll, 0, mOffsetAll + getHorizontalSpace(), getVerticalSpace());
        int cur = getCenterPosition();
        for (int i = cur - 1; ; i--) {
            Rect rect = getFrame(i);
            if (rect.left <= displayFrame.left) {
                return Math.abs(i) % getItemCount();
            }
        }
    }

    /**
     * 获取最后一个可见的Item位置
     * <p>Note:该Item为绘制在可见区域的最后一个Item，有可能被倒数第二个Item遮挡
     */
    public int getLastVisiblePosition() {
        Rect displayFrame = new Rect(mOffsetAll, 0, mOffsetAll + getHorizontalSpace(), getVerticalSpace());
        int cur = getCenterPosition();
        for (int i = cur + 1; ; i++) {
            Rect rect = getFrame(i);
            if (rect.right >= displayFrame.right) {
                return Math.abs(i) % getItemCount();
            }
        }
    }

    /**
     * 该方法主要用于{@link RecyclerCoverFlow#getChildDrawingOrder(int, int)}判断中间位置
     *
     * @param index child 在 RecyclerCoverFlow 中的位置
     * @return child 的实际位置，如果 {@link #mIsLoop} 为 true ，返回结果可能为负值
     */
    int getChildActualPos(int index) {
        View child = getChildAt(index);
        if (child.getTag() != null) {
            TAG tag = checkTag(child.getTag());
            return tag.pos;
        } else {
            return getPosition(child);
        }
    }

    /**
     * 获取可见范围内最大的显示Item个数
     */
    public int getMaxVisibleCount() {
        int oneSide = (getHorizontalSpace() - mStartX) / (getIntervalDistance());
        return oneSide * 2 + 1;
    }

    /**
     * 获取中间位置
     * <p>Note:该方法主要用于{@link RecyclerCoverFlow#getChildDrawingOrder(int, int)}判断中间位置
     * <p>如果需要获取被选中的Item位置，调用{@link #getSelectedPos()}
     */
    int getCenterPosition() {
        int pos = mOffsetAll / getIntervalDistance();
        int more = mOffsetAll % getIntervalDistance();
        if (Math.abs(more) >= getIntervalDistance() * 0.5f) {
            if (more >= 0) {
                pos++;
            } else {
                pos--;
            }
        }
        return pos;
    }

    /**
     * 设置选中监听
     *
     * @param l 监听接口
     */
    public void setOnSelectedListener(OnSelected l) {
        mSelectedListener = l;
    }

    /**
     * 获取被选中Item位置
     */
    public int getSelectedPos() {
        return mSelectPosition;
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 获取被选中item的自然位置
     **/
    public int getmSelectPositionNature() {
        return mSelectPositionNature;
    }

    private boolean destoryed = false;

    public void onDestory() {
        Log.e("savion","coverFlowLayout render destory:"+destoryed);
        destoryed = true;
        if (mAnimation != null) {
            mAnimation.cancel();
        }
    }

    /**
     * 选中监听接口
     */
    public interface OnSelected {
        /**
         * 监听选中回调
         *
         * @param position 显示在中间的Item的位置
         */
        void onItemSelected(int position);

        /**
         * @author savion
         * @date 2022/1/20
         * @desc
         **/
        default void onItemSelectStart() {
        }

        /**
         * @author savion
         * @date 2022/1/20
         * @desc 滑动结束
         **/
        default void onItemSelectEnd() {
        }
    }

    private class TAG {
        int pos;

        TAG(int pos) {
            this.pos = pos;
        }
    }

    static class Builder {
        boolean isFlat = false;
        boolean isGreyItem = false;
        boolean isAlphaItem = false;
        float cstIntervalRatio = -1f;
        boolean isLoop = false;
        int orientation = OrientationHelper.VERTICAL;
        boolean is3DItem = false;

        Builder setFlat(boolean flat) {
            isFlat = flat;
            return this;
        }

        Builder setOrientation(int orientation) {
            this.orientation = orientation;
            return this;
        }

        Builder setGreyItem(boolean greyItem) {
            isGreyItem = greyItem;
            return this;
        }

        Builder setAlphaItem(boolean alphaItem) {
            isAlphaItem = alphaItem;
            return this;
        }

        Builder setIntervalRatio(float ratio) {
            cstIntervalRatio = ratio;
            return this;
        }

        Builder loop() {
            isLoop = true;
            return this;
        }

        Builder set3DItem(boolean d3) {
            is3DItem = d3;
            return this;
        }

        public CoverFlowLayoutManger build() {
            return new CoverFlowLayoutManger(isFlat, isGreyItem,
                    isAlphaItem, cstIntervalRatio, isLoop, is3DItem, orientation);
        }
    }
}