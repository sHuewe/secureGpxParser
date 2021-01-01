package de.shuewe.gpx;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static de.shuewe.gpx.SecureGPXParser.LOG_TAG;

/**
 * Class which represents Tracks in GPX.
 */
public class Track extends GPXElement {

    //Name of track
    private String m_name;

    //List of List of Waypoints. A single list represents a track-segment.
    private List<TrackSegment> m_waypoints = new ArrayList<TrackSegment>();

    /**
     * Public constructor.
     *
     * @param name of track
     */
    public Track(String name) {
        m_name=name;
        m_waypoints.add(getTrackSegmentInstance());

    }

    protected TrackSegment getTrackSegmentInstance(){
        return new TrackSegment();
    }

    /**
     * Public constructor
     *
     * @param name of track
     * @param points Initial set of points
     */
    public Track(String name,List<WayPoint> points) {
        this(name);
        addPoints(points);
    }

    public List<TrackSegment> getNumberedSegments(){
        List<TrackSegment> res= getSegments();
        int i=1;
        for(TrackSegment seg:res){
            seg.setSegmentNumber(i);
            i++;
        }
        return res;
    }

    public boolean removeSegment(int segmentNumber){
        //Starts with 1!
        if(m_waypoints.size()<segmentNumber){
            Log.i(LOG_TAG,"Cannot remove segment "+segmentNumber+", max size: "+m_waypoints.size());
            return false;
        }
        TrackSegment seg =m_waypoints.remove(segmentNumber-1);
        return seg!=null;
    }

    /**
     * Adds a Set of points to track
     *
     * @param points to be added
     */
    public void addPoints(List<? extends WayPoint> points) {
        if(points.isEmpty()){
            return;
        }
        m_waypoints.get(m_waypoints.size()-1).addPoints(points);
    }

    public void addPoints(TrackSegment segment){
        addPoints(segment.getPoints());
    }

    @Override
    protected Date getSortDate() {
        return getSegments().get(0).isEmpty() ? null: getSegments().get(0).get(0).getDate();
    }

    @Override
    public int compareTo(GPXElement gpxElement) {
        Track track = (Track)gpxElement;
        if (track.getSegments().get(0).isEmpty()) {
            return 1;
        }
        if (getSegments().get(0).isEmpty()) {
            return 0;
        }
        return getSegments().get(0).get(0).compareTo(track.getSegments().get(0).get(0));
    }

    @Override
    public View getListViewRow(Context context, LayoutInflater inflater, View convertView, ViewGroup viewGroup) {
        return null;
    }


    public List<? extends WayPoint> getAllPoints() {
        List<WayPoint> res = new ArrayList<>();
        for(TrackSegment segment: getSegments()){
            res.addAll(segment.getPoints());
        }
        return res;
    }

    /**
     * Finds number of matching segment to given List of points (=segment)
     *
     * @param points to match a segment for
     * @return number of segment, -1 if no suitable segment is available
     */
    private int findMatchingSegment(TrackSegment points){

        for(int i = 0; i< getSegments().size(); i++){
            TrackSegment segmentPoints = getSegments().get(i);
            if(segmentPoints.isEmpty()){
                continue;
            }
            Date minDate=segmentPoints.getFirst().getDate();
            Date maxDate=segmentPoints.getLast().getDate();
            if(WayPoint.isPointInRange(points.getFirst(),minDate,maxDate) || WayPoint.isPointInRange(points.getLast(),minDate,maxDate)){
                return i;
            }
        }
        return -1; //No matching segment found
    }

    /**
     * Find position of new track to be created.
     *
     * @param points to find position of new track for
     * @return index of new track
     */
    private int findNewTrackPosition(TrackSegment points){
        for(int i = 0; i<(getSegments().size()-1); i++){
            TrackSegment prevSegment = getSegments().get(i);
            TrackSegment nextSegment = getSegments().get(i+1);
            if(nextSegment.isEmpty()){
                continue;
            }
            Date minDate=prevSegment.get(prevSegment.size()-1).getDate();
            Date maxDate=nextSegment.get(0).getDate();
            if(WayPoint.isPointInRange(points.getFirst(),minDate,maxDate)){
                return i+1;
            }
        }
        //First point is not in current range -> Check if track is before or after current segments
        if(getSegments().get(0).getFirst().compareTo(points.getFirst())>0){
            return 0;
        }
        return -1; //No matching segment found, just add it at end of list
    }

    public TrackSegment getCurrentSegment(){
        return m_waypoints.get(m_waypoints.size()-1);
    }

    public void addSegments(List<TrackSegment> newSegments) {
        //Map<Integer,List<WayPoint>>
        startNewSegment();
        for(TrackSegment segment:newSegments){
            if(segment.isEmpty()){
                continue;
            }
            addPoints(segment);
            startNewSegment();
        }
        Collections.sort(m_waypoints);
        List<TrackSegment> segments = getNumberedSegments();
        Log.i(LOG_TAG,"Added segments. Total segments: "+segments.size());
    }



    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Track)) {
            return false;
        }
        Track otherTrack = (Track) o;
        if (otherTrack.getSegments().size() != getSegments().size()) {
            return false;
        }
        for (int i = 0; i < getSegments().size(); i++) {
            if(getSegments().get(i).size() != otherTrack.getSegments().size()){
                return false;
            }
            for(int j = 0; j< getSegments().get(i).size(); j++){
                if (!getSegments().get(i).get(j).equals(otherTrack.getSegments().get(i).get(j))) {
                    return false;
                }
            }

        }
        return true;
    }

    /**
     * Gets the distance of the track. The full distance is the sum of all segments.
     * The distances between several segments is not considered
     *
     * @return distance in km
     */
    public double getDistance(){
        double res=0;
        for(TrackSegment points:m_waypoints){
            res+=points.getDistanceInKilometer();
        }
        return res;
    }

    /**
     * Gets the name of the track
     *
     * @return String
     */
    public String getName(){
        return m_name;
    }


    /**
     * Sets the name of the track
     *
     * @param name to be set
     */
    public void setName(String name){
        m_name=name;
    }

    /**
     * Get Points
     *
     * @return List<List<WayPoint>>
     */
    public List<TrackSegment> getSegments() {
        return m_waypoints;
    }

    /**
     * Gets the total size of track (including all segments)
     *
     * @return int
     */
    public int getSize(){
        int res=0;
        for(TrackSegment points:m_waypoints){
            res+=points.size();
        }
        return res;
    }

    /**
     * Removes a given waypoint.
     *
     * @param point to be removed
     * @return true if WayPoint was removed
     */
    public boolean removeWaypoint(WayPoint point,boolean correctStart) {
        boolean removed=false;
        for(TrackSegment points: getSegments()){
            removed = removed | points.remove(point);
            if(removed && correctStart){
                if(!points.isEmpty()){
                    points.getFirst().setIsStartPoint(true);
                }
                break;
            }
        }
        return removed;
    }

    public boolean removeWaypoint(WayPoint point){
        return removeWaypoint(point,true);
    }


    public void removeWaypoints(TrackSegment toBeMoved){
        removeWaypoints(toBeMoved,true);
    }

    /**
     * Returns true if segment was removed
     *
     * @param toBeMoved
     * @param correctStart
     * @return
     */
    public boolean removeWaypoints(TrackSegment toBeMoved,boolean correctStart) {
        boolean res=false;
        for(WayPoint point: toBeMoved.clone().getPoints()){
            removeWaypoint(point,correctStart);
        }
        List<TrackSegment> emptyLists=new ArrayList<TrackSegment>();
        for(int i = 0; (i+1)< getSegments().size(); i++){
            if(getSegments().get(i).isEmpty()){
                emptyLists.add(getSegments().get(i));
            }
        }
        for(TrackSegment points:emptyLists){
            getSegments().remove(points);
            res=true;
        }
        return res;
    }

    /**
     * Starts new segment.
     */
    public void startNewSegment(){
        if(getCurrentSegment().isEmpty()){
            return;
        }
        m_waypoints.add(getTrackSegmentInstance());
    }
}
