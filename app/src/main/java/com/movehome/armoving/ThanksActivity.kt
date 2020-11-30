package com.movehome.armoving

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.movehome.armoving.model.CardListData
import kotlinx.android.synthetic.main.activity_thanks.*

class ThanksActivity : AppCompatActivity() {
    val TAG = ThanksActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thanks)

        val bundle = intent.extras!!
        val items = bundle.getParcelableArrayList<CardListData>("items")

        var num = 0
        for(i in 0..items!!.size-1) {
            val item = items[i]
            val rnd = (20..30).random()
            num += (Math.round(item.data[i].card_volume!!.toFloat()/rnd))
            Log.d(TAG, "num("+i+") = "+(Math.round(item.data[i].card_volume!!.toFloat()/rnd))+"("+item.data[i].card_volume+"/"+rnd+")")
        }

        text_thanks.text = "總金額："+num+" 元"
    }
}
