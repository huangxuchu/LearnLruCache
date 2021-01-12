package com.learn.lrucache.bean.request;

/**
 * @author Huangxuchu
 * @date 2021/1/12
 * @description
 */
public class GetDataRequest {
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "GetDataRequest{" +
                "remark='" + remark + '\'' +
                '}';
    }
}
