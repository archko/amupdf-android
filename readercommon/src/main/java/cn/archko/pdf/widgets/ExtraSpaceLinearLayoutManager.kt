package cn.archko.pdf.widgets

import android.app.Activity
import android.content.Context
import androidx.recyclerview.awidget.LinearLayoutManager
import androidx.recyclerview.awidget.ARecyclerView
import cn.archko.pdf.utils.Utils

/**
 * Created by daniel.teixeira on 20/01/19
 */
class ExtraSpaceLinearLayoutManager(private val context: Context?, orientation: Int = VERTICAL) :
    LinearLayoutManager(context, orientation, false) {

    override fun getExtraLayoutSpace(state: ARecyclerView.State?): Int {
        return Utils.getScreenHeightPixelWithOrientation(context as Activity)
    }

    override fun calculateExtraLayoutSpace(state: ARecyclerView.State, extraLayoutSpace: IntArray) {
        super.calculateExtraLayoutSpace(state, extraLayoutSpace)
    }
}