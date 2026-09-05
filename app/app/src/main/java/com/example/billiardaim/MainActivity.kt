package com.example.billiardaim

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val title = android.widget.TextView(this).apply {
            text = "Billiard Aim Master"
            textSize = 22f
            setTextColor(android.graphics.Color.BLACK)
        }
        layout.addView(title)

        val billiardView = BilliardView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        layout.addView(billiardView)

        val btnReflect = Button(this).apply {
            text = "Переключить отскок (Борта)"
            setOnClickListener {
                billiardView.maxCushionReflections = if (billiardView.maxCushionReflections == 1) 2 else 1
                billiardView.invalidate()
            }
        }
        layout.addView(btnReflect)

        setContentView(layout)
    }
}
