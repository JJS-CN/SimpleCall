package com.simplelibrary.mvp;

/**
 * 说明：
 * Created by jjs on 2018/11/27
 */

public class BaseModel<P extends BasePersenter> implements IContract.IModel {
    protected P mPersenter;
    protected IContract.IView mView;


    public BaseModel(P persenter) {
        mPersenter = persenter;
        if (mPersenter != null)
            mView = mPersenter.mView;
    }

    @Override
    public void subscribe() {

    }

    public void showLoadDialog() {
        if (mView != null) {
            mView.showLoadDialog();
        }
    }

    public void dismissLoadDialog() {
        if (mView != null) {
            mView.dismissLoadDialog();
        }
    }

    @Override
    public void unSubscribe() {
        mPersenter = null;
    }
}
