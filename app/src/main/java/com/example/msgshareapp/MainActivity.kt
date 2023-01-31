package com.example.msgshareapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var mTextView: TextView? = null
    private val mAppContext: Context? = null
    private var mRecording = false
    private var mViewRecorder: com.example.msgshareapp.ViewRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        mTextView = TextView(this).apply { setContentView(this) }

        val TAG = "MainActivityHere"
        val buttonClick = findViewById<Button>(R.id.btnShowToast)
        val buttonDoubleClick = findViewById<Button>(R.id.btnRecordBabe)

        if (mTextView != null) {
            mViewRecorder?.setRecordedView(mTextView!!)
        }
        buttonDoubleClick.setOnClickListener{
            Log.i("Main_Activity", "Button is Clicked Babe")
            Toast.makeText(this, "Button is Clicked", Toast.LENGTH_SHORT).show()

            if (mRecording) {
                stopRecord();
            } else {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1);

                } else {

                    startRecord();

                }

            }

        }
        buttonClick.setOnClickListener {
            Log.i("Main_Activity", "Button is Clicked")
            Toast.makeText(this, "Button is Clicked", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ModelActivity::class.java)


            startActivity(intent)
        }

        val buttonSendMsgToNextActivity = findViewById<Button>(R.id.btnSendMsg)
        buttonSendMsgToNextActivity.setOnClickListener {
            val msgToSent = findViewById<TextView>(R.id.etUserMessage)
            val message: String = msgToSent.text.toString()
//            Toast.makeText(this,message,Toast.LENGTH_SHORT).show()

            val intent = Intent(this, CoroutineActivity::class.java)
            intent.putExtra("user_msg", message)
            startActivity(intent)
        }

        val shareButton = findViewById<Button>(R.id.shareBtn)
        shareButton.setOnClickListener {
            val msgToSent = findViewById<TextView>(R.id.etUserMessage)
            val message: String = msgToSent.text.toString()
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, message)
            intent.type = "text/plain"
            startActivity(Intent.createChooser(intent, "Share to following:"))
        }

        val recycleViewBtn = findViewById<Button>(R.id.recycleViewBtn)
        Log.d("TED1", "clicked")
        recycleViewBtn.setOnClickListener {
            Log.d("TED1", "clicked here")
            val intent1 = Intent(this, RatingMainActivity::class.java)
            startActivity(intent1)
        }


    }

    private val mOnErrorListener =
        MediaRecorder.OnErrorListener { mr, what, extra ->
            Log.e("yoyoyoyo", "MediaRecorder error: type = $what, code = $extra")
            mViewRecorder!!.reset()
            mViewRecorder!!.release()
        }

    override fun onPause() {
        super.onPause()
        stopRecord();
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecord() {
        val directory: File? = mAppContext?.externalCacheDir
        if (directory != null) {
            directory.mkdirs();
            if (!directory.exists()) {
                Log.w("YOYOYO", "startRecord failed: " + directory + " does not exist!");
                return;
            }
        }

        mViewRecorder = com.example.msgshareapp.ViewRecorder()
        mViewRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mViewRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mViewRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mViewRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mViewRecorder!!.setVideoFrameRate(5); // 5fps
        mViewRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mViewRecorder!!.setVideoSize(720, 1280);
        mViewRecorder!!.setVideoEncodingBitRate(2000 * 1000);
        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).toString() + File.separator + "hello.mp4"
        val file = File(imagesDir)
        mViewRecorder!!.setOutputFile(file);
        mViewRecorder!!.setOnErrorListener(mOnErrorListener);

        if (mTextView != null) {
            mViewRecorder!!.setRecordedView(mTextView!!)
        }
        try {
            mViewRecorder!!.prepare()
            mViewRecorder!!.start()
        } catch (e: IOException) {
            Log.e("YOYO", "startRecord failed", e)
            return
        }

        Log.d("YOYO", "startRecord successfully!")
        mRecording = true

    }

    private fun stopRecord() {
        try {
            mViewRecorder!!.stop()
            mViewRecorder!!.reset()
            mViewRecorder!!.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mRecording = false
        Log.d("YOYOYO", "stopRecord successfully!")
    }
}