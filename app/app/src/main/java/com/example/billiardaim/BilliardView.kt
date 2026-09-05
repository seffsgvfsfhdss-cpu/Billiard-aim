package com.example.billiardaim

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BilliardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val feltPaint = Paint().apply { color = Color.parseColor("#0B6623"); style = Paint.Style.FILL }
    private val railPaint = Paint().apply { color = Color.parseColor("#4A2E18"); style = Paint.Style.STROKE; strokeWidth = 30f }
    private val cueBallPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; antiAlias = true }
    private val targetBallPaint = Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL; antiAlias = true }
    private val ghostPaint = Paint().apply { color = Color.argb(100, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 3f; antiAlias = true }

    private val lineCuePaint = Paint().apply { color = Color.CYAN; strokeWidth = 5f; antiAlias = true }
    private val lineTargetPaint = Paint().apply { color = Color.MAGENTA; strokeWidth = 5f; antiAlias = true; pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    private val pocketPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; antiAlias = true }

    private var tableLeft = 50f
    private var tableTop = 50f
    private var tableRight = 1000f
    private var tableBottom = 600f
    private val railWidth = 30f

    private val ballRadius = 25f
    private var cueBall = Vector2D(300f, 300f)
    private var targetBall = Vector2D(600f, 300f)

    private var pockets = listOf<Vector2D>()
    private var selectedPocketIndex = 0
    var maxCushionReflections = 1
    private var activeTouchBall: String? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val margin = 40f
        tableLeft = margin + railWidth
        tableTop = margin + railWidth
        tableRight = w - margin - railWidth
        tableBottom = tableTop + (tableRight - tableLeft) * 0.5f

        if (tableBottom > h - margin - railWidth) {
            tableBottom = h - margin - railWidth
            tableRight = tableLeft + (tableBottom - tableTop) * 2.0f
        }

        pockets = listOf(
            Vector2D(tableLeft, tableTop),
            Vector2D((tableLeft + tableRight) / 2f, tableTop - 10f),
            Vector2D(tableRight, tableTop),
            Vector2D(tableLeft, tableBottom),
            Vector2D((tableLeft + tableRight) / 2f, tableBottom + 10f),
            Vector2D(tableRight, tableBottom)
        )

        cueBall = Vector2D(tableLeft + (tableRight - tableLeft) * 0.25f, (tableTop + tableBottom) / 2f)
        targetBall = Vector2D(tableLeft + (tableRight - tableLeft) * 0.75f, (tableTop + tableBottom) / 2f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touch = Vector2D(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (touch.distance(cueBall) < ballRadius * 2) {
                    activeTouchBall = "cue"
                } else if (touch.distance(targetBall) < ballRadius * 2) {
                    activeTouchBall = "target"
                } else {
                    var minDist = Float.MAX_VALUE
                    pockets.forEachIndexed { index, pocket ->
                        val dist = touch.distance(pocket)
                        if (dist < minDist) {
                            minDist = dist
                            selectedPocketIndex = index
                        }
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val clampedX = touch.x.coerceIn(tableLeft + ballRadius, tableRight - ballRadius)
                val clampedY = touch.y.coerceIn(tableTop + ballRadius, tableBottom - ballRadius)

                if (activeTouchBall == "cue") {
                    cueBall.x = clampedX
                    cueBall.y = clampedY
                } else if (activeTouchBall == "target") {
                    targetBall.x = clampedX
                    targetBall.y = clampedY
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeTouchBall = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(tableLeft - railWidth, tableTop - railWidth, tableRight + railWidth, tableBottom + railWidth, railPaint)
        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, feltPaint)

        pockets.forEachIndexed { index, p ->
            val pRadius = if (index == selectedPocketIndex) 28f else 22f
            canvas.drawCircle(p.x, p.y, pRadius, pocketPaint)
        }

        drawAimingLines(canvas)

        canvas.drawCircle(cueBall.x, cueBall.y, ballRadius, cueBallPaint)
        canvas.drawCircle(targetBall.x, targetBall.y, ballRadius, targetBallPaint)
    }

    private fun drawAimingLines(canvas: Canvas) {
        val targetPocket = pockets[selectedPocketIndex]
        val dirToPocket = targetPocket.sub(targetBall).normalize()
        val targetPocketEnd = targetBall.add(dirToPocket.scale(targetBall.distance(targetPocket)))
        canvas.drawLine(targetBall.x, targetBall.y, targetPocketEnd.x, targetPocketEnd.y, lineTargetPaint)

        val ghostPos = targetBall.sub(dirToPocket.scale(ballRadius * 2f))
        canvas.drawCircle(ghostPos.x, ghostPos.y, ballRadius, ghostPaint)

        drawReflectedPath(canvas, cueBall, ghostPos, maxCushionReflections)
    }

    private fun drawReflectedPath(canvas: Canvas, start: Vector2D, end: Vector2D, reflectionsLeft: Int) {
        val direction = end.sub(start).normalize()
        val totalDist = start.distance(end)

        var nearestHit = end
        var hitCushion = -1

        val minX = tableLeft + ballRadius
        val maxX = tableRight - ballRadius
        val minY = tableTop + ballRadius
        val maxY = tableBottom - ballRadius

        if (direction.x != 0f) {
            val tLeft = (minX - start.x) / direction.x
            if (tLeft > 0.001f && tLeft < totalDist) {
                val hitY = start.y + direction.y * tLeft
                if (hitY in minY..maxY) {
                    nearestHit = Vector2D(minX, hitY)
                    hitCushion = 0
                }
            }
            val tRight = (maxX - start.x) / direction.x
            if (tRight > 0.001f && tRight < totalDist) {
                val hitY = start.y + direction.y * tRight
                if (hitY in minY..maxY && start.distance(Vector2D(maxX, hitY)) < start.distance(nearestHit)) {
                    nearestHit = Vector2D(maxX, hitY)
                    hitCushion = 1
                }
            }
        }
        if (direction.y != 0f) {
            val tTop = (minY - start.y) / direction.y
            if (tTop > 0.001f && tTop < totalDist && start.distance(start.add(direction.scale(tTop))) < start.distance(nearestHit)) {
                val hitX = start.x + direction.x * tTop
                if (hitX in minX..maxX) {
                    nearestHit = Vector2D(hitX, minY)
                    hitCushion = 2
                }
            }
            val tBottom = (maxY - start.y) / direction.y
            if (tBottom > 0.001f && tBottom < totalDist && start.distance(start.add(direction.scale(tBottom))) < start.distance(nearestHit)) {
                val hitX = start.x + direction.x * tBottom
                if (hitX in minX..maxX) {
                    nearestHit = Vector2D(hitX, maxY)
                    hitCushion = 3
                }
            }
        }

        if (hitCushion != -1 && reflectionsLeft > 0) {
            canvas.drawLine(start.x, start.y, nearestHit.x, nearestHit.y, lineCuePaint)
            val reflectedDir = when (hitCushion) {
                0, 1 -> Vector2D(-direction.x, direction.y)
                else -> Vector2D(direction.x, -direction.y)
            }
            val nextEnd = nearestHit.add(reflectedDir.scale(nearestHit.distance(end)))
            drawReflectedPath(canvas, nearestHit, nextEnd, reflectionsLeft - 1)
        } else {
            canvas.drawLine(start.x, start.y, end.x, end.y, lineCuePaint)
        }
    }
}
