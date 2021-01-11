package com.example.terrozerg.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.maps.android.SphericalUtil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK;

public class PolylinesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polylines);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle("Saved routes");

        loadRoutes();
    }

    //TODO save activity instance?
    //TODO what to do here? reorder or not?
    @Override
    public void onBackPressed() {
        //Intent intent = new Intent();
        Intent intent = new Intent(this, MapsActivity.class);
        //intent.putExtra("test", "test");
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        //etResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }

    //TODO proper recursion to delete all in 1 call
    //debug delete all files
    private void deleteFiles(File files){
        if(!files.delete()){
            File[] children = files.listFiles();
            if(children!=null) {
                for (int i = 0; i < children.length; i++) {
                    deleteFiles(children[i]);
                }
                files.delete();
            }
        }else {
            Log.d(MapsActivity.Tag, "successfully deleted all files.");
        }
    }

    private void loadRoutes(){
        //all files in directory
        File filesDir = new File(getApplicationContext().getFilesDir(),"routes");
        File[] folders = filesDir.listFiles();

        //deleteFiles(filesDir);

        String diff = "";
        ArrayList<LatLng> contents;
        long distance;
        Bitmap image = null;

        if(folders!=null) {

            //go through all added dates// date[image,route]

            for (int i = 0; i < folders.length; i++) {
                //date of a route
                String folderName = folders[i].getName();
                //adding poly points per rotation, need to reset array
                contents = new ArrayList<>();

                //image and route files
                File[] files = folders[i].listFiles();
                if (files != null) {
                    for (int j = 0; j < files.length; j++){
                        String fileName = files[j].getName();

                        if(fileName.contains("image")){
                            //get map image
                            image = BitmapFactory.decodeFile(files[j].getAbsolutePath());
                        }

                        else if(fileName.contains("route")){
                            //get file contents
                            try(FileInputStream fis = new FileInputStream(files[j])) {
                                InputStreamReader inputStreamReader =
                                        new InputStreamReader(fis, StandardCharsets.UTF_8);
                                try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                                    //get diff
                                    diff = reader.readLine();

                                    String line = reader.readLine();
                                    String line2;

                                    while(line!=null){
                                        line2 = reader.readLine();
                                        contents.add(new LatLng(Double.parseDouble(line),Double.parseDouble(line2)));
                                        line = reader.readLine();
                                    }

                                } catch (IOException e) {
                                    Log.d("DEBUG_LOGS","raw file reading error."+e.getMessage());
                                } finally {
                                    inputStreamReader.close();
                                }

                            } catch (IOException e){
                                Log.d("DEBUG_LOGS","fileinputstream error."+e.getMessage());
                            }
                        }
                    }
                }

                if(!diff.equals("")) {
                    addPolyView(Long.parseLong(diff), contents, folderName, image);
                }
            }
        }
    }

    //TODO content description
    // content positioning
    // sort by date?
    // all above via MySQL?
    private void addPolyView(long time, final ArrayList<LatLng> route, final String dateInMillis, Bitmap mapImage){
        if(route==null){
            Log.d(MapsActivity.Tag,"route is empty.");
            return;
        }
        else if(mapImage==null){
            Log.d(MapsActivity.Tag,"map is empty.");
            return;
        }

        //screen density
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;

        //distance
        float distance = (float) SphericalUtil.computeLength(route);
        String measure;

        //speed
        long timeS = time/1000;

        long speed = Math.round((distance/timeS)*3.6);
        if(speed<1){
            speed = 0;
        }

        //distance measurements
        if(distance>999){
            distance = distance/1000;
            measure = "km";
        }
        else{
            measure = "m";
        }

        //time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss",Locale.getDefault());
        String duration = simpleDateFormat.format(calendar.getTime());

        //date
        calendar.setTimeInMillis(Long.parseLong(dateInMillis));
        simpleDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
        String routeDate = simpleDateFormat.format(calendar.getTime());

        //dimens
        int textPadding = getResources().getDimensionPixelSize(R.dimen.text_padding);

        //base layout contents
        //TextView textView = (TextView) findViewById(R.id.textBase);
        ConstraintLayout containerLayout = (ConstraintLayout) findViewById(R.id.constrainBase);
        ConstraintSet set = new ConstraintSet();

        //define upmost element
        int upperElementId;
        int last = containerLayout.getChildCount();

        if(last>0) {
            upperElementId = containerLayout.getChildAt(last-1).getId();
        }
        else{
            upperElementId = containerLayout.getId();
        }

        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        //important
        //text needs to align by gravity to setGravity to apply to text
        params.addRule(RelativeLayout.TEXT_ALIGNMENT_GRAVITY);

        //view with route date
        final TextView newDate = new TextView(PolylinesActivity.this);

        newDate.setId(ViewCompat.generateViewId());
        newDate.setGravity(Gravity.START);
        newDate.setTextSize(20);
        newDate.setPadding(0,textPadding,0,0);

        //set text and context
        newDate.setText(routeDate);
        newDate.setContentDescription("Route date "+routeDate);

        containerLayout.addView(newDate, params);

        /*
        //view with route data
        TextView newText = new TextView(PolylinesActivity.this);
        newText.setId(ViewCompat.generateViewId());

        newText.setPadding(0,0,0,textPadding);
        newText.setLines(3);
        newText.setTextSize(20);
        newText.setGravity(Gravity.CENTER);
        newText.setText(String.format(Locale.ENGLISH,"Distance: %.1f %s\nDuration: %s\nSpeed: %s k/h", distance, measure, duration, speed));

        //map image size
        float drawableSize = getResources().getDimension(R.dimen.drawable_size);
        int size = (int) (drawableSize * scale + 0.5f);

        //map image
        Drawable mapDrawable = new BitmapDrawable(getResources(),Bitmap.createScaledBitmap(mapImage,size,size,true));

        //delete button
        Drawable closeDrawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_delete_forever_black_36dp);

        newText.setCompoundDrawablesRelativeWithIntrinsicBounds(mapDrawable,null,closeDrawable,null);
        newText.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.drawable_padding));

        newText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("route", route);

                intent.putExtras(bundle);

                setResult(Activity.RESULT_OK,intent);
                //TODO dialog to make sure user wants to leave activity

                finish();
            }
        });*/

        //TODO if ontouchlistener - need custom view | if onclick - normal views are ok, but need to match view size with clickable area
        // split all information in separate views, and wrap in constrain layout container?
        // or create custom view(somehow) with all information in matching fields?
        //test
        //custom view
        RouteView newText = new RouteView(PolylinesActivity.this);
        newText.setId(ViewCompat.generateViewId());

        newText.setPadding(0,0,0,textPadding);
        newText.setLines(3);
        newText.setTextSize(20);
        newText.setGravity(Gravity.CENTER);

        //test
        newText.setThisRoute(route);
        newText.setDate(dateInMillis);

        //set text and context
        String text = String.format(Locale.ENGLISH,"Distance: %.1f %s\nDuration: %s\nSpeed: %s k/h", distance, measure, duration, speed);
        newText.setText(text);
        newText.setContentDescription(text);

        //map image size
        float drawableSize = getResources().getDimension(R.dimen.drawable_size);
        int size = (int) (drawableSize * scale + 0.5f);

        //map image
        Drawable mapDrawable = new BitmapDrawable(getResources(),Bitmap.createScaledBitmap(mapImage,size,size,true));

        //delete button
        Drawable closeDrawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_delete_forever_black_36dp);

        newText.setCompoundDrawablesRelativeWithIntrinsicBounds(mapDrawable,null,closeDrawable,null);
        newText.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.drawable_padding));

        /*
        newText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();


                    Log.d(MapsActivity.Tag, "x: " + x+", y: "+y);
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            return true;
                        case MotionEvent.ACTION_UP:
                            view.performClick();

                            if(x<300) {

                                Intent intent = new Intent();
                                Bundle bundle = new Bundle();
                                bundle.putParcelableArrayList("route", route);

                                intent.putExtras(bundle);

                                setResult(Activity.RESULT_OK, intent);
                                finish();

                            }
                            else if (x>800){
                                AlertDialog dialog = removalConfirmation(view, dateInMillis);
                                dialog.show();

                                // and remove file from /routes/
                            }


                            return true;
                    }
                return false;
            }
        });*/

        /*newText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                float x = motionEvent.getX();
                //float y = motionEvent.getY();

                if(x<300) {
                    Log.d(MapsActivity.Tag, "x: " + x);
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_UP:
                            view.performClick();

                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putParcelableArrayList("route", route);

                            intent.putExtras(bundle);

                            setResult(Activity.RESULT_OK,intent);

                            finish();
                            return true;
                    }
                }
                return false;
            }
        });*/

        containerLayout.addView(newText, params);

        final int dateId = newDate.getId();
        final int textId = newText.getId();

        //constrain params
        set.clone(containerLayout);

        if(upperElementId==containerLayout.getId()){
            int[] ids = {dateId, textId};
            set.createVerticalChain(ConstraintSet.PARENT_ID, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, ids,null,0 );
        }
        else{
            set.addToVerticalChain(dateId,upperElementId,textId);
            set.addToVerticalChain(textId,dateId,ConstraintSet.PARENT_ID);
        }

        set.applyTo(containerLayout);
    }

    /*
    public AlertDialog removalConfirmation(final View view, final String dateInMillis){
        return new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Do you want to delete route?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(MapsActivity.Tag, "Confirmation dialog delete pressed.");

                        ConstraintLayout parent = (ConstraintLayout) view.getParent();
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)view.getLayoutParams();

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
                        recreate();

                        File fileDir = new File(getApplicationContext().getFilesDir()+"/routes/"+dateInMillis);
                        File[] files = fileDir.listFiles();
                        Log.d(MapsActivity.Tag, "files: "+ Arrays.toString(files));

                        if (files != null) {
                            for(int j=0;j<files.length;j++){
                                if(!(files[j].delete())){
                                    Log.d(MapsActivity.Tag, "Failed to delete file."+ files[j].getName());
                                }
                            }
                            if(!fileDir.delete()){
                                Log.d(MapsActivity.Tag, "Failed to delete file."+ fileDir.getName());
                            }
                            else{
                                Log.d(MapsActivity.Tag, "folder deleted: "+fileDir.getName());
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
    }*/
}