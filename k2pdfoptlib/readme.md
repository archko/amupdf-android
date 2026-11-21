### compile:
```
cd k2pdfoptlib/src/main/cpp
git clone https://github.com/koreader/libk2pdfopt.git
```

```
package com.archko.reader.k2pdfopt

import java.awt.image.BufferedImage

class K2PdfOpt {
    private var handle: Long = 0L

    companion object {
        init {
            System.loadLibrary("k2pdfopt_jni")
        }
    }

    external fun create(width: Int, height: Int, dpi: Int)
    external fun setFontSize(size: Float)
    external fun getFontSize(): Float
    external fun setSourceDpi(dpi: Float)
    external fun setMargin(margin: Float)
    external fun setColumns(columns: Int)
    external fun setQuality(quality: Float)
    external fun setWrap(wrap: Boolean)
    external fun setTrim(trim: Boolean)
    external fun setWordSpacing(spacing: Float)

    external fun loadBuffer(argbPixels: IntArray, width: Int, height: Int)
    external fun renderPageBuffer(): IntArray?
    external fun renderTileBuffer(x: Int, y: Int, width: Int, height: Int): IntArray?

    external fun getPageWidth(): Int
    external fun getPageHeight(): Int
    external fun releasePage()
    external fun close()

    fun loadBufferedImage(image: BufferedImage) {
        val w = image.width
        val h = image.height
        val pixels = IntArray(w * h)
        image.getRGB(0, 0, w, h, pixels, 0, w)
        loadBuffer(pixels, w, h)
    }

    fun renderTileBufferedImage(x: Int, y: Int, w: Int, h: Int): BufferedImage? {
        val pixels = renderTileBuffer(x, y, w, h) ?: return null
        val actualW = minOf(w, (pageWidth - x).coerceAtLeast(0))
        val actualH = minOf(h, (pageHeight - y).coerceAtLeast(0))
        if (actualW <= 0 || actualH <= 0 || pixels.size < actualW * actualH) return null
        val out = BufferedImage(actualW, actualH, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, actualW, actualH, pixels, 0, actualW)
        return out
    }
}
```

### 桌面端打包与发布到本地 Maven 简要步骤

1. **编动态库放进 Desktop 模块 `src/main/resources/natives/<os>-<arch>/`**：
   * macOS 上编译时加上 `-I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin"` 与 `-shared -fPIC`，注意排除 `k2pdfopt` 源码中包含 `main()` 的入口文件，输出为 `libk2pdfopt_jni.dylib`（Linux 为 `libk2pdfopt_jni.so`，Windows 为 `k2pdfopt_jni.dll`）。
2. **桌面 JAR 内自动解压加载**：
   * `K2PdfOpt` 的 `init` 块按 `os.name` / `os.arch` 从 classpath 读取对应动态库写到临时文件并 `System.load(...)`。
3. **发布到本地 Maven**：
   * 在桌面专属子模块（例如 `k2pdfopt-desktop`）配置 `maven-publish`，设置独立的 `artifactId`（如 `k2pdfopt-desktop`）和版本号，运行 `./gradlew :k2pdfopt-desktop:publishToMavenLocal` 即可在 Compose Desktop 工程中通过 `mavenLocal()` 依赖使用。

你桌面端目前是在 macOS 上先跑通，还是需要同时顾及 Linux/Windows 架构？