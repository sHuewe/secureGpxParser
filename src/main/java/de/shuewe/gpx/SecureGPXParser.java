package de.shuewe.gpx;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Class for GPX parser.
 * Handles read/write operations and validation in GPXThread
 */
public class SecureGPXParser {

    //XML fields and attributes according to gpx version 1.1.
    private static final String ATTRIBUTE_LAT = "lat";
    private static final String ATTRIBUTE_LONG = "lon";
    private static final String TAG_CMT = "cmt";
    private static final String TAG_GPX = "gpx";
    private static final String TAG_METADATA = "metadata";
    private static final String TAG_NAME = "name";
    private static final String TAG_PDOP = "pdop";
    private static final String TAG_ROUTE = "rte";
    private static final String TAG_ROUTE_POINT = "rtept";
    private static final String TAG_TIME = "time";
    private static final String TAG_TRACK = "trk";
    private static final String TAG_TRACK_POINT = "trkpt";
    private static final String TAG_TRACK_SEG = "trkseg";
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TAG_ALTITUDE="ele";



    private GPXHandler m_handler = null;
    private GPXHandler m_default_handler = new DefaultGPXHandler(this);

    //Set of change listeners.
    private Set<GPXChangeListener> m_changeListener = new HashSet<GPXChangeListener>();
    //File name, can be null
    private String m_filename = null;
    //Flag indicates if initialization was ok
    private boolean m_init_ok = false;
    //Name of the file
    private String m_name = null;
    //List of single waypoints (without tracks)
    private List<WayPoint> m_points = new ArrayList<>();
    //Set of save listeners.
    private GPXChangeListener m_saveListener;

    private boolean m_isChanged=false;

    private GPXThread m_thread;
    //Map of available tracks.
    private Map<String, Track> m_tracks = new LinkedHashMap<String, Track>();
    //Flag indicates if file is valid
    private Boolean m_valid = null;
    //Validation result listener (set by isValid)
    private GPXValidationListener m_validationListener;

    /**
     * Default constructor.
     */
    public SecureGPXParser() {
        m_thread = GPXThread.getInstance();
    }

    public GPXHandler getHandler(){
        return m_handler != null ? m_handler : m_default_handler;
    }

    List<WayPoint> getPoints(){
        return m_points;
    }

    public void setHandler(GPXHandler handler){
        if(m_handler!=null){
            m_handler.writeTempData();
        }
        m_handler=handler;
        if(handler==null){
            return;
        }
        m_handler.init(this);
    }


    public boolean isChanged(){
        return m_isChanged;
    }

    /**
     * Add point from XmlSerializer
     *
     * @param xmlSerializer serializer
     * @param point         Waypoint to be added
     * @param tagName       Tag name to be used, should be Waypoint, Routepoint or Trackpoint
     * @throws IOException
     */
    private static void addPointToParser(XmlSerializer xmlSerializer, WayPoint point, String tagName) throws IOException {
        xmlSerializer.startTag("", tagName);
        xmlSerializer.attribute("", ATTRIBUTE_LAT, Double.toString(point.getLat()));
        xmlSerializer.attribute("", ATTRIBUTE_LONG, Double.toString(point.getLng()));
        if (point.getName() != null) {
            xmlSerializer.startTag("", TAG_NAME);
            xmlSerializer.text(point.getName());
            xmlSerializer.endTag("", TAG_NAME);
        }
        if (point.getHash() != null) {
            xmlSerializer.startTag("", TAG_CMT);
            xmlSerializer.text(point.getHash());
            xmlSerializer.endTag("", TAG_CMT);
        }
        if (point.getDate() != null) {
            xmlSerializer.startTag("", TAG_TIME);
            xmlSerializer.text(getDateString(point.getDate()));
            xmlSerializer.endTag("", TAG_TIME);
        }
        if(point.getAltitude() != null){
            xmlSerializer.startTag("",TAG_ALTITUDE);
            xmlSerializer.text(Double.toString(point.getAltitude()));
            xmlSerializer.endTag("",TAG_ALTITUDE);
        }
        xmlSerializer.startTag("", TAG_PDOP);
        xmlSerializer.text(String.valueOf(point.getAccuracy()));
        xmlSerializer.endTag("", TAG_PDOP);
        xmlSerializer.endTag("", tagName);
    }

    /**
     * Convertrs bytes to string.
     *
     * @param bytes to be parsed
     * @return String representation
     */
    static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Generates parser instance from given filepath.
     *
     * @param filePath to read gpx from
     * @return SecureGPXParser instance
     * @throws FileNotFoundException exception
     * @throws IOException           exception
     */
    public static SecureGPXParser fromFile(String filePath) throws FileNotFoundException, IOException {
        SecureGPXParser res = getNewInstance();
        res.init(new FileInputStream(new File(filePath)));
        return res;
    }

    /**
     * Generates parser instance from Inputstream.
     *
     * @param input InputStream
     * @return SecureGPXParser instance
     */
    public static SecureGPXParser fromInputStream(InputStream input) {
        SecureGPXParser res = new SecureGPXParser();
        res.init(input);
        return res;
    }

    /**
     * Get date from UTC string.
     *
     * @param dateStr utc string
     * @return Date (UTC)
     */
    protected static Date getDateFromString(String dateStr) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        try {
            return df.parse(dateStr);
        } catch (ParseException e) {
            try {
                return df2.parse(dateStr);
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Get date string suitable for gpx.
     *
     * @param date Date to be parsed
     * @return String
     */
    public static String getDateString(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        return df.format(date);
    }

    protected static SecureGPXParser getNewInstance() {
        return new SecureGPXParser();
    }

    /**
     * Adds/Registers a ChangeListener
     *
     * @param listener Listener to be added
     */
    public void addChangeListener(GPXChangeListener listener) {
        m_changeListener.add(listener);
    }



    public void setOnSaveListener(GPXChangeListener listener) {
        m_saveListener=listener;
    }



    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SecureGPXParser)) {
            return false;
        }
        SecureGPXParser parser = (SecureGPXParser) o;
        if (parser.getName() == null) {
            if (getName() != null) {
                return false;
            }
        } else {
            if (!parser.getName().equals(getName())) {
                return false;
            }
        }

        if (!(parser.getHandler().getLocations().size() == getHandler().getLocations().size())) {
            return false;
        }
        List<Track> otherTracks = parser.getSortedTracks();
        List<Track> ownTracks = getSortedTracks();
        if (otherTracks.size() != ownTracks.size()) {
            return false;
        }
        for (int i = 0; i < ownTracks.size(); i++) {
            if (otherTracks.get(i).getSize() != ownTracks.get(i).getSize()) {
                return false;
            }
        }
        for (int i = 0; i < getHandler().getLocations().size(); i++) {
            WayPoint p0 = getHandler().getLocations().get(i);
            WayPoint p1 = parser.getHandler().getLocations().get(i);
            if (!p0.equals(p1)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the filename
     *
     * @return filename, null if not set
     */
    public String getFilename() {
        return m_filename;
    }

    /**
     * Sets the filename
     *
     * @param name to be set
     */
    public void setFilename(String name) {
        m_filename = name;
    }



    /**
     * Get locations from given track name.
     *
     * @param trackName name of track to get Waypoints for
     * @return List of Waypoint
     */
    public List<? extends WayPoint> getLocationsFromTrack(String trackName) {
        List<WayPoint> res = new ArrayList<WayPoint>();
        for (TrackSegment points : m_tracks.get(trackName).getSegments()) {
            res.addAll(points.getPoints());
        }
        return res;
    }

    /**
     * Gets the name of the gpx file
     *
     * @return filename
     */
    public String getName() {
        return m_name;
    }

    /**
     * Sets the filename.
     *
     * @param name to be set
     */
    public void setName(String name) {
        m_name = name;
    }

    /**
     * Gets sorted List of tracks (by date)
     *
     * @return sorted track List
     */
    public List<Track> getSortedTracks() {
        List<Track> res = new ArrayList<Track>(m_tracks.values());
        Collections.sort(res);
        return res;
    }

    /**
     * Get the tracks.
     *
     * @return Map<String, Track>
     */
    public Map<String, Track> getTracks() {
        return m_tracks;
    }

    /**
     * Checks if gpx parser was initialized successfully.
     *
     * @return boolean
     */
    public boolean isInit() {
        return m_init_ok;
    }



    /**
     * Notifies registered listeners in case of changed values
     */
    public void notifyListener() {
        for (GPXChangeListener listener : m_changeListener) {
            listener.handleChangedData(this);
        }
    }

    /**
     * Notifies registered listeners in case of changed values
     */
    public void notifySaveListener() {
        if(m_saveListener!=null){
            m_saveListener.handleChangedData(this);
            m_saveListener=null;
        }
    }

    /**
     * Unregisters given GPXChangeListener
     *
     * @param listener to be removed
     */
    public void removeChangeListener(GPXChangeListener listener) {
        m_changeListener.remove(listener);
    }





    /**
     * Unregisters given GPXChangeListener
     *
     * @param listener to be removed
     */
    public void removeSaveListener(GPXChangeListener listener) {
        if(m_saveListener.equals(listener)){
            m_saveListener=null;
        }
    }


    /**
     * Reset the parser
     */
    public void reset() {
        m_points.clear();
        m_tracks.clear();
        m_valid = null;
        getHandler().clear();
        m_name = null;
    }

    /**
     * Saves the parser. Needs to have filename set.
     */
    void save() {
        if (m_filename == null) {
            throw new IllegalArgumentException("No filename set! Call setFilename before!");
        }
        final String fileName = m_filename;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(GPXThread.TAG_THREAD, String.format("Write %s to %s", getName(), fileName));
                    write(new FileOutputStream(new File(fileName)));
                    m_isChanged=false;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
        addRunnableToBackgroundThread(GPXThread.ACTION.SAVE, runnable);
    }

    /**
     * Returns a WayPoint instance for parameter
     *
     * @param name     of waypoint
     * @param lat      latitude
     * @param lng      longitude
     * @param date     date
     * @param accuracy accuracy
     * @return WayPoint instance
     */
    protected WayPoint getWayPointInstance(String name, double lat, double lng, Date date, double accuracy) {
        return new WayPoint(name, lat, lng, date, accuracy);
    }

    /**
     * Returns a WayPoint instance for parameter
     *
     * @param lat      latitude
     * @param lng      longitude
     * @param date     date
     * @param accuracy accuracy
     * @return WayPoint instance
     */
    protected WayPoint getWayPointInstance(double lat, double lng, Date date, double accuracy) {
        return new WayPoint(lat, lng, date, accuracy);
    }

    protected Track getTrackInstance(String trackName){
        return new Track(trackName);
    }

    /**
     * Initializes the parser from given InputStream
     *
     * @param inStream inputstream (e.g. from file)
     */
    protected void init(InputStream inStream) {
        reset();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inStream, null);
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, TAG_GPX);

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (parser.getName().equals(TAG_METADATA)) {

                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        String name = parser.getName();
                        // Starts by looking for the entry tag
                        if (name != null && name.equals(TAG_NAME)) {
                            m_name = readText(parser);
                        } else {
                            skip(parser);
                        }
                    }
                } else if (parser.getName().equals(TAG_WAYPOINT)) {
                    m_points.add(readWayPoint(parser));
                } else if (parser.getName().equals(TAG_ROUTE)) {
                    List<WayPoint> routePoints = new ArrayList<WayPoint>();
                    String routeName = "";
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        String name = parser.getName();
                        // Starts by looking for the entry tag
                        if (name.equals(TAG_ROUTE_POINT)) {
                            routePoints.add(readWayPoint(parser, TAG_ROUTE_POINT));
                        } else if (name.equals(TAG_NAME)) {
                            routeName = readText(parser);
                        } else {
                            skip(parser);
                        }

                    }

                    if (!m_tracks.containsKey(routeName)) {
                        m_tracks.put(routeName,getTrackInstance(routeName));
                    }
                    for (WayPoint p : routePoints) {
                        p.setParentTrack(m_tracks.get(routeName));
                    }
                    m_tracks.get(routeName).addPoints(routePoints);
                    m_tracks.get(routeName).startNewSegment();
                } else if (parser.getName().equals(TAG_TRACK)) {

                    String trackName = "";
                    List<List<WayPoint>> wayPointList = new ArrayList<List<WayPoint>>();
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        String name = parser.getName();
                        if (name.equals(TAG_NAME)) {
                            trackName = readText(parser);
                        } else if (name.equals(TAG_TRACK_SEG)) {
                            List<WayPoint> trackPoints = new ArrayList<WayPoint>();
                            while (parser.next() != XmlPullParser.END_TAG) {
                                if (parser.getEventType() != XmlPullParser.START_TAG) {
                                    continue;
                                }
                                if (parser.getName().equals(TAG_TRACK_POINT)) {
                                    trackPoints.add(readWayPoint(parser, TAG_TRACK_POINT));
                                } else {
                                    skip(parser);
                                }
                            }
                            wayPointList.add(trackPoints);
                        } else {
                            skip(parser);
                        }
                    }
                    int size = 0;
                    for (List<WayPoint> points : wayPointList) {
                        size += points.size();
                    }
                    if (size > 0) {
                        if (!m_tracks.containsKey(trackName)) {
                            m_tracks.put(trackName, getTrackInstance(trackName));
                        }
                        for (List<WayPoint> points : wayPointList) {
                            for (WayPoint point : points) {
                                point.setParentTrack(m_tracks.get(trackName));
                            }
                        }
                        Track track = m_tracks.get(trackName);
                        for (List<WayPoint> points : wayPointList) {
                            track.addPoints(points);
                            track.startNewSegment();
                        }
                    }
                } else {
                    skip(parser);
                }
            }
            Collections.sort(m_points);
            m_init_ok = true;
            m_isChanged = false;
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            m_init_ok = false;
        }
    }

    void notifyListener(GPXThread.ACTION action) {
        if (action.equals(GPXThread.ACTION.INIT_AND_CHANGE)) {
            notifyListener();
            notifySaveListener();
            return;
        }
        if (action.equals(GPXThread.ACTION.CHANGE_DATA)) {
            notifyListener();
            return;
        }
        if (action.equals(GPXThread.ACTION.SAVE)) {
            notifySaveListener();
            return;
        }
    }

    /**
     * Write the data to a given OutputStream.
     *
     * @param stream to be written to
     */
    void write(OutputStream stream) {
        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            xmlSerializer.setOutput(writer);
            // start DOCUMENT
            xmlSerializer.startDocument("UTF-8", true);

            xmlSerializer.startTag("", TAG_GPX);
            xmlSerializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
            xmlSerializer.attribute("", "version", "1.1");
            xmlSerializer.attribute("", "creator", "https://www.shuewe.de");
            if (m_name != null) {
                xmlSerializer.startTag("", TAG_METADATA);
                xmlSerializer.startTag("", TAG_NAME);
                xmlSerializer.text(m_name);
                xmlSerializer.endTag("", TAG_NAME);
                xmlSerializer.endTag("", TAG_METADATA);
            }
            for (WayPoint point : m_points) {
                addPointToParser(xmlSerializer, point, TAG_WAYPOINT);
            }
            for (String trackName : m_tracks.keySet()) {
                Track track = m_tracks.get(trackName);
                xmlSerializer.startTag("", TAG_TRACK);
                xmlSerializer.startTag("", TAG_NAME);
                xmlSerializer.text(trackName);
                xmlSerializer.endTag("", TAG_NAME);

                for (TrackSegment segmentPoints : track.getSegments()) {
                    if (segmentPoints.isEmpty()) {
                        continue;
                    }
                    xmlSerializer.startTag("", TAG_TRACK_SEG);
                    for (WayPoint trackPoint : segmentPoints.getPoints()) {
                        addPointToParser(xmlSerializer, trackPoint, TAG_TRACK_POINT);
                    }
                    xmlSerializer.endTag("", TAG_TRACK_SEG);
                }
                xmlSerializer.endTag("", TAG_TRACK);
            }
            xmlSerializer.endTag("", TAG_GPX);


            // end DOCUMENT
            xmlSerializer.endDocument();
            stream.write(writer.toString().getBytes());
        } catch (IOException e) {

        }
    }

    private void addRunnableToBackgroundThread(GPXThread.ACTION action, Runnable runnable) {
        Log.d(GPXThread.TAG_THREAD, "Add runnable to background thread");
        m_thread.m_handler.sendMessage(GPXThread.getPreparedMessage(action, this, runnable));
    }


    /**
     * Read date from XML.
     *
     * @param parser to read from
     * @return Date
     * @throws XmlPullParserException
     * @throws IOException
     */
    private Date readDate(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, TAG_TIME);
        String dateString = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, TAG_TIME);
        return getDateFromString(dateString);
    }

    /**
     * Read text from XML.
     *
     * @param parser to read from
     * @return String
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * Read WayPoint from XML
     *
     * @param parser to read from
     * @return WayPoint instance
     * @throws XmlPullParserException
     * @throws IOException
     */
    private WayPoint readWayPoint(XmlPullParser parser) throws XmlPullParserException, IOException {
        return readWayPoint(parser, TAG_WAYPOINT);
    }

    /**
     * Read WayPoint from XML
     *
     * @param parser   to read from
     * @param tag_name Tag to read (from Waypoint, Track or Route)
     * @return WayPoint instance
     * @throws XmlPullParserException
     * @throws IOException
     */
    private WayPoint readWayPoint(XmlPullParser parser, String tag_name) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, tag_name);
        //This is a waypoint
        String lat = "0";
        String lng = "0";
        String pointName = null;
        Date date = null;
        String accuracy = "20";
        String hashCmt = null;
        String altitude=null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attrName = parser.getAttributeName(i);
            if (attrName.equals(ATTRIBUTE_LAT)) {
                lat = parser.getAttributeValue(i);
            }
            if (attrName.equals(ATTRIBUTE_LONG)) {
                lng = parser.getAttributeValue(i);
            }
        }
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals(TAG_TIME)) {
                date = (readDate(parser));
            } else if (name.equals(TAG_PDOP)) {
                accuracy = readText(parser);
            } else if (name.equals(TAG_NAME)) {
                pointName = readText(parser);
            } else if (name.equals(TAG_CMT)) {
                hashCmt = readText(parser);
            } else if (name.equals(TAG_ALTITUDE)){
                altitude=readText(parser);
            } else {
                skip(parser);
            }
        }
        parser.require(XmlPullParser.END_TAG, null, tag_name);
        WayPoint res = getWayPointInstance(pointName, Double.parseDouble(lat), Double.parseDouble(lng), date, Double.parseDouble(accuracy));
        if(altitude!=null){
            res.setAltitude(Double.parseDouble(altitude));
        }
        if (hashCmt != null) {
            res.setHash(hashCmt);
        }
        return res;
    }

    /**
     * Skip tag (including child tags)
     *
     * @param parser to handle
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public void markChanged(){
        m_isChanged=true;
    }

    private void skipText(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.getEventType() == XmlPullParser.TEXT){
            parser.next();
        }
    }






    /**
     * Interface for handling change events.
     */
    public interface GPXChangeListener {
        /**
         * Passes changed parser to GPXChangeListener (called from SecureGPXParser)
         */
        void handleChangedData(SecureGPXParser parser);
    }

    public interface GPXOnInitListener {

        void onInitReady(SecureGPXParser parser);
    }

    public interface GPXValidationListener {

        void handleValidation(boolean valid);
    }

    public List<? extends GPXElement> getTrackSegmentsAndSinglePlaces(){
        List<GPXElement> res = new ArrayList<GPXElement>();
        res.addAll(m_points);
        for(Track track:m_tracks.values()){
            for(TrackSegment segment:track.getNumberedSegments()){
                if(!segment.isEmpty()){
                    res.add(segment);
                }
            }
        }
        Collections.sort(res,Collections.reverseOrder());
        return res;
    }

}