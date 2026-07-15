package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.online.smart.liveplayer.tv.Adapter.Historychannel
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState


class HistoryFragment : Fragment() {
    private var rvHistory: RecyclerView? = null
    private var layoutNoData: LinearLayout? = null
    private var adapter: Historychannel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val back = view.findViewById<ImageView?>(R.id.back)
        back.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                requireActivity().onBackPressed()
            }
        })
        rvHistory = view.findViewById<RecyclerView?>(R.id.rvHistory)
        layoutNoData = view.findViewById<LinearLayout?>(R.id.layoutNoData)


        AdsManager.getAdLive("native_history_2005").observe(viewLifecycleOwner) { state ->
            if (state is NativeAdUiState.Success) {
                adapter?.notifyDataSetChanged()
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }


  private fun loadHistory() {

      val historyList = AppDatabase.getInstance(requireContext()).historyDao().allHistory

      if (historyList.isNullOrEmpty()) {
          rvHistory?.visibility = View.GONE
          layoutNoData?.visibility = View.VISIBLE
      } else {
          rvHistory?.visibility = View.VISIBLE
          layoutNoData?.visibility = View.GONE

          if (adapter == null) {
              adapter = Historychannel(requireContext(), ArrayList(historyList))
              rvHistory?.layoutManager = LinearLayoutManager(context)
              rvHistory?.adapter = adapter
          } else {
              adapter?.updateList(ArrayList(historyList))
              adapter?.notifyDataSetChanged()
          }
      }
  }

    override fun onDestroy() {
        super.onDestroy()
    }
}