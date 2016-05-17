package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailGraphActivity extends AppCompatActivity implements ActionBar.OnNavigationListener {

    public static final String CLOSING_VALUES = "closing_values";
    public static final int FETCH_SUCCESS = 3;
    private static final String LOG_TAG = DetailGraphActivity.class.getSimpleName();
    public  static final String TREND_SPAN = "trend_span";
    public static final String CLOSING_DATE_LABELS = "closing_date_labels";
    private static final String SELECTED_TREND_SPAN = "selected_trend_span";

    private LineChart mChart;
    private Intent mServiceIntent;
    private ResultReceiver mResultReceiver;
    private static Bundle resultData;
    private static @TrendSpan int selected_trend_index = 0;
    private Cursor mCursor;
    private static String symbol;

    @IntDef({TREND_SPAN_ONE_MONTH, TREND_SPAN_THREE_MONTHS, TREND_SPAN_SIX_MONTHS,
             TREND_SPAN_TWELVE_MONTHS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TrendSpan{}


    public static final int TREND_SPAN_ONE_MONTH = 0;
    public static final int TREND_SPAN_THREE_MONTHS = 1;
    public static final int TREND_SPAN_SIX_MONTHS = 2;
    public static final int TREND_SPAN_TWELVE_MONTHS = 3;

    private static String[] month_arr;

    @BindView(R.id.stock_name)
    TextView stockName;

    @BindView(R.id.stock_info)
    TextView stockInfo;

    @BindView(R.id.bid_price)
    TextView bidPrice;

    @BindView(R.id.change)
    TextView stockPriceChange;

    @BindView(R.id.percent_change)
    TextView stockPricePercentChange;

    @BindView(R.id.date_range)
    TextView date_range;

    @BindString(R.string.stock_excahnge)
    String stockExchange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail_graph);
        ButterKnife.bind(this);
        mChart = (LineChart) findViewById(R.id.chart);
        mChart.setNoDataText(getString(R.string.fetch_data));
        month_arr = getResources().getStringArray(R.array.months);
        mResultReceiver = new MyReceiver();
        mChart = (LineChart) findViewById(R.id.chart);
        symbol = getIntent().getStringExtra(QuoteColumns.SYMBOL);
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
           fetchData();
        }else{
            setData(resultData);
        }

        mCursor = getContentResolver().
                query(QuoteProvider.Quotes.withSymbol(getIntent().getStringExtra(QuoteColumns.SYMBOL)),
                        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{String.valueOf(QuoteColumns.IS_CURRENT)},
                        null);

        if(mCursor.moveToFirst())
            setupTextFields();

        setupActionBar();

    }

    private void setupTextFields() {
        String name = mCursor.getString(QuoteColumns.COL_INDEX_NAME);
        stockName.setText(name);
        stockName.setContentDescription(name);
        String stock_info = String.format(stockExchange,mCursor.getString(QuoteColumns.COL_INDEX_SYMBOL));
        stockInfo.setText(stock_info);
        stockInfo.setContentDescription(stock_info);
        bidPrice.setText(mCursor.getString(QuoteColumns.COL_INDEX_BIDPRICE));
        bidPrice.setContentDescription(bidPrice.getText());
        stockPriceChange.setText(mCursor.getString(QuoteColumns.COL_INDEX_CHANGE));

        String percentChange = mCursor.getString(QuoteColumns.COL_INDEX_PERCENT_CHANGE);
        percentChange = percentChange.replace(percentChange.charAt(0), '(').concat(")");
        stockPricePercentChange.setText(percentChange);


        if (mCursor.getInt(QuoteColumns.COL_INDEX_ISUP) == QuoteColumns.IS_UP) {
            stockPriceChange.setContentDescription(getString(R.string.stock_price) + " "
                    + name + getString(R.string.up) + stockPriceChange.getText());


            stockPricePercentChange.setContentDescription(getString(R.string.stock_price) + " "
                    + name + getString(R.string.up) + stockPricePercentChange.getText());

        } else {
            stockPriceChange.setContentDescription(getString(R.string.stock_price) + " "
                    + name + getString(R.string.down) + stockPriceChange.getText());


            stockPricePercentChange.setContentDescription(getString(R.string.stock_price) + " "
                    + name + getString(R.string.down) + stockPricePercentChange.getText());
        }


    }

    private String getFormattedDayRange(String startDate, String endDate)
    {
        String[] temp = startDate.split("-");
        String month = getMonthString(temp[1]);
        startDate = month+" "+temp[2]+", "+temp[0];

        temp = endDate.split("-");
        month = getMonthString(temp[1]);
        endDate = month+" "+temp[2]+", "+temp[0];

        return startDate +" - "+endDate;
    }


    private void fetchData() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        mServiceIntent.putExtra(getString(R.string.tag_key), getString(R.string.detail_tag));
        mServiceIntent.putExtra(QuoteColumns.SYMBOL, symbol);
        mServiceIntent.putExtra(TREND_SPAN, selected_trend_index);
        mServiceIntent.putExtra(getString(R.string.result_receiver), mResultReceiver);
        if (isConnected) {
            startService(mServiceIntent);
        }
    }


    private void setupActionBar() {

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(symbol.toUpperCase());

        final String[] dropdownValues = getResources().getStringArray(R.array.trend_span);

        // Specify a SpinnerAdapter to populate the dropdown list.
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item, android.R.id.text1,
                dropdownValues);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(adapter, this);
        actionBar.setSelectedNavigationItem(selected_trend_index);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBundle(CLOSING_VALUES, resultData);
        outState.putInt(SELECTED_TREND_SPAN, selected_trend_index);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.containsKey(CLOSING_VALUES))
            resultData = savedInstanceState.getBundle(CLOSING_VALUES);
        if(savedInstanceState.containsKey(SELECTED_TREND_SPAN)) {
            selected_trend_index = Utils.getTrendSpan(savedInstanceState.getInt(SELECTED_TREND_SPAN));
        }
    }



    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition)
        {
            case TREND_SPAN_ONE_MONTH:
                selected_trend_index = TREND_SPAN_ONE_MONTH;
                break;

            case TREND_SPAN_THREE_MONTHS:
                selected_trend_index = TREND_SPAN_THREE_MONTHS;
                break;

            case TREND_SPAN_SIX_MONTHS:
                selected_trend_index = TREND_SPAN_SIX_MONTHS;
                break;

            case TREND_SPAN_TWELVE_MONTHS:
                selected_trend_index = TREND_SPAN_TWELVE_MONTHS;
                break;
        }

        getSupportActionBar().setSelectedNavigationItem(selected_trend_index);
      //  Log.d(LOG_TAG, "INDEX CHANGESSSSSS: "+selected_trend_index);

        mChart.invalidate();
        fetchData();
        return true;
    }

    private class MyReceiver extends ResultReceiver {

        public MyReceiver() {
            super(null);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.d(LOG_TAG, "RECEIVED RESULT CODE: "+resultCode+" DATA: "+resultData);
            if(resultCode == FETCH_SUCCESS)
            {
                setData(resultData);
            }
            super.onReceiveResult(resultCode, resultData);
        }
    }

    private void setData(Bundle resultData) {

        final String startDate = Utils.getFormattedStartDate(selected_trend_index);
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String endDate = shortenedDateFormat.format(System.currentTimeMillis());

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                date_range.setText(getFormattedDayRange(startDate, endDate));
            }
        });

        this.resultData = resultData;
        ArrayList<Entry> stock_closing_vals = resultData.getParcelableArrayList(CLOSING_VALUES);
        ArrayList<String> resultDates = resultData.getStringArrayList(CLOSING_DATE_LABELS);
        String[] temp;
        ArrayList<String> xLabels = new ArrayList<>();
        for(int i =0; i<stock_closing_vals.size() ; i++)
        {
            temp = resultDates.get(i).split("-");
         //   Log.d(LOG_TAG, "After SPlit: "+getMonthString(temp[1])+temp[2]);
            xLabels.add(i,getMonthString(temp[1])+temp[2]);
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        LineDataSet lineDataSet = new LineDataSet(stock_closing_vals, CLOSING_VALUES);
        dataSets.add(lineDataSet);
        LineData lineData = new LineData(xLabels, dataSets);
        final CustomMarkerView mv = new CustomMarkerView(this, R.layout.custom_marker_view_layout, xLabels);
        String formattedContentDesc = String.format(getString(R.string.stock_trend_graph),stockName.getText(),
                startDate, endDate);
        mChart.setContentDescription(formattedContentDesc);
        date_range.setContentDescription(mChart.getContentDescription());
        // set the marker to the chart
        mChart.setMarkerView(mv);
        mChart.setData(lineData);
        mChart.getAxisRight().setDrawLabels(false);
        mChart.fitScreen();
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTH_SIDED);
        lineDataSet.setDrawValues(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChart.invalidate();
            }
        });
    }

    private String getMonthString(String month) {
        return month_arr[Integer.parseInt(month)-1];
    }
}
