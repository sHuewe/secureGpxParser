package de.shuewe.gpx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TrackTest {

    private static Track m_track;
    private static final String TRACK_NAME="testTrack";

    @Before
    public void setUp() {

        m_track= new Track(TRACK_NAME);
        for(int j=-1000000;j<-100000;j+=100000) {
            List<WayPoint> points= new ArrayList<WayPoint>();
            for (int i = -10000; i < 0; i += 100) {
                WayPoint point = new WayPoint(10, 11, new Date(System.currentTimeMillis() + j + i), 10);
                point.setParentName(TRACK_NAME);
                points.add(point);
            }
            m_track.addPoints(points);
            m_track.startNewSegment();
        }
    }

    @Test
    public void testSetup(){
        assertEquals(10,m_track.getSegments().size());
        boolean emptyPoints=false;
        for(List<WayPoint> points:m_track.getSegments()) {
            if(!emptyPoints && points.isEmpty()){
                //This should be the last segment, if not, next iteration will fail
                emptyPoints=true;
                continue;
            }
            assertFalse(emptyPoints); //Only last segment shout be empty!
            assertEquals(100,points.size());
        }
        assertTrue(emptyPoints);

        //Check date order
        //First WayPoint of first segment < First WayPoint of second segment (compareTo considers date only for WayPoint)
        assertTrue(m_track.getSegments().get(0).get(0).compareTo(m_track.getSegments().get(1).get(0)) <0);
        //First WayPoint of first segment < Second WayPoint of first segment
        assertTrue(m_track.getSegments().get(0).get(0).compareTo(m_track.getSegments().get(0).get(1)) <0);
    }

    @Test
    public void testAddSegmentsInBetween(){
        List<WayPoint> points=m_track.getSegments().get(3);
        m_track.getSegments().remove(3);
        points.remove(0);
        points.remove(0);
        assertEquals(9,m_track.getSegments().size());
        assertEquals(98,points.size());

        //The 3rd segment should be removed. From original 3rd segment two points were removed
        //Add them again to track
        m_track.addSegments(Collections.singletonList(points));
        assertEquals(10,m_track.getSegments().size());
        assertEquals(98,m_track.getSegments().get(3).size());
    }
    @Test
    public void testAddSegmentsInExitingSegment(){
        List<WayPoint> points=m_track.getSegments().get(3);
        points=new ArrayList<>(points.subList(points.size()-5,points.size()-1));
        m_track.removeWaypoints(points);

        assertEquals(10,m_track.getSegments().size());
        assertEquals(96,m_track.getSegments().get(3).size());

        //The 3rd segment should be removed. From original 3rd segment two points were removed
        //Add them again to track
        m_track.addSegments(Collections.singletonList(points));
        assertEquals(10,m_track.getSegments().size());
        assertEquals(100,m_track.getSegments().get(3).size());
    }
}
