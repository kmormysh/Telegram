package com.katy.telegram.Activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.katy.telegram.R;

public class BaseActivity extends ActionBarActivity {

    private static BaseActivity currentActivity = null;
    private ProgressBar progressBar;
    private boolean isBusy;

    public static BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentActivity = this;
        super.onCreate(savedInstanceState);
    }

    protected boolean isBusy() {
        return isBusy;
    }

    protected void showProgress(boolean show) {
        isBusy = show;
        if (progressBar == null) {
            progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
        }
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
