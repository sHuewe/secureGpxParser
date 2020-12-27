package de.shuewe.gpx;

import android.net.wifi.aware.DiscoverySession;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class GPXThread extends Thread {

    public static final String TAG_THREAD="GPXThread";

    private static GPXThread m_instance;

    public Handler m_handler;

    private GPXThread(){
    }

    public Handler getHandler()  {
        return m_handler;
    }

    public static enum ACTION{
        CHANGE_DATA(1), CHANGE_VALIDATION(2), SAVE(3),INIT(4);

        private int m_id;
        ACTION(int id){
            m_id=id;
        }
        int getId(){
            return m_id;
        }

        static ACTION fromInt(int i){
            for(ACTION a:values()){
                if(a.getId() == i){
                    return a;
                }
            }
            return null;
        }

    }

    public static Message getPreparedMessage(ACTION action,SecureGPXParser parser,Runnable runnable){
        Message res = new Message();
        res.arg1=action.getId();
        res.obj=new Object[]{parser,runnable};
        return res;
    }

    public static SecureGPXParser getGPXParserFromMessage(Message msg){
        return (SecureGPXParser) ((Object[])msg.obj)[0];
    }
    public static Runnable getRunnableFromMessage(Message msg){
        return (Runnable) ((Object[])msg.obj)[1];
    }



    public static GPXThread getInstance(){
        if(m_instance == null){
            m_instance = new GPXThread();
            m_instance.start();
            while (m_instance.getHandler() == null){

            }
        }
        return m_instance;
    }

    @Override
    public void run(){
        Looper.prepare();
        m_handler= new Handler() {
            public void handleMessage(Message msg) {
                ACTION action = ACTION.fromInt(msg.arg1);
                switch (action){
                    case CHANGE_DATA:
                        Log.d(TAG_THREAD,"Handle change..");
                        break;
                    case CHANGE_VALIDATION:
                        Log.d(TAG_THREAD,"Handle validation..");

                    case SAVE:
                        Log.d(TAG_THREAD,"Handle save..");

                    case INIT:
                        Log.d(TAG_THREAD,"Handle init");
                }
                Runnable runnable = getRunnableFromMessage(msg);
                SecureGPXParser parser = getGPXParserFromMessage(msg);
                Log.d(TAG_THREAD,"startRunnable");
                runnable.run();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(Looper.myLooper().getQueue().isIdle()){
                        Log.d(TAG_THREAD,"isIdle");
                    }else{
                        Log.d(TAG_THREAD,"notIdle");
                    }
                }
                if(parser!=null) {
                    parser.notifyListener(action);
                }
            }
        };
        Looper.loop();
    }
}
