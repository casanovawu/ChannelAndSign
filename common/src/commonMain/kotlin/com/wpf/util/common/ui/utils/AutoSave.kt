package com.wpf.util.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty

class AutoSave<T : @Serializable Any>(
    private val key: String,
    private var data: T
) {

    var value: T = data
        get() = data
        set(value) {
            field = value
            data = value
            settings.putString(key, gson.toJson(data))
        }

    init {
        val json = settings.getString(key, "")
        val t = gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
        t?.let {
            data = it
        }
    }
}

inline fun <reified T : @Serializable Any> autoSave(
    key: String, crossinline calculation: () -> T
) = AutoSave(key, calculation())

internal inline operator fun <reified T : @Serializable Any> AutoSave<T>.getValue(
    thisObj: Any?, property: KProperty<*>
) = value

internal inline operator fun <reified T : Any> AutoSave<T>.setValue(
    thisObj: Any?, property: KProperty<*>, value: T
) {
    this.value = value
}

class AutoSaveState<T : @Serializable Any, H : MutableState<T>>(
    private var key: String, value: H) {
    private var data: H = value

    var value = data

    var stateValue: T = data.value
        set(value) {
            this.data.value = value
            settings.putString(key, gson.toJson(value))
        }

    init {
        val json = settings.getString(key, "")
        val t = gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
        t?.let {
            data.value = it
        }
    }
}

@Composable
inline fun <reified T : @Serializable Any, H : MutableState<T>> autoSaveComposable(
    key: String, crossinline calculation: @Composable () -> H
) = AutoSaveState(key, calculation())

internal inline operator fun <reified T : @Serializable Any, H : MutableState<T>> AutoSaveState<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = value

internal inline operator fun <reified T : @Serializable Any, H : MutableState<T>> AutoSaveState<T, H>.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: T
) {
    stateValue = value
}

class AutoSaveList<T :@Serializable Any, H : SnapshotStateList<T>>(
    private val key: String, var value: H
) {

    val size: Int = value.size

    fun remove(t: T) {
        value.remove(t)
        saveData()
    }

    fun add(t: T) {
        value.add(t)
        saveData()
    }

    private fun saveData() {
        settings.putString(key, gson.toJson(value))
    }

    init {
        val json = settings.getString(key, "")
        val t = gson.fromJson(json, value.javaClass)
        t?.let {
            value = t
        }
    }
}

internal inline operator fun <reified T :@Serializable Any, H : SnapshotStateList<T>> AutoSaveList<T, H>.getValue(
    thisObj: Any?, property: KProperty<*>
) = value

internal inline operator fun <reified T :@Serializable Any, H : SnapshotStateList<T>> AutoSaveList<T, H>.setValue(
    thisObj: Any?, property: KProperty<*>, value: H
) {
    this.value = value
}

inline fun <reified T :@Serializable Any, H : SnapshotStateList<T>> autoSaveList(
    key: String, crossinline calculation: () -> H
) = AutoSaveList(key, calculation())

@Composable
inline fun <reified T :@Serializable Any, H : SnapshotStateList<T>> autoSaveListComposable(
    key: String, crossinline calculation: @Composable () -> H
) = AutoSaveList(key, calculation())


