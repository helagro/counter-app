package com.hlag.counter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CounterReceiver : BroadcastReceiver() {
    companion object {
        const val INTENT_DELTA_NUM = "add to counter"
        const val TAG = "Receiver"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "GET_NUM" -> {

            }
            "ADD_TO_NUM" -> {
                if (context != null) {
                    StorageHelper.loadData(context)
                    StorageHelper.loadI(context)
                    Log.d(TAG, "I loaded: " + StorageHelper.i)
                    StorageHelper.i += intent.getIntExtra(INTENT_DELTA_NUM, 0)
                    Log.d(TAG, "I after: " + StorageHelper.i)

                    StorageHelper.writeData(context)
                }
            }
        }
    }

}