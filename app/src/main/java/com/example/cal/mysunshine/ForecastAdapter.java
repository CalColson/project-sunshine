package com.example.cal.mysunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cal.mysunshine.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {
    final private Context mContext;
    private Cursor mCursor;
    final private ForecastAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;

    boolean mUseTodayLayout = true;

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;

    public static interface ForecastAdapterOnClickHandler {
        void onClick(long date, ForecastAdapterViewHolder vh);
    }

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler ch, View emptyView) {
        super();
        mContext = context;
        mClickHandler = ch;
        mEmptyView = emptyView;
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        return Utility.formatTemperature(mContext, high, isMetric) + "/" +
                Utility.formatTemperature(mContext, low, isMetric);
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {

        String highAndLow = formatHighLows(
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(ForecastFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            int layoutId = -1;
            if (viewType == VIEW_TYPE_TODAY) layoutId = R.layout.list_item_forecast_today;
            else if (viewType == VIEW_TYPE_FUTURE_DAY) layoutId = R.layout.list_item_forecast;

            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection");
        }
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */

    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_TODAY) {
            String cityName = mCursor.getString(ForecastFragment.COL_CITY_NAME);
            holder.cityView.setText(cityName);
        }

        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int fallbackIconId;

        if (viewType == VIEW_TYPE_TODAY)
            fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherId);
        else fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
        //holder.iconView.setImageResource(image);
        Glide.with(mContext)
                .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                .error(fallbackIconId)
                .crossFade()
                .into(holder.iconView);
        ViewCompat.setTransitionName(holder.iconView, "iconView" + position);

        // Read date from cursor
        long dateInMillis = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        String date = Utility.getFriendlyDayString(mContext, dateInMillis);
        holder.dateView.setText(date);

        // Read weather forecast from cursor
        String desc = mCursor.getString(ForecastFragment.COL_WEATHER_DESC);
        holder.descriptionView.setText(desc);
        holder.iconView.setContentDescription(desc);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(mContext);

        // Read high temperature from cursor
        double high = mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        holder.highTempView.setText(Utility.formatTemperature(mContext, high, isMetric));

        // Read low temperature from cursor
        double low = mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        holder.lowTempView.setText(Utility.formatTemperature(mContext, low, isMetric));
    }


    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public final TextView cityView;
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ForecastAdapterViewHolder(View view) {
            super(view);
            cityView = (TextView) view.findViewById(R.id.list_item_city_textview);
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPostion = getAdapterPosition();
            mCursor.moveToPosition(adapterPostion);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
        }
    }
}
