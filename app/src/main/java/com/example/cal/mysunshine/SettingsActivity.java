package com.example.cal.mysunshine;


import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.cal.mysunshine.data.WeatherContract;
import com.example.cal.mysunshine.sync.SunshineSyncAdapter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;


/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    protected final static int PLACE_PICKER_REQUEST = 9090;
    private ImageView mAttribution;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        // TODO: Add preferences from XML
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        // TODO: Add preferences
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_country_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_art_pack_key)));
        //unnecessary
        //bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_enable_notifications_key)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mAttribution = new ImageView(this);
            mAttribution.setImageResource(R.drawable.powered_by_google_light);

            if (!Utility.isLocationAvailable(this)) mAttribution.setVisibility(View.GONE);
            setListFooter(mAttribution);
        }
    }

    @Override
    protected void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {

        String stringValue = value.toString();
        String key = preference.getKey();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }

        else if (key.equals(getString(R.string.pref_location_key))) {
            //SunshineSyncAdapter.syncImmediately(this);

            @SunshineSyncAdapter.LocationStatus int status = Utility.getLocationStatus(this);
            switch (status) {
                case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                    preference.setSummary(getString(R.string.pref_location_unknown_description, stringValue));
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                    preference.setSummary(getString(R.string.pref_location_error_description, stringValue));
                    break;
                default:
                    preference.setSummary(stringValue);
            }
        }

        else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_key)) ||
                key.equals(getString(R.string.pref_country_key))) {

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(getString(R.string.pref_location_latitude));
            editor.remove(getString(R.string.pref_location_longitude));
            editor.commit();

            if (mAttribution != null) mAttribution.setVisibility(View.GONE);

            Utility.resetLocationStatus(this);
            SunshineSyncAdapter.syncImmediately(this);
        }
        else if (key.equals(getString(R.string.pref_units_key))) {
            getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
        else if (key.equals(getString(R.string.pref_art_pack_key))) {
            getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
               if (resultCode == RESULT_OK) {
                   Place place = PlacePicker.getPlace(this, data);
                   String address = place.getAddress().toString();
                   LatLng latLong = place.getLatLng();

                   if (TextUtils.isEmpty(address)) address = String.format("(%.2f, %.2f)",
                           latLong.latitude, latLong.longitude);

                   SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                   SharedPreferences.Editor editor = prefs.edit();
                   editor.putString(getString(R.string.pref_location_key), address);
                   editor.putFloat(getString(R.string.pref_location_latitude), (float) latLong.latitude);
                   editor.putFloat(getString(R.string.pref_location_longitude), (float) latLong.longitude);
                   editor.commit();

                   Preference locationPreference = findPreference(getString(R.string.pref_location_key));
                   locationPreference.setSummary(address);

                   if (mAttribution != null) mAttribution.setVisibility(View.VISIBLE);
                   else {
                       View rootView = findViewById(android.R.id.content);
                       Snackbar.make(rootView, getString(R.string.attribution_text), Snackbar.LENGTH_LONG).show();
                   }

                   Utility.resetLocationStatus(this);
                   SunshineSyncAdapter.syncImmediately(this);
               }
        }
        else super.onActivityResult(requestCode, resultCode, data);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
