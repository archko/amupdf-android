package cn.archko.pdf.fragments

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.activities.HomeActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentMineBinding

/**
 * @author archko
 */
class MineFragment : Fragment() {

    private lateinit var _binding: FragmentMineBinding
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
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
            // TODO: implement split PDF
        }

        binding.btnMergePdf.setOnClickListener {
            // TODO: implement merge PDF
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
        binding.version.text = "Version: $versionName"

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        const val TAG = "SettingsFragment"
    }
}