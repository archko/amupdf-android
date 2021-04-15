package cn.archko.sunflower.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * @author: archko 2020/12/22 :4:03 下午
 */
public class OkHttpSingleton {
    public static final String TAG = "okhttp3";
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/html";
    public static final MediaType MEDIATYPE_JSON = MediaType.parse("application/json");
    public static final MediaType MEDIATYPE_TEXT = MediaType.parse("text/html");

    private OkHttpClient okHttpClient;

    public static OkHttpSingleton getInstance() {
        return Factory.instance;
    }

    private static final class Factory {
        private static final OkHttpSingleton instance = new OkHttpSingleton();
    }

    private OkHttpSingleton() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        setCache(builder);
        builder.connectTimeout(15, TimeUnit.SECONDS);
        builder.readTimeout(300, TimeUnit.SECONDS);
        builder.writeTimeout(300, TimeUnit.SECONDS);
        okHttpClient = builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                return chain.proceed(chain.request());
            }
        }).build();
        //okHttpClient.setConnectTimeout(15000, TimeUnit.MILLISECONDS); // connect timeout,3.0之前的
        //okHttpClient.setReadTimeout(120000, TimeUnit.MILLISECONDS);
    }

    private void setCache(OkHttpClient.Builder builder) {
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public OkHttpClient getHttpClient(Interceptor interceptor) {
        OkHttpClient httpClient = okHttpClient.newBuilder()
                .addInterceptor(interceptor)
                .build();

        return httpClient;
    }

    public OkHttpClient getRetrofitHttpClient() {
        return getRetrofitHttpClient(null);
    }

    /**
     * 获取一个client,可以添加一个拦截器,默认gzip与日志拦截器
     *
     * @param interceptor
     * @return
     */
    public OkHttpClient getRetrofitHttpClient(Interceptor interceptor) {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        if (null != interceptor) {
            builder.addInterceptor(interceptor);
        }

        return builder.build();
    }

    /**
     * 获取一个client,可以添加多个拦截器
     *
     * @param interceptors 如果是空的,则默认添加日志与gzip,如果不是空的,则不添加默认的.
     * @return
     */
    public OkHttpClient getRetrofitHttpClientWithIntercepters(List<Interceptor> interceptors) {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        if (null != interceptors && interceptors.size() > 0) {
            for (Interceptor interceptor : interceptors) {
                builder.addInterceptor(interceptor);
            }
        } else {
        }

        return builder.build();
    }

    /**
     * 获取下载的client,下载需要的时间较长.
     *
     * @param interceptor
     * @return
     */
    public OkHttpClient getDownloadClient(Interceptor interceptor) {
        OkHttpClient.Builder builder = okHttpClient.newBuilder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS);
        if (null != interceptor) {
            builder.addInterceptor(interceptor);
        }
        setCache(builder);
        OkHttpClient httpClient = builder.build();
        return httpClient;
    }

    /**
     * 立即执行网络请求,这是同步操作,不能在ui线程处理
     *
     * @param request
     * @return
     * @throws IOException
     */
    public Response execute(Request request) throws IOException {
        return okHttpClient.newCall(request).execute();
    }

    /**
     * 将请求放入队列中,异步操作
     *
     * @param request
     * @param responseCallback
     */
    public void enqueue(Request request, Callback responseCallback) {
        okHttpClient.newCall(request).enqueue(responseCallback);
    }

    /**
     * 由于接口的多样性,目前的替换方法不再了.
     *
     * @param
     */
//    public void proxyEnqueue(final com.ganji.android.core.network.Request oriRequest) {
//        ArrayList<NameValuePair> headers = oriRequest.getHeaders();
//        Request.Builder build = new Request.Builder();
//        //Config里面默认湢这两项.
//        build.addHeader("Accept", "*/*");
//        build.addHeader("Accept-Encoding", "gzip");
//        String contentType = null;
//        for (NameValuePair nvp : headers) {
//            if (!StringUtil.isEmpty(nvp.value)) {
//                build.addHeader(nvp.name, nvp.value);
//                if ("Content-Type".equals(nvp.name)) {
//                    contentType = nvp.value;
//                }
//            }
//        }
//        if (null == contentType) {
//            contentType = CONTENT_TYPE_FORM_URLENCODED;
//            build.addHeader("Content-Type", contentType);
//        }
//
//        boolean doOutput = oriRequest.isDoOutput();
//        boolean isMultiPart = doOutput && oriRequest.getUploadFiles().size() > 0;
//
//        String url = oriRequest.getUrl();
//        if (isMultiPart) {  //发文件,分为表单数据与文件数据两部分.
//            build.removeHeader("Content-Type");
//            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
//            //表单数据
//            ArrayList<NameValuePair> params = oriRequest.getParams();
//            JSONObject bodyObject = new JSONObject();
//            for (NameValuePair nv : params) {
//                try {
//                    bodyObject.put(nv.name, nv.value);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (bodyObject.length() > 0) {
//                multipartBuilder.addFormDataPart("jsonArgs", bodyObject.toString());
//            }
//
//            //文件数据
//            ArrayList<UploadFile> uploadFiles = oriRequest.getUploadFiles();
//            for (UploadFile nv : uploadFiles) {
//                InputStream is = null;
//                try {
//                    is = new FileInputStream(new File(nv.filePath));
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//
//                multipartBuilder.addFormDataPart(nv.fieldName, nv.fieldName, createCustomRequestBody(MultipartBody.FORM, is,
//                        new ProgressListener() {
//                            @Override
//                            public void onProgress(long totalBytes, long remainingBytes, boolean done) {
//                                if (oriRequest.getResponseListener() != null) {
//                                    oriRequest.getResponseListener().onHttpProgress(false, totalBytes, totalBytes - remainingBytes);
//                                }
//                            }
//                        }));
//            }
//        } else {
//            String method = oriRequest.getMethod();
//            try {
//                if (oriRequest.getParams() != null && oriRequest.getParams().size() > 0) {
//                    String postBody = oriRequest.getPostBody();
//                    if (!StringUtil.isEmpty(postBody)) {
//                        DLog.d(TAG, "postBody:" + StringUtil.urlDecode(postBody));
//                    }
//                    if (!oriRequest.isDoOutput()) {
//                        if (url.contains("?")) {
//                            url += "&" + postBody;
//                        } else {
//                            url += "?" + postBody;
//                        }
//                        build.method(method, null);
//                    } else {
//                        RequestBody body = okhttp3.internal.Util.EMPTY_REQUEST;
//                        if (CONTENT_TYPE_FORM_URLENCODED.equals(contentType)) {
//                            ArrayList<NameValuePair> params = oriRequest.getParams();
//                            FormBody.Builder builder = new FormBody.Builder();
//                            for (NameValuePair nv : params) {
//                                if (null != nv.value) {
//                                    builder.add(nv.name, URLDecoder.decode(nv.value));
//                                }
//                            }
//                            body = builder.build();
//                        } else {
//                            body = RequestBody.create(MEDIATYPE_JSON, postBody);
//                        }
//                        if (com.ganji.android.core.network.Request.POST.equalsIgnoreCase(method)) {
//                            build.post(body);
//                        } else if (com.ganji.android.core.network.Request.PUT.equalsIgnoreCase(method)) {
//                            build.put(RequestBody.create(MEDIATYPE_JSON, postBody));
//                        } else if (com.ganji.android.core.network.Request.DELETE.equalsIgnoreCase(method)) {
//                            build.delete(body);
//                        }
//                    }
//                } else {
//                    if (com.ganji.android.core.network.Request.POST.equalsIgnoreCase(method)) { //post不允许null
//                        build.method(method, okhttp3.internal.Util.EMPTY_REQUEST);
//                    } else {
//                        build.method(method, null);
//                    }
//                }
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
//        build.url(url);
//
//        OkHttpClient httpClient = okHttpClient.newBuilder()
//                .addInterceptor(new LogInterceptor()).build();
//        httpClient.newCall(build.build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                if (oriRequest.getResponseListener() != null) {
//                    com.ganji.android.core.network.Response oriResponse = new com.ganji.android.core.network.Response();
//                    if (NetworkUtil.isNetworkAvailable()) {
//                        oriResponse.mStatusCode = com.ganji.android.core.network.Response.STATUS_CONNECTION_TIMEOUT;
//                    } else {
//                        oriResponse.mStatusCode = com.ganji.android.core.network.Response.STATUS_CONNECTION_UNAVAILABLE;
//                        //oriResponse.mStatusCode = com.ganji.android.core.network.Response.STATUS_UNKNOWN_ERROR;
//                    }
//                    oriRequest.getResponseListener().onHttpComplete(oriRequest, oriResponse);
//                }
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (oriRequest.getResponseListener() != null) {
//                    com.ganji.android.core.network.Response oriResponse = new com.ganji.android.core.network.Response();
//                    oriResponse.mStatusCode = response.code();
//                    ResponseBody responseBody = response.body();
//                    //有一些接口, 会用gzip压缩,比如公司的职位.这里里面的数据是gzipsource,通过拦截器,就不用下面这段了.
//                    //String gzip = null == response.networkResponse() ? "" : response.networkResponse().header("Content-Encoding");
//                    //if ("gzip" .equalsIgnoreCase(gzip)) {
//                    //    if (null != responseBody) {
//                    //        BufferedSource source = responseBody.source();
//                    //        oriResponse.mInputStream = StreamUtil.flushInputStream(new GZIPInputStream(new ByteArrayInputStream(source.readByteArray())));
//                    //    }
//                    //} else {    //流再刷一次,避免不可以markSupported
//                    //    if (null != responseBody) {
//                    //        if (!responseBody.byteStream().markSupported()) {
//                    //            oriResponse.mInputStream = StreamUtil.flushInputStream(response.body().byteStream());
//                    //        } else {
//                    //            oriResponse.mInputStream = response.body().byteStream();
//                    //        }
//                    //    }
//                    //}
//                    oriResponse.mInputStream = response.body().byteStream();
//                    oriRequest.getResponseListener().onHttpComplete(oriRequest, oriResponse);
//                }
//            }
//        });
//    }
    public static RequestBody createCustomRequestBody(final MediaType contentType, final InputStream is, final ProgressListener listener) {
        return new RequestBody() {
            int contentLength = 0;

            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                if (is != null) {
                    try {
                        if (contentLength == 0) {
                            contentLength = is.available();
                        }
                        return contentLength;
                    } catch (Exception e) {
                        return 0;
                    }
                } else {
                    return 0;
                }
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source;
                try {
                    source = Okio.source(is);
                    //sink.writeAll(source);
                    Buffer buf = new Buffer();
                    long remaining = contentLength();
                    for (long readCount; (readCount = source.read(buf, 128)) != -1; ) {
                        sink.write(buf, readCount);
                        if (listener != null) {
                            listener.onProgress(contentLength(), remaining -= readCount, remaining == 0);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public interface ProgressListener {
        void onProgress(long totalBytes, long remainingBytes, boolean done);
    }
}
