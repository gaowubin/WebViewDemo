package gaowubin.webviewdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_RECORDER_IMAGE = 100;
    private final static int REQUEST_CODE_RECORDER_VIDEO = 120;
    private WebView mWebView;
    private Uri imageUri;
    private ValueCallback<Uri> mUploadFile;
    private ValueCallback<Uri[]> mFilePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.webView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
//        mWebView.getSettings().setCacheMode(ActivityUtils.isNetConnected() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (null != mWebView && null != mWebView.getSettings())
                    mWebView.getSettings().setBlockNetworkImage(false);
                //判断webview是否加载了，图片资源
                if (null != mWebView && null != mWebView.getSettings() && !mWebView.getSettings().getLoadsImagesAutomatically()) {
                    //设置wenView加载图片资源
                    mWebView.getSettings().setLoadsImagesAutomatically(true);
                }
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.i("test", " shouldOverrideUrlLoading url: " + url);
                //如果url以weixin://开头，表示要拉起微信；以alipay开头表示要拉起支付宝；以tel:开头表示拔打电话
                if (url.startsWith("weixin://") || url.startsWith("alipay") || url.startsWith("tel:")) {
                    try {//try catch 以免崩溃
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        PackageManager packageManager = getPackageManager();
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent);
                            return true;//处理成功不调用默认实现
                        }
                    } catch (Exception e) {
                    }
                }
                // 其它调用默认实现(即return false 浏览器自己会处理页面跳转)
                // 注意不可用view.loadUrl(url),会丢失referer导致微信支付报“商家参数格式有误”的错
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

            //Android 5.0 以下 必须重写此方法
            public void openFileChooser(final ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                boolean fromCamera = !TextUtils.isEmpty(capture);
                if (acceptType != null && acceptType.contains("video")) {//处理视频
                    if (fromCamera) {//只处理拍摄视频
                        if (captureVideoFromCamera()) {
                            mUploadFile = uploadFile;//暂存，用于拍摄完视频后回调H5
                            return;
                        }
                    }
                }
                if (acceptType != null && acceptType.contains("image")) {//处理图片
                    if (fromCamera) {//只处理拍照
                        if (captureImageFromCamera()) {
                            mUploadFile = uploadFile;//暂存，用于拍完照片后回调H5
                        }
                    }
                }
            }

            //Android 5.0 及以上 必须重写此方法
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                boolean fromCamera = fileChooserParams.isCaptureEnabled();
                String[] acceptTypes = fileChooserParams.getAcceptTypes();
                if (acceptTypes != null && acceptTypes.length > 0) {
                    boolean isVideo = false;
                    for (String acceptType : acceptTypes) {
                        if (acceptType.contains("video")) {
                            isVideo = true;
                            break;
                        }
                    }
                    if (isVideo) {//处理视频
                        if (fromCamera) {//只处理拍摄视频
                            if (captureVideoFromCamera()) {
                                mFilePathCallback = filePathCallback;//暂存，用于拍摄完视频后回调H5
                                return true;//返回true表示APP处理文件选择
                            }
                        }
                    }
                    boolean isImage = false;
                    for (String acceptType : acceptTypes) {
                        if (acceptType.contains("image")) {
                            isImage = true;
                            break;
                        }
                    }
                    if (isImage) {//处理图片
                        if (fromCamera) {//只处理拍照
                            if (captureImageFromCamera()) {
                                mFilePathCallback = filePathCallback;//暂存，用于拍完照片后回调H5
                                return true;//返回true表示APP处理文件选择
                            }
                        }
                    }
                }
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });

        String url = "https://m-stg2.tianxiaxinyong.com/cooperation/b-test.html";
        Log.i("test", "url: " + url);


        mWebView.loadUrl(url);
    }

    private void requestPermissions() {
        final RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.CAMERA)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) { // Always true pre-M
                        } else {
                            Toast.makeText(MainActivity.this, "请开启相机权限", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean captureVideoFromCamera() {
        File cacheDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cacheDir = new File(getExternalCacheDir(), "video");
        }
        if (cacheDir == null) {
            cacheDir = new File(getCacheDir(), "video");
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        File outFile = new File(cacheDir, System.currentTimeMillis() + ".mp4");
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {//如果api大于Android api23 要替换获取文件Uri方式
            //此方法第二个参数authority的值要用项目中的值来替换,可网上找Android 7.0 FileProvider相关介绍
            Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", outFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//加入flag
        } else {
            Uri uri = Uri.fromFile(outFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        }
        //调系统相机拍摄视频需要用到相机权限，先判断有没有这个权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {//有相机权限
            PackageManager packageManager = getPackageManager();
            if (intent.resolveActivity(packageManager) != null) {
                try {
                    startActivityForResult(intent, REQUEST_CODE_RECORDER_VIDEO);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {//没有相机权限
            requestPermissions();
        }
        return false;
    }


    private boolean captureImageFromCamera() {
        File cacheDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cacheDir = new File(getExternalCacheDir(), "images");
        }
        if (cacheDir == null) {
            cacheDir = new File(getCacheDir(), "images");
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        File outFile = new File(cacheDir, System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {//如果api大于Android api23 要替换获取文件Uri方式
            //此方法第二个参数authority的值要用项目中的值来替换,可网上找Android 7.0 FileProvider相关介绍
            imageUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", outFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            imageUri = Uri.fromFile(outFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {//有相机权限
            PackageManager packageManager = getPackageManager();
            if (intent.resolveActivity(packageManager) != null) {
                try {
                    startActivityForResult(intent, REQUEST_CODE_RECORDER_IMAGE);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {//没有相机权限
            requestPermissions();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = null;
        if (requestCode == REQUEST_CODE_RECORDER_VIDEO && resultCode == RESULT_OK && data != null) {
            uri = data.getData();
        }
        if (requestCode == REQUEST_CODE_RECORDER_IMAGE && resultCode == RESULT_OK) {
            uri = imageUri;//拍照片data不会返回uri，用之前暂存下的
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//android 5.0及以上
            if (mFilePathCallback != null) {//将拍摄的照片或者视频回调给H5
                if (uri != null) {
                    mFilePathCallback.onReceiveValue(new Uri[]{uri});
                } else {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = null;
            }
        } else {//android 5.0以下
            if (mUploadFile != null) {//将拍摄的照片或者视频回调给H5
                if (uri != null) {
                    mUploadFile.onReceiveValue(uri);
                } else {
                    mUploadFile.onReceiveValue(null);
                }
                mUploadFile = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }
}
