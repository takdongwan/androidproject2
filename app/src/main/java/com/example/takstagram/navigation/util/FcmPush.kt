package com.example.takstagram.navigation.util

import com.example.takstagram.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class FcmPush {
    var JSON = MediaType.parse("application/json; charset=utf=8")
    var url = "https://fcm.googleapis.com/fcm/send" //푸시를 보낼 값을 firebase에서가져옴
    var serverKey ="AAAAEOE96EE:APA91bFnumNDqIEk0hMqMBY4ktMbiK_qOTThhSFPh4FweLs-i6oIt4mt8A0pfBKxb4jhwFYXuJoZg-I7t6VtsGWBw0J-hGEoG3QbI3WPfj5eRXsnEIrb-rAECPj9pMdeF6VPdkhMOIc-"
    var gson : Gson? = null
    var okhttpClient :OkHttpClient? =null

    companion object {//싱글톤 패턴 선언
        var instance = FcmPush()
    }

    init {
         gson = Gson()
        okhttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid: String, title : String , message  :String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var token = task?.result?.get("pushToken").toString()
                var pushDTO  = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title =title
                pushDTO.notification.body =message

                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type","application/json")
                    .addHeader("Authorization","key="+serverKey)
                        .url(url)
                        .post(body)
                        .build()

                okhttpClient?.newCall(request)?.enqueue(object: Callback{
                    override fun onFailure(call: Call?, e: IOException?) {

                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }
                })
            }
        }
    }

}

//푸시를 전송해주는 class ,안드로이드 폰 안에 서버를 개발