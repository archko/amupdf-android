package com.baidu.ai.edge.core.base;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/BaseException.class */
public class BaseException extends Exception {

    /* renamed from: a  reason: collision with root package name */
    protected int f9a;

    public BaseException(int i, String str, Throwable th) {
        super(str, th);
        this.f9a = i;
    }

    public BaseException(int i, String str) {
        super(str);
        this.f9a = i;
    }

    public static BaseException transform(OutOfMemoryError outOfMemoryError) {
        return new BaseException(Consts.EC_BASE_JNI_OUT_OF_MEMORY, "SDKException: out of memory:", outOfMemoryError);
    }

    public static BaseException transform(Exception exc, String str) {
        String message = exc.getMessage();
        int indexOf = message.indexOf(58);
        if (indexOf > 0) {
            return new BaseException(Integer.parseInt(message.substring(0, indexOf)), str + message.substring(indexOf + 1));
        }
        return new BaseException(Consts.EC_BASE_JNI_UNKNOWN, str + exc.getMessage(), exc);
    }

    public int getErrorCode() {
        return this.f9a;
    }
}
