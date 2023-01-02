package com.example.msgshareapp
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonClick = findViewById<Button>(R.id.btnShowToast)
        buttonClick.setOnClickListener {
            Log.i("Main_Activity", "Button is Clicked")
            Toast.makeText(this,"Button is Clicked",Toast.LENGTH_SHORT).show()
        }

        val buttonSendMsgToNextActivity = findViewById<Button>(R.id.btnSendMsg)
        buttonSendMsgToNextActivity.setOnClickListener{
            val msgToSent = findViewById<TextView>(R.id.etUserMessage)
            val message: String = msgToSent.text.toString()
//            Toast.makeText(this,message,Toast.LENGTH_SHORT).show()

            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra("user_msg", message)
            startActivity(intent)
        }

        val shareButton = findViewById<Button>(R.id.shareBtn)
        shareButton.setOnClickListener{
            val msgToSent = findViewById<TextView>(R.id.etUserMessage)
            val message: String = msgToSent.text.toString()
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, message)
            intent.type = "text/plain"
            startActivity(Intent.createChooser(intent, "Share to following:"))
        }
    }
}