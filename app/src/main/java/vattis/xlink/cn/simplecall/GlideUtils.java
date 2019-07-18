package vattis.xlink.cn.simplecall;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

/**
 * 说明：
 * Created by jjs on 2018/12/27
 */
public class GlideUtils {
    public static void loadUrl(Context context, Object url, ImageView mIvImg) {
        load(context, url, mIvImg, null, true);
    }

    public static void loadHeadUrl(Context context, Object url, ImageView mIvImg) {
        load(context, url, mIvImg, RequestOptions.circleCropTransform().placeholder(R.mipmap.img_user_default).error(R.mipmap.img_user_default), true);
    }

    public static void loadCircleUrl(Context context, Object url, ImageView mIvImg) {
        load(context, url, mIvImg, RequestOptions.circleCropTransform(), true);
    }

    private static void load(Context context, Object url, ImageView mIvImg, RequestOptions options, boolean hasAllVisible) {
        RequestBuilder<Drawable> load = Glide.with(context).load(url);
        if (options != null) {
            load.apply(options);
        }
        if (mIvImg != null) {
            if (!hasAllVisible && (TextUtils.isEmpty(url.toString()) || url.toString().endsWith("null"))) {
                mIvImg.setVisibility(View.GONE);
            }
            load.into(mIvImg);
        }

    }
}
