package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import cn.archko.pdf.R
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.viewmodel.BookmarkViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 书签编辑弹窗
 * @author: archko 2026/3/11
 */
class BookmarkEditDialog : DialogFragment() {

    private var pageIndex: Int = 0
    private var bookmarkViewModel: BookmarkViewModel? = null
    private var existingBookmark: ABookmark? = null
    private var path: String? = null

    private lateinit var titleEditText: TextInputEditText
    private lateinit var contentEditText: TextInputEditText
    private var selectedColor: Long? = null

    private val colorViews = mutableListOf<Pair<View, Long?>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = R.style.AppTheme
        setStyle(STYLE_NORMAL, themeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.apply {
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0f
            setCanceledOnTouchOutside(true)
            setCancelable(true)
            // 设置宽度为屏幕宽度的 85%
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val view = inflater.inflate(R.layout.dialog_bookmark_edit, container, false)

        titleEditText = view.findViewById(R.id.bookmark_title)
        contentEditText = view.findViewById(R.id.bookmark_content)
        val pageTextView = view.findViewById<android.widget.TextView>(R.id.tv_page)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)

        pageTextView.text = getString(R.string.page_label, pageIndex + 1)

        setupColorViews(view)

        if (existingBookmark != null) {
            titleEditText.setText(existingBookmark?.title)
            contentEditText.setText(existingBookmark?.note)
            selectedColor = existingBookmark?.color
            updateColorSelection()
        }

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            saveBookmark()
        }

        return view
    }

    private fun setupColorViews(view: View) {
        val colorDefault = view.findViewById<View>(R.id.colorDefault)
        val colorRed = view.findViewById<View>(R.id.colorRed)
        val colorYellow = view.findViewById<View>(R.id.colorYellow)
        val colorGreen = view.findViewById<View>(R.id.colorGreen)
        val colorBlue = view.findViewById<View>(R.id.colorBlue)
        val colorPurple = view.findViewById<View>(R.id.colorPurple)

        colorViews.apply {
            add(colorDefault to null)
            add(colorRed to 0xFFFF5252L)
            add(colorYellow to 0xFFFFD740L)
            add(colorGreen to 0xFF69F0AEL)
            add(colorBlue to 0xFF40C4FFL)
            add(colorPurple to 0xFFE040FBL)
        }

        colorViews.forEach { (colorView, colorValue) ->
            colorView.setOnClickListener {
                selectedColor = colorValue
                updateColorSelection()
            }
        }
    }

    private fun updateColorSelection() {
        colorViews.forEach { (colorView, colorValue) ->
            val isSelected = selectedColor == colorValue
            val scale = if (isSelected) 1.1f else 1.0f
            colorView.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
            colorView.background.alpha = if (isSelected) 255 else 180
        }
    }

    private fun saveBookmark() {
        val title = titleEditText.text?.toString()?.trim()
        val note = contentEditText.text?.toString()?.trim()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    if (existingBookmark != null) {
                        // 更新现有书签
                        existingBookmark?.title = title
                        existingBookmark?.note = note
                        existingBookmark?.color = selectedColor
                        bookmarkViewModel?.updateBookmark(existingBookmark!!)
                    } else {
                        // 添加新书签
                        bookmarkViewModel?.addBookmark(
                            path = path ?: "",
                            pageIndex = pageIndex,
                            title = title,
                            note = note,
                            color = selectedColor
                        )
                    }
                }
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Failed to save bookmark",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        fun showDialog(
            pageIndex: Int,
            path: String,
            bookmarkViewModel: BookmarkViewModel,
            existingBookmark: ABookmark? = null
        ): BookmarkEditDialog {
            return BookmarkEditDialog().apply {
                this.pageIndex = pageIndex
                this.path = path
                this.bookmarkViewModel = bookmarkViewModel
                this.existingBookmark = existingBookmark
            }
        }
    }
}
