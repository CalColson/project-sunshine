package com.example.cal.mysunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.cal.mysunshine.gcm.RegistrationIntentService;
import com.example.cal.mysunshine.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;


public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {
    String mLocation;
    String mUnits;
    boolean mTwoPane;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    final static String DETAILFRAGMENT_TAG = "DFTAG";
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mLocation = Utility.getPreferredLocation(this);
        Uri contentUri = getIntent() != null ? getIntent().getData() : null;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mUnits = pref.getString(getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric));

        if (findViewById(R.id.weather_detail_container) != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                DetailFragment fragment = new DetailFragment();
                if (contentUri != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(DetailFragment.DETAIL_URI, contentUri);
                    fragment.setArguments(args);
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                        .commit();
            }
        }
        else {
            mTwoPane = false;
            //getSupportActionBar().setElevation(0f);
        }

        ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast);
        forecastFragment.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);

        if (checkPlayServices()) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean sentToken = prefs.getBoolean(SENT_TOKEN_TO_SERVER, false);

            if (!sentToken) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        String curLocation = Utility.getPreferredLocation(this);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String curUnits = pref.getString(getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric));

        if (!curLocation.equals(mLocation) || !curUnits.equals(mUnits)) {
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast);
            ff.onLocationChanged();

            DetailFragment df = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (df != null) df.onLocationChanged(curLocation);

            mLocation = curLocation;
            mUnits = curUnits;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        Uri geolocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", location).build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geolocation);
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else Log.d(LOG_TAG, "Couldn't call " + location);
    }

    @Override
    public void onItemSelected(Uri dateUri, ForecastAdapter.ForecastAdapterViewHolder vh) {
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, dateUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        }

        else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.setData(dateUri);
            ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                    new Pair<View, String>(vh.iconView, getString(R.string.detail_icon_transition_name)));
            ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
            //startActivity(intent);
        }
    }

    /**
     +     * Check the device to make sure it has the Google Play Services APK. If
     +     * it doesn't, display a dialog that allows users to download the APK from
     +     * the Google Play Store or enable it in the device's system settings.
     +     */
        private boolean checkPlayServices() {
               GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
                if (resultCode != ConnectionResult.SUCCESS) {
                        if (apiAvailability.isUserResolvableError(resultCode)) {
                                apiAvailability.getErrorDialog(this, resultCode,
                                                PLAY_SERVICES_RESOLUTION_REQUEST).show();
                            } else {
                                Log.i(LOG_TAG, "This device is not supported.");
                                finish();
                            }
                        return false;
                    }
                return true;
            }
}