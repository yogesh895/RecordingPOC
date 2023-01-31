package com.example.msgshareapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import kotlinx.android.synthetic.main.activity_rates.*

class RatesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rates)
        val mColors = arrayOf(
            ColorDrawable(Color.RED),
            ColorDrawable(Color.LTGRAY),
            ColorDrawable(Color.MAGENTA),
            ColorDrawable(Color.GREEN),
            ColorDrawable(Color.BLUE)
        )
        val colors = listOf(
            Color.parseColor("#ff2255"),
            Color.parseColor("#F17E48"),
            Color.parseColor("#8848F1"),
            Color.parseColor("#39B0C5"),
            Color.parseColor("#3FD348")
        )

        val images = listOf(
            R.drawable.emoji1,
            R.drawable.emoji2,
            R.drawable.emoji3,
            R.drawable.emoji4,
            R.drawable.emoji5
        )

        val adapter = ViewPagerAdapter(images)
        val mRelativeLayout = findViewById<RelativeLayout>(R.id.layoutR)
        val skip = findViewById<TextView>(R.id.skipTxt)

        skip.setOnClickListener {
            this.finish()
        }

        val send = findViewById<Button>(R.id.sendRates)
        send.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.instagram.android")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=")
                    )
                )
            }
        }

//        viewP.adapter = adapter
        val dotsIndicator = findViewById<SpringDotsIndicator>(R.id.dotIndicator)

        dotsIndicator.attachTo(viewP1)
        viewP1.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {


                mRelativeLayout.setBackgroundColor(colors[position])
            }
        })
        Log.d("NEWy71", viewP1.currentItem.toString())
        Log.d("NEWy71", viewP1.scrollState.toString())
    }

//    fun View.colorTransition(@ColorRes startColor: Int, @ColorRes endColor: Int, duration: Long = 250L){
//        val colorFrom = ContextCompat.getColor(context, startColor)
//        val colorTo =  ContextCompat.getColor(context, endColor)
//        val colorAnimation: ValueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
//        colorAnimation.duration = duration
//
//        colorAnimation.addUpdateListener {
//            if (it.animatedValue is Int) {
//                val color=it.animatedValue as Int
//                setBackgroundColor(color)
//            }
//        }
//        colorAnimation.start()
//    }
}