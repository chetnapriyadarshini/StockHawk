package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.DetailGraphActivity;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {


    private static final String LOG_TAG = StockIntentService.class.getSimpleName();

    public StockIntentService(){
    super(StockIntentService.class.getName());
  }

  public StockIntentService(String name) {
    super(name);
  }

  @Override protected void onHandleIntent(Intent intent) {
      Context context = getApplicationContext();
      Log.d(LOG_TAG, "Context------------"+context);
    Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
    StockTaskService stockTaskService = new StockTaskService(this);
    Bundle args = new Bundle();
    if (intent.getStringExtra(context.getString(R.string.tag_key)).equals(context.getString(R.string.add_tag))){
      args.putString(context.getString(R.string.symbol_key), intent.getStringExtra(context.getString(R.string.symbol_key)));
        if(intent.getParcelableExtra(getString(R.string.result_receiver)) != null)
        {
            args.putParcelable(context.getString(R.string.result_receiver),
                    intent.getParcelableExtra(getString(R.string.result_receiver)));
        }
    }

      if (intent.getStringExtra(context.getString(R.string.tag_key)).equals(context.getString(R.string.detail_tag))){
          args.putString(context.getString(R.string.symbol_key), intent.getStringExtra(context.getString(R.string.symbol_key)));

          if(intent.getParcelableExtra(getString(R.string.result_receiver)) != null)
          {
              args.putParcelable(context.getString(R.string.result_receiver),
                      intent.getParcelableExtra(getString(R.string.result_receiver)));
          }
          args.putInt(DetailGraphActivity.TREND_SPAN, intent.getIntExtra(DetailGraphActivity.TREND_SPAN, 0));
      }

    // We can call OnRunTask from the intent service to force it to run immediately instead of
    // scheduling a task.
    stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(context.getString(R.string.tag_key)), args));
  }

}
