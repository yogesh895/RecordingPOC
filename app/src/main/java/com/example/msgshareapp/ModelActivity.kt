package com.example.msgshareapp

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.filament.Skybox
import com.google.android.filament.utils.*
import com.yoyo.flam.utility.ViewRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.schedule


private var mViewRecorder: com.example.msgshareapp.ViewRecorder? = null
private val mAppContext: Context? = null
private var mRecording = false


class ModelActivity : AppCompatActivity() {

    companion object {
        init {
            Utils.init()

        }
    }


    private lateinit var surfaceView: TextureView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer
    private val viewerContent = AutomationEngine.ViewerContent()
    private val automation = AutomationEngine()
    private val HEIGHT = 360f
    private var bitmaplist = ArrayList<Bitmap?>()


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Iam", "Iam called")
        surfaceView = TextureView(this).apply { setContentView(this) }

        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)
        loadGlb("mbap.glb")
        modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)

        createNeutralIndirectLight()
        addNewEntity()

        var capturer = ViewRecorder()
        mViewRecorder?.setRecordedView(surfaceView)

        Timer("SettingUp", false).schedule(10000) {
//            capturer.startRecording(surfaceView)
            if (mRecording) {
                stopRecord();
            } else {
                if (ActivityCompat.checkSelfPermission(this@ModelActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this@ModelActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1);

                } else {

                    startRecord(surfaceView);

                }

            }
        }



    }

    private val mOnErrorListener =
        MediaRecorder.OnErrorListener { mr, what, extra ->
            Log.e("yoyoyoyo", "MediaRecorder error: type = $what, code = $extra")
            mViewRecorder!!.reset()
            mViewRecorder!!.release()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecord(surfaceView: TextureView) {
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

        mViewRecorder!!.setRecordedView(surfaceView)
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

    @Throws(IOException::class)
    private fun saveBitmap(@NonNull name: String, bitmap: Bitmap) {
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

    @RequiresApi(Build.VERSION_CODES.N)
    fun recordView(key: String, view: View): Bitmap? {
        var bitmap: Bitmap? = null
        when (view) {
            is SurfaceView -> {
                Log.d("Surface View", "Its a Surface View Baby")
                bitmap = recordSurfaceView(key, view)
            }
            is TextureView -> {
                Log.d("Texture View", "It's a Texture View Baby")
                bitmap = recordTextureView(key, view)
            }
        }
        return bitmap
    }


    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = assets?.open(assetName)
        val bytes = input?.let { ByteArray(it.available()) }
        input?.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun recordSurfaceView(key: String, surfaceView: SurfaceView): Bitmap {
        val recordBitmap = createBitmap(surfaceView)
        PixelCopy.request(surfaceView.holder.surface, recordBitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                Log.d("Yeahh", "Yeahh")
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
                Log.d("Yeahh", "Yeahh")
                saveBitmap(key, recordBitmap)
            }
        }, Handler(Looper.getMainLooper()))
        return recordBitmap
    }

    private fun createBitmap(view: View): Bitmap {
        val height: Float
        val width: Float

        val surfaceWidth = view.measuredWidth
        val surfaceHeight = view.measuredHeight


        Log.d("Surface View Width", surfaceWidth.toString())
        Log.d("Surface View Height", surfaceHeight.toString())
        if (surfaceHeight > surfaceWidth) {
            width = HEIGHT
            height = width / surfaceWidth * surfaceHeight
        } else {
            height = HEIGHT
            width = height / surfaceHeight * surfaceWidth
        }

        return Bitmap.createBitmap(
            width.toInt(),
            height.toInt(),
            Bitmap.Config.RGB_565
        )
    }

    private fun createNeutralIndirectLight() {
        val engine = modelViewer.engine
        readCompressedAsset("test_ibl.ktx").let {
            modelViewer.scene.indirectLight = KTX1Loader.createIndirectLight(engine, it)
            modelViewer.scene.indirectLight!!.intensity = 40_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }

        readCompressedAsset("test_skybox.ktx").let {
            modelViewer.scene.skybox = KTX1Loader.createSkybox(engine, it)
        }
    }

    private fun addNewEntity() {
        val asset = modelViewer.asset!!
        val rm = modelViewer.engine.renderableManager

        for (entity in asset.entities) {
//            Log.d("ASSET", asset.getName(entity))
            val renderable = rm.getInstance(entity)
            if (renderable == 0) {
                continue
            }

            Log.d("ASSET", entity.getTransform().toString())
            modelViewer.scene.addEntity(entity)
//            Log.d("ASSET", asset.getName(entity))
            if (asset.getName(entity) == "head") {
                rm.setLayerMask(renderable, 0xff, 0x00)
            }
            val material = rm.getMaterialInstanceAt(renderable, 0)
            material.setParameter("emissiveFactor", 0f, 0f, 0f)
        }
    }

//    private fun loadEnvironment() {
//        // Create the indirect light source and add it to the scene.
//        var buffer = readAsset("test_ibl.ktx")
//        KtxLoader.createIndirectLight(modelViewer.engine, buffer).apply {
//            intensity = 50_000f
//            modelViewer.scene.indirectLight = this
//        }
//
//        // Create the sky box and add it to the scene.
//        buffer = readAsset("test_skybox.ktx")
//        KtxLoader.createSkybox(modelViewer.engine, buffer).apply {
//            modelViewer.scene.skybox = this
//        }
//    }

    private fun createBackground() {

    }

    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    applyAnimation(2, seconds.toFloat())
                }
                updateBoneMatrices()
            }
            modelViewer.render(currentTime)
            modelViewer.asset?.apply {
                modelViewer.transformToUnitCube()
//                modelViewer.clearRootTransform()
//                val rootTransform = this.root.getTransform()
//                val degrees = 20f * seconds.toFloat()
//                val zAxis = Float3(0f, -1f, 1f)
//                this.root.setTransform(rootTransform * rotation(zAxis, degrees))
            }

        }
    }

    private fun Int.getTransform(): Mat4 {
        val tm = modelViewer.engine.transformManager
        val arr1 = FloatArray(16)
        return Mat4.of(*tm.getTransform(tm.getInstance(this), arr1))
//        return Mat4.of(0.0f)

    }


    private fun Int.setTransform(mat: Mat4) {
        val tm = modelViewer.engine.transformManager
        tm.setTransform(tm.getInstance(this), mat.toFloatArray())
    }

//    private val frameCallback = object : Choreographer.FrameCallback {
//        override fun doFrame(currentTime: Long) {
//            choreographer.postFrameCallback(this)
//            modelViewer.render(currentTime)
//        }
//    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)

    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
        stopRecord()
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        stopRecord()
    }

    private fun loadGlb(glbName: String) {
        val buffer = assets?.open(glbName).use { input ->
            val bytes = input?.let { ByteArray(it.available()) }
            input?.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.loadModelGltfAsync(buffer) { uri -> readCompressedAsset("$uri") }
        updateRootTransform()
    }

    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun loadGltf(name: String) {
        val buffer = readAsset("${name}.gltf")
        modelViewer.loadModelGltf(buffer) { uri -> readAsset("$uri") }
        modelViewer.transformToUnitCube()
    }


}


