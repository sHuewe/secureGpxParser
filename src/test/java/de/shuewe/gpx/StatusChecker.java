package de.shuewe.gpx;

public class StatusChecker implements SecureGPXParser.GPXValidationListener, SecureGPXParser.GPXChangeListener, SecureGPXParser.GPXOnInitListener {

    private boolean isInit=false;
    private boolean isValid=false;
    private boolean validReady=false;

    public void reset(){
        isInit=false;
        isValid=false;
        validReady=false;
    }

    @Override
    public void handleChangedData(SecureGPXParser parser) {

    }

    @Override
    public void handleValidation(boolean valid) {
        isValid=valid;
        validReady=true;
    }

    @Override
    public void onInitReady(SecureGPXParser parser) {
        isInit=true;
    }
    public boolean isInit(){
        return isInit;
    }

    public boolean isValid() throws InterruptedException {
        int counter=0;
        while(!validReady){
            Thread.sleep(100);
            counter++;
            if(counter>100){
                System.out.println("NO VALIDATION AVAILABLE");
                return false;
            }
        }
        return isValid;
    }

    public void waitOnInit() throws InterruptedException {
        int counter = 0;
        while(!isInit){
            Thread.sleep(100);
            counter++;
            if(counter > 20){
                return;
            }
        }
    }

    public void waitOnThread() throws InterruptedException {
        int counter = 0;
        while(!isThreadReady()){
            Thread.sleep(100);
            counter++;
            if(counter > 20){
                return;
            }
        }
    }

    public boolean isThreadReady(){
        return GPXThread.getInstance().getHandler().getLooper().getQueue().isIdle();
    }
}