package com.project.nicki.displaystabilizer.UI.UIv1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.project.nicki.displaystabilizer.R;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;

public class UIv1_main0 extends AppCompatActivity {
    //draw view
    DemoDraw3 j_DemoDraw ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uiv1_main0);
        
        j_DemoDraw = new DemoDraw3(this);
        j_DemoDraw = (DemoDraw3)findViewById(R.id.id_DemoDraw1);
    }


}
