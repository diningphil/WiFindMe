package it.sam.diningphil.samproject;


import android.os.AsyncTask;

/**
 * Callback interface through which the fragment will report the
 * task's progress and results back to the Activity.
 */
public interface TaskCallbacks {
    void onPreExecute(AsyncTask<Void, String, Void> task);
    void onProgressUpdate(String reason);
    void onCancelled();
    void onPostExecute();
}
