package com.GuangSu.Lucy;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.unity3d.player.UnityPlayer;

import java.lang.reflect.Method;
import java.util.List;

//屏幕适配代码
public class ScreenAdapation {
    public enum AndroidPhoneType {
        NONE,
        // 华为
        HuaWei,
        // 小米
        XiaoMi,
        //oppo
        OPPO,
        //vivo
        VIVO
    }

    private static String LogTag = "ScreenAdapation";
    public static final String DISPLAY_NOTCH_STATUS = "display_notch_status";

    /*
    开始适配
     */
    public static void InitScreenAdapation(Activity activity)
    {
        // Android手机适配不使用刘海区域
        if (Build.VERSION.SDK_INT >= 28) {
            Log.i("android版本","28及以上");
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            //始终允许窗口延伸到屏幕短边上的刘海区域
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            activity.getWindow().setAttributes(lp);
            //getNotchParams(activity);
        }else
        {
            Log.i("android版本","28以下");
            // 全屏显示
            //ScreenAdapation.StartScreenAdapation(this);
        }

    }


   /******************************************************AndroidP及以上版本*****************************************/

    /**
     * 获得刘海区域信息
     */
    @TargetApi(28)
    public static void getNotchParams(Activity activity) {
        final View decorView = activity.getWindow().getDecorView();
        if (decorView != null) {
            decorView.post(new Runnable() {
                @Override
                public void run() {
                    WindowInsets windowInsets = decorView.getRootWindowInsets();
                    if (windowInsets != null) {
                        // 当全屏顶部显示黑边时，getDisplayCutout()返回为null
                        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
                        float height=displayCutout.getSafeInsetTop();
                        Log.e("TAG", "安全区域距离屏幕左边的距离 SafeInsetLeft:" + displayCutout.getSafeInsetLeft());
                        Log.e("TAG", "安全区域距离屏幕右部的距离 SafeInsetRight:" + displayCutout.getSafeInsetRight());
                        Log.e("TAG", "安全区域距离屏幕顶部的距离 SafeInsetTop:" + displayCutout.getSafeInsetTop());
                        Log.e("TAG", "安全区域距离屏幕底部的距离 SafeInsetBottom:" + displayCutout.getSafeInsetBottom());
                        // 获得刘海区域
                        List<Rect> rects = displayCutout.getBoundingRects();
                        if (rects == null || rects.size() == 0) {
                            Log.e("TAG", "不是刘海屏");
                        } else {
                            UnityPlayer.UnitySendMessage("ScreenAdapation", "ScreenAdaptation", Float.toString(height) );
                            Log.e("TAG", "刘海屏数量:" + rects.size());
                            for (Rect rect : rects) {
                                Log.e("TAG", "刘海屏区域：" + rect);
                            }
                        }
                    }
                }
            });
        }
    }

    /******************************************Android P以下的版本*******************************************************/
    public static void StartScreenAdapation(Context curContext) {

        Log.i(LogTag, "curContext:" + curContext);
        String phoneModel = GetManufature();
        Log.i(LogTag, "phoneModel:" + phoneModel);

        AndroidPhoneType type = GetAndroidPhoneType(phoneModel);

        switch (type) {
            case HuaWei:
                huaWeiScreenAdaptation(curContext);
                break;
            case XiaoMi:
                xiaomiScreenAdaptation(curContext);
                break;
            case OPPO:
                oppoScreenAdaptation(curContext);
                break;
            case VIVO:
                vivoScreenAdaptation(curContext);
                break;
            default:
                break;
        }
    }

    private static AndroidPhoneType GetAndroidPhoneType(String phoneModel) {
        AndroidPhoneType type = AndroidPhoneType.NONE;
        String phoneUpperModel = phoneModel.toUpperCase();

        Log.i(LogTag, "phoneUpperModel:" + phoneUpperModel);

        if (phoneUpperModel.contains("HUAWEI")) {
            type = AndroidPhoneType.HuaWei;
        } else if (phoneUpperModel.contains("XIAOMI")) {
            type = AndroidPhoneType.XiaoMi;
        } else if (phoneUpperModel.contains("OPPO")) {
            type = AndroidPhoneType.OPPO;
        } else if (phoneUpperModel.contains("VIVO")) {
            type = AndroidPhoneType.VIVO;
        }

        Log.i(LogTag, "type:" + type);
        return type;
    }

    /*获取手机型号
     */
    private static String GetModel() {
        String str = android.os.Build.MODEL;
        Log.i(LogTag, str);
        return str;
    }

    /*获取手机制造商
     */
    private static String GetManufature() {
        String str = android.os.Build.MANUFACTURER;
        Log.i(LogTag, str);
        return str;
    }

    /*华为的刘海屏*/
    private static void huaWeiScreenAdaptation(Context curContext) {
        if (hasNotchInScreen_huawei(curContext)) {
            int[] iwh = getNotchSize_huawei(curContext);
            // 0表示“默认”，1表示“隐藏显示区域”
            int mIsNotchSwitchOpen = Settings.Secure.getInt(curContext.getContentResolver(), DISPLAY_NOTCH_STATUS, 0);
            Log.i(LogTag, iwh[0] + "|" + iwh[1] + ",mIsNotchSwitchOpen:" + mIsNotchSwitchOpen);
            UnityPlayer.UnitySendMessage("ScreenAdapation", "ScreenAdaptation", Integer.toString(iwh[1]));
        } else {
            Log.i(LogTag, "不是刘海屏幕！");
        }
    }

    /*
     * 是否是刘海屏手机： true：是刘海屏   false：非刘海屏
     */
    private static boolean hasNotchInScreen_huawei(Context context) {
        boolean ret = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
            Object obj = get.invoke(HwNotchSizeUtil);
            Log.e(LogTag, "000:"+obj.toString());
            if(obj != null)
                ret = obj.toString().toUpperCase().equals("TRUE") ? true : false;
            Log.e(LogTag, "000:"+ret);
        } catch (ClassNotFoundException e) {
            Log.e(LogTag, "error hasNotchInScreen ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(LogTag, "error hasNotchInScreen NoSuchMethodException");
        } catch (Exception e) {
            Log.e(LogTag, "error hasNotchInScreen Exception:"+e.getMessage());
        } finally {
            return ret;
        }
    }

    /*
     * 获取刘海尺寸：width、height int[0]值为刘海宽度 int[1]值为刘海高度
     */
    private static int[] getNotchSize_huawei(Context context) {
        int[] ret = new int[]{0, 0};
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("getNotchSize");
            ret = (int[]) get.invoke(HwNotchSizeUtil);
        } catch (ClassNotFoundException e) {
            Log.e(LogTag, "error getNotchSize ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(LogTag, "error getNotchSize NoSuchMethodException");
        } catch (Exception e) {
            Log.e(LogTag, "error getNotchSize Exception:"+e.getMessage());
        } finally {
            return ret;
        }
    }


    private static void xiaomiScreenAdaptation(Context context) {
        if (hasNotchInScreen_Xiaomi(context)) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                int result = context.getResources().getDimensionPixelSize(resourceId); //刘海屏的高度
                Log.e(LogTag, "xiaomiScreenAdaptation:" + result);
                UnityPlayer.UnitySendMessage("ScreenAdapation", "ScreenAdaptation", Integer.toString(result));
            }
            Log.e(LogTag, "00000:" + resourceId);
        } else {
            Log.e(LogTag, "222222222");
        }
    }

    private static boolean hasNotchInScreen_Xiaomi(Context context) {
        boolean result = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class systemPro = cl.loadClass("android.os.SystemProperties");
            Method get = systemPro.getMethod("get", String.class);
            Object obj = (get.invoke(systemPro, "ro.miui.notch"));
            if(obj != null)
                result = (String) obj == "1" ? true : false;
            Log.e(LogTag, "curResult:"+result);
        } catch (ClassNotFoundException e) {
            Log.e(LogTag, "error hasNotchInScreen_Xiaomi ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(LogTag, "error hasNotchInScreen_Xiaomi NoSuchMethodException");
        } catch (Exception e) {
            Log.e(LogTag, "error hasNotchInScreen_Xiaomi Exception:" + e.getMessage());
        } finally {
            return result;
        }
    }

    private static void oppoScreenAdaptation(Context context) {

        if(context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism")) {
            Log.i(LogTag, "oppoScreenAdaptation true");
            UnityPlayer.UnitySendMessage("ScreenAdapation", "ScreenAdaptation", "80");
        }else{
            Log.i(LogTag, "oppoScreenAdaptation false");
        }
    }

    private static void vivoScreenAdaptation(Context context) {
        if(hasNotchInScreen_Vivo(context)){
            Log.e(LogTag, "vivoScreenAdaptation true");
            //UnityPlayer.UnitySendMessage("ScreenAdapation", "ScreenAdaptation", "32");
        }
        else{
            Log.e(LogTag, "vivoScreenAdaptation false");
        }
    }
    private static final int NOTCH_IN_SCREEN_VOIO=0x00000020;//是否有凹槽
    private static final int ROUNDED_IN_SCREEN_VOIO=0x00000008;//是否有圆角
    private static boolean hasNotchInScreen_Vivo(Context context){
        boolean ret = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class FtFeature = cl.loadClass("android.util.FtFeature");
            Method get = FtFeature.getMethod("isFeatureSupport",int.class);
            Object obj = get.invoke(FtFeature,NOTCH_IN_SCREEN_VOIO);
            Log.e(LogTag, "000:"+obj.toString());
            if(obj != null)
                ret = obj.toString().toUpperCase().equals("TRUE") ? true : false;
            else {
                Log.e(LogTag, "obj is null!");
            }
        } catch (ClassNotFoundException e){
            Log.e(LogTag, "hasNotchInScreen ClassNotFoundException");
        }
        catch (NoSuchMethodException e){
            Log.e(LogTag, "hasNotchInScreen NoSuchMethodException");
        }
        catch (Exception e){
            Log.e(LogTag, "hasNotchInScreen Exception");
        }
        finally{
            return ret;}
    }
}
