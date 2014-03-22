
package com.huewu.pla.lib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.huewu.pla.R;
import com.huewu.pla.lib.internal.PLA_ListView;

/**
 * @author Chen.Hui
 * @date 2014-03-21
 */
public class CrossMultiColumnListView extends PLA_ListView {
    
    @SuppressWarnings("unused")
    private static final String TAG = "CrossMultiColumnListView";
    
    private static final int DEFAULT_COLUMN_NUMBER = 2;

    private int mColumnNumber = DEFAULT_COLUMN_NUMBER;
    private Column[] mColumns = null;
    private Column mFixedColumn = null; //column for footers & headers.

    private Rect mFrameRect = new Rect();

    public CrossMultiColumnListView(Context context) {
        super(context);
        init(null);
    }

    public CrossMultiColumnListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CrossMultiColumnListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        getWindowVisibleDisplayFrame(mFrameRect );

        if( attrs == null ){
            mColumnNumber = DEFAULT_COLUMN_NUMBER;  //default column number is 2.
        }else{
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MultiColumnListView);

            int landColNumber = a.getInteger(R.styleable.MultiColumnListView_plaLandscapeColumnNumber, -1);
            int defColNumber = a.getInteger(R.styleable.MultiColumnListView_plaColumnNumber, -1);

            if(mFrameRect.width() > mFrameRect.height() && landColNumber != -1 ){
                mColumnNumber = landColNumber;
            }else if(defColNumber != -1){
                mColumnNumber = defColNumber;
            }else{
                mColumnNumber = DEFAULT_COLUMN_NUMBER;
            }
            
            a.recycle();
        }

        mColumns = new Column[mColumnNumber];
        for( int i = 0; i < mColumnNumber; ++i )
            mColumns[i] = new Column(i);

        mFixedColumn = new FixedColumn();
    }

    // /////////////////////////////////////////////////////////////////////
    // Override Methods...
    // /////////////////////////////////////////////////////////////////////
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // TODO the adapter status may be changed. what should i do here...
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = (getMeasuredWidth() - mListPadding.left - mListPadding.right) / mColumnNumber;

        for( int index = 0; index < mColumnNumber; ++ index ){
            mColumns[index].mColumnWidth = width;
            mColumns[index].mColumnLeft = mListPadding.left + width * index;
        }

        mFixedColumn.mColumnLeft = mListPadding.left;
        mFixedColumn.mColumnWidth = getMeasuredWidth();
    }

    @Override
    protected void onMeasureChild(View child, int position, int widthMeasureSpec,
            int heightMeasureSpec) {
        // super.onMeasureChild(child, position, widthMeasureSpec,
        // heightMeasureSpec);
        if (isFixedView(child))
            child.measure(widthMeasureSpec, heightMeasureSpec);
        else
            child.measure(MeasureSpec.EXACTLY | getItemWidth(position), heightMeasureSpec);
    }

    @Override
    protected int modifyFlingInitialVelocity(int initialVelocity) {
        return initialVelocity;
    }

    @Override
    protected void onItemAddedToList(int position, boolean flow) {
        super.onItemAddedToList(position, flow);

        if( isHeaderOrFooterPosition(position) == false){
            int span = getItemColumnSpanAtPosition(position);
            Column col = getColumn( flow, position ,span);
            //add state to columns
            int colIndex = col.getIndex();
            ItemState itemState = new ItemState(position, colIndex, span);
            for (int i = colIndex; i < mColumns.length; i++) {
                if (i >= colIndex + span) {
                    break;
                }
//                mColumns[i].mItems.put(position, itemState);
                mColumns[i].mItems.append(position, itemState);
            }
        }
    }

    @Override
    protected void onLayoutSync(int syncPos) {
        for( Column c : mColumns ){
            c.save();
        }
    }

    @Override
    protected void onLayoutSyncFinished(int syncPos) {
        for( Column c : mColumns ){
            c.clear();
        }
    }

    @Override
    protected void onAdjustChildViews(boolean down) {
        
        int firstItem = getFirstVisiblePosition();
        if( down == false && firstItem == 0){
            final int firstColumnTop = mColumns[0].getTop();
            for( Column c : mColumns ){
                final int top = c.getTop();
                //align all column's top to 0's column.
                c.offsetTopAndBottom( firstColumnTop - top );
            }
        }
        super.onAdjustChildViews(down);
    }





    @Override
    protected int getScrollChildBottom() {
        //return largest bottom value.
        //for checking scrolling region...
        int result = Integer.MIN_VALUE;
        for(Column c : mColumns){
            int bottom = c.getBottom();
            result = result < bottom ? bottom : result;
        }
        return result;
    }

    @Override
    protected int getScrollChildTop() {
        //find largest column.
        int result = Integer.MAX_VALUE;
        for(Column c : mColumns){
            int top = c.getTop();
            result = result > top ? top : result;
        }
        return result;
    }

    @Override
    protected int getItemLeft(int pos) {
        if( isHeaderOrFooterPosition(pos) )
            return super.getItemLeft(pos);
        
        return getColumnLeft(pos);
    }
    
    @Override
    protected int getFillChildBottom() {
        //return smallest bottom value.
        //in order to determine fill down or not... (calculate below space)
        int result = Integer.MAX_VALUE;
        for(Column c : mColumns){
            int bottom = c.getBottom();
            result = result > bottom ? bottom : result;
        }
        return result;
    }

    @Override
    protected int getItemTop(int pos) {
        if( isHeaderOrFooterPosition(pos) )
            return super.getItemTop(pos);
        int span = getItemColumnSpanAtPosition(pos);
        Column col = getColumn(true, pos, span);
        if (col != null) {
            if (span > 1) {
                int start = col.getIndex();
                for (int i = start; i < start + span; i++) {
                    col = col.getBottom() < mColumns[i].getBottom() ? mColumns[i] : col;
                }
            }
            return col.getBottom();
        }
        return 0;
    }
    
    @Override
    protected int getFillChildTop() {
        //find largest column.
        int result = Integer.MIN_VALUE;
        for(Column c : mColumns){
            int top = c.getTop();
            result = result < top ? top : result;
        }
        return result;
    }

    @Override
    protected int getItemBottom(int pos) {
        if( isHeaderOrFooterPosition(pos) )
            return super.getItemTop(pos);
        int span = getItemColumnSpanAtPosition(pos);
        Column col = getColumn(false, pos, span);
        if(col != null){
            if (span > 1) {
                int start = col.getIndex();
                for (int i = start; i < start + span; i++) {
                    col = col.getTop() > mColumns[i].getTop() ? mColumns[i] : col;
                }
            }
            return col.getTop();
        }
        return 0;
    }
    
    

    // ////////////////////////////////////////////////////////////////////////////
    // Private Methods...
    // ////////////////////////////////////////////////////////////////////////////

    private int getColumnLeft(int pos) {
        int span = getItemColumnSpanAtPosition(pos);
        Column col = getColumn(true,pos,span);
        if(col!=null){
            return col.getColumnLeft();
        }
        
        int colIndex = -1;

        for (Column column : mColumns) {
            ItemState itemState = column.get(pos);
            if (itemState != null) {
                colIndex = column.getIndex();
                break;
            }
        }

        if (colIndex == -1)
            return 0;

        return mColumns[colIndex].getColumnLeft();
    }

    private Column getColumn(boolean flow, int position, int span) {
        ItemState item = null;
        Column minTopColumn = mColumns[0],minBottomColumn = mColumns[0];
        for (Column column : mColumns) {
            item = column.get(position);
            if (item != null)
                break;
            if(minTopColumn.getTop() > column.getTop()){
                minTopColumn = column;
            }
            if(minBottomColumn.getBottom() > column.getBottom()){
                minBottomColumn = column;
            }
        }
        if (item != null){
            return mColumns[item.mColIndex];
        }
        Column column = flow ? minBottomColumn : minTopColumn;
        if (column.getIndex() + span > mColumnNumber) {
            return mColumns[0];
        }
        return column;
    }

    
    private boolean isHeaderOrFooterPosition( int pos ){
        int type = mAdapter.getItemViewType(pos);
        return type == ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }
    
    private int getItemColumnSpanAtPosition(int pos) {
        int type = mAdapter.getItemViewType(pos);
        int span = 1;
        if(type == ITEM_VIEW_TYPE_COLUMN_SPAN_ALL)
            span = mColumnNumber;
        else if (type <= ITEM_VIEW_TYPE_COLUMN_SPAN_ONE){
            span = type / ITEM_VIEW_TYPE_COLUMN_SPAN_ONE;
        }
        if (span > 1) {
            return span;
        }
        return 1;
    }

    private int getItemWidth(int position) {
        ItemState item = null;
        for (Column column : mColumns) {
            item = column.get(position);
            if (item != null)
                break;
        }
        if (item == null){
            return 0;
        }
        return mColumns[item.mColIndex].mColumnWidth * item.mSpan;
    }

    // /////////////////////////////////////////////////////////////
    // Inner Class.
    // /////////////////////////////////////////////////////////////

    private class ItemState {

        private int mPosition;

        private int mColIndex;

        private int mSpan;
        
        private int mViewType;

        private int mMarginBottom;

        private int mMarginTop;

        public ItemState(int pos, int index, int span) {
            mPosition = pos;
            mColIndex = index;
            mSpan = span;
        }

        public int getPosition() {
            return mPosition;
        }

        public int getColumnIndex() {
            return mColIndex;
        }

        public int getSpan() {
            return mSpan;
        }

    }

    private class Column {
        
        private SparseArray<ItemState> mItems = new SparseArray<ItemState>();
        private int mIndex;
        private int mColumnWidth;
        private int mColumnLeft;
        private int mSynchedTop;
        private int mSynchedBottom;

        
        public Column(int index) {
            mIndex = index;
        }

        public void save() {
            mSynchedTop = 0;
            mSynchedBottom = getTop(); //getBottom();
        }

        public void clear() {
            mSynchedTop = 0;
            mSynchedBottom = 0;
        }

        public void offsetTopAndBottom(int offset) {
            if( offset == 0 )
                return; 

            //find biggest value.
            int childCount = getChildCount();

            for( int index = 0; index < childCount; ++index ){
                View v = getChildAt(index);

                int viewLeft = v.getLeft();
                int viewRight = v.getRight();
                if (isFixedView(v)
                        || ((viewLeft >= getLeft() && viewLeft < getRight()) || (viewRight > getLeft() && viewRight <= getRight()))) {

                v.offsetTopAndBottom(offset);
                }
            }
        }

        public int getColumnLeft() {
            return mColumnLeft;
        }

        public int getColumnWidth() {
            return mColumnWidth;
        }

        public int getIndex() {
            return mIndex;
        }

        public int getLeft() {
            return mColumnLeft;
        }

        public int getRight() {
            return mColumnLeft + mColumnWidth;
        }

        public int getTop() {
            // find smallest value.
            int top = Integer.MAX_VALUE;
            int childCount = getChildCount();
            for (int index = 0; index < childCount; ++index) {
                View v = getChildAt(index);
                int viewLeft = v.getLeft();
                int viewRight = v.getRight();
                if (isFixedView(v)
                        || ((viewLeft >= getLeft() && viewLeft < getRight()) || (viewRight > getLeft() && viewRight <= getRight()))) {
                    top = top > v.getTop() ? v.getTop() : top;
                }
            }

            if (top == Integer.MAX_VALUE)
                return 0; // no child for this column. 
            return top;
        }

        public int getBottom() {
            // find biggest value.
            int bottom = Integer.MIN_VALUE;
            int childCount = getChildCount();

            for (int index = 0; index < childCount; ++index) {
                View v = getChildAt(index);
                int viewLeft = v.getLeft();
                int viewRight = v.getRight();
                if (isFixedView(v)
                        || ((viewLeft >= getLeft() && viewLeft < getRight()) || (viewRight > getLeft() && viewRight <= getRight()))) {
                    bottom = bottom < v.getBottom() ? v.getBottom() : bottom;
                }
            }

            if (bottom == Integer.MIN_VALUE)
                return mSynchedBottom; // no child for this column..
            return bottom;
        }
        
        public ItemState get(int position){
            return mItems.get(position);
        }

    }
    
    private class FixedColumn extends Column {

        public FixedColumn() {
            super(Integer.MAX_VALUE);
        }

        @Override
        public int getBottom() {
            return getScrollChildBottom();
        }

        @Override
        public int getTop() {
            return getScrollChildTop();
        }

    }//end of class

}
