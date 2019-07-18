package com.simplelibrary.dialog;

import android.view.View;

import com.simplelibrary.R;
import com.simplelibrary.utils.ChoosePhotoUtils;

/**
 * Created by dangdang on 2018/11/24.
 */

public class ChoosePhotoDialog extends BaseDialog {
    protected ChoosePhotoUtils mChoosePhotoUtils;
    private ChoosePhotoUtils.OnChooseListener mOnChooseListener;
    private int mAspectX, mAspectY;
    private int mMaxKB = 150;


    {
        setLayoutId(R.layout.dialog_choose_photo_default);
        mChoosePhotoUtils = new ChoosePhotoUtils();
    }


    @Override
    protected void initView() {
        super.initView();
        setClickId(R.id.tv_pw_title, R.id.tv_pw_confirm, R.id.tv_pw_cancel);
        mChoosePhotoUtils.setChooseListener(mOnChooseListener)
                .setAspectXY(mAspectX, mAspectY)
                .setLuBanMaxKB(mMaxKB);
    }

    public void setClickId(int openCameraId, int openAlbumId, int cancelId) {
        View cameraView = mViewHolder.getView(openCameraId);
        if (cameraView != null) {
            cameraView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mChoosePhotoUtils.requestCamera();
                    dismiss();
                }
            });
        }
        View albumView = mViewHolder.getView(openAlbumId);
        if (albumView != null) {
            albumView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mChoosePhotoUtils.requestAlbum();
                    dismiss();
                }
            });
        }
        View cancelView = mViewHolder.getView(cancelId);
        if (cancelView != null) {
            cancelView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

    }

    public void setOnChooseListener(ChoosePhotoUtils.OnChooseListener onChooseListener) {
        mOnChooseListener = onChooseListener;
    }

    public void setAspectXY(int aspectX, int aspectY) {
        mAspectX = aspectX;
        mAspectY = aspectY;
    }

    public void setMaxKB(int kb) {
        mMaxKB = kb;
    }

}
