package com.raghav.gfgffmpeg;

import android.app.Activity;

public abstract class AsyncTask {

    private Activity activity;
    public AsyncTask(Activity activity) {
        this.activity = activity;
    }

    private void startBackground() {
        new Thread(new Runnable() {
            public void run() {

                doInBackground();
                activity.runOnUiThread(new Runnable() {
                    public void run() {

                        onPostExecute();
                    }
                });
            }
        }).start();
    }
    public void execute(){
        startBackground();
    }

    public abstract void doInBackground();
    public abstract void onPostExecute();

}