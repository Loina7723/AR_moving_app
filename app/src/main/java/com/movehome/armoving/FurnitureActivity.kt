//package com.movehome.armoving
//
//import android.content.Intent
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.android.synthetic.main.activity_furniture.*
//
//class FurnitureActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_furniture)
//
//        val arrayList = ArrayList<FurnitureModel>()
//        arrayList.add(FurnitureModel("closet", "Here are closet des", R.drawable.closet))
//        arrayList.add(FurnitureModel("fridge", "Here are fridge des", R.drawable.fridge))
//        arrayList.add(FurnitureModel("nightstand", "Here are nightstand des", R.drawable.nightstand))
//        arrayList.add(FurnitureModel("washer", "Here are washer des", R.drawable.washer))
//
//        val furnitureAdapter = FurnitureAdapter(arrayList, this)
//
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = furnitureAdapter
//        val button = findViewById<Button>(R.id.btn_complete)
//        button.setOnClickListener{
//            val intent = Intent(this, IntroActivity::class.java)
//            startActivity(intent)
//        }
//    }
//}