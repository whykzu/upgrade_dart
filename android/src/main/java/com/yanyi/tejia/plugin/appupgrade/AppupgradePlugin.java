package com.yanyi.tejia.plugin.appupgrade;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * AppupgradePlugin
 */
public class AppupgradePlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;

    private Context mContext = null;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        mContext = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "appupgrade");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if ("getAppInfo".equals(call.method)) {
            getAppInfo(mContext, result);
        } else if ("getApkDownloadPath".equals(call.method)) {
            result.success(mContext.getExternalFilesDir("").getAbsolutePath());
        } else if ("install".equals(call.method)) {
            String path = (String) ((Map) call.arguments).get("path");
            startInstall(mContext, path);
        } else if ("getInstallMarket".equals(call.method)) {
            Map map = (Map) call.arguments;
            ArrayList<String> packageList = getInstallMarket(mContext, (ArrayList<String>)map.get("packages"));
            result.success(packageList);
        } else if ("toMarket".equals(call.method)) {
            Map<String, String> map = (Map<String, String>) call.arguments;
            String marketPackageName = map.get("marketPackageName");
            String marketClassName = map.get("marketClassName");
            toMarket(mContext, marketPackageName, marketClassName);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    /**
     * ??????app??????
     *
     * @param context
     * @param result
     * @throws PackageManager.NameNotFoundException
     */
    private void getAppInfo(Context context, Result result){
        try{
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            HashMap<String, String> map = new HashMap<>();
            map.put("packageName", packageInfo.packageName);
            map.put("versionName", packageInfo.versionName);
            map.put("versionCode", String.valueOf(packageInfo.versionCode));
            result.success(map);
        }catch (PackageManager.NameNotFoundException e1) {
            Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
            result.error("404", "???????????????", null);
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param context
     */
    private void toMarket(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            Uri uri = Uri.parse("market://details?id=" + packageInfo.packageName);
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(goToMarket);
        } catch (PackageManager.NameNotFoundException e1) {
            Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e2) {
            Toast.makeText(context, "???????????????????????????????????????", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param context
     * @param marketPackageName
     * @param marketClassName
     */
    private void toMarket(Context context, String marketPackageName, String marketClassName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            Uri uri = Uri.parse("market://details?id=" + packageInfo.packageName);
            boolean nameEmpty = marketPackageName == null || marketPackageName.trim().length() == 0;
            boolean classEmpty = marketClassName == null || marketClassName.trim().length() == 0;
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            if (nameEmpty || classEmpty) {
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                goToMarket.setClassName(marketPackageName, marketClassName);
            }
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(goToMarket);
        } catch (PackageManager.NameNotFoundException e1) {
            Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e2) {
            Toast.makeText(context, "????????????????????????????????????(" + marketPackageName + ")", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ??????????????????????????????????????????
     * @param packages
     * @return
     */
    private ArrayList<String> getInstallMarket(Context context, ArrayList<String> packages) {
        ArrayList<String> pkgs = new ArrayList<>();
        for(String pk : packages){
            if (isPackageExist(context, pk)){
                pkgs.add(pk);
            }
        }
        return pkgs;
    }

    private boolean isPackageExist(Context context, String packageName) {
        if (packageName == null || "".equals(packageName.trim()))
            return false;
        try {
            context.getPackageManager().getApplicationInfo(packageName,PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * ??????app???android 7.0??????????????????????????????
     * @param context
     * @param path
     */
    private void startInstall(Context context, String path){
        File file = new File(path);
        if (file.exists()){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //7.0?????????
                Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", file);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                context.startActivity(intent);
            }else {
                //7.0??????
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

}
