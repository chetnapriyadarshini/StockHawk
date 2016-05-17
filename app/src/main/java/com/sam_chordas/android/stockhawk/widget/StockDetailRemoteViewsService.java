package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;


import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Created by chetna_priya on 5/12/2016.
 */
public class StockDetailRemoteViewsService extends RemoteViewsService {
    private static final String LOG_TAG = StockDetailRemoteViewsService.class.getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    private class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private Cursor mCursor;

        public ListRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;

        }

        @Override
        public void onCreate() {
            Log.d(LOG_TAG, "ON CREATEEEEEEEEEEEEEEEEEE");
        }

        @Override
        public void onDataSetChanged() {
            Log.d(LOG_TAG, "ON DATA SET CHANGEDDDDDDDDDDDDDDDD");
            if(mCursor != null)
                mCursor.close();
            mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
        }

        @Override
        public void onDestroy() {
            if(mCursor != null)
                mCursor.close();
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {

            final Intent fillInIntent = new Intent();
            final int itemId = R.layout.widget_list_item;
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), itemId);
            remoteViews.setEmptyView(R.id.item_stock, R.id.stock_list_empty_view);
            if(mCursor.moveToPosition(position))
            {
                Log.d(LOG_TAG, "FILL IN THE VIEW AT POSITION: "+position);
                remoteViews.setTextViewText(R.id.stock_symbol,mCursor.getString(QuoteColumns.COL_INDEX_SYMBOL));
                remoteViews.setTextViewText(R.id.bid_price,mCursor.getString(QuoteColumns.COL_INDEX_BIDPRICE));

                boolean isUp = false;
                int change;
                if (mCursor.getInt(QuoteColumns.COL_INDEX_ISUP) == QuoteColumns.IS_UP) {
                    remoteViews.setViewVisibility(R.id.change_neg, View.GONE);
                    remoteViews.setViewVisibility(R.id.change_pos, View.VISIBLE);
                    change = R.id.change_pos;
                    //    remoteViews.setImageViewResource(R.id.change,R.drawable.percent_change_pill_green);
                    isUp = true;

                } else {
                    remoteViews.setViewVisibility(R.id.change_pos, View.GONE);
                    remoteViews.setViewVisibility(R.id.change_neg, View.VISIBLE);
                    change = R.id.change_neg;
                 //   remoteViews.setImageViewResource(R.id.change,R.drawable.percent_change_pill_red);
                }
                String text;

                if (Utils.showPercent) {
                    text = mCursor.getString(QuoteColumns.COL_INDEX_PERCENT_CHANGE);
                } else {
                    text = mCursor.getString(QuoteColumns.COL_INDEX_CHANGE);
                }
                Log.d(LOG_TAG, "CHANGEEEEEEEEE: "+text);
                remoteViews.setTextViewText(change,text);

                String name = mCursor.getString(QuoteColumns.COL_INDEX_NAME);
                Log.d(LOG_TAG, "NAMEEEEEEEE OF COMP "+name);

                if (isUp)
                    remoteViews.setContentDescription(change, mContext.getString(R.string.stock_price)+" "
                            +name +mContext.getString(R.string.up)+text);
                else
                    remoteViews.setContentDescription(change, mContext.getString(R.string.stock_price)+" "
                            +name +mContext.getString(R.string.down)+text);

            }
            fillInIntent.putExtra(QuoteColumns.SYMBOL, mCursor.getString(QuoteColumns.COL_INDEX_SYMBOL));
            remoteViews.setOnClickFillInIntent(R.id.item_stock, fillInIntent);
            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
