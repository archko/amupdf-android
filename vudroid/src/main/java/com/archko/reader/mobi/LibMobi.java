package com.archko.reader.mobi;

import android.util.Log;

import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.IntentFile;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.utils.FileUtils;
import cn.archko.pdf.core.utils.StreamUtils;
import io.documentnode.epub4j.domain.Author;
import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Metadata;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.epub.EpubWriter;

/**
 * added for support doc, docx files.
 */
public class LibMobi {

    static {
        System.loadLibrary("mobi");
    }

    public static native int convertToEpub(String input, String output);

    public static File convertMobiToEpub(File file) {
        String input = file.getAbsolutePath();
        int hashCode = (input + file.length() + file.lastModified()).hashCode();
        File outputFile = FileUtils.getDiskCacheDir(App.Companion.getInstance(), hashCode + ".epub");
        Logcat.d(String.format("convertMobiToEpub: file=%s, convertFilePath=%s",
                input, outputFile.getAbsoluteFile()));

        int res = -1;
        if (!outputFile.exists()) {
            try {
                res = LibMobi.convertToEpub(input, outputFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e("", e.getMessage());
            }
        } else {
            res = 0;
        }

        if (res != 0) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        return outputFile;
    }

    /**
     * 转为epub,存储相同的目录
     *
     * @param path
     * @return
     */
    public static int convertMobiToEpub(String input) {
        String name = FileUtils.getNameWithoutExt(input);
        String folderPath = input.substring(0, input.lastIndexOf("/"));
        File outputFile = new File(folderPath + File.separator + name + ".epub");
        Logcat.d(String.format("convertMobiToEpubBatch: file=%s, convertFilePath=%s",
                input, outputFile.getAbsoluteFile()));

        int res = -1;
        if (!outputFile.exists()) {
            try {
                res = LibMobi.convertToEpub(input, outputFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e("", e.getMessage());
            }
        } else {
            res = 0;
        }
        if (res != 0) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        return res;
    }

    public static int convertToEpubBatch(List<String> paths) {
        int count = 0;
        for (String path : paths) {
            if (IntentFile.INSTANCE.isMobi(path)) {
                int res = convertMobiToEpub(path);
                if (res == 0) {
                    count++;
                }
            } else if (IntentFile.INSTANCE.isDocx(path)) {
                boolean res = convertDocxToHtml(path);
                if (res) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * docx先转为html,图片可以保存下来,然后通过下面的库转为epub.
     *
     * @param file
     */
    public static File convertDocxToHtml(File file) {
        String input = file.getAbsolutePath();
        int hashCode = (input + file.length() + file.lastModified()).hashCode();
        File outputFile = FileUtils.getDiskCacheDir(App.Companion.getInstance(), hashCode + ".epub");
        if (outputFile.exists()) {
            return outputFile;
        }
        String folderPath = input.substring(0, input.lastIndexOf("/"));
        Map<String, String> images = new HashMap<>();
        DocumentConverter converter = new DocumentConverter().
                imageConverter(image -> {
                    String imageName = image.hashCode() + "." + image.getContentType().replace("image/", "");
                    Log.d("", "ImageConverter:" + imageName);

                    File imageFile = new File(folderPath, imageName);
                    StreamUtils.saveStreamToFile(image.getInputStream(), imageFile);
                    images.put(imageName, imageFile.getAbsolutePath());

                    Map<String, String> map = new HashMap<>();
                    map.put("src", imageName);
                    return map;
                });

        Result<String> result = null;
        try {
            result = converter.convertToHtml(file);
            String html = result.getValue(); // The generated HTML

            String outputHtml = folderPath + File.separator + hashCode + ".html";
            Log.d("", String.format("convertDocxToHtml: file=%s, folder=%s, convertFilePath=%s", input, folderPath, outputHtml));
            boolean res = StreamUtils.saveStringToFile("<html><head></head><body>" + html + "</body></html>", outputHtml);
            if (res) {
                res = convertToEpub("archko", file.getName(), outputFile.getAbsolutePath(), outputHtml, images);
                for (Map.Entry<String, String> image : images.entrySet()) {
                    new File(image.getValue()).delete();
                }
                new File(outputHtml).delete();
                if (res) {
                    return outputFile;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean convertDocxToHtml(String input) {
        File file = new File(input);
        String name = FileUtils.getNameWithoutExt(input);
        String folderPath = input.substring(0, input.lastIndexOf("/"));
        File outputFile = new File(folderPath + File.separator + name + ".epub");
        if (outputFile.exists()) {
            return true;
        }
        Map<String, String> images = new HashMap<>();
        DocumentConverter converter = new DocumentConverter().
                imageConverter(image -> {
                    String imageName = image.hashCode() + "." + image.getContentType().replace("image/", "");
                    Log.d("", "ImageConverter:" + imageName);

                    File imageFile = new File(folderPath, imageName);
                    StreamUtils.saveStreamToFile(image.getInputStream(), imageFile);
                    images.put(imageName, imageFile.getAbsolutePath());

                    Map<String, String> map = new HashMap<>();
                    map.put("src", imageName);
                    return map;
                });

        Result<String> result = null;
        try {
            result = converter.convertToHtml(file);
            String html = result.getValue(); // The generated HTML

            int hashCode = (input + file.length() + file.lastModified()).hashCode();
            String outputHtml = folderPath + File.separator + hashCode + ".html";
            Log.d("", String.format("convertDocxToHtml: file=%s, folder=%s, convertFilePath=%s", input, folderPath, outputHtml));
            boolean res = StreamUtils.saveStringToFile("<html><head></head><body>" + html + "</body></html>", outputHtml);
            if (res) {
                res = convertToEpub("archko", file.getName(), outputFile.getAbsolutePath(), outputHtml, images);
                for (Map.Entry<String, String> image : images.entrySet()) {
                    new File(image.getValue()).delete();
                }
                new File(outputHtml).delete();
                if (res) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static InputStream getResource(String path) throws FileNotFoundException {
        return new FileInputStream(path);
    }

    private static Resource getResource(String path, String href) throws IOException {
        return new Resource(getResource(path), href);
    }

    private static boolean convertToEpub(String author, String name, String output, String outputHtml, Map<String, String> images) {
        try {
            // Create new Book
            Book book = new Book();
            Metadata metadata = book.getMetadata();

            metadata.addTitle(name);
            metadata.addAuthor(new Author(author, "Author"));

            // Set cover image
            //book.setCoverImage(getResource("/book1/test_cover.png", "cover.png"));

            // Add Chapter 1
            book.addSection("Introduction", getResource(outputHtml, "chapter1.html"));

            // Add css file
            //Resource css = new Resource(context.getAssets().open("epub.css"), "epub.css");
            //book.getResources().add(css);

            // Add Chapter 2
            //TOCReference chapter2 = book.addSection("Second Chapter", getResource("/book1/chapter2.html", "chapter2.html"));

            // Add image used by Chapter 2
            //book.getResources().add(getResource("/book1/flowers_320x240.jpg", "flowers.jpg"));
            for (Map.Entry<String, String> image : images.entrySet()) {
                book.getResources().add(getResource(image.getValue(), image.getKey()));
            }

            // Add Chapter2, Section 1
            //book.addSection(chapter2, "Chapter 2, section 1", getResource("/book1/chapter2_1.html", "chapter2_1.html"));

            EpubWriter epubWriter = new EpubWriter();

            // Write the Book as Epub
            epubWriter.write(book, new FileOutputStream(output));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
