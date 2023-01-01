package com.baidu.ai.edge.ui.activity;

import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel;
import com.baidu.ai.edge.ui.view.model.BaseResultModel;

import java.util.List;

/**
 * Created by ruanshimin on 2018/11/12.
 */
public interface ResultListener {

    interface OcrListener {
        void onResult(List<BasePolygonResultModel> models);
    }

    interface ListListener<T extends BaseResultModel> {
        void onResult(List<BasePolygonResultModel> models);
    }
}
