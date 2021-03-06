package com.movehome.armoving

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.movehome.armoving.helper.SessionManager
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


        onSignupSuccess()
    }


    fun onSignupSuccess() {
        runOnUiThread({ _signupButton!!.isEnabled = true })

        val bundle: Bundle = intent.extras!!
        bundle.putString("email", email)
        bundle.putString("password", _passwordText!!.text.toString())
        bundle.putString("phone", _mobileText!!.text.toString())

        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
//        startActivity(Intent(this, StartActivity::class.java))
    }

    fun onSignupFailed() {
        Toast.makeText(baseContext, "????????????", Toast.LENGTH_LONG).show()

        _signupButton!!.isEnabled = true
    }

    fun validate(): Boolean {
        var valid = true

        val mobile = _mobileText!!.text.toString()
        val password = _passwordText!!.text.toString()
        val reEnterPassword = _reEnterPasswordText!!.text.toString()

        email = editText.text.toString().trim()


        if (mobile.isEmpty() || mobile.length != 10) {
            _mobileText!!.error = "?????????????????????"
            valid = false

        } else {
            _mobileText!!.error = null
        }

        if (email.matches(emailPattern.toRegex())) {
            editText!!.error = null
        } else {
            editText!!.error = "?????????????????????"
//            Toast.makeText(applicationContext, "?????????????????????",
//                Toast.LENGTH_SHORT).show()
            valid = false
        }

        if (password.isEmpty() || password.length < 4 || password.length > 10) {
            _passwordText!!.error = "?????????4~10??????????????????"
            valid = false
        } else {
            _passwordText!!.error = null
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length < 4 || reEnterPassword.length > 10 || reEnterPassword != password) {
            _reEnterPasswordText!!.error = "???????????????"
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