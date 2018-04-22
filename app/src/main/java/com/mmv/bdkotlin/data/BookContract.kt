package com.mmv.bdkotlin.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns

/**
 * API Contract for the BD app.
 */


class BookContract {
    companion object {
        /**
         * The "Content authority"
         **/
        val CONTENT_AUTHORITY: String = "com.mmv.bdkotlin"

        /**
         * Base URI
         */
        val  BASE_CONTENT_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

        /**
         * Books BD path
         */
        val PATH_BOOKS: String = "books"
    }

    class BookEntry: BaseColumns {
        companion object {
            /** The content URI to access the book data in the provider */
            val CONTENT_URI: Uri = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_BOOKS)

            /**
             * The MIME type of the {@link #CONTENT_URI} for a list of books.
             */
            val CONTENT_LIST_TYPE: String = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOKS

            /**
             * The MIME type of the {@link #CONTENT_URI} for a single book.
             */
            val CONTENT_ITEM_TYPE: String = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOKS

            /**
             * Name of database table
              */
            val TABLE_NAME: String = "books"

            /**
             * Unique ID number for the book (only for use in the database table).
             *
             * Type: INTEGER
             */
            val _ID: String = BaseColumns._ID

            /**
             * Title of the book
             *
             * Type: TEXT
             */
            val COLUMN_BOOK_TITLE: String ="title"

            /**
             * Author of the book
             *
             * Type: TEXT
             */
            val COLUMN_BOOK_AUTHOR: String ="author"

            /**
             * Type of the book.
             *
             * The only possible values are {@link #TYPE_UNKNOWN},
             * {@link #TYPE_NOVEL} or {@link #TYPE_POETRY}.
             *
             * Type: INTEGER
             */
            val COLUMN_BOOK_TYPE: String = "type"

            /**
             * Possible values for the type of the book.
             */
            val TYPE_UNKNOWN: Int = 0
            val TYPE_NOVEL: Int = 1
            val TYPE_POETRY: Int = 2

            fun isValidType(book: Int): Boolean {
                return (book == TYPE_UNKNOWN || book == TYPE_NOVEL || book == TYPE_POETRY)
            }
        }
    }
}