 package de.shellfire.vpn.android;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockSharedPreferences implements SharedPreferences {

    private final Map<String, Object> preferences = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        return preferences;
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = preferences.get(key);
        return value instanceof String ? (String) value : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = preferences.get(key);
        return value instanceof Set ? (Set<String>) value : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = preferences.get(key);
        return value instanceof Integer ? (int) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = preferences.get(key);
        return value instanceof Long ? (long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = preferences.get(key);
        return value instanceof Float ? (float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = preferences.get(key);
        return value instanceof Boolean ? (boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
        return preferences.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new MockEditor(preferences);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // Not implemented for mock
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // Not implemented for mock
    }

    private static class MockEditor implements Editor {

        private final Map<String, Object> preferences;

        MockEditor(Map<String, Object> preferences) {
            this.preferences = preferences;
        }

        @Override
        public Editor putString(String key, String value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            preferences.put(key, values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            preferences.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            preferences.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            preferences.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
            // No need to implement for mock
        }
    }
}
