package cn.archko.pdf.base

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar

abstract class BaseDialogFragment() :
    DialogFragment() {

    var onFinishLoading: (() -> Unit)? = null
    open val isFullScreen: Boolean = true

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFullScreen) {
            setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setupView()
            onFinishLoading?.invoke()
        } catch (e: Exception) {
            Log.e(this::javaClass.name, e.printStackTrace().toString())
        }
    }

    open fun setupView() {}
    open fun setupDataObserver() {}

    protected fun showSnackBar(resMessage: Int) {
        runOnUiThread(Runnable {
            if (view != null) {
                val snackBar = Snackbar.make(
                    view ?: return@Runnable, (activity
                        ?: return@Runnable).getString(resMessage), Snackbar.LENGTH_LONG
                )
                snackBar.show()
            }
        })
    }

    fun showSnackBar(message: String?) {
        runOnUiThread(Runnable {
            if (view != null) {
                val snackBar = Snackbar.make(
                    view ?: return@Runnable, message
                        ?: return@Runnable, Snackbar.LENGTH_SHORT
                )
                snackBar.show()
            }
        })
    }

    private fun runOnUiThread(runnable: Runnable?) {
        if (activity == null || !isAdded) {
            return
        }
        activity?.runOnUiThread(runnable)
    }

}