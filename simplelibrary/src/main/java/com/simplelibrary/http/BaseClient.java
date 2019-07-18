package com.simplelibrary.http;

import android.text.TextUtils;

import com.simplelibrary.BaseConst;
import com.simplelibrary.http.adapter.RxJavaFactory;
import com.simplelibrary.http.converter.GsonConverterFactory;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * 说明：client,由于ApiService每个app都不相同，所以需要子类继承父类，传入apiService类；
 * 并实现单例；
 * Created by jjs on 2018/7/2.
 */

public abstract class BaseClient<T> {

    private T api;
    protected OkHttpClient.Builder mOkhttpBuilder;
    //todo 逻辑待优化
    public BaseClient() {
        this.buildOkHttp();
    }

    protected void buildOkHttp() {

     /*   if (mOkhttpBuilder == null) {
            mOkhttpBuilder = new OkHttpClient.Builder();
            //mOkhttpBuilder.addInterceptor(new TokenInterceptor());
            mOkhttpBuilder.addInterceptor(new FixedInterceptor());
            mOkhttpBuilder.addInterceptor(new LoggerInterceptor());
            mOkhttpBuilder.cookieJar(new NovateCookieManger(Utils.getApp()));
        }*/
    }

    /**
     * 获取对应的Service
     */
    public T create(Class<T> service) {
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl(BaseConst.Default.isDebug && !TextUtils.isEmpty(BaseConst.Default.Host_Http_Debug) ? BaseConst.Default.Host_Http_Debug : BaseConst.Default.Host_Http)
                    .client(mOkhttpBuilder.build())
                    .addCallAdapterFactory(RxJavaFactory.getInstance())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(service);
        }
        return api;
    }

}
