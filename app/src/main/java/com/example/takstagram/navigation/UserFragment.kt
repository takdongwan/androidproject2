package com.example.takstagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.takstagram.LoginActivity
import com.example.takstagram.MainActivity
import com.example.takstagram.R
import com.example.takstagram.navigation.model.AlarmDTO
import com.example.takstagram.navigation.model.ContentDTO
import com.example.takstagram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.fragment_user.view.account_iv_profile

class UserFragment : Fragment(){

    var fragmentView : View? = null
    var firestore: FirebaseFirestore? = null
    var uid : String? = null
    var auth :FirebaseAuth? =null
    var currentUserUid : String? = null//내정보와 상대정보등을 확인할 수 있다.
    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10  //PICK_PROFILE_FROM_ALBUM 을 compain object 로 선언 해줌, static 으로 선언해 준다 라고 보면 됨.
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView =LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
      //  var fragmentView =LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid
        if(uid ==currentUserUid){
            //나의 페이지
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //다른 사람 페이지
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity?.toolbar_btn_back.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId= R.id.action_home
            }
            mainactivity?.toolbar_title_image?.visibility =View.GONE
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility = View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }

        }
        fragmentView?.account_recyclerview?.adapter= UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager =GridLayoutManager(activity!!,3)


        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent= Intent(Intent.ACTION_PICK)
            photoPickerIntent.type ="image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    fun getProfileImage(){//올린이미지를 다운 받은 함수.
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null ) return@addSnapshotListener
            if(documentSnapshot.data != null) {
                var url = documentSnapshot?.data!!["image"]
               Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }
    fun getFollowerAndFollowing() {//화면에 팔로잉 느는거 보여주는 fun
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener{ documentSnapshot,firebaseFirestoreException  ->
            if(documentSnapshot ==null)return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!)){
                    fragmentView?.account_btn_follow_signout?.text =getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!,R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid){
                        fragmentView?.account_btn_follow_signout?.text= getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter =null
                    }
                }
            }
        }
    }
    fun requestFollow(){
        //save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)//transection
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount =1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){
                //it remove following third person when a third person follow me
                followDTO?.followingCount =followDTO?.followingCount -1
                followDTO?.followers?.remove(uid)
            }else{
                //it add  following third person when a third person do not follow me
                followDTO?.followingCount = followDTO?.followingCount +1
                followDTO?.followers[uid!!]=true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        // save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)//상대방계정에접근
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO ==null){
                followDTO = FollowDTO()
                followDTO!!.followerCount =1
                followDTO!!.followers[currentUserUid!!]= true
                followerAlarm(uid!!) //최초에 누군가 팔로우를 할 경우 알람
                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            if (followDTO!!.followers.containsKey(currentUserUid)){
                //It cancle my follower when i follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount -1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount+1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)//db에 값 저장
            return@runTransaction
        }

    }
    fun followerAlarm(destinationUid : String) {
        var alarmDTO =AlarmDTO()
        alarmDTO.destinationUid =destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp =System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)


    }
   /* fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener{
            documentSnapshot,firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]

                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
               // Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
             // Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }*/

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs :ArrayList<ContentDTO> = arrayListOf()
                init{
                    firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener{
                        querySnapshot, firebaseFirestoreException ->
                        //sometimes , this code return null of querysnapshot  when it signout
                        if(querySnapshot == null) return@addSnapshotListener

                        //Get data
                        for(snapshot in querySnapshot.documents){
                            contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)//!! 두개로 null safety를 제거
                        }
                        fragmentView?.account_tv_post_count?.text=contentDTOs.size.toString()
                        notifyDataSetChanged()
                    }
                }
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3 // 화면 폭의 1/3 만큼 가져옴
            var imageview = ImageView(p0.context) // 이미지 뷰에 가져오면 정사각형형태로 가져옴
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return  contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var imageview = (p0 as CustomViewHolder).imageview
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }
    }
}
