package com.movehome.armoving

import android.content.Intent
import android.os.Bundle
//import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var buttonAddPage: Button? = null
    var fragmentParent: FragmentParent? = null
//    var textView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        iDs
//        setEvents()

        val bundle: Bundle? = intent.extras
        val page = bundle!!.getString("Page")
        Toast.makeText(this, page, Toast.LENGTH_SHORT).show()
//        editTextPageName.text=page

        if (page != "") {
            fragmentParent!!.addPage(page + "")
        }

        //next activity
        val buttonDone = findViewById<Button>(R.id.buttonDone)
        buttonDone.setOnClickListener{
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)}

        // new room
        val buttonAddRoom = findViewById<Button>(R.id.buttonAddRoom)
            buttonAddRoom.setOnClickListener {
                val intent = Intent(this, StartActivity::class.java)
                startActivity(intent)
        }

        //add image
        val plusBtn = findViewById<ImageButton>(R.id.plusBtn)
        plusBtn.setOnClickListener{
            val intent = Intent(this, ArcoreMeasurement::class.java)
            startActivity(intent)}
    }

    private val iDs: Unit
        private get() {
//            buttonAddPage = findViewById<View>(R.id.buttonAddPage) as Button
            fragmentParent = this.supportFragmentManager.findFragmentById(R.id.fragmentParent) as FragmentParent
//            textView = findViewById<View>(R.id.editTextPageName) as TextView
        }

//    private fun setEvents() {
//        buttonAddPage!!.setOnClickListener {
//            else {
//                Toast.makeText(this@MainActivity, "Page name is empty", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

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
}