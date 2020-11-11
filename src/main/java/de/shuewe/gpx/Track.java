package de.shuewe.gpx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        m_waypoints.get(m_waypoints.size()-1).addAll(points);
        Collections.sort(m_waypoints.get(m_waypoints.size()-1));
    }

    public void addPointsToSegments(List<WayPoint> toBeMoved) {
        //TODO implement
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
    public boolean removeWaypoint(WayPoint point) {
        boolean removed=false;
        for(List<WayPoint> points:getPoints()){
            removed = removed | points.remove(point);
            if(removed){
                break;
            }
        }
        return removed;
    }


    public void removeWaypoints(List<WayPoint> toBeMoved) {
        for(WayPoint point:toBeMoved){
            removeWaypoint(point);
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
