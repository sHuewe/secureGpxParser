package de.shuewe.gpx;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;


/**
 * Class representing a single WayPoint. Can be single WayPoint, Track-Point or Route-Point in gpx.
 */
public class WayPoint implements Comparable<WayPoint>, Serializable {

    //Radius of earth
    public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;

    //Secret key used for hash generating. Should be replaced in production by a secret key, or the corresponding method should be overwritten
    private static final String HASH_SECRET_KEY="";

    //Coordinates
    protected double m_lat, m_lng;

    //Accuracy
    private double m_accuracy;

    //Date
    private Date m_date;

    //Blockchain Hash value
    private String m_hash;

    //Name of point
    private String m_name = null;

    //Name of parent track
    private String m_parentName = null;

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
    public int compareTo(WayPoint wayPoint) {
        if (m_date == null) {
            return 1;
        }
        return m_date.compareTo(wayPoint.getDate());
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
        return m_parentName;
    }

    /**
     * Sets the parent name
     *
     * @param parentName to be set
     */
    public void setParentName(String parentName) {
        m_parentName = parentName;
    }

    /**
     * Returns the secret hash key. Should be changed / overwritten in production to return a secret (but constant) String
     *
     * @return secret String
     */
    protected String getHashSecretKey(){
        return HASH_SECRET_KEY;
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
            return res;

        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        return res;
    }

    private String parseCoordinate(double doubleVal) {
        return String.format("%.6f",doubleVal);
    }

    private String parseAccuracy(double acc){
        return String.format("%.1f",acc);
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

}