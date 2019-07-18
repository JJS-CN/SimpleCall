package com.simplelibrary.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.simplelibrary.BaseConst;
import com.simplelibrary.R;
import com.simplelibrary.mvp.BasePersenter;
import com.simplelibrary.mvp.IContract;
import com.simplelibrary.mvp.ListPersenter;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 说明：
 * Created by jjs on 2018/12/21
 */
public abstract class BaseFragment<P extends BasePersenter> extends Fragment implements IContract.IView {
    protected View rootView;
    private boolean isInitView = false;
    private boolean isVisible = false;
    private BaseActivity mActivity;

    private Unbinder mUnBinder;
    protected P mPersenter;

    /*** httpStatus */
    private SparseArray<View> mStatusArray;
    private SparseArray<View> mMarginStatusArray;
    private Unbinder mUnbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(getLayoutId(), container, false);
        mUnbinder = ButterKnife.bind(this, rootView);
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        mPersenter = createPersenter();//todo 考虑上移到initView之前实例化
        initView();
        if (mPersenter != null && mPersenter instanceof ListPersenter) {
            ((ListPersenter) mPersenter).bindRecycler();
        }
        isInitView = true;
        // isCanLoadData();
        initData();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (BaseActivity) context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //isVisibleToUser这个boolean值表示:该Fragment的UI 用户是否可见，获取该标志记录下来
        if (isVisibleToUser) {
            isVisible = true;
            // isCanLoadData();
        } else {
            isVisible = false;
        }
    }

   /* private void isCanLoadData() {
        //所以条件是view初始化完成并且对用户可见
        if (isInitView && isVisible) {


            //防止重复加载数据
            isInitView = false;
            isVisible = false;
        }
    }*/

    /*** 跳转相关 **/
    public void readyGo(Class<?> clazz, String key, String value, int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        readyGo(clazz, bundle, requestCode);
    }

    public void readyGo(Class<?> clazz, String key, boolean value, int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(key, value);
        readyGo(clazz, bundle, requestCode);
    }

    public void readyGo(Class<?> clazz, String key, double value, int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putDouble(key, value);
        readyGo(clazz, bundle, requestCode);
    }

    public void readyGo(Class<?> clazz, String key, int value, int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        readyGo(clazz, bundle, requestCode);
    }

    public void readyGo(Class<?> clazz, String key, Serializable value, int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(key, value);
        readyGo(clazz, bundle, requestCode);
    }

    public void readyGo(Class<?> clazz) {
        readyGo(clazz, null, -1);
    }

    public void readyGo(Class<?> clazz, Bundle bundle) {
        readyGo(clazz, bundle, -1);
    }

    public void readyGo(Class<?> clazz, int requestCode) {
        readyGo(clazz, null, requestCode);
    }

    public void readyGo(Class<?> clazz, Bundle bundle, int requestCode) {
        if (clazz != null) {
            Intent intent = new Intent(getActivity(), clazz);
            if (null != bundle) {
                intent.putExtras(bundle);
            }
            if (requestCode == -1) {
                startActivity(intent);
            } else {
                startActivityForResult(intent, requestCode);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUnbinder != null) {
            mUnbinder.unbind();
        }
        EventBus.getDefault().unregister(this);
    }

    protected abstract P createPersenter();

    /**
     * 加载页面布局文件
     */
    protected abstract int getLayoutId();

    /**
     * 让布局中的view与fragment中的变量建立起映射
     */
    protected abstract void initView();

    /**
     * 加载要显示的数据
     */
    protected abstract void initData();

    @Override
    public void showShortToast(CharSequence charSequence) {
        mActivity.showShortToast(charSequence);
    }

    @Override
    public void showShortToast(int stringsId) {
        mActivity.showShortToast(stringsId);
    }

    @Override
    public void showLoadDialog() {
        mActivity.showLoadDialog();
    }

    @Override
    public void dismissLoadDialog() {
        mActivity.dismissLoadDialog();
    }

    @Override
    public void showHttpStatusView(int status) {
        //dialog展示时，不需要statusView的加载框
        if (BaseConst.Default.mBaseHttpStatus == null || status == BaseConst.HttpStatus.Status_LOADING || rootView == null) {
            return;
        }
        if (rootView instanceof ViewGroup) {

            removeStatusView();
            View view = getStatusView(status, true);
            if (view != null) {
                ((ViewGroup) rootView).addView(view);
            }
        }
    }

    private void removeStatusView() {
        for (int i = ((ViewGroup) rootView).getChildCount() - 1; i > 0; i--) {
            ((ViewGroup) rootView).removeViewAt(i);
        }
    }

    /**
     * @param status       http状态
     * @param hasTopMargin 是否需要头部margin，true适用activity，false适用recyclerview
     */
    public View getStatusView(int status, boolean hasTopMargin) {
        View statusView = null;
        if (hasTopMargin) {
            if (mMarginStatusArray == null) {
                mMarginStatusArray = new SparseArray<>(6);
            }
            statusView = mMarginStatusArray.get(status);
        } else {
            if (mStatusArray == null) {
                mStatusArray = new SparseArray<>(6);
            }
            statusView = mStatusArray.get(status);
        }
        //新建StatusView
        if (statusView == null) {
            int layoutId = 0;
            if (status == BaseConst.HttpStatus.Status_LOADING) {
                layoutId = BaseConst.Default.mBaseHttpStatus.LoadingLayout();
            } else if (status == BaseConst.HttpStatus.Status_NOTHING) {
                layoutId = BaseConst.Default.mBaseHttpStatus.NothingLayout();
            } else if (status == BaseConst.HttpStatus.Status_NETWORK) {
                layoutId = BaseConst.Default.mBaseHttpStatus.NetWorkErrorLayout();
            } else if (status == BaseConst.HttpStatus.Status_ERROR) {
                layoutId = BaseConst.Default.mBaseHttpStatus.ErrorLayout();
            }
            if (layoutId == 0) {
                return null;
            }
            statusView = View.inflate(mActivity, layoutId, null);
            FrameLayout.MarginLayoutParams lp = new FrameLayout.MarginLayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            if (hasTopMargin) {
                lp.topMargin = getResources().getDimensionPixelSize(R.dimen.ActionBarHeight);
            }
            statusView.setLayoutParams(lp);
            if (hasTopMargin) {
                mMarginStatusArray.put(status, statusView);
            } else {
                mStatusArray.put(status, statusView);
            }
        }
        return statusView;
    }

    @Override
    public void onError(int status, String message) {

    }

    @Override
    public void close() {

    }
}
