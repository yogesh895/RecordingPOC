package com.example.msgshareapp

//import com.xyoye.common_component.database.DatabaseManager
//import com.xyoye.common_component.source.media.TorrentMediaSource
//import com.xyoye.common_component.utils.JsonHelper
//import com.xyoye.common_component.utils.MediaUtils
//import com.xyoye.common_component.utils.PathHelper
//import com.xyoye.data_component.entity.PlayHistoryEntity
//import com.xyoye.player.surface.InterSurfaceView
//import org.videolan.libvlc.util.VLCVideoLayout

//import androidx.test.core.app.ApplicationProvider.getApplicationContext

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.example.msgshareapp.BitmapToVideoEncoder.IBitmapToVideoEncoderCallback
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * Created by xyoye on 2022/1/15.
 */

object PlayRecorder : Application() {
    private const val HEIGHT = 150f

//    fun recordProgress(source: BaseVideoSource, position: Long, duration: Long) {
//        GlobalScope.launch(context = Dispatchers.IO) {
//            var torrentPath: String? = null
//            var torrentIndex = -1
//            if (source is TorrentMediaSource) {
//                torrentPath = source.getTorrentPath()
//                torrentIndex = source.getTorrentIndex()
//            }
//
//            val history = PlayHistoryEntity(
//                0,
//                source.getVideoTitle(),
//                source.getVideoUrl(),
//                source.getMediaType(),
//                position,
//                duration,
//                Date(),
//                source.getDanmuPath(),
//                source.getEpisodeId(),
//                source.getSubtitlePath(),
//                torrentPath,
//                torrentIndex,
//                JsonHelper.toJson(source.getHttpHeader()),
//                null,
//                source.getUniqueKey()
//            )
//
//            DatabaseManager.instance.getPlayHistoryDao()
//                .insert(history)
//
//            //部分视频无法获取到视频时长，播放后再更新时长
//            if (source.getMediaType() == MediaType.LOCAL_STORAGE) {
//                DatabaseManager.instance
//                    .getVideoDao()
//                    .updateDuration(duration, source.getVideoUrl())
//            }
//        }
//    }
//
//    fun recordImage(key: String, renderView: InterSurfaceView?) {
//        val view = renderView?.getView()
//            ?: return
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//            return
//        }
//        try {
//            recordTextureView(key, view)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    var bitmapToVideoEncoder = BitmapToVideoEncoder(object : IBitmapToVideoEncoderCallback {
        override fun onEncodingComplete(outputFile: File?) {
//            Toast.makeText(this, "Encoding complete!", Toast.LENGTH_LONG).show()
            Log.d("Yeahhhhhhhhhh!!!!", "Enccccoddddinnnnggg Donnneee Babeeee!")
        }
    })


    private fun saveImage(file: File, bitmap: Bitmap): Boolean {
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            closeIO(fileOutputStream)
        }
        return false
    }


    private fun closeIO(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
            // ignore
        }
    }
}