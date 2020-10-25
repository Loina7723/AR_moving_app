package com.movehome.armoving

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CardData (val img: String?, val card_name: String?, val card_volume: String?) :
    Parcelable {
}