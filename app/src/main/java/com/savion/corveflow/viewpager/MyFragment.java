package com.savion.corveflow.viewpager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.savion.corveflow.Adapter;
import com.savion.corveflow.R;
import com.savion.corverflow.CoverFlowLayoutManger;
import com.savion.corverflow.RecyclerCoverFlow;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


/**
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version RecyclerCoverFlow
 * @Datetime 2017-07-26 15:11
 * @since RecyclerCoverFlow
 */

public class MyFragment extends Fragment {
    private RecyclerCoverFlow mList;
    private TextView mIndex;


    public static Fragment newInstance() {
        MyFragment fragment = new MyFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment, container, false);
        initList(rootView);
        return rootView;
    }

    private void initList(View rootView) {
        mList = (RecyclerCoverFlow) rootView.findViewById(R.id.list);
        mIndex = ((TextView)rootView.findViewById(R.id.index));
//        mList.setFlatFlow(true); //平面滚动
        mList.setGreyItem(true); //设置灰度渐变
//        mList.setAlphaItem(true); //设置半透渐变
        mList.setAdapter(new Adapter(getActivity(), false));
        mList.setOnItemSelectedListener(new CoverFlowLayoutManger.OnSelected() {
            @Override
            public void onItemSelected(int position) {
                mIndex.setText((position+1)+"/"+mList.getLayoutManager().getItemCount());
            }
        });
    }

    @Override
    public void onDestroyView() {
        mList.onDestory();
        super.onDestroyView();
    }
}
