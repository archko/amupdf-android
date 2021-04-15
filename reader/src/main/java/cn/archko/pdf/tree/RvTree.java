package cn.archko.pdf.tree;

/**
 * @author kanade on 2016/11/15.
 * @see:https://github.com/pye52/TreeView/blob/master/treeadapter/src/main/java/com/kanade/treeadapter
 */
public interface RvTree {
    long getNid();

    long getPid();

    String getTitle();

    int getImageResId();
}
