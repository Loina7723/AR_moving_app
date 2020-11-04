package com.movehome.armoving.model

import android.os.Parcelable
import com.movehome.armoving.model.CardData
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CardListData (val title: String, val data: ArrayList<CardData>) : Parcelable {
}