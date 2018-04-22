package com.mmv.bdkotlin.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mmv.bdkotlin.data.BookContract.BookEntry


class BooksDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        /** Name of the database file */
        private val DATABASE_NAME:String = "books.db";

        private val DATABASE_VERSION:Int = 1;
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create a String that contains the SQL statement to create the people table
        val SQL_CREATE_PEOPLE_TABLE: String =  "CREATE TABLE ${BookEntry.TABLE_NAME} (" +
                "${BookEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${BookEntry.COLUMN_BOOK_TITLE} TEXT, " +
                "${BookEntry.COLUMN_BOOK_AUTHOR} TEXT, " +
                "${BookEntry.COLUMN_BOOK_TYPE} INTEGER );"

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_PEOPLE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // The database is still at version 1, so there's nothing to do be done here.
    }

}