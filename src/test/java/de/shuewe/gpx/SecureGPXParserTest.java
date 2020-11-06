package de.shuewe.gpx;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SecureGPXParserTest {

    @Test
    public void checkHashes(){
        SecureGPXParser parser = new SecureGPXParser();
        SecureGPXParser parser2 = new SecureGPXParser();
        for(int i=0;i<10;i++){
            parser.addTrackPoint("Test"+i,"",10.01,10.05,10.0);
            if(i!=5){
                parser2.addTrackPoint("Test"+i,10.01,10.05,10);
            }
        }
        assertEquals(parser.getLocations().size(),10);
        assertEquals(parser2.getLocations().size(),9);
        assertNotEquals(parser.getLocations().get(9).getHash(),parser2.getLocations().get(8).getHash());
            }



    @Test
    public void checkValidation_reload(){
        SecureGPXParser parser = new SecureGPXParser();
        parser.addTrackPoint("Test1",10.01,10.05,10);
        parser.addTrackPoint("Test2",12.01,9.05,10);
        parser.addTrackPoint("Test3",14.01,9.05,10);
        assertTrue(parser.isValid());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(out);
        String content=new String(out.toByteArray());
        assertFalse(content.isEmpty());
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        SecureGPXParser readParser=new SecureGPXParser();
        readParser.init(in);
        assertTrue(readParser.isValid());
    }

    @Test
    public void checkValidation_reload_manipulated(){
        SecureGPXParser parser = new SecureGPXParser();
        parser.addTrackPoint("Test1",10.01,10.05,10);
        parser.addTrackPoint("Test2",12.01,9.05,10);
        parser.addTrackPoint("Test3",14.01,9.05,10);
        assertTrue(parser.isValid());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(out);
        String content=new String(out.toByteArray());
        assertFalse(content.isEmpty());

        //Change name of a point, should not affect validation
        String newName="CustomNewName";
        content=content.replace("Test1",newName);
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        SecureGPXParser readParser=new SecureGPXParser();
        readParser.init(in);
        assertTrue(readParser.isValid());
        assertEquals(readParser.getLocations().get(0).getName(),newName);
        //Change coordinates, should break validation
        content=content.replace("12.01","09.11");
        assertFalse(content.isEmpty());
        in = new ByteArrayInputStream(content.getBytes());
        readParser=new SecureGPXParser();
        readParser.init(in);
        assertFalse(readParser.isValid());
    }

}
