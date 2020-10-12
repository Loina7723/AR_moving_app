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
import kotlinx.android.synthetic.main.activity_info.*

class InfoActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    var floor = arrayOf("1", "2", "3", "4", "5樓以上")
    var spinner: Spinner? = null
    var _nameText: EditText? = null
    var _addressOut: EditText? = null
    var _addressIn: EditText? = null
    var _OKButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        // Get radio group selected item using on checked change listener
        sex_rg.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { group, checkedId ->
                val radio: RadioButton = findViewById(checkedId)
                Toast.makeText(
                    applicationContext, " 您選擇了 : ${radio.text}",
                    Toast.LENGTH_SHORT
                ).show()
            })

        // Get radio group selected status and text using button click event
        sex_rg.setOnClickListener {
            // Get the checked radio button id from radio group
            var id: Int = sex_rg.checkedRadioButtonId
            if (id != -1) { // If any radio button checked from radio group
                // Get the instance of radio button using id
                val radio: RadioButton = findViewById(id)
                Toast.makeText(
                    applicationContext, "您選擇了 : ${radio.text}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // If no radio button checked in this radio group
                Toast.makeText(
                    applicationContext, "您尚未填寫性別欄位",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        _nameText = findViewById(R.id.name) as EditText
        _addressOut = findViewById(R.id.addressOut) as EditText
        _addressIn = findViewById(R.id.addressIn) as EditText
        _OKButton = findViewById(R.id.button_OK) as Button
//        _radioGroup = findViewById(R.id.sex_rg) as RadioGroup
//
        _OKButton!!.setOnClickListener {
            done()
        }

        //  spinner
        spinner = this.floor_spinner
        spinner!!.setOnItemSelectedListener(this)

        // Create an ArrayAdapter using a simple spinner layout and languages array
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, floor)
        // Set layout to use when the list of choices appear
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Set Adapter to Spinner
        spinner!!.setAdapter(aa)

    }

    // Get the selected radio button text using radio button on click listener
    fun radio_button_click(view: View){
        // Get the clicked radio button instance
        val radio: RadioButton = findViewById(sex_rg.checkedRadioButtonId)
        Toast.makeText(applicationContext,"您選擇了 : ${radio.text}",
            Toast.LENGTH_SHORT).show()
    }

    fun done() {
        Log.d(TAG, "done")

        if (!validate()) {
            onFailed()
            return
        }

        _OKButton!!.isEnabled = false

        android.os.Handler().postDelayed(
            {
                onSuccess()
            }, 3000
        )
    }

    fun onSuccess() {
        _OKButton!!.isEnabled = true
//        setResult(Activity.RESULT_OK, null)
//        finish()      // default
        startActivity(Intent(this, ThanksActivity::class.java))
//        finish()
    }

    fun onFailed() {
        Toast.makeText(baseContext, "資料儲存失敗", Toast.LENGTH_LONG).show()

        _OKButton!!.isEnabled = true
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

        return valid
    }

    companion object {
        private val TAG = "done"
    }

    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
    }
    override fun onNothingSelected(arg0: AdapterView<*>) {
    }
}



