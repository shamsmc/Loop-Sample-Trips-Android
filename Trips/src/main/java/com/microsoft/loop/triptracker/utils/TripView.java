package com.microsoft.loop.triptracker.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.loop.triptracker.MainActivity;
import com.microsoft.loop.triptracker.R;
import com.microsoft.loop.triptracker.SampleAppApplication;

import java.text.SimpleDateFormat;
import java.util.Locale;

import ms.loop.loopsdk.profile.Drive;

import ms.loop.loopsdk.profile.Trip;


/**
 * Created on 6/22/16.
 */
public class TripView {

    private TextView txtDriveFrom;
    private TextView txtDriveFrom2;
    private TextView txtDriveTo;
    private TextView txtTime;
    private TextView txtDurationHours;
    private TextView txtDurationMinutes;
    private TextView txtDurationSecs;
    private TextView txtDistance;
    private TextView txtSample;

    private ImageView imgDirectionIcon;
    private ImageView imgDirectionIcon2;
    private ImageView driveToUnknown;
    private ImageView driveFromUnknown;

    private ImageView toKnownLocation;
    private ImageView fromKnownLocation;

    private final SimpleDateFormat dateFormatWithDay = new SimpleDateFormat("MM/dd h:mm a", Locale.US);
    private final SimpleDateFormat dateFormat        = new SimpleDateFormat("h:mm a", Locale.US);

    private Context mContext;


    public TripView(View view) {

        txtTime = (TextView) view.findViewById(R.id.drive_time);
        txtDistance = (TextView) view.findViewById(R.id.drive_distance);
        txtDriveFrom = (TextView) view.findViewById(R.id.drive_from);
        txtDriveFrom2 = (TextView) view.findViewById(R.id.drive_from2);
        txtTime = (TextView) view.findViewById(R.id.drive_time);
        txtDriveTo = (TextView) view.findViewById(R.id.drive_to);
        txtDurationHours = (TextView) view.findViewById(R.id.drive_duration_hour);
        txtDurationMinutes = (TextView) view.findViewById(R.id.drive_duration_min);
        txtDurationSecs = (TextView) view.findViewById(R.id.drive_duration_sec);
        imgDirectionIcon = (ImageView) view.findViewById(R.id.drive_direction_icon);
        imgDirectionIcon2 = (ImageView) view.findViewById(R.id.drive_direction_icon2);
        driveFromUnknown = (ImageView) view.findViewById(R.id.drive_from_location_unknwon);
        driveToUnknown = (ImageView) view.findViewById(R.id.drive_to_location_unknwon);
        txtSample = (TextView) view.findViewById(R.id.sample_trip);
        toKnownLocation = (ImageView) view.findViewById(R.id.to_knownLocation);
        fromKnownLocation = (ImageView) view.findViewById(R.id.from_knownLocation);
    }
    public void update(Context context, Trip trip, boolean singleView){

        if (trip == null) {
            SampleAppApplication.instance.sendDebugEvent("trip null");
            return;
        }

        mContext = context;

        txtDriveTo.setVisibility(View.VISIBLE);
        txtDriveFrom.setVisibility(View.VISIBLE);
        txtDriveFrom2.setVisibility(View.GONE);

        txtDriveFrom.setText(getTripStartLocation(trip));
        txtDriveFrom2.setText(getTripStartLocation(trip));

        String strEndPlace = getTripEndLocation(trip);
        if (TextUtils.isEmpty(strEndPlace)){
            imgDirectionIcon2.setVisibility(View.VISIBLE);
            imgDirectionIcon.setVisibility(View.GONE);
        }
        else {
            imgDirectionIcon.setVisibility(View.VISIBLE);
            imgDirectionIcon2.setVisibility(View.GONE);
        }
        txtDriveTo.setText(getTripEndLocation(trip));
        txtDistance.setText(getTripDistance(trip));
        txtTime.setText(getTripTimeInfo(trip));
       fillTripDuration(trip);

        if (trip.entityId.startsWith("test") &&!singleView) {
            txtSample.setVisibility(View.VISIBLE);
            if(trip instanceof Drive){
                txtSample.setText("SAMPLE DRIVE");
            }
            else {
                txtSample.setText("SAMPLE TRIP");
            }
        }
        else {
            txtSample.setVisibility(View.GONE);
        }

        toKnownLocation.setVisibility(View.GONE);
        fromKnownLocation.setVisibility(View.GONE);

        if ((imgDirectionIcon2.getVisibility() == View.GONE) && MainActivity.isKnownLocation(trip.endLocation, "work")){

            String uri = "@drawable/ic_work";
            int imageResource = context.getResources().getIdentifier(uri, null, context.getPackageName());
            Drawable res = context.getResources().getDrawable(imageResource);
            toKnownLocation.setImageDrawable(res);
            toKnownLocation.setVisibility(View.VISIBLE);

        }
        if ((imgDirectionIcon2.getVisibility() == View.GONE) && MainActivity.isKnownLocation(trip.endLocation, "home")){
            String uri = "@drawable/ic_home";
            int imageResource = context.getResources().getIdentifier(uri, null, context.getPackageName());
            Drawable res = context.getResources().getDrawable(imageResource);
            toKnownLocation.setImageDrawable(res);
            toKnownLocation.setVisibility(View.VISIBLE);
        }

        if (MainActivity.isKnownLocation(trip.startLocation, "work")){

            String uri = "@drawable/ic_work";
            int imageResource = context.getResources().getIdentifier(uri, null, context.getPackageName());
            Drawable res = context.getResources().getDrawable(imageResource);
            fromKnownLocation.setImageDrawable(res);
            fromKnownLocation.setVisibility(View.VISIBLE);
            imgDirectionIcon2.setVisibility(View.GONE);
        }
        if (MainActivity.isKnownLocation(trip.startLocation, "home")){

            String uri = "@drawable/ic_home";
            int imageResource = context.getResources().getIdentifier(uri, null, context.getPackageName());
            Drawable res = context.getResources().getDrawable(imageResource);
            fromKnownLocation.setImageDrawable(res);
            fromKnownLocation.setVisibility(View.VISIBLE);
            imgDirectionIcon2.setVisibility(View.GONE);
        }
    }

    private String getTripStartLocation(Trip trip) {

        if (trip.startLocale == null || TextUtils.isEmpty(trip.startLocale.getFriendlyName())) {
            driveFromUnknown.setVisibility(View.VISIBLE);
            return "UNKNOWN";
        }
        driveFromUnknown.setVisibility(View.GONE);
        return trip.startLocale.getFriendlyName().toUpperCase(Locale.US);
    }

    private String getTripEndLocation(Trip trip) {

        String start = TextUtils.isEmpty(trip.startLocale.getFriendlyName()) ? "Unknown" : trip.startLocale.getFriendlyName();
        String end = TextUtils.isEmpty(trip.endLocale.getFriendlyName()) ? "Unknown" : trip.endLocale.getFriendlyName();

        if (start.equals("unknown") && SampleAppApplication.isNetworkAvailable(mContext)){
            trip.updateStartLocale();
        }
        if (end.equals("unknown") && SampleAppApplication.isNetworkAvailable(mContext)){
            trip.updateEndLocale();
        }

        if (start.equalsIgnoreCase(end)) {

            // just display one lable
            driveToUnknown.setVisibility(View.GONE);
            txtDriveTo.setVisibility(View.GONE);
            txtDriveFrom.setVisibility(View.GONE);
            txtDriveFrom2.setVisibility(View.VISIBLE);
            return "";
        }

        if (end.equals("unknown")) {
            driveToUnknown.setVisibility(View.VISIBLE);
        } else {
            driveToUnknown.setVisibility(View.GONE);
        }
        return trip.endLocale.getFriendlyName().toUpperCase(Locale.US);
    }

    private String getTripDistance(Trip trip) {
        Double dist = trip.getRouteDistanceInKilometers();
        Double miles = dist * 0.621371;
        return String.format(Locale.US, "%.2f mi.", miles);
    }

    private String getTripTimeInfo(Trip trip) {
        String start = dateFormatWithDay.format(trip.startedAt);
        if (DateUtils.isToday(trip.startedAt.getTime())){
            start = "TODAY "+ dateFormat.format(trip.startedAt);
        }
        String end = dateFormat.format(trip.endedAt);
        Double dur = trip.getDurationMinutes();

        return String.format(Locale.US, "%s - %s", start, end, dur);
    }

    private void fillTripDuration(Trip trip)
    {
        long diffInSeconds = (trip.endedAt.getTime() - trip.startedAt.getTime()) / 1000;

        long diff[] = new long[] {0, 0, 0 };
        diff[2] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
        diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
        diff[0] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;

        txtDurationHours.setVisibility(View.GONE);
        txtDurationMinutes.setVisibility(View.GONE);
        txtDurationSecs.setVisibility(View.GONE);

        if (diff[0] > 0){
            txtDurationHours.setText(String.format(Locale.US, "%sh", diff[0]));
            txtDurationHours.setVisibility(View.VISIBLE);
        }

        if (diff[1] > 0){
            txtDurationMinutes.setText(String.format(Locale.US, "%sm", diff[1]));
            txtDurationMinutes.setVisibility(View.VISIBLE);
        }

        if (diff[2] > 0){
            txtDurationSecs.setText(String.format(Locale.US, "%ss", diff[2]));
            txtDurationSecs.setVisibility(View.VISIBLE);
        }
    }

    private int getTextWidthInDip(int width){
        int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                (float) 170, mContext.getResources().getDisplayMetrics());
        return value;
    }

}
