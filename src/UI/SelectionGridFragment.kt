package com.discobandit.app.jabberwalk.UI

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.discobandit.app.jabberwalk.Utils.OnFragmentInteractionListener
import com.discobandit.app.jabberwalk.R
import com.discobandit.app.jabberwalk.Utils.RecyclerFace
import com.discobandit.app.jabberwalk.Utils.log
import kotlinx.android.synthetic.main.book_card.view.*
import kotlinx.android.synthetic.main.fragment_book_select_grid.view.*
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SelectionGridFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SelectionGridFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class SelectionGridFragment : Fragment(), RecyclerFace {
    override fun notifyChange(position: Int, payload: MutableList<Any>) {
        val list = mutableListOf<Any>()
        list.add(true)
        //view?.recyclerView?.adapter?.notifyItemChanged(position, list)
        view?.recyclerView?.adapter?.notifyDataSetChanged()
    }

    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private var mListener: OnFragmentInteractionListener? = null
    var bookList = mutableListOf<MediaBrowserCompat.MediaItem>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments?.getString(ARG_PARAM1)
            mParam2 = arguments?.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_book_select_grid, container, false)
        view.recyclerView.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.columns))
        view.recyclerView.adapter = SelectionGridAdapter(bookList) {
            book, bool, position -> onButtonPressed(book, bool, position)
        }
        return view
    }


    // TODO: Rename method, update argument and hook method into UI event
    private fun onButtonPressed(book: MediaBrowserCompat.MediaItem, playOnly: Boolean = false, position: Int) {
        if (mListener != null) {
            mListener!!.onBookSelected(book.mediaId!!, playOnly, position)
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

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

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
         * @return A new instance of fragment SelectionGridFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): SelectionGridFragment {
            val fragment = SelectionGridFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
