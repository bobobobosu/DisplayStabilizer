package com.project.nicki.displaystabilizer.UI.UIv1;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.project.nicki.displaystabilizer.R;

public class UIv1_splash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全螢幕顯示
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 不要顯示標題列
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_uiv1_splash);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Intent goto_UIv1_draw0 = new Intent();
                overridePendingTransition(0, 0);
                goto_UIv1_draw0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                goto_UIv1_draw0.setClass(getBaseContext(), UIv1_draw0.class);
                startActivity(goto_UIv1_draw0);
            }
        }).start();
    }
}
