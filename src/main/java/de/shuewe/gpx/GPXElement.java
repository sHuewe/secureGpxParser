package de.shuewe.gpx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

public abstract class GPXElement implements Comparable<GPXElement> {

    protected abstract Date getSortDate();
    @Override
    public int compareTo(GPXElement gpxElement){
        Date date2 = gpxElement.getSortDate();
        Date date=getSortDate();
        if(date == null && date2 == null){
            return 0;
        }
        if (date == null) {
            return 1;
        }

        if(date2 == null){
            return -1;
        }
        return date.compareTo(date2);
    }

    public abstract View getListViewRow(Context context, LayoutInflater inflater, View convertView, ViewGroup viewGroup);
}
