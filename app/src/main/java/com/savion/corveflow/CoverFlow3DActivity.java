package com.savion.corveflow;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.savion.corverflow.CoverFlowLayoutManger;
import com.savion.corverflow.RecyclerCoverFlow;

import androidx.appcompat.app.AppCompatActivity;


/**
 * 3D 旋转
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version RecyclerCoverFlow
 * @Datetime 2020-09-01 09:21
 * @since RecyclerCoverFlow
 */
public class CoverFlow3DActivity extends AppCompatActivity implements Adapter.onItemClick {

    private RecyclerCoverFlow mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_just_coverflow);
        initList();
    }

    public void scroll(View view) {
        mList.smoothScrollToPosition(mList.getSelectedNaturePos() + 10);
    }

    public void random(View view) {
        mList.randomSmoothScrollToPosition();
    }

    private void initList() {
        mList = findViewById(R.id.list);
        mList.set3DItem(true); //3D 滚动
        mList.setIntervalRatio(-1);
        mList.setFlatFlow(true);
        mList.setLoop(); //循环滚动
        mList.setAdapter(new Adapter(this, this, true));
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