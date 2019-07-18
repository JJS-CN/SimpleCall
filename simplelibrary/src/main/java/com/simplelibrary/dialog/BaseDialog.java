package com.simplelibrary.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.chad.library.adapter.base.BaseViewHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;


/**
 * 内容通过 setArguments(); 进行添加，传入bundle
 * 在initView方法中执行对View的操作
 * Created by jjs on 2018/5/28
 */

public class BaseDialog extends DialogFragment {
    protected int mLayoutId;
    private OnCustomListener mCustomListener;
    public BaseViewHolder mViewHolder;
    private boolean isBottom;
    private boolean windowTransparent;
    private boolean dialogTransparent = true;


    /**
     * 设置布局id
     */
    public BaseDialog setLayoutId(@LayoutRes int layoutId) {
        mLayoutId = layoutId;
        return this;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mRootView = inflater.inflate(mLayoutId, container);
        mViewHolder = new BaseViewHolder(mRootView);
        updateView();
        if (mCustomListener != null) {
            mCustomListener.onCustom(mViewHolder, this);
        }
        Window mWindow = getDialog().getWindow();
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        return mRootView;
    }

    //继承方式使用时，在构造方法中初始化数据，initView中解析绑定数据
    //展示后需要更新UI时，需判断viewholder是否为空，因为调用show()到onCreateView需要一段时间
    protected void initView() {
    }

    //做一次非空判断，从走initview
    public void updateView() {
        if (mViewHolder != null && isAdded()) {
            initView();
        }
    }

    //会在初始化时调用一次，可通过viewHolder拿到所有控件
    public BaseDialog setCustomListener(OnCustomListener customListener) {
        this.mCustomListener = customListener;
        return this;
    }

    public interface OnCustomListener {
        void onCustom(BaseViewHolder holder, BaseDialog dialog);
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), "dialog");
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        manager.beginTransaction().remove(this).commitAllowingStateLoss();
        try {
            Class c = Class.forName("android.support.v4.app.DialogFragment");
            Constructor con = c.getConstructor();
            Object obj = con.newInstance();
            Field dismissed = c.getDeclaredField("mDismissed");
            dismissed.setAccessible(true);
            dismissed.set(obj, false);
            Field shownByMe = c.getDeclaredField("mShownByMe");
            shownByMe.setAccessible(true);
            shownByMe.set(obj, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
        updateView();
        //super.show(manager, tag);
    }

    @Override
    public void dismiss() {
        dismissAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isBottom) {
            Window window = getDialog().getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
        if (windowTransparent) {
            Window window = getDialog().getWindow();
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = 0.0f;
            window.setAttributes(windowParams);
        }
        if (dialogTransparent) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

    }

    public BaseDialog hasTransparentForWindow(boolean windowTransparent) {
        this.windowTransparent = windowTransparent;
        return this;
    }

    public BaseDialog hasTransparentForDialog(boolean dialogTransparent) {
        this.dialogTransparent = dialogTransparent;
        return this;
    }

    public void hasBottomUP(boolean isBottom) {
        this.isBottom = isBottom;
    }


    //    public void hasBottomDialog() {
    //        Window window = getDialog().getWindow();
    //        WindowManager.LayoutParams params = window.getAttributes();
    //        params.gravity = Gravity.BOTTOM;
    //        params.width = WindowManager.LayoutParams.MATCH_PARENT;
    //        window.setAttributes(params);
    //        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    //    }


}
