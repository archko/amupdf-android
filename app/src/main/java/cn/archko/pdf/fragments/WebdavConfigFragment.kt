package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentWebdavConfigBinding
import cn.archko.pdf.utils.SardineHelper
import com.tencent.mmkv.MMKV

/**
 * webdav备份列表
 * @author: archko 2024/8/7 :15:58
 */
open class WebdavConfigFragment : DialogFragment() {

    private lateinit var binding: FragmentWebdavConfigBinding
    private lateinit var backupViewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(DialogFragment.STYLE_NO_TITLE, themeId)
        backupViewModel = BackupViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWebdavConfigBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { dismiss() }

        binding.btnTest.setOnClickListener {
            testAuth()
        }
        binding.btnOk.setOnClickListener {
            save()
        }

        val mmkv = MMKV.mmkvWithID(SardineHelper.KEY_CONFIG)
        val name = mmkv.decodeString(SardineHelper.KEY_NAME)
        binding.password.setText(name)
    }

    private fun testAuth() {
        backupViewModel.testAuth(
            binding.name.editableText.toString(),
            binding.password.editableText.toString()
        )
    }

    private fun save() {
        val name = binding.name.editableText.toString()
        val pass = binding.password.editableText.toString()

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pass)) {
            Toast.makeText(requireActivity(), "Please config webdav first", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val mmkv = MMKV.mmkvWithID(SardineHelper.KEY_CONFIG)
        mmkv.encode(SardineHelper.KEY_NAME, name)
        mmkv.encode(SardineHelper.KEY_PASS, pass)

        dismiss()
    }

    companion object {

        const val TAG = "WebdavConfigFragment"

        fun showCreateDialog(activity: FragmentActivity?) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("config_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = WebdavConfigFragment()
            pdfFragment.show(ft!!, "config_dialog")
        }
    }
}
