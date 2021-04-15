import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import java.io.Serializable

/// list: {
///    "_id": "5e8c80ae2bce50b3ceaa80f0",
///    "author": "\u9e22\u5a9b",
///    "category": "Girl",
///    "createdAt": "2020-04-11 08:00:00",
///    "desc": "\u6211\u6ca1\u90a3\u4e48\u575a\u5f3a\uff0c\u53ea\u662f\u4e60\u60ef\u4e86\u4ec0\u4e48\u4e8b\u90fd\u81ea\u5df1\u625b\u3002 \u200b\u200b\u200b\u200b",
///    "images": ["http://gank.io/images/1c5cebd307fd49eaa75b368b11118b61"],
///    "likeCounts": 0,
///    "publishedAt": "2020-04-11 08:00:00",
///    "stars": 1,
///    "title": "\u7b2c52\u671f",
///    "type": "Girl",
///    "url": "http://gank.io/images/1c5cebd307fd49eaa75b368b11118b61",
///    "views": 274
///    }
/// detail:
///   {
///    "_id": "5e777432b8ea09cade05263f",
///    "author": "\u9e22\u5a9b",
///    "category": "Girl",
///    "content": "\u8fd9\u4e16\u754c\u603b\u6709\u4eba\u5728\u7b28\u62d9\u5730\u7231\u7740\u4f60\uff0c\u60f3\u628a\u5168\u90e8\u7684\u6e29\u67d4\u90fd\u7ed9\u4f60\u3002",
///    "createdAt": "2020-03-25 08:00:00",
///    "desc": "\u8fd9\u4e16\u754c\u603b\u6709\u4eba\u5728\u7b28\u62d9\u5730\u7231\u7740\u4f60\uff0c\u60f3\u628a\u5168\u90e8\u7684\u6e29\u67d4\u90fd\u7ed9\u4f60\u3002",
///    "images": ["http://gank.io/images/624ade89f93f421b8d4e8fafd86b1d8d"],
///    "index": 35,
///    "isOriginal": true,
///    "license": "",
///    "likeCount": 0,
///    "likeCounts": 1,
///    "likes": ["DBRef('users', ObjectId('5b6ce9c89d21226f4e09c779'))"],
///    "markdown": "",
///    "originalAuthor": "",
///    "publishedAt": "2020-03-25 08:00:00",
///    "stars": 1,
///    "status": 1,
///    "tags": [],
///    "title": "\u7b2c35\u671f",
///    "type": "Girl",
///    "updatedAt": "2020-03-25 08:00:00",
///    "url": "http://gank.io/images/624ade89f93f421b8d4e8fafd86b1d8d",
///    "views": 1043
///    },
data class GankBean(
    val id: String,
    val author: String,
    val category: String,
    val content: String,
    val createdAt: String,
    val desc: String,
    val index: Int,
    val isOriginal: Boolean,
    val licenseval: String,
    val likeCount: Int,
    val likeCounts: Int,
    val markdown: String,
    val originalAuthor: String,
    val publishedAt: String,
    val stars: Int,
    val status: Int,
    val tags: List<String>,
    val title: String,
    val type: String,
    val url: String,
    val views: Int,
    val updatedAt: String,
    val images: List<String>,
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}
