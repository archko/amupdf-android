package cn.archko.pdf.core.common;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * 识别文件编码
 * 使用方法：String code = EncodingDetect.getJavaEncode(path);返回文件编码
 */
public class EncodingDetect {


    public static void writeFile(String path, String content, String charSet) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(path), charSet);
            out.write(content);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取文件
     */
    public static String readFile(String file, boolean... isTrims) {
        StringBuilder buffer = new StringBuilder();
        try {
            FileInputStream in = new FileInputStream(file);
            String encoding = UniversalDetector.detectCharset(in);
            in = new FileInputStream(file);
            BufferedReader fr;
            boolean isTrim = isTrims.length > 0 ? isTrims[0] : false;
            String line_separator = System.getProperty("line.separator");
            String myCode = encoding != null && !"".equals(encoding) ? encoding : "UTF-8";
            InputStreamReader read = new InputStreamReader(in, myCode);
            fr = new BufferedReader(read);
            String line = null;
            int flag = 1;
            while (true) {
                line = fr.readLine();
                if (line == null) {
                    break;
                }
                if (!isTrim) {
                    if (flag != 1) {
                        buffer.append(line_separator);
                    }
                }
                flag++;
                buffer.append(isTrim ? line.isEmpty() : line);
            }
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
}