package cn.tiup.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import cn.tiup.sdk.oauth.Config;
import cn.tiup.sdk.oauth.Oauth2;
import cn.tiup.sdk.oauth.Token;


public class TiupClient {

    private static TiupClient instance = null;
    private  Config mOauthConfig;
    private  Context mContext;
    private  Oauth2 oauth2Client;
    private  Token token;

    /**
     * 获取全局client单例
     * @return
     */
    public static TiupClient getInstance() {
        if (instance == null) {
            instance = new TiupClient();
        }
        return instance;
    }


    /**
     * 初始化Oauth， 你可能需要从保存的学校配置文件中初始化
     * @return
     */
    public void init(Context context) {
        //relace this in your code, when your config file is dynamic
        Config config = Config.getInstance();
        config.setClientID("58bd26679d74e279c8421ecc.demo");
        config.setClientSecret("Op00P1TrqfIKzM9qbo44mcIXFiOxKTRytx");
        config.setBaseURL("https://accounts.tiup.cn");
        //根据自己到实际情况，获取scope
        config.setScope("userinfo");
        config.setRedirectURI("tiupapp://oauth/callback");
        String userAgent = getUserAgent(context) + " " + config.getUserAgent();
        config.setUserAgent(userAgent);
        instance.mOauthConfig = config;
        mContext = context;
    }

    /**
     * 退出client
     */
    public void logout() {
        this.oauth2Client.revoke();
        this.token = null;
        this.oauth2Client = null;
        Intent intent = new Intent(mContext, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
    }



    /**
     * 获取Oauth2Client单例
     * @return
     */
    public Oauth2 getOauth2Client() {
        if (oauth2Client != null) {
            return oauth2Client;
        }
        token = PrefUtils.getToken(mContext);
        if (!token.isValid()){
            return null;
        }
        oauth2Client = new Oauth2(mOauthConfig,token);
        oauth2Client.setOnTokenChangeListener(new Oauth2.TokenChangeListener() {
            @Override
            public void onRefresh(Token org, Token newToken) {
                PrefUtils.saveToken(mContext, newToken);
                token = newToken;
            }
            @Override
            public void onRevoke(Token token) {
                PrefUtils.removeToken(mContext);
            }
        });
        oauth2Client.setOnTokenUnauthorizedListener(new Oauth2.TokenUnauthorizedListener() {
            @Override
            public void onUnauthorized() {
                logout();
                if ((new Date().getTime()- token.getExpiryAt().getTime())> (TimeUnit.DAYS.toMillis(25))) {
                    Toast.makeText(mContext,"您长期没有登录，请重新登录", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext,"您已在其他设备上登录，请重新登录", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return oauth2Client;

    }

    /**
     * 获取当前Oauth配置内容
     * @return
     */
    public Config getOauthConfig() {
       return mOauthConfig;
    }

    /**
     * 每个应用都应该有自己都UserAgent，包括：当前包名，版本号，系统等
     * @return
     */
    private static String getUserAgent(Context context) {
        String packageName = context.getPackageName();
        String versionName;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return packageName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE + ")";
    }

}

