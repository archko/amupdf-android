package cn.archko.pdf.core.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MuPdfHtmlMerger {
    // 根据文档特征确定的典型行间距
    private static final float TYPICAL_LINE_SPACING = 18.0f;
    // 段落间距阈值（行间距的倍数）- 降低阈值以合并更多行
    private static final float PARAGRAPH_SPACING_RATIO = 1.1f;
    // 缩进差异阈值（超过此值视为不同段落）- 增加阈值以允许更多缩进变化
    private static final float INDENT_THRESHOLD = 3.0f;
    // 字号差异阈值
    private static final float FONT_SIZE_THRESHOLD = 0.8f;

    // 空行检测阈值 - 内容过少的段落视为空行
    private static final int EMPTY_LINE_THRESHOLD = 2;

    // 用于检测列表项的模式
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile(
            "^(\\d+\\.|\\*|\\-|•|·)\\s+.*");

    // 用于检测标题的模式
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^#{1,6}\\s+.*");

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

        /* 规则5：检测列表项 */
        String text = p.text().trim();
        if (LIST_ITEM_PATTERN.matcher(text).matches()) {
            return true;
        }

        /* 规则6：检测标题 */
        if (HEADING_PATTERN.matcher(text).matches()) {
            return true;
        }

        return false;   // 默认参与合并
    }

    private static final Set<String> CODE_KEYWORDS = Set.of(
            // Java / Kotlin / C / Objective-C 共用核心关键字
            "public", "private", "protected", "class", "interface", "void",
            "if", "for", "while", "switch", "case", "default", "try", "catch",
            "return", "import", "package", "static", "final", "this",
            "extends", "implements", "throws", "new", "throw", "enum", "@Override",
            "const", "struct", "typedef", "union", "sizeof", "extern",
            "#include", "#define", "#ifdef", "#ifndef", "#endif",
            // Kotlin 独有
            "fun", "val", "var", "when", "object", "companion", "data",
            // Python 独有
            "def", "elif", "else", "except", "with", "as", "lambda", "yield",
            // 常见基础类型
            "String", "int", "long", "double", "float", "boolean", "bool",
            "List", "Map", "Set", "Array", "Dict"
    );

    private void mergePage(Element page) {
        /* 0. 先把所有“整段保留”的段落克隆+标记 */
        List<Node> toRemove = new ArrayList<>();
        List<Node> keepNodes = new ArrayList<>();
        for (Element p : page.select("p")) {
            // 额外检测空行并标记移除
            String text = p.text().trim();
            if (text.isEmpty() || text.length() <= EMPTY_LINE_THRESHOLD) {
                toRemove.add(p);
                continue;
            }

            if (shouldKeepIntact(p)) {
                toRemove.add(p);
                keepNodes.add(p.clone());
            }
        }

        /* 1. 对剩余段落做合并逻辑 */
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
                removeElements(currentGroup, 1);
                currentGroup.clear();
                currentGroup.add(curr);
            }
        }
        if (!currentGroup.isEmpty()) {
            Element mergedP = mergeGroup(currentGroup);
            currentGroup.get(0).element.replaceWith(mergedP);
            removeElements(currentGroup, 1);
        }

        /* 2. 把保留段插回（顺序不变） */
        for (Node old : toRemove) old.remove();
        for (Node keep : keepNodes) page.appendChild(keep);

        // 最后清理可能的连续空段落
        cleanEmptyParagraphs(page);
    }

    /**
     * 批量移除元素
     */
    private void removeElements(List<MuPdfParagraph> group, int startIndex) {
        for (int j = startIndex; j < group.size(); j++) {
            group.get(j).element.remove();
        }
    }

    /**
     * 清理连续的空段落
     */
    private void cleanEmptyParagraphs(Element page) {
        Elements paragraphs = page.select("p");
        Element prev = null;

        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.isEmpty() || text.length() <= EMPTY_LINE_THRESHOLD) {
                if (prev != null &&
                        (prev.text().trim().isEmpty() ||
                                prev.text().trim().length() <= EMPTY_LINE_THRESHOLD)) {
                    p.remove(); // 移除连续空段落
                    continue;
                }
            }
            prev = p;
        }
    }

    /**
     * 判断两个相邻段落是否应该合并 - 优化版本
     */
    private boolean shouldMerge(MuPdfParagraph prev, MuPdfParagraph curr) {
        // 1. 缩进差异判断：缩进不同视为不同段落
        if (Math.abs(prev.left - curr.left) > INDENT_THRESHOLD) {
            return false;
        }

        // 2. 间距判断：超过行间距的1.1倍视为新段落（降低了阈值）
        float spacing = curr.top - prev.bottom;
        // 对于非常接近的行，即使间距略大也尝试合并
        if (spacing > TYPICAL_LINE_SPACING * PARAGRAPH_SPACING_RATIO &&
                spacing > TYPICAL_LINE_SPACING + 2) {
            return false;
        }

        // 3. 样式判断：字体或字号不同则不合并，但增加了一定容错
        if (!isFontFamilySimilar(prev.fontFamily, curr.fontFamily) ||
                Math.abs(prev.fontSize - curr.fontSize) > FONT_SIZE_THRESHOLD) {
            return false;
        }

        // 4. 特殊规则：以标点符号结尾的段落处理优化
        String prevText = prev.element.text().trim();
        if (!prevText.isEmpty()) {
            char lastChar = prevText.charAt(prevText.length() - 1);
            // 对于引号内的标点，允许合并
            boolean inQuotes = prevText.contains("\"") || prevText.contains("'") ||
                    prevText.contains("“") || prevText.contains("”");

            if (!inQuotes && (lastChar == '。' || lastChar == '！' || lastChar == '？')) {
                return false;
            }
        }

        // 5. 特殊规则：以序号开头的段落处理优化
        String currText = curr.element.text().trim();
        if (currText.matches("^[一二三四五六七八九十]+、.*") ||
                currText.matches("^[ABCDE]+、.*") ||
                currText.matches("^［.*］.*")) {
            // 如果前一段落很短，可能是标题，不合并
            if (prev.element.text().trim().length() < 10) {
                return false;
            }
        }

        // 6. 特殊规则：如果当前段落以小写字母开头，很可能是前一段的继续
        if (!currText.isEmpty() && Character.isLowerCase(currText.charAt(0))) {
            return true;
        }

        return true;
    }

    /**
     * 判断字体家族是否相似（中文字体或英文字体）
     */
    private boolean isFontFamilySimilar(String fontFamily1, String fontFamily2) {
        // 如果字体家族完全相同，直接返回true
        if (fontFamily1.equalsIgnoreCase(fontFamily2)) {
            return true;
        }

        // 判断是否都是中文字体
        boolean isChinese1 = fontFamily1.contains("宋体") || fontFamily1.contains("微软雅黑") ||
                fontFamily1.contains("黑体") || fontFamily1.contains("楷体");
        boolean isChinese2 = fontFamily2.contains("宋体") || fontFamily2.contains("微软雅黑") ||
                fontFamily2.contains("黑体") || fontFamily2.contains("楷体");

        if (isChinese1 && isChinese2) {
            return true;
        }

        // 判断是否都是英文字体
        boolean isEnglish1 = fontFamily1.contains("Arial") || fontFamily1.contains("Times") ||
                fontFamily1.contains("Helvetica") || fontFamily1.contains("sans-serif");
        boolean isEnglish2 = fontFamily2.contains("Arial") || fontFamily2.contains("Times") ||
                fontFamily2.contains("Helvetica") || fontFamily2.contains("sans-serif");

        return isEnglish1 && isEnglish2;
    }

    /**
     * 合并一组段落为一个段落 - 优化版本
     */
    private Element mergeGroup(List<MuPdfParagraph> group) {
        // 使用第一个段落的样式作为基础
        MuPdfParagraph first = group.get(0);
        Element mergedPara = new Element("p");

        // 保留左侧缩进和行高，移除顶部位置限制以增强自适应
        mergedPara.attr("style", String.format(
                "margin-left:%.1fpt; margin-top:0.5em; margin-bottom:0.5em;",
                first.left));

        // 合并所有span内容，保留各自的样式
        for (MuPdfParagraph para : group) {
            Elements spans = para.element.select("span");
            if (spans.isEmpty()) {
                // 如果没有span，直接添加文本
                String text = para.element.text();
                if (!text.isEmpty()) {
                    mergedPara.append(text);
                }
            } else {
                // 保留每个span的样式
                for (Element span : spans) {
                    Element newSpan = new Element("span");
                    newSpan.attr("style", span.attr("style"));
                    newSpan.text(span.text());
                    mergedPara.appendChild(newSpan);
                }
            }
            // 添加适当的空格分隔不同段落的内容
            if (para != group.get(group.size() - 1)) {
                mergedPara.append(" ");
            }
        }

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

            // 如果行高无法提取，使用典型行高
            if (this.lineHeight <= 0) {
                this.lineHeight = TYPICAL_LINE_SPACING;
            }

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

            // 处理可能的字体大小提取失败情况
            if (this.fontSize <= 0) {
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
