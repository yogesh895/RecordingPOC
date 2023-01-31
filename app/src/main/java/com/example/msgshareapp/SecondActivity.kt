package com.example.msgshareapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val bundle: Bundle? = intent.extras
        val msg: String = bundle!!.get("user_msg") as String
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        val newMsg = findViewById<TextView>(R.id.showMsg)
        newMsg.text = msg
    }
}