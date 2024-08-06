package vn.chungha.flowbus

/**
 * @author: archko 2024/8/6 :08:52
 */
import android.app.Application

object FlowBusInitApplication {
    lateinit var application: Application

    fun initializer(application: Application) {
        FlowBusInitApplication.application = application
    }
}