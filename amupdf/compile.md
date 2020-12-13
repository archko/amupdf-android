## how to compile mupdf
### step 1:
->git clone https://github.com/archko/amupdf-android.git

### step 2:
compile libmupdf:
cd amupdf-android/solib/mupdf-android-fitz
->git pull
->git submodule init
->git submodule update
->make -C libmupdf generate

### step 3:
run the project

### step 4:
get diff:
git diff ./ > archko.patch

### add custom function
before step2, you can modify /mupdf_c/libmupdf/platform/java/mupdf_native.c
in my options,i add Page_textAsHtml2,Page_textAsXHtml,Page_textAsText,Page_textAsTextOrHtml
and goto to step 5, generate the diff.
