package vattis.xlink.cn.simplecall;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * 说明：
 * Created by jjs on 2019/6/20
 */
public class CallManager {
    private static final CallManager ourInstance = new CallManager();
    private static CallEntityDao mBoxEntityDao;

    public static CallManager getInstance() {
        return ourInstance;
    }

    private CallManager() {

    }

    public static void init(Context context) {
        DaoMaster.DevOpenHelper devOpenHelper = new DaoMaster.DevOpenHelper(context, "draftBox");
        mBoxEntityDao = new DaoMaster(devOpenHelper.getWritableDatabase()).newSession().getCallEntityDao();
    }


    public void delete(long boxId) {
        mBoxEntityDao.deleteByKey(boxId);
    }

    public List<CallEntity> getList() {
        return mBoxEntityDao.queryBuilder().orderAsc(CallEntityDao.Properties.UpdateTime).list();
    }

    public CallEntity get(Long boxId) {
        return mBoxEntityDao.load(boxId);
    }

    public void update(CallEntity box) {
        box.updateTime = System.currentTimeMillis();
        CallEntity load = mBoxEntityDao.load(box.id);
        if (load == null) {
            mBoxEntityDao.insert(box);
        } else {
            mBoxEntityDao.update(box);
        }
        EventBus.getDefault().post("updateList");
    }

}
