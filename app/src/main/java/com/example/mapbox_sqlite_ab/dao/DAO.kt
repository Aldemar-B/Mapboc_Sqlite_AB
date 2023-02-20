package com.example.mapbox_sqlite_ab.dao


import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Binder
import android.os.IBinder
import com.example.mapbox_sqlite_ab.R
import com.example.mapbox_sqlite_ab.dao.database.DataBase
import com.example.mapbox_sqlite_ab.dao.entity.map_points
import com.mapbox.geojson.Point
import java.util.ArrayList


class DAO : Service() {
    private val mBinder: IBinder = LocalBinder()
    private var db: SQLiteDatabase? = null
    var version: String? = null

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): DAO = this@DAO
    }


    fun getDb(): SQLiteDatabase? {
        return db
    }

    override fun onBind(intent: Intent?): IBinder? {
        // TODO Auto-generated method stub
        return mBinder
    }


    override fun onCreate() {
        val dataBase = DataBase(this, "mapbox.db")
        db = dataBase.dataBase
        version = getString(R.string.version)
        val preferences = getSharedPreferences("session", MODE_PRIVATE)
    }

    override fun onDestroy() {
        db!!.close()
    }

    fun testDB(): Boolean {
        var ok = false
        try {
            var sql = "SELECT id, version FROM M_VERSION_CODE where version = 1"
            val rs = db!!.rawQuery(sql, null)
            while (rs.moveToNext()) {
                ok = true
            }
            rs.close()

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return ok
    }

    fun save_p(mText: String, point: Point) {
        try {
            val campo = ContentValues()
            campo.put("Name", mText)
            campo.put("Point_latitude", point.latitude())
            campo.put("Point_longitud", point.longitude())
            db!!.insert("map_points", null, campo)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


    }

    fun delete_p(point: Point) {
        try {
            db!!.delete(
                "map_points",
                "Point_latitude=? and Point_longitud=?",
                arrayOf(point.latitude().toString(), point.longitude().toString())
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    @SuppressLint("Range")
    fun search_pm(): ArrayList<map_points>? {
        var mylist = ArrayList<map_points>()

        try {
            var sql = ("select * from map_points")
            val rs = db!!.rawQuery(sql, null)
            while (rs.moveToNext()) {
                val objc = map_points()
                data(objc, rs)
                mylist.add(objc)
            }
            rs.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return mylist

    }


    @SuppressLint("Range")
    fun data(data: map_points, rs: Cursor) {
        data?.Name = rs.getString(rs.getColumnIndex("Name"))
        data?.Point_latitude = rs.getDouble(rs.getColumnIndex("Point_latitude"))
        data?.Point_longitud = rs.getDouble(rs.getColumnIndex("Point_longitud"))
    }

}
