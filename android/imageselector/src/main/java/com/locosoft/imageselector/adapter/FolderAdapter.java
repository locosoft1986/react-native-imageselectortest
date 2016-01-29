package com.locosoft.imageselector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.locosoft.imageselector.utils.FakeR;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.locosoft.imageselector.bean.Folder;

/**
 * Folder's Adapter
 * Created by Nereo on 2015/4/7. Modified by Jeremy Weasley on 2015.06.20
 */
public class FolderAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private FakeR mFakeR;

    private List<Folder> mFolders = new ArrayList<Folder>();

    int mImageSize;

    int lastSelected = 0;

    public FolderAdapter(Context context){
        mContext = context;
        mFakeR = new FakeR(mContext);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageSize = mContext.getResources().getDimensionPixelOffset(mFakeR.getId("dimen", "folder_cover_size"));
    }

    /**
     * setting folder data source
     * @param folders
     */
    public void setData(List<Folder> folders) {
        if(folders != null && folders.size()>0){
            mFolders = folders;
        }else{
            mFolders.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mFolders.size()+1;
    }

    @Override
    public Folder getItem(int i) {
        if(i == 0) return null;
        return mFolders.get(i-1);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(view == null){
            view = mInflater.inflate(mFakeR.getId("layout", "list_item_folder"), viewGroup, false);
            holder = new ViewHolder(view, mFakeR, mContext);
        }else{
            holder = (ViewHolder) view.getTag();
        }
        if (holder != null) {
            if(i == 0){
                holder.name.setText(mFakeR.getId("string", "imageselector_folder_all"));

                String imageCountUnit;
                if(getTotalImageSize() > 1) {
                    imageCountUnit = getTotalImageSize()
                            + mContext.getResources().getString(mFakeR.getId("string", "imageselector_image_size_multiple"));
                } else {
                    imageCountUnit = getTotalImageSize()
                            + mContext.getResources().getString(mFakeR.getId("string", "imageselector_image_size_single"));
                }

                holder.size.setText(imageCountUnit);
                if(mFolders.size()>0){
                    Folder f = mFolders.get(0);
                    Picasso.with(mContext)
                            .load(new File(f.cover.path))
                            .error(mFakeR.getId("drawable", "default_error"))
                            .resize(mImageSize, mImageSize)
                            .centerCrop()
                            .into(holder.cover);
                }
            }else {
                holder.bindData(getItem(i));
            }
            if(lastSelected == i){
                holder.indicator.setVisibility(View.VISIBLE);
            }else{
                holder.indicator.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    private int getTotalImageSize(){
        int result = 0;
        if(mFolders != null && mFolders.size()>0){
            for (Folder f: mFolders){
                result += f.images.size();
            }
        }
        return result;
    }

    public void setSelectIndex(int i) {
        if(lastSelected == i) return;

        lastSelected = i;
        notifyDataSetChanged();
    }

    public int getSelectIndex(){
        return lastSelected;
    }

    class ViewHolder{
        FakeR fakeR;
        ImageView cover;
        TextView name;
        TextView size;
        ImageView indicator;
        Context context;
        ViewHolder(View view, FakeR fakeR, Context context){
            this.fakeR = fakeR;
            this.context = context;
            cover = (ImageView)view.findViewById(fakeR.getId("id", "imageselector_cover"));
            name = (TextView) view.findViewById(fakeR.getId("id", "imageselector_name"));
            size = (TextView) view.findViewById(fakeR.getId("id", "imageselector_size"));
            indicator = (ImageView) view.findViewById(fakeR.getId("id", "imageselector_indicator"));
            view.setTag(this);
        }

        void bindData(Folder data) {
            name.setText(data.name);
            String imageCountUnit;
            if(data.images.size() > 1) {
                imageCountUnit = data.images.size()
                        + context.getResources().getString(fakeR.getId("string", "imageselector_image_size_multiple"));
            } else {
                imageCountUnit = data.images.size()
                        + context.getResources().getString(fakeR.getId("string", "imageselector_image_size_single"));
            }
            size.setText(imageCountUnit);
            // 显示图片
            Picasso.with(mContext)
                    .load(new File(data.cover.path))
                    .placeholder(fakeR.getId("drawable", "default_error"))
                    .resize(mImageSize, mImageSize)
                    .centerCrop()
                    .into(cover);
            // TODO Choose tag
        }
    }

}
