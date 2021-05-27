package com.vdhieu.doan.play

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.firebase.database.*
import java.util.*

private const val STROKE_WIDTH = 12f // has to be float

class PaintView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    var width: Int? = 150
    var height: Int? = 150
    var canvas: Canvas? = null
    private lateinit var extraBitmap: Bitmap

    private var currentX = 0f
    private var currentY = 0f
    var allowDraw = false
    private var scale = 1f
    var ref: DatabaseReference? = null
    private var currentDrawSegment: DrawSegment? = null
    var pointsCount = 0
    lateinit var roomCodePaintView: String
    var chosenColor = Color.BLACK


    var chosenWidth = 10f

    private lateinit var frame: Rect

    // Set up the paint with which to draw.
    val paint: Paint = Paint().apply {
        color = Color.BLACK
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 10f // default: Hairline-width (really thin)
    }
    private val path: Path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scale = Math.min(1.0f * w / width!!, 1.0f * h / height!!)
        width = w
        height = h
        extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(extraBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {

        // Draw the bitmap that has the saved path.
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
        // Draw a frame around the canvas.
        canvas.drawPath(path, paint)
    }

    inner class TouchEvent : AsyncTask<String, String, String>(), View.OnTouchListener {
        override fun doInBackground(vararg params: String?): String {
            return "kjd"
        }


        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (allowDraw) {
                val x = event?.x
                val y = event?.y
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (x != null && y != null) {
                            touchStart(x, y)
                        }
                        invalidate()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (x != null && y != null) {
                            touchMove(x, y)
                        }
                        invalidate()
                    }
                    MotionEvent.ACTION_UP -> {
                        touchUp()
                        invalidate()
                    }
                }
            }
            return true
        }

    }

    private fun touchStart(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        currentX = x
        currentY = y

        currentDrawSegment = DrawSegment()
        currentDrawSegment!!.addPoint(currentX.toInt(), currentY.toInt())
    }

    private fun touchUp() {
        pointsCount = 0
        path.lineTo(currentX, currentY)
        canvas!!.drawPath(path, paint)
        path.reset()
        saveDrawinf().execute(currentDrawSegment)
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - currentX)
        val dy = Math.abs(y - currentY)
        if (dx >= 0.0 || dy >= 0.0) {
            ++pointsCount
            path.quadTo(currentX, currentY, (x + currentX) / 2, (y + currentY) / 2)
            currentX = x
            currentY = y
            currentDrawSegment!!.addPoint(currentX.toInt(), currentY.toInt())
            if (pointsCount == 100) {
                pointsCount = 0
                path.lineTo(currentX, currentY)
                canvas!!.drawPath(path, paint)
                saveDrawinf().execute(currentDrawSegment)
            }
        }
    }

    fun clearDrawing() {
        isDrawingCacheEnabled = false
        width?.let { height?.let { it1 -> onSizeChanged(it, it1, width!!, height!!) } }
        invalidate()
        isDrawingCacheEnabled = true
    }
    fun changeStrokeColor(sColor: Int) {
        paint!!.color = sColor
        chosenColor = sColor
    }
    fun clearDrawingForAll() {
        ref!!.removeValue()
        paint.color = chosenColor
        paint.strokeWidth = chosenWidth
    }

    fun eraser() {
        paint.color = Color.WHITE
    }

    fun setStrokeWidth(progress: Int) {
        var strokeWidth: Float = (progress).toFloat()
        paint.strokeWidth = strokeWidth
        chosenWidth = strokeWidth
    }

    fun addDatabaseListeners() {
        ref!!.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val segment = dataSnapshot.getValue(DrawSegment::class.java)
                val sColor = segment?.color
                val sWidth = segment?.strokeWidth
                if (sColor != null) {
                    paint.color = sColor
                }
                paint.strokeWidth = sWidth ?: return
                Log.d("MainActivity", "Màu : ${segment.points[0]}")
                val task = ShowDrawing()
                task.execute(segment)

                Log.d("Fetch", "Drawing fetching")

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                val segment = dataSnapshot.getValue(DrawSegment::class.java)
                val sColor = segment?.color
                val sWidth = segment?.strokeWidth
                if (sColor != null) {
                    paint.color = sColor
                }
                paint.strokeWidth = sWidth ?: return

                val task = ShowDrawing()
                task.execute(segment)

                Log.d("Fetch", "Drawing fetching")

            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                clearDrawing()
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    inner class ShowDrawing : AsyncTask<DrawSegment, String, Path>() {
        override fun doInBackground(vararg params: DrawSegment?): Path {
            val path = getPathForPoints(params[0]!!.points, scale)
            Log.d("AsyncTask doinbg", "doInBackground:")

            return path
        }

        @SuppressLint("LongLogTag")
        override fun onPostExecute(result: Path?) {
            super.onPostExecute(result)
            Log.d("AsyncTask onpost execute", "onPostExecute: ")
            if (result != null) {
                canvas!!.drawPath(result, paint)
            }
            invalidate()
        }

    }

    companion object {
        fun getPathForPoints(points: List<Point>, scale: Float): Path {
            val path = Path()
            var current = points[0]
            path.moveTo(
                (scale * current!!.x),
                (scale * current.y)
            )
            var next: Point? = null
            for (i in 1 until points.size) {
                next = points[i]
                path.quadTo(
                    Math.round(scale * current!!.x).toFloat(),
                    Math.round(scale * current.y).toFloat(),
                    Math.round(scale * (next!!.x + current.x) / 2).toFloat(),
                    Math.round(scale * (next.y + current.y) / 2)
                        .toFloat()
                )
                current = next
            }
            if (next != null) {
                path.lineTo(
                    Math.round(scale * next.x).toFloat(),
                    Math.round(scale * next.y).toFloat()
                )
            }
            return path
        }
    }

    inner class saveDrawinf : AsyncTask<DrawSegment, Int, DrawSegment>() {
        override fun doInBackground(vararg params: DrawSegment?): DrawSegment {
            val segment = DrawSegment()
            for (point in params[0]!!.points) {
                segment.addPoint((point.x), (point.y))
            }
            return segment
        }

        override fun onPostExecute(result: DrawSegment?) {
            super.onPostExecute(result)
            if (result != null) {
                result.addColor(paint.color)
                result.addStrokeWidth(paint.strokeWidth)
                val drawId = UUID.randomUUID().toString().substring(0, 15)

                val db = FirebaseDatabase.getInstance()

                val keyRef = db.getReference("games/$roomCodePaintView/drawing/$drawId")
                Log.d("MainActivity", "Saving segment to firebase ${keyRef}")
                keyRef
                    .setValue(result)
                keyRef.child("$drawId").removeValue()

            }
        }
    }
}