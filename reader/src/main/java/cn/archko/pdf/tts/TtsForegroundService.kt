package cn.archko.pdf.tts

import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.ReflowBean

/**
 * TTS前台服务，用于后台持续朗读
 */
class TtsForegroundService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tts_service_channel"
    }

    // Binder用于Activity绑定
    private val binder = TtsServiceBinder()

    private var textToSpeech: TextToSpeech? = null
    var isInitialized = false

    // TTS监听器
    private var progressListener: TtsProgressListener? = null

    // 数据队列
    private val beanList = mutableListOf<ReflowBean>()

    var currentBean: ReflowBean? = null
    private var currentIndex = 0

    // 前台服务相关
    private var isForegroundStarted = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Logcat.d(TAG, "onCreate")

        createNotificationChannel()
        initWakeLock()
        initializeTts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logcat.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Logcat.d(TAG, "onBind")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Logcat.d(TAG, "onDestroy")

        shutdown()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TTS朗读服务",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "文档朗读服务"
            channel.setSound(null, null)

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, TtsForegroundService::class.java)
        stopIntent.action = "STOP"
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("文档朗读")
            .setContentText(if (isSpeaking) "正在朗读..." else "已暂停")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_delete, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundIfNeeded() {
        if (!isForegroundStarted) {
            startForeground(NOTIFICATION_ID, createNotification())
            isForegroundStarted = true
            Logcat.d(TAG, "startForeground")
        }
    }

    private fun stopForegroundIfNeeded() {
        if (isForegroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            isForegroundStarted = false
            Logcat.d(TAG, "stopForeground")
        }
    }

    private fun initWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TtsForegroundService:TTS")
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            val config = resources.configuration
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.locales[0]
            } else {
                config.locale
            }
            textToSpeech?.setLanguage(locale)
            setupTtsListener()
            Logcat.d(TAG, "TTS initialized successfully")
        } else {
            Logcat.e(TAG, "TTS initialization failed")
        }
    }

    private fun setupTtsListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Logcat.d(TAG, String.format("onStart:%s, index:%s, size:%s", utteranceId, currentIndex, beanList.size))
                startForegroundIfNeeded()
                currentBean?.let { progressListener?.onStart(it) }
            }

            override fun onDone(utteranceId: String?) {
                Logcat.d(TAG, String.format("onDone:%s, index:%s, size:%s", utteranceId, currentIndex, beanList.size))
                currentBean?.let { progressListener?.onDone(it) }

                // 直接进行下一个
                playNext()
            }

            override fun onError(utteranceId: String?) {
                Logcat.d(TAG, "onError: $utteranceId, stopping and clearing queue")
                // 朗读失败，则停止并且清空
                stopAndClear()
            }
        })
    }

    private fun playNext() {
        if (currentIndex < beanList.size) {
            currentBean = beanList[currentIndex]
            val text = currentBean?.data
            if (!text.isNullOrBlank()) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, currentBean?.page)
                currentIndex++
            } else {
                currentIndex++
                playNext() // 跳过空内容
            }
        } else {
            // 所有项目播放完成
            stopForegroundIfNeeded()
            wakeLock?.takeIf { it.isHeld }?.release()
            progressListener?.onFinish()
        }
    }

    fun setProgressListener(listener: TtsProgressListener) {
        progressListener = listener
    }

    fun speak(bean: ReflowBean) {
        if (isInitialized) {
            reset()
            addToQueue(bean)
            playNext()
        }
    }

    fun addToQueue(bean: ReflowBean) {
        val data = bean.data
        val segmentCount = if (data.isNullOrBlank() || data.length <= 300) 1 else (data.length / 300.0).toInt() + 1
        if (data.isNullOrBlank() || data.length <= 300) {
            beanList.add(bean)
        } else {
            var index = 0
            for (i in data.indices step 300) {
                val sub = data.substring(i, (i + 300).coerceAtMost(data.length))
                beanList.add(ReflowBean(sub, bean.type, bean.page + "-$index"))
                index++
            }
        }
        if (!isSpeaking && isInitialized && beanList.size == segmentCount) {
            currentIndex = 0
            playNext()
        }
    }

    fun addToQueue(beans: List<ReflowBean>) {
        Logcat.d(TAG, "addToQueue beans size: ${beans.size}")
        val allSegments = mutableListOf<ReflowBean>()
        for (bean in beans) {
            val data = bean.data
            if (data != null && data.length > 300) {
                var index = 0
                for (i in data.indices step 300) {
                    val sub = data.substring(i, (i + 300).coerceAtMost(data.length))
                    allSegments.add(ReflowBean(sub, bean.type, bean.page + "-$index"))
                    index++
                }
            } else {
                allSegments.add(bean)
            }
        }
        beanList.addAll(allSegments)
        if (!isSpeaking && isInitialized) {
            currentIndex = 0
            playNext()
        }
    }

    fun stop() {
        textToSpeech?.stop()
        reset()
        stopForegroundIfNeeded()
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    fun stopAndClear() {
        textToSpeech?.stop()
        reset()
        progressListener?.onFinish()
        stopForegroundIfNeeded()
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    fun pause() {
        textToSpeech?.stop()
        stopForegroundIfNeeded()
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    val isSpeaking: Boolean
        get() = textToSpeech?.isSpeaking == true

    fun reset() {
        beanList.clear()
        currentIndex = 0
        currentBean = null
    }

    fun getQueueSize(): Int = beanList.size

    fun getQueue(): List<ReflowBean> = ArrayList(beanList)

    private fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        reset()
        stopForegroundIfNeeded()
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    // Binder类
    inner class TtsServiceBinder : Binder() {
        fun getService(): TtsForegroundService = this@TtsForegroundService
    }

    // TTS监听器接口
    interface TtsProgressListener {
        fun onStart(bean: ReflowBean)
        fun onDone(bean: ReflowBean)
        fun onFinish()
    }
}