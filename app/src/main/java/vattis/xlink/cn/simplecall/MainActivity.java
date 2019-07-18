package vattis.xlink.cn.simplecall;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;
import com.blankj.utilcode.util.SPUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.simplelibrary.base.BaseActivity;
import com.simplelibrary.mvp.BasePersenter;

import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;

public class MainActivity extends BaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rv)
    RecyclerView rv;
    @BindView(R.id.floatButton)
    FloatingActionButton mActionButton;
    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.open)
    Switch mSwitch;


    private CallManager mManager;
    private BaseQuickAdapter mAdapter;
    private boolean isUpdate = false;
    private SpeechSynthesizer mSpeechSynthesizer = SpeechSynthesizer.getInstance();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected BasePersenter createPersenter() {
        mManager = CallManager.getInstance();
        mSpeechSynthesizer.setContext(this);
        mSpeechSynthesizer.setAppId("16829635");
        mSpeechSynthesizer.setApiKey("5z4TVm8Y6uEATuFPvoiwC4Ch", "1xfAF4My4UZC43nitWaMOSsC79MaKCqa");
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        int code = mSpeechSynthesizer.initTts(TtsMode.ONLINE);
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.release();
            mSpeechSynthesizer = null;
        }
    }

    @Override
    protected void initView() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("SimpleCall");
        //  toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                readyGo(AddActivity.class);
                return false;
            }
        });
        mAdapter = new BaseQuickAdapter<CallEntity, BaseViewHolder>(R.layout.recycler_user) {
            @Override
            protected void convert(BaseViewHolder helper, CallEntity item) {
                helper.setText(R.id.tv_name, item.name)
                        .setText(R.id.tv_phone, item.phone + (!TextUtils.isEmpty(item.wechat) ? "/" + item.wechat : ""))
                        .setVisible(R.id.iv_call, !TextUtils.isEmpty(item.phone) || isUpdate)
                        .setVisible(R.id.iv_wechat, !TextUtils.isEmpty(item.wechat) || isUpdate)
                        .setImageResource(R.id.iv_call, isUpdate ? R.mipmap.ic_delete : R.mipmap.ic_call_phone)
                        .setImageResource(R.id.iv_wechat, isUpdate ? R.mipmap.ic_edit : R.mipmap.ic_call_wechat)
                        .itemView
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mSpeechSynthesizer.stop();
                                mSpeechSynthesizer.speak(item.name);
                            }
                        });
                helper.getView(R.id.iv_call)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isUpdate) {
                                    mManager.delete(item.id);
                                } else {
                                    mManager.update(item);
                                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.phone)));
                                }
                                initData();
                            }
                        });
                helper.getView(R.id.iv_wechat)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isUpdate) {
                                    Bundle bundle = new Bundle();
                                    bundle.putLong("id", item.id);
                                    readyGo(AddActivity.class, bundle, -1);
                                } else {

                                }
                            }
                        });
                GlideUtils.loadHeadUrl(getContext(), item.head, helper.getView(R.id.iv_head));

            }
        };
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mAdapter);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUpdate = !isUpdate;
                mAdapter.notifyDataSetChanged();
                mActionButton.setImageResource(isUpdate ? R.mipmap.floating_success : R.mipmap.floating_edit);
                mActionButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(isUpdate ? R.color.colorPrimary : R.color.colorAccent)));
            }
        });

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPUtils.getInstance().put("switch", isChecked);
                invalidateOptionsMenu();
            }
        });
        mSwitch.setChecked(SPUtils.getInstance().getBoolean("switch", true));
    }

    @Override
    protected void initData() {
        mAdapter.setNewData(mManager.getList());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean aSwitch = SPUtils.getInstance().getBoolean("switch", true);
        mActionButton.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER);
        mActionButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if (aSwitch) {
            mActionButton.show();
            getMenuInflater().inflate(R.menu.menu_main, menu);
        } else {
            mActionButton.hide();
        }
        return true;
    }

    @Subscribe
    public void updateList(String s) {
        if ("updateList".equals(s)) {
            initData();
        }
    }
}
