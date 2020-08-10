package com.example.takstagram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    var auth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            signinAndSignup()
        }
    }
    //회원가입 코드
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            email_edittext.text.toString(),
            password_edittext.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //아이디가 생성되었을 때 입력될 코드
                moveMainPage(task.result?.user)
            } else if (task.exception?.message.isNullOrEmpty()) {
                //error 발생시
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            } else {
                //회원가입도 로그인도 아닐 경우 로그인으로 빠지게
                signinEmail()
            }
        }
    }
    fun signinEmail() {
        auth?.signInWithEmailAndPassword(
            email_edittext.text.toString(),
            password_edittext.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //아이디 v패스워드가 맞았을때
                moveMainPage(task.result?.user)
            } else {
                //틀렸을 경우
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {//파이어베이스 유저 상태를 넘겨줌
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}



