package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.sam_chordas.android.stockhawk.BuildConfig;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by chetna_priya on 5/15/2016.
 */
public class CustomMarkerView extends MarkerView {

    private static final String LOG_TAG = CustomMarkerView.class.getSimpleName();
    private TextView tvContent;

    Context context;
    private float min_offset = 100f;
    ArrayList<String> xLabels;

    public CustomMarkerView(Context context, int layoutResource, ArrayList<String> xLabels) {
        super(context, layoutResource);
        this.context = context;
        this.xLabels = xLabels;
        // this markerview only displays a textview
        tvContent = (TextView) findViewById(R.id.tv_content);
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        String text  = getDate(e.getXIndex())+"\n "
                +context.getString(R.string.price)+" " + e.getVal();
        tvContent.setText(text); // set the entry-value as the display text
        String formattedString = String.format(context.getString(R.string.closing_value),
                e.getVal(),getDate(e.getXIndex()));
        tvContent.setContentDescription(formattedString);
        this.setContentDescription(formattedString);
        this.setFocusable(true);
        Log.d(LOG_TAG, "Refresh content "+isFocused());
    }

    private String getDate(int xIndex) {
       return xLabels.get(xIndex);
    }





    @Override
    public int getXOffset(float xpos) {
        // this will center the marker-view horizontally
        if(xpos < min_offset)
            return 0;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        if(metrics.widthPixels - xpos < min_offset)
            return -getWidth();
        return -(getWidth() / 2);
    }

    @Override
    public int getYOffset(float ypos) {
        // this will cause the marker-view to be above the selected value
        return -getHeight();
    }
}