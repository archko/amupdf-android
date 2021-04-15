package cn.archko.sunflower.ui.utils

import android.util.Log

/**
 * @author: archko 2021/4/5 :7:47 上午
 */
class VLog {

    companion object {
        val TAG = "VLog"

        fun v(str: String, tag: String = TAG) {
            Log.v(tag, str)
        }

        fun d(str: String, tag: String = TAG) {
            Log.d(tag, str)
        }

        fun i(str: String, tag: String = TAG) {
            Log.i(tag, str)
        }

        fun w(str: String, tag: String = TAG) {
            Log.w(tag, str)
        }

        fun e(str: String, tag: String = TAG) {
            Log.e(tag, str)
        }
    }
}