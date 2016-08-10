package it.sam.diningphil.samproject;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class TimerIntentService extends IntentService {

    private static final String TIMER_DIALOG_INTENT = "Timer_Dialog";
    private static final java.lang.String TIMER_ID = "TIMER_ID";

    int timerID = -1;
    int myID = -1;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public TimerIntentService(String name) {
        super(name);
    }

    public TimerIntentService(){
        super("Timer Intent Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        myID = startId;

        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if(intent != null) {

            timerID = intent.getIntExtra(TIMER_ID, -1);

            try {
                Thread.sleep(30000);

                Intent i = new Intent().setAction(TIMER_DIALOG_INTENT).putExtra(TIMER_ID, timerID);

                LocalBroadcastManager.getInstance(this).sendBroadcast(i);

            } catch (InterruptedException e) {
                e.getMessage();
            }

            stopSelf(myID);
        }
    }
}
