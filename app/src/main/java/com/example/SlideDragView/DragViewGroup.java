package com.example.SlideDragView;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.FloatRange;
import android.support.annotation.LayoutRes;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by trs on 2016/2/15.
 */
public class DragViewGroup extends FrameLayout {
    /**
     * 控件拖拽功能
     */
    private ViewDragHelper dragHelper;
    /**
     * 回调方法
     */
    private ViewDragHelper.Callback callback;
    /**
     * mainView:主页<p>
     * slideView:侧边栏
     */
    private View mainView, slideView = null;
    private int width, height;
    private int slideLeft;//计算好的slideView的原始left值
    private int range;//mainView滑动打开后最终停留的位置距离屏幕左边的距离
    private int openRange;//mainView打开锁需要的最小距离
    private int closeRange;//mainView关闭所需要的最小距离
    private float rangePercent = 0.8f;//mainView打开距离的百分比
    private Status status = Status.CLOSE;
    private Status lastStatus = Status.CLOSE;
    private OnDraggingListener listener;

    /**
     * 状态值
     */
    public enum Status {
        OPEN, CLOSE
    }

    public interface OnDraggingListener {
        void onOpen();

        void onClose();

        void onDragging(float percent);
    }

    public DragViewGroup(Context context) {
        super(context);
        init();
    }

    public DragViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        callback = new ViewDragHelper.Callback() {
            /**
             * 判断当前的child是否允许被拖拽
             * @param child 当前接触的View
             * @param pointerId
             * @return true - 所有view都允许被拖拽
             * false - 都不允许被拖拽
             */
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return slideView != null;
            }

            /**
             * <p>返回被拖拽的View的可拖拽范围<p/>
             * <p>当View设置了clickable后，需要重写该回调，让其返回值大于0，则可以拖动<p/>
             *
             * @param child
             * @return
             */
            @Override
            public int getViewHorizontalDragRange(View child) {
                return width;
            }

            /**
             * 垂直移动的设置回调，返回0则不调用
             * @param child
             * @param top 当前移动到的位置，一般直接返回该值，但是也可以自行计算
             * @param dy 增量，可以理解为速度值
             * @return 返回最终移动的位置
             */
            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return 0;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                Log.v("clampViewPosition", "clampViewPositionHorizontal");
                if (child == mainView) {
                    //通过dx的正负值判断当前滑动方向
                    //第一个判断是为了让其滑动到边界后就不能拖拽
                    if (left >= range) {
                        return range;
                    } else {
                        //第二个判断是为了让其在正常状态下无法向左拖拽
                        return left > 0 ? left : 0;
                    }
                } else {
//                    return dx < 0 ? left : 0;
                    return left;
                }
            }

            /**
             * 可以理解为当前被拖拽的View被释放后（即手指离开屏幕后）的操作
             * @param releasedChild
             * @param xvel X方向的速度（相对左边），pixel/second
             * @param yvel Y方向的速度（相对上面），pixel/second
             */
            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                Log.v("onViewReleased", "onViewReleased");
                //可以用xvel的正负值判断手指滑动方向
                //侧边栏打开和关闭，两个判断
                //一个是滑动速度够了，就打开、关闭
                //另一个是移动距离够了，就打开、关闭
                if (xvel > 5) {
                    open();
                } else if (xvel < -5) {
                    close();
                } else {
                    int left = mainView.getLeft();
                    if (left <= openRange) {
                        close();
                    } else if (left >= closeRange) {
                        open();
                    } else {
                        if (status == Status.OPEN) {
                            close();
                        } else {
                            open();
                        }
                    }
                }
            }

            /**
             * View的位置变动后将调用该方法，这里的view是通过拖拽的view，通过layout方法改变位置的不会回调该方法
             * @param changedView
             * @param left 当前位置变动的view相对屏幕左边的值
             * @param top
             * @param dx 增量
             * @param dy
             */
            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                int newLeft = left;
                if (changedView == slideView) {
                    //这段代码使得侧边栏打开之后，滑动侧边栏时，主页也会移动
                    //但是侧边栏没有移动
                    //这一步是让mainView的左边距离跟着侧边栏的递减速度递减
                    newLeft = mainView.getLeft() + dx;
                    //限制newLeft范围在0~Range之间，使得mainView不会滑动出范围
                    if (newLeft <= 0)
                        newLeft = 0;
                    if (newLeft > range)
                        newLeft = range;
                    mainView.layout(newLeft, 0, newLeft + width, height);
                    //这里是让侧边栏保持静止不动的状态，然后使用动画让侧边栏移动
//                    slideView.layout(slideLeft, 0, width + slideLeft, height);
                }
                //在这里加上侧边栏动画效果
                slideViewAnimator(newLeft);
                draggingListener(newLeft);
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                super.onViewCaptured(capturedChild, activePointerId);
                Log.v("onViewCaptured", "onViewCaptured");
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                Log.v("onViewDragStateChanged", "onViewDragStateChanged");
            }
        };
        dragHelper = ViewDragHelper.create(this, callback);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = mainView.getMeasuredWidth();
        height = mainView.getMeasuredHeight();
        range = (int) (width * rangePercent);
        openRange = (int) (range * 0.3);
        closeRange = (int) (range * 0.7);
    }

    /**
     * 要在onLayout中给两个View指定位置，否则不会显示布局
     *
     * @param changed
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (slideView != null) {
            slideLeft = (int) (-range * 0.5);
            slideView.layout(slideLeft, 0, width + slideLeft, height);
        }
        mainView.layout(0, 0, width, height);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //重写该Layout的触摸拦截事件，将其交给ViewDragHelper处理
        //可以加上手势的onTouchEvent判断返回，这样可以重写MainView的onTouch事件
        return dragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //将触摸事件交给ViewDragHelper处理
        dragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (dragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
    }

    /**
     * 加载完布局后会回调该函数，通过该函数获取子控件
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() <= 1)
            slideView = null;
        else {
            slideView = getChildAt(0);
            slideView.setClickable(true);
        }
        mainView = getChildAt(slideView == null ? 0 : 1);
        mainView.setClickable(true);
        //设置点击事件让主页面可以点击关闭
        mainView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status != Status.CLOSE)
                    close();
            }
        });
    }

    /**
     * 设置slideView的滑动效果
     *
     * @param mainLeft
     */
    private void slideViewAnimator(int mainLeft) {
        //使用这种动画代码，view虽然移动了，但是实际上并没有移动，因此会导致部分地方滑动无效
        //slideView.setTranslationX((float) (mainLeft * 0.5));
        int newLeft = (int) (slideLeft + (mainLeft * 0.5));
        slideView.layout(newLeft, 0, width - newLeft, height);
    }

    /**
     * 设置监听回调
     *
     * @param mainLeft
     */
    private void draggingListener(int mainLeft) {
        if (listener == null)
            return;
        float percent = mainLeft / (float) range;
        listener.onDragging(percent);
    }


    /**
     * 打开侧边栏
     */
    public void open() {
        //平滑移动View，finalLeft和finalTop是最终距离屏幕左边和上边的距离
        dragHelper.smoothSlideViewTo(mainView, range, 0);
        ViewCompat.postInvalidateOnAnimation(DragViewGroup.this);
        status = Status.OPEN;
        if (lastStatus != status) {
            lastStatus = status;
            if (listener != null)
                listener.onOpen();
        }
    }

    /**
     * 关闭侧边栏
     */
    public void close() {
        dragHelper.smoothSlideViewTo(mainView, 0, 0);
        ViewCompat.postInvalidateOnAnimation(DragViewGroup.this);
        status = Status.CLOSE;
        if (lastStatus != status) {
            lastStatus = status;
            if (listener != null)
                listener.onClose();
        }
    }

    /**
     * 设置侧边栏打开的距离比例
     *
     * @param percent
     */
    public void setRangePercent(@FloatRange(from = 0.0, to = 100.0) float percent) {
        rangePercent = percent;
        setRange((int) (width * rangePercent));
    }

    /**
     * 获取侧边栏打开的距离比例
     *
     * @return
     */
    public float getRangePercent() {
        return rangePercent;
    }

    /**
     * 设置侧边栏打开的距离
     *
     * @param range
     */
    public void setRange(int range) {
        this.range = range;
        openRange = (int) (range * 0.3);
        closeRange = (int) (range * 0.7);
    }

    public int getRange() {
        return range;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * 设置侧边栏的View
     *
     * @param view
     */
    public void setSlideView(View view) {
        slideView = view;
        addView(slideView, 0);
        slideLeft = (int) (-range * 0.5);
        slideView.layout(slideLeft, 0, width + slideLeft, height);
        slideView.setClickable(true);
    }

    /**
     * 设置侧边栏的View
     *
     * @param resId
     */
    public void setSlideView(@LayoutRes int resId) {
        setSlideView(LayoutInflater.from(getContext()).inflate(resId, null));
    }

    public View getSlideView() {
        return slideView;
    }

    public void setOnDraggingListener(OnDraggingListener listener) {
        this.listener = listener;
    }
}
