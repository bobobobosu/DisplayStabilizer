package com.project.nicki.displaystabilizer.UI.UIv1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.StringDef;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.project.nicki.displaystabilizer.R;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.init;

public class UIv1_draw0 extends AppCompatActivity {
    //display results
    public static TextView view_ORI_CHAR;
    public static TextView view_ORI_CONF;
    public static TextView view_STA_CHAR;
    public static TextView view_STA_CONF;
    public static Handler update_results;


    //draw view
    DemoDraw3 j_DemoDraw ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        j_DemoDraw = new DemoDraw3(this);
        j_DemoDraw = (DemoDraw3)findViewById(R.id.view_DemoDraw3);

        setContentView(R.layout.activity_uiv1_draw0);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        view_ORI_CHAR = (TextView)findViewById(R.id.ori_char);
        view_ORI_CONF = (TextView)findViewById(R.id.ori_conf);
        view_STA_CHAR = (TextView)findViewById(R.id.sta_char);
        view_STA_CONF = (TextView)findViewById(R.id.sta_conf);
        update_results = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                view_ORI_CHAR.setText(msg.getData().getString("ORI_CHAR"));
                view_ORI_CHAR.setTextColor(Color.BLACK);
                view_ORI_CONF.setText(String.valueOf("conf: "+msg.getData().getFloat("ORI_CONF")));
                view_ORI_CONF.setTextColor(Color.BLACK);
                view_STA_CHAR.setText(msg.getData().getString("STA_CHAR"));
                view_STA_CHAR.setTextColor(Color.RED);
                view_STA_CONF.setText(String.valueOf("conf: "+msg.getData().getFloat("STA_CONF")));
                view_STA_CONF.setTextColor(Color.RED);
            }
        };
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                init.initTouchCollection.save_and_clean();
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.history0:
                Intent goto_UIv1_compare0 = new Intent();
                goto_UIv1_compare0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                goto_UIv1_compare0.setClass(UIv1_draw0.this, UIv1_compare0.class);
                startActivity(goto_UIv1_compare0);
                break;
            case R.id.setttings0:
                Intent goto_UIv1_settings0 = new Intent();
                goto_UIv1_settings0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                goto_UIv1_settings0.setClass(UIv1_draw0.this, UIv1_setttings0.class);
                startActivity(goto_UIv1_settings0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
