package com.movehome.armoving

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class IntroActivity : AppCompatActivity() {

    var _IntroButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        _IntroButton = findViewById(R.id.IntroButton_OK) as Button

        _IntroButton!!.setOnClickListener {

            _IntroButton!!.isEnabled = true

            val bundle: Bundle = intent.extras!!
            val intent = Intent(this, SignupActivity::class.java)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }
}
