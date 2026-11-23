### Android AAR

```bash
./gradlew :k2pdfoptlib:publishToMavenLocal
```

产物：`~/.m2/repository/com/archko/reader/k2pdfopt/0.3.0/k2pdfopt-0.3.0.aar`。

### 桌面 JAR + 原生库

同一份 `jni.cpp` 里已经带了桌面分支（`loadBuffer` / `renderPageBuffer` / `renderTileBuffer`），不需要单独的 Compose 模块。桌面发布用脚本完成：

```bash
k2pdfoptlib/scripts/build-desktop.sh
```

脚本会依次执行：

1. CMake 编译当前机器的 `libk2pdfopt_jni`（macOS 为 `.dylib`，Linux 为 `.so`）。
2. 编译 `src/desktop/java/com/archko/reader/k2pdfopt/K2PdfOpt.java`。
3. 把动态库打进 JAR 的 `natives/<os>-<arch>/` 目录。
4. 生成 POM 并安装到 Maven 本地仓库。

可选环境变量：

```bash
K2PDFOPT_DESKTOP_VERSION=0.3.1 \
MAVEN_LOCAL_REPO=/path/to/maven/repo \
k2pdfoptlib/scripts/build-desktop.sh
```

默认产物：

- 原生库：`k2pdfoptlib/build/desktop/native/<os>-<arch>/libk2pdfopt_jni.{dylib,so}`
- JAR：`k2pdfoptlib/build/desktop/k2pdfopt-desktop-0.3.0.jar`
- Maven：`~/.m2/repository/com/archko/reader/k2pdfopt-desktop/0.3.0/`

桌面工程依赖：

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("com.archko.reader:k2pdfopt-desktop:0.3.0")
}
```

`K2PdfOpt` 会优先从 `java.library.path` 加载 `k2pdfopt_jni`，找不到时自动从 JAR 的 `natives/` 解压并 `System.load()`，因此消费方不需要再手动放 `.so`/`.dylib`。

使用示例：

```java
K2PdfOpt opt = new K2PdfOpt();
opt.create(1080, 1920, 300);
opt.setSourceDpi(300);
opt.loadBufferedImage(image);
BufferedImage page = opt.renderPageBufferedImage();
BufferedImage tile = opt.renderTileBufferedImage(0, 0, 1080, 1920);
opt.releasePage();
opt.close();
```

前置条件：

- JDK 17（`JAVA_HOME` 可用；脚本会尝试自动定位）
- CMake 3.22+
- macOS：Xcode Command Line Tools
- Linux：`build-essential`、`zlib1g-dev`

当前脚本支持 macOS（arm64/x86_64）和 Linux（x86_64/arm64）。

### 原生源码

```bash
cd k2pdfoptlib/src/main/cpp
git clone https://github.com/koreader/libk2pdfopt.git
```

桌面和 Android 共用 `src/main/cpp/jni.cpp` 与 `src/main/cpp/libk2pdfopt`，只是 CMake 目标和输出扩展名不同。
