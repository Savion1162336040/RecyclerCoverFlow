package com.savion.corveflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.savion.corveflow.recyclerview.RecyclerViewActivity;
import com.savion.corveflow.viewpager.ViewpagerActivity;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onJustCoverFlowClick(View view) {
        Intent intent = new Intent(this, JustCoverFlowActivity.class);
        startActivity(intent);
    }

    public void onViewPagerClick(View view) {
        Intent intent = new Intent(this, ViewpagerActivity.class);
        startActivity(intent);
    }

    public void onRecyclerViewClick(View view) {
        Intent intent = new Intent(this, RecyclerViewActivity.class);
        startActivity(intent);
    }

    public void on3DCoverFlowClick(View view) {
        Intent intent = new Intent(this, CoverFlow3DActivity.class);
        startActivity(intent);
    }

}
