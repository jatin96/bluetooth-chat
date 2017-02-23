//DroidRush project by Jatin and Manveer
package com.example.android.common.activities;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogWrapper;

public class SampleActivityBase extends FragmentActivity {
    public static final String TAG = "SampleActivityBase";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    protected  void onStart() {
        super.onStart();
        initializeLogging();
    }

    public void initializeLogging() {
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);
        Log.i(TAG, "Ready");
    }
}
