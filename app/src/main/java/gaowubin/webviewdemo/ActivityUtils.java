package gaowubin.webviewdemo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ActivityUtils {


    /**
     * 查看网络是否连接
     *
     * @param
     * @return
     */
    public static boolean isNetConnected(Context context) {
        boolean isNetConnected;
        // 获得网络连接服务
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
//			String name = info.getTypeName();
//			L.i("当前网络名称：" + name);
            isNetConnected = true;
        } else {
            isNetConnected = false;
        }
        return isNetConnected;
    }

}
