package com.example.wms_scan.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetRackResponse
import com.example.scanmate.data.response.GetShelfResponse
import com.example.scanmate.data.response.GetWarehouseResponse
import com.example.scanmate.data.response.UserLocationResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.LogMessages.error
import com.example.scanmate.util.Constants.LogMessages.loading
import com.example.scanmate.util.Constants.LogMessages.success
import com.example.scanmate.util.Constants.Toast.NoInternetFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isRefreshRequired
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.pallets.PalletsAdapter
import com.example.wms_scan.data.response.GetPalletResponse
import com.example.wms_scan.databinding.ActivityPalletsBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PalletsActivity : AppCompatActivity() {
    private lateinit var binding:ActivityPalletsBinding
    private lateinit var palletAdapter: PalletsAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private var selectedBusLocNo = ""
    private var selectedWareHouseNo = ""
    private var selectedRackNo = ""
    private var selectedShelveNo = ""
    private var busLocName = ""
    private var warehouseName = ""
    private var rackName = ""
    private var shelfName = ""
    private lateinit var bmp:Bitmap
    private val bmpList = mutableListOf<Bitmap>()
    private val palletCodeList = mutableListOf<String>()
    private var STORAGE_CODE = 1001
    private var shelfCode = ""
    private var rackCode = ""
    private var whCode = ""
    private var capacity = ""
    private var palletCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPalletsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initListeners()
        initObservers()

    }

    private fun setupUi(){

        dialog = CustomProgressDialog(this)
        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        val businessSpinner = binding.businessLocationSpinner
        val warehouseSpinner = binding.warehouseSpinner
        val rackSpinner = binding.rackSpinner
        val shelfSpinner = binding.shelfSpinner

        if ((businessSpinner.adapter != null) and (warehouseSpinner.adapter != null)
            and (rackSpinner.adapter != null) and (shelfSpinner.adapter != null))
        {
            viewModel.userLocation(
                Utils.getSimpleTextBody(
                    LocalPreferences.getInt(this, LocalPreferences.AppLoginPreferences.userNo).toString()
                ))
            warehouseSpinner.gone()
            binding.availablePallets.gone()
        }

    }

    private fun initListeners(){

        binding.swipeRefresh.setOnRefreshListener {
            if (isNetworkConnected(this))
            {
                binding.palletCont.visible()
                binding.connectionTimeout.gone()
                viewModel.userLocation(
                    Utils.getSimpleTextBody(
                        LocalPreferences.getInt(this, LocalPreferences.AppLoginPreferences.userNo).toString()
                    )
                )
                viewModel.getWarehouse("", selectedBusLocNo)

                viewModel.getRack(
                    Utils.getSimpleTextBody(""),
                    Utils.getSimpleTextBody(selectedWareHouseNo),
                    Utils.getSimpleTextBody(selectedBusLocNo)
                )

                viewModel.getShelf(
                    Utils.getSimpleTextBody(""),
                    Utils.getSimpleTextBody(selectedRackNo),
                    Utils.getSimpleTextBody(selectedBusLocNo)
                )
            }
            else
            {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.palletAddBTN.click {

            if (isNetworkConnected(this))
            {
                    val intent = Intent(this, AddUpdatePalletDetails::class.java)
                    intent.putExtra("addBusLocNo",selectedBusLocNo)
                    intent.putExtra("addWHNo",selectedWareHouseNo)
                    intent.putExtra("addRackNo",selectedRackNo)
                    intent.putExtra("addShelfNo",selectedShelveNo)
                    intent.putExtra("addBusLocName",busLocName)
                    intent.putExtra("addWHName",warehouseName)
                    intent.putExtra("addRackName",rackName)
                    intent.putExtra("addShelfName",shelfName)
                    intent.putExtra("palletCode",palletCode)
                    intent.putExtra("palletCap",capacity)
                    intent.putExtra("AddPalletKey",true)
                    startActivity(intent)

            }
        }


        binding.printIV.click {
            try
            {
                generatePDF()
            }
            catch (e:Exception)
            {
                Log.i("exception","${e.message}")
            }

        }

        binding.backBtn.click{
            onBackPressed()
        }

    }

    private fun initObservers(){

        /**
         *      USER LOCATION OBSERVER
         */

        viewModel.userLocation(
            Utils.getSimpleTextBody(
                LocalPreferences.getInt(this,
                    LocalPreferences.AppLoginPreferences.userNo
                ).toString()
            ))
        viewModel.userLoc.observe(this, Observer {
            when(it.status){
                Status.LOADING->{
                    dialog.setCanceledOnTouchOutside(true)
                }
                Status.SUCCESS ->{

                    binding.businessLocationSpinner.visible()
                    binding.businessSpinnerCont.visible()

                    if (isNetworkConnected(this)){
                        it.let {
                            if(it.data?.get(0)?.status == true)
                            {
                                dialog.dismiss()
                                showBusLocSpinner(it.data!!)
                                binding.palletAddBTN.isEnabled = true
                            }
                            else
                            { }
                        }
                    }
                    else
                    {
                        binding.palletsRV.adapter = null
                        binding.palletAddBTN.isEnabled = false
                        binding.businessLocationSpinner.gone()
                    }
                }

                Status.ERROR ->
                {
                    dialog.dismiss()
                    binding.swipeRefresh.isRefreshing = false
                    binding.palletCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

        /**
         *      GET WAREHOUSE OBSERVER
         */

        viewModel.getWarehouse.observe(this, Observer{
            when(it.status){
                Status.LOADING->{
                }
                Status.SUCCESS ->{

                    binding.warehouseSpinner.visible()
                    binding.warehouseSpinnerCont.visible()

                    if (isNetworkConnected(this)){
                        try {
                            if(it.data?.get(0)?.status == true)
                            {
                                it.data[0].wHName?.let { it1 -> Log.i("warehouseResponse", it1) }
                                showWarehouseSpinner(it.data)
                                binding.warehouseSpinnerCont.visible()
                                binding.rackSpinnerCont.visible()
                                binding.shelfSpinnerCont.visible()
                                binding.palletAddBTN.visible()

                            }
                            else
                            {
                                binding.warehouseSpinnerCont.gone()
                                binding.rackSpinnerCont.gone()
                                binding.shelfSpinnerCont.gone()
                                binding.palletsRV.adapter = null
                                binding.palletAddBTN.gone()
                                binding.availablePallets.gone()
                                binding.printIV.click {
                                    toast("Nothing to print!")
                                }
                            }
                        }
                        catch(e:Exception){
                            Log.i("rackAdapter","${e.message}")
                            Log.i("rackAdapter","${e.stackTrace}")
                        }
                        //warehouseAdapter.addItems(list)
                    }
                    else
                    {
                        binding.palletsRV.adapter = null
                        binding.palletAddBTN.isEnabled = false
                        binding.warehouseSpinnerCont.gone()
                    }
                }
                Status.ERROR ->{
                    dialog.dismiss()
                    binding.swipeRefresh.isRefreshing = false
                    binding.palletCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

        /**
         *      GET RACK OBSERVER
         */

        viewModel.getRack.observe(this, Observer{
            when(it.status)
            {
                Status.LOADING ->{
                }
                Status.SUCCESS ->{

                    binding.rackSpinner.visible()
                    binding.rackSpinnerCont.visible()

                    if (isNetworkConnected(this)){
                        try
                        {
                            if(it.data?.get(0)?.status == true)
                            {
                                showRackSpinner(it.data)
                                binding.availablePallets.visible()
                                binding.palletAddBTN.visible()
                                binding.rackSpinnerCont.visible()
                                binding.shelfSpinnerCont.visible()

                            }
                            else
                            {
                                binding.palletsRV.adapter = null
                                binding.rackSpinnerCont.gone()
                                binding.shelfSpinnerCont.gone()
                                binding.palletAddBTN.gone()
                                binding.availablePallets.gone()
                                binding.printIV.click {
                                    toast("Nothing to print!")
                                }
                            }
                        }
                        catch (e: Exception)
                        {
                            Log.i("RACK_OBSERVER","${e.message}")
                            Log.i("RACK_OBSERVER","${e.stackTrace}")
                        }
                    }
                    else
                    {
                        binding.palletsRV.adapter = null
                        binding.palletAddBTN.isEnabled = false
                        binding.rackSpinnerCont.gone()
                    }
                    // Log.i("getRack",it.data?.get(0)?.rackNo.toString())

                }
                Status.ERROR ->{
                    dialog.dismiss()
                    binding.swipeRefresh.isRefreshing = false
                    binding.palletCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

        /**
         *      GET SHELF OBSERVER
         */

        viewModel.getShelf.observe(this, Observer{
            when(it.status){
                Status.LOADING ->{
                }
                Status.SUCCESS ->{

                    binding.shelfSpinner.visible()
                    binding.shelfSpinnerCont.visible()
                    binding.availablePallets.visible()

                    if (isNetworkConnected(this)){
                        try {
                            if(it.data?.get(0)?.status == true)
                            {
                                showShelfSpinner(it.data)
                                binding.shelfSpinnerCont.visible()
                                binding.availablePallets.visible()
                                binding.palletAddBTN.visible()
                            }
                            else
                            {
                                binding.palletsRV.adapter = null
                                binding.shelfSpinnerCont.gone()
                                binding.availablePallets.gone()
                                binding.palletAddBTN.gone()
                                binding.printIV.click {
                                    toast("Nothing to print!")
                                }
                            }
                        }
                        catch (e:Exception){
                            Log.i("","${e.message}")
                            Log.i("rackAdapter","${e.stackTrace}")
                        }
                    }
                    else
                    {
                        binding.palletsRV.adapter = null
                        binding.palletAddBTN.isEnabled = false
                        binding.shelfSpinnerCont.gone()
                    }
                }
                Status.ERROR ->{
                    binding.swipeRefresh.isRefreshing = false
                    binding.palletCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

        /**
         *      GET PALLET OBSERVER
         */

        viewModel.getPallet.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{
                    binding.swipeRefresh.isRefreshing = true
                    Log.i(loading,"Loading")
                }
                Status.SUCCESS ->{
                    if (isNetworkConnected(this))
                    {
                        binding.swipeRefresh.isRefreshing = false
                        try
                        {
                            LocalPreferences.put(this,isRefreshRequired, true)
                            if(it.data?.get(0)?.status == true)
                            {

                                Log.i(success,"Success")
                                palletAdapter = PalletsAdapter(this, it.data)

                                bmpList.clear()
                                capacity = it.data[0].capacity.toString()
                                palletCode = it.data[0].pilotCode.toString()
                                binding.availablePallets.visible()
                                binding.palletAddBTN.visible()
                                binding.printIV.click { btn ->

                                    for (i in it.data)
                                    {
                                        Log.i("WarehouseCodeName","${whCode}")
                                        Log.i("RackCodeName","${rackCode}")
                                        Log.i("ShelfCodeName","${shelfCode}")
                                        Log.i("PalletCodeName","${palletCodeList}")

                                        generateQRCode("${i.pilotCode}")
                                        Log.i("palletCode","${selectedBusLocNo}L-${whCode}-${rackCode}-${shelfCode}-${i.pilotCode}")

                                        palletCodeList.add("${i.pilotName}")
//                                Log.i("PalletCodes","$palletCodeList")
                                    }
                                    generatePDF()
                                }
                                binding.palletsRV.apply {
                                    adapter = palletAdapter
                                    layoutManager = LinearLayoutManager(this@PalletsActivity)
                                }

                            }
                            else
                            {
                                binding.palletsRV.adapter = null
                                binding.availablePallets.gone()
                                binding.printIV.click { btn ->
                                    toast("Nothing to print!")
                                }
                            }
                        }
                        catch (e:Exception)
                        {
                            Log.i("","${e.message}")
                            Log.i("rackAdapter","${e.stackTrace}")
                        }
                    }
                    else
                    {
                        binding.palletsRV.adapter = null
                        binding.palletAddBTN.isEnabled = false
                    }

                }

                Status.ERROR ->{
                    Log.i(error,"Success")
                    binding.swipeRefresh.isRefreshing = false
                    binding.palletCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

    }

    fun showAction(palletName:String,palletNo:String,palletCode: String){

        val intent = Intent(this, AddUpdatePalletDetails::class.java)
        intent.putExtra("updatedBusLocNo",selectedBusLocNo)
        intent.putExtra("updatedWHNo",selectedWareHouseNo)
        intent.putExtra("updatedRackNo",selectedRackNo)
        intent.putExtra("updatedShelveNo",selectedShelveNo)
        intent.putExtra("updatedBusLocName",busLocName)
        intent.putExtra("updatedWHName",warehouseName)
        intent.putExtra("updatedRackName",rackName)
        intent.putExtra("updatedShelveName",shelfName)
        intent.putExtra("updatedPalletName",palletName)
        intent.putExtra("updatedPalletNo",palletNo)
        intent.putExtra("updatedPalletCode",palletCode)
        intent.putExtra("updatedPalletCap",capacity)
        intent.putExtra("UpdatePalletKey",true)
        startActivity(intent)

    }

    fun showQrCode(palletCode:String, palletName:String, palletNo:String){
        val intent = Intent(this, QrCodeDetailActivity::class.java)
        intent.putExtra("palletKey",true)
        intent.putExtra("palletQrCode",palletCode)
        intent.putExtra("palletQrNo",palletNo)
        intent.putExtra("palletQrName",palletName)
        startActivity(intent)
    }

    private fun showBusLocSpinner(data:List<UserLocationResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val businessLocSpinner = binding.businessLocationSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].busLocationName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        businessLocSpinner.adapter = adapter

        businessLocSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {

                Log.i("LocBus","business Location no ${data[position].orgBusLocNo}")
                // binding.rackSpinnerCont.visible()

                selectedBusLocNo = data[position].orgBusLocNo.toString()
                busLocName = data[position].busLocationName.toString()
                binding.warehouseSpinner.adapter = returnNoAdapter()
                binding.rackSpinner.adapter = returnNoAdapter()
                binding.shelfSpinner.adapter = returnNoAdapter()

                if (isNetworkConnected(this@PalletsActivity))
                {
                    viewModel.getWarehouse("", selectedBusLocNo)
                    binding.palletAddBTN.visible()
                    binding.palletAddBTN.isEnabled = true
                }
                else
                {
                    binding.palletsRV.adapter = null
                    toast(NoInternetFound)
                    binding.palletAddBTN.gone()
                }

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun showWarehouseSpinner(data:List<GetWarehouseResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val warehouseSpinner = binding.warehouseSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].wHName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        warehouseSpinner.adapter = adapter
        warehouseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {

                selectedWareHouseNo = data[position].wHNo.toString()
                warehouseName = data[position].wHName.toString()
                whCode = data[position].wHCode.toString()
                binding.rackSpinner.adapter = returnNoAdapter()
                binding.shelfSpinner.adapter = returnNoAdapter()
                binding.palletsRV.adapter = null

                if (warehouseSpinner.selectedItem != "No Record")
                {
                    if (isNetworkConnected(this@PalletsActivity))
                    {
                        viewModel.getRack(
                            Utils.getSimpleTextBody(""),
                            Utils.getSimpleTextBody(selectedWareHouseNo),
                            Utils.getSimpleTextBody(selectedBusLocNo)
                        )
                        binding.palletAddBTN.isEnabled = true
                        binding.printIV.isEnabled = true
                        Log.i("LocBus","This is warehouse name is ${adapter?.getItemAtPosition(position)}")
                        Log.i("LocBus","This is warehouse pos is ${data[position].wHNo}")
                    }
                    else
                    {
                        binding.palletAddBTN.gone()
                        binding.palletsRV.adapter = null
                        toast(NoInternetFound)
                    }
                }
                else
                {
                    binding.palletAddBTN.isEnabled = false
                    binding.printIV.isEnabled = false
                    binding.palletsRV.adapter = null
                }




            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun showRackSpinner(data:List<GetRackResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val rackSpinner = binding.rackSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].rackName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        rackSpinner.adapter = adapter

        rackSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {

                selectedRackNo = data[position].rackNo.toString()
                rackCode = data[position].rackCode.toString()
                rackName = data[position].rackName.toString()
                binding.shelfSpinner.adapter = returnNoAdapter()
                binding.palletsRV.adapter = null

                if (rackSpinner.selectedItem != "No Record")
                {
                    if (isNetworkConnected(this@PalletsActivity))
                    {
                        viewModel.getShelf(
                            Utils.getSimpleTextBody(""),
                            Utils.getSimpleTextBody(selectedRackNo),
                            Utils.getSimpleTextBody(selectedBusLocNo)
                        )
                        binding.palletAddBTN.isEnabled = true
                        binding.printIV.isEnabled = true
                    }
                    else
                    {
                        binding.palletAddBTN.gone()
                        binding.palletsRV.adapter = null
                        toast(NoInternetFound)
                    }
                }
                else
                {
                    binding.palletAddBTN.isEnabled = false
                    binding.printIV.isEnabled = false
                    binding.palletsRV.adapter = null
                }

                Log.i("LocBus","This is rack pos ${adapter?.getItemAtPosition(position)}")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun showShelfSpinner(data:List<GetShelfResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val shelfSpinner = binding.shelfSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].shelfName
            val adapter: ArrayAdapter<String?> =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
            //setting adapter to spinner
            shelfSpinner.adapter = adapter
            shelfSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

                override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {

                    selectedShelveNo = data[position].shelfNo.toString()
                    shelfName = data[position].shelfName.toString()
                    shelfCode = data[position].shelfCode.toString()
                    binding.palletsRV.adapter = null

                    if (shelfSpinner.selectedItem != "No Record")
                    {
                        if (isNetworkConnected(this@PalletsActivity))
                        {
                            Log.i("LocBus","This is shelf pos ${adapter?.getItemAtPosition(position)}")

                            viewModel.getPallet(
                                Utils.getSimpleTextBody(""),
                                Utils.getSimpleTextBody(selectedShelveNo),
                                Utils.getSimpleTextBody(selectedBusLocNo)
                            )
                            binding.palletAddBTN.isEnabled = true
                            binding.printIV.isEnabled = true
                        }
                        else
                        {
                            binding.palletAddBTN.gone()
                            binding.palletsRV.adapter = null
                            toast(NoInternetFound)
                        }
                    }
                    else
                    {
                        binding.palletAddBTN.isEnabled = false
                        binding.printIV.isEnabled = false
                        binding.palletsRV.adapter = null
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }

    /**
     *  CLEAR ALL PREFERENCES
     */

    private fun clearPreferences(context: Context){
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
    }

    override fun onResume() {
        super.onResume()
        if (LocalPreferences.getBoolean(this, isRefreshRequired)){

            viewModel.getPallet(
                Utils.getSimpleTextBody(""),
                Utils.getSimpleTextBody(selectedShelveNo),
                Utils.getSimpleTextBody(selectedBusLocNo)
            )

        }
    }

    private fun generateQRCode(text:String) {
        val qrWriter = QRCodeWriter()
        try
        {
            val bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, 512,512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            bmpList.add(bmp)

            for (x in 0 until width)
            {
                for(y in 0 until height)
                {
                    bmp.setPixel(x,y, if (bitMatrix[x,y]) Color.BLACK else Color.WHITE)
                }
            }
            binding.qrImageView.setImageBitmap(bmp)
        }
        catch (e:Exception) { }
    }

    private fun generatePDF(){
        //handle button click
        //we need to handle runtime permission for devices with marshmallow and above
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            //system OS >= Marshmallow(6.0), check permission is enabled or not
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
                //permission was not granted, request it
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permissions, STORAGE_CODE)
            }
            else{
                //permission already granted, call savePdf() method
                savePdf()
            }
        }
        else{
            //system OS < marshmallow, call savePdf() method
            savePdf()
        }
    }

    private fun savePdf() {
        //create object of Document class

        //pdf file name
        val mFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        //pdf file path
        val mFilePath = Environment.getExternalStorageDirectory().toString() + "/" + "QrGeneratedFile" +".pdf"
        try {

            val file = File(mFilePath)
            val mDoc = Document()
            PdfWriter.getInstance(mDoc, FileOutputStream(mFilePath))
            mDoc.open()

            val pdfTable = PdfPTable(2)

            for (i in bmpList.indices)
            {
                val stream = ByteArrayOutputStream()
                bmpList[i].compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val myImg: Image = Image.getInstance(stream.toByteArray())
                myImg.scaleAbsolute(100f,100f)
                myImg.setAbsolutePosition(100f,100f)
                myImg.alignment = Element.ALIGN_CENTER

                val headingPara = Paragraph(Chunk("Pallets"))
                headingPara.alignment = Element.ALIGN_CENTER

                val palletCode = Paragraph(Chunk(palletCodeList[i]))
                palletCode.alignment = Element.ALIGN_CENTER

                val pdfcell = PdfPCell()
                with(pdfcell)
                {
                    rowspan = 2
                    addElement(headingPara)
                    addElement(myImg)
                    addElement(palletCode)
                    paddingBottom = 10f
                }
                pdfTable.addCell(pdfcell)
            }
            pdfTable.completeRow()

            mDoc.add(pdfTable)

            mDoc.close()

            openPDF(file, "QrGeneratedFile.pdf\nis saved to\n$mFilePath")

            //show file saved message with file name and path
            Toast.makeText(this, "$mFileName.pdf\nis saved to\n$mFilePath", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception)
        {
            Log.i("pdfException","${e.message}")
            //if anything goes wrong causing exception, get and show exception message
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            STORAGE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted, call savePdf() method
                    savePdf()
                }
                else{
                    //permission from popup was denied, show error message
                    Toast.makeText(this, "Permission denied...!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPDF(file: File, text: String)
    {
        val path = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName.toString() + ".provider",
            file
        )
        val pdfOpenIntent = Intent(Intent.ACTION_VIEW)
        pdfOpenIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        pdfOpenIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        pdfOpenIntent.setDataAndType(path, "application/pdf")
        try
        {
            startActivity(pdfOpenIntent)
        }
        catch (e: ActivityNotFoundException)
        {
            Log.i("openPDFException","${e.message}")
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        finish()
    }

}