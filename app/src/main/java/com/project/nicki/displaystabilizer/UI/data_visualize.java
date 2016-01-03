package com.project.nicki.displaystabilizer.UI;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.project.nicki.displaystabilizer.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class data_visualize extends AppCompatActivity {
    public TextView myTextView;

    @Bind(R.id.textView1)
    TextView textView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_visualize);
        ButterKnife.bind(this);
        myTextView = (TextView) findViewById(R.id.textView1);
        myTextView.setText("Example TextView by corn");
        myTextView.append("\tExample TextView by corn");
        myTextView.setTextSize(20);
        myTextView.setTextColor(Color.GREEN);
    }
}
