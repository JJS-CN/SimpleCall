package vattis.xlink.cn.simplecall;

import android.app.Application;

/**
 * 说明：
 * Created by jjs on 2019/7/12
 */
public class APP extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CallManager.init(this);
    }
}
