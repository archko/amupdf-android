package com.baidu.ai.edge.core.pose;

import android.graphics.Point;
import com.baidu.ai.edge.core.base.BaseResultModel;
import java.util.ArrayList;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/pose/PoseResultModel.class */
public class PoseResultModel extends BaseResultModel implements IPoseResultModel {
    private int d;
    private int e;
    private boolean f = false;
    private Point g;
    private List<IPoseResultModel> h;

    public void setPairs(List<PoseResultModel> list) {
        this.h = new ArrayList();
        this.h.addAll(list);
    }

    @Override // com.baidu.ai.edge.core.pose.IPoseResultModel
    public List<IPoseResultModel> getPairs() {
        return this.h;
    }

    public void setPoint(Point point) {
        this.g = point;
    }

    @Override // com.baidu.ai.edge.core.pose.IPoseResultModel
    public Point getPoint() {
        return this.g;
    }

    public List<Point> getPoints() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.g);
        for (IPoseResultModel iPoseResultModel : getPairs()) {
            arrayList.add(iPoseResultModel.getPoint());
        }
        return arrayList;
    }

    @Override // com.baidu.ai.edge.core.pose.IPoseResultModel
    public int getIndex() {
        return this.d;
    }

    public void setIndex(int i) {
        this.d = i;
    }

    public int getGroupIndex() {
        return this.e;
    }

    public void setGroupIndex(int i) {
        this.e = i;
    }

    public boolean hasGroups() {
        return this.f;
    }

    public void setHasGroups(boolean z) {
        this.f = z;
    }
}
