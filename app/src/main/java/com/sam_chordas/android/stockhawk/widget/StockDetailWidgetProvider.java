package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.DetailGraphActivity;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by chetna_priya on 5/12/2016.
 */
public class StockDetailWidgetProvider extends AppWidgetProvider {

    private static final String LOG_TAG = StockDetailWidgetProvider.class.getSimpleName();
    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;
    private static StockDataObserver sDataObserver;

    public StockDetailWidgetProvider()
    {
        sWorkerThread = new HandlerThread("StockWidgetProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(MyStocksActivity.ACTION_DATA_UPDATED)) {
            Log.d(LOG_TAG, "ACTION_DATA_UPDATED RECEIVED Start Remote Views Service NOWWWWWWWWW");
            context.startService(new Intent(context, StockDetailRemoteViewsService.class));
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(LOG_TAG, "UPDATEEEEEE APP WIDGETTTTTTTTTTTT");
        int N= appWidgetIds.length;
        for(int i=0;i<N; i++)
        {
            RemoteViews layout = buildLayout(context,appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds,layout);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private RemoteViews buildLayout(Context context, int appWidgetId) {
        final Intent intent = new Intent(context, StockDetailRemoteViewsService.class);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_detail);
        rv.setRemoteAdapter(R.id.stock_list, intent);

        final Intent onClickIntent = new Intent(context, DetailGraphActivity.class);
        final PendingIntent onClickPendingIntent = PendingIntent.getActivity(context, 0,
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.stock_list, onClickPendingIntent);
        return rv;
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        ContentResolver r = context.getContentResolver();
        if(sDataObserver == null)
        {
            sDataObserver = new StockDataObserver(AppWidgetManager.getInstance(context),
                    new ComponentName(context, StockDetailWidgetProvider.class),
                    sWorkerQueue);
        }
        r.registerContentObserver(QuoteProvider.Quotes.CONTENT_URI, true, sDataObserver);
    }

    class StockDataObserver extends ContentObserver
    {
        private AppWidgetManager mAppWidgetManager;
        private ComponentName mComponentName;

        public StockDataObserver(AppWidgetManager manager, ComponentName cn, Handler handler) {
            super(handler);
            mAppWidgetManager = manager;
            mComponentName = cn;
        }

        @Override
        public void onChange(boolean selfChange) {
            mAppWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetManager.getAppWidgetIds(mComponentName),
                    R.id.stock_list);
        }
    }
}
