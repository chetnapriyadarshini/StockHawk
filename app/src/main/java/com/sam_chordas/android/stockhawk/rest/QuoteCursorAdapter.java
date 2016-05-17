package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by sam_chordas on 10/6/15.
 *  Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
    implements ItemTouchHelperAdapter{

  private static final String LOG_TAG = QuoteCursorAdapter.class.getSimpleName();
  private static Context mContext;
  private static Typeface robotoLight;

  @BindDrawable(R.drawable.percent_change_pill_green)
  Drawable greenPill;

  @BindDrawable(R.drawable.percent_change_pill_red)
  Drawable redPill;


  public QuoteCursorAdapter(Context context, Cursor cursor){
    super(context, cursor);
    mContext = context;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
    robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_item_quote, parent, false);
    ViewHolder vh = new ViewHolder(itemView);
    ButterKnife.bind(this, itemView);
    return vh;
  }

  @Override
  public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
    viewHolder.symbol.setText(cursor.getString(QuoteColumns.COL_INDEX_SYMBOL));
    viewHolder.bidPrice.setText(cursor.getString(QuoteColumns.COL_INDEX_BIDPRICE));
    boolean isUp = false;
    if (cursor.getInt(QuoteColumns.COL_INDEX_ISUP) == QuoteColumns.IS_UP) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        viewHolder.change.setBackgroundDrawable(greenPill);
      } else {
        viewHolder.change.setBackground(greenPill);
        isUp = true;
      }

    } else {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        viewHolder.change.setBackgroundDrawable(redPill);
      } else {
        viewHolder.change.setBackground(redPill);
      }
    }
    if (Utils.showPercent) {
      viewHolder.change.setText(cursor.getString(QuoteColumns.COL_INDEX_PERCENT_CHANGE));
    } else {
      viewHolder.change.setText(cursor.getString(QuoteColumns.COL_INDEX_CHANGE));
    }

    String name = cursor.getString(QuoteColumns.COL_INDEX_NAME);

    if (isUp)
      viewHolder.change.setContentDescription(mContext.getString(R.string.stock_price)+" "
              +name +mContext.getString(R.string.up)+viewHolder.change.getText());
    else
      viewHolder.change.setContentDescription(mContext.getString(R.string.stock_price)+" "
              + name +mContext.getString(R.string.down)+viewHolder.change.getText());
    Log.d(LOG_TAG, "DESCRIPTION: "+viewHolder.change.getContentDescription());
  }

  @Override public void onItemDismiss(int position) {
    Cursor c = getCursor();
    c.moveToPosition(position);
    String symbol = c.getString(QuoteColumns.COL_INDEX_SYMBOL);
    mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
    notifyItemRemoved(position);
  }



  @Override public int getItemCount() {
    return super.getItemCount();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder
      implements ItemTouchHelperViewHolder, View.OnClickListener{

      @BindView(R.id.stock_symbol) TextView symbol;
      @BindView(R.id.change) TextView change;
      @BindView(R.id.bid_price) TextView bidPrice;

    public ViewHolder(View itemView){
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    @Override
    public void onItemSelected(){
      itemView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onItemClear(){
      itemView.setBackgroundColor(0);
    }

    @Override
    public void onClick(View v) {

    }
  }
}
