package com.movehome.armoving

import android.content.Intent
import android.os.Bundle
//import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.movehome.armoving.model.CardListData
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var fragmentParent: FragmentParent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.iDs

        val items: ArrayList<CardListData>
        val bundle = intent.extras
        var position: Int? = null
        if(bundle != null){
            items = bundle.getParcelableArrayList<CardListData>("items")!!
            position = bundle.getInt("position")

            var total: Float= 0f
            for(item in items){
                for(furniture in item.data){
                    if(furniture.card_volume != null) total = total + furniture.card_volume.toFloat()
                }
            }
            val totalFloor = "%.2f".format(total/1000000)
            total_M.text = "總體積 "+totalFloor+" m²"
        }
        else items = ArrayList()

        val adapter = LocalAdapter(this)
        for(item in items){
            val page = ViewFragment(this, item.data)
            adapter.addFragment(page)
        }
        val viewPager = findViewById<ViewPager2>(R.id.viewPager_main)
        viewPager.adapter = adapter

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout_main)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        TabLayoutMediator(tabLayout, viewPager, object : TabLayoutMediator.TabConfigurationStrategy {
            override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                tab.text = items[position].title
            }
        }).attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //add image
                val addBtn = findViewById<Button>(R.id.plus_btn_main)
                addBtn.setOnClickListener {
                    val intent = Intent(this@MainActivity, ArcoreMeasurement::class.java)
                    intent.putExtra("items", items)
                    intent.putExtra("position", position)
                    startActivity(intent)
                }
            }
        })
        if (position != null) viewPager.currentItem = position

        // new room
        val addRoomBtn = findViewById<Button>(R.id.addRoom_btn_main)
        addRoomBtn.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("items", items)
            startActivity(intent)
        }


        val page  = "page"
        if (page != "") {
            fragmentParent!!.addPage(page + "")
        }

        //next activity
        val buttonDone = findViewById<Button>(R.id.done_btn_main)
        buttonDone.setOnClickListener{
            val intent = Intent(this, IntroActivity::class.java)
            intent.putExtras(bundle!!)
            startActivity(intent)
        }
    }

    private val iDs: Unit
        private get() {
//            buttonAddPage = findViewById<View>(R.id.buttonAddPage) as Button
            fragmentParent = this.supportFragmentManager.findFragmentById(R.id.fragmentParent) as FragmentParent
//            textView = findViewById<View>(R.id.editTextPageName) as TextView
        }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    class LocalAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        private val pages = ArrayList<Fragment>()

        override fun getItemCount(): Int = pages.size

        override fun createFragment(position: Int): Fragment {
            return pages[position]
        }

        fun addFragment(fragment: Fragment) {
            pages.add(fragment)
        }
    }
}