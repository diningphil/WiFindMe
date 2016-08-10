package it.sam.diningphil.samproject;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by federico on 26/08/15.
 */
public class MyApplication extends Application{

    private ExecutorService pool;

    @Override
    public void onCreate() {
        super.onCreate();

        pool = Executors.newCachedThreadPool();
    }

    public ExecutorService getApplicationThreadPool(){
        return pool;
    }
}
