package de.shuewe.gpx;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class GPXHandler {

    static final String LOG_TAG="GPXHandler";
    public GPXThread m_thread;
    protected SecureGPXParser m_parser;
    //List of sorted Waypoints (sorted by date, if date == null, points are on end of list).
    private List<WayPoint> m_sortedPoints = null;
    private Boolean m_valid;

    protected abstract void writeTempData();

    public abstract void processWaypoint(String name, double lat, double lng, double accuracy, double alt);

    public abstract void processTrackpoint(String trackName, double lat, double lng, double accuracy, double alt);

    public void save(){
        writeTempData();
        m_parser.save();
    }

    public GPXHandler(){
        m_thread = GPXThread.getInstance();
    }

    /**
     * Adds a trackpoint
     *
     * @param lat      latitude
     * @param lng      longitude
     * @param accuracy accuracy
     * @return WayPoint
     */
    protected void addTrackPoint(double lat, double lng, double accuracy,Double alt) {
        addTrackPoint(null, lat, lng, accuracy,alt);
    }

    /**
     * Adds a trackpoint
     *
     * @param name     Name of point
     * @param lat      latitude
     * @param lng      longitude
     * @param accuracy accuracy
     * @return WayPoint
     */
    protected void addTrackPoint(String name, double lat, double lng, double accuracy,Double alt) {
        addTrackPoint(null, name, lat, lng, accuracy,alt);
    }

    /**
     * Adds a trackpoint
     *
     * @param parentName Parent name != null indicates, that point belongs to track
     * @param name       Name of point
     * @param lat        latitude
     * @param lng        longitude
     * @param accuracy   accuracy
     * @return WayPoint
     */
    protected void addTrackPoint(String parentName, String name, double lat, double lng, double accuracy,Double alt) {
        final Date date = new Date();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                WayPoint res;
                if (name == null) {
                    res = m_parser.getWayPointInstance(lat, lng, date, accuracy);
                } else {
                    res = m_parser.getWayPointInstance(name, lat, lng, date, accuracy);
                }
                res.setAltitude(alt);
                String prevHash = null;
                if (!getLocations().isEmpty()) {
                    prevHash = getLocations().get(getLocations().size() - 1).getHash();
                }
                res.generateHash(prevHash);
                if (parentName == null) {
                    m_parser.getPoints().add(res);
                } else {
                    if (!m_parser.getTracks().containsKey(parentName)) {
                        m_parser.getTracks().put(parentName, m_parser.getTrackInstance(parentName));
                    }
                    res.setParentTrack(m_parser.getTracks().get(parentName));
                    m_parser.getTracks().get(parentName).addPoints(Collections.singletonList(res));
                }
                m_valid = null;
                if (!m_sortedPoints.isEmpty() && m_sortedPoints.get(m_sortedPoints.size() - 1).getDate() == null) {
                    m_sortedPoints.clear();
                    getLocations();
                } else {
                    m_sortedPoints.add(res);
                }
                m_parser.markChanged();
            }
        };
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_DATA, runnable);
    }

    public void changeTrackFromWaypoint(WayPoint point, String newTrackname) {

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                List<TrackSegment> toBeMoved = new ArrayList<TrackSegment>();
                Track sourceTrack = m_parser.getTracks().get(point.get_parentName());
                int i = 0;
                int iPos = -1;
                List<TrackSegment> pointList = sourceTrack.getNumberedSegments();
                for (TrackSegment points : pointList) {
                    int pos = points.indexOf(point);
                    i++;
                    if (pos == -1) {
                        //Not in list
                        continue;
                    }
                    iPos = i; //+1 -> start with next segment
                    toBeMoved.add(points.subList(pos, points.size()).clone());
                }
                if (pointList.size() >= iPos && iPos != -1) {
                    for (int j = iPos; j < pointList.size(); j++) {
                        toBeMoved.add(pointList.get(j).clone());
                    }
                }
                if(toBeMoved.isEmpty()){
                    return;
                }
                Log.i(LOG_TAG,"Found "+toBeMoved.size()+" Segments to move");
                for(TrackSegment segment:toBeMoved){
                    Log.i(LOG_TAG,"Segment size: "+segment.size());
                }
                int firstSegmentToBeRemoved=-1;
                boolean hasRemovedSegment=false;
                if(point.isStart()){
                    Log.i(LOG_TAG,"Remove segment with size: "+toBeMoved.get(0).size());
                    removeSegmentPrivate(toBeMoved.get(0));
                    firstSegmentToBeRemoved=toBeMoved.get(0).getSegmentNumber();

                }else{
                    Log.i(LOG_TAG,"Remove waypoints with size: "+toBeMoved.get(0).size());
                    hasRemovedSegment=sourceTrack.removeWaypoints(toBeMoved.get(0), false);

                }
                if(toBeMoved.size()>1){
                    if(firstSegmentToBeRemoved==-1){
                        firstSegmentToBeRemoved=toBeMoved.get(1).getSegmentNumber();
                        if(hasRemovedSegment){
                            firstSegmentToBeRemoved+=-1;
                        }
                    }
                    for (TrackSegment segment : toBeMoved.subList(1,toBeMoved.size())) {
                        Log.i(LOG_TAG,"Remove segment with size: "+segment.size());
                        segment.setSegmentNumber(firstSegmentToBeRemoved);
                        removeSegmentPrivate(segment);

                    }
                }
                if (sourceTrack.getSize() == 0) {
                    Log.i(LOG_TAG,"Track is empty -> Remove track");
                    m_parser.getTracks().remove(sourceTrack.getName());
                }else{
                    Log.i(LOG_TAG,"Track is not empty: "+sourceTrack.getSize()+" in "+sourceTrack.getSegments().size()+" segments");
                }
                if (!m_parser.getTracks().containsKey(newTrackname)) {
                    m_parser.getTracks().put(newTrackname, m_parser.getTrackInstance(newTrackname));
                }
                for (TrackSegment points : toBeMoved) {
                    for (WayPoint p : points.getPoints()) {
                        p.setParentTrack(m_parser.getTracks().get(newTrackname));
                    }
                }
                if (m_parser.getTracks().get(newTrackname).getSize() == 0) {
                    Log.i(LOG_TAG,"Add segments to empty track");
                    for (TrackSegment points : toBeMoved) {
                        m_parser.getTracks().get(newTrackname).addPoints(points);
                        m_parser.getTracks().get(newTrackname).startNewSegment();
                    }
                } else {
                    Log.i(LOG_TAG,"Add segments to existing track");
                    m_parser.getTracks().get(newTrackname).addSegments(toBeMoved);
                }
                Log.i(LOG_TAG,"Moving Waypoints finished!");
                m_parser.markChanged();
            }
        };
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_DATA, runnable);
        //notifyListener();
    }

    public void clear(){
        if (m_sortedPoints != null) {
            m_sortedPoints.clear();
        }
        m_valid=null;
    }

    /**
     * Gets sorted List of all locations (from single Waypoints and tracks)
     *
     * @return sorted list of WayPoint
     */
    public List<? extends WayPoint> getLocations() {
        if (m_sortedPoints == null || m_sortedPoints.isEmpty()) {
            if (m_sortedPoints == null) {
                m_sortedPoints = new ArrayList<WayPoint>();
            }
            m_sortedPoints.addAll(m_parser.getPoints());
            for (String trackName : m_parser.getTracks().keySet()) {
                for (TrackSegment segments : m_parser.getTracks().get(trackName).getSegments()) {
                    m_sortedPoints.addAll(segments.getPoints());
                }
            }
            Collections.sort(m_sortedPoints);
        }
        return m_sortedPoints;
    }

    public void init(SecureGPXParser parser){

        m_parser = parser;
    }

    /**
     * Checks if parser is validateable
     *
     * @return boolean
     */
    public boolean isValidateable() {
        if (getLocations().isEmpty()) {
            return false;
        }
        return getLocations().get(0).getHash() != null;
    }

    /**
     * Removes the given WayPoint (either from points or from tracks)
     *
     * @param point to be removed
     */
    public void removeLocation(final WayPoint point) {
        final boolean wasValid = isValid(); //May create own runnable in future (which would be executed before remove
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean removed = false;
                if (point.get_parentName() == null || point.get_parentName().isEmpty()) {
                    //Single waypoint
                    removed = m_parser.getPoints().remove(point);
                } else {
                    //Track
                    removed = m_parser.getTracks().get(point.get_parentName()).removeWaypoint(point);
                    if (m_parser.getTracks().get(point.get_parentName()).getSize() == 0) {
                        m_parser.getTracks().remove(point.get_parentName());
                    }
                }
                if (removed) {
                    m_sortedPoints.clear();
                    m_valid = null;
                    getLocations();
                    if (wasValid && m_sortedPoints.size() > 0) {
                        validate(true);
                    }
                }
                m_parser.markChanged();
            }
        };
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_DATA, runnable);
    }

    public void removeSegment(TrackSegment segment){
        final boolean wasValid = isValid();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (removeSegmentPrivate(segment)) {
                    m_sortedPoints.clear();
                    m_valid = null;
                    getLocations();
                    if (wasValid && m_sortedPoints.size() > 0) {
                        validate(true);
                    }
                }
                m_parser.markChanged();
            }
        };
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_DATA, runnable);
    }

    public void removeTrack(Track track) {
        String trackName=track.getName();
        final boolean wasValid=isValid();
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_DATA, new Runnable() {
            @Override
            public void run() {

                m_parser.getTracks().remove(trackName);
                m_sortedPoints.clear();
                getLocations();
                if(wasValid){
                    validate(true);
                }
                m_parser.markChanged();
            }
        });

    }

    public void renameTrack(Track track, String newName) {
        m_parser.getTracks().remove(track.getName());
        track.setName(newName);
        m_parser.getTracks().put(newName,track);
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_DATA, new Runnable() {
            @Override
            public void run() {
                m_parser.markChanged();
            }
        });
    }

    /**
     * Starts a reqeust for validation.
     *
     * @param validationListener to handle validation result
     */
    public void requestValidation(SecureGPXParser.GPXValidationListener validationListener) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                validationListener.handleValidation(isValid());
            }
        };
        addRunnableToBackgroundThread(GPXThread.ACTION.CHANGE_VALIDATION, run);
    }

    protected void addRunnableToBackgroundThread(GPXThread.ACTION action, Runnable runnable) {
        Log.d(GPXThread.TAG_THREAD, "Add runnable to background thread");
        m_thread.m_handler.sendMessage(GPXThread.getPreparedMessage(action, getParser(), runnable));
    }

    protected SecureGPXParser getParser(){
        return m_parser;
    }

    /**
     * Checks if data of parser are valid.
     *
     * @return boolean
     */
    private boolean isValid() {
        if (m_valid == null) {
            m_valid = validate();
        }
        return m_valid.booleanValue();
    }

    private boolean removeSegmentPrivate(TrackSegment segment){
        if(segment.isEmpty()){
            return false;
        }
        boolean removed=false;
        String initName=segment.getFirst().get_parentName();
        Track track = m_parser.getTracks().get(initName);
        removed=track.removeSegment(segment.getSegmentNumber());
        if(track.getSegments().isEmpty()) {
            m_parser.getTracks().remove(initName);
        }
        Log.i(LOG_TAG,"Segment "+segment.getSegmentNumber()+" removed. Remaining segments: "+track.getSegments().size());
        return removed;
    }

    /**
     * Validates the data, and repairs broken blockchain if needed
     *
     * @param repair flag which indicates, if blockchain should be repaired
     * @return
     */
    private boolean validate(boolean repair) {
        String prevHash = null;
        for (WayPoint wayP : getLocations()) {
            prevHash = wayP.generateHash(prevHash, repair);
        }
        return getLocations().get(getLocations().size() - 1).getHash() == null ? false : prevHash.equals(getLocations().get(getLocations().size() - 1).getHash());
    }

    /**
     * Validate the data.
     *
     * @return boolean
     */
    private boolean validate() {
        return validate(false);
    }



}
