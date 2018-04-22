package com.mmv.bdkotlin

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import com.mmv.bdkotlin.data.BookContract


class BookCursorAdapter(context: Context?, c: Cursor?) : CursorAdapter(context, c) {
    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the book data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current book can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        // Find individual views that we want to modify in the list item layout
        val titleTextView: TextView = view!!.findViewById(R.id.title)

        // Find the columns of book attributes that we're interested in
        val columnIndex = cursor!!.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_TITLE)

        // Read the people attributes from the Cursor for the current book
        val bookTitle = cursor!!.getString(columnIndex)

        // Update the TextViews with the attributes for the current person
        titleTextView.text = bookTitle
    }
}