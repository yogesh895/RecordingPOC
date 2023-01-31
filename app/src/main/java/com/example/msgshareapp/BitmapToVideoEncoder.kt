package com.example.msgshareapp

import android.graphics.Bitmap
import android.media.*
import android.media.MediaCodecInfo.CodecCapabilities
import android.util.Log
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch


class BitmapToVideoEncoder(private val mCallback: IBitmapToVideoEncoderCallback) {
    private var mOutputFile: File? = null
    private var mEncodeQueue: Queue<Bitmap?> = ConcurrentLinkedQueue<Bitmap>()
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private val mFrameSync = Any()
    private var mNewFrameLatch: CountDownLatch? = null
    private var mGenerateIndex = 0
    private var mTrackIndex = 0
    private var mNoMoreFrames = false
    private var mAbort = false

    interface IBitmapToVideoEncoderCallback {
        fun onEncodingComplete(outputFile: File?)
    }

    val isEncodingStarted: Boolean
        get() = mediaCodec != null && mediaMuxer != null && !mNoMoreFrames && !mAbort

//    fun getActiveBitmaps(): Int {
//        return mEncodeQueue.field
//    }

    fun startEncoding(width: Int, height: Int, outputFile: File) {
        mWidth = width
        mHeight = height
        mOutputFile = outputFile
        val outputFileString: String
        outputFileString = try {
            outputFile.canonicalPath

        } catch (e: IOException) {
            Log.e(
                TAG,
                "Unable to get path for $outputFile"
            )
            Log.d("Output File String", "Path Not Found")
            return
        }

        val codecInfo = selectCodec(MIME_TYPE)
        if (codecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE)
            return
        }
        Log.d(TAG, "found codec: " + codecInfo.name)
        val colorFormat: Int
        colorFormat = try {
            selectColorFormat(codecInfo, MIME_TYPE)
        } catch (e: Exception) {
            CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
        mediaCodec = try {
            MediaCodec.createByCodecName(codecInfo.name)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to create MediaCodec " + e.message)
            return
        }
        val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec!!.start()
        Log.d("Output File String", outputFileString)
        mediaMuxer = try {
            MediaMuxer(outputFileString, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            Log.e(TAG, "MediaMuxer creation failed. " + e.message)
            return
        }
        Log.d(TAG, "Initialization complete. Starting encoder...")
        Completable.fromAction { encode() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }

    fun stopEncoding() {
        if (mediaCodec == null || mediaMuxer == null) {
            Log.d(TAG, "Failed to stop encoding since it never started")
            return
        }
        Log.d(TAG, "Stopping encoding")
        mNoMoreFrames = true
        synchronized(mFrameSync) {
            if (mNewFrameLatch != null && mNewFrameLatch!!.count > 0) {
                mNewFrameLatch!!.countDown()
            }
        }
    }

    fun abortEncoding() {
        if (mediaCodec == null || mediaMuxer == null) {
            Log.d(TAG, "Failed to abort encoding since it never started")
            return
        }
        Log.d(TAG, "Aborting encoding")
        mNoMoreFrames = true
        mAbort = true
        mEncodeQueue = ConcurrentLinkedQueue<Bitmap?>() // Drop all frames
        synchronized(mFrameSync) {
            if (mNewFrameLatch != null && mNewFrameLatch!!.count > 0) {
                mNewFrameLatch!!.countDown()
            }
        }
    }

    fun queueFrame(bitmap: Bitmap?) {
        if (mediaCodec == null || mediaMuxer == null) {
            Log.d(TAG, "Failed to queue frame. Encoding not started")
            return
        }
        Log.d(TAG, "Queueing frame")
        mEncodeQueue.add(bitmap)
        synchronized(mFrameSync) {
            if (mNewFrameLatch != null && mNewFrameLatch!!.count > 0) {
                mNewFrameLatch!!.countDown()
            }
        }
    }

    private fun encode() {
        Log.d(TAG, "Encoder started")
        while (true) {
            if (mNoMoreFrames && mEncodeQueue.size == 0) break
            var bitmap = mEncodeQueue.poll()
            if (bitmap == null) {
                synchronized(mFrameSync) { mNewFrameLatch = CountDownLatch(1) }
                try {
                    mNewFrameLatch!!.await()
                } catch (e: InterruptedException) {
                }
                bitmap = mEncodeQueue.poll()
            }
            if (bitmap == null) continue
            val byteConvertFrame = getNV21(bitmap.width, bitmap.height, bitmap)
            val TIMEOUT_USEC: Long = 500000
            val inputBufIndex = mediaCodec!!.dequeueInputBuffer(TIMEOUT_USEC)
            val ptsUsec = computePresentationTime(mGenerateIndex.toLong(), FRAME_RATE)
            if (inputBufIndex >= 0) {
                val inputBuffer = mediaCodec!!.getInputBuffer(inputBufIndex)
                inputBuffer!!.clear()
                inputBuffer.put(byteConvertFrame)
                mediaCodec!!.queueInputBuffer(inputBufIndex, 0, byteConvertFrame.size, ptsUsec, 0)
                mGenerateIndex++
            }
            val mBufferInfo = MediaCodec.BufferInfo()
            val encoderStatus = mediaCodec!!.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                Log.e(TAG, "No output from encoder available")
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                val newFormat = mediaCodec!!.outputFormat
                mTrackIndex = mediaMuxer!!.addTrack(newFormat)
                mediaMuxer!!.start()
            } else if (encoderStatus < 0) {
                Log.e(
                    TAG,
                    "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus"
                )
            } else if (mBufferInfo.size != 0) {
                val encodedData = mediaCodec!!.getOutputBuffer(encoderStatus)
                if (encodedData == null) {
                    Log.e(
                        TAG,
                        "encoderOutputBuffer $encoderStatus was null"
                    )
                } else {
                    encodedData.position(mBufferInfo.offset)
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size)
                    mediaMuxer!!.writeSampleData(mTrackIndex, encodedData, mBufferInfo)
                    mediaCodec!!.releaseOutputBuffer(encoderStatus, false)
                }
            }
        }
        release()
        if (mAbort) {
            mOutputFile!!.delete()
        } else {
            mCallback.onEncodingComplete(mOutputFile)
        }
    }

    private fun release() {
        if (mediaCodec != null) {
            mediaCodec!!.stop()
            mediaCodec!!.release()
            mediaCodec = null
            Log.d(TAG, "RELEASE CODEC")
        }
        if (mediaMuxer != null) {
            mediaMuxer!!.stop()
            mediaMuxer!!.release()
            mediaMuxer = null
            Log.d(TAG, "RELEASE MUXER")
        }
    }

    private fun getNV21(inputWidth: Int, inputHeight: Int, scaled: Bitmap): ByteArray {
        val argb = IntArray(inputWidth * inputHeight)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
        scaled.recycle()
        return yuv
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var a: Int
        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                a = argb[index] and -0x1000000 shr 24 // a is not used obviously
                R = argb[index] and 0xff0000 shr 16
                G = argb[index] and 0xff00 shr 8
                B = argb[index] and 0xff shr 0
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                }
                index++
            }
        }
    }

    private fun computePresentationTime(frameIndex: Long, framerate: Int): Long {
        return 132 + frameIndex * 1000000 / framerate
    }

    companion object {
        private val TAG = BitmapToVideoEncoder::class.java.simpleName
        private const val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        private var mWidth = 0
        private var mHeight = 0
        private const val BIT_RATE = 16000000
        private const val FRAME_RATE = 30 // Frames per second
        private const val I_FRAME_INTERVAL = 1
        private fun selectCodec(mimeType: String): MediaCodecInfo? {
            val numCodecs = MediaCodecList.getCodecCount()
            for (i in 0 until numCodecs) {
                val codecInfo = MediaCodecList.getCodecInfoAt(i)
                if (!codecInfo.isEncoder) {
                    continue
                }
                val types = codecInfo.supportedTypes
                for (j in types.indices) {
                    if (types[j].equals(mimeType, ignoreCase = true)) {
                        return codecInfo
                    }
                }
            }
            return null
        }

        private fun selectColorFormat(
            codecInfo: MediaCodecInfo,
            mimeType: String
        ): Int {
            val capabilities = codecInfo
                .getCapabilitiesForType(mimeType)
            for (i in capabilities.colorFormats.indices) {
                val colorFormat = capabilities.colorFormats[i]
                if (isRecognizedFormat(colorFormat)) {
                    return colorFormat
                }
            }
            return 0 // not reached
        }

        private fun isRecognizedFormat(colorFormat: Int): Boolean {
            return when (colorFormat) {
                CodecCapabilities.COLOR_FormatYUV420Planar, CodecCapabilities.COLOR_FormatYUV420PackedPlanar, CodecCapabilities.COLOR_FormatYUV420SemiPlanar, CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
                else -> false
            }
        }
    }
}