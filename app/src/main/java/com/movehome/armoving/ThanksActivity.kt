package com.movehome.armoving

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.movehome.armoving.model.CardListData
import kotlinx.android.synthetic.main.activity_thanks.*

class ThanksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thanks)

        val bundle = intent.extras!!
        val items = bundle.getParcelableArrayList<CardListData>("items")

        var num = 0
        for(item in items!!) {
            num += item.data.size
        }

        text_thanks.text = "總金額："+num+" 元"
    }
}
