package org.vudroid.djvudroid.codec;

import com.archko.reader.image.DjvuLoader;

import org.vudroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

public class DjvuOutline {

    public List<OutlineLink> getOutline(final DjvuLoader djvuLoader) {
        final ArrayList<OutlineLink> list = new ArrayList<>(20);
        if (djvuLoader != null) {
            List<com.archko.reader.image.DjvuOutline> outlines = djvuLoader.getOutline();
            if (outlines != null) {
                for (com.archko.reader.image.DjvuOutline outline : outlines) {
                    flattenDjvuOutline(outline, list, 0);
                }
            }
        }
        list.trimToSize();
        return list;
    }

    private void flattenDjvuOutline(com.archko.reader.image.DjvuOutline outline, List<OutlineLink> items, int level) {
        if (outline.getPage() >= 0) {
            String link = "#" + (outline.getPage() + 1);
            OutlineLink item = new OutlineLink(outline.getTitle(), link, level);
            items.add(item);
        }

        if (outline.getChildren() != null) {
            for (com.archko.reader.image.DjvuOutline child : outline.getChildren()) {
                flattenDjvuOutline(child, items, level + 1);
            }
        }
    }
}
