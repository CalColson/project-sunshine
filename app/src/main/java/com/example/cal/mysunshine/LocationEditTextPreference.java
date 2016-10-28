package com.example.cal.mysunshine;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

/**
 * Created by Cal on 10/19/2016.
 */

public class LocationEditTextPreference extends EditTextPreference {
    int mMinLength;

    private static final int DEFAULT_MINIMUM_LOCATION_LENGTH = 2;


    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.LocationEditTextPreference, 0, 0);

        try {
            mMinLength = a.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MINIMUM_LOCATION_LENGTH);
        }
        finally {
            a.recycle();
        }

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode == ConnectionResult.SUCCESS) setWidgetLayoutResource(R.layout.pref_current_location);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        View currentLocation = view.findViewById(R.id.current_location);
        currentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                Activity settingsActivity = (SettingsActivity) context;

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    settingsActivity.startActivityForResult(builder.build(settingsActivity),
                            SettingsActivity.PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        return view;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        EditText et = getEditText();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Dialog d = getDialog();
                if (d instanceof AlertDialog) {
                    AlertDialog dialog = (AlertDialog) d;
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                    if (s.length() < mMinLength) positiveButton.setEnabled(false);
                    else positiveButton.setEnabled(true);
                }
            }
        });
    }
}
