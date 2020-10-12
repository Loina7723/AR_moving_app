package com.movehome.armoving


import android.os.Bundle
//import android.support.design.widget.TabLayout
//import android.support.design.widget.TabLayout.ViewPagerOnTabSelectedListener
//import android.support.v4.app.Fragment
//import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

/**
 * Created by DAT on 9/1/2015.
 */
class FragmentParent : Fragment() {
    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager? = null
    private var adapter: ViewPagerAdapter? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_parent, container, false)
        getIDs(view)
        setEvents()
        return view
    }

    private fun getIDs(view: View) {
        viewPager = view.findViewById<View>(R.id.my_viewpager) as ViewPager
        tabLayout = view.findViewById<View>(R.id.my_tab_layout) as TabLayout
        adapter = ViewPagerAdapter(fragmentManager, activity!!, viewPager!!, tabLayout!!)
        viewPager!!.adapter = adapter
    }

    var selectedTabPosition = 0
    private fun setEvents() {
        tabLayout!!.setOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            override fun onTabSelected(tab: TabLayout.Tab) {
                super.onTabSelected(tab)
                viewPager!!.currentItem = tab.position
                selectedTabPosition = viewPager!!.currentItem
                Log.d("Selected", "Selected " + tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                super.onTabUnselected(tab)
                Log.d("Unselected", "Unselected " + tab.position)
            }
        })
    }

    fun addPage(pagename: String?) {
        val bundle = Bundle()
        bundle.putString("data", pagename)
        val fragmentChild = FragmentChild()
        fragmentChild.arguments = bundle
        adapter!!.addFrag(fragmentChild, pagename!!)
        adapter!!.notifyDataSetChanged()
        if (adapter!!.count > 0) tabLayout!!.setupWithViewPager(viewPager)
        viewPager!!.currentItem = adapter!!.count - 1
        setupTabLayout()
    }

    fun setupTabLayout() {
        selectedTabPosition = viewPager!!.currentItem
        for (i in 0 until tabLayout!!.tabCount) {
            tabLayout!!.getTabAt(i)!!.customView = adapter!!.getTabView(i)
        }
    }
}