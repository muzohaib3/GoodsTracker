package com.example.scanmate.util

import android.content.Context
import android.os.Parcelable
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.PREF

object LocalPreferences {

    fun put(context: Context, key: String, value: String) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(key, value).apply()
    }

    fun put(context: Context, key: String, value: Int) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putInt(key, value).apply()
    }

    fun put(context: Context, key: String, value: Boolean) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putBoolean(key, value).apply()
    }


    fun getBoolean(context: Context, key: String): Boolean {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getBoolean(key, false)
    }

    fun getString(context: Context, key: String): String? {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getString(key, null)
    }

    fun getInt(context: Context, key: String): Int {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getInt(key, 0)
    }


    object AppLoginPreferences {

        const val PREF = "Pref"
        const val userNo = "userNo"
        const val isLogin = "isLogin"
        const val userDesignation = "userDesignation"
        const val loginTime = "loginTime"
        const val userName = "userName"
        const val userLocNo = "userLocNo"
        const val busLocNo = "busLocNo"
        const val whNo = "whNo"
        const val rackNo = "rackNo"
        const val shelfNo = "shelfNo"
        const val isRefreshRequired = "isRefreshRequired"
        const val isSpinnerSelected = "isSpinnerSelected"
        const val scanCarton = "scanCarton"


        // save data from busLoc to pallet
        const val busLoc = "busLocName"
        const val warehouse = "whName"
        const val rack = "rackName"
        const val shelf = "shelfName"
        const val pallets = "pallets"
    }

    object AppConstants{

        const val orgBusLocNo = "OrgBusLocNo"

    }

}