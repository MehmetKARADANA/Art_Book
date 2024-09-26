package com.mehmetkaradana.artbookkotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.mehmetkaradana.artbookkotlin.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var artlist : ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter
    private lateinit var database : SQLiteDatabase


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        val toolbar: Toolbar = binding.toolbar
         setSupportActionBar(toolbar)

        setContentView(view)

        artlist= java.util.ArrayList<Art>()
       artAdapter = ArtAdapter(artlist)
       binding.recyclerView1.layoutManager=LinearLayoutManager(this)
       binding.recyclerView1.adapter=artAdapter

        try {
            database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")


            val cursor = database.rawQuery("SELECT * FROM arts",null)
            val artNameIx = cursor.getColumnIndex("artname")
            val idIx = cursor.getColumnIndex("id")

            while (cursor.moveToNext()){
                val name=cursor.getString(artNameIx)
                val id = cursor.getInt(idIx)
                val art = Art(id,name)
                artlist.add(art)
            }
            artAdapter.notifyDataSetChanged()
            cursor.close()

        }catch (e : IOException){
            e.printStackTrace()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==R.id.add_art_item){
            val intent = Intent(this@MainActivity,ArtActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}