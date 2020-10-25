package com.movehome.armoving

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.cards_page.view.*

class ViewFragment(val mContext: Context, val data: ArrayList<CardData>) : Fragment() {
    var mAdapter: RecyclerView.Adapter<*>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.cards_page, container, false)
        val rv = rootView.recyclerview_cp
        setRv(rv)
        return rootView
    }

    fun setRv(rv: RecyclerView){
        rv.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
        mAdapter = RecyclerAdapter(data)
        rv.adapter = mAdapter
    }
}