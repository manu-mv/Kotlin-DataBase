package com.mmv.bdkotlin

import android.app.AlertDialog
import android.app.LoaderManager
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mmv.bdkotlin.data.BookContract.BookEntry
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_editor.*;
import android.widget.AdapterView
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import android.content.ContentValues
import android.content.CursorLoader
import android.support.v4.app.NavUtils
import android.content.DialogInterface
import android.view.Menu
import android.view.MenuItem


/**
 * Allows user to create a new book or edit an existing one.
 */
class EditorActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    /** Identifier for the book data loader  */
    private val EXISTING_BOOK_LOADER = 0

    /** Content URI for the existing book (null if it's a new book)  */
    private var mCurrentBookUri: Uri? = null

    /**
     * Type of the book. The possible valid values are in the BookContract.kt file:
     * [BookEntry.TYPE_UNKNOWN], [BookEntry.TYPE_NOVEL], or
     * [BookEntry.TYPE_POETRY].
     */
    private var mType = BookEntry.TYPE_UNKNOWN

    /** Boolean flag that keeps track of whether the book has been edited (true) or not (false)  */
    private var mBookHasChanged = false

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mBookHasChanged boolean to true.
     */
    private val mTouchListener = object : View.OnTouchListener{
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            mBookHasChanged = true
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new book or editing an existing one.
        mCurrentBookUri = intent.data

        // If the intent DOES NOT contain a book content URI, then we know that we are
        // creating a new book.
        if (mCurrentBookUri == null) {
            // This is a new book, so change the app bar to say "Add a Book"
            title = getString(R.string.editor_activity_title_new)

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a book that hasn't been created yet.)
            invalidateOptionsMenu()
        } else {
            // Otherwise this is an existing book, so change app bar to say "Edit Book"
            title = getString(R.string.editor_activity_title_edit)

            // Initialize a loader to read the book data from the database
            // and display the current values in the editor
            loaderManager.initLoader(EXISTING_BOOK_LOADER, null, this)
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        edit_title.setOnTouchListener(mTouchListener)
        edit_author.setOnTouchListener(mTouchListener)
        spinner_type.setOnTouchListener(mTouchListener)

        setupSpinner()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        // If this is a new book, hide the "Delete" menu item.
        if (mCurrentBookUri == null) {
            val menuItem = menu.findItem(R.id.action_delete)
            menuItem.setVisible(false)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // User clicked on a menu option in the app bar overflow menu
        when (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            R.id.action_save -> {
                // Save book to database
                saveBook()
                // Exit activity
                finish()
                return true
            }
            // Respond to a click on the "Delete" menu option
            R.id.action_delete -> {
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog()
                return true
            }
            // Respond to a click on the "Up" arrow button in the app bar
            android.R.id.home -> {
                // If the book hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mBookHasChanged) {
                    NavUtils.navigateUpFromSameTask(this@EditorActivity)
                    return true
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                val discardButtonClickListener = DialogInterface.OnClickListener { dialogInterface, i ->
                    // User clicked "Discard" button, navigate to parent activity.
                    NavUtils.navigateUpFromSameTask(this@EditorActivity)
                }

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * This method is called when the back button is pressed.
     */
    override fun onBackPressed() {
        // If the book hasn't changed, continue with handling back button press
        if (!mBookHasChanged) {
            super.onBackPressed()
            return
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        val discardButtonClickListener = DialogInterface.OnClickListener { dialogInterface, i ->
            // User clicked "Discard" button, close the current activity.
            finish()
        }

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener)
    }


    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        // Since the editor shows all book attributes, define a projection that contains
        // all columns from the books table
        val projection = arrayOf<String>(BookEntry._ID, BookEntry.COLUMN_BOOK_TITLE, BookEntry.COLUMN_BOOK_AUTHOR, BookEntry.COLUMN_BOOK_TYPE)

        // This loader will execute the ContentProvider's query method on a background thread
        return CursorLoader(this, // Parent activity context
                mCurrentBookUri, // Query the content URI for the current book
                projection, // No selection arguments
                null,// Columns to include in the resulting Cursor
                null,// No selection clause
                null)// Default sort order
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.count < 1) {
            return
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of book attributes that we're interested in
            val titleColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_TITLE)
            val authorColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_AUTHOR)
            val typeColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_TYPE)

            // Extract out the value from the Cursor for the given column index
            val title = cursor.getString(titleColumnIndex)
            val author = cursor.getString(authorColumnIndex)
            val type = cursor.getInt(typeColumnIndex)

            // Update the views on the screen with the values from the database
            edit_title.setText(title)
            edit_author.setText(author)

            // Type is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is Novel, 2 is Poetry).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            when (type) {
                BookEntry.TYPE_NOVEL -> spinner_type.setSelection(1)
                BookEntry.TYPE_POETRY -> spinner_type.setSelection(2)
                else -> spinner_type.setSelection(0)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        // If the loader is invalidated, clear out all the data from the input fields.
        edit_title.setText("");
        edit_author.setText("");
        spinner_type.setSelection(0); // Select "Unknown" type
    }

    /**
     * Setup the dropdown spinner that allows the user to select the type of the book.
     */
    private fun setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        val typeSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_type_options, android.R.layout.simple_spinner_item)

        // Specify dropdown layout style - simple list view with 1 item per line
        typeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)

        // Apply the adapter to the spinner
        spinner_type.setAdapter(typeSpinnerAdapter)

        // Set the integer mSelected to the constant values
        spinner_type.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selection = parent.getItemAtPosition(position) as String
                if (!TextUtils.isEmpty(selection)) {
                    if (selection == getString(R.string.type_novel)) {
                        mType = BookEntry.TYPE_NOVEL
                    } else if (selection == getString(R.string.type_poetry)) {
                        mType = BookEntry.TYPE_POETRY
                    } else {
                        mType = BookEntry.TYPE_UNKNOWN
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            override fun onNothingSelected(parent: AdapterView<*>) {
                mType = BookEntry.TYPE_UNKNOWN
            }
        })
    }

    /**
     * Get user input from editor and save book into database.
     */
    private fun saveBook() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        val title = edit_title.getText().toString().trim()
        val author = edit_author.getText().toString().trim()

        // Check if this is supposed to be a new book
        // and check if all the fields in the editor are blank
        if (mCurrentBookUri == null && TextUtils.isEmpty(title) &&
                TextUtils.isEmpty(author) && mType === BookEntry.TYPE_UNKNOWN) {
            // Since no fields were modified, we can return early without creating a new book.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return
        }

        // Create a ContentValues object where column names are the keys,
        // and book attributes from the editor are the values.
        val values = ContentValues()
        values.put(BookEntry.COLUMN_BOOK_TITLE, title)
        values.put(BookEntry.COLUMN_BOOK_AUTHOR, author)
        values.put(BookEntry.COLUMN_BOOK_TYPE, mType)

        // Determine if this is a new or existing book by checking if mCurrentBookUri is null or not
        if (mCurrentBookUri == null) {
            // This is a NEW peson, so insert a new book into the provider,
            // returning the content URI for the new book.
            val newUri = contentResolver.insert(BookEntry.CONTENT_URI, values)

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_failed),
                        Toast.LENGTH_SHORT).show()
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_successful),
                        Toast.LENGTH_SHORT).show()
            }
        } else {
            // Otherwise this is an EXISTING book, so update the book with content URI: mCurrentPetUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentBookUri will already identify the correct row in the database that
            // we want to modify.
            val rowsAffected = contentResolver.update(mCurrentBookUri, values, null, null)

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_failed),
                        Toast.LENGTH_SHORT).show()
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_successful),
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     * the user confirms they want to discard their changes
     */
    private fun showUnsavedChangesDialog(
            discardButtonClickListener: DialogInterface.OnClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.unsaved_changes_dialog_msg)
        builder.setPositiveButton(R.string.discard, discardButtonClickListener)
        builder.setNegativeButton(R.string.keep_editing, DialogInterface.OnClickListener { dialog, id ->
            // User clicked the "Keep editing" button, so dismiss the dialog
            // and continue editing the book.
            dialog.dismiss()
        })

        // Create and show the AlertDialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    /**
     * Prompt the user to confirm that they want to delete this book.
     */
    private fun showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.delete_dialog_msg)
        builder.setPositiveButton(R.string.delete, DialogInterface.OnClickListener { dialog, id ->
            // User clicked the "Delete" button, so delete the book.
            deleteBook()
        })
        builder.setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, id ->
            // User clicked the "Cancel" button, so dismiss the dialog
            // and continue editing the book.
            dialog.dismiss()
        })

        // Create and show the AlertDialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    /**
     * Perform the deletion of a book in the database.
     */
    private fun deleteBook() {
        // Only perform the delete if this is an existing book.
        if (mCurrentBookUri != null) {
            // Call the ContentResolver to delete the book at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentBookUri
            // content URI already identifies the book that we want.
            val rowsDeleted = contentResolver.delete(mCurrentBookUri, null, null)

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_failed),
                        Toast.LENGTH_SHORT).show()
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_successful),
                        Toast.LENGTH_SHORT).show()
            }
        }

        // Close the activity
        finish()
    }
}