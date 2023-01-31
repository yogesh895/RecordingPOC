package com.example.msgshareapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_recycler_view.*

class RecycleViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TED1", "called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)
        Log.d("TED1", "clicked...")
        var todoList = mutableListOf(
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
            Todo("Dancing", false),
        )

        val adapter = TodoAdapter(todoList)
        listView.adapter = adapter
        listView.layoutManager = LinearLayoutManager(this)
        Log.d("TED1", "clicked... $adapter")
        val addBtns = findViewById<Button>(R.id.addBtn)

        addBtns.setOnClickListener {
//            val addTodoTitle = findViewById<Button>(R.id.addTodo)
            Log.d("TED1", "clickedfasd... $it")
            Log.d("TED1", "clicked...")
            val title = addTodo.text.toString()
            val todo = Todo(title, false)
            todoList.add(todo)
            adapter.notifyDataSetChanged()
            adapter.notifyItemInserted(todoList.size - 1)
        }
    }
}