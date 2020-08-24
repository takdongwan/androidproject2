package com.example.takstagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.takstagram.R
import com.example.takstagram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM=0
    var storage : FirebaseStorage?= null
    var photoUri : Uri?=null
    var auth :FirebaseAuth? =null//유저정보가져올수있게하기위함
    var firestore : FirebaseFirestore?=null//데이터베이스사용
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)
        //Initiate
        storage =FirebaseStorage.getInstance()
        auth =FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        //open the album
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type="image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)
        //add Image upload event
        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

//선택한 이미지를 받는부분
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                //T사진을 선택했을 때 이미즤 경로가 넘어옴
                photoUri = data?.data
                addphoto_imgae.setImageURI(photoUri)
            }else{
                //취소를 눌럿을시 작동하는 부분
                finish()
            }
        }
    }

    fun contentUpload(){
        //파일내임을 만들어 주는 부분
        var timestemp = SimpleDateFormat("yyyMMdd_HHmmss").format(Date())
        var imageFileName ="Image_"+ timestemp + "_.png" //중복생성되지않는 파일명을 만드는 코드

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        //넘겨주는 방식 2 promise method
        storageRef?.putFile(photoUri!!)?.continueWithTask{
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO= ContentDTO()
            //Insert DownLoadUrl of image
            contentDTO.imageUrl =uri.toString()
            //Insert uid of user
            contentDTO.uid = auth?.currentUser?.uid
            //insert userid
            contentDTO.userId =auth?.currentUser?.email
            //insert explain of content
            contentDTO.explain = addphoto_edit_explain.text.toString()
            //timestamp
            contentDTO.timestamp =System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)
            setResult(Activity.RESULT_OK)
            finish()
        }

        //넘겨주는 방식 1Callback method
    /*    storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener {
                uri ->
                var contentDTO= ContentDTO()
                //Insert DownLoadUrl of image
                contentDTO.imageUrl =uri.toString()
                //Insert uid of user
                contentDTO.uid = auth?.currentUser?.uid
                //insert userid
                contentDTO.userId =auth?.currentUser?.email
                //insert explain of content
                contentDTO.explain = addphoto_edit_explain.text.toString()
                //timestamp
                contentDTO.timestamp =System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }*/
    }
}
