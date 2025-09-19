package cn.archko.pdf.core.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MuPdfHtmlMerger {
    // 根据文档特征确定的典型行间距
    private static final float TYPICAL_LINE_SPACING = 14.0f;
    // 段落间距阈值（行间距的倍数）
    private static final float PARAGRAPH_SPACING_RATIO = 1.0f;
    // 缩进差异阈值（超过此值视为不同段落）
    private static final float INDENT_THRESHOLD = 2.0f;
    // 字号差异阈值
    private static final float FONT_SIZE_THRESHOLD = 0.5f;

    public String mergeParagraphs(String html) {
        Document doc = Jsoup.parse(html);
        Elements pages = doc.select("div[id^=page]");

        for (Element page : pages) {
            // 完全移除div的style属性
            page.removeAttr("style");
            mergePage(page);
        }

        return doc.html();
    }

    /**
     * 移除div元素的width和height样式，使其自适应
     */
    private void removeSizeAttributes(Element page) {
        String style = page.attr("style");
        if (style == null || style.isEmpty()) {
            return;
        }

        // 拆分样式属性
        String[] styleParts = style.split(";");
        StringBuilder newStyle = new StringBuilder();

        for (String part : styleParts) {
            part = part.trim();
            // 保留除width和height之外的其他样式
            if (!part.startsWith("width") && !part.startsWith("height")) {
                if (newStyle.length() > 0) {
                    newStyle.append("; ");
                }
                newStyle.append(part);
            }
        }

        // 更新样式属性，如果没有样式则移除该属性
        if (newStyle.length() > 0) {
            page.attr("style", newStyle.toString());
        } else {
            page.removeAttr("style");
        }
    }

    /* 扩展：统一判断“整段保留”规则，拆成两步检测 ---------- */
    private boolean shouldKeepIntact(Element p) {
        /* 规则1：图片段落 */
        if (p.children().size() == 1 && "img".equals(p.child(0).tagName()))
            return true;

        /* 规则2：代码段落（单<span>，含关键字或以代码结束符结尾） */
        if (p.children().size() == 1 && "span".equals(p.child(0).tagName())) {
            String txt = p.child(0).text().trim();
            boolean isCode = CODE_KEYWORDS.stream().anyMatch(txt::contains)
                    || txt.endsWith(";") || txt.endsWith("{")
                    || txt.endsWith("}") || txt.endsWith(")");
            if (isCode) {
                // 识别为代码后，立即把字体改为 monospace
                Element span = p.child(0);
                String style = span.attr("style");
                style = appendFontFamily(style, "monospace");
                span.attr("style", style);
                return true;
            }
        }

        /* 规则3：字体为 Monaco/monospace 仅作为辅助条件，不再单独判定为代码 */

        /* 规则4：文本以 >>> 或 > 开头（HTML 转义后）视为终端输入代码 */
        for (Element span : p.select("span")) {
            String txt = span.text().trim();
            if (txt.startsWith(">>>") || txt.startsWith(">")) {
                // 识别为终端代码后，立即把字体改为 monospace
                String style = span.attr("style");
                style = appendFontFamily(style, "monospace");
                span.attr("style", style);
                return true;
            }
        }

        /* 规则5：已移除——仅保留更精确的代码特征判断 */

        /* 规则6：以后想加任何规则，直接在这里写 if (...) return true; */

        return false;   // 默认参与合并
    }

    /* 新增：代码关键字白名单（涵盖 Python/Kotlin/C/Dart/Java/Swift/Objective-C 等常用保留字与类型） */
    private static final java.util.Set<String> CODE_KEYWORDS = java.util.Set.of(
            // ===== Java / Kotlin / C / Objective-C 共用核心关键字 =====
            "public", "private", "protected", "class", "interface", "void",
            "if", "for", "while", "switch", "case", "default", "try", "catch",
            "return", "import", "package", "static", "final", "this",
            "extends", "implements", "throws", "new", "throw", "enum", "@Override",
            "const", "struct", "typedef", "union", "sizeof", "extern", "register",
            "volatile", "goto", "inline", "#include", "#define", "#ifdef", "#ifndef", "#endif",
            "@interface", "@implementation", "@end", "@property", "@synthesize", "@protocol", "@selector",
            // ===== Kotlin 独有 =====
            "fun", "val", "var", "when", "object", "companion", "data", "sealed", "suspend", "reified", "crossinline", "noinline",
            // ===== Python 独有 =====
            "def", "elif", "else", "except", "with", "as", "lambda", "yield", "from", "global", "nonlocal", "pass",  "del", "raise", "is", "in", "not", "and", "or", "True", "False", "None",
            // ===== Dart 独有 =====
            "Future", "async", "await", "mixin", "extension", "late", "required", "factory", "external", "operator", "covariant", "part", "show", "hide", "deferred", "assert", "library", "export",
            // ===== Swift 独有 =====
            "let", "guard", "defer", "fallthrough", "associatedtype", "typealias", "where", "some", "any", "actor", "nonisolated", "convenience", "lazy", "weak", "unowned", "optional", "willSet", "didSet", "get", "set", "inout", "escaping", "autoclosure",
            // ===== 常见基础类型 =====
            "String", "int", "long", "double", "float", "boolean", "bool", "char", "byte", "short", "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short", "StringBuilder", "StringBuffer", "List", "Map", "Set", "Array", "Dict", "Optional", "Any", "Void", "Self", "self", "super", "id", "instancetype", "NSObject", "NSString", "NSNumber", "NSArray", "NSDictionary", "NSMutableArray", "NSMutableDictionary",
            // ===== 常见修饰符 / 注解 =====
            "@FunctionalInterface", "@Deprecated", "@SafeVarargs", "@SuppressWarnings", "@Nullable", "@NonNull", "@JvmStatic", "@JvmField", "@JvmOverloads", "@Composable", "@Preview"
    );

    private void mergePage(Element page) {
        /* 0. 先移除空行：仅含空白或高度≤单行高的<p> ---------------- */
        for (Element p : page.select("p")) {
            String txt = p.text().trim();
            float h = new MuPdfParagraph(p).lineHeight;
            if (txt.isEmpty() || h <= TYPICAL_LINE_SPACING) {
                p.remove();
            }
        }

        /* 1. 再把所有“整段保留”的段落克隆+标记 -------------------- */
        List<Node> toRemove = new ArrayList<>();
        List<Node> keepNodes = new ArrayList<>();
        for (Element p : page.select("p")) {
            if (shouldKeepIntact(p)) {
                toRemove.add(p);
                keepNodes.add(p.clone());
            }
        }

        /* 1. 对剩余段落做原来的合并逻辑 ---------------------------- */
        Elements paragraphs = page.select("p");
        if (paragraphs.size() <= 1) return;

        List<MuPdfParagraph> paraList = new ArrayList<>();
        for (Element p : paragraphs) {
            if (toRemove.contains(p)) continue;   // 跳过保留段
            paraList.add(new MuPdfParagraph(p));
        }

        List<MuPdfParagraph> currentGroup = new ArrayList<>();
        if (!paraList.isEmpty()) currentGroup.add(paraList.get(0));

        for (int i = 1; i < paraList.size(); i++) {
            MuPdfParagraph prev = paraList.get(i - 1);
            MuPdfParagraph curr = paraList.get(i);
            if (shouldMerge(prev, curr)) {
                currentGroup.add(curr);
            } else {
                Element mergedP = mergeGroup(currentGroup);
                currentGroup.get(0).element.replaceWith(mergedP);
                for (int j = 1; j < currentGroup.size(); j++) {
                    currentGroup.get(j).element.remove();
                }
                currentGroup.clear();
                currentGroup.add(curr);
            }
        }
        if (!currentGroup.isEmpty()) {
            Element mergedP = mergeGroup(currentGroup);
            currentGroup.get(0).element.replaceWith(mergedP);
            for (int j = 1; j < currentGroup.size(); j++) {
                currentGroup.get(j).element.remove();
            }
        }

        /* 2. 把保留段插回（顺序不变） ------------------------------ */
        for (Node old : toRemove) old.remove();
        for (Node keep : keepNodes) page.appendChild(keep);
    }

    /**
     * 判断两个相邻段落是否应该合并
     */
    private boolean shouldMerge(MuPdfParagraph prev, MuPdfParagraph curr) {
        // 1. 缩进差异判断：缩进不同视为不同段落
        if (Math.abs(prev.left - curr.left) > INDENT_THRESHOLD) {
            return false;
        }

        // 2. 间距判断：超过行间距的1.2倍视为新段落
        float spacing = curr.top - prev.bottom;
        if (spacing > TYPICAL_LINE_SPACING * PARAGRAPH_SPACING_RATIO) {
            return false;
        }

        // 3. 样式判断：字体或字号不同则不合并
        if (!prev.fontFamily.equals(curr.fontFamily) ||
                Math.abs(prev.fontSize - curr.fontSize) > FONT_SIZE_THRESHOLD) {
            return false;
        }

        // 4. 特殊规则：以标点符号结尾的段落不与后续段落合并
        String prevText = prev.element.text().trim();
        if (!prevText.isEmpty()) {
            char lastChar = prevText.charAt(prevText.length() - 1);
            if (lastChar == '。' || lastChar == '：' || lastChar == '；' ||
                    lastChar == '！' || lastChar == '？' || lastChar == '、') {
                return false;
            }
        }

        // 5. 特殊规则：以序号开头的段落（如"一、"）作为新段落
        String currText = curr.element.text().trim();
        if (currText.matches("^[一二三四五六七八九十]+、.*") ||
                currText.matches("^[ABCDE]+、.*") ||
                currText.matches("^［.*］.*")) {
            return false;
        }

        return true;
    }

    /**
     * 合并一组段落为一个段落
     */
    private Element mergeGroup(List<MuPdfParagraph> group) {
        // 使用第一个段落的样式作为基础
        MuPdfParagraph first = group.get(0);
        Element mergedPara = new Element("p");

        // 仅保留左侧缩进，移除顶部和底部边距
        mergedPara.attr("style", String.format(
                "margin-left:%.1fpt;",
                first.left));

        // 合并所有span内容，并强制行高倍率为1
        Element mergedSpan = new Element("span");
        mergedSpan.attr("style", String.format(
                "font-family:%s;font-size:%.1fpt;line-height:1.0;",
                first.fontFamily, first.fontSize));

        StringBuilder textBuilder = new StringBuilder();
        for (MuPdfParagraph para : group) {
            textBuilder.append(para.element.text());
        }

        mergedSpan.text(textBuilder.toString());
        mergedPara.appendChild(mergedSpan);

        return mergedPara;
    }

    /**
     * MuPDF段落信息封装类
     * 提取缩进、位置、样式等关键信息
     */
    private static class MuPdfParagraph {
        float top;          // 顶部位置
        float left;         // 左侧缩进
        float bottom;       // 底部位置
        float lineHeight;   // 行高
        String fontFamily;  // 字体
        float fontSize;     // 字号
        Element element;    // 原始元素

        MuPdfParagraph(Element p) {
            this.element = p;
            String style = p.attr("style");

            // 提取位置和行高
            this.top = extractValue(style, "top");
            this.left = extractValue(style, "left");
            this.lineHeight = extractValue(style, "line-height");
            this.bottom = this.top + this.lineHeight;

            // 提取字体样式（从span中）
            Element span = p.selectFirst("span");
            if (span != null) {
                String spanStyle = span.attr("style");
                this.fontFamily = extractFontFamily(spanStyle);
                this.fontSize = extractValue(spanStyle, "font-size");
            } else {
                this.fontFamily = "unknown";
                this.fontSize = 12.0f;
            }
        }

        // 提取样式中的数值（如12.0pt中的12.0）
        private float extractValue(String style, String property) {
            if (style == null || !style.contains(property)) return 0.0f;

            String[] parts = style.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith(property)) {
                    String valueStr = part.split(":")[1].trim()
                            .replace("pt", "").replace("px", "");
                    try {
                        return Float.parseFloat(valueStr);
                    } catch (NumberFormatException e) {
                        return 0.0f;
                    }
                }
            }
            return 0.0f;
        }

        // 提取字体家族
        private String extractFontFamily(String style) {
            if (style == null || !style.contains("font-family")) return "unknown";

            String[] parts = style.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("font-family")) {
                    return part.split(":")[1].trim()
                            .replace("'", "").replace("\"", "");
                }
            }
            return "unknown";
        }
    }

    /**
     * 工具：向现有 style 字符串追加 font-family，若已存在则替换
     */
    private static String appendFontFamily(String style, String font) {
        if (style == null) style = "";
        // 去掉已有 font-family 声明
        style = style.replaceAll("\\s*font-family\\s*:[^;]+", "").trim();
        // 去掉首尾多余分号与空格
        style = style.replaceAll("^;+|;+$", "").trim();
        // 追加新的 font-family
        return style.isEmpty()
                ? "font-family:" + font
                : style + ";font-family:" + font;
    }

}