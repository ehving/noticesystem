package com.notice.system.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code; // 0成功，1失败
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.setCode(1);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }

}

