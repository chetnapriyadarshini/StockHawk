package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.os.ResultReceiver;
import android.text.format.Time;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.DetailGraphActivity;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;


  public static ArrayList quoteJsonToContentVals(ResultReceiver resultReceiver,String JSON){

    final String OWM_QUERY = "query";
    final String OWM_COUNT = "count";
    final String OWM_RESULTS = "results";
    final String OWM_QUOTE = "quote";

    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(OWM_QUERY);
        int count = Integer.parseInt(jsonObject.getString(OWM_COUNT));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject(OWM_RESULTS)
              .getJSONObject(OWM_QUOTE);
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject(OWM_RESULTS).getJSONArray(OWM_QUOTE);

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }catch (NumberFormatException e)
    {
        Log.d(LOG_TAG, "Caught number format excsptionnnnnnnnnn");
        e.printStackTrace();
        if(resultReceiver != null)
            resultReceiver.send(MyStocksActivity.RESULT_INVALID_STOCK, null);
        else
            Log.d(LOG_TAG, "NO result receiverrr for this exception resultttt");
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.Quotes.CONTENT_URI);

    final String OWM_CHANGE = "Change";
    final String OWM_PERCENT_CHANGE = "ChangeinPercent";
    final String OWM_SYMBOL = "symbol";
    final String OWM_BID_PRICE = "Bid";
    final String OWM_NAME = "Name";

    try {
      String change = jsonObject.getString(OWM_CHANGE);
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(OWM_SYMBOL));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString(OWM_BID_PRICE)));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString(OWM_PERCENT_CHANGE), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }
      builder.withValue(QuoteColumns.NAME, jsonObject.getString(OWM_NAME));

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }

  public static void addData(ResultReceiver resultReceiver, String response) {

    final String OWM_QUERY = "query";
    final String OWM_RESULTS = "results";
    final String OWM_QUOTE = "quote";
    final String OWM_CLOSE = "Close";
    final String OWM_DATE = "Date";

    ArrayList<Entry> closingVals = new ArrayList<>();
    ArrayList<String> closingDates = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(response);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(OWM_QUERY);
        Bundle resultData = new Bundle();
          resultsArray = jsonObject.getJSONObject(OWM_RESULTS).getJSONArray(OWM_QUOTE);
          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = resultsArray.length()-1; i >= 0 ; i--){
              JSONObject obj  = resultsArray.getJSONObject(i);
              // Log.d(LOG_TAG , "X INDEXX: "+(resultsArray.length() -1 -i));
               Entry entry = new Entry((float) obj.getDouble(OWM_CLOSE),(resultsArray.length() - i));
               closingVals.add(entry);
               closingDates.add((obj.getString(OWM_DATE)));
              // closingVals[i] = (float) obj.getDouble(OWM_CLOSE);
            }
          }
            resultData.putParcelableArrayList(DetailGraphActivity.CLOSING_VALUES,closingVals);
            resultData.putStringArrayList(DetailGraphActivity.CLOSING_DATE_LABELS, closingDates);
           // resultData.putFloatArray(DetailGraphActivity.CLOSING_VALUES, closingVals);
            if(resultReceiver != null)
              resultReceiver.send(DetailGraphActivity.FETCH_SUCCESS, resultData);
            else
              Log.d(LOG_TAG, "Could not find any result reciever to send data!!!");
        }
      }
    catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }catch (NumberFormatException e)
    {
      Log.d(LOG_TAG, "Caught number format excsptionnnnnnnnnn");
      e.printStackTrace();
      if(resultReceiver != null)
        resultReceiver.send(MyStocksActivity.RESULT_INVALID_STOCK, null);
      else
        Log.d(LOG_TAG, "NO result receiverrr for this exception resultttt");
    }
  }

  public static @DetailGraphActivity.TrendSpan
  int getTrendSpan(int index) {

    switch(index)
    {
      case 0:
        return DetailGraphActivity.TREND_SPAN_ONE_MONTH;
      case 1:
        return DetailGraphActivity.TREND_SPAN_THREE_MONTHS;
      case 2:
        return DetailGraphActivity.TREND_SPAN_SIX_MONTHS;
      case 3:
        return DetailGraphActivity.TREND_SPAN_TWELVE_MONTHS;
    }
    return DetailGraphActivity.TREND_SPAN_ONE_MONTH;
  }

  public static String formatDate(long dateInMillis) {

    SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String formattedTime =  shortenedDateFormat.format(dateInMillis);
    Log.d(LOG_TAG, "Formatted END DATE: "+formattedTime);
    return formattedTime;
  }


  public static String getFormattedStartDate(@DetailGraphActivity.TrendSpan int trendSpan)
  {

    long dayMonthbefore = getStartDate(trendSpan);
    SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String formattedTime =  shortenedDateFormat.format(dayMonthbefore);
    Log.d(LOG_TAG,"Formatted Start Date: "+ formattedTime);
    return formattedTime;
  }

  public static long getStartDate(int trendSpan) {

    long currentTime = System.currentTimeMillis();
    Log.d(LOG_TAG, "CURRENT TIME MILLIS: "+currentTime);
    /*
    int julianDay = Time.getJulianDay(currentTime, time.gmtoff);
    int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);*/

    long dayMonthbefore = 0;
    long millis = 0;

    switch (trendSpan)
    {
      case DetailGraphActivity.TREND_SPAN_ONE_MONTH:
        Log.d(LOG_TAG, "Trend span 1 months numdays "+getNumDays(trendSpan));
        millis = TimeUnit.DAYS.toMillis(30);
        dayMonthbefore = currentTime - millis;
        break;

      case DetailGraphActivity.TREND_SPAN_THREE_MONTHS:
        Log.d(LOG_TAG, "Trend span 3 months numdays "+getNumDays(trendSpan));
        millis = TimeUnit.DAYS.toMillis(getNumDays(trendSpan));
        dayMonthbefore = currentTime - millis;
        break;

      case DetailGraphActivity.TREND_SPAN_SIX_MONTHS:
        millis = TimeUnit.DAYS.toMillis(getNumDays(trendSpan));
        dayMonthbefore = currentTime - millis;
        break;


      case DetailGraphActivity.TREND_SPAN_TWELVE_MONTHS:
        millis = TimeUnit.DAYS.toMillis(getNumDays(trendSpan));
        dayMonthbefore = currentTime - millis;
        break;
    }

      return dayMonthbefore;
  }


  private static long getNumDays(int trendSpan) {
    int[] daysInMonth = {31,28,31,30,31,30,31,31,30,31,30,31};
    boolean leapYear = false;
    long currentTime = System.currentTimeMillis();
    SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("MM");
    String month = shortenedDateFormat.format(currentTime);
    shortenedDateFormat = new SimpleDateFormat("yyyy");
    int year = Integer.parseInt(shortenedDateFormat.format(currentTime));
    Log.d(LOG_TAG, "YEAR: "+year);
    if(year%4 == 0)
    {
      Log.d(LOG_TAG, "This year is a leap year");
      leapYear = true;
    }
    int totMonths = 0;
    Log.d(LOG_TAG, "Month: "+month);
    switch (trendSpan)
    {
      case DetailGraphActivity.TREND_SPAN_ONE_MONTH:
        int currMonth = Integer.parseInt(month)-1;
        if(currMonth == 1 && leapYear)//Leap year has 29 days in Feb
          return 29;
        else
          return daysInMonth[currMonth];

      case DetailGraphActivity.TREND_SPAN_THREE_MONTHS:
        //return 30 * 3;
       totMonths = 3;
        break;

      case DetailGraphActivity.TREND_SPAN_SIX_MONTHS:
      //  return 30 * 6;
        totMonths = 6;
        break;
/*
      case DetailGraphActivity.TREND_SPAN_NINE_MONTHS:
        totMonths = 9;
        break;*/

      case DetailGraphActivity.TREND_SPAN_TWELVE_MONTHS:
     //   return 365;
        totMonths = 12;
        break;
    }

    int currentMonth =  Integer.parseInt(month);
    Log.d(LOG_TAG, "Current Month: "+currentMonth+" Total Months: "+totMonths);
    int startIndex = currentMonth;
    if(currentMonth - totMonths < 0)
    {
      startIndex = 12 - (totMonths - currentMonth)-1;
    }
    Log.d(LOG_TAG, "startIndex: "+startIndex);
    int totDays = 0;
    for(int i = 0 ; i< totMonths; i++)
    {
      if(leapYear && startIndex == 1)
        totDays = totDays + 29;
      else
        totDays = totDays + daysInMonth[startIndex];
      startIndex++;
      if(startIndex > daysInMonth.length - 1)
        startIndex = 0;
    }
    Log.d(LOG_TAG, "Total days: "+totDays);
    return totDays;
  }
}

