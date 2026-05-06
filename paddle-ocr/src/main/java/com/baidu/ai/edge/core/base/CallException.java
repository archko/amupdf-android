package com.baidu.ai.edge.core.base;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/CallException.class */
public class CallException extends BaseException {
    public CallException(int i, String str, Throwable th) {
        super(i, str, th);
    }

    public CallException(int i, String str) {
        super(i, str);
    }
}
