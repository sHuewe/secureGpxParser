package de.shuewe.gpx;

public class DefaultGPXHandler extends GPXHandler {

    public DefaultGPXHandler(SecureGPXParser parser){
        init(parser);
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
        addTrackPoint(trackName,null,lat,lng,accuracy,alt);
    }
}
