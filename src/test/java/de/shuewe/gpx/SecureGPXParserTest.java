package de.shuewe.gpx;

import android.os.Build;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class SecureGPXParserTest {
    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        //you other setup here
    }

    @Test
    public void checkHashes() throws InterruptedException {
        SecureGPXParser parser = new SecureGPXParser();
        SecureGPXParser parser2 = new SecureGPXParser();
        StatusChecker status = new StatusChecker();
        for (int i = 0; i < 10; i++) {
            parser.getHandler().processWaypoint("Test" + i, 10.01, 10.05, 10.0, 10.0);
            if (i != 5) {
                parser2.getHandler().processWaypoint("Test" + i, 10.01, 10.05, 10, 10);
            }
        }
      status.waitOnThread();
        assertEquals(10, parser.getHandler().getLocations().size());
        assertEquals(9, parser2.getHandler().getLocations().size());
        assertNotEquals(parser.getHandler().getLocations().get(9).getHash(), parser2.getHandler().getLocations().get(8).getHash());
    }


    @Test
    public void checkValidation_reload() throws InterruptedException {

        SecureGPXParser parser = new SecureGPXParser();
        StatusChecker status = new StatusChecker();
        parser.getHandler().processWaypoint("Test1",10.01,10.05,10,10);
        parser.getHandler().processWaypoint("Test2",12.01,9.05,10,10);
        parser.getHandler().processWaypoint("Test3",14.01,9.05,10,10);
        status.reset();
        parser.getHandler().requestValidation(status);
        assertTrue(status.isValid());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(out);
        String content=new String(out.toByteArray());
        assertFalse(content.isEmpty());
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        SecureGPXParser readParser=new SecureGPXParser();
        readParser.init(in);
        readParser.getHandler().requestValidation(status);
        assertTrue(status.isValid());
    }

    @Test
    public void checkReset() throws InterruptedException {
        SecureGPXParser parser = new SecureGPXParser();
        StatusChecker status = new StatusChecker();
        parser.getHandler().processWaypoint("Test1",10.01,10.05,10,10);
        parser.getHandler().processWaypoint("Test2",12.01,9.05,10,10);
        parser.getHandler().processWaypoint("Test3",14.01,9.05,10,10);
        parser.getHandler().requestValidation(status);
        assertTrue(status.isValid());
        assertEquals(3,parser.getHandler().getLocations().size());
        parser.reset();
        status.reset();
        parser.getHandler().processWaypoint("Test1",10.01,10.05,10,10);
        parser.getHandler().processWaypoint("Test2",12.01,9.05,10,10);
        parser.getHandler().requestValidation(status);
        assertTrue(status.isValid());
        assertEquals(2,parser.getHandler().getLocations().size());
    }

    @Test
    public void checkValidation_reload_manipulated() throws InterruptedException {
        SecureGPXParser parser = new SecureGPXParser();
        StatusChecker status = new StatusChecker();
        parser.getHandler().processWaypoint("Test1",10.01,10.05,10,10);
        parser.getHandler().processWaypoint("Test2",12.01,9.05,10,10);
        parser.getHandler().processWaypoint("Test3",14.01,9.05,10,10);
        parser.getHandler().requestValidation(status);
        assertTrue(status.isValid());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(out);
        String content=new String(out.toByteArray());
        assertFalse(content.isEmpty());

        status.reset();
        //Change name of a point, should not affect validation
        String newName="CustomNewName";
        content=content.replace("Test1",newName);
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        SecureGPXParser readParser=new SecureGPXParser();
        readParser.init(in);
        readParser.getHandler().requestValidation(status);
        assertTrue(status.isValid());
        assertEquals(readParser.getHandler().getLocations().get(0).getName(),newName);
        //Change coordinates, should break validation
        status.reset();
        content=content.replace("12.01","09.11");
        assertFalse(content.isEmpty());
        in = new ByteArrayInputStream(content.getBytes());
        readParser=new SecureGPXParser();
        readParser.init(in);
        readParser.getHandler().requestValidation(status);
        assertFalse(status.isValid());

    }

}
