package cn.archko.pdf.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity(private val layoutId: Int) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layoutId)
        setupView()
        setupDataObserver()
    }

    abstract fun setupView()
    open fun setupDataObserver() {}
}