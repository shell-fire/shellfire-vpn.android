package de.shellfire.vpn.android;

import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;


public class SetPreferenceActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        return true;
    }

}

