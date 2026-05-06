package cn.archko.pdf.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import cn.archko.pdf.R
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.EncodingDetect
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.StreamUtils
import com.archko.reader.image.MobiConverter
import com.archko.reader.image.MobiMetadata
import com.artifex.mupdf.fitz.Device
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.DocumentWriter
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFObject
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.Story
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author: archko 2018/12/21 :1:03 PM
 */
object PDFCreaterHelper {

    /**
     * A4: 210x297
     * A3：297×420
     * A2：420×594
     * A1：594×841
     * A0：841×1189 mm
     */
    const val OPTS = "compress-images;compress;incremental;linearize;pretty;compress-fonts;garbage"
    private const val PAPER_WIDTH = 1080f
    private const val PAPER_HEIGHT = 1800f
    private const val PAPER_PADDING = 40f
    private const val PAPER_FONT_SIZE = 17f

    /**
     * 用这个创建,带cjk字体的内容,会产生很大体积,而且字体不好看
     */
    fun createPdfFromText(sourcePath: String, destPath: String): Boolean {
        val text = EncodingDetect.readFile(sourcePath)
        val mediabox = Rect(0f, 0f, 420f, 594f) //A2
        val margin = 10f
        val writer = DocumentWriter(destPath, "PDF", OPTS)

        val snark = "<!DOCTYPE html>" +
                "<style>" +
                "#body { font-family: \"Noto Sans\", sans-serif; }" +
                "</style>" +
                "<body>" +
                text +
                "</body></html>"
        val story = Story(snark, "", 12f)

        var more: Boolean

        do {
            val filled = Rect()
            val where = Rect(
                mediabox.x0 + margin,
                mediabox.y0 + margin,
                mediabox.x1 - margin,
                mediabox.y1 - margin
            )
            val dev: Device = writer.beginPage(mediabox)
            more = story.place(where, filled)
            story.draw(dev, Matrix.Identity())
            writer.endPage()
        } while (more)

        writer.close()
        writer.destroy()
        story.destroy()

        return true
    }

    /**
     * 如果是原图的高宽,不经过缩放,pdf的页面高宽设置与图片大小一致,得到的pdf会很大.
     * 图片是否超过指定值,都应该做一次压缩
     */
    fun createPdfFromImages(pdfPath: String?, imagePaths: List<String>): Boolean {
        Log.d("TAG", String.format("imagePaths:%s", imagePaths))
        var mDocument: PDFDocument? = null
        try {
            mDocument = PDFDocument.openDocument(pdfPath) as PDFDocument
        } catch (e: Exception) {
            Log.d("TAG", "could not open:$pdfPath")
        }
        if (mDocument == null) {
            mDocument = PDFDocument()
        }

        val resultPaths = processLargeImage(imagePaths)

        //空白页面必须是-1,否则会崩溃,但插入-1的位置的页面会成为最后一个,所以追加的时候就全部用-1就行了.
        var index = -1
        for (path in resultPaths) {
            val page = addPage(path, mDocument, index++)

            mDocument.insertPage(-1, page)
        }
        mDocument.save(pdfPath, OPTS)
        Log.d("TAG", String.format("save,%s,%s", mDocument.toString(), mDocument.countPages()))
        val cacheDir = FileUtils.getExternalCacheDir(App.instance).path + File.separator + "create"
        val dir = File(cacheDir)
        if (dir.isDirectory) {
            dir.deleteRecursively()
        }
        return mDocument.countPages() > 0
    }

    /**
     * used for k2pdf
     */
    fun createPdfFromFormatedImages(
        context: Context,
        parent: ViewGroup,
        pdfPath: String?, imagePaths: List<String>
    ): Boolean {
        Log.d("TAG", String.format("imagePaths:%s", imagePaths))
        var mDocument: PDFDocument? = null
        try {
            mDocument = PDFDocument.openDocument(pdfPath) as PDFDocument
        } catch (e: Exception) {
            Log.d("TAG", "could not open:$pdfPath")
        }
        if (mDocument == null) {
            mDocument = PDFDocument()
        }

        var index = 0
        for (path in imagePaths) {
            val page = addPage(path, mDocument, index++)

            mDocument.insertPage(-1, page)
            index++
        }
        mDocument.save(pdfPath, OPTS)
        return mDocument.countPages() > 0
    }

    private fun addPage(
        path: String,
        mDocument: PDFDocument,
        index: Int
    ): PDFObject? {
        val image = Image(path)
        val resources = mDocument.newDictionary()
        val xobj = mDocument.newDictionary()
        val obj = mDocument.addImage(image)
        xobj.put("I", obj)
        resources.put("XObject", xobj)

        val w = image.width
        val h = image.height
        val mediabox = Rect(0f, 0f, w.toFloat(), h.toFloat())
        val contents = "q $w 0 0 $h 0 0 cm /I Do Q\n"
        val page = mDocument.addPage(mediabox, 0, resources, contents)
        Log.d("TAG", String.format("index:%s,page,%s,w:%s,h:%s", index, contents, w, h))
        return page
    }

    /**
     * 将大图片切割成小图片,以长图片切割,不处理宽图片
     */
    private fun processLargeImage(imagePaths: List<String>): List<String> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val maxHeight = 6000

        val result = arrayListOf<String>()
        for (path in imagePaths) {
            try {
                BitmapFactory.decodeFile(path, options)
                if (options.outHeight > maxHeight) {
                    //split image,maxheight=PAPER_HEIGHT
                    splitImages(result, path, options.outWidth, options.outHeight)
                } else {
                    if (IntentFile.isSupportedImageForCreater(path)) {
                        result.add(path)
                    } else {
                        convertImageToJpeg(result, path)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    /**
     * 默认不支持bmp,svg,heic,webp这些直接转换,所以先解析为jpg
     */
    private fun convertImageToJpeg(result: java.util.ArrayList<String>, path: String) {
        val bitmap: Bitmap = BitmapFactory.decodeFile(path)
        val file = File(
            FileUtils.getExternalCacheDir(App.instance).path
                    //FileUtils.getStorageDirPath() + "/amupdf"
                    + File.separator + "create" + File.separator + System.currentTimeMillis() + ".jpg"
        )
        BitmapUtils.saveBitmapToFile(bitmap, file, Bitmap.CompressFormat.JPEG, 100)
        BitmapPool.getInstance().release(bitmap)
        Log.d("TAG", "convertImageToJpeg path:${file.absolutePath}")

        result.add(file.absolutePath)
    }

    private fun splitImages(
        result: ArrayList<String>,
        path: String,
        width: Int,
        height: Int,
    ) {
        var top = 0f
        val right = 0 + width
        var bottom = PAPER_HEIGHT

        val decoder = BitmapRegionDecoder.newInstance(path, true)

        while (bottom < height) {
            val rect = android.graphics.Rect()
            rect.set(0, top.toInt(), right, bottom.toInt())
            splitImage(path, rect, result, decoder)

            top = bottom
            bottom += PAPER_HEIGHT
        }
        if (top < height) {
            val rect = android.graphics.Rect()
            rect.set(0, top.toInt(), right, height)
            splitImage(path, rect, result, decoder)
        }
        decoder.recycle()
    }

    private fun splitImage(
        path: String,
        rect: android.graphics.Rect,
        result: ArrayList<String>,
        decoder: BitmapRegionDecoder
    ) {
        val bitmap: Bitmap = decoder.decodeRegion(rect, null)
        val file = File(
            FileUtils.getExternalCacheDir(App.instance).path
                    //FileUtils.getStorageDirPath() + "/amupdf"
                    + File.separator + "create" + File.separator + System.currentTimeMillis() + ".jpg"
        )
        BitmapUtils.saveBitmapToFile(bitmap, file, Bitmap.CompressFormat.JPEG, 100)
        BitmapPool.getInstance().release(bitmap)
        Log.d("TAG", "new file:height:${rect.bottom - rect.top}, path:${file.absolutePath}")

        result.add(file.absolutePath)
    }

    var canExtract: Boolean = true

    fun extractToImages(
        context: Context, screenWidth: Int, dir: String, pdfPath: String,
        start: Int,
        end: Int
    ): Int {
        try {
            Log.d(
                "TAG",
                "extractToImages:$screenWidth, start:$start, end:$end dir:$dir, dst:$pdfPath"
            )
            val document = Document.openDocument(pdfPath)
            val count: Int = document.countPages()
            var startPage = start
            if (startPage < 0) {
                startPage = 0
            } else if (startPage >= count) {
                startPage = 0
            }
            var endPage = end
            if (end > count) {
                endPage = count
            } else if (endPage < 0) {
                endPage = count
            }
            for (i in startPage until endPage) {
                if (!canExtract) {
                    Log.d("TAG", "extractToImages.stop")
                    return i
                }
                val page = document.loadPage(i)
                if (null != page) {
                    val pageWidth = page.bounds.x1 - page.bounds.x0
                    val pageHeight = page.bounds.y1 - page.bounds.y0

                    var exportWidth = screenWidth
                    if (exportWidth == -1) {
                        exportWidth = pageWidth.toInt()
                    }
                    val scale = exportWidth / pageWidth
                    val width = exportWidth
                    val height = pageHeight * scale
                    val bitmap = BitmapPool.getInstance().acquire(width, height.toInt())
                    val ctm = Matrix(scale)
                    render(page, ctm, bitmap, 0, 0, 0)
                    page.destroy()
                    BitmapUtils.saveBitmapToFile(bitmap, File("$dir/${i + 1}.jpg"))
                    BitmapPool.getInstance().release(bitmap)
                }
                Log.d("TAG", "extractToImages:page:${i + 1}.jpg")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -2
        }
        return 0
    }

    fun render(
        page: Page?,
        ctm: Matrix?,
        bitmap: Bitmap?,
        xOrigin: Int,
        leftBound: Int,
        topBound: Int
    ) {
        if (page == null) {
            return
        }
        val dev = AndroidDrawDevice(bitmap, xOrigin + leftBound, topBound)
        page.run(dev, ctm, null)
        dev.close()
        dev.destroy()
    }

    fun extractToHtml(
        start: Int, end: Int, context: Context, path: String, pdfPath: String
    ): Boolean {
        try {
            val mupdfDocument = Document.openDocument(pdfPath)
            val count: Int = mupdfDocument.countPages()
            var startPage = start
            if (startPage < 0) {
                startPage = 0
            } else if (startPage >= count) {
                startPage = 0
            }
            var endPage = end
            if (end > count) {
                endPage = count
            } else if (endPage < 0) {
                endPage = count
            }
            val stringBuilder = StringBuilder()
            for (i in startPage until endPage) {
                val page = mupdfDocument.loadPage(i)
                if (null != page) {
                    val content =
                        String(page.textAsHtml2("preserve-whitespace,inhibit-spaces,preserve-images"))
                    stringBuilder.append(content)
                    Log.d(
                        "extractToHtml",
                        String.format(
                            "============%s-content:%s==========",
                            i,
                            content,
                        )
                    )
                    page.destroy()
                    StreamUtils.appendStringToFile(stringBuilder.toString(), path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    //================== 系统创建的api,通过打印的方式来创建 ==================


    /**
     * Page width for our PDF.
     */
    private const val PDF_PAGE_WIDTH = 841

    /**
     * Page height for our PDF.
     */
    private const val PDF_PAGE_HEIGHT = 1189

    fun createPdfUseSystemFromTxt(
        context: Context?,
        parent: ViewGroup?,
        fontSize: Float,
        padding: Int,
        lineSpace: Float,
        bgColor: Int,
        sourcePath: String?,
        destPath: String?
    ): Boolean {
        val content: String = EncodingDetect.readFile(sourcePath)
        try {
            return doCreatePdfUseSystemFromTxt(
                context, parent,
                fontSize,
                padding,
                lineSpace,
                bgColor,
                content, destPath
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * @param context
     * @param parent  需要有一个临时的布局
     * @param path    保存的文件名
     * @throws FileNotFoundException
     */
    @Throws(FileNotFoundException::class)
    fun doCreatePdfUseSystemFromTxt(
        context: Context?,
        parent: ViewGroup?,
        fontSize: Float,
        padding: Int,
        lineSpace: Float,
        bgColor: Int,
        content: String,
        path: String?
    ): Boolean {
        val pdfDocument = PdfDocument()
        val pageWidth = PDF_PAGE_WIDTH
        val pageHeight = PDF_PAGE_HEIGHT
        val contentView =
            LayoutInflater.from(context).inflate(R.layout.pdf_content, parent, false) as TextView
        applyStyle(contentView, fontSize, padding, bgColor, lineSpace)
        contentView.text = content

        val measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        contentView.measure(measureWidth, measuredHeight)
        contentView.layout(0, 0, pageWidth, pageHeight)

        val lineCountPerPage = contentView.lineCount
        var lineHeight = contentView.lineHeight
        if (lineSpace > 0) {
            lineHeight = (lineHeight * contentView.lineSpacingMultiplier).toInt()
        }
        val layout = contentView.layout
        var start = 0
        var end: Int
        var pageH = 0
        val paddingTopAndBottom: Int = 0// Utils.dipToPixel(context, 40f)
        //循环遍历打印每一行
        val sb = StringBuilder()
        var i = 0
        var page = 1
        while (i < lineCountPerPage) {
            end = layout.getLineEnd(i)
            val line = content.substring(start, end) //指定行的内容
            start = end
            sb.append(line)
            pageH += lineHeight
            if (pageH >= pageHeight - paddingTopAndBottom) {
                Log.d(
                    "TextView",
                    String.format(
                        "============page:%s, count:%s, lineHeight-pheight:%s-%s==========",
                        page,
                        lineCountPerPage,
                        lineHeight,
                        pageH
                    )
                )
                createTxtPage(
                    context,
                    parent,
                    pdfDocument,
                    fontSize,
                    padding,
                    lineSpace,
                    bgColor,
                    pageWidth,
                    pageHeight,
                    page,
                    sb.toString()
                )
                page++
                pageH = 0
                sb.setLength(0)
            }
            i++
        }
        if (sb.isNotEmpty()) {
            Logcat.d("TextView", "last page:$page")
            createTxtPage(
                context, parent, pdfDocument,
                fontSize,
                padding,
                lineSpace,
                bgColor,
                pageWidth, pageHeight, page, sb.toString()
            )
        }
        return savePdf(path, pdfDocument)
    }

    private fun applyStyle(
        contentView: TextView,
        fontSize: Float,
        padding: Int,
        bgColor: Int,
        lineSpace: Float
    ) {
        contentView.textSize = fontSize
        contentView.setPadding(padding, padding, padding, padding)
        contentView.setBackgroundColor(bgColor)
        contentView.setLineSpacing(0f, lineSpace)
        Logcat.d("size:$fontSize, padding:$padding, line:$lineSpace,color:$bgColor")
    }

    private fun createTxtPage(
        context: Context?,
        parent: ViewGroup?,
        pdfDocument: PdfDocument,
        fontSize: Float,
        padding: Int,
        lineSpace: Float,
        bgColor: Int,
        pageWidth: Int,
        pageHeight: Int,
        pageNo: Int,
        content: String?
    ) {
        val contentView =
            LayoutInflater.from(context).inflate(R.layout.pdf_content, parent, false) as TextView
        applyStyle(contentView, fontSize, padding, bgColor, lineSpace)
        contentView.text = content
        val pageInfo: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo)
                .create()
        val page: PdfDocument.Page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        contentView.measure(measureWidth, measuredHeight)
        contentView.layout(0, 0, pageWidth, pageHeight)
        contentView.draw(canvas)

        // finish the page
        pdfDocument.finishPage(page)
    }

    private fun createImagePage(
        context: Context?,
        parent: ViewGroup?,
        pdfDocument: PdfDocument,
        pageWidth: Int,
        pageNo: Int,
        path: String
    ) {
        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.pdf_image_content, parent, false) as ImageView
        val bitmap = BitmapFactory.decodeFile(path)
        contentView.setImageBitmap(bitmap)
        val pageHeight = bitmap.height * pageWidth / bitmap.width
        val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNo
        )
            .create()
        val page: PdfDocument.Page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        contentView.measure(measureWidth, measuredHeight)
        contentView.layout(0, 0, pageWidth, pageHeight)
        contentView.draw(canvas)
        Log.d(
            "TAG",
            String.format(
                "createImagePage:%s-%s",
                pageWidth,
                pageHeight,
            )
        )
        BitmapPool.getInstance().release(bitmap)

        // finish the page
        pdfDocument.finishPage(page)
    }

    @Throws(FileNotFoundException::class)
    private fun savePdf(path: String?, document: PdfDocument): Boolean {
        val outputStream = FileOutputStream(path)
        try {
            document.writeTo(outputStream)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            document.close()
        }
        return false
    }

    /**
     * 通过系统api创建pdf,比mupdf创建的要小不少,测试中,14m对10m.
     */
    fun createPdfUseSystemFromImages(
        context: Context,
        parent: ViewGroup,
        pdfPath: String?, imagePaths: List<String>
    ): Boolean {
        Log.d("TAG", String.format("imagePaths:%s", imagePaths))
        val pdfDocument = PdfDocument()

        val resultPaths = processLargeImage(imagePaths)

        var index = 0
        for (path in resultPaths) {
            createImagePage(context, parent, pdfDocument, PDF_PAGE_WIDTH, index, path)
            index++
        }
        return savePdf(pdfPath, pdfDocument)
    }

    /**
     * 会有内存溢出的可能,图片数量过多时.所以这个方法尽量用图片少的情况.目前无法通过追加形式添加
     * 已经处理好的图片,不再处理切割,因为切割需要再解析一次
     */
    fun createPdfUseSystemFromFormatedImages(
        context: Context,
        parent: ViewGroup,
        pdfPath: String?, imagePaths: List<String>
    ): Boolean {
        Log.d("TAG", String.format("imagePaths:%s", imagePaths))
        val pdfDocument = PdfDocument()

        var index = 0
        for (path in imagePaths) {
            createImagePage(context, parent, pdfDocument, PDF_PAGE_WIDTH.toInt(), index, path)
            index++
        }
        return savePdf(pdfPath, pdfDocument)
    }

    // =================== encrypt/decrypt ===================

    /**
     * 保存时添加密码保护
     */
    fun encryptPDF(
        inputFile: String?, outputFile: String?,
        userPassword: String?, ownerPassword: String?
    ): Boolean {
        try {
            val doc: Document? = Document.openDocument(inputFile)

            if (doc !is PDFDocument) {
                System.err.println("输入文件不是PDF格式")
                return false
            }

            if (doc.needsPassword()) {
                println("原文档需要密码验证")
                // 这里需要提供原文档的密码
                // pdfDoc.authenticatePassword("original_password");
            }

            val options = StringBuilder()
            // 设置加密算法 (AES 256位是最安全的)
            options.append("encrypt=aes-256")

            // 设置用户密码（打开文档时需要）
            if (userPassword != null && !userPassword.isEmpty()) {
                options.append(",user-password=").append(userPassword)
            }

            // 设置所有者密码（拥有完整权限）
            if (ownerPassword != null && !ownerPassword.isEmpty()) {
                options.append(",owner-password=").append(ownerPassword)
            }

            // 设置权限（-1表示所有权限）
            options.append(",permissions=-1")

            println("保存选项: $options")

            // 保存加密后的PDF
            doc.save(outputFile, options.toString())
            println("PDF加密成功，保存到: $outputFile")
            return true
        } catch (e: java.lang.Exception) {
            System.err.println("加密PDF时出错: " + e.message)
        }
        return false
    }

    /**
     * 移除PDF密码保护（解密）
     */
    fun decryptPDF(inputFile: String?, outputFile: String?, password: String?): Boolean {
        try {
            val doc: Document? = Document.openDocument(inputFile)
            if (doc !is PDFDocument) {
                System.err.println("输入文件不是PDF格式")
                return false
            }

            // 验证密码（如果需要）
            if (doc.needsPassword()) {
                if (!doc.authenticatePassword(password)) {
                    System.err.println("密码验证失败，无法解密")
                    return false
                }
                println("密码验证成功")
            }

            // 构建保存选项字符串 - 移除加密
            val options = "encrypt=no,decrypt=yes"

            // 保存解密后的PDF
            doc.save(outputFile, options)
            println("PDF解密成功，保存到: $outputFile")
            return true
        } catch (e: java.lang.Exception) {
            System.err.println("解密PDF时出错: " + e.message)
        }
        return false
    }

    /**
     * 修改PDF密码
     */
    fun changePDFPassword(
        inputFile: String?, outputFile: String?,
        oldPassword: String?, newUserPassword: String?, newOwnerPassword: String?
    ): Boolean {
        try {
            val doc: Document? = Document.openDocument(inputFile)
            if (doc !is PDFDocument) {
                System.err.println("输入文件不是PDF格式")
                return false
            }

            // 验证原密码（如果需要）
            if (doc.needsPassword()) {
                if (!doc.authenticatePassword(oldPassword)) {
                    System.err.println("原密码验证失败")
                    return false
                }
                println("原密码验证成功")
            }

            // 构建保存选项字符串
            val options = StringBuilder()

            // 设置新的加密算法
            options.append("encrypt=aes-256")

            // 设置新密码
            if (newUserPassword != null && !newUserPassword.isEmpty()) {
                options.append(",user-password=").append(newUserPassword)
            }
            if (newOwnerPassword != null && !newOwnerPassword.isEmpty()) {
                options.append(",owner-password=").append(newOwnerPassword)
            }

            // 设置权限
            options.append(",permissions=-1")

            // 保存修改密码后的PDF
            doc.save(outputFile, options.toString())
            println("PDF密码修改成功，保存到: $outputFile")
            return true
        } catch (e: java.lang.Exception) {
            System.err.println("修改PDF密码时出错: " + e.message)
        }
        return false
    }

    // =================== split PDF ===================

    /**
     * 解析拆分范围字符串
     * 例如: "1-10,11-20" -> [(1,10), (11,20)]
     */
    private fun parseRanges(rangeInput: String, maxPage: Int): List<Pair<Int, Int>>? {
        try {
            val ranges = mutableListOf<Pair<Int, Int>>()
            val parts = rangeInput.split(",").map { it.trim() }

            for (part in parts) {
                if (part.isEmpty()) continue

                val rangeParts = part.split("-").map { it.trim() }
                when (rangeParts.size) {
                    1 -> {
                        // 单页，如 "5"
                        val page = rangeParts[0].toInt()
                        if (page < 1 || page > maxPage) return null
                        ranges.add(Pair(page, page))
                    }

                    2 -> {
                        // 范围，如 "1-10"
                        val start = rangeParts[0].toInt()
                        val end = rangeParts[1].toInt()
                        if (start < 1 || end > maxPage || start > end) return null
                        ranges.add(Pair(start, end))
                    }

                    else -> return null
                }
            }

            return ranges
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 拆分PDF文件 - 使用临时文件和删除页面的方式
     * @param inputFile 输入PDF文件路径
     * @param outputBaseName 输出文件基础名称
     * @param rangeInput 拆分范围，如 "1-10,11-20"
     * @return 成功拆分的文件数量，失败返回-1
     */
    fun splitPDF(
        outDir: File,
        inputFile: String,
        outputBaseName: String,
        rangeInput: String
    ): Int {
        try {
            val checkDoc = Document.openDocument(inputFile)
            if (checkDoc !is PDFDocument) {
                System.err.println("输入文件不是PDF格式")
                return -1
            }
            val totalPages = checkDoc.countPages()
            checkDoc.destroy()

            println("源PDF总页数: $totalPages")

            val ranges = parseRanges(rangeInput, totalPages)
            if (ranges == null || ranges.isEmpty()) {
                System.err.println("无效的页面范围")
                return -1
            }

            var successCount = 0

            for ((index, range) in ranges.withIndex()) {
                val (startPage, endPage) = range
                println("处理范围: $startPage-$endPage")

                try {
                    // 创建临时文件
                    val tempFile = File.createTempFile("pdf_split_", ".pdf")

                    // 先保存一份完整的副本到临时文件
                    val sourceDoc = Document.openDocument(inputFile) as PDFDocument
                    sourceDoc.save(tempFile.absolutePath, "compress")
                    sourceDoc.destroy()

                    // 重新打开临时文件进行编辑
                    val editDoc = Document.openDocument(tempFile.absolutePath) as PDFDocument

                    // 构建要保留的页面列表（0-based index）
                    val pagesToKeep = (startPage - 1 until endPage).toSet()

                    // 从后往前删除不需要的页面
                    for (pageIndex in totalPages - 1 downTo 0) {
                        if (pageIndex !in pagesToKeep) {
                            editDoc.deletePage(pageIndex)
                        }
                    }

                    println("保留页面: $startPage-$endPage, 当前文档页数: ${editDoc.countPages()}")

                    // 保存最终文件
                    val outputFileName = if (ranges.size == 1) {
                        "$outputBaseName.pdf"
                    } else {
                        "${outputBaseName}_${index + 1}_p${startPage}-${endPage}.pdf"
                    }
                    val outputFile = File(outDir, outputFileName)

                    // 使用 clean 选项，这会清理未使用的对象
                    editDoc.save(outputFile.absolutePath, "clean,compress,garbage")
                    editDoc.destroy()

                    // 删除临时文件
                    tempFile.delete()

                    println("已保存: ${outputFile.absolutePath}")
                    successCount++

                } catch (e: Exception) {
                    System.err.println("处理范围 $startPage-$endPage 时出错: ${e.message}")
                    e.printStackTrace()
                }
            }

            return successCount
        } catch (e: Exception) {
            System.err.println("拆分PDF时出错: ${e.message}")
            e.printStackTrace()
            return -1
        }
    }

    /**
     * 合并多个PDF文件
     * @param outputFile 输出PDF文件的完整路径
     * @param pdfFiles 要合并的PDF文件路径列表（按顺序）
     * @return 成功合并的文件数量，失败返回-1
     */
    fun mergePDF(
        outputFile: String,
        pdfFiles: List<String>
    ): Int {
        try {
            if (pdfFiles.isEmpty()) {
                System.err.println("没有要合并的PDF文件")
                return -1
            }

            val firstPdfPath = pdfFiles[0]
            val mergedDoc = Document.openDocument(firstPdfPath) as PDFDocument

            var successCount = 0
            println("以文件 $firstPdfPath 为基础（${mergedDoc.countPages()}页），开始合并后续文件...")

            for ((index, pdfPath) in pdfFiles.withIndex().filter { it.index >= 1 }) {
                try {
                    val sourceDoc = Document.openDocument(pdfPath)
                    if (sourceDoc !is PDFDocument) {
                        System.err.println("文件 $pdfPath 不是有效的PDF格式")
                        sourceDoc.destroy()
                        continue
                    }

                    val pageCount = sourceDoc.countPages()
                    println("正在合并文件 ${index + 1}/${pdfFiles.size}: $pdfPath (${pageCount}页)")

                    // 将源文档的所有页面 graft 到合并文档末尾
                    for (pageIndex in 0 until pageCount) {
                        mergedDoc.graftPage(-1, sourceDoc, pageIndex)
                    }

                    sourceDoc.destroy()
                    successCount++
                } catch (e: Exception) {
                    System.err.println("处理文件 $pdfPath 时出错: ${e.message}")
                    e.printStackTrace()
                }
            }

            mergedDoc.save(outputFile, "incremental")
            mergedDoc.destroy()

            println("PDF合并成功: $outputFile (合并了 ${successCount + 1} 个文件)")
            return successCount
        } catch (e: Exception) {
            System.err.println("合并PDF时出错: ${e.message}")
            e.printStackTrace()
            return -1
        }
    }

    fun convertToEpub(
        outputFile: String,
        files: List<String>
    ): Int {
        try {
            if (files.isEmpty()) {
                System.err.println("没有要转换的文件")
                return -1
            }

            var count = 0
            for (path in files) {
                try {
                    val converter = MobiConverter()

                    // 测试1: 验证文件
                    println("=== 测试1: 验证 MOBI 文件 ===")
                    val isValid: Boolean = converter.isValidMobiFile(path)
                    println("文件有效性: " + (if (isValid) "✓ 有效" else "✗ 无效"))

                    if (!isValid) {
                        System.err.println("文件无效，退出测试")
                        System.exit(1)
                    }

                    // 测试2: 获取元数据
                    println("\n=== 测试2: 获取文件元数据 ===")
                    val metadata: MobiMetadata? = converter.getMetadata(path)
                    if (metadata != null) {
                        println("标题: " + metadata.title)
                        println("作者: " + metadata.author)
                        println("出版社: " + metadata.publisher)
                        println("语言: " + metadata.language)
                        println("有封面: " + metadata.hasCover)
                        //println("有目录: " + metadata.hasToc)
                    } else {
                        println("✗ 无法获取元数据")
                    }

                    // 测试3: 转换为 EPUB
                    println("\n=== 测试3: 转换为 EPUB ===")

                    val success: Boolean = converter.convertToEpub(path, outputFile)
                    println("输出路径: $outputFile, 结果:$success")
                    if (success) {
                        count++
                    }
                } catch (e: Exception) {
                    System.err.println("转换为epub时出错: ${e.message}")
                }
            }

            return count
        } catch (e: Exception) {
            System.err.println("转换为epub时出错: ${e.message}")
            e.printStackTrace()
            return -1
        }
    }
}