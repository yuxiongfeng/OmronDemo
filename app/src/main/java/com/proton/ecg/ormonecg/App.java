package com.proton.ecg.ormonecg;

import android.app.Application;

import com.proton.ecg.omron.CarePatchECGCardKitManager;
import com.tencent.bugly.crashreport.CrashReport;
import com.wms.logger.Logger;

import java.io.File;

/**
 * @Description:
 * @Author: yxf
 * @CreateDate: 2021/4/15 10:33
 * @UpdateUser: yxf
 * @UpdateDate: 2021/4/15 10:33
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        File loggerFile = getExternalFilesDir("logger");
        if (!loggerFile.exists()) {
            loggerFile.mkdirs();
        }
        com.wms.logger.Logger.newBuilder()
                .tag("omronLog")
                .showThreadInfo(false)
                .methodCount(1)
                .methodOffset(5)
                .context(this)
                .deleteOnLaunch(true)
                .isDebug(BuildConfig.DEBUG)
                .logPath(loggerFile.getPath())
                .saveFile(BuildConfig.DEBUG)
                .build();
        Logger.w("loggerFile path===", loggerFile.getPath());
        CrashReport.initCrashReport(getApplicationContext(), "2fc46bc871", BuildConfig.DEBUG);
        CarePatchECGCardKitManager.getInstance().init(this);
    }
}
