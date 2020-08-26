package com.example.takstagram.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.takstagram.R
import com.example.takstagram.navigation.model.AlarmDTO
import com.example.takstagram.navigation.model.ContentDTO
import com.example.takstagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){
    var firestore : FirebaseFirestore? = null
    var uid : String? =null
    override fun onCreateView(nflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view =LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        firestore = FirebaseFirestore.getInstance()
         uid= FirebaseAuth.getInstance().currentUser?.uid
        view.detailviewfragment_recyclerview.adapter =DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager=LinearLayoutManager(activity)//화면을 세로로 배치하기위해 linear layout manger 사용
        return view
    }
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs :ArrayList<ContentDTO> = arrayListOf()
        var contentUidList :ArrayList<String> = arrayListOf()
        init{
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{
                querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                //쿼리 스냅샷 이 null일때 return 해줄수 있도록함
                if(querySnapshot==null)  return@addSnapshotListener
                for(snapshot in querySnapshot!!.documents  ){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
           var view =LayoutInflater.from(p0.context).inflate(R.layout.item_detail,p0,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)
        //리사이클러뷰를 사용하면 메모리를적게 사용하기위해 커스텀뷰홀더를 사용하다는 약속  문법은아님.
        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var viewholder=(p0 as CustomViewHolder).itemView//서버에서넘어오는 데이터를 mapping
            //user Id
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![p1].userId
            //image
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholder.detailviewitem_imageview_content)
            //explain of content
            viewholder.detailviewitem_explain_textview.text= contentDTOs!![p1].explain
            //likes
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes "+ contentDTOs!![p1].favoriteCount
            //profile Imgae
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholder.detailviewitem_profile_image)
        //좋아요 버튼에 이미지
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(p1)
            }
            //좋아요카운터와 색칠되거나 비어있는 이벤트
            if(contentDTOs!![p1].favorites.containsKey(uid)){
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            //this code is when the profile image is clicked
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[p1].uid)
                bundle.putString("userId",contentDTOs[p1].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            //말풍선을 클릭하게되면 comment activity가 뜨도록함 코드를 넣음
            viewholder.detailviewitem_comment_imageview.setOnClickListener {
                v ->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[p1])// 리스트에 포지션값을 넣어주게되면 선택한 이미지의 uid 의값이 담김
                intent.putExtra("destinationUid",contentDTOs[p1].uid)


                startActivity(intent)
            }
        }

        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction {
                transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                if(contentDTO!!.favorites.containsKey(uid)){
                    //버튼이 눌려있을 경우
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount -1
                    contentDTO.favorites.remove(uid)

                }else{
                    //눌려있지 않을 경우우
                    contentDTO?.favoriteCount= contentDTO?.favoriteCount+1
                    contentDTO?.favorites[uid!!]= true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc,contentDTO)
            }
        }
        fun favoriteAlarm(destinationUid : String){
            var alarmDTO  =AlarmDTO()
            alarmDTO.destinationUid =destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp =System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)



            //좋아요 알람을 눌렀을 때 fcm 메시지를 전송
            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid,"Takstagram",message)

        }
    }
}

