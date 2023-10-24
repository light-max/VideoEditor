package com.lifengqiang.videoeditor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.lifengqiang.videoeditor.ui.mediatrackeditor.MediaTrackEditorActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val start = findViewById<Button>(R.id.start)
        start.setOnClickListener {
            startActivity(Intent(this, MediaTrackEditorActivity::class.java))
        }
    }
}