package com.example.terrozerg.routes.saved_route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.terrozerg.routes.MapsActivity;
import com.example.terrozerg.routes.PolylinesActivity;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * custom view of saved route
 * allows removal of this route
 */
public class RouteView extends androidx.appcompat.widget.AppCompatTextView {

    private ArrayList<LatLng> thisRoute;
    private String date;

    private float x;
    private float y;

    public RouteView(Context context) {
        super(context);
    }

    public RouteView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        x = event.getX();
        y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;

            case MotionEvent.ACTION_UP:
                performClick();

                return true;
        }
        return false;
    }

    @Override
    public boolean performClick() {
        super.performClick();

        if(x<300){
            dataPassInit(thisRoute);
        }
        else if(x>800 && y>90 && y<210){

            AlertDialog dialog = removalConfirmation(this,date);
            dialog.show();
        }

        return true;
    }

    public void setThisRoute(ArrayList<LatLng> thisRoute) {
        this.thisRoute = thisRoute;
    }

    public void setDate(String date) {
        this.date = date;
    }

    private void dataPassInit(ArrayList<LatLng> route){
        Context context = getContext();
        if(context instanceof Activity){
            Activity currActivity = (PolylinesActivity)context;

            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("route", route);

            intent.putExtras(bundle);

            currActivity.setResult(Activity.RESULT_OK, intent);
            currActivity.finish();
        }


    }

    public AlertDialog removalConfirmation(final View view, final String dateInMillis) {
        Context context = getContext();
        if (context instanceof Activity) {
            final Activity currActivity = (PolylinesActivity) context;

            return new AlertDialog.Builder(currActivity)
                    .setTitle("Delete")
                    .setMessage("Do you want to delete route?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(MapsActivity.Tag, "Confirmation dialog delete pressed.");

                            ConstraintLayout parent = (ConstraintLayout) view.getParent();
                            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) view.getLayoutParams();

                            int textId = view.getId();
                            int dateId = params.topToBottom;

                            parent.removeViewInLayout(view);
                            parent.removeViewInLayout(findViewById(params.topToBottom));

                            //update constrains via chain
                            ConstraintSet set = new ConstraintSet();
                            set.clone(parent);
                            set.removeFromVerticalChain(textId);
                            set.removeFromVerticalChain(dateId);

                            //TODO to recreate activity to apply changes?
                            currActivity.recreate();

                            File fileDir = new File(currActivity.getApplicationContext().getFilesDir() + "/routes/" + dateInMillis);
                            File[] files = fileDir.listFiles();
                            Log.d(MapsActivity.Tag, "files: " + Arrays.toString(files));

                            if (files != null) {
                                for (int j = 0; j < files.length; j++) {
                                    if (!(files[j].delete())) {
                                        Log.d(MapsActivity.Tag, "Failed to delete file." + files[j].getName());
                                    }
                                }
                                if (!fileDir.delete()) {
                                    Log.d(MapsActivity.Tag, "Failed to delete file." + fileDir.getName());
                                } else {
                                    Log.d(MapsActivity.Tag, "folder deleted: " + fileDir.getName());
                                }
                            }

                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(MapsActivity.Tag, "Confirmation dialog no pressed.");
                        }
                    })
                    .create();
        }
        else {
            Log.d(MapsActivity.Tag, "RouteView context is not an activity.");
            return null;
        }
    }
}