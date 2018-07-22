package com.discobandit.app.jabberwalk.UI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.view.*
import android.widget.SeekBar
import com.discobandit.app.jabberwalk.*
import com.discobandit.app.jabberwalk.R.id.play
import com.discobandit.app.jabberwalk.Utils.*
import kotlinx.android.synthetic.main.fragment_book_player2.*
import kotlinx.android.synthetic.main.fragment_book_player2.view.*
import java.io.File


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MediaPlayerFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MediaPlayerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MediaPlayerFragment : Fragment() {

    // TODO: Rename and change types of parameters
    var isPlaying = false
    private var mParam1: String? = null
    private var mParam2: String? = null
    private var mListener: OnFragmentInteractionListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }

        log{"The view is definitely created"}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_book_player2, container, false)
        val _duration = bookSelected?.description?.extras?.getLong("duration") ?: 0
        val timeStamp = bookSelected?.description?.extras?.getLong("time_stamp") ?: 0
        with(view) {
            book_title.text = bookSelected?.description?.title
            equalizer.size = FloatingActionButton.SIZE_MINI
            assignListener(play, equalizer,skip_back,
            skip_next,forward_30,back_30,lock,speed_button, listener = {onButtonPressed(it)})
            val file = File(context.filesDir, bookSelected!!.description!!.title!!.toString())
            if(file.exists())
                album_art.setImageBitmap(BitmapFactory.decodeFile(file.path))
            else if(bookSelected?.description?.iconUri != null)
                album_art.setImageBitmap(BitmapFactory.decodeFile(bookSelected?.description?.iconUri.toString()))
            duration.text = millisToDate(_duration)
            current_location.text = millisToDate(timeStamp)
            seek_bar.max = _duration.toInt()
            seek_bar.progress = timeStamp.toInt()
            bookTracker = BookTracker { _, _, new -> view.seek_bar.progress = new }
            bookTracker.currentPosition = timeStamp.toInt()
            if(isPlaying) play.setImageResource(R.drawable.ic_pause_white_48dp)
            seek_bar.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            view.current_location.text = millisToDate(progress.toLong())
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            bookTracker.currentPosition = seekBar!!.progress
                            mListener!!.onSeekbarChanged(seekBar!!.progress)

                        }

                    }
            )
            return this
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(view: View) {
        if (mListener != null) {
            mListener!!.obButtonClicked(view)
        }
    }
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onResume() {
        super.onResume()
        seek_bar.progress = bookTracker.currentPosition
    }
    override fun onDetach() {
        super.onDetach()
        mListener = null
    }
    fun assignListener(vararg views: View,listener: (View)->Unit){
        views.forEach{it.setOnClickListener{listener(it)}}
    }

    fun setPlayIcon() = play?.setImageResource(R.drawable.ic_play_arrow_white_48dp)
    fun setPauseIcon() = play?.setImageResource(R.drawable.ic_pause_white_48dp)

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MediaPlayerFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): MediaPlayerFragment {
            val fragment = MediaPlayerFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
