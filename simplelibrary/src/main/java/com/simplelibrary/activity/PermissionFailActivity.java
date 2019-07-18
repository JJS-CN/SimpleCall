package com.simplelibrary.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.PermissionUtils;
import com.simplelibrary.R;

/**
 * 说明：
 * Created by jjs on 2018/11/22
 */

public class PermissionFailActivity extends Activity {

    public static final String PermissionFail_Message = "message";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);
        setContentView(R.layout.activity_permission_fail);
        String message = getIntent().getStringExtra(PermissionFail_Message);
        if (!TextUtils.isEmpty(message)) {
            TextView title = findViewById(R.id.tv_title);
            title.setText(message);
        }
        findViewById(R.id.tv_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
        findViewById(R.id.tv_submit)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PermissionUtils.launchAppDetailsSettings();
                        finish();
                    }
                });
    }
}
