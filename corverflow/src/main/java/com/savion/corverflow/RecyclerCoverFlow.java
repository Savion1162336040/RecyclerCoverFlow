package com.savion.corverflow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 继承RecyclerView重写{@link #getChildDrawingOrder(int, int)}对Item的绘制顺序进行控制
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version V1.0
 * @Datetime 2017-04-18
 */

public class RecyclerCoverFlow extends RecyclerView {
    /**
     * 按下的X轴坐标
     */
    private float mDownX;
    private float mDownY;

    /**
     * 布局器构建者
     */
    private CoverFlowLayoutManger.Builder mManagerBuilder;

    public RecyclerCoverFlow(Context context) {
        super(context);
        init();
    }

    public RecyclerCoverFlow(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecyclerCoverFlow(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        createManageBuilder();
        setLayoutManager(mManagerBuilder.build());
        setChildrenDrawingOrderEnabled(true); //开启重新排序
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    /**
     * 创建布局构建器
     */
    private void createManageBuilder() {
        if (mManagerBuilder == null) {
            mManagerBuilder = new CoverFlowLayoutManger.Builder();
        }
    }

    /**
     * 设置是否为普通平面滚动
     *
     * @param isFlat true:平面滚动；false:叠加缩放滚动
     */
    public void setFlatFlow(boolean isFlat) {
        createManageBuilder();
        mManagerBuilder.setFlat(isFlat);
        setLayoutManager(mManagerBuilder.build());
    }

    /**
     * 设置Item灰度渐变
     *
     * @param greyItem true:Item灰度渐变；false:Item灰度不变
     */
    public void setGreyItem(boolean greyItem) {
        createManageBuilder();
        mManagerBuilder.setGreyItem(greyItem);
        setLayoutManager(mManagerBuilder.build());
    }

    /**
     * 设置Item灰度渐变
     *
     * @param alphaItem true:Item半透渐变；false:Item透明度不变
     */
    public void setAlphaItem(boolean alphaItem) {
        createManageBuilder();
        mManagerBuilder.setAlphaItem(alphaItem);
        setLayoutManager(mManagerBuilder.build());
    }

    /**
     * 设置无限循环滚动
     */
    public void setLoop() {
        createManageBuilder();
        mManagerBuilder.loop();
        setLayoutManager(mManagerBuilder.build());
    }

    /**
     * 设置Item 3D 倾斜
     *
     * @param d3 true：Item 3d 倾斜；false：Item 正常摆放
     */
    public void set3DItem(boolean d3) {
        createManageBuilder();
        mManagerBuilder.set3DItem(d3);
        setLayoutManager(mManagerBuilder.build());
    }

    /**
     * 设置Item的间隔比例
     *
     * @param intervalRatio Item间隔比例。
     *                      即：item的宽 x intervalRatio
     */
    public void setIntervalRatio(float intervalRatio) {
        createManageBuilder();
        mManagerBuilder.setIntervalRatio(intervalRatio);
        setLayoutManager(mManagerBuilder.build());
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 设置方向
     * {@link OrientationHelper#HORIZONTAL}
     * {@link OrientationHelper#VERTICAL}
     **/
    public void setOrientation(int orientation) {
        createManageBuilder();
        mManagerBuilder.setOrientation(orientation);
        setLayoutManager(mManagerBuilder.build());
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (!(layout instanceof CoverFlowLayoutManger)) {
            throw new IllegalArgumentException("The layout manager must be CoverFlowLayoutManger");
        }
        super.setLayoutManager(layout);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int center = getCoverFlowLayout().getCenterPosition();
        // 获取 RecyclerView 中第 i 个 子 view 的实际位置
        int actualPos = getCoverFlowLayout().getChildActualPos(i);

        // 距离中间item的间隔数
        int dist = actualPos - center;
        int order;
        if (dist < 0) { // [< 0] 说明 item 位于中间 item 左边，按循序绘制即可
            order = i;
        } else { // [>= 0] 说明 item 位于中间 item 右边，需要将顺序颠倒绘制
            order = childCount - 1 - dist;
        }

        if (order < 0) {
            order = 0;
        } else if (order > childCount - 1) {
            order = childCount - 1;
        }

        return order;
    }

    /**
     * 获取LayoutManger，并强制转换为CoverFlowLayoutManger
     */
    public CoverFlowLayoutManger getCoverFlowLayout() {
        return ((CoverFlowLayoutManger) getLayoutManager());
    }

    /**
     * 获取被选中的Item位置
     */
    public int getSelectedPos() {
        return getCoverFlowLayout().getSelectedPos();
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 获取被选中的item自然位置，如开启了循环滚动
     **/
    public int getSelectedNaturePos() {
        return getCoverFlowLayout().getmSelectPositionNature();
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 随机滚动到位置
     **/
    public boolean randomSmoothScrollToPosition() {
        return getCoverFlowLayout().randomSmoothScrollToPosition();
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 随机滚动到随机位置，指定动画时长
     **/
    public boolean randomSmoothScrollToPosition(long duration) {
        return getCoverFlowLayout().randomSmoothScrollToPosition(duration);
    }

    /**
     * @author savion
     * @date 2022/1/14
     * @desc 随机滚动到随机位置，指定动画时长与位置
     **/
    public boolean randomSmoothScrollToPosition(long duration, int pos) {
        return getCoverFlowLayout().randomSmoothScrollToPosition(duration, pos);
    }

    /**
     * 设置选中监听
     *
     * @param l 监听接口
     */
    public void setOnItemSelectedListener(CoverFlowLayoutManger.OnSelected l) {
        getCoverFlowLayout().setOnSelectedListener(l);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getCoverFlowLayout().isHorizontal()) {
                    mDownX = ev.getX();
                    getParent().requestDisallowInterceptTouchEvent(true); //设置父类不拦截滑动事件
                } else {
                    mDownY = ev.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);//设置父类不拦截滑动事件
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (getCoverFlowLayout().isHorizontal()) {
                    if ((ev.getX() > mDownX && getCoverFlowLayout().getCenterPosition() == 0) ||
                            (ev.getX() < mDownX && getCoverFlowLayout().getCenterPosition() ==
                                    getCoverFlowLayout().getItemCount() - 1)) {
                        //如果是滑动到了最前和最后，开放父类滑动事件拦截
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        //滑动到中间，设置父类不拦截滑动事件
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                } else {
                    if ((ev.getY() > mDownY && getCoverFlowLayout().getCenterPosition() == 0) ||
                            (ev.getY() < mDownY && getCoverFlowLayout().getCenterPosition() ==
                                    getCoverFlowLayout().getItemCount() - 1)) {
                        //如果是滑动到了最前和最后，开放父类滑动事件拦截
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        //滑动到中间，设置父类不拦截滑动事件
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    public void onDestory() {
        getCoverFlowLayout().onDestory();
    }
}
