package com.google.android.gms.maps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

/**
 * Stub for SupportMapFragment to prevent Google Maps dependency in the F-Droid build.
 */
public class SupportMapFragment extends Fragment {

    public SupportMapFragment() {
        // Required empty public constructor
    }

    public static SupportMapFragment newInstance() {
        return new SupportMapFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a placeholder view
        TextView textView = new TextView(getContext());
        textView.setText("Maps feature is unavailable in this version.");
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }
}
