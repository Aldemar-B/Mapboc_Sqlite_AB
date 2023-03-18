package com.example.mapbox_sqlite_ab.dao.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.*


class DaoVersionCode(db: SQLiteDatabase?) {
    private var db: SQLiteDatabase? = null
    fun loadVersionCode(): Int {
        var rs: Cursor? = null
        var result = -1
        try {
            val sql = "SELECT version FROM m_version_code limit 1"
            rs = db!!.rawQuery(sql, null)
            if (rs.moveToNext()) {
                result = rs.getInt(0)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            @Suppress("UNNECESSARY_SAFE_CALL")
            Objects.requireNonNull(rs)?.close()
        }
        return result
    }

    fun saveVersionCode(value: Int) {
        var rs: Cursor? = null
        try {
            val sql = "INSERT INTO m_version_code(version) VALUES($value)"
            rs = db!!.rawQuery(sql, null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            @Suppress("UNNECESSARY_SAFE_CALL")
            Objects.requireNonNull(rs)?.close()
        }
    }

    init {
        try {
            this.db = db
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
