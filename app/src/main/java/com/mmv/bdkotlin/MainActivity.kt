package com.mmv.bdkotlin

import android.app.LoaderManager
import android.content.Loader
import android.database.Cursor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*;
import android.content.ContentUris
import android.content.CursorLoader
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import com.mmv.bdkotlin.data.BookContract.BookEntry


class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for the book data loader  */
    private val BOOK_LOADER = 0

    /** Adapter for the ListView  */
    private lateinit var mCursorAdapter: BookCursorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup FAB to open EditorActivity
        fab.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                val intent = Intent(this@MainActivity, EditorActivity::class.java)
                startActivity(intent)
            }
        })


        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        list.setEmptyView(empty_view)

        // Setup an Adapter to create a list item for each row of book data in the Cursor.
        // There is no book data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = BookCursorAdapter(this, null)
        list.setAdapter(mCursorAdapter)

        // Setup the item click listener
        list.setOnItemClickListener(AdapterView.OnItemClickListener { adapterView, view, position, id ->
            // Create new intent to go to {@link EditorActivity}
            val intent = Intent(this@MainActivity, EditorActivity::class.java)

            // Form the content URI that represents the specific book that was clicked on,
            // by appending the "id" (passed as input to this method) onto the
            // {@link BookEntry#CONTENT_URI}.
            val currentBookUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, id)

            // Set the URI on the data field of the intent
            intent.data = currentBookUri

            // Launch the {@link EditorActivity} to display the data for the current book.
            startActivity(intent)
        })

        // Kick off the loader
        loaderManager.initLoader(BOOK_LOADER, null, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        menuInflater.inflate(R.menu.menu_catalog, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // User clicked on a menu option in the app bar overflow menu
        when (item.getItemId()) {
            R.id.action_delete_all_entries -> {
                deleteAllBooks()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        // Define a projection that specifies the columns from the table we care about.
        val projection = arrayOf<String>(BookEntry._ID, BookEntry.COLUMN_BOOK_TITLE)

        // This loader will execute the ContentProvider's query method on a background thread
        return CursorLoader(this, // Parent activity context
                BookEntry.CONTENT_URI, // Provider content URI to query
                projection, // No selection arguments
                null, // Columns to include in the resulting Cursor
                null,// No selection clause
                null)// Default sort order
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        // Update {@link BookCursorAdapter} with this new cursor containing updated book data
        mCursorAdapter.swapCursor(data);
    }

    override fun onLoaderReset(p0: Loader<Cursor>?) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Helper method to delete all books in the database.
     */
    private fun deleteAllBooks() {
        contentResolver.delete(BookEntry.CONTENT_URI, null, null)
    }
}
