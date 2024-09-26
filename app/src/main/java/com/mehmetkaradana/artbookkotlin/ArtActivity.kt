package com.mehmetkaradana.artbookkotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.mehmetkaradana.artbookkotlin.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream
import java.io.IOException

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher:ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedBitmap : Bitmap? =null
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding =ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)

        registerLauncher()

        val intent=intent

        val  info = intent.getStringExtra("info")
        if(info.equals("new")){
            binding.ArtName.setText("")
            binding.ArtistName.setText("")
            binding.ArtYear.setText("")
            binding.button.visibility = View.VISIBLE

            val selectedImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.selectimage)
            binding.imageView1.setImageBitmap(selectedImageBackground)
        }else {
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNmaeIx = cursor.getColumnIndex("artistname")
            val yearIx= cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.ArtName.setText(cursor.getString(artNameIx))
                binding.ArtistName.setText(cursor.getString(artistNmaeIx))
                binding.ArtYear.setText(cursor.getString(yearIx))

                //blob binary bir veri
                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView1.setImageBitmap(bitmap)
            }

            cursor.close()

        }


    }

    fun SelectImage(view: View){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                        View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                        View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }


    }

    fun makeSmallerBitmap(image: Bitmap,maximumSize: Int): Bitmap{
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if(bitmapRatio > 1) {
            //landscape
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        }else {
            //portrait
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }


    fun SaveButtonClicked(view: View){

        val artName = binding.ArtName.text.toString()
        val artistName = binding.ArtistName.text.toString()
        val year = binding.ArtYear.text.toString()

        if(selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")

                val sqlString = "INSERT INTO arts (artname,artistname,year,image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)

                statement.execute()

            }catch (e: IOException){
                e.printStackTrace()
            }

            val intent = Intent(this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }

private fun registerLauncher() {
    activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intentFromResult = result.data
            if (intentFromResult != null) {
                val imageData = intentFromResult.data
                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        val source = ImageDecoder.createSource(this@ArtActivity.contentResolver, imageData!!)
                        selectedBitmap = ImageDecoder.decodeBitmap(source)
                        binding.imageView1.setImageBitmap(selectedBitmap)
                    } else {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this@ArtActivity.contentResolver, imageData)
                        binding.imageView1.setImageBitmap(selectedBitmap)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (result) {
            //permission granted
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        } else {
            //permission denied
            Toast.makeText(this@ArtActivity, "Permisson needed!", Toast.LENGTH_LONG).show()
        }
    }
}
}

