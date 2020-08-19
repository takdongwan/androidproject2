package com.example.takstagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.takstagram.R
import com.example.takstagram.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){
    var firestore : FirebaseFirestore? = null

    override fun onCreateView(nflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view =LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        firestore = FirebaseFirestore.getInstance()

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
        }

    }




}

