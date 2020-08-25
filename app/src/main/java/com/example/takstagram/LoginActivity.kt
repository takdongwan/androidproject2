package com.example.takstagram
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.tv.TvContract.Programs.Genres.encode
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_login.*
import java.net.URLEncoder.encode
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    var auth: FirebaseAuth? = null
    var googleSignInClient:GoogleSignInClient?=null
    var GOOGLE_LOGIN_CODE= 9001   // 구글로그인할때 사용할 REQUEST CODE
    var callbackManager :CallbackManager?=null//페이스북 로그인 결과를 가져오는 callback method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            signinAndSignup()
        }
        google_sign_in_button.setOnClickListener {
            //첫번째 단계
             googleLogin()
        }
        facebook_login_button.setOnClickListener {
            //이부분도 성공시 첫번째 단계
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))//구글 api넣어주기 여기 game sign in 이빨갈경우 구글에서 api 못찾을 때라 직접 찾아와야함   //project- app - generate-google services- debug- values xml , 직접 " 코드들" 로 넣어줌
            .requestEmail()//이메일 아이디 받아오기
            .build()//빌드로 닫아주고
        googleSignInClient = GoogleSignIn.getClient(this,gso)
       // printHashKey()
        callbackManager = CallbackManager.Factory.create()
   }
    //SLIgr6XAGlrSYVdgelFqy6VrWnw=

    override fun onStart() {//자동로그인
        super.onStart()
        moveMainPage(auth?.currentUser)
    }
    fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(packageName,PackageManager.GET_SIGNATURES)
            for (signature in info.signatures){
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey =String(Base64.encode(md.digest(),0))
                Log.i("TAG","printHashKey() Hash Key: $hashKey")
                //logcat에 해쉬가 나오는데 그것을 facebook에 등록을 해줘야 페이스북에 로그인이 가능함.
            }
        }catch (e:NoSuchAlgorithmException){
            Log.e("TAG","printHashKey()",e)
        }catch (e:Exception){
            Log.e("TAG","printHashKey()",e)
        }
    }

    fun googleLogin(){
        var signIntent = googleSignInClient?.signInIntent
        startActivityForResult(signIntent,GOOGLE_LOGIN_CODE)

    }
    fun facebookLogin(){
        LoginManager.getInstance().logInWithReadPermissions(this,Arrays.asList("public_profile","email"))

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {

                handleFacebookAccessToken(result?.accessToken)
            }

            override fun onCancel() {

            }

            override fun onError(error: FacebookException?) {

            }
        })
    }
    fun handleFacebookAccessToken(token: AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //아이디 v패스워드가 맞았을때
                    moveMainPage(task.result?.user)
                } else {
                    //틀렸을 경우
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode,resultCode,data)
        if(requestCode == GOOGLE_LOGIN_CODE){
            //구글에서 넘겨주는 로그인 결과 값을 가져옴
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result?.isSuccess!!){
                //result 가 성공했을 시 firebase에 결과를 넘겨줌
                var account =result?.signInAccount
                //두번째 단계
                firebaseAuthWithGoogle(account)
            }
        }
    }
    fun firebaseAuthWithGoogle(account : GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken,null) //토큰아이디를 넘겨 줌
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //아이디 v패스워드가 맞았을때
                    moveMainPage(task.result?.user)
                } else {
                    //틀렸을 경우
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
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
            finish()
        }
    }
}



