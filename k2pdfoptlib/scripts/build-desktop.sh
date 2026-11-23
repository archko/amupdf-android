#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DESKTOP_DIR="$MODULE_DIR/src/desktop"
BUILD_DIR="$MODULE_DIR/build/desktop"
VERSION="${K2PDFOPT_DESKTOP_VERSION:-0.3.0}"
GROUP_ID="com.archko.reader"
ARTIFACT_ID="k2pdfopt-desktop"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -x /usr/libexec/java_home ]]; then
        JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
    fi
    if [[ -z "${JAVA_HOME:-}" ]]; then
        JAVAC_BIN="$(command -v javac || true)"
        if [[ -n "$JAVAC_BIN" ]]; then
            JAVA_HOME="$(cd "$(dirname "$JAVAC_BIN")/.." && pwd)"
        fi
    fi
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/javac" ]]; then
    echo "JAVA_HOME is not set and javac was not found" >&2
    exit 1
fi
export JAVA_HOME

OS_NAME="$(uname -s)"
ARCH_NAME="$(uname -m)"
case "$OS_NAME" in
    Darwin)
        PLATFORM_OS="macos"
        LIB_FILE="libk2pdfopt_jni.dylib"
        ;;
    Linux)
        PLATFORM_OS="linux"
        LIB_FILE="libk2pdfopt_jni.so"
        ;;
    *)
        echo "unsupported OS: $OS_NAME (macOS/Linux only)" >&2
        exit 1
        ;;
esac
case "$ARCH_NAME" in
    arm64|aarch64)
        PLATFORM_ARCH="arm64"
        ;;
    x86_64|amd64)
        PLATFORM_ARCH="x86_64"
        ;;
    *)
        PLATFORM_ARCH="$ARCH_NAME"
        ;;
esac

CMAKE_ARGS=(-S "$DESKTOP_DIR/cpp" -B "$BUILD_DIR/cmake" -DCMAKE_BUILD_TYPE=Release)
if [[ "$PLATFORM_OS" == "macos" ]]; then
    CMAKE_ARGS+=("-DCMAKE_OSX_ARCHITECTURES=$ARCH_NAME")
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/native/$PLATFORM_OS-$PLATFORM_ARCH"

cmake "${CMAKE_ARGS[@]}"
cmake --build "$BUILD_DIR/cmake" --config Release --parallel

BUILT_LIB="$(find "$BUILD_DIR/cmake" -maxdepth 3 -name "$LIB_FILE" -type f | head -n 1 || true)"
if [[ -z "$BUILT_LIB" ]]; then
    echo "native library not found: $LIB_FILE" >&2
    exit 1
fi

find "$DESKTOP_DIR/java" -name '*.java' -print0 \
    | xargs -0 "$JAVA_HOME/bin/javac" --release 17 -encoding UTF-8 -d "$BUILD_DIR/classes"

RESOURCE_DIR="$BUILD_DIR/classes/natives/$PLATFORM_OS-$PLATFORM_ARCH"
mkdir -p "$RESOURCE_DIR"
cp "$BUILT_LIB" "$RESOURCE_DIR/$LIB_FILE"

JAR_FILE="$BUILD_DIR/$ARTIFACT_ID-$VERSION.jar"
"$JAVA_HOME/bin/jar" --create --file "$JAR_FILE" -C "$BUILD_DIR/classes" .

MAVEN_REPO="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"
GROUP_PATH="$(printf '%s' "$GROUP_ID" | tr '.' '/')"
DEST_DIR="$MAVEN_REPO/$GROUP_PATH/$ARTIFACT_ID/$VERSION"
mkdir -p "$DEST_DIR"
cp "$JAR_FILE" "$DEST_DIR/"
cat > "$DEST_DIR/$ARTIFACT_ID-$VERSION.pom" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$GROUP_ID</groupId>
  <artifactId>$ARTIFACT_ID</artifactId>
  <version>$VERSION</version>
  <packaging>jar</packaging>
  <name>k2pdfopt-desktop</name>
  <description>Desktop JNI build of libk2pdfopt</description>
</project>
EOF

echo "native: $RESOURCE_DIR/$LIB_FILE"
echo "jar:    $JAR_FILE"
echo "maven:  $DEST_DIR"
