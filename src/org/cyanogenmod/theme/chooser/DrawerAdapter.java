/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.theme.chooser;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class DrawerAdapter extends BaseAdapter {
    private final static String TAG = DrawerAdapter.class.getName();
    private final static int NUM_OF_VIEW_TYPES = 1;
    private final static String TAG_DRAWER_ITEM = "DrawerItem";

    private Context mContext;
    private List<DrawerItem> mDrawerList = new ArrayList<DrawerItem>();
    private DrawerClickListener mDrawerClickListener;

    public interface DrawerClickListener {
        public void onNavItemSelected(DrawerItem item);
    }

    public DrawerAdapter(Context context,
                         DrawerClickListener listener) {
        mDrawerClickListener = listener;
        mContext = context;
        loadItems();
    }

    @Override
    public int getViewTypeCount() {
        return NUM_OF_VIEW_TYPES;
    }

    @Override
    public int getCount() {
        return mDrawerList.size();
    }

    @Override
    public Object getItem(int pos) {
        return mDrawerList.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return mDrawerList.get(pos).id;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final DrawerItem drawerItem = (DrawerItem) getItem(position);
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            row = inflater.inflate(R.layout.drawer_list_standard_item, parent, false);
            DrawerItemHolder item = new DrawerItemHolder();
            row.setTag(item);
            item.icon = (ImageView) row.findViewById(R.id.icon);
            item.firstLine = (TextView) row.findViewById(R.id.label);
        }

        DrawerItemHolder holder = (DrawerItemHolder) row.getTag();
        holder.icon.setImageResource(drawerItem.iconRes);
        holder.firstLine.setText(drawerItem.title);
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDrawerClickListener.onNavItemSelected(drawerItem);
            }
        });

        return row;
    }

    public static class DrawerItemHolder {
        ImageView icon;
        TextView firstLine;
    }

    private void loadItems() {
        try {
            XmlResourceParser parser = mContext.getResources().getXml(R.xml.drawer_list);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG &&
                        parser.getName().equals(TAG_DRAWER_ITEM)) {
                    final TypedArray a =
                            mContext.obtainStyledAttributes(attrs, R.styleable.DrawerItem);
                    int id = a.getResourceId(R.styleable.DrawerItem_id, 0);
                    String title = a.getString(R.styleable.DrawerItem_title);
                    int iconRes = a.getResourceId(R.styleable.DrawerItem_icon, 0);
                    String component = a.getString(R.styleable.DrawerItem_components);

                    DrawerItem item = new DrawerItem(id, title, iconRes, component);

                    mDrawerList.add(item);
                    a.recycle();
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Received exception parsing drawer list", e);
        }
    }

    public static class DrawerItem {
        public static final int TYPE_STANDARD = 0;

        public int id;
        public String title;
        public int iconRes;
        public String components;

        public DrawerItem(int id, String title, int iconRes, String components) {
            this.id = id;
            this.title = title;
            this.iconRes = iconRes;
            this.components = components;
        }
    }
}