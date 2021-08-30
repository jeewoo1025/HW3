package com.example.hw3;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class BaseAdapterEx extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<Mp3File> mp3List;
    private LayoutInflater mLayoutInflater = null;

    public BaseAdapterEx(Context context, ArrayList<Mp3File> mp3List) {
        this.mContext = context;
        this.mp3List = mp3List;
        mLayoutInflater = LayoutInflater.from(context);  // 주어진 Context에서 객체 획득
    }

    @Override
    public int getCount() {
        return mp3List.size();
    }

    @Override
    public Object getItem(int i) {
        return mp3List.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View itemLayout = view;
        ViewHolder viewHolder = null;

        if(itemLayout == null) {
            itemLayout = mLayoutInflater.inflate(R.layout.list_view_item_layout, null);

            viewHolder = new ViewHolder();
            viewHolder.imgAlbum = (ImageView)itemLayout.findViewById(R.id.imgAlbum);
            viewHolder.txtFile = (TextView)itemLayout.findViewById(R.id.txtFile);
            itemLayout.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)itemLayout.getTag();
        }

        // 앨범 이미지, 파일 이름 설정
        viewHolder.txtFile.setText(mp3List.get(position).getName());

        // 앨범 사진
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), mp3List.get(position).getAlbumArtUri());
            viewHolder.imgAlbum.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.d("AAAA", e.getMessage());
            viewHolder.imgAlbum.setImageResource(R.drawable.song);
        }

        return itemLayout;
    }

    class ViewHolder {
        ImageView imgAlbum;
        TextView txtFile;
    }
}
