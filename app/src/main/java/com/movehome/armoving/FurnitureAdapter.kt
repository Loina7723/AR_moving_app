package com.movehome.armoving

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.furniture_row.view.*

class FurnitureAdapter(val arrayList: ArrayList<FurnitureModel>, val context: Context) :
    RecyclerView.Adapter<FurnitureAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        fun bindItems(model: FurnitureModel) {
//            itemView.titleTv.text = model.title
//            // card 的 title
//            itemView.descriptionTv.text = model.des
//            // card 的 description
//            itemView.imageIv.setImageResource(model.image)
//            // card 的 image
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.furniture_row, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(arrayList[position])
    }
}