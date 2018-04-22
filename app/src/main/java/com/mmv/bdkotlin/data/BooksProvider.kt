package com.mmv.bdkotlin.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri

import com.mmv.bdkotlin.data.BookContract.BookEntry



class BooksProvider: ContentProvider() {
    /** URI matcher code for the content URI for the books table */
    private val BOOKS: Int = 100
    /** URI matcher code for the content URI for a single book in the books table */
    private val BOOKS_ID: Int = 101

    /** UriMatcher object to match a content URI to a corresponding code.*/
    private val sUriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    init {
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS, BOOKS)
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS+"/#", BOOKS_ID)
    }


    /** Database helper object */
    private lateinit var mDbHelper: BooksDBHelper

    override fun onCreate(): Boolean {
        mDbHelper = BooksDBHelper(getContext())
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        // Get readable database
        var database: SQLiteDatabase = mDbHelper.readableDatabase

        // This cursor will hold the result of the query
        var cursor: Cursor

        // Figure out if the URI matcher can match the URI to a specific code
        var match: Int = sUriMatcher.match(uri)
        cursor = when (match) {
            BOOKS -> {
                database.query(BookContract.BookEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
            }
            BOOKS_ID -> {
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                var selection: String = BookContract.BookEntry._ID + "=?"
                var selectionArgs: Array<String> = Array<String>(1) { ContentUris.parseId(uri).toString() }

                // This will perform a query on the books table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                database.query(BookContract.BookEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
            }
            else-> {
                throw IllegalArgumentException ("Cannot query unknown URI " + uri)
            }
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(context.contentResolver, uri)

        // Return the cursor
        return cursor
    }

    override fun insert(uri: Uri, contentValues: ContentValues): Uri? {
        val match = sUriMatcher.match(uri)
        when (match) {
            BOOKS -> return insertBooks(uri, contentValues)
            else -> throw IllegalArgumentException("Insertion is not supported for " + uri)
        }
    }

    private fun insertBooks(uri: Uri, contentValues: ContentValues): Uri?{
        // Check that the title is not null
        contentValues.getAsString(BookEntry.COLUMN_BOOK_TITLE) ?: throw IllegalArgumentException("Book requires a title")
        // Check that the author is not null
        contentValues.getAsString(BookEntry.COLUMN_BOOK_AUTHOR) ?: throw IllegalArgumentException("Book requires a author")

        // Check that the type is valid
        val type = contentValues.getAsInteger(BookEntry.COLUMN_BOOK_TYPE)
        if (type == null || !BookEntry.isValidType(type)) {
            throw IllegalArgumentException("Book requires valid type")
        }

        // Get writeable database
        val database = mDbHelper.writableDatabase

        // Insert the new book with the given values
        val id = database.insert(BookEntry.TABLE_NAME, null, contentValues)
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1L) {
            return null
        }

        // Notify all listeners that the data has changed for the content URI
        context.contentResolver.notifyChange(uri, null)

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id)
    }

    override fun update(uri: Uri, contentValues: ContentValues, selection: String?, selectionArgs: Array<out String>?): Int {
        val match = sUriMatcher.match(uri)
        when (match) {
            BOOKS -> {
                return updateBook(uri, contentValues, selection, selectionArgs)
            }
            BOOKS_ID -> {
                // For the BOOKS_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                var selection = BookEntry._ID + "=?"
                var selectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                return updateBook(uri, contentValues, selection, selectionArgs)
            }
            else -> {
                throw IllegalArgumentException("Update is not supported for " + uri)
            }
        }
    }

    private fun updateBook(uri: Uri, contentValues: ContentValues, selection: String?, selectionArgs: Array<out String>?): Int {
        // If the {@link BookEntry#COLUMN_BOOK_TITLE} key is present,
        // check that the value is not null.
        if (contentValues.containsKey(BookEntry.COLUMN_BOOK_TITLE)) {
            contentValues.getAsString(BookEntry.COLUMN_BOOK_TITLE) ?: throw IllegalArgumentException("Book requires a title")
        }
        // If the {@link BookEntry#COLUMN_BOOK_AUTHOR} key is present,
        // check that the value is not null.
        if (contentValues.containsKey(BookEntry.COLUMN_BOOK_AUTHOR)) {
            contentValues.getAsString(BookEntry.COLUMN_BOOK_AUTHOR) ?: throw IllegalArgumentException("Book requires a author")
        }

        // Check that the type is valid
        val type = contentValues.getAsInteger(BookEntry.COLUMN_BOOK_TYPE)
        if (type == null || !BookEntry.isValidType(type)) {
            throw IllegalArgumentException("Book requires valid type")
        }

        // If the {@link BookEntry#COLUMN_BOOK_TYPE} key is present,
        // check that the value is valid.
        if (contentValues.containsKey(BookEntry.COLUMN_BOOK_TYPE)) {
            val type = contentValues.getAsInteger(BookEntry.COLUMN_BOOK_TYPE)
            if (type == null || !BookEntry.isValidType(type)) {
                throw IllegalArgumentException("Book requires valid type")
            }
        }

        // If there are no values to update, then don't try to update the database
        if (contentValues.size() === 0) {
            return 0
        }

        // Otherwise, get writeable database to update the data
        val database = mDbHelper.writableDatabase

        // Perform the update on the database and get the number of rows affected
        val rowsUpdated = database.update(BookEntry.TABLE_NAME, contentValues, selection, selectionArgs)

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            context.contentResolver.notifyChange(uri, null)
        }

        // Return the number of rows updated
        return rowsUpdated
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        // Get writeable database
        val database = mDbHelper.writableDatabase

        // Track the number of rows that were deleted
        val rowsDeleted: Int

        val match = sUriMatcher.match(uri)
        rowsDeleted = when (match) {
            BOOKS -> {
                // Delete all rows that match the selection and selection args
                database.delete(BookEntry.TABLE_NAME, selection, selectionArgs)
            }
            BOOKS_ID -> {
                // Delete a single row given by the ID in the URI
                var selection: String = BookEntry._ID + "=?"
                var selectionArgs: Array<String> = arrayOf(ContentUris.parseId(uri).toString())
                database.delete(BookEntry.TABLE_NAME, selection, selectionArgs)
            }
            else -> {
                throw IllegalArgumentException("Deletion is not supported for " + uri)
            }
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            context.contentResolver.notifyChange(uri, null)
        }

        // Return the number of rows deleted
        return rowsDeleted
    }

    override fun getType(uri: Uri): String {
        val match:Int = sUriMatcher.match(uri)

        when (match) {
            BOOKS -> return BookEntry.CONTENT_LIST_TYPE
            BOOKS_ID -> return BookEntry.CONTENT_ITEM_TYPE
            else -> IllegalStateException("Unknown URI " + uri + " with match " + match)
        }
        return ""
    }
}