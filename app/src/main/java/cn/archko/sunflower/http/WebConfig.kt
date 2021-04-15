package cn.archko.sunflower.http

/**
 * @author: archko 2021/3/22 :19:44
 */
object WebConfig {
    const val baseUrl = "https://baobab.kaiyanapp.com/api/";

    const val bannerUrl = "${baseUrl}v2/feed?"; //首页banner,num=1
    const val dailySelectionUrl = "${baseUrl}v4/tabs/selected"; //每日精选

    //发现-关注 ?&num=10&start=10&query=%s
    const val findFollowUrl = "${baseUrl}v4/tabs/follow?";

    const val keywordUrl = "${baseUrl}v3/queries/hot";

    //搜索?&num=10&start=10&query=%s
    const val searchUrl = "${baseUrl}v1/search";

    const val authorUrl = "${baseUrl}v4/pgcs/detail/tab";

    const val categoriesUrl = "${baseUrl}v4/categories?"; //发现-分类

    ////?&id=%d
    const val categoryDetailUrl = "${baseUrl}v4/categories/videoList";

    //&id=
    const val relatedVideoUrl = "${baseUrl}v4/video/related";

    //热门-周/月/总排行strategy=weekly,monthly,historical
    const val hotUrl = "${baseUrl}v4/rankList/videos";
}