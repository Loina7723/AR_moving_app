package com.movehome.armoving

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    var _mobileText: EditText? = null
    private lateinit var editText: EditText
    private lateinit var email: String
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    var _passwordText: EditText? = null
    var _reEnterPasswordText: EditText? = null
    var _signupButton: Button? = null

    //var _loginLink: TextView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        _mobileText = findViewById(R.id.phone_number) as EditText
        _passwordText = findViewById(R.id.password) as EditText
        _reEnterPasswordText = findViewById(R.id.ensure_password) as EditText
        _signupButton = findViewById(R.id.button_signUp) as Button

        editText = findViewById(R.id.email)

        _signupButton!!.setOnClickListener {
            // Finish the registration screen and return to the Login activity
            signup()
        }
    }

    fun signup() {
        Log.d(TAG, "Signup")

        if (!validate()) {
            onSignupFailed()
            return
        }

        _signupButton!!.isEnabled = false
//
//        val mobile = _mobileText!!.text.toString()
//        val password = _passwordText!!.text.toString()
//        val reEnterPassword = _reEnterPasswordText!!.text.toString()

        val body = FormBody.Builder()
            .add("if_elev", "0")
            .add("name", email)
            .add("email", email)
            .add("password", _passwordText!!.text.toString())
            .add("phone", _mobileText!!.text.toString())
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
                        Toast.makeText(this@SignupActivity, "connect fail", Toast.LENGTH_SHORT).show()
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
                    Log.i(TAG, "user: "+user)

                    onSignupSuccess()
                }
                else{
                    onSignupFailed();
                    Log.i(TAG, "register fail")
                    runOnUiThread (object : Runnable {
                        override fun run() {
                            Toast.makeText(this@SignupActivity, "register fail", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        });


    }


    fun onSignupSuccess() {
        runOnUiThread({ _signupButton!!.isEnabled = true })
//        setResult(Activity.RESULT_OK, null)
//        finish()
//        startActivity(Intent(this, InfoActivity::class.java))
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }

    fun onSignupFailed() {
        Toast.makeText(baseContext, "註冊失敗", Toast.LENGTH_LONG).show()

        _signupButton!!.isEnabled = true
    }

    fun validate(): Boolean {
        var valid = true

        val mobile = _mobileText!!.text.toString()
        val password = _passwordText!!.text.toString()
        val reEnterPassword = _reEnterPasswordText!!.text.toString()

        email = editText.text.toString().trim()


        if (mobile.isEmpty() || mobile.length != 10) {
            _mobileText!!.error = "無效的手機號碼"
            valid = false

        } else {
            _mobileText!!.error = null
        }

        if (email.matches(emailPattern.toRegex())) {
            editText!!.error = null
        } else {
            editText!!.error = "無效的電子信箱"
//            Toast.makeText(applicationContext, "無效的電子信箱",
//                Toast.LENGTH_SHORT).show()
            valid = false
        }

        if (password.isEmpty() || password.length < 4 || password.length > 10) {
            _passwordText!!.error = "請輸入4~10位字母或數字"
            valid = false
        } else {
            _passwordText!!.error = null
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length < 4 || reEnterPassword.length > 10 || reEnterPassword != password) {
            _reEnterPasswordText!!.error = "密碼不一致"
            valid = false
        } else {
            _reEnterPasswordText!!.error = null
        }

        return valid
    }

    companion object {
        private val TAG = "Signup"
    }
}