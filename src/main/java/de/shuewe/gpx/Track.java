package de.shuewe.gpx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class which represents Tracks in GPX.
 */
public class Track implements Comparable<Track> {

    //Name of track
    private String m_name;

    //List of List of Waypoints. A single list represents a track-segment.
    private List<List<WayPoint>> m_waypoints = new ArrayList<List<WayPoint>>();

    /**
     * Public constructor.
     *
     * @param name of track
     */
    public Track(String name) {
        m_name=name;
        m_waypoints.add(new ArrayList<WayPoint>());

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

    /**
     * Adds a Set of points to track
     *
     * @param points to be added
     */
    public void addPoints(List<WayPoint> points) {
        if(points.isEmpty()){
            return;
        }
        m_waypoints.get(m_waypoints.size()-1).addAll(points);
        WayPoint oldFirst = m_waypoints.get(m_waypoints.size()-1).get(0);
        Collections.sort(m_waypoints.get(m_waypoints.size()-1));
        if(!oldFirst.equals(m_waypoints.get(m_waypoints.size()-1).get(0))){
            for(WayPoint point:m_waypoints.get(m_waypoints.size()-1)){
                point.setIsStartPoint(false);
            }
        }
        m_waypoints.get(m_waypoints.size()-1).get(0).setIsStartPoint(true);
    }

    public List<? extends WayPoint> getAllPoints() {
        List<WayPoint> res = new ArrayList<>();
        for(List<WayPoint> segment:getPoints()){
            res.addAll(segment);
        }
        return res;
    }

    /**
     * Finds number of matching segment to given List of points (=segment)
     *
     * @param points to match a segment for
     * @return number of segment, -1 if no suitable segment is available
     */
    private int findMatchingSegment(List<WayPoint> points){

        for(int i=0;i<getPoints().size();i++){
            List<WayPoint> segmentPoints = getPoints().get(i);
            if(segmentPoints.isEmpty()){
                continue;
            }
            Date minDate=segmentPoints.get(0).getDate();
            Date maxDate=segmentPoints.get(segmentPoints.size()-1).getDate();
            if(WayPoint.isPointInRange(points.get(0),minDate,maxDate) || WayPoint.isPointInRange(points.get(points.size()-1),minDate,maxDate)){
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
    private int findNewTrackPosition(List<WayPoint> points){
        for(int i=0;i<(getPoints().size()-1);i++){
            List<WayPoint> prevSegment = getPoints().get(i);
            List<WayPoint> nextSegment = getPoints().get(i+1);
            if(nextSegment.isEmpty()){
                continue;
            }
            Date minDate=prevSegment.get(prevSegment.size()-1).getDate();
            Date maxDate=nextSegment.get(0).getDate();
            if(WayPoint.isPointInRange(points.get(0),minDate,maxDate)){
                return i+1;
            }
        }
        if(getPoints().get(0).get(0).compareTo(points.get(0))>0){
            return 0;
        }
        return -1; //No matching segment found, just add it at end of list
    }

    public void addPointsToSegments(List<List<WayPoint>> newSegments) {
        //Map<Integer,List<WayPoint>>
        for(List<WayPoint> points:newSegments){
            if(points.isEmpty()){
                continue;
            }
            //Reset is start point, will be corrected if needed later
            points.get(0).setIsStartPoint(false);
            int segmentIndex = findMatchingSegment(points);
            if(segmentIndex!=-1){
                //Segment is matching to existing one
                getPoints().get(segmentIndex).addAll(points);
                Collections.sort(getPoints().get(segmentIndex));
            }else{
                int newPos=findNewTrackPosition(points);
                if(newPos!=-1) {
                    m_waypoints.add(newPos, points);
                }else{
                    startNewSegment();
                    addPoints(points);
                }
            }
        }
    }

    @Override
    public int compareTo(Track track) {
        if (track.getPoints().get(0).isEmpty()) {
            return 1;
        }
        if (getPoints().get(0).isEmpty()) {
            return 0;
        }
        return getPoints().get(0).get(0).compareTo(track.getPoints().get(0).get(0));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Track)) {
            return false;
        }
        Track otherTrack = (Track) o;
        if (otherTrack.getPoints().size() != getPoints().size()) {
            return false;
        }
        for (int i = 0; i < getPoints().size(); i++) {
            if(getPoints().get(i).size() != otherTrack.getPoints().size()){
                return false;
            }
            for(int j=0;j< getPoints().get(i).size();j++){
                if (!getPoints().get(i).get(j).equals(otherTrack.getPoints().get(i).get(j))) {
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
        WayPoint oldPoint = null;
        for(List<WayPoint> points:m_waypoints){
            oldPoint=null;
            for(WayPoint point:points) {
                if (oldPoint != null) {
                    res += oldPoint.calculateDistanceInKilometer(point);
                }
                oldPoint = point;
            }
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
    public List<List<WayPoint>> getPoints() {
        return m_waypoints;
    }

    /**
     * Gets the total size of track (including all segments)
     *
     * @return int
     */
    public int getSize(){
        int res=0;
        for(List<WayPoint> points:m_waypoints){
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
        for(List<WayPoint> points:getPoints()){
            removed = removed | points.remove(point);
            if(removed && correctStart){
                if(!points.isEmpty()){
                    points.get(0).setIsStartPoint(true);
                }
                break;
            }
        }
        return removed;
    }

    public boolean removeWaypoint(WayPoint point){
        return removeWaypoint(point,true);
    }


    public void removeWaypoints(List<WayPoint> toBeMoved){
        removeWaypoints(toBeMoved,true);
    }

    public void removeWaypoints(List<WayPoint> toBeMoved,boolean correctStart) {
        for(WayPoint point:new ArrayList<>(toBeMoved)){
            removeWaypoint(point,correctStart);
        }
        List<List<WayPoint>> emptyLists=new ArrayList<List<WayPoint>>();
        for(int i=0;(i+1)<getPoints().size();i++){
            if(getPoints().get(i).isEmpty()){
                emptyLists.add(getPoints().get(i));
            }
        }
        for(List<WayPoint> points:emptyLists){
            getPoints().remove(points);
        }
    }

    /**
     * Starts new segment.
     */
    public void startNewSegment(){
        if(m_waypoints.get(m_waypoints.size()-1).isEmpty()){
            return;
        }
        m_waypoints.add(new ArrayList<WayPoint>());
    }
}
