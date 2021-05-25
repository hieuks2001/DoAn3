package com.vdhieu.doan

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.SeekBar
import com.vdhieu.doan.room.MainActivity.Companion.myPaintView
import com.vdhieu.doan.room.MainActivity.Companion.seekProgress
import kotlinx.android.synthetic.main.increase_stroke.*

class DialogStroke
    (var c: Activity) : Dialog(c), View.OnClickListener {
    var d: Dialog? = null
    var progressDetected: Int = 10
    val transparent = Color.TRANSPARENT
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.increase_stroke)
        set_textview.setOnClickListener(this)
        cancel_textview.setOnClickListener(this)

        stroke_seekbar.progress = seekProgress
        stroke_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressDetected = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.set_textview -> {
                seekProgress = progressDetected
                myPaintView?.setStrokeWidth(seekProgress)
                Log.d("Main Activity","Stroke : ${seekProgress}")

            }
            R.id.cancel_textview -> {
                dismiss()
            }
            else -> {
                dismiss()
            }
        }
        dismiss()
    }

}