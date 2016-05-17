package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.DetailGraphActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
  private static final String QUOTES_URL = "select * from yahoo.finance.quotes where symbol "
          + "in (";
  private static final String TREND_URL = "select * from yahoo.finance.historicaldata where ";
  private static final String CHARSET = "UTF-8";
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;
  private ResultReceiver resultReceiver;

    public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    boolean isDetail = false;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append(BASE_URL);
      if(params.getTag().equals(mContext.getString(R.string.detail_tag)))
      {
        urlStringBuilder.append(URLEncoder.encode(TREND_URL, CHARSET));
      }else
      {
        urlStringBuilder.append(URLEncoder.encode(QUOTES_URL, CHARSET));
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals(mContext.getString(R.string.init_tag)) ||
            params.getTag().equals(mContext.getString(R.string.periodic_tag))){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex(mContext.getString(R.string.symbol_key)))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    }
    else if (params.getTag().equals(mContext.getString(R.string.add_tag))){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      Bundle extras = params.getExtras();
      if(extras.getParcelable(mContext.getString(R.string.result_receiver))!= null)
      {
          Log.d(LOG_TAG, "INITIALIZE RESULT RECEIVERRRRRRR");
          resultReceiver = extras.getParcelable(mContext.getString(R.string.result_receiver));
      }
      String stockInput = params.getExtras().getString(mContext.getString(R.string.symbol_key));
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    } else if(params.getTag().equals(mContext.getString(R.string.detail_tag)))
    {
      Bundle extras = params.getExtras();
      if(extras.getParcelable(mContext.getString(R.string.result_receiver))!= null)
      {
        Log.d(LOG_TAG, "INITIALIZE RESULT RECEIVERRRRRRR");
        resultReceiver = extras.getParcelable(mContext.getString(R.string.result_receiver));
      }
      isUpdate = false;
      isDetail = true;
      //String stockInput = params.getExtras().getString(mContext.getString(R.string.symbol_key));
      String symb = extras.getString(QuoteColumns.SYMBOL);
      int trendSpan = Utils.getTrendSpan(extras.getInt(DetailGraphActivity.TREND_SPAN));
      Log.d(LOG_TAG, "TREND SPANNN :"+trendSpan+extras.getInt(DetailGraphActivity.TREND_SPAN));
      String startDate = Utils.getFormattedStartDate(trendSpan);
      String endDate = Utils.formatDate(System.currentTimeMillis());

      try
      {
        urlStringBuilder.append(URLEncoder.encode("symbol = "+"\""+symb+"\"", "UTF-8"));
        urlStringBuilder.append(URLEncoder.encode(" and startDate = "+"\""+startDate+"\"", "UTF-8"));
        urlStringBuilder.append(URLEncoder.encode(" and endDate = "+"\""+endDate+"\"", "UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {
        e.printStackTrace();
      }
      Log.d(LOG_TAG, "FETCH DETAILLLLLLLLLLLLLLLLLLLLL");
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
        Log.d(LOG_TAG, urlString);
      try{
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }
                Log.d(LOG_TAG, "RESPONSE >>>>> " + getResponse + " IS UPDATING>>>>> " + isUpdate);
          if(isDetail)
          {
            Utils.addData(resultReceiver, getResponse);
          }else
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                  Utils.quoteJsonToContentVals(resultReceiver, getResponse));
        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      } catch (IOException e){
        e.printStackTrace();
      }
    }

    return result;
  }

}
