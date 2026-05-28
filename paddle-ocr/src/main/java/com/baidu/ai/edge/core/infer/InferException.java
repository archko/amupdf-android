package com.baidu.ai.edge.core.infer;

import com.baidu.ai.edge.core.base.BaseException;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/infer/InferException.class */
public class InferException extends BaseException {
    public InferException(int i, String str, Throwable th) {
        super(i, str, th);
    }

    public InferException(int i, String str) {
        super(i, str);
    }
}
