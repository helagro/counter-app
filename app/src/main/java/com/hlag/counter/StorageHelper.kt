package com.hlag.counter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList


class StorageHelper {
    companion object {
        private const val TAG = "StorageHelper"
        private const val COUNTER_EVENTS = "counter events"
        private const val NOT_LOADED = Integer.MAX_VALUE
        private const val LAST_DAY_END = "last_dayEnd"
        lateinit var url : String

        private lateinit var iHistory: ArrayList<IEvent>
        var storageI = 0
        var i = NOT_LOADED

        fun getSP(context: Context): SharedPreferences {
            return context.getSharedPreferences("std", Context.MODE_PRIVATE)
        }

        fun loadI(context: Context) {
            val iEventsString = getSP(context).getString(COUNTER_EVENTS, "[]")
            if (i == NOT_LOADED) {
                val type: Type = object : TypeToken<List<IEvent?>?>() {}.type
                iHistory = Gson().fromJson(iEventsString, type)

                for (iEvent in iHistory){
                    storageI += iEvent.delta
                }
            }
            if(context is MainActivity){
                context.changeI(storageI)
            }
            else {
                i = storageI
            }
            syncWithServer(context, iEventsString!!)
        }

        fun loadData(context: Context): ArrayList<Rule> {
            url = getSP(context).getString("url", "http://192.168.1.46:3000")!!

            val text = StringBuilder()

            try {
                val br =
                    BufferedReader(FileReader(File("/storage/emulated/0/Mega Sync/Counter Rules.txt")))

                var line: String?
                while (br.readLine().also { line = it } != null) {
                    text.append(line)
                }
                br.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val turnsType = object : TypeToken<List<Rule>>() {}.type
            return Gson().fromJson<ArrayList<Rule>>(text.toString(), turnsType)
        }

        private fun syncWithServer(context: Context, iHistoryString: String) {
            val queue = Volley.newRequestQueue(context)

            val postData: MutableMap<String, String> =
                HashMap()
            postData[LAST_DAY_END] = getSP(context).getLong(LAST_DAY_END, 0).toString()
            postData["iHistory"] = iHistoryString

            val req: StringRequest = object : StringRequest(
                Method.POST,
                url,
                Response.Listener { response ->
                    val resObj = Gson().fromJson(response, ResObj::class.java)
                    val changesWhileSynking = i - storageI

                    iHistory.clear()
                    if(changesWhileSynking != 0){
                        iHistory.add(IEvent(Calendar.getInstance().timeInMillis, changesWhileSynking))
                    }
                    iHistory.add(IEvent(-1, resObj.i))
                    getSP(context).edit().putString(COUNTER_EVENTS, Gson().toJson(iHistory)).apply()

                    if(context is MainActivity){
                        context.changeI(resObj.i)
                    } else{
                        i = resObj.i
                    }
                    storageI = resObj.i
                },
                Response.ErrorListener { error -> Log.d(TAG, error.toString()) }) {
                override fun getParams(): Map<String, String> {
                    return postData
                }
                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> =
                        HashMap()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }

            queue.add(req)
        }

        fun writeData(context: Context) {
            iHistory.add(IEvent(Calendar.getInstance().timeInMillis, i - storageI))
            storageI = i

            getSP(context).edit().putString(COUNTER_EVENTS, Gson().toJson(iHistory)).apply()
            syncWithServer(context, Gson().toJson(iHistory))
        }

        fun setServerUrl(url: String, context: Context){
            this.url = url
            getSP(context).edit().putString("url", url).apply()
        }

        fun dayEnded(context: Context){
            iHistory.clear()
            val curTime = Calendar.getInstance().timeInMillis

            val editor = getSP(context).edit()
            editor.putString(COUNTER_EVENTS, "")
            editor.putLong(LAST_DAY_END, curTime)
            editor.apply()

            storageI = 0
            i = storageI
        }
    }
}