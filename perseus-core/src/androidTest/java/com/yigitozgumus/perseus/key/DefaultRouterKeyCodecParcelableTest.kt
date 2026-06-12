package com.yigitozgumus.perseus.key

import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultNavigationKeyCodecParcelableTest {

    @Test
    fun parcelableNavigationKeyCanRoundTripWithoutSerializable() {
        val key = ParcelableOnlyKey(42)

        val encoded = DefaultNavigationKeyCodec.encode(key)
        val decoded = DefaultNavigationKeyCodec.decode(encoded)

        assertEquals(key, decoded)
    }
}

private data class ParcelableOnlyKey(val id: Int) : NavigationKey, Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ParcelableOnlyKey> {
        override fun createFromParcel(parcel: Parcel): ParcelableOnlyKey = ParcelableOnlyKey(parcel)
        override fun newArray(size: Int): Array<ParcelableOnlyKey?> = arrayOfNulls(size)
    }
}
