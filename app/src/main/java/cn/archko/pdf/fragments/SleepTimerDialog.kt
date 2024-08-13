package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.DialogSleepTimerBinding
import cn.archko.pdf.widgets.SeekArc
import cn.archko.pdf.widgets.SeekArc.OnSeekArcChangeListener

class SleepTimerDialog(private var timeListener: TimeListener) : DialogFragment(R.layout.dialog_sleep_timer) {

    interface TimeListener {

        fun onTime(minute: Int)
    }

    private lateinit var binding: DialogSleepTimerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppDialogTheme
        setStyle(STYLE_NORMAL, themeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogSleepTimerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.seekArc.setOnSeekArcChangeListener(object : OnSeekArcChangeListener {
            override fun onStopTrackingTouch(seekArc: SeekArc) {

            }

            override fun onStartTrackingTouch(seekArc: SeekArc) {
            }

            override fun onProgressChanged(
                seekArc: SeekArc, progress: Int,
                fromUser: Boolean
            ) {
                binding.timerDisplay.text = progress.toString()
            }
        })

        binding.timerDisplay.text = binding.seekArc.progress.toString()
        binding.ok.setOnClickListener {
            timeListener.onTime(binding.seekArc.progress)
            dismiss()
        }
    }

    fun showDialog(activity: FragmentActivity?) {
        val ft = activity?.supportFragmentManager?.beginTransaction()
        val prev = activity?.supportFragmentManager?.findFragmentByTag("timer_dialog")
        if (prev != null) {
            ft?.remove(prev)
        }
        ft?.addToBackStack(null)

        show(ft!!, "timer_dialog")
    }
}
