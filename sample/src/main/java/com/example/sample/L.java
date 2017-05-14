package com.example.sample;

import android.util.Log;

/**
 * Created by Administrator on 2017/5/7.
 */

public class L {

    private static final String TAG= "IMOOC_OKhttp";
    private static boolean debug=true;
    public static void e(String msg) {
        if(debug)
            Log.e(TAG,msg);
    }
}
