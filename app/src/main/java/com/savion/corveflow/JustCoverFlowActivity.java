package com.savion.corveflow;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.savion.corverflow.CoverFlowLayoutManger;
import com.savion.corverflow.RecyclerCoverFlow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.OrientationHelper;


public class JustCoverFlowActivity extends AppCompatActivity implements Adapter.onItemClick {

    private RecyclerCoverFlow mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_just_coverflow);
        initList();
    }

    public void scroll(View view) {
        mList.smoothScrollToPosition(mList.getSelectedNaturePos() + 10000);
    }

    public void random(View view) {
        mList.randomSmoothScrollToPosition();
    }

    private void initList() {
        mList = findViewById(R.id.list);
//        mList.setFlatFlow(true); //平面滚动
//        mList.setGreyItem(true); //设置灰度渐变
//        mList.setAlphaItem(true); //设置半透渐变
        mList.setLoop(); //循环滚动，注：循环滚动模式暂不支持平滑滚动
        mList.setFlatFlow(true);
        mList.setIntervalRatio(1f);
        mList.set3DItem(false);
        mList.setAlphaItem(false);
        mList.setOrientation(OrientationHelper.VERTICAL);
        mList.setAdapter(new Adapter(this, this, false));
        mList.setOnItemSelectedListener(new CoverFlowLayoutManger.OnSelected() {
            @Override
            public void onItemSelected(int position) {
                ((TextView) findViewById(R.id.index)).setText((position + 1) + "/" + mList.getLayoutManager().getItemCount());
            }
        });
    }

    @Override
    public void clickItem(int pos) {
        mList.smoothScrollToPosition(pos);
    }

    @Override
    protected void onDestroy() {
        mList.onDestory();
        super.onDestroy();
    }
}
