package com.simplelibrary.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.simplelibrary.R;

import java.util.ArrayList;
import java.util.List;

public class PickerView extends View {
    private static final int REVISE_MIN_ANGLE = 3;

    //限制最大速度
    private static final int REVISE_MAX_FLING_VELOCITY = 1;

    private static final int REVISE_DURATION_MILLIS = 200;
    //角度的精度因子
    private static final int PRECISION_FACTOR = 10000000;

    private Paint mTextPaint;
    private float mTextSize;
    private int mTextColor;

    private Paint mHighlightTextPaint;
    private float mHighlightTextSize;
    private int mHighlightTextColor;

    //item height为每个item的高度，根据夹角计算出显示的高度 itemHeight*cos(夹角)
    private float mItemHeight;//item的高度
    //控件中心的高度，会进行剪切，可以比itemHeight稍微大一点
    private float mCenterClipHeight;

    private int mCount;
    private boolean mCycle;

    private Drawable mDividerDrawable;
    private Drawable mShadowDrawable;
    private Drawable mCenterClipDrawable;

    private List<?> mData = new ArrayList<>();

    //以center item的position以及它与屏幕的夹角为参照，绘制其它item
    private AnchorInfo mAnchorInfo;

    //绘制出所有item所占据的rect，不一定是看到的
    private Rect mContentRect;
    //看到的显示出来的item所占据的rect
    private Rect mClipRect;

    private RectF mCenterClipRect, mPrevRangeClipRect, mNextRangeClipRect;

    //滑动部分的变量
    public enum Mode {
        RESET, FLING, DRAG, TAP
    }

    ;
    private Mode mTouchMode;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mActivePointerId;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private VelocityTracker mVelocityTracker;

    //用来回调
    private OnPickListener mOnPickListener;
    private int mPrevPosition;
    private CallbackRunnable mCallbackRunnable;

    //fling - 以一定的速度滑行
    //以距离作为滑动参数
    private FlingScroller mFlingScroller;
    //手指松开(速度小于最小值)、ACTION_CANCEL、fling结束时，将center item的偏移逐渐减小为0
    //以角度为参数，因为Scroller只能是整形而角度值在很小的范围内，所以乘以PRECISION_FACTOR。
    private ReviseScroller mReviseScroller;

    //mTiltDegree为倾斜角，指一个item所能够倾斜的最大角度
    //mDrawCount为绘制个数，根据绘制个数计算出倾斜角
    //mTiltDegree与mDrawCount只需要设置一个即可，如果同时设置会以mDrawCount优先
    private int mTiltDegree;
    private int mDrawCount;
    //按照tiltDegree来绘制半边，可以绘制几个
    private int mHalfPageTiltDrawCount;
    private int mTotalPageTiltDrawCount;
    private int mRealHalfPageTiltDrawCount;
    private int mRealTotalPageTiltDrawCount;

    //label
    private String mLabelText;
    private Paint mLabelPaint;
    private float mLabelMarginLeft;
    private boolean mLabelMarginPinned;

    //icon - 暂时只有left icon
    private int mIconMarginRight;

    public PickerView(Context context) {
        this(context, null);
    }

    public PickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //attrs
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PickerView);
        mTextSize = a.getDimension(R.styleable.PickerView_textSize, 12);
        mTextColor = a.getColor(R.styleable.PickerView_textColor, Color.BLACK);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);

        mHighlightTextSize = a.getDimension(R.styleable.PickerView_highlightTextSize, 12);
        mHighlightTextColor = a.getColor(R.styleable.PickerView_highlightTextColor, Color.BLACK);
        mHighlightTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightTextPaint.setTextSize(mHighlightTextSize);
        mHighlightTextPaint.setColor(mHighlightTextColor);

        mItemHeight = a.getDimension(R.styleable.PickerView_itemHeight, 50);
        mCenterClipHeight = a.getDimension(R.styleable.PickerView_centerClipHeight, 50);

        mCycle = a.getBoolean(R.styleable.PickerView_cycle, true);

        mTiltDegree = (int) (a.getFloat(R.styleable.PickerView_tiltDegree, 25) * PRECISION_FACTOR);
        if (mTiltDegree <= 0)
            throw new IllegalArgumentException("Tilt degree must be greater than zero.");

        //如果tiltDegree与drawCount同时被设置了，就使用drawCount。如果都不设置就使用默认的30度倾斜角
        mDrawCount = a.getInteger(R.styleable.PickerView_drawCount, -1);
        if (mDrawCount > 0 && mDrawCount % 2 == 0)
            throw new IllegalArgumentException("Visible count must be odd.");
        if (mDrawCount > 0) {
            mTiltDegree = (int) (1f / (mDrawCount + 1) * 180 * PRECISION_FACTOR);
        }

        mAnchorInfo = new AnchorInfo();
        mAnchorInfo.init();

        //        //如果是通过设置倾斜角的方式，就保证degree不会超过90度即可；如果是设置draw count，就比较简单。
        if (mDrawCount > 0) {
            mRealHalfPageTiltDrawCount = (mDrawCount - 1) / 2;
            mRealTotalPageTiltDrawCount = mDrawCount;
        } else {
            mRealHalfPageTiltDrawCount = 0;
            for (float degree = mAnchorInfo.calTiltDegree; Float.compare(degree, 90) < 0; degree += mAnchorInfo.calTiltDegree)
                mRealHalfPageTiltDrawCount += 1;
            mRealTotalPageTiltDrawCount = mRealHalfPageTiltDrawCount * 2 + 1;
        }

        mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float labelTextSize = a.getDimension(R.styleable.PickerView_labelTextSize, 20);
        mLabelPaint.setTextSize(labelTextSize);
        int labelTextColor = a.getColor(R.styleable.PickerView_labelTextColor, Color.BLACK);
        mLabelPaint.setColor(labelTextColor);
        mLabelText = a.getString(R.styleable.PickerView_labelText);
        mLabelMarginLeft = a.getDimension(R.styleable.PickerView_labelMarginLeft, 0);
        mLabelMarginPinned = a.getBoolean(R.styleable.PickerView_labelMarginPinned, true);

        mDividerDrawable = a.getDrawable(R.styleable.PickerView_dividerDrawable);
        mShadowDrawable = a.getDrawable(R.styleable.PickerView_shadowDrawable);
        mCenterClipDrawable = a.getDrawable(R.styleable.PickerView_centerClipDrawable);

        mIconMarginRight = a.getDimensionPixelSize(R.styleable.PickerView_iconMarginLeft, 20);
        a.recycle();

        mContentRect = new Rect();
        mClipRect = new Rect();

        mCenterClipRect = new RectF();
        mPrevRangeClipRect = new RectF();
        mNextRangeClipRect = new RectF();

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity() / REVISE_MAX_FLING_VELOCITY;

        mTouchMode = Mode.RESET;
        mReviseScroller = new ReviseScroller(context);
        mFlingScroller = new FlingScroller(context);

        mCallbackRunnable = new CallbackRunnable();
    }

    public List<?> getData() {
        return mData;
    }

    public Mode getTouchMode() {
        return mTouchMode;
    }

    public void setTextTypeface(Typeface typeface) {
        mTextPaint.setTypeface(typeface);
    }

    public void setHighlightTextTypeface(Typeface typeface) {
        mHighlightTextPaint.setTypeface(typeface);
    }

    public void setValueIndex(int pos, boolean callback) {
        mTouchMode = Mode.RESET;
        mFlingScroller.abort();
        mReviseScroller.abort();

        if (pos >= 0 && pos < mCount) {
            mAnchorInfo.update(pos, 0);
            if (!callback) {
                mPrevPosition = mAnchorInfo.position;
            }
            invalidate();
        }
    }

    public int getValueIndex() {
        return mAnchorInfo.position;
    }

    public void setLabel(String label) {
        mLabelText = label;
        invalidate();
    }

    public void setOnPickListener(OnPickListener onPickListener) {
        mOnPickListener = onPickListener;
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFlingScroller.abort();
        mReviseScroller.abort();
    }

    public float getCenterRangeHeight() {
        return mCenterClipHeight;
    }

    public void setData(List<?> data) {
        setData(data, 0);
    }

    public void setData(List<?> data, int selectPosition) {
        mData = data == null ? new ArrayList<Object>() : data;
        if (selectPosition < 0 || selectPosition >= mData.size()&&selectPosition>0) {
            throw new ArrayIndexOutOfBoundsException("position index out of mData!");
        }

        mTouchMode = Mode.RESET;
        mFlingScroller.abort();
        mReviseScroller.abort();

        mHalfPageTiltDrawCount = mRealHalfPageTiltDrawCount;
        mTotalPageTiltDrawCount = mRealTotalPageTiltDrawCount;
        reviseParameter(selectPosition);

        requestLayout();
    }

    //数据源改变会导致实际可见数变化，绘制的anchor info也需要改变
    private void reviseParameter(int selectPosition) {
        mCount = mData.size();

        if (mCount == 0)
            return;

        mPrevPosition = selectPosition;

        //修正滑动位置、选中序号
        //没有设置，就选中之前的值
        if (selectPosition < 0)
            selectPosition = mAnchorInfo.position;

        selectPosition = Math.max(0, selectPosition);
        selectPosition = Math.min(selectPosition, mCount - 1);
        mAnchorInfo.update(selectPosition, 0);

        //修正visible count
        if (mCount == 1) {
            mTotalPageTiltDrawCount = 1;
            mHalfPageTiltDrawCount = 0;
            return;
        }

        if (mCycle && mCount < mRealTotalPageTiltDrawCount) {
            for (int i = mCount - 1; i >= 1; i--) {//小于count的最大奇数
                if (i % 2 != 0) {
                    mTotalPageTiltDrawCount = i;
                    mHalfPageTiltDrawCount = (mTotalPageTiltDrawCount - 1) / 2;
                    break;
                }
            }
        }

        if (!mCycle && mCount <= mRealHalfPageTiltDrawCount) {
            mTotalPageTiltDrawCount = mCount * 2 - 1;
            mHalfPageTiltDrawCount = (mTotalPageTiltDrawCount - 1) / 2;
        }
    }

    private int prev(int position, int delta) {
        if (mCycle) {
            int ret = (position - delta) % mCount;
            if (ret < 0)
                ret = mCount + ret;
            return ret;
        } else
            return position - delta;
    }

    private int next(int position, int delta) {
        if (mCycle)
            return (position + delta) % mCount;
        else
            return position + delta;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mData == null || mData.size() == 0)
            return;

        canvas.save();
        canvas.translate(mContentRect.centerX(), mContentRect.centerY());
        canvas.clipRect(mClipRect);

        //绘制label
        if (!TextUtils.isEmpty(mLabelText)) {
            String centerText = mData.get(mAnchorInfo.position).toString();
            float centerTextWidth = mHighlightTextPaint.measureText(centerText);
            canvas.drawText(mLabelText, mLabelMarginPinned ? mLabelMarginLeft : (centerTextWidth / 2 + mLabelMarginLeft), getTextBaseline(mLabelPaint, 0), mLabelPaint);
        }

        //绘制center range
        drawCenter(canvas);

        int position = mAnchorInfo.position;
        float degree = mAnchorInfo.calOffsetDegree;

        int rangeCount = 0;
        int topRangeCount = mAnchorInfo.offsetDegree > 0 ? mHalfPageTiltDrawCount + 1 : mHalfPageTiltDrawCount;
        int bottomRangeCount = mAnchorInfo.offsetDegree < 0 ? mHalfPageTiltDrawCount + 1 : mHalfPageTiltDrawCount;
        if (mCount == 1) {
            topRangeCount = 0;
            bottomRangeCount = 0;
        }

        float y = (float) (mAnchorInfo.polarDis * Math.sin(degree * Math.PI / 180));
        float mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));

        //绘制下半部分
        float prevMappedHeight;
        boolean first = true;
        degree += mAnchorInfo.calTiltDegree;
        while (degree <= 90 && rangeCount < bottomRangeCount) {//判断position的合法性
            position = next(position, 1);
            rangeCount++;

            prevMappedHeight = mappedHeight;
            mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));
            y += (prevMappedHeight + mappedHeight) / 2;
            if (first && mAnchorInfo.offsetDegree < 0) {
                first = false;
                canvas.save();
                canvas.clipRect(mNextRangeClipRect);
                drawItem(canvas, mTextPaint, position, y, mappedHeight / mItemHeight, 0.06f);
                degree += mAnchorInfo.calTiltDegree;
                canvas.restore();
            } else {
                drawItem(canvas, mTextPaint, position, y, mappedHeight / mItemHeight, 0.06f);
                degree += mAnchorInfo.calTiltDegree;
            }
        }

        //绘制上半部分
        rangeCount = 0;
        degree = mAnchorInfo.calOffsetDegree;
        mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));
        y = (float) (mAnchorInfo.polarDis * Math.sin(degree * Math.PI / 180));
        position = mAnchorInfo.position;
        degree -= mAnchorInfo.calTiltDegree;
        first = true;

        while (degree >= -90 && rangeCount < topRangeCount) {
            rangeCount++;
            position = prev(position, 1);
            prevMappedHeight = mappedHeight;
            mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));
            y -= (mappedHeight + prevMappedHeight) / 2;

            if (first && mAnchorInfo.offsetDegree > 0) {
                first = false;
                canvas.save();
                canvas.clipRect(mPrevRangeClipRect);
                drawItem(canvas, mTextPaint, position, y, mappedHeight / mItemHeight, -0.06f);
                degree -= mAnchorInfo.calTiltDegree;
                canvas.restore();
            } else {
                drawItem(canvas, mTextPaint, position, y, mappedHeight / mItemHeight, -0.06f);
                degree -= mAnchorInfo.calTiltDegree;
            }
        }

        if (mCenterClipDrawable != null)
            mCenterClipDrawable.draw(canvas);

        if (mShadowDrawable != null)
            mShadowDrawable.draw(canvas);

        if (mDividerDrawable != null)
            mDividerDrawable.draw(canvas);

        canvas.rotate(180);

        if (mShadowDrawable != null)
            mShadowDrawable.draw(canvas);

        if (mDividerDrawable != null)
            mDividerDrawable.draw(canvas);

        canvas.restore();
    }

    private void drawCenter(Canvas canvas) {
        int position = mAnchorInfo.position;
        float prevMappedHeight;
        float degree = mAnchorInfo.calOffsetDegree;
        float y = (float) (mAnchorInfo.polarDis * Math.sin(degree * Math.PI / 180));
        float mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));

        canvas.save();
        canvas.clipRect(mCenterClipRect);
        drawItem(canvas, mHighlightTextPaint, position, y, mappedHeight / mItemHeight, 0);
        canvas.restore();

        if (mAnchorInfo.offsetDegree > 0) {
            degree -= mAnchorInfo.calTiltDegree;
            position = prev(position, 1);
            prevMappedHeight = mappedHeight;
            mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));
            y -= (mappedHeight + prevMappedHeight) / 2;
            canvas.save();
            canvas.clipRect(mCenterClipRect);
            drawItem(canvas, mHighlightTextPaint, position, y, mappedHeight / mItemHeight, 0);
            canvas.restore();
        }

        if (mAnchorInfo.offsetDegree < 0) {
            degree += mAnchorInfo.calTiltDegree;
            position = next(position, 1);
            prevMappedHeight = mappedHeight;
            mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));
            y += (prevMappedHeight + mappedHeight) / 2;
            canvas.save();
            canvas.clipRect(mCenterClipRect);
            drawItem(canvas, mHighlightTextPaint, position, y, mappedHeight / mItemHeight, 0);
            canvas.restore();
        }

        degree = mAnchorInfo.calOffsetDegree;
        position = mAnchorInfo.position;
        y = (float) (mAnchorInfo.polarDis * Math.sin(degree * Math.PI / 180));
        mappedHeight = (float) (mItemHeight * Math.cos(degree * Math.PI / 180));
        if (mAnchorInfo.offsetDegree > 0) {
            canvas.save();
            canvas.clipRect(mNextRangeClipRect);
            drawItem(canvas, mTextPaint, position, y, mappedHeight / mItemHeight, 0);
            canvas.restore();
        }

        if (mAnchorInfo.offsetDegree < 0) {
            canvas.save();
            canvas.clipRect(mPrevRangeClipRect);
            drawItem(canvas, mTextPaint, position, y, mappedHeight / mItemHeight, 0);
            canvas.restore();
        }

    }

    //x,y为center位置
    private void drawItem(Canvas canvas, Paint paint, int position, float y, float scaleY, float skewX) {
        String text = position >= 0 && position < mCount ? mData.get(position).toString() : "";
        float textWidth = paint.measureText(text);
        float textX = -textWidth / 2;
        float textY = getTextBaseline(paint, y);
        canvas.save();
        canvas.scale(1, scaleY, 0, y);//scale中心设置后怎么出错！！！

        canvas.drawText(text, textX, textY, paint);
   /*     Drawable icon = position >= 0 && position < mCount ? mData.getIcon(position) : null;
        if (icon != null) {
            int s = Math.min(icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            int l = (int) (textX - mIconMarginRight - s);
            icon.setBounds(l, (int) y - s / 2, l + s, (int) (y + s / 2));
            icon.draw(canvas);
        }*/
        canvas.restore();
    }

    //确定content rect以及layout frame
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int contentHeight = (int) (mAnchorInfo.radius * Math.sin(mRealTotalPageTiltDrawCount * mAnchorInfo.calTiltDegree * Math.PI / 360) * 2 + .5f);
        mContentRect.set(0, 0, widthSize - getPaddingLeft() - getPaddingRight(), contentHeight);

        int height;
        if (heightMode == MeasureSpec.EXACTLY)
            height = heightSize;
        else if (heightMode == MeasureSpec.AT_MOST) {
            height = getPaddingTop() + getPaddingBottom() + contentHeight;
            height = Math.min(heightSize, height);
        } else
            height = getPaddingTop() + getPaddingBottom() + contentHeight;

        setMeasuredDimension(widthSize, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        //设置content rect的偏移
        int centerX = (getWidth() - getPaddingLeft() - getPaddingRight()) / 2 + getPaddingLeft();
        int centerY = (getHeight() - getPaddingTop() - getPaddingBottom()) / 2 + getPaddingTop();
        mContentRect.offset(centerX - mContentRect.centerX(), centerY - mContentRect.centerY());

        mClipRect.set(0, 0, mContentRect.width(), Math.min(mContentRect.height(), getHeight() - getPaddingTop() - getPaddingBottom()));
        mClipRect.offset(centerX - mClipRect.centerX(), centerY - mClipRect.centerY());
        mClipRect.offset(-centerX, -centerY);

        mCenterClipRect.set(-mContentRect.width() / 2, -mCenterClipHeight / 2, mContentRect.width() / 2, mCenterClipHeight / 2);
        float mappedHeight = (float) (mItemHeight * Math.cos(mAnchorInfo.calTiltDegree * Math.PI / 180));
        mPrevRangeClipRect.set(-mContentRect.width() / 2, -mCenterClipHeight / 2 - mappedHeight, mContentRect.width() / 2, -mCenterClipHeight / 2);
        mNextRangeClipRect.set(-mContentRect.width() / 2, mCenterClipHeight / 2, mContentRect.width() / 2, mCenterClipHeight / 2 + mappedHeight);

        if (mDividerDrawable != null) {
            int dividerHeight = mDividerDrawable.getIntrinsicHeight();
            mDividerDrawable.setBounds((int) mCenterClipRect.left, (int) mCenterClipRect.top - dividerHeight, (int) mCenterClipRect.right, (int) mCenterClipRect.top);
        }

        if (mShadowDrawable != null)
            mShadowDrawable.setBounds(mClipRect.left, mClipRect.top - 1, mClipRect.right, (int) mCenterClipRect.top + 1);

        if (mCenterClipDrawable != null)
            mCenterClipDrawable.setBounds((int) mCenterClipRect.left, (int) mCenterClipRect.top, (int) mCenterClipRect.right, (int) mCenterClipRect.bottom);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        final int actionMasked = e.getActionMasked();

        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(e);

        if (mData == null || mData.size() == 0 || mData.size() == 1) {
            if (actionMasked == MotionEvent.ACTION_MOVE)
                return false;

            if (actionMasked == MotionEvent.ACTION_CANCEL) {
                mTouchMode = Mode.RESET;
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    return false;
                }
            }
        }

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = e.getPointerId(0);
                mLastMotionX = e.getX();
                mLastMotionY = e.getY();

                mFlingScroller.abort();
                mReviseScroller.abort();
                mTouchMode = mTouchMode == Mode.FLING ? Mode.DRAG : Mode.TAP;

                final ViewParent parent = getParent();
                if (parent != null)
                    parent.requestDisallowInterceptTouchEvent(true);
            }
            break;

            case MotionEvent.ACTION_POINTER_DOWN:
                final int actionIndex = e.getActionIndex();
                mActivePointerId = e.getPointerId(actionIndex);

                mLastMotionX = e.getX(actionIndex);
                mLastMotionY = e.getY(actionIndex);
                break;

            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = -1;
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                //位置还原
                revise();
                break;

            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = e.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1)
                    break;

                final float x = e.getX(activePointerIndex);
                final float y = (int) e.getY(activePointerIndex);
                float absDeltaX = Math.abs(x - mLastMotionX);
                float deltaY = y - mLastMotionY;
                float absDeltaY = Math.abs(deltaY);
                if (mTouchMode == Mode.TAP && absDeltaY > mTouchSlop && absDeltaY > absDeltaX) {
                    final ViewParent parent = getParent();
                    if (parent != null)
                        parent.requestDisallowInterceptTouchEvent(true);
                    mTouchMode = Mode.DRAG;
                    if (deltaY > 0)
                        deltaY -= mTouchSlop;
                    else
                        deltaY += mTouchSlop;
                }

                if (mTouchMode == Mode.DRAG) {
                    mLastMotionX = x;
                    mLastMotionY = y;
                    int toDegree = (int) (mAnchorInfo.offsetDegree + deltaY * .8f * 180f * PRECISION_FACTOR / (Math.PI * mAnchorInfo.radius));
                    trackMotionScroll(toDegree);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mTouchMode == Mode.DRAG) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);

                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        fling(initialVelocity);
                    } else {
                        revise();
                    }

                    mActivePointerId = -1;
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }

                if (mTouchMode == Mode.TAP) {
                    mActivePointerId = -1;
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    return false;//对click进行了判断，但是没有进行callback，所以，简单返回false。后期需要可添加
                }
                break;
        }

        return true;
    }

    private void fling(int vel) {
        if (!mCycle && ((vel > 0 && mAnchorInfo.position == 0) || (vel < 0 && mAnchorInfo.position == mCount - 1))) {
            mTouchMode = Mode.RESET;
            trackMotionScroll(mAnchorInfo.offsetDegree);
            notifyChanged();
            return;
        }
        mTouchMode = Mode.FLING;
        mFlingScroller.startFling(vel);
    }

    private void revise() {
        mTouchMode = Mode.RESET;

        int absOffsetAngle = Math.abs(mAnchorInfo.offsetDegree);
        if (absOffsetAngle <= REVISE_MIN_ANGLE) {
            trackMotionScroll(mAnchorInfo.offsetDegree);
            notifyChanged();
            return;
        }

        boolean isForward = mAnchorInfo.offsetDegree > 0;
        boolean isOver = absOffsetAngle > mTiltDegree / 2;

        int deltaAngle = mTiltDegree - absOffsetAngle;

        if (isForward && isOver) {
            int prevPos = prev(mAnchorInfo.position, 1);
            mAnchorInfo.update(prevPos, -deltaAngle);
        }

        if (!isForward && isOver) {
            int nextPos = next(mAnchorInfo.position, 1);
            mAnchorInfo.update(nextPos, deltaAngle);
        }
        mReviseScroller.startScroll(mAnchorInfo.offsetDegree, 0);
        notifyChanged();
    }

    //将deltaY理解为点在圆周上的位移
    private boolean trackMotionScroll(int toDegree) {
        int deltaDegree = toDegree - mAnchorInfo.offsetDegree;

        if (deltaDegree == 0) {
            ViewCompat.postInvalidateOnAnimation(this);
            return false;
        }

        int roundCount;
        int scrollPosition;

        if (!mCycle) {
            //已经达到边界，此时不能继续滑动
            if (((toDegree > 0 && mAnchorInfo.position == 0) || (toDegree < 0 && mAnchorInfo.position == mCount - 1))) {
                mAnchorInfo.update(mAnchorInfo.position, 0);
                ViewCompat.postInvalidateOnAnimation(this);
                return true;
            }

            deltaDegree += mAnchorInfo.offsetDegree;
            roundCount = deltaDegree / mTiltDegree;
            if (roundCount < 0) {
                scrollPosition = next(mAnchorInfo.position, -roundCount);
                if (scrollPosition >= mCount - 1) {
                    scrollPosition = mCount - 1;
                    mAnchorInfo.update(scrollPosition, 0);
                } else {
                    deltaDegree -= roundCount * mTiltDegree;
                    mAnchorInfo.update(scrollPosition, deltaDegree);
                }
            } else if (roundCount > 0) {
                scrollPosition = prev(mAnchorInfo.position, roundCount);
                if (scrollPosition <= 0) {
                    scrollPosition = 0;
                    mAnchorInfo.update(scrollPosition, 0);
                } else {
                    deltaDegree -= roundCount * mTiltDegree;
                    mAnchorInfo.update(scrollPosition, deltaDegree);
                }
            } else
                mAnchorInfo.update(mAnchorInfo.position, deltaDegree);
        } else {
            deltaDegree += mAnchorInfo.offsetDegree;
            roundCount = deltaDegree / mTiltDegree;
            if (roundCount < 0) {
                scrollPosition = next(mAnchorInfo.position, -roundCount);
                deltaDegree -= roundCount * mTiltDegree;
            } else {
                scrollPosition = prev(mAnchorInfo.position, roundCount);
                deltaDegree -= roundCount * mTiltDegree;
            }
            mAnchorInfo.update(scrollPosition, deltaDegree);
        }

        ViewCompat.postInvalidateOnAnimation(this);
        return false;
    }

    //居中绘制text,paint已经设置了text size
    private float getTextBaseline(Paint paint, float centerY) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return centerY - (fontMetrics.bottom + fontMetrics.top) / 2;
    }

    //将center item作为anchor，position相对于adapter来讲的，offsetAngle指旋转偏移角度
    private class AnchorInfo {
        int position;
        int offsetDegree;

        float calTiltDegree;
        float calOffsetDegree;
        float radius;//滚轮的半径
        float polarDis;//圆心到content rect中心的距离

        void init() {
            calTiltDegree = mTiltDegree * 1f / PRECISION_FACTOR;

            offsetDegree = 0;
            calOffsetDegree = 0f;

            position = 0;
            radius = (float) (180 * mItemHeight / (calTiltDegree * Math.PI));
            polarDis = (float) (radius * Math.cos(calTiltDegree * Math.PI / 360));
        }

        void update(int pos, int angle) {
            position = pos;
            offsetDegree = angle;
            calOffsetDegree = offsetDegree * 1f / PRECISION_FACTOR;
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            update();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            update();
        }

        private void update() {
            mTouchMode = Mode.RESET;
            mFlingScroller.abort();
            mReviseScroller.abort();

            mHalfPageTiltDrawCount = mRealHalfPageTiltDrawCount;
            mTotalPageTiltDrawCount = mRealTotalPageTiltDrawCount;
            reviseParameter(mAnchorInfo.position);
            invalidate();
        }
    }

    //里面的degree都是乘以PRECISION_FACTOR后的值
    private class ReviseScroller implements Runnable {
        private boolean mAbort;
        private Scroller mScroller;

        ReviseScroller(Context context) {
            mScroller = new Scroller(context);
            mAbort = false;
        }

        void startScroll(int startDegree, int endDegree) {
            mAbort = false;

            mScroller.startScroll(0, startDegree, 0, endDegree - startDegree, REVISE_DURATION_MILLIS);
            ViewCompat.postOnAnimation(PickerView.this, this);
        }

        void abort() {
            if (!mAbort) {
                mAbort = true;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    removeCallbacks(this);
                }
            }
        }

        @Override
        public void run() {
            if (!mAbort) {
                boolean more = mScroller.computeScrollOffset();
                int curDegree = mScroller.getCurrY();

                //                Log.e("ReviseScroller delta",curDegree+" ");
                trackMotionScroll(curDegree);

                if (more) {
                    ViewCompat.postOnAnimation(PickerView.this, this);
                } else {
                    removeCallbacks(this);
                    ViewCompat.postInvalidateOnAnimation(PickerView.this);
                }
            }
        }
    }

    private class FlingScroller implements Runnable {
        private boolean mAbort;
        private Scroller mScroller;
        private int mLastDistance;

        FlingScroller(Context context) {
            mScroller = new Scroller(context, new AccelerateDecelerateInterpolator(), true);
            mAbort = false;
        }

        void startFling(int vel) {
            int scrollDistanceRange = getHeight() * 3;//(int) (mItemHeight*mTotalPageTiltDrawCount);

            mAbort = false;
            mLastDistance = 0;

            mScroller.fling(0, 0, 0, vel, 0, 0, -scrollDistanceRange, scrollDistanceRange);
            ViewCompat.postOnAnimation(PickerView.this, this);
        }

        void abort() {
            if (!mAbort) {
                mAbort = true;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    removeCallbacks(this);
                }
            }
        }

        @Override
        public void run() {
            if (!mAbort) {
                boolean more = mScroller.computeScrollOffset();
                int curDistance = mScroller.getCurrY();
                int delta = curDistance - mLastDistance;

                int absDelta = Math.abs(delta);
                //                int threshold = (int) Math.min(10,mItemHeight/3);
                //                if(absDelta>threshold)
                //                    absDelta = threshold;
                delta = delta > 0 ? absDelta : -absDelta;

                boolean atEdge = trackMotionScroll((int) (mAnchorInfo.offsetDegree + PRECISION_FACTOR * delta * 180f / (Math.PI * mAnchorInfo.radius)));

                if (atEdge || !more) {
                    removeCallbacks(this);
                    if (!mScroller.isFinished())
                        mScroller.abortAnimation();

                    revise();
                    return;
                }

                mLastDistance = curDistance;
                ViewCompat.postOnAnimation(PickerView.this, this);
            }
        }
    }

    private void notifyChanged() {
        if (mTouchMode == Mode.RESET && mPrevPosition != mAnchorInfo.position) {
            mCallbackRunnable.call(mPrevPosition, mAnchorInfo.position);
            mPrevPosition = mAnchorInfo.position;
        }
    }

    public interface OnPickListener {
        void onPick(PickerView picker, int oldPosition, int newPosition);
    }

    class CallbackRunnable implements Runnable {
        int mOldPosition, mNewPosition;

        void call(int oldPosition, int newPosition) {
            mOldPosition = oldPosition;
            mNewPosition = newPosition;
            post(this);
        }

        @Override
        public void run() {
            if (mOnPickListener != null)
                mOnPickListener.onPick(PickerView.this, mOldPosition, mNewPosition);
        }
    }

    private static class SavedState extends BaseSavedState {
        private final int mIndex;

        private SavedState(Parcelable superState, int index) {
            super(superState);
            mIndex = index;
        }

        private SavedState(Parcel in) {
            super(in);
            mIndex = in.readInt();
        }

        public int getIndex() {
            return mIndex;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mIndex);
        }

        @SuppressWarnings({"unused", "hiding"})
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getValueIndex());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setValueIndex(ss.getIndex(), false);
    }

}