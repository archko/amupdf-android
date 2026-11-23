cmake_minimum_required(VERSION 3.22.1)

project(k2pdfopt_jni C CXX)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(LIBK2PDFOPT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp/libk2pdfopt)

set(HAVE_Z_LIB 1)
configure_file(
        ${LIBK2PDFOPT_DIR}/config.h.in
        ${CMAKE_CURRENT_BINARY_DIR}/config.h
)

find_package(JNI REQUIRED)
find_package(ZLIB REQUIRED)

include_directories(
        ${CMAKE_CURRENT_BINARY_DIR}
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib
        ${LIBK2PDFOPT_DIR}/willuslib
        ${LIBK2PDFOPT_DIR}/lib
        ${LIBK2PDFOPT_DIR}/include_mod
        ${JNI_INCLUDE_DIRS}
)

add_library(k2pdfopt_jni SHARED
        ${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp/jni.cpp

        ${LIBK2PDFOPT_DIR}/lib/koptreflow.c
        ${LIBK2PDFOPT_DIR}/lib/setting.c

        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/bmpregion.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/devprofile.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2bmp.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2file.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2mark.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2master.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2mem.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2ocr.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2proc.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2settings.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2sys.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/k2version.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/pagelist.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/pageregions.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/textrows.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/textwords.c
        ${LIBK2PDFOPT_DIR}/k2pdfoptlib/wrapbmp.c

        ${LIBK2PDFOPT_DIR}/willuslib/ansi.c
        ${LIBK2PDFOPT_DIR}/willuslib/array.c
        ${LIBK2PDFOPT_DIR}/willuslib/bmp.c
        ${LIBK2PDFOPT_DIR}/willuslib/filelist.c
        ${LIBK2PDFOPT_DIR}/willuslib/gslpolyfit.c
        ${LIBK2PDFOPT_DIR}/willuslib/linux.c
        ${LIBK2PDFOPT_DIR}/willuslib/math.c
        ${LIBK2PDFOPT_DIR}/willuslib/mem.c
        ${LIBK2PDFOPT_DIR}/willuslib/ocr.c
        ${LIBK2PDFOPT_DIR}/willuslib/ocrgocr.c
        ${LIBK2PDFOPT_DIR}/willuslib/ocrwords.c
        ${LIBK2PDFOPT_DIR}/willuslib/strbuf.c
        ${LIBK2PDFOPT_DIR}/willuslib/string.c
        ${LIBK2PDFOPT_DIR}/willuslib/token.c
        ${LIBK2PDFOPT_DIR}/willuslib/wfile.c
        ${LIBK2PDFOPT_DIR}/willuslib/willusversion.c
        ${LIBK2PDFOPT_DIR}/willuslib/wsys.c
)

target_compile_definitions(k2pdfopt_jni PRIVATE
        USE_CMAKE
        BUILDING_LIBK2PDFOPT
        NO_FILELIST
)

target_link_libraries(k2pdfopt_jni PRIVATE ZLIB::ZLIB)

if(NOT WIN32)
    target_link_libraries(k2pdfopt_jni PRIVATE m)
endif()

if(APPLE)
    set_target_properties(k2pdfopt_jni PROPERTIES LINK_FLAGS "-undefined dynamic_lookup")
endif()

set_target_properties(k2pdfopt_jni PROPERTIES OUTPUT_NAME "k2pdfopt_jni")
