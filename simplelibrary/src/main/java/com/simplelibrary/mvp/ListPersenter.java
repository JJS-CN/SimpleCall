package com.simplelibrary.mvp;

import android.graphics.Color;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.simplelibrary.R;
import com.simplelibrary.http.BaseObserver;
import com.simplelibrary.http.IBaseEntity;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * 说明：
 * Created by jjs on 2018/11/28
 */

public class ListPersenter extends BasePersenter<ListContract.View, IContract.IModel> implements ListContract.Persenter, SwipeRefreshLayout.OnRefreshListener {
    private int pageNo = 1;
    private int pageSize = 10;
    private int lastPage = -1;//双等级
    private boolean hasDoubleAPi;//是否双数据内容
    private SwipeRefreshLayout mSwipe;
    private RecyclerView mRv;
    private BaseQuickAdapter mAdapter;
    private Disposable mDisposable2;
    private OnBeforeLoadDataListener mOnBeforeLoadDataListener;
    private OnAfterLoadDataListener mOnAfterLoadDataListener;
    private boolean isBind = false;
    private boolean hasNextLoadMore = true;//是否还有数据需要加载

    public ListPersenter(ListContract.View view) {
        super(view);
    }

    @Override
    protected IContract.IModel createModel() {
        return null;
    }

    public void setHasDoubleAPi(boolean hasDoubleAPi) {
        this.hasDoubleAPi = hasDoubleAPi;
    }

    public boolean isNextApi() {
        //是否是下一个api
        return lastPage > -1;
    }

    @Override
    public void bindRecycler() {
        if (isBind) {
            return;
        }
        isBind = true;
        this.mSwipe = mView.getSwipeRefreshLayout();
        this.mRv = mView.getRecyclerView();
        if (mRv == null) {
            throw new NullPointerException("SRvPersenter:  RecyclerView is Null");
        } else {
            if (mRv.getLayoutManager() == null) {
                mRv.setLayoutManager(new LinearLayoutManager(mRv.getContext()));
            }
            if (mRv.getAdapter() == null) {
                mAdapter = new BaseQuickAdapter<Object, BaseViewHolder>(mView.getItemRes(), null) {

                    @Override
                    protected void convert(BaseViewHolder helper, Object item) {
                        if (mView != null)
                            mView.loadItemData(helper, helper.getAdapterPosition(), item);
                    }

                  /*  @Override
                    public int getItemCount() {

                        int count = super.getItemCount();
                        if (hasNextLoadMore && getData().size() > 0 && getData().size() % 2 == 0) {
                            count = count - 1;
                        }
                        Log.e("eeee", "page:" + pageNo + "    " + count + "  " + super.getItemCount());
                        return count;
                    }*/

                };
                mRv.setAdapter(mAdapter);
            } else {
                mAdapter = (BaseQuickAdapter) mRv.getAdapter();
            }
            mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
                @Override
                public void onLoadMoreRequested() {
                    ++pageNo;
                    if (mView != null)
                        mView.reLoadData();
                }
            }, mRv);
            mAdapter.disableLoadMoreIfNotFullPage();
            if (mSwipe != null) {
                mSwipe.setColorSchemeColors(Color.parseColor("#FA9E6F"), Color.parseColor("#FEF193"), Color.parseColor("#C0FBCF"), Color.parseColor("#C5D1FB"));
                mSwipe.setOnRefreshListener(this);
                mSwipe.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipe.setRefreshing(true);
                        onRefresh();
                    }
                });
            } else {
                onRefresh();
            }
        }
    }

    @Override
    public <T extends IBaseEntity> void loadData(Observable<T> observable) {
        if (observable != null) {
            observable.subscribe(new BaseObserver<T>(mView, false) {
                @Override
                protected void onSuccess(T t) {
                    if (mOnAfterLoadDataListener != null) {
                        mOnAfterLoadDataListener.onResult(t);
                    }
                    if (pageNo == 1 && lastPage == -1) {
                        mAdapter.setNewData(t == null ? null : t.getList());
                    } else {
                        if (t != null) {
                            mAdapter.addData(t.getList());
                        }
                    }
                    if (t != null && t.getList() != null && t.getList().size() >= pageSize) {
                        mAdapter.loadMoreComplete();
                    } else {
                        if (hasDoubleAPi && lastPage == -1) {
                            //需要双接口数据时,立即重请求数据
                            lastPage = pageNo;
                            pageNo++;
                            mAdapter.loadMoreComplete();
                            mView.reLoadData();

                        } else {
                            mAdapter.loadMoreEnd();
                            hasNextLoadMore = false;
                        }
                    }

                    if (mAdapter.getItemCount() == 0 && mAdapter.getEmptyViewCount() == 0) {
                        mAdapter.setEmptyView(R.layout.layout_rv_empty, mRv);
                    }
                    if (mOnBeforeLoadDataListener != null) {
                        mOnBeforeLoadDataListener.onResult(t);
                    }
                }

                @Override
                protected Boolean hasLoading() {
                    if (hasLoading && mSwipe != null) {
                        return !mSwipe.isRefreshing();
                    }
                    return super.hasLoading();
                }

                @Override
                public void onSubscribe(Disposable d) {
                    super.onSubscribe(d);
                    mDisposable2 = d;
                }

                @Override
                protected void onError(int status, String msg) {
                    super.onError(status, msg);
                    if (getPage() > 1) {
                        pageNo--;
                    }
                    mAdapter.loadMoreFail();
                }

                @Override
                public void onComplete() {
                    super.onComplete();
                    if (mSwipe != null)
                        mSwipe.setRefreshing(false);
                    mAdapter.setEnableLoadMore(true);
                }
            });
        }
    }

    @Override
    public void refreshData() {
        onRefresh();
    }

    @Override
    public int getPage() {
        return lastPage > -1 ? pageNo - lastPage : pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
        if (pageNo == 1) {
            this.lastPage = -1;
            hasNextLoadMore = true;
        }
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setOnBeforeLoadDataListener(OnBeforeLoadDataListener onBeforeLoadDataListener) {
        mOnBeforeLoadDataListener = onBeforeLoadDataListener;
    }

    @Override
    public void setOnAfterLoadDataListener(OnAfterLoadDataListener onAfterLoadDataListener) {
        mOnAfterLoadDataListener = onAfterLoadDataListener;
    }


    @Override
    public BaseQuickAdapter getAdapter() {
        return mAdapter;
    }


    @Override
    public void onRefresh() {
        setPageNo(1);
        if (mDisposable2 != null && !mDisposable2.isDisposed()) {
            mDisposable2.dispose();
        }
        mAdapter.setEnableLoadMore(false);
        mView.reLoadData();

    }

    public interface OnBeforeLoadDataListener {
        void onResult(IBaseEntity resp);
    }

    public interface OnAfterLoadDataListener {
        void onResult(IBaseEntity resp);
    }

}
