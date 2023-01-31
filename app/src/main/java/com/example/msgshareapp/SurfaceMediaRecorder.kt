package com.example.msgshareapp

import android.graphics.Canvas
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean


/**
 * This class extends [MediaRecorder] and manages to compose each video frame for recording.
 * Two extra initialization steps before [.start],
 * <pre>
 * [.setWorkerLooper]
 * [.setVideoFrameDrawer]
</pre> *
 *
 * Also you can use it as same as [MediaRecorder] for other functions.
 *
 *
 *  By the way, one more error type [.MEDIA_RECORDER_ERROR_SURFACE] is defined for surface error.
 *
 * Created by z4hyoung on 2017/11/8.
 */
open class SurfaceMediaRecorder : MediaRecorder() {
    /**
     * Interface defined for user to customize video frame composition
     */
    interface VideoFrameDrawer {
        /**
         * Called when video frame is composing
         *
         * @param canvas the canvas on which content will be drawn
         */
        fun onDraw(canvas: Canvas?)
    }

    private var mVideoSource = 0
    private var mOnErrorListener: OnErrorListener? = null
    private var mInterframeGap = DEFAULT_INTERFRAME_GAP // 1000 milliseconds as default
    private var mSurface: Surface? = null

    // if set, this class works same as MediaRecorder
    private var mInputSurface: Surface? = null
    private var mWorkerHandler: Handler? = null
    private var mVideoFrameDrawer: VideoFrameDrawer? = null

    // indicate surface composing started or not
    private val mStarted = AtomicBoolean(false)

    // indicate surface composing paused or not
    private val mPaused = AtomicBoolean(false)
    private val mWorkerRunnable: Runnable = object : Runnable {
        private fun handlerCanvasError(errorCode: Int) {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (mOnErrorListener != null) {
                mOnErrorListener!!.onError(
                    this@SurfaceMediaRecorder,
                    MEDIA_RECORDER_ERROR_SURFACE,
                    errorCode
                )
            }
        }

        override fun run() {
            if (!isRecording) {
                return
            }
            var errorCode: Int? = null
            val start = SystemClock.elapsedRealtime()
            do {
                var canvas: Canvas?
                try {
                    canvas = mSurface!!.lockCanvas(null)
                } catch (e: Exception) {
                    errorCode = MEDIA_RECORDER_ERROR_CODE_LOCK_CANVAS
                    e.printStackTrace()
                    break
                }
                mVideoFrameDrawer!!.onDraw(canvas)
                try {
                    mSurface!!.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    errorCode = MEDIA_RECORDER_ERROR_CODE_UNLOCK_CANVAS
                    e.printStackTrace()
                    break
                }
            } while (false)
            if (!isRecording) {
                return
            }
            errorCode?.let { handlerCanvasError(it) }
                ?: // delay will be reset to 0 if negative in Handler:sendMessageDelayed
                mWorkerHandler!!.postDelayed(
                    this,
                    start + mInterframeGap - SystemClock.elapsedRealtime()
                )
        }
    }

    @Throws(IllegalStateException::class)
    override fun pause() {
        if (isSurfaceAvailable) {
            mPaused.set(true)
            mWorkerHandler!!.removeCallbacks(mWorkerRunnable)
        }
        super.pause()
    }

    override fun reset() {
        localReset()
        super.reset()
    }

    @Throws(IllegalStateException::class)
    override fun resume() {
        super.resume()
        if (isSurfaceAvailable) {
            mPaused.set(false)
            mWorkerHandler!!.post(mWorkerRunnable)
        }
    }

    override fun setOnErrorListener(l: OnErrorListener) {
        super.setOnErrorListener(l)
        mOnErrorListener = l
    }

    override fun setInputSurface(surface: Surface) {
        super.setInputSurface(surface)
        mInputSurface = surface
    }

    @Throws(IllegalStateException::class)
    override fun setVideoFrameRate(rate: Int) {
        super.setVideoFrameRate(rate)
        mInterframeGap = (1000 / rate + if (1000 % rate == 0) 0 else 1).toLong()
    }

    @Throws(IllegalStateException::class)
    override fun setVideoSource(video_source: Int) {
        super.setVideoSource(video_source)
        mVideoSource = video_source
    }

    @Throws(IllegalStateException::class)
    override fun start() {
        if (isSurfaceAvailable) {
            checkNotNull(mWorkerHandler) { "worker looper is not initialized yet" }
            checkNotNull(mVideoFrameDrawer) { "video frame drawer is not initialized yet" }
        }
        super.start()
        if (isSurfaceAvailable) {
            mSurface = surface
            mStarted.set(true)
            mWorkerHandler!!.post(mWorkerRunnable)
        }
    }

    @Throws(IllegalStateException::class)
    override fun stop() {
        localReset()
        super.stop()
    }

    /**
     * Sets video frame drawer for composing.
     * @param drawer the drawer to compose frame with [Canvas]
     * @throws IllegalStateException if it is called after [.start]
     */
    @Throws(IllegalStateException::class)
    fun setVideoFrameDrawer(drawer: VideoFrameDrawer) {
        check(!isRecording) { "setVideoFrameDrawer called in an invalid state: Recording" }
        mVideoFrameDrawer = drawer
    }

    /**
     * Sets worker looper in which composing task executed
     * @param looper the looper for composing
     * @throws IllegalStateException if it is called after [.start]
     */
    @Throws(IllegalStateException::class)
    fun setWorkerLooper(looper: Looper) {
        check(!isRecording) { "setWorkerLooper called in an invalid state: Recording" }
        mWorkerHandler = Handler(looper)
    }

    /**
     * Returns whether Surface is editable
     * @return true if surface editable
     */
    protected val isSurfaceAvailable: Boolean
        protected get() = mVideoSource == VideoSource.SURFACE && mInputSurface == null
    private val isRecording: Boolean
        private get() = mStarted.get() && !mPaused.get()

    private fun localReset() {
        if (isSurfaceAvailable) {
            mStarted.compareAndSet(true, false)
            mPaused.compareAndSet(true, false)
            if (mWorkerHandler != null) {
                mWorkerHandler!!.removeCallbacks(mWorkerRunnable)
            }
        }
        mInterframeGap = DEFAULT_INTERFRAME_GAP
        mInputSurface = null
        mOnErrorListener = null
        mVideoFrameDrawer = null
        mWorkerHandler = null
    }

    companion object {
        /**
         * Surface error during recording, In this case, the application must release the
         * MediaRecorder object and instantiate a new one.
         *
         * @see android.media.MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_SURFACE = 10000

        /**
         * Surface error when getting for drawing into this [Surface].
         *
         * @see android.media.MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_CODE_LOCK_CANVAS = 1

        /**
         * Surface error when releasing and posting content to [Surface].
         *
         * @see android.media.MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_CODE_UNLOCK_CANVAS = 2

        /**
         * default inter-frame gap
         */
        private const val DEFAULT_INTERFRAME_GAP: Long = 1000
    }
}