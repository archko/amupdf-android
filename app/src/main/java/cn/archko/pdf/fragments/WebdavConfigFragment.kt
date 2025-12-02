package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.mupdf.databinding.FragmentWebdavConfigBinding
import cn.archko.pdf.R
import cn.archko.pdf.core.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * webdav备份列表
 * @author: archko 2024/8/7 :15:58
 */
open class WebdavConfigFragment : DialogFragment() {

    private lateinit var binding: FragmentWebdavConfigBinding
    private lateinit var backupViewModel: BackupViewModel
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)
        backupViewModel = BackupViewModel()
        progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
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

        /*binding.btnTest.setOnClickListener {
            testAuth()
        }*/
        binding.btnOk.setOnClickListener {
            save()
        }

        if (backupViewModel.checkAndLoadUser()) {
            backupViewModel.webdavUser?.let {
                binding.name.setText(it.name)
                binding.host.setText(it.host)
                binding.path.setText(it.path)
            }
        }
    }

    private fun save() {
        val name = binding.name.editableText.toString()
        val pass = binding.password.editableText.toString()
        val host = binding.host.editableText.toString()
        val path = binding.path.editableText.toString()

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pass)
            || TextUtils.isEmpty(host) || TextUtils.isEmpty(path)
        ) {
            Toast.makeText(requireActivity(), "Please config webdav first", Toast.LENGTH_SHORT)
                .show()
            return
        }

        progressDialog.show()
        lifecycleScope.launch {
            backupViewModel.saveWebdavUser(name, pass, host, path)
                .flowOn(Dispatchers.IO)
                .collectLatest {
                    progressDialog.dismiss()
                    if (it) {
                        Toast.makeText(App.instance, "Success", Toast.LENGTH_SHORT).show()
                        dismiss()
                    } else {
                        Toast.makeText(App.instance, "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }
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
