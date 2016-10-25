package com.example.cal.mysunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cal.mysunshine.data.WeatherContract;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    static final String DETAIL_URI = "URI";

    private String mForecastStr;
    ShareActionProvider mShareActionProvider;
    private ViewHolder mHolder;
    private Uri mUri;

    private static final int DETAIL_LOADER = 0;

    static final String[] DETAIL_COLUMNS = new String[]{WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_PRESSURE = 6;
    static final int COL_WEATHER_WIND_SPEED = 7;
    static final int COL_WEATHER_DEGREES = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;
    static final int COL_LOCATION_SETTING = 10;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.detailfragment, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);

        mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mForecastStr != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        } else Log.d(LOG_TAG, "Share action provider is null?");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mHolder = new ViewHolder(rootView);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
        }

        return rootView;
    }

    public void onLocationChanged (String newLocation) {
        Uri uri = mUri;
        if (uri != null) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mUri == null) return null;

        CursorLoader loader = new CursorLoader(getActivity(),
                mUri,
                DETAIL_COLUMNS,
                null, null, null);
        return loader;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (!data.moveToFirst()) return;

        boolean isMetric = Utility.isMetric(getActivity());

        long date = data.getLong(COL_WEATHER_DATE);
        String weatherDescription = data.getString((COL_WEATHER_DESC));
        String high = Utility.formatTemperature(getActivity(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
        String low = Utility.formatTemperature(getActivity(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
        float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
        float windDir = data.getFloat(COL_WEATHER_DEGREES);
        float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);
        int image = Utility.getArtResourceForWeatherCondition(weatherId);

        String dateString = Utility.formatDate(date);
        mForecastStr = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);

        mHolder.friendlyDateView.setText(Utility.getDayName(getActivity(), date));
        mHolder.dateView.setText(Utility.getFormattedMonthDay(getActivity(), date));
        mHolder.highTempView.setText(high);
        mHolder.lowTempView.setText(low);
        //mHolder.iconView.setImageResource(image);
        //Log.v(LOG_TAG, Utility.getArtUrlForWeatherCondition(getActivity(), weatherId));
        Glide.with(this)
                .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                .error(Utility.getArtResourceForWeatherCondition(weatherId))
                .crossFade()
                .into(mHolder.iconView);
        mHolder.iconView.setContentDescription(weatherDescription);
        mHolder.descriptionView.setText(weatherDescription);
        mHolder.humidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
        mHolder.windView.setText(Utility.getFormattedWind(getActivity(), windSpeed, windDir));
        mHolder.pressureView.setText(getActivity().getString(R.string.format_pressure, pressure));

        if (mShareActionProvider != null)
            mShareActionProvider.setShareIntent(createShareForecastIntent());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class ViewHolder {
        ImageView iconView;
        TextView dateView;
        TextView friendlyDateView;
        TextView descriptionView;
        TextView highTempView;
        TextView lowTempView;
        TextView humidityView;
        TextView windView;
        TextView pressureView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.detail_icon);
            dateView = (TextView) view.findViewById(R.id.detail_date_textview);
            friendlyDateView = (TextView) view.findViewById(R.id.detail_day_textview);
            descriptionView = (TextView) view.findViewById(R.id.detail_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.detail_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.detail_low_textview);
            humidityView = (TextView) view.findViewById(R.id.detail_humidity_textview);
            windView = (TextView) view.findViewById(R.id.detail_wind_textview);
            pressureView = (TextView) view.findViewById(R.id.detail_pressure_textview);
        }
    }
}
