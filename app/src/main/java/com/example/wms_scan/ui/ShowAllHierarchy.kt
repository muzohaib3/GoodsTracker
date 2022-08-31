package com.example.wms_scan.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetRackResponse
import com.example.scanmate.data.response.GetShelfResponse
import com.example.scanmate.data.response.GetWarehouseResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.Toast.noRecordFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.carton.CartonDetailAdapter
import com.example.wms_scan.adapter.carton.ScanCartonAdapter
import com.example.wms_scan.adapter.pallets.ScanPalletAdapter
import com.example.wms_scan.adapter.racks.ScanRackAdapter
import com.example.wms_scan.adapter.shelf.ScanShelfAdapter
import com.example.wms_scan.adapter.warehouse.ScanWarehouseAdapter
import com.example.wms_scan.data.response.GetCartonQnWiseResponse
import com.example.wms_scan.data.response.GetCartonResponse
import com.example.wms_scan.data.response.GetPalletResponse
import com.example.wms_scan.databinding.ActivityScanAllHierarchyBinding
import kotlin.collections.ArrayList

class ShowAllHierarchy : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityScanAllHierarchyBinding
    private lateinit var warehouseAdapter: ScanWarehouseAdapter
    private lateinit var racksAdapter: ScanRackAdapter
    private lateinit var shelfAdapter: ScanShelfAdapter
    private lateinit var palletAdapter: ScanPalletAdapter
    private lateinit var cartonAdapter: ScanCartonAdapter
    private lateinit var cartonQnWiseAdapter: CartonDetailAdapter
    private lateinit var dialog: CustomProgressDialog
    private var currentScreen = ""
    private var whNo = ""
    private var rackNo = ""
    private var shelfNo = ""
    private var palletNo = ""
    private var busLocNo = ""
    private var cartonAnalyticalNo = ""
    private var cartonAnalyticalKey:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanAllHierarchyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /** IS NETWORK CONNECTED VALIDATION
         */

        if (isNetworkConnected(this)) {
            setupUi()
            initObserver()
            initListener()
        }
        else {
            gotoActivity(NoNetworkActivity::class.java)
        }

    }

    private fun setupUi(){

        /**
         *  STATUS BAR AND VIEW MODEL WITH DIALOG BOX
         */

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)
        viewModel = obtainViewModel(MainViewModel::class.java)
        dialog = CustomProgressDialog(this)

        /**
         *  USER INFO FOR HEADER
         */

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        Log.i("cartonAnalyticalNo",cartonAnalyticalNo)
        Log.i("cartonAnalyticalKey",cartonAnalyticalKey.toString())

        binding.backBtn.click {
            finishAffinity()
        }

        binding.searchViewCont.queryHint = "Search item"

        /**
         *  REQUEST FOR ANALYTICAL NUMBER USING MANUAL OPTION
         */

        when
        {
            intent.extras?.getBoolean("manualAnalyticalKey") == true ->{
                var manualName = intent.extras!!.getString("manualMatName").toString()
                Log.i("ManualInput", " $manualName ")
                viewModel.getCartonQnWise(manualName)


            }
        }

        /**
         *  REQUEST FOR ANALYTICAL NUMBER USING SCAN OPTION
         */

        when
        {
            intent.extras?.getBoolean("analyticalKey") == true ->
            {
                cartonAnalyticalNo = intent.extras?.getString("c").toString()
                viewModel.getCartonQnWise(cartonAnalyticalNo)
            }
        }
    }

    private fun initObserver(){

        /**
         *  GET SCANALL OBSERVER
         */

        viewModel.scanAll.observe(this)
        {
            when(it.status)
            {
                Status.LOADING -> {
                    dialog.show()
                }

                Status.SUCCESS -> {

                    dialog.dismiss()
                    binding.hierarchyCont.visible()

                    it.let {

                        if (it.data?.get(0)?.status == true)
                        {
                            try
                            {
                                val whName = it.data[0].wHName.toString()
                                val rackName = it.data[0].rackName.toString()
                                val shelfName = it.data[0].shelfName.toString()
                                val palletName = it.data[0].pilotName.toString()
                                whNo = it.data[0].wHNo.toString()
                                rackNo = it.data[0].rackNo.toString()
                                shelfNo = it.data[0].shelfNo.toString()
                                palletNo = it.data[0].pilotNo.toString()
                                busLocNo = it.data[0].locationNo.toString()

                                binding.WHTV.text = whName
                                binding.rackTV.text = rackName
                                binding.shelfTV.text = shelfName
                                binding.palletTV.text = palletName

                                Log.i("allHierarchy", it.data[0].rackCode.toString())

                                val warehouse = intent.extras?.getString("w").toString()
                                val rack = intent.extras?.getString("r").toString()
                                val shelve = intent.extras?.getString("s").toString()
                                val palette = intent.extras?.getString("p").toString()

                                Log.i("scannerCameraActivityRack",rack)
                                Log.i("scannerCameraActivityWarehouse",warehouse)
                                Log.i("scannerCameraActivityShelve",shelve)
                                Log.i("scannerCameraActivityPalette",palette)
                                Log.i("scannerCameraActivityLocNo",busLocNo)

                                when
                                {

                                    palette.contains("PL") ->
                                    {
                                        viewModel.getCarton(
                                            Utils.getSimpleTextBody(palletNo),
                                            Utils.getSimpleTextBody(busLocNo)
                                        )
                                        Log.i("palLoc","${it.data[0].pilotCode}")
                                        Log.i("palletNoScan",palletNo)
                                        toast("global var palletNo: $palletNo")
                                    }

                                    shelve.contains("SF") ->
                                    {
                                        val animFade = AnimationUtils.loadAnimation(this,R.anim.fade_out)
                                        viewModel.getPallet(
                                            Utils.getSimpleTextBody(""),
                                            Utils.getSimpleTextBody(shelfNo),
                                            Utils.getSimpleTextBody(busLocNo),
                                        )
                                        binding.view7.gone()
                                        binding.view7.startAnimation(animFade)
                                        binding.view8.gone()
                                        binding.view8.startAnimation(animFade)
                                        binding.palletCont.gone()
                                        binding.palletCont.startAnimation(animFade)
                                    }

                                    rack.contains("RK") ->
                                    {
                                        val animFade = AnimationUtils.loadAnimation(this,R.anim.fade_out)
                                        viewModel.getShelf(
                                            Utils.getSimpleTextBody(""),
                                            Utils.getSimpleTextBody(rackNo),
                                            Utils.getSimpleTextBody(busLocNo),
                                        )

                                        binding.view5.gone()
                                        binding.view5.startAnimation(animFade)
                                        binding.view6.gone()
                                        binding.view6.startAnimation(animFade)
                                        binding.shelfCont.gone()
                                        binding.shelfCont.startAnimation(animFade)
                                        binding.view7.gone()
                                        binding.view7.startAnimation(animFade)
                                        binding.view8.gone()
                                        binding.view8.startAnimation(animFade)
                                        binding.palletCont.gone()
                                        binding.palletCont.startAnimation(animFade)

                                    }

                                    warehouse.contains("WH") ->
                                    {
                                        viewModel.getRack(
                                            Utils.getSimpleTextBody(""),
                                            Utils.getSimpleTextBody(whNo),
                                            Utils.getSimpleTextBody(busLocNo)
                                        )

                                        binding.view3.gone()
                                        binding.view4.gone()
                                        binding.rackCont.gone()
                                        binding.view5.gone()
                                        binding.view6.gone()
                                        binding.shelfCont.gone()
                                        binding.view7.gone()
                                        binding.view8.gone()
                                        binding.palletCont.gone()

                                    }
                                }

                                binding.WHTV.click {
                                    if (isNetworkConnected(this))
                                    {
                                        currentScreen = "W"
                                        viewModel.getRack(
                                            Utils.getSimpleTextBody(""),
                                            Utils.getSimpleTextBody(whNo),
                                            Utils.getSimpleTextBody(busLocNo),
                                        )
                                        binding.view3.gone()
                                        binding.view4.gone()
                                        binding.rackCont.gone()
                                        binding.view5.gone()
                                        binding.view6.gone()
                                        binding.shelfCont.gone()
                                        binding.view7.gone()
                                        binding.view8.gone()
                                        binding.palletCont.gone()
                                    }
                                    else
                                    {
                                        binding.WHTV.isEnabled = false
                                    }
                                }

                                binding.rackTV.click {
                                    if (isNetworkConnected(this)) {
                                        currentScreen = "R"
                                        viewModel.getShelf(
                                            Utils.getSimpleTextBody(""),
                                            Utils.getSimpleTextBody(rackNo),
                                            Utils.getSimpleTextBody(busLocNo)
                                        )
                                        binding.view5.gone()
                                        binding.view6.gone()
                                        binding.shelfCont.gone()
                                        binding.view7.gone()
                                        binding.view8.gone()
                                        binding.palletCont.gone()
                                    }
                                    else
                                    {
                                        binding.rackTV.isEnabled = false
                                    }
                                }

                                binding.shelfTV.click {
                                    if (isNetworkConnected(this))
                                    {
                                        currentScreen = "S"
                                        viewModel.getPallet(
                                            Utils.getSimpleTextBody(""),
                                            Utils.getSimpleTextBody(shelfNo),
                                            Utils.getSimpleTextBody(busLocNo)
                                        )
                                        binding.view7.gone()
                                        binding.view8.gone()
                                        binding.palletCont.gone()
                                    }
                                    else
                                    {
                                        binding.shelfTV.isEnabled = false
                                    }
                                }

                                binding.palletTV.click {
                                    if (isNetworkConnected(this)) {
                                        currentScreen = "P"
                                        viewModel.getCarton(
                                            Utils.getSimpleTextBody(palletNo),
                                            Utils.getSimpleTextBody(busLocNo),
                                        )
                                        binding.palletCont.visible()
                                    }
                                    else {
                                        binding.palletTV.isEnabled = false
                                    }
                                }
                            }
                            catch (e:Exception) {
                                dialog.dismiss()
                                Log.i("scanAllHierarchy","${e.message}")
                                toast("No data found")
                                binding.listSize.gone()
                                binding.itemTV.text = "No data"

                            }
                        }
                        else
                        {
                            dialog.dismiss()
                            finish()
                            toast("Response not found")
                            binding.listSize.gone()
                            binding.itemTV.text = "No data"
                        }
                    }
                }

                Status.ERROR ->
                {
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    binding.hierarchyCont.gone()
                    dialog.dismiss()
                    Log.i("scanAllHierarchy","${Exception().message}")
                    toast("Error found")
                    binding.listSize.gone()
                    binding.itemTV.text = "No data"
                }
                else -> {}
            }
        }

        /**
         *  GET WAREHOUSE OBSERVER
         */

        viewModel.getWarehouse.observe(this){

            when(it.status)
            {
                Status.LOADING ->{
                    dialog.show()
                    dialog.setCanceledOnTouchOutside(true)
                }

                Status.SUCCESS ->
                {
                    try
                    {
                        if (it.data?.get(0)?.status == true)
                        {
                            dialog.dismiss()
                            binding.hierarchyCont.visible()
                            warehouseAdapter = ScanWarehouseAdapter(this,
                                it.data as ArrayList<GetWarehouseResponse>
                            )

                            binding.showAllRV.apply {
                                layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                                adapter = warehouseAdapter
                            }

                            binding.listSize.text = "Total Record : ${it.data.size}"
                            binding.itemTV.text = it.data[0].wHName
                            Log.i("warehouseCode", it.data[0].wHName.toString())

                            binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                                override fun onQueryTextSubmit(query: String?): Boolean {
                                    return true
                                }

                                override fun onQueryTextChange(newText: String?): Boolean {
                                    warehouseAdapter.filter.filter(newText)
                                    return true
                                }
                            })
                        }
                        else
                        {
                            toast(noRecordFound)
                            dialog.dismiss()
                            binding.showAllRV.adapter = null
                        }

                    }
                    catch (e:Exception)
                    {
                        Log.i("getWarehouse","${e.message}")
                        toast("${e.message}")
                    }

                }
                Status.ERROR ->
                {
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    binding.hierarchyCont.gone()
                    toast(it.data?.get(0)?.error!!)
                    dialog.dismiss()
                }

                else -> {}
            }
        }

        /**
         *  GET SHELF OBSERVER
         */

        viewModel.getRack.observe(this)
        {
            when(it.status){
                Status.LOADING ->{
                    dialog.show()
                    dialog.setCanceledOnTouchOutside(true)
                }

                Status.SUCCESS ->
                {
                    try
                    {
                        if (it.data?.get(0)?.status == true)
                        {
                            dialog.dismiss()
                            binding.hierarchyCont.visible()
                            racksAdapter = ScanRackAdapter(this, it.data as ArrayList<GetRackResponse>)

                            binding.showAllRV.apply {
                                layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                                adapter = racksAdapter

                            }

                            binding.itemTV.text = it.data[0].wHName
                            Log.i("warehouseCode", it.data[0].wHName.toString())
                            binding.listSize.text = "Total Record : ${it.data.size}"
                            binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                                override fun onQueryTextSubmit(query: String?): Boolean {
                                    return true
                                }

                                override fun onQueryTextChange(newText: String?): Boolean {
                                    racksAdapter.filter.filter(newText)
                                    return true
                                }
                            })
                        }
                        else
                        {
                            toast(noRecordFound)
                            dialog.dismiss()
                            binding.showAllRV.adapter = null
                        }
                    }
                    catch (e:Exception)
                    {
                        Log.i("getRack","${e.message}")
                        toast("${e.message}")
                    }
                }
                Status.ERROR ->
                {
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    binding.hierarchyCont.gone()
                    toast(it.data?.get(0)?.error!!)
                    dialog.dismiss()
                    finish()
                }

                else -> {}
            }
        }

        /**
         *  GET SHELF OBSERVER
         */

        viewModel.getShelf.observe(this){
            when(it.status){

                Status.LOADING ->
                {
                    dialog.show()
                    dialog.setCanceledOnTouchOutside(true)
                }

                Status.SUCCESS ->
                {
                    try
                    {
                        if (it.data?.get(0)?.status == true)
                        {
                            dialog.dismiss()
                            binding.hierarchyCont.visible()
                            shelfAdapter = ScanShelfAdapter(this,
                                it.data as ArrayList<GetShelfResponse>
                            )

                            binding.showAllRV.apply {
                                layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                                adapter = shelfAdapter
                            }
                            binding.itemTV.text = it.data[0].rackName
                            Log.i("shelfData", it.data[0].shelfName.toString())
                            binding.listSize.text = "Total Record : ${it.data.size}"

                            binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                                override fun onQueryTextSubmit(query: String?): Boolean {
                                    return true
                                }

                                override fun onQueryTextChange(newText: String?): Boolean {
                                    shelfAdapter.filter.filter(newText)
                                    return true
                                }
                            })
                        }
                        else
                        {
                            toast(noRecordFound)
                            dialog.dismiss()
                            binding.showAllRV.adapter = null
                        }
                    }
                    catch (e:Exception)
                    {
                        Log.i("getShelf","${e.message}")
                        toast("${e.message}")
                    }
                }
                Status.ERROR ->
                {
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    binding.hierarchyCont.gone()
                    toast(it.data?.get(0)?.error!!)
                    dialog.dismiss()
                }
                else -> {}
            }
        }

        /**
         *  GET PALLET OBSERVER
         */

        viewModel.getPallet.observe(this){
            when(it.status) {
                Status.LOADING ->
                {
                    dialog.show()
                    dialog.setCanceledOnTouchOutside(true)
                }

                Status.SUCCESS ->
                {
                    try
                    {
                        if (it.data?.get(0)?.status == true)
                        {
                            dialog.dismiss()
                            binding.hierarchyCont.visible()
                            palletAdapter = ScanPalletAdapter(this,
                                it.data as ArrayList<GetPalletResponse>
                            )

                            binding.showAllRV.apply {

                                layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                                adapter = palletAdapter

                            }
                            binding.itemTV.text = it.data[0].shelfName
                            binding.listSize.text = "Total Record : ${it.data.size}"

                            binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                                override fun onQueryTextSubmit(query: String?): Boolean
                                {
                                    return true
                                }

                                override fun onQueryTextChange(newText: String?): Boolean
                                {
                                    palletAdapter.filter.filter(newText)
                                    return true
                                }
                            })
                        }
                        else
                        {
                            toast(noRecordFound)
                            dialog.dismiss()
                            binding.showAllRV.adapter = null
                        }
                    }
                    catch (e:Exception)
                    {
                        Log.i("getPallet","${e.message}")
                        toast("${e.message}")
                        binding.showAllRV.adapter = null
                    }
                }

                Status.ERROR -> {
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    binding.hierarchyCont.gone()
                    toast(it.data?.get(0)?.error!!)
                    dialog.dismiss()
                    binding.showAllRV.adapter = null
                }
                else -> {}
            }
        }

        /**
         *  GET CARTON OBSERVER
         */

        viewModel.getCarton.observe(this){
            when(it.status){
                Status.LOADING ->
                {
                    dialog.show()
                    dialog.setCanceledOnTouchOutside(true)
                }

                Status.SUCCESS ->
                {
                    try
                    {
                      if (it.data?.get(0)?.status == true)
                      {

                          dialog.dismiss()
                          binding.hierarchyCont.visible()
                          binding.hierarchyTree.visible()
                          binding.treeView.visible()
                          binding.cartonQnWiseCont.gone()
                          cartonAdapter = ScanCartonAdapter(this,
                              it.data as ArrayList<GetCartonResponse>
                          )
                          binding.showAllRV.apply {
                              layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                              adapter = cartonAdapter

                          }
                          binding.hierarchyCont.visible()
                          binding.itemTV.text = it.data[0].pilotName
                          binding.listSize.text = "Total Record : ${it.data.size}"

                          Log.i("getCartonData", it.data[0].toString())
                          Log.i(
                              "getCartonDetailParam",
                              "${it.data[0].cartonCode.toString()}\n${it.data[0].analyticalNo}\n ${it.data[0].cartonSNo}\n" +
                                      "${it.data[0].pilotNo}"
                          )

                          binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                              override fun onQueryTextSubmit(query: String?): Boolean
                              {
                                  return true
                              }

                              override fun onQueryTextChange(newText: String?): Boolean
                              {
                                  cartonAdapter.filter.filter(newText)
                                  return false
                              }
                          })
                      }
                        else
                      {
                          toast(noRecordFound)
                          dialog.dismiss()
                      }
                    }
                    catch (e:Exception)
                    {
                        Log.i("getCarton","${e.message}")
                        toast("${e.message}")
                        binding.listSize.gone()
                        binding.itemTV.text = "No data"

                    }
                }

                Status.ERROR -> {

                    Log.i("cartonError","${Exception().message}")
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    toast("${Exception().message}")
                    dialog.dismiss()
                    binding.listSize.gone()
                    binding.scanCont.gone()
                    binding.connectionTimeout.visible()
                    binding.itemTV.text = "No data"

                }
                else -> {}
            }
        }

        /**
         *  GET CARTONQNWISE OBSERVER
         */

        viewModel.getCartonQnWise.observe(this){

            when(it.status){
                Status.LOADING ->
                {
                    dialog.show()
                    dialog.setCanceledOnTouchOutside(true)
                }

                Status.SUCCESS ->
                {
                    try
                    {
                       if(it.data?.get(0)?.status == true)
                       {

                           if (it.data[0].analyticalNo?.contains("RM")!!)
                           {
                               binding.hierarchyCont.gone()
                               binding.hierarchyTree.gone()
                               binding.treeView.gone()
                               dialog.dismiss()
                               cartonQnWiseAdapter = CartonDetailAdapter(this,
                                   it.data as ArrayList<GetCartonQnWiseResponse>
                               )
                               binding.showAllRV.apply {
                                   layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                                   adapter = cartonQnWiseAdapter
                                   Log.i("analyticalNo", it.data[0].analyticalNo.toString())
                                   binding.slash.visible()
                               }
                               Log.i(
                                   "getCartonQnWiseParam",
                                   "${it.data[0].materialName.toString()}\n${it.data[0].analyticalNo}\n ${it.data[0].cartonSNo}"
                               )
//                           binding.itemTV.text = "Analytical Number : ${it.data[0].analyticalNo.toString()}"
                               binding.listSize.text = "Total Record : ${it.data.size}"
                               binding.subDirectoryIcon.gone()
                               binding.hierarchyNameCont.gone()
                               binding.cartonQnWiseCont.visible()

                               //analytical number container visible
                               binding.analyticalCont.visible()
                               binding.analyticalNoTV.text = "${it.data[0].analyticalNo?.trim()}"

                               //stock number container visible
                               binding.stockCont.visible()
                               binding.cartonStockTV.text = "Stock inhand:\n${it.data[0].matStock!!}"
                               Log.i("cartonQNData","${it.data[0].matStock}")


                               //tot carton visible
                               binding.cartonDetailCont.visible()
                               binding.totCartonTV.text = "${it.data[0].totCarton}"
                               binding.cartonSNOTV.text = " Total Carton :  ${it.data[0].cartonSNo}"
                               Log.i("cartonQNData","${it.data[0].cartonSNo}")


                               /**
                                * Search filter for recyclerview
                                */

                               binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                                   override fun onQueryTextSubmit(query: String?): Boolean
                                   {

                                       return true
                                   }

                                   override fun onQueryTextChange(newText: String?): Boolean
                                   {
                                       cartonQnWiseAdapter.filter.filter(newText)
                                       return false
                                   }
                               })

                           }

                           cartonQnWiseAdapter = CartonDetailAdapter(this,
                               it.data as ArrayList<GetCartonQnWiseResponse>
                           )
                           binding.showAllRV.apply {
                               layoutManager = LinearLayoutManager(this@ShowAllHierarchy)
                               adapter = cartonQnWiseAdapter
                               Log.i("analyticalNo", it.data[0].analyticalNo.toString())
                               binding.slash.visible()
                           }
                           Log.i(
                               "getCartonQnWiseParam",
                               "${it.data[0].materialName.toString()}\n${it.data[0].analyticalNo}\n ${it.data[0].cartonSNo}"
                           )

                           /**
                            * Search filter for recyclerview
                            */

                           binding.searchViewCont.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                               override fun onQueryTextSubmit(query: String?): Boolean
                               {

                                   return true
                               }

                               override fun onQueryTextChange(newText: String?): Boolean
                               {
                                   cartonQnWiseAdapter.filter.filter(newText)
                                   return false
                               }
                           })


                           //material name visible
                           binding.materialNameCont.visible()
                           binding.materialNameTV.text = "${it.data[0].materialName?.trim()}"
                           Log.i("cartonQNData","${it.data[0].materialName}")


                           //item code visible
                           binding.itemCodeCont.visible()
                           binding.itemCodeTV.text = "${it.data[0].itemCode?.trim()}"
                           Log.i("cartonQNData","${it.data[0].itemCode}")

                           //potency
                           binding.potencyTV.text = "Potency : ${it.data[0].potency}"
                           Log.i("cartonQNData","${it.data[0].potency}")


                           //release date
                           binding.releaseTV.text = "Release date :${it.data[0].releaseDate}"
                           Log.i("cartonQNData","${it.data[0].releaseDate}")

                           //storage condition
                           binding.storageConditionTV.text = "Storage condition : ${it.data[0].storageCondition}"
                           Log.i("cartonQNData","${it.data[0].storageCondition}")

                           //batch no
                           binding.batchNoTV.text = "${it.data[0].batchNo}"
                           Log.i("cartonQNData","Batch No : ${it.data[0].batchNo}")

                           //source
                           binding.sourceTV.text = "Source : ${it.data[0].suppName}"
                           Log.i("cartonQNData","${it.data[0].suppName}")

                           binding.hierarchyCont.gone()
                           binding.hierarchyTree.gone()
                           binding.treeView.gone()
                           dialog.dismiss()

//                           binding.itemTV.text = "Analytical Number : ${it.data[0].analyticalNo.toString()}"
                           binding.listSize.text = "Total Record : ${it.data.size}"
                           binding.subDirectoryIcon.gone()
                           binding.hierarchyNameCont.gone()
                           binding.cartonQnWiseCont.visible()

                           //analytical number container visible
                           binding.analyticalCont.visible()
                           binding.analyticalNoTV.text = "${it.data[0].analyticalNo?.trim()}"

                           //stock number container visible
                           binding.stockCont.visible()
                           binding.cartonStockTV.text = "Stock inhand:\n${it.data[0].matStock!!}"


                           //tot carton visible
                           binding.cartonDetailCont.visible()
                           binding.totCartonTV.text = "${it.data[0].totCarton}"
                           binding.cartonSNOTV.text = " Total Carton :  ${it.data[0].cartonSNo}"


                           //material name visible
                           binding.materialNameCont.visible()
                           binding.materialNameTV.text = "${it.data[0].materialName?.trim()}"


                           //item code visible
                           binding.itemCodeCont.visible()
                           binding.itemCodeTV.text = "${it.data[0].itemCode?.trim()}"

                       }
                        else
                       {
                           toast(noRecordFound)
                           dialog.dismiss()
                           binding.showAllRV.adapter = null
                           finish()
                       }
                    }
                    catch (e:Exception)
                    {
                        Log.i("getCartonQnWiseException","${e.message}")
                        toast("${e.message}")
                        binding.showAllRV.adapter = null
                    }

                }
                Status.ERROR -> {
                    binding.hierarchyTree.gone()
                    binding.treeView.gone()
                    binding.hierarchyCont.gone()
                    binding.showAllRV.adapter = null
                    toast(noRecordFound)
                    dialog.dismiss()
                }
                else -> {}
            }
        }
    }

    /**
     *  INITIALIZE LISTENERS
     */
    private fun initListener()
    {
        binding.scanIV.click {
            if (isNetworkConnected(this))
            {
                val intent = Intent(this, ScannerCameraActivity::class.java)
                finish()
                startActivity(intent)
            }
            else
            {
                binding.scanIV.isEnabled = false
            }
        }


        binding.scanBtn.click {
            gotoActivity(ScannerActivity::class.java)
            finish()
        }

        binding.showMoreCont
            .click {
            binding.allDetailsCont.visible()
            binding.showMoreCont.gone()
            binding.showLessCont.visible()
            binding.searchViewCont.gone()
            binding.listSize.gone()
            binding.showAllRV.gone()
        }

        binding.showLessCont.click {
            binding.allDetailsCont.gone()
            binding.showMoreCont.visible()
            binding.showLessCont.gone()
            binding.searchViewCont.visible()
            binding.listSize.visible()
            binding.showAllRV.visible()

        }
    }

    /**
     *  LISTENERS OF ADAPTER ONCLICK
     */

    fun doAction(cs: String, actionNo: String, actionName: String)
    {
        when(cs)
        {
            "W" ->
            {
                currentScreen = "W"
                this.whNo = actionNo
                    viewModel.getRack(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(actionNo),
                        Utils.getSimpleTextBody(busLocNo)
                    )
            }

            "R" ->
            {
                currentScreen = "R"
                this.rackNo = actionNo
                    viewModel.getShelf(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(actionNo),
                        Utils.getSimpleTextBody(busLocNo),
                    )
                    binding.view3.visible()
                    binding.view4.visible()
                    binding.rackCont.visible()
                    binding.rackTV.text = actionName
            }

            "S" ->
            {
                currentScreen = "S"
                this.shelfNo = actionNo
                    viewModel.getPallet(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(actionNo),
                        Utils.getSimpleTextBody(busLocNo),
                    )
                    binding.view3.visible()
                    binding.view4.visible()
                    binding.rackCont.visible()
                    binding.view5.visible()
                    binding.view6.visible()
                    binding.shelfCont.visible()
                    binding.shelfTV.text = actionName
            }

            "P" ->
            {
                currentScreen = "P"
                this.palletNo = actionNo
                    viewModel.getCarton(
                        Utils.getSimpleTextBody(actionNo),
                        Utils.getSimpleTextBody(busLocNo)
                    )
                toast(actionNo)
                    binding.view3.visible()
                    binding.view4.visible()
                    binding.rackCont.visible()
                    binding.view5.visible()
                    binding.view6.visible()
                    binding.shelfCont.visible()
                    binding.view7.visible()
                    binding.view8.visible()
                    binding.palletCont.visible()
                    binding.palletTV.text = actionName

            }

            "M"->
            {
                currentScreen = "M"
                Log.i("analyticalNo",actionNo)
                viewModel.getCartonQnWise(actionNo)

            }
        }

    }

    /**
     *  FILTER USING SEARCH VIEW FOR ALL ADAPTERS
     */

    fun filterUpdateRecord(size:Int)
    {
        binding.listSize.text = "Total Record : $size"
    }

    /**
     *  ON RESUME INITIALIZATION
     */

    override fun onResume() {
        super.onResume()

        val warehouse = intent.extras?.getString("w").toString()
        val rack = intent.extras?.getString("r").toString()
        val shelve = intent.extras?.getString("s").toString()
        val palette = intent.extras?.getString("p").toString()
        val scannedData = intent.extras?.getString("scannedData").toString()
        val locationNo = scannedData.substringBefore("L")

        if(palette.contains("PL")) {
            viewModel.scanAll(palette, locationNo)
            currentScreen = "P"

        }
        if (shelve.contains("SF")) {
            viewModel.scanAll(shelve, locationNo)
            currentScreen = "S"
        }
        if (rack.contains("RK")) {
            viewModel.scanAll(rack, locationNo)
            currentScreen = "R"
        }
        if (warehouse.contains("WH")) {
            viewModel.scanAll(warehouse, locationNo)
            currentScreen = "W"
        }
    }

    /**
     *  ON BACK PRESS FUNCTIONALITY
     */

    override fun onBackPressed() {

//        toast(currentScreen)
        if (isNetworkConnected(this))
        {
           when(currentScreen)
           {
               "M"->
               {
                   viewModel.getCarton(
                               Utils.getSimpleTextBody(palletNo),
                               Utils.getSimpleTextBody(busLocNo),
                           )
                   binding.view7.visible()
                   binding.view8.visible()
                   binding.palletCont.visible()
                   currentScreen = "P"

               }
               "P" ->
               {
                  viewModel.getPallet(
                      Utils.getSimpleTextBody(""),
                      Utils.getSimpleTextBody(shelfNo),
                      Utils.getSimpleTextBody(busLocNo),
                  )
                   binding.view7.gone()
                   binding.view8.gone()
                   binding.palletCont.gone()
                   currentScreen = "S"

               }
               "S" ->
               {
                    viewModel.getShelf(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(rackNo),
                        Utils.getSimpleTextBody(busLocNo),
                    )
                   binding.view5.gone()
                   binding.view6.gone()
                   binding.shelfCont.gone()
                   binding.view7.gone()
                   binding.view8.gone()
                   binding.palletCont.gone()
                   currentScreen = "R"
               }
               "R" ->
               {
                   viewModel.getRack(
                       Utils.getSimpleTextBody(""),
                       Utils.getSimpleTextBody(whNo),
                       Utils.getSimpleTextBody(busLocNo),
                   )
                   binding.view3.gone()
                   binding.view4.gone()
                   binding.rackCont.gone()
                   binding.view5.gone()
                   binding.view6.gone()
                   binding.shelfCont.gone()
                   binding.view7.gone()
                   binding.view8.gone()
                   binding.palletCont.gone()
                   currentScreen = "W"
               }
               "W" ->
               {
                  finish()
               }
               else->
               {
                   finish()
               }
           }
        }
        }
    }
