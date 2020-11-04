package com.movehome.armoving

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.RadioGroup
import androidx.constraintlayout.widget.Constraints.TAG
import com.movehome.armoving.helper.SessionManager
import com.movehome.armoving.model.CardListData
import kotlinx.android.synthetic.main.activity_info.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class InfoActivity : AppCompatActivity() {
    val TAG = InfoActivity::class.java.simpleName

    var gender: String? = null
    var floor = arrayOf("1", "2", "3", "4", "5樓以上")
    var currentFloor: String? = null
    var spinner: Spinner? = null
    var elevator: String? = null
    var _nameText: EditText? = null
    var _addressOut: EditText? = null
    var _addressIn: EditText? = null
    var _OKButton: Button? = null


    var bundle: Bundle? = null

    var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        bundle = intent.extras

        // Get radio group selected item using on checked change listener
        sex_rg.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { group, checkedId ->
                val radio: RadioButton = findViewById(checkedId)
                gender = radio.text.toString()
                Toast.makeText(
                    applicationContext, " 您選擇了 : ${radio.text}",
                    Toast.LENGTH_SHORT
                ).show()
            })

        option_rg.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { group, checkedId ->
                val radio: RadioButton = findViewById(checkedId)
                if(radio.text.toString().equals("是")) elevator = "true"
                else elevator = "false"
                Toast.makeText(
                    applicationContext, " 您選擇了 : ${radio.text}",
                    Toast.LENGTH_SHORT
                ).show()
            })


        _nameText = findViewById(R.id.name) as EditText
        _addressOut = findViewById(R.id.addressOut) as EditText
        _addressIn = findViewById(R.id.addressIn) as EditText
        _OKButton = findViewById(R.id.button_OK) as Button
        _OKButton!!.setOnClickListener { done() }

        //  spinner
        spinner = this.floor_spinner
        spinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentFloor = null
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFloor = floor[position]
            }
        }

        // Create an ArrayAdapter using a simple spinner layout and languages array
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, floor)
        // Set layout to use when the list of choices appear
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Set Adapter to Spinner
        spinner!!.setAdapter(aa)


    }

    fun done() {
        Log.d(TAG, "done")

        if (!validate()) {
            onFailed()
            return
        }

//        val session = SessionManager.getInstance(this)
//        val user = session?.userDetail
//        val token = user?.token

        val name = _nameText!!.text.toString()
        val addrOut = _addressOut!!.text.toString()
        val addrIn = _addressIn!!.text.toString()

        if(bundle == null) return

        val body = FormBody.Builder()
            .add("name", name)
            .add("email", bundle!!.getString("email")!!)
            .add("password", bundle!!.getString("password")!!)
            .add("phone",  bundle!!.getString("phone")!!)
            .add("addrOut", addrOut)
            .add("addrIn", addrIn)
            .add("floor", currentFloor!!)
//            .add("if_elev", elevator!!)
            .build()

        val server_url = "http://140.117.71.79:8000/api"
        val request = Request.Builder()
            .url(server_url+"/register")
            .post(body)
            .build()

        val okHttpClient = OkHttpClient()
        val call: Call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
                Log.d(TAG, "request fail: "+e.message)
                runOnUiThread (object : Runnable {
                    override fun run() {
                        Toast.makeText(this@InfoActivity, "connect fail", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
//                Log.i(TAG, "responseData: "+responseData)

                val responseObj = JSONObject(responseData)
                Log.i(TAG, "responseObj: "+responseObj)

                if(responseObj.getBoolean("success")){
                    val user = responseObj.getJSONObject("user")
                    user.put("token", responseObj.getString("token"))
                    token = responseObj.getString("token")
                    Log.i(TAG, "user: "+user)

                    onSighupSuccess(user)
                }
                else{
                    onFailed();
                    Log.i(TAG, "register fail")
                    runOnUiThread ({ Toast.makeText(this@InfoActivity, "register fail", Toast.LENGTH_SHORT).show() })
                }
            }
        });

        runOnUiThread({ _OKButton!!.isEnabled = false })


    }

    fun onSighupSuccess(user: JSONObject) {
        val session = SessionManager.getInstance(this)
        session?.createLoginSession(user.getInt("id").toString(), user.toString())

        runOnUiThread({ _OKButton!!.isEnabled = true })
        createfurnitures()
    }

    fun onFailed() {
        Toast.makeText(this, "資料儲存失敗", Toast.LENGTH_LONG).show()

        runOnUiThread({ _OKButton!!.isEnabled = true })
    }

    fun validate(): Boolean {
        var valid = true

        val name = _nameText!!.text.toString()
        val addrOut = _addressOut!!.text.toString()
        val addrIn = _addressIn!!.text.toString()


        if (name.isEmpty()) {
            _nameText!!.error = "姓名欄不得為空"
            valid = false
        } else {
            _nameText!!.error = null
        }

        if (addrOut.isEmpty()) {
            _addressOut!!.error = "遷出地址欄不得為空"
            valid = false
        } else {
            _addressOut!!.error = null
        }

        if (addrIn.isEmpty()) {
            _addressIn!!.error = "遷入地址欄不得為空"
            valid = false
        }
        else if (addrOut == addrIn){
            _addressIn!!.error = "遷出與遷入地址不得相同"
            valid = false
        }
        else {
            _addressIn!!.error = null
        }

        if (gender == null) {
            Toast.makeText(applicationContext, "您尚未填寫性別欄位", Toast.LENGTH_SHORT).show()
            valid = false
        }

        if(currentFloor == null) {
            Toast.makeText(applicationContext, "您尚未填寫樓層欄位", Toast.LENGTH_SHORT).show()
            valid = false
        }

        return valid
    }

    fun createfurnitures(){
        val items = bundle!!.getParcelableArrayList<CardListData>("items")!!
        for(item in items) {
            addRoom(item.title)
            for(i in 0..item.data.size-1){
                val furniture = item.data[i]
                addFurniture(furniture.card_name!!, furniture.card_volume!!, i.toString())
            }
        }

        startActivity(Intent(this, ThanksActivity::class.java))
    }

    fun addRoom(rName: String) {
        val body = FormBody.Builder()
            .add("rName", rName)
            .add("token", token!!)
            .build()

        val server_url = "http://140.117.71.79:8000/api"
        val request = Request.Builder()
            .url(server_url+"/rooms/create")
            .post(body)
            .build()

        val okHttpClient = OkHttpClient()
        val call: Call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
                runOnUiThread({ Toast.makeText(this@InfoActivity, "connect fail", Toast.LENGTH_SHORT).show() })
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.i(TAG, "responseData of room: "+responseData)
            }
        });
    }

    fun addFurniture(fName: String, fvol: String, room_id: String) {
        val body = FormBody.Builder()
            .add("rooms_id", room_id)
            .add("fName", fName)
            .add("fvol", fvol)
            .add("token", token!!)
            .build()

        val server_url = "http://140.117.71.79:8000/api"
        val request = Request.Builder()
            .url(server_url+"/furniture/create")
            .post(body)
            .build()

        val okHttpClient = OkHttpClient()
        val call: Call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
                runOnUiThread({ Toast.makeText(this@InfoActivity, "connect fail", Toast.LENGTH_SHORT).show() })
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.i(TAG, "responseData: "+responseData)
            }
        });
    }
}



