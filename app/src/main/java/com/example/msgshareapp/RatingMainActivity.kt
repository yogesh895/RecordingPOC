package com.example.msgshareapp

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_rating_main.*

class RatingMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating_main)

        val colors = listOf(
            Color.parseColor("#ff2255"),
            Color.parseColor("#F17E48"),
            Color.parseColor("#8848F1")
        )

        val images = listOf(
            R.raw.rate12,
            R.raw.rate34,
            R.raw.rate5
        )

        val adapter = ViewPagerAdapter(images)
        val mRelativeLayout = findViewById<ConstraintLayout>(R.id.rateLayout)
        viewP.adapter = adapter

        viewP.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                mRelativeLayout.setBackgroundColor(colors[position])
            }
        })
    }
}