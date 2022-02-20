package com.example.yemektariflerisqlite

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_tarif.*
import java.io.ByteArrayOutputStream


class TarifFragment : Fragment() {

    var secilengorsel : Uri? = null
    var secilenBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tarif, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setOnClickListener {
            kaydet(it)
        }

        imageView.setOnClickListener {
            gorselsec(it)
        }

        arguments?.let {

            val gelenBilgi = TarifFragmentArgs.fromBundle(it).bilgi

            if (gelenBilgi.equals("menudengeldim")){
                //yeni bir yemek eklemeye geldi
                yemekIsmiText.setText("")
                yemekMalzemeText.setText("")
                button.visibility = View.VISIBLE

                val gorselSecmeArkaPlani = BitmapFactory.decodeResource(context?.resources , R.drawable.gorselsecimi)
                imageView.setImageBitmap(gorselSecmeArkaPlani)


            }else{
                //daha önce oluşturulan yemeği görmeye geldi
                button.visibility = View.INVISIBLE

                val secilenId = TarifFragmentArgs.fromBundle(it).id

                context?.let {

                    try {

                        val db = it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE,null)
                        val cursor = db.rawQuery("SELECT * FROM yemekler WHERE id = ?", arrayOf(secilenId.toString()))

                        val yemekIsmiIndex = cursor.getColumnIndex("yemekismi")
                        val yemekMalzemeIndex = cursor.getColumnIndex("yemekmalzemesi")
                        val yemekGorseli = cursor.getColumnIndex("gorsel")

                        while (cursor.moveToNext()){

                            yemekIsmiText.setText(cursor.getString(yemekIsmiIndex))
                            yemekMalzemeText.setText(cursor.getString(yemekMalzemeIndex))

                            val byteDizisi = cursor.getBlob(yemekGorseli)
                            val bitmap = BitmapFactory.decodeByteArray(byteDizisi, 0 , byteDizisi.size)
                            imageView.setImageBitmap(bitmap)

                        }
                        cursor.close()


                    }catch (e : Exception){
                        e.printStackTrace()
                    }

                }


            }


        }

    }

    fun kaydet(view: View){
        //SQLite'a kaydetme
        val yemekIsmi = yemekIsmiText.text.toString()
        val yemekMalzemeleri = yemekMalzemeText.text.toString()

        if( secilenBitmap != null ){

            val kucukBitmap =kucukBitmapOlustur(secilenBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50 , outputStream)
            val byteDizisi = outputStream.toByteArray()

            try {

                context?.let {

                    val database = it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE , null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler ( id INTEGER PRIMARY KEY, yemekismi VARCHAR, yemekmalzemesi VARCHAR, gorsel BLOB )")

                    val sqlString = "INSERT INTO yemekler (yemekismi, yemekmalzemesi, gorsel) VALUES(?, ?, ?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1,yemekIsmi)
                    statement.bindString(2,yemekMalzemeleri)
                    statement.bindBlob(3, byteDizisi)
                    statement.execute()


                }



            }catch (e : Exception){
                e.printStackTrace()
            }

            val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)

        }

    }

    fun gorselsec(view: View){

        activity?.let{

            if (ContextCompat.checkSelfPermission(it.applicationContext , Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){//İZİN KONTROLU
                //izin verilmedi izin istememiz gerekiyor
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)


            }else{
                //izin zaten verildi tekrar istemeden galeriye git
                val galeriIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent, 2)


            }

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1){


            if ( grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //izni aldık
                val galeriIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent, 2)
            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null){

            secilengorsel = data.data

            try {

                context?.let {

                    if (secilengorsel != null){

                        if ( Build.VERSION.SDK_INT >= 28){
                            //SDK 28 ve üstü olan cihazlarda çalıştırmak için
                            val source = ImageDecoder.createSource(it.contentResolver, secilengorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            imageView.setImageBitmap(secilenBitmap)

                        }else{
                            //SDK 28  altı olan cihazlarda çalıştırmak için
                            secilenBitmap = MediaStore.Images.Media.getBitmap(it.contentResolver, secilengorsel)
                            imageView.setImageBitmap(secilenBitmap)

                        }

                    }
                }



            }
            catch (e: Exception){
                e.printStackTrace()
            }

        }


        super.onActivityResult(requestCode, resultCode, data)
    }

    fun kucukBitmapOlustur (kullanicininSecdigiBitmap : Bitmap , maximumBoyut : Int) : Bitmap{

        var widht = kullanicininSecdigiBitmap.width
        var height = kullanicininSecdigiBitmap.height

        val bitmapOrani : Double = widht.toDouble() / height.toDouble()

        if (bitmapOrani > 1 ){
            //GORSEL YATAY
            widht = maximumBoyut
            val kisaltilmisHeight = widht / bitmapOrani
            height = kisaltilmisHeight.toInt()

        }else{
            //GORSEL DİKEY
            height = maximumBoyut
            val kisaltilmisWidht = height * bitmapOrani
            widht = kisaltilmisWidht.toInt()

        }


        return Bitmap.createScaledBitmap(kullanicininSecdigiBitmap ,widht  , height  , true)

    }



}


