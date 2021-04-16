package cn.archko.pdf.theme

/**
 * @author: archko 2021/4/16 :9:51 下午
 */
data class AppThemeState(
    var darkTheme: Boolean = false,
    var pallet: ColorPallet = ColorPallet.GREEN
)