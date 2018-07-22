package com.discobandit.app.jabberwalk.Utils

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.media.MediaDescriptionCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils.replace
import android.util.Log
import android.view.View
import com.discobandit.app.jabberwalk.BuildConfig
import com.discobandit.app.jabberwalk.UI.MediaPlayerFragment
import com.discobandit.app.jabberwalk.UI.SelectionGridFragment

/**
 * Created by Kaine on 1/1/2018.
 */
inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commitAllowingStateLoss()
}

fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int) {
    if(fragment is MediaPlayerFragment) supportFragmentManager.inTransaction{replace(frameId, fragment).addToBackStack(null)}
    else supportFragmentManager.inTransaction{replace(frameId, fragment)}
}
fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction{add(frameId, fragment)}
}
fun AppCompatActivity.removeFragment(fragment: Fragment) {
    supportFragmentManager.inTransaction{remove(fragment)}
}
//Interface for fragment/activity interaction
interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    fun onBookSelected(mediaID: String, playOnly: Boolean = false, position: Int)
    fun onSeekbarChanged(progress: Int)
    fun obButtonClicked(view: View)
    fun onSpeedChanged(speed: Int)
    fun onRootOptionSelected(view: View)
}
interface RecyclerFace{
    fun notifyChange(position: Int, payload: MutableList<Any>)
}

inline fun log(lambda: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.d("TAG", lambda())
    }
}

fun millisToDate(millis: Long): String{
    val seconds = millis/1000%60
    val minutes = millis/1000/60%60
    val hours = millis/1000/60/60
    val secondString = if(seconds < 10) "0" + seconds else seconds.toString()
    val minuteString = if(minutes < 10) "0" + minutes else minutes.toString()
    val hourString = if(hours < 10) "0" + hours else hours.toString()
    return "$hourString:$minuteString:$secondString"
}

fun getPositionPair(tracks: List<MediaDescriptionCompat>, position: Long): Pair<Int, Long> {
    var timeLeft = position
    for((index, it) in tracks.withIndex()){
        val duration = it.extras!!.getLong("duration")
        if(timeLeft <= duration){
            return Pair(index, timeLeft)
        }
        timeLeft-=duration
    }
    return Pair(0,0)
}

fun getPositionMS(tracks: List<MediaDescriptionCompat>, position: Long, index: Int): Long{
    return position + tracks.subList(0,index).sumBy { it.extras!!.getLong("duration").toInt() }
}