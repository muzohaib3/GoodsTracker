package com.example.wms_scan.adapter.carton

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scanmate.extensions.click
import com.example.wms_scan.R
import com.example.wms_scan.adapter.pallets.PalletsAdapter
import com.example.wms_scan.data.response.GetCartonResponse
import com.example.wms_scan.data.response.GetPalletResponse
import com.example.wms_scan.databinding.CartonListViewBinding
import com.example.wms_scan.databinding.PalletListViewBinding
import com.example.wms_scan.ui.CreateCartonActivity
import com.example.wms_scan.ui.PalletsActivity

class CartonAdapter(
    private val context:Context,
    private val list:List<GetCartonResponse>
    ) : RecyclerView.Adapter<CartonAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val binding = CartonListViewBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.carton_list_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data= list[position]
        with(holder){
            binding.cartonTV.text = data.analyticalNo
        }
    }
//
    override fun getItemCount(): Int = list.size
}