package com.example.cal.mysunshine.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.example.cal.mysunshine.MainActivity;
import com.example.cal.mysunshine.R;
import com.example.cal.mysunshine.Utility;
import com.example.cal.mysunshine.data.WeatherContract;

public class TodayWidgetIntentService extends IntentService {
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;
    private static final int INDEX_MAX_TEMP = 2;
    private static final int INDEX_MIN_TEMP = 3;

    public TodayWidgetIntentService() {
        super("TodayWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = awm.getAppWidgetIds(new ComponentName(this, TodayWidgetProvider.class));
        boolean isMetric = Utility.isMetric(this);
        String location = Utility.getPreferredLocation(this);

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = getContentResolver().query(weatherForLocationUri,
                FORECAST_COLUMNS, null, null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

        if (data == null) return;
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        int weatherId = data.getInt(INDEX_WEATHER_ID);
        int weatherArtResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
        String description = data.getString(INDEX_SHORT_DESC);
        double maxTemp = data.getDouble(INDEX_MAX_TEMP);
        double minTemp = data.getDouble(INDEX_MIN_TEMP);
        String formattedMaxTemperature = Utility.formatTemperature(this, maxTemp, isMetric);
        String formattedMinTemperature = Utility.formatTemperature(this, minTemp, isMetric);

        for (int appWidgetId : appWidgetIds) {
            int widgetWidth = getWidgetWidth(awm, appWidgetId);
            int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
            int largeWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_large_width);

            int layoutId;
            if (widgetWidth >= largeWidth) layoutId = R.layout.widget_today_large;
            else if (widgetWidth >= defaultWidth) layoutId = R.layout.widget_today;
            else layoutId = R.layout.widget_today_small;

            RemoteViews views = new RemoteViews(this.getPackageName(), layoutId);

            views.setImageViewResource(R.id.widget_icon, weatherArtResourceId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                views.setContentDescription(R.id.widget_icon, description);
            }

            views.setTextViewText(R.id.widget_description, description);
            views.setTextViewText(R.id.widget_high_temperature, formattedMaxTemperature);
            views.setTextViewText(R.id.widget_low_temperature, formattedMinTemperature);

            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pi);

            awm.updateAppWidget(appWidgetId, views);
        }
    }

    private int getWidgetWidth(AppWidgetManager awm, int appWidgetId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
        }
        return getWidgetWidthFromOptions(awm, appWidgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager awm, int appWidgetId) {
        Bundle options = awm.getAppWidgetOptions(appWidgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp, dm);
        }
        return getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
    }
}