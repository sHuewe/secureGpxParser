package de.shuewe.gpx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;




import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TrackSegment extends GPXElement {


    private static DateFormat DATE_FORMAT=null;
    private List<WayPoint> m_points;

    private int m_segmentNumber;

    private Double m_distance=null;

    public TrackSegment(){
        m_points=new ArrayList<>();
    }

    public TrackSegment(List<? extends WayPoint> points){
        this();
        m_points.addAll(points);
    }

    public void addPoints(List<? extends WayPoint> points){
        m_points.addAll(points);
        WayPoint oldFirst =getFirst();
        Collections.sort(m_points);
        if(!oldFirst.equals(getFirst())){
            for(WayPoint point:m_points){
                point.setIsStartPoint(false);
            }
        }
        getFirst().setIsStartPoint(true);
        m_distance=null;
    }

    public TrackSegment subList(int posStart,int posEnd){
        TrackSegment res= getInstance(m_points.subList(posStart,posEnd));
        res.setSegmentNumber(getSegmentNumber());
        return res;
    }

    public void addPoints(TrackSegment segment){
        List<? extends WayPoint> points = segment.getPoints();
        if(points ==null || points.isEmpty()){
            return;
        }
        addPoints(points);
    }

    public TrackSegment clone(){
        TrackSegment res =getInstance(new ArrayList<WayPoint>(m_points));
        res.setSegmentNumber(getSegmentNumber());
        return res;
    }


    protected TrackSegment getInstance(List<WayPoint> points){
        return new TrackSegment(points);
    }

    public int indexOf(WayPoint point){
        return m_points.indexOf(point);
    }

    public void setSegmentNumber(int pos){
        m_segmentNumber=pos;
    }

    public int getSegmentNumber(){
        return m_segmentNumber;
    }

    @Override
    protected Date getSortDate() {
        return m_points.isEmpty() ? new Date(0l):getLast().getDate();
    }

    @Override
    public View getListViewRow(Context context, LayoutInflater inflater, View convertView, ViewGroup viewGroup) {
    return null;
    }

    public boolean isEmpty(){
        return m_points.isEmpty();
    }

    public int size(){
        return m_points.size();
    }

    public WayPoint getFirst(){
        if(m_points.isEmpty()){
            return null;
        }
        return m_points.get(0);
    }

    public List<? extends WayPoint> getPoints(){
        return m_points;
    }

    public double getDistanceInKilometer(){
        if(m_distance == null) {
            WayPoint oldPoint = null;
            double res = 0;
            for (WayPoint point : m_points) {
                if (oldPoint != null) {
                    res += oldPoint.calculateDistanceInKilometer(point);
                }
                oldPoint = point;
            }
            m_distance=res;
        }
        return m_distance;
    }

    public double getAvgSpeedInKmH(){
        if(getLast().getDate() == null || getFirst().getDate() == null){
            return 0;
        }
        double hoursDif=((double)(getLast().getDate().getTime()-getFirst().getDate().getTime()))/(1000*60*60);
        if(hoursDif == 0){
            return 0;
        }
        return getDistanceInKilometer()/hoursDif;
    }

    public WayPoint getLast(){
        if(m_points.isEmpty()){
            return null;
        }
        return m_points.get(m_points.size()-1);
    }

    public WayPoint get(int i){
        if(m_points.isEmpty() || i>=m_points.size()){
            return null;
        }
        return m_points.get(i);
    }

    protected boolean remove(WayPoint point){
        return m_points.remove(point);
    }

}
