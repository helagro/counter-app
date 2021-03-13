package com.hlag.counter

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs


class MainActivity : AppCompatActivity(), View.OnClickListener{
    private val TAG = "MainActivity"
    lateinit var sp : SharedPreferences
    lateinit var rules: ArrayList<Rule>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0);
            return
        }

        setContentView(R.layout.activity_main)

        rules = StorageHelper.loadData(this)

        val repeatListener = RepeatListener(400, 120, this)
        add_btn.setOnTouchListener(repeatListener)
        remove_btn.setOnTouchListener(repeatListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        StorageHelper.loadI(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.set_url -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Title")
                val input = EditText(this)
                input.inputType =
                    InputType.TYPE_CLASS_TEXT
                input.setText(StorageHelper.url)
                builder.setView(input)

                builder.setPositiveButton("OK"
                ) { _, _ ->
                    StorageHelper.setServerUrl(input.text.toString(), this)
                }
                builder.setNegativeButton("Cancel"
                ) { dialog, _ -> dialog.cancel() }

                builder.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            add_btn -> {
                changeI(StorageHelper.i + 1)
            }
            remove_btn -> {
                changeI(StorageHelper.i - 1)
            }
            day_over_btn -> {
                changeI(0)
                StorageHelper.dayEnded(this)
                //testing
                StorageHelper.writeData(this)
            }
        }
        showToast("Reacting...")
    }

    private var curToast: Toast? = null
    private fun showToast(text: String?) {
        curToast?.cancel()
        curToast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        curToast?.show()
    }

    fun changeI(newI : Int){
        StorageHelper.i = newI
        setNumText()
        updateRulesView()
    }

    private fun setNumText(){
        main_num_view.text = StorageHelper.i.toString()
    }

    private fun updateRulesView(){
        rules_disply.text = ""
        rules.filter {
            if(it.limit > 0){StorageHelper.i >= it.limit}
            else{StorageHelper.i <= it.limit}
        }.forEach {
            var str = ""
            val m: Matcher = Pattern.compile("\\((.*?)\\)").matcher(it.text)
            while (m.find()) {
                str = m.group(1)
                str = calcEquation(str.replace("i", StorageHelper.i.toString()))
            }
            rules_disply.append("${it.limit}  ${it.text.replace("\\((.*?)\\)".toRegex(), str)}\n")
        }
    }

    private fun calcEquation(equation: String): String{
        val sc = Scanner(equation)

        val firstValue = abs(sc.findInLine("-?[1-9]\\d*|0").toInt())

        // get everything which follows and is not a number (might contain white spaces)
        val operator: String = (sc.findInLine("[^0-9]*")?: return equation).trim()

        val secondValue: Int = sc.findInLine("[0-9]*").toInt()
        val res = when (operator) {
            "+" -> firstValue + secondValue
            "-" -> firstValue - secondValue
            "/" -> firstValue / secondValue
            "*" -> firstValue * secondValue
            "%" -> firstValue % secondValue
            else -> throw RuntimeException("unknown operator: $operator")
        }

        return res.toString()
    }

    override fun onPause() {
        super.onPause()
        StorageHelper.writeData(this)
    }

}
