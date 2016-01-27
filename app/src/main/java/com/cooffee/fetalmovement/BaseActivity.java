package com.cooffee.fetalmovement;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by eric on 16/1/26.
 */
public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void back(View view) {
        this.finish();
    }
}
