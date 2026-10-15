package com.omerngs.photoscanner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class PhotoMatch(val barcode: String, val uri: String, val name: String)

class BarcodeDb(context: Context) : SQLiteOpenHelper(context, "photos.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE photos (barcode TEXT NOT NULL, uri TEXT NOT NULL, name TEXT NOT NULL)")
        db.execSQL("CREATE INDEX idx_barcode ON photos(barcode)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun beginIndexing() { writableDatabase.beginTransaction() }
    fun finishIndexing() {
        val db = writableDatabase
        if (db.inTransaction()) {
            db.setTransactionSuccessful()
            db.endTransaction()
        }
    }
    fun clear() { writableDatabase.delete("photos", null, null) }
    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM photos", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }
    fun insert(barcode: String, uri: String, name: String) {
        val v = ContentValues().apply { put("barcode", barcode); put("uri", uri); put("name", name) }
        writableDatabase.insert("photos", null, v)
    }
    fun find(barcode: String): List<PhotoMatch> {
        val out = mutableListOf<PhotoMatch>()
        readableDatabase.query("photos", arrayOf("barcode", "uri", "name"), "barcode = ? COLLATE NOCASE",
            arrayOf(barcode), null, null, "name ASC").use { c ->
            while (c.moveToNext()) out.add(PhotoMatch(c.getString(0), c.getString(1), c.getString(2)))
        }
        return out
    }
}
