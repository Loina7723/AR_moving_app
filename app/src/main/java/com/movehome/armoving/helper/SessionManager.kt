package com.movehome.armoving.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.movehome.armoving.model.User
import org.json.JSONException
import org.json.JSONObject

@SuppressLint("CommitPrefEdits")
class SessionManager(_context: Context) {
    private val TAG = SessionManager::class.java.simpleName
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    var user: User? = null

    init {
        pref = _context.getSharedPreferences("ARmovingAppPref", Context.MODE_PRIVATE)
        editor = pref.edit()
    }

    companion object{
        private var INSTANCE: SessionManager? = null
        fun getInstance(context: Context): SessionManager? {
            if (INSTANCE == null) INSTANCE = SessionManager(context)
            return INSTANCE
        }
    }

    fun createLoginSession(id: String, userJsonObjStr: String) {
        editor.putBoolean("IsLoggidIn", true)
        editor.putString("id", id)
        editor.putString("user", userJsonObjStr)
        editor.commit()
    }

    val userDetail: User
        get() {
            if (user == null) user = User()
            val userJsonObjStr = pref.getString("user", null)
            if (userJsonObjStr != null) {
                try {
                    val obj = JSONObject(userJsonObjStr)
                    user!!.id = obj.getString("id")
                    user!!.name = obj.getString("name")
                    user!!.token = obj.getString("token")
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.d(TAG, "user detail data no json")
                }
            }
            return user!!
        }

}