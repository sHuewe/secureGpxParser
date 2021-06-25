package de.shuewe.gpx;

import java.util.Date;

public class MinDistanceHandler extends GPXHandler {

    private WayPoint m_last;
    private double m_factor;

    private static boolean isSeperated(WayPoint p1, WayPoint p2, double factor){
        double distMeter = p1.calculateDistanceInKilometer(p2)*1000;
        return distMeter > factor*(p1.getAccuracy() + p2.getAccuracy());
    }

    public MinDistanceHandler(double factor){
        m_factor=factor;
    }

    @Override
    protected void writeTempData() {

    }

    @Override
    public void processWaypoint(String name, double lat, double lng, double accuracy, double alt) {
        addTrackPoint(null,name,lat,lng,accuracy,alt);
    }

    @Override
    public void processTrackpoint(String trackName, double lat, double lng, double accuracy, double alt) {
        WayPoint newPoint =  new WayPoint(lat,lng,new Date(),accuracy);
        newPoint.setAltitude(alt);
        if(m_last==null || isSeperated(newPoint,m_last,m_factor)){
            m_last = newPoint;
            addTrackPoint(trackName,null,lat,lng,accuracy,alt);
        }
    }
}
