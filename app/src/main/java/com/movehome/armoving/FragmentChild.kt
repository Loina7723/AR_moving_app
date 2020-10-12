package com.movehome.armoving

import android.os.Bundle
//import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Created by DAT on 9/1/2015.
 */
class FragmentChild : Fragment() {
    var childname: String? = null
    var textViewChildName: TextView? = null
    var editText: EditText? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_child, container, false)
        val bundle = arguments
        childname = bundle!!.getString("data")
        getIDs(view)
        setEvents()
        return view
    }

    private fun getIDs(view: View) {
        textViewChildName = view.findViewById<View>(R.id.textViewChild) as TextView
        textViewChildName!!.text = childname
        editText = view.findViewById<View>(R.id.editText) as EditText
        editText!!.setText("")
    }

    private fun setEvents() {}
}