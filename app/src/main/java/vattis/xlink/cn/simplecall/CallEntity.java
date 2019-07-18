package vattis.xlink.cn.simplecall;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 说明：
 * Created by jjs on 2019/7/12
 */
@Entity
public class CallEntity {
    @Id
    public Long id;
    public String head;
    public String name;
    public String phone;
    public String wechat;
    public long updateTime;
    @Generated(hash = 203751639)
    public CallEntity(Long id, String head, String name, String phone,
            String wechat, long updateTime) {
        this.id = id;
        this.head = head;
        this.name = name;
        this.phone = phone;
        this.wechat = wechat;
        this.updateTime = updateTime;
    }
    @Generated(hash = 1699550873)
    public CallEntity() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getHead() {
        return this.head;
    }
    public void setHead(String head) {
        this.head = head;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPhone() {
        return this.phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getWechat() {
        return this.wechat;
    }
    public void setWechat(String wechat) {
        this.wechat = wechat;
    }
    public long getUpdateTime() {
        return this.updateTime;
    }
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
