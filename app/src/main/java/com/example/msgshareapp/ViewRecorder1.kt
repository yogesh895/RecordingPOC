package com.yoyo.flam.utility

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.msgshareapp.RecordViewModel
import com.example.msgshareapp.RecordingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jcodec.api.android.AndroidSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class ViewRecorder {
    private val HEIGHT = 360F
    private var bitmaplist = ArrayList<Bitmap?>()


    sealed class CallbackHandler<T>(
        val data: T? = null,
        val error: String? = null
    ) {
        class Success<T>(data: T) : CallbackHandler<T>(data = data)
        class Failure<T>(error: String) : CallbackHandler<T>(error = error)
        class Loading<T>() : CallbackHandler<T>()
    }


    private val _recordingView = MutableLiveData<RecordingResult<RecordViewModel>>()
    val recordingView: LiveData<RecordingResult<RecordViewModel>>
        get() = _recordingView

    fun startRecording(view: View) {
        var capturing = false
        var fps = 30
        var count = 0
        val mHandler = Handler(Looper.getMainLooper())
        var recordBitmap: Bitmap? = null
        val numberOfFrames: Int = 100
        val fileName = "Flam_" + "${System.currentTimeMillis()}.mp4"
        val videoFile = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).toString() + File.separator + fileName
        val file = File(videoFile)

        var out: SeekableByteChannel? = NIOUtils.writableFileChannel(file.toString())

        val encoder = AndroidSequenceEncoder(out, Rational.R(10, 1))

        try {
            val width = view.measuredWidth
            val height = view.measuredHeight

        } catch (e: Exception) {
            Log.e("Error in Encoding", "$e")
            _recordingView.postValue(RecordingResult.Error("Something went wrong!"))
            throw e
        } finally {
//            encoder.finish()
//            NIOUtils.closeQuietly(out)
        }


        val newFrame: Runnable = object : Runnable {

            override fun run() {
                when (capturing && count<numberOfFrames) {
                    true->{
                        val successData = RecordViewModel(file.toString(),fileName, (count/numberOfFrames *100).toFloat() )
                        _recordingView.postValue(RecordingResult.Success(successData))

                        recordBitmap = recordView("ScreenRecord", view)
                        // bitmaplist.add(recordBitmap)
                        Log.d("Capturing View", "Frames Done")
                        encoder.encodeImage(recordBitmap)
                        count++
                        mHandler.postDelayed(this, (1000 / fps).toLong())
                    }
                    else -> {
                        capturing = false
                        encoder.finish();
                        NIOUtils.closeQuietly(out)
                        mHandler.removeCallbacks(this)
                        Log.d("Yeah Successs Data Pushed", "Yeahh Success Data Pushed")
                        val successData = RecordViewModel(file.toString(),fileName, 100F )
                        _recordingView.postValue(RecordingResult.Success(successData))
                    }
                }
        }

        }

        fun startCapturing() {
            capturing = true
            mHandler.postDelayed(newFrame, (1000 / fps).toLong())
        }

        CoroutineScope(Dispatchers.IO).launch {
            startCapturing()
        }








    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun recordView(key: String, view: View): Bitmap? {
        var bitmap: Bitmap? = null
        when (view) {
            is SurfaceView -> {
                bitmap = recordSurfaceView(key, view)
            }
            is TextureView -> {
                bitmap = recordTextureView(key, view)
            }
        }
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun recordSurfaceView(key: String, surfaceView: SurfaceView): Bitmap {
        val recordBitmap = createBitmap(surfaceView)
        PixelCopy.request(surfaceView.holder.surface, recordBitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                Log.d("Record Surface View", "Successfully Captured Surface Bitmap")
//                saveBitmap(key, recordBitmap)
            }
        }, Handler(Looper.getMainLooper()))
        return recordBitmap
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun recordTextureView(key: String, textureView: TextureView): Bitmap {
        val recordBitmap = createBitmap(textureView)
        val surface = Surface(textureView.surfaceTexture)
        PixelCopy.request(surface, recordBitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                Log.d("Record Texture View", "Successfully Captured Texture Bitmap")
//                saveBitmap(key, recordBitmap)
            }
        }, Handler(Looper.getMainLooper()))
        return recordBitmap
    }

    private fun createBitmap(view: View): Bitmap {
        var height: Float
        var width: Float

        val surfaceWidth = view.measuredWidth
        val surfaceHeight = view.measuredHeight

        if (surfaceHeight > surfaceWidth) {
            width = HEIGHT
            height = width / surfaceWidth * surfaceHeight

            if (width.toInt() % 2 != 0) width++
            if (height.toInt() % 2 != 0) height ++
            Log.d("Width", width.toString())
            Log.d("Height", height.toString())
        } else {
            height = HEIGHT
            width = height / surfaceHeight * surfaceWidth

            if (width.toInt() % 2 != 0) width++
            if (height.toInt() % 2 != 0) height ++
            Log.d("Width", width.toString())
            Log.d("Height", height.toString())
        }

        return Bitmap.createBitmap(
            width.toInt(), height.toInt(), Bitmap.Config.RGB_565
        )
    }

    @Throws(IOException::class)
    private fun saveBitmap(
        @NonNull name: String, bitmap: Bitmap, contentResolver: ContentResolver
    ) {
        val saved: Boolean
        val fos: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/hello")
            val imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            resolver.openOutputStream(imageUri!!)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).toString() + File.separator + "hello"
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, "$name.png")
            FileOutputStream(image)
        }
        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos!!.flush()
        fos.close()
    }
}