package com.example.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView mtvResult;
    private ImageView mIvResult;
    private String mBaseUrl = "http://192.168.199.156:8080/okhttp/";
    OkHttpClient okHttpClient;

    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //保持sission一致
        okHttpClient = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    private Map<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<Cookie>();
                    }
                }).build();
        mtvResult = (TextView) findViewById(R.id.id_tv_result);
        mIvResult = (ImageView) findViewById(R.id.id_iv_result);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * 1.拿到Okhttp对象
     * 2.构造Request
     * 2.1 构造requestBody
     * 2.2包装构造requestBody
     * 3.Call ->execute
     * @param view
     */
    public void doPost(View view) {
        //1.拿到Okhttp对象
        FormBody.Builder requsstBodyBuilder = new FormBody.Builder();
        //2.构造Request
        //2.1 构造requestBody
        requsstBodyBuilder.add("username", "zhm").add("password", "123");
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(mBaseUrl + "login").post(requsstBodyBuilder.build()).build();
        executeRequest(request);
    }

    public void doGet(View view) throws IOException {
        //1.拿到Okhttp对象
        //2.构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder
                .get()
                .url(mBaseUrl + "login?username=周鹤铭 &password=123")
                .build();
        executeRequest(request);
    }

    //传Joson
    public void doPostString(View view) {
        RequestBody requestbody = RequestBody.create(MediaType.parse("text/plain;chaset=utf-8"), "{username:周鹤铭,password:123}");
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(mBaseUrl + "postString").post(requestbody).build();
        executeRequest(request);
    }

    //传File
    public void doPostFile(View view) {
        File file = new File(Environment.getExternalStorageDirectory(), "banner2.jpg");
        if (!file.exists()) {
            L.e(file.getAbsolutePath() + "not exist");
            return;
        }
        RequestBody requestbody = RequestBody.create(MediaType.parse("appliction/octet-stream"), file);
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(mBaseUrl + "postFile").post(requestbody).build();
        executeRequest(request);
    }

    public void doUpload(View view) {
        File file = new File(Environment.getExternalStorageDirectory(), "banner2.jpg");
        if (!file.exists()) {
            L.e(file.getAbsolutePath() + "not exist");
            return;
        }

        MultipartBody.Builder multipartBUilder = new MultipartBody.Builder();
        RequestBody requestbody = multipartBUilder.setType(MultipartBody.FORM)
                .addFormDataPart("username", "zhm1234")
                .addFormDataPart("password", "1233445")
                .addFormDataPart("mPhoto", "zhm.jpg", RequestBody.create(MediaType.parse("appliction/octet-stream"), file))
                .build();

        CountingResquestBody body = new CountingResquestBody(requestbody, new CountingResquestBody.Listener() {
            @Override
            public void onRequestProgress(long byteWritten, long contentLength) {
                L.e(byteWritten + "/" + contentLength);
            }
        });

        Request.Builder builder = new Request.Builder();
        Request request = builder.url(mBaseUrl + "uploadInfo").post(body).build();
        executeRequest(request);
    }

    public void doDownload(View view) {
        //1.拿到Okhttp对象
        //2.构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder
                .get()
                .url(mBaseUrl + "files/a.jpg")
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e("onFailure:" + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                L.e("onResponse:");
                //文件总长度
                final long total = response.body().contentLength();
                Long sum = 0L;
                InputStream is = response.body().byteStream();
                int len = 0;
                File file = new File(Environment.getExternalStorageDirectory(), "a12306.jpg");
                byte[] buf = new byte[128];
                FileOutputStream fos = new FileOutputStream(file);
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    sum += len;
                    L.e(sum + "/" + total);
                    final long finalsum = sum;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //下载进度条
                            mtvResult.setText(finalsum + "/" + total);
                        }
                    });
                }
                fos.flush();
                fos.close();
                is.close(); //正常不能直接close 要分开try-catch
                L.e("download success !");
            }
        });
    }

    //下载图片
    public void doDownloadImg(View view) {
        Request.Builder builder = new Request.Builder();
        Request request = builder
                .get()
                .url(mBaseUrl + "files/a.jpg")
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e("onFailure:" + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                L.e("onResponse:");
                InputStream is = response.body().byteStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(is);
//                is.mark();
//                is.reset();  //需要图片压缩
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIvResult.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private void executeRequest(Request request) {
        //3.将Request封装为Call
        Call call = okHttpClient.newCall(request);
        //4.执行call
//        Response response = call.execute(); //同步
        //异步
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e("onFailure:" + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                L.e("onResponse:");
                final String res = response.body().string();
                L.e(res);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mtvResult.setText(res);
                    }
                });
            }
        });
    }

    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
