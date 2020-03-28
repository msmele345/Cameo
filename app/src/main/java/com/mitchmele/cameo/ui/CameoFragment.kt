package com.mitchmele.cameo.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.mitchmele.cameo.CameoViewModel
import com.mitchmele.cameo.PollWorker
import com.mitchmele.cameo.R
import com.mitchmele.cameo.model.GalleryItem
import com.mitchmele.cameo.model.ViewState
import com.mitchmele.cameo.network.PhotoFetcher
import com.mitchmele.cameo.util.CameoConstants.CAMEO_FRAG_TAG
import com.mitchmele.cameo.util.CameoConstants.POLL_WORK
import com.mitchmele.cameo.util.QueryPreferences
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.empty_view.*
import kotlinx.android.synthetic.main.error_view.*
import kotlinx.android.synthetic.main.fragment_cameo_gallery.*
import kotlinx.android.synthetic.main.progress_spinner.*

class CameoFragment : Fragment() {

    private lateinit var cameoRecyclerView: RecyclerView
    private lateinit var cameoViewModel: CameoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = true
        cameoViewModel = ViewModelProviders.of(this).get(CameoViewModel::class.java)
        getData()
    }

    private fun getData() {
        val flickrLiveData: LiveData<List<GalleryItem>> = PhotoFetcher().fetchPhotos()
        flickrLiveData.observe(
            this, Observer { galleryItems ->
                Log.d(CAMEO_FRAG_TAG, "Response: $galleryItems")
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cameo_gallery, container, false)

        cameoRecyclerView = view.findViewById(R.id.cameo_recycler_view) as RecyclerView
        cameoRecyclerView.layoutManager = GridLayoutManager(context, 3)
        return view
    }

    //viewLifeCycleOwner here ensures all views are ready (frag and view have different lifecycle)
    //Fragments can be detached when view is destroyed - this will handle liveData when re-attached
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservable()
    }

    private fun setupObservable() {
        setViewState(ViewState.Loading)
        cameoViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner,
            Observer { galleryItems -> //ensures that the liveData object will remove the observer when destroyed
                when {
                    galleryItems.isNotEmpty() -> {
                        setViewState(ViewState.Data)
                        Log.d(CAMEO_FRAG_TAG, "Received ${galleryItems.size} GalleryItems")
                        cameoRecyclerView.adapter = CameoAdapter(galleryItems)
                    }
                    galleryItems.isEmpty() -> {
                        setViewState(ViewState.Empty)
                    }
                    else -> {
                        setViewState(ViewState.Failure)
                    }
                }
            }
        )
    }

//        val bindDrawable: (Drawable) -> Unit = itemImageView::setImageDrawable

    private class CameoViewHolder(itemImageView: ImageView) :
        RecyclerView.ViewHolder(itemImageView) {
        val v = itemImageView
        fun bindDrawable(galleryItem: GalleryItem) {
            Picasso.get()
                .load(galleryItem.url)
                .placeholder(R.drawable.bill_up_close)
                .into(v)
        }
    }

    private inner class CameoAdapter(private val galleryItems: List<GalleryItem>) :
        RecyclerView.Adapter<CameoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameoViewHolder {
            val view = layoutInflater
                .inflate(
                    R.layout.list_item_gallery, parent, false
                ) as ImageView

            return CameoViewHolder(view)
        }

        override fun getItemCount(): Int {
            return galleryItems.size
        }

        override fun onBindViewHolder(holder: CameoViewHolder, position: Int) {
            val galleryItem = galleryItems[position]
            holder.bindDrawable(galleryItem)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater) //pg 544 for reference
        inflater.inflate(R.menu.fragment_came_gallery_menu, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView

        searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(queryText: String): Boolean {
                    Log.d(CAMEO_FRAG_TAG, "QueryTextSubmit $queryText")
                    cameoViewModel.fetchPhotos(queryText)

                    searchView.clearFocus() // hides soft keyboard after submission
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    Log.d(CAMEO_FRAG_TAG, "QueryTextChange $newText")
                    return false
                }
            })
            //pre-populates search field when users presses search icon
            setOnSearchClickListener {
                searchView.setQuery(cameoViewModel.searchTerm, false)
            }
        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = when {
            isPolling -> R.string.stop_polling
            else -> R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
                cameoViewModel.fetchPhotos("")
                true
            }
            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                when (isPolling) {
                    true -> {
                        WorkManager.getInstance().cancelUniqueWork(POLL_WORK)
                        QueryPreferences.setPolling(requireContext(), false)
                    }
                    else -> {
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .build()
                        val periodicRequest = PeriodicWorkRequest
                            .Builder(
                                PollWorker::class.java,
                                15,
                                java.util.concurrent.TimeUnit.MINUTES
                            )
                            .setConstraints(constraints)
                            .build()
                        WorkManager.getInstance().enqueueUniquePeriodicWork(
                            POLL_WORK,
                            ExistingPeriodicWorkPolicy.KEEP,
                            periodicRequest
                        )
                        QueryPreferences.setPolling(requireContext(), true)
                    }
                }
                activity?.invalidateOptionsMenu()
                return true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    //set types in setObservable
    //add Resource impl
    fun setViewState(viewState: ViewState) {
        progress_spinner.visibility = View.GONE
        empty_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        default_error_view.visibility = View.GONE
        cameo_recycler_view.visibility = View.GONE
        return when (viewState) {
            is ViewState.Data -> {
                cameo_recycler_view.visibility = View.VISIBLE
            }
            is ViewState.Failure -> {
                default_error_view.visibility = View.VISIBLE
            }
            is ViewState.Loading -> {
                progress_spinner.visibility = View.VISIBLE
            }
            is ViewState.Empty -> {
                empty_view.visibility = View.VISIBLE
            }
        }
    }

    /*  USER PUSHES BACK BUTTON: BACK BUTTON destroys the Activity and any fragments that it hosts
        onPause()
        onStop()
        onDestroy() ->
     */
    //The final call you receive before your activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = CameoFragment()
    }
}