package de.shellfire.vpn.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class FilterViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public FilterViewModelFactory(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid memory leaks
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FilterViewModel.class)) {
            return (T) new FilterViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
