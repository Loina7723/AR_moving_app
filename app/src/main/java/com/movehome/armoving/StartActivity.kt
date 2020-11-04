package com.movehome.armoving

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.movehome.armoving.helper.SessionManager
import com.movehome.armoving.model.CardListData

class StartActivity : AppCompatActivity() {
    private var items: ArrayList<CardListData> = ArrayList()
    private var etMessage : EditText? = null

    private val TAG = StartActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        val livingBtn = findViewById<Button>(R.id.living_btn_start)
        val kitchenBtn = findViewById<Button>(R.id.kitchen_btn_start)
        val bathBtn = findViewById<Button>(R.id.bath_btn_start)
        val readingBtn = findViewById<Button>(R.id.reading_btn_start)
        val addButton = findViewById<Button>(R.id.submit_btn_start)
        etMessage = findViewById(R.id.addRoom_et_start)

        val data = intent.extras
        if(data != null) {
            items = data.getParcelableArrayList<CardListData>("items")!!
            for(item in items){
                Log.d(TAG, "tab: "+item.title)
            }
        }

        setRoomBtn(livingBtn)
        setRoomBtn(kitchenBtn)
        setRoomBtn(bathBtn)
        setRoomBtn(readingBtn)



        addButton.setOnClickListener{
            val newRoom: String = etMessage?.text.toString()
            if(newRoom.trim().isNotEmpty()) {
                if(isTabExist(newRoom)){
                    Toast.makeText(this, "已存在此房間，請輸入其他房間名字", Toast.LENGTH_LONG).show()
                }
                else{
                    items.add(CardListData(newRoom, ArrayList()))
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.putExtra("items", items)
                    intent.putExtra("position", items.size-1)
//                    intent.putExtra("Page", newRoom)
                    startActivity(intent)
                }
            }else{
                Toast.makeText(applicationContext, "請輸入所在位置! ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRoomBtn(room: Button){
        if(isTabExist(room.text.toString())){
            Log.d(TAG, room.text.toString()+" exist")
            room.setBackgroundResource(R.drawable.btn_round_corner_gray)
            room.setTextColor(Color.parseColor("#D5D5D5"))
//            return
        }
        room.setOnClickListener { setRoomClick(room.text.toString()) }
    }

    private fun setRoomClick(text: String) {
        Log.d(TAG, "$text click")
        etMessage?.setText(text, TextView.BufferType.EDITABLE)
        etMessage?.setTextColor(Color.BLACK)
    }

    private fun isTabExist(tabName: String): Boolean {
        for(item in items)
            if(item.title.equals(tabName))
                return true
        return false
    }
}
