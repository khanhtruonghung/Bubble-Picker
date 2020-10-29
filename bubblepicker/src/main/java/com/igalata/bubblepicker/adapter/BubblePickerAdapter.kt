package com.igalata.bubblepicker.adapter

import com.igalata.bubblepicker.model.PickerItem

/**
 * Created by irinagalata on 5/22/17.
 */
interface BubblePickerAdapter {

    val totalCount: Int

    fun getItem(position: Int): PickerItem

}

abstract class BubbleAdapter() {
    abstract var totalItem: Int
    abstract fun getItem(position: Int): PickerItem
    abstract fun onClick(x: Float, y: Float, itemData: PickerItem)

    // Listener
    lateinit var listener: InternalListener
    interface InternalListener {
        fun scalingItem(position: Int, newRadius: Float)
        fun setNewItems(updateItems: ArrayList<PickerItem>, removeItems: ArrayList<PickerItem>, addItems: ArrayList<PickerItem>)
    }

    fun setNewItems(updateItems: ArrayList<PickerItem>, removeItems: ArrayList<PickerItem>, addItems: ArrayList<PickerItem>) {
        listener.setNewItems(updateItems, removeItems, addItems)
    }

    fun scalingItem(position:Int, newRadius: Float) {
        listener.scalingItem(position, newRadius)
    }
}