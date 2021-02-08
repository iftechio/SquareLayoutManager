package com.sinyu.android.square

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.sinyu.android.library.square.SquareLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_holder_debug_rv_square.view.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private var squareLayoutManager: SquareLayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_holder_debug_rv_square, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                holder.itemView.setBackgroundColor(
                    Color.argb(
                        255,
                        Random.nextInt(255),
                        Random.nextInt(255),
                        Random.nextInt(255)
                    )
                )
                holder.itemView.tvContent.text = position.toString()
                holder.itemView.setOnClickListener {
                    rvSquare.smoothScrollToPosition(position)
                }
            }

            override fun getItemCount(): Int {
                return 400
            }
        }

        rvSquare.apply {
            this.adapter = adapter
            rvSquare.layoutManager = SquareLayoutManager(20).apply {
                squareLayoutManager = this
                setOnItemSelectedListener { postion ->
                    Toast.makeText(context, "当前选中：$postion", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCenter.setOnClickListener {
            squareLayoutManager?.smoothScrollToCenter()
        }
    }
}