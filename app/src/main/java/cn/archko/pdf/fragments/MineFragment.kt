package cn.archko.pdf.fragments

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentMineBinding
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.activities.HomeActivity
import cn.archko.pdf.activities.PdfOptionsActivity

/**
 * @author archko
 */
class MineFragment : Fragment() {

    private lateinit var binding: FragmentMineBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMineBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.btnCreatePdf.setOnClickListener {
            HomeActivity.createPdf(requireActivity())
        }

        binding.btnExportPdf.setOnClickListener {
            HomeActivity.extractImage(requireActivity())
        }

        binding.btnEncryptDecrypt.setOnClickListener {
            HomeActivity.encryptOrDecrypt(requireActivity())
        }

        binding.btnSplitPdf.setOnClickListener {
            HomeActivity.splitPdf(requireActivity())
        }

        binding.btnMergePdf.setOnClickListener {
            HomeActivity.mergePdf(requireActivity())
        }

        binding.btnConvertEpub.setOnClickListener {
            HomeActivity.convertToEpub(requireActivity())
        }

        binding.btnWebdav.setOnClickListener {
            WebdavConfigFragment.showCreateDialog(requireActivity())
        }

        binding.btnSettings.setOnClickListener {
            PdfOptionsActivity.start(requireActivity())
        }

        binding.btnAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        // 获取并设置版本号
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val versionName = packageInfo?.versionName ?: ""
        binding.version.text = String.format(getString(R.string.version), versionName)

        return view
    }

    companion object {
        const val TAG = "MineFragment"
    }
}
