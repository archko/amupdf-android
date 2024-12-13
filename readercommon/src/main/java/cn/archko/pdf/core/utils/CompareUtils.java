package cn.archko.pdf.core.utils;

import org.jetbrains.annotations.NotNull;

import java.text.Collator;
import java.util.Comparator;

import cn.archko.pdf.core.entity.FileBean;

/**
 * @author: archko 2024/12/13 :10:05
 */
public final class CompareUtils {

    public static final NaturalFileComparator NAME_ASC = new NaturalFileComparator(true);
    public static final NaturalFileComparator NAME_DESC = new NaturalFileComparator(false);
    public static final FileModifyComparator MODIFY_ASC = new FileModifyComparator(true);
    public static final FileModifyComparator MODIFY_DESC = new FileModifyComparator(false);
    public static final FileSizeComparator SIZE_ASC = new FileSizeComparator(true);
    public static final FileSizeComparator SIZE_DESC = new FileSizeComparator(false);

    public static int compareNatural(String firstString, String secondString) {
        int firstIndex = 0;
        int secondIndex = 0;

        Collator collator = Collator.getInstance();

        while (true) {
            if (firstIndex == firstString.length() && secondIndex == secondString.length()) {
                return 0;
            }
            if (firstIndex == firstString.length()) {
                return -1;
            }
            if (secondIndex == secondString.length()) {
                return 1;
            }

            if (Character.isDigit(firstString.charAt(firstIndex))
                    && Character.isDigit(secondString.charAt(secondIndex))) {
                int firstZeroCount = 0;
                while (firstString.charAt(firstIndex) == '0') {
                    firstZeroCount++;
                    firstIndex++;
                    if (firstIndex == firstString.length()) {
                        break;
                    }
                }
                int secondZeroCount = 0;
                while (secondString.charAt(secondIndex) == '0') {
                    secondZeroCount++;
                    secondIndex++;
                    if (secondIndex == secondString.length()) {
                        break;
                    }
                }
                if ((firstIndex == firstString.length() || !Character.isDigit(firstString.charAt(firstIndex)))
                        && (secondIndex == secondString.length() || !Character
                        .isDigit(secondString.charAt(secondIndex)))) {
                    continue;
                }
                if ((firstIndex == firstString.length() || !Character.isDigit(firstString.charAt(firstIndex)))
                        && !(secondIndex == secondString.length() || !Character.isDigit(secondString
                        .charAt(secondIndex)))) {
                    return -1;
                }
                if ((secondIndex == secondString.length() || !Character.isDigit(secondString.charAt(secondIndex)))) {
                    return 1;
                }

                int diff = 0;
                do {
                    if (diff == 0) {
                        diff = firstString.charAt(firstIndex) - secondString.charAt(secondIndex);
                    }
                    firstIndex++;
                    secondIndex++;
                    if (firstIndex == firstString.length() && secondIndex == secondString.length()) {
                        return diff != 0 ? diff : firstZeroCount - secondZeroCount;
                    }
                    if (firstIndex == firstString.length()) {
                        if (diff == 0) {
                            return -1;
                        }
                        return Character.isDigit(secondString.charAt(secondIndex)) ? -1 : diff;
                    }
                    if (secondIndex == secondString.length()) {
                        if (diff == 0) {
                            return 1;
                        }
                        return Character.isDigit(firstString.charAt(firstIndex)) ? 1 : diff;
                    }
                    if (!Character.isDigit(firstString.charAt(firstIndex))
                            && !Character.isDigit(secondString.charAt(secondIndex))) {
                        if (diff != 0) {
                            return diff;
                        }
                        break;
                    }
                    if (!Character.isDigit(firstString.charAt(firstIndex))) {
                        return -1;
                    }
                    if (!Character.isDigit(secondString.charAt(secondIndex))) {
                        return 1;
                    }
                } while (true);
            } else {
                int aw = firstIndex;
                int bw = secondIndex;
                do {
                    firstIndex++;
                } while (firstIndex < firstString.length() && !Character.isDigit(firstString.charAt(firstIndex)));
                do {
                    secondIndex++;
                } while (secondIndex < secondString.length() && !Character.isDigit(secondString.charAt(secondIndex)));

                String as = firstString.substring(aw, firstIndex);
                String bs = secondString.substring(bw, secondIndex);
                int subwordResult = collator.compare(as, bs);
                if (subwordResult != 0) {
                    return subwordResult;
                }
            }
        }
    }

    @NotNull
    public static Comparator<FileBean> getSortor(int sort) {
        return switch (sort) {
            case 0 -> NAME_ASC;
            case 1 -> NAME_DESC;
            case 2 -> MODIFY_ASC;
            case 3 -> MODIFY_DESC;
            case 4 -> SIZE_ASC;
            case 5 -> SIZE_DESC;
            default -> null;
        };
    }

    public static final class NaturalFileComparator implements Comparator<FileBean> {

        private boolean asc = true;

        public NaturalFileComparator(boolean asc) {
            this.asc = asc;
        }

        public int compare(FileBean o1, FileBean o2) {
            if (asc) {
                if (o1 == null || o1.getFile() == null) {
                    return -1;
                }
                if (o2 == null || o2.getFile() == null) {
                    return 1;
                }
                return compareNatural(o1.getFile().getAbsolutePath(), o2.getFile().getAbsolutePath());
            } else {
                if (o1 == null || o1.getFile() == null) {
                    return 1;
                }
                if (o2 == null || o2.getFile() == null) {
                    return -1;
                }
                return compareNatural(o2.getFile().getAbsolutePath(), o1.getFile().getAbsolutePath());
            }
        }
    }

    public static final class FileModifyComparator implements Comparator<FileBean> {
        private boolean asc = true;

        public FileModifyComparator(boolean asc) {
            this.asc = asc;
        }

        public int compare(FileBean o1, FileBean o2) {
            if (asc) {
                if (o1 == null || o1.getFile() == null) {
                    return -1;
                }
                if (o2 == null || o2.getFile() == null) {
                    return 1;
                }
                return (int) (o1.getFile().lastModified() - o2.getFile().lastModified());
            } else {
                if (o1 == null || o1.getFile() == null) {
                    return 1;
                }
                if (o2 == null || o2.getFile() == null) {
                    return -1;
                }
                return (int) (o2.getFile().lastModified() - o1.getFile().lastModified());
            }
        }
    }

    public static final class FileSizeComparator implements Comparator<FileBean> {
        private boolean asc = true;

        public FileSizeComparator(boolean asc) {
            this.asc = asc;
        }

        public int compare(FileBean o1, FileBean o2) {
            if (asc) {
                if (o1 == null || o1.getFile() == null) {
                    return -1;
                }
                if (o2 == null || o2.getFile() == null) {
                    return 1;
                }
                return (int) (o1.getFile().length() - o2.getFile().length());
            } else {
                if (o1 == null || o1.getFile() == null) {
                    return 1;
                }
                if (o2 == null || o2.getFile() == null) {
                    return -1;
                }
                return (int) (o2.getFile().length() - o1.getFile().length());
            }
        }
    }

}
