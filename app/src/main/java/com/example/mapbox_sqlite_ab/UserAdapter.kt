package com.example.mapbox_sqlite_ab

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapbox_sqlite_ab.dao.entity.map_points
import com.mapbox.geojson.Point


interface OnPointClickListener {
    fun onPointClick(point: Point)
}

class UserAdapter(private var map_points: List<map_points>) : RecyclerView.Adapter<UserViewHolder>() {
    private var onPointClickListener: OnPointClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.patronlist, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = map_points[position]
        holder.bind(user)
        holder.setOnPointClickListener(onPointClickListener)
    }

    override fun getItemCount(): Int { return map_points.size }

    fun setOnPointClickListener(listener: OnPointClickListener?) {
        onPointClickListener = listener
    }
}

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var onPointClickListener: OnPointClickListener? = null

    fun bind(map: map_points) {
        itemView.findViewById<TextView>(R.id.name_point).text = map.Name.toString()
        itemView.findViewById<Button>(R.id.focus_ubication).setOnClickListener {
            val point = Point.fromLngLat(map.Point_longitud, map.Point_latitude)
            onPointClickListener?.onPointClick(point)
        }
    }

    fun setOnPointClickListener(listener: OnPointClickListener?) {
        onPointClickListener = listener
    }
}


