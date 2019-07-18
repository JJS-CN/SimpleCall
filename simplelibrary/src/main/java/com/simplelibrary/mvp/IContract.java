package com.simplelibrary.mvp;

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.support.annotation.StringRes;

/**
 * 说明：
 * Created by jjs on 2018/11/22
 */

public interface IContract {
    interface IView {
        Lifecycle getLifecycle();

        void showShortToast(CharSequence charSequence);

        void showShortToast(@StringRes int stringsId);

        void showLoadDialog();

        void dismissLoadDialog();

        void showHttpStatusView(int status);

        void onError(int status, String message);

        void close();

        Context getContext();

        //<T> AutoDisposeConverter<T> bindAutoDispose();
    }

        interface IPersenter extends GenericLifecycleObserver {
        void subscribe();

        void unSubscribe();

        // <T> AutoDisposeConverter<T> bindAutoDispose();
    }

    interface IModel {
        void subscribe();

        void unSubscribe();
    }
}
