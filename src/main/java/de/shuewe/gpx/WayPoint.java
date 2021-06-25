package de.shuewe.gpx;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.Serializable;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Class representing a single WayPoint. Can be single WayPoint, Track-Point or Route-Point in gpx.
 */
public class WayPoint extends GPXElement implements Serializable {

    //Radius of earth
    public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;

    //Secret key used for hash generating. Should be replaced in production by a secret key, or the corresponding method should be overwritten
    private static final String HASH_SECRET_KEY="";

    //Coordinates
    protected double m_lat, m_lng;

    //The altitude
    protected Double m_altitude;

    //Accuracy
    private double m_accuracy;

    private boolean m_isStart=false;

    //Date
    private Date m_date;

    //Blockchain Hash value
    private String m_hash;

    //Name of point
    private String m_name = null;

    //Name of parent track
    private Track m_parentTrack = null;

    /**
     * public constructor
     *
     * @param name name of point
     * @param lat latitude
     * @param lng longitude
     * @param date date
     * @param accuracy accuracy
     */
    public WayPoint(String name, double lat, double lng, Date date, double accuracy) {
        m_name = name;
        m_date = date;
        m_accuracy = accuracy;
        m_lat = lat;
        m_lng = lng;
    }

    public WayPoint withAltitude(Double altitude){
       setAltitude(altitude);
       return this;
    }

    public void setAltitude(Double altitude){
        m_altitude=altitude;
    }

    public Double getAltitude(){
        return m_altitude;
    }

    public void setIsStartPoint(boolean isStart){
        m_isStart=isStart;
    }

    public boolean isStart(){
        return m_isStart;
    }

    /**
     * public constructor
     *
     * @param lat latitude
     * @param lng longitude
     * @param date date
     * @param accuracy accuracy
     */
    public WayPoint(double lat, double lng, Date date, double accuracy) {

        this(null, lat, lng, date, accuracy);
    }

    /**
     * Returns the distance to given WayPoint
     *
     * @param point to calculate distance to
     * @return distance in km
     */
    public double calculateDistanceInKilometer(WayPoint point) {

        double latDistance = Math.toRadians(m_lat - point.m_lat);
        double lngDistance = Math.toRadians(m_lng - point.m_lng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(m_lat)) * Math.cos(Math.toRadians(point.m_lat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return AVERAGE_RADIUS_OF_EARTH_KM * c;
    }



    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WayPoint)) {
            return false;
        }
        WayPoint p1 = (WayPoint) o;
        if (getName() == null) {
            if (p1.getName() != null) {
                return false;
            }
        } else {
            if (!getName().equals(p1.getName())) {
                return false;
            }
        }
        if ((getDate() == null && p1.getDate() != null) || (getDate() != null && p1.getDate() == null)) {
            return false;
        }
        if (!(getDate() == null && p1.getDate() == null)) {
            if (!SecureGPXParser.getDateString(getDate()).equals(SecureGPXParser.getDateString(p1.getDate()))) {
                return false;
            }
        }
        return getLat() == p1.getLat() && getLng() == p1.getLng() && getAccuracy() == p1.getAccuracy();
    }

    /**
     * Returns the accuracy
     *
     * @return double
     */
    public double getAccuracy() {
        return m_accuracy;
    }

    /**
     * Returns the date
     *
     * @return date
     */
    public Date getDate() {
        return m_date;
    }

    /**
     * Returns the latitude
     *
     * @return double
     */
    public double getLat() {
        return m_lat;
    }

    /**
     * Returns the longitude
     *
     * @return double
     */
    public double getLng() {
        return m_lng;
    }

    /**
     * Returns the name
     *
     * @return String
     */
    public String getName() {
        return m_name;
    }

    /**
     * Sets the name
     *
     * @param name to be set
     */
    public void setName(String name) {
        m_name = name;
    }

    /**
     * Gets the parent name
     *
     * @return String
     */
    public String get_parentName() {
        return m_parentTrack == null ? null : m_parentTrack.getName();
    }

    /**
     * Sets the parent name via track
     *
     * @param track to be set as parent track
     */
    public void setParentTrack(Track track) {
        m_parentTrack = track;
    }

    /**
     * Returns the secret hash key. Should be changed / overwritten in production to return a secret (but constant) String
     *
     * @return secret String
     */
    protected String getHashSecretKey(){
        return HASH_SECRET_KEY;
    }

    @Override
    protected Date getSortDate() {
        return m_date != null ? m_date: new Date(0l);
    }

    @Override
    public View getListViewRow(Context context, LayoutInflater inflater, View convertView, ViewGroup viewGroup) {
        return null;
    }

    /**
     * Estimates the hash value from previous hash value
     *
     * @param prevHash previous hash
     * @param setHash flag indicates if hash should be generated and set to WayPoint
     * @return hash
     */
    String generateHash(String prevHash, boolean setHash) {
        String res = null;
        String toEncode =  getHashSecretKey();
        if (prevHash != null) {
            toEncode += prevHash;
        }
        if (m_date == null) {
            return null;
        }
        toEncode += SecureGPXParser.getDateString(m_date) + parseAccuracy(m_accuracy) + parseCoordinate(getLat()) + parseCoordinate(getLng());
        MessageDigest digest = null;
        String hash;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(toEncode.getBytes());
            res = SecureGPXParser.bytesToHexString(digest.digest());
            if (setHash) {
                setHash(res);
            }
            Log.d(GPXHandler.LOG_TAG,res+" "+getLat()+" "+parseCoordinate(getLat()));
            return res;

        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        return res;
    }

    protected Locale getHashLocale(){
        return Locale.ENGLISH;
    }

    private String parseDouble(int digits,double val){
        String res= Double.toString(val);
        if(!res.contains(".")){
            return res;
        }
        int prePoint = res.split("\\.")[0].length();
        if(res.length() > prePoint + 1+ digits){
            return res.substring(0,prePoint+1+digits); //digits + .
        }
        return res;
    }

    private String parseCoordinate(double doubleVal) {
        return parseDouble(6,doubleVal);
    }

    private String parseAccuracy(double acc){
        return parseDouble(1,acc);
    }

    /**
     * Generates the hash
     *
     * @param prevHash previous hash
     * @return hash
     */
    String generateHash(String prevHash) {
        return generateHash(prevHash, true);
    }

    /**
     * Gets the hash
     *
     * @return hash value
     */
    String getHash() {
        return m_hash;
    }

    /**
     * Sets the hash value
     *
     * @param hashValue to be set
     */
    void setHash(String hashValue) {
        m_hash = hashValue;
    }

    public static boolean isPointInRange(WayPoint point,Date minDate,Date maxDate){
        return minDate.compareTo(point.getDate())<0 && maxDate.compareTo(point.getDate())>0;
    }

}