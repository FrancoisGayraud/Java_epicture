package com.freeze.epitech.epicture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeze.epitech.epicture.R;
import com.squareup.picasso.Picasso;

public class AdapterImgur extends ArrayAdapter {
    private Context context;
    private LayoutInflater inflater;

    private String[] imageTitle;
    private String[] imageUrls;

    public AdapterImgur(Context context, String[] imageUrls, String[] imageTitle) {
        super(context, R.layout.item_list, imageUrls);

        this.context = context;
        this.imageUrls = imageUrls;
        this.imageTitle = imageTitle;

        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = inflater.inflate(R.layout.item_list, parent, false);
        }
        ImageView img = convertView.findViewById(R.id.photo);
        TextView title = convertView.findViewById(R.id.title);
        title.setText(imageTitle[position]);
        Picasso
                .with(context)
                .load(imageUrls[position])
                .into((ImageView) img);

        return convertView;
    }
}