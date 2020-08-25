package com.example.takstagram.navigation.model

import com.google.api.Billing

data class AlarmDTO(
    var destinationUid :String? = null,
    var userId : String? =null,
    var uid : String? =null,
    var kind :Int? = null,
    var message : String? =null,
    var timestamp : Long? = null


    // 0 : Like alarm  , 1: comment alarm , 2 : follow alarm 을 나타네는 flag 값
)