package vattis.xlink.cn.simplecall;

import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.blankj.utilcode.util.RegexUtils;
import com.simplelibrary.base.BaseActivity;
import com.simplelibrary.dialog.ChoosePhotoDialog;
import com.simplelibrary.mvp.BasePersenter;
import com.simplelibrary.utils.ChoosePhotoUtils;

import java.io.File;

import butterknife.BindView;

/**
 * 说明：
 * Created by jjs on 2019/7/12
 */
public class AddActivity extends BaseActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.iv_head)
    ImageView mIvHead;
    @BindView(R.id.edit_name)
    AppCompatEditText mEditName;
    @BindView(R.id.edit_phone)
    AppCompatEditText mEditPhone;
    @BindView(R.id.edit_wechat)
    AppCompatEditText mEditWechat;


    private CallEntity mEntity;
    private ChoosePhotoDialog mPhotoDialog;

    {
        hasClicksInspect = false;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_add;
    }

    @Override
    protected BasePersenter createPersenter() {
        return null;
    }

    @Override
    protected void initView() {
        long id = getIntent().getLongExtra("id", -1);

        mPhotoDialog = new ChoosePhotoDialog();
        mPhotoDialog.hasBottomUP(true);
        mPhotoDialog.setMaxKB(3000);
        mPhotoDialog.setAspectXY(1, 1);
        mPhotoDialog.setOnChooseListener(new ChoosePhotoUtils.OnChooseListener() {
            @Override
            public void onChoose(File originalFile, File compressFile) {
                mEntity.head = originalFile.getPath();
                GlideUtils.loadHeadUrl(AddActivity.this, mEntity.head, mIvHead);
            }
        });

        if (id < 0) {
            mToolbar.setTitle("新增");
            mEntity = new CallEntity();
        } else {
            mToolbar.setTitle("编辑");
            mEntity = CallManager.getInstance().get(id);
        }
        GlideUtils.loadHeadUrl(AddActivity.this, mEntity.head, mIvHead);
        mEditName.setText(mEntity.name);
        mEditPhone.setText(mEntity.phone);
        mEditWechat.setText(mEntity.wechat);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.mipmap.ic_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String name = mEditName.getText().toString().trim();
                String phone = mEditPhone.getText().toString().trim();
                String wechat = mEditWechat.getText().toString().trim();
                if (TextUtils.isEmpty(mEntity.head)) {
                    showShortToast("请选择头像");
                    return true;
                } else if (TextUtils.isEmpty(name)) {
                    showShortToast("请输入姓名");
                    return true;
                } else if (TextUtils.isEmpty(phone) || !RegexUtils.isMobileSimple(phone)) {
                    showShortToast("请输入正确手机号");
                    return true;
                }
                mEntity.name = name;
                mEntity.phone = phone;
                mEntity.wechat = wechat;
                CallManager.getInstance().update(mEntity);
                finish();
                return false;
            }
        });
        mIvHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoDialog.show(AddActivity.this);
            }
        });

    }

    @Override
    protected void initData() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }
}
