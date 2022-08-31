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
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetRackResponse
import com.example.scanmate.data.response.GetShelfResponse
import com.example.scanmate.data.response.GetWarehouseResponse
import com.example.scanmate.data.response.UserLocationResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.Toast.NoInternetFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isRefreshRequired
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isSpinnerSelected
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.shelf.ShelfAdapter
import com.example.wms_scan.databinding.ActivityShelfBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ShelfActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShelfBinding
    private lateinit var shelfAdapter: ShelfAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private val textList = mutableListOf<String>()
    private lateinit var shelfList: ArrayList<GetShelfResponse>
    private var selectedBusLocNo = ""
    private var selectedWareHouseNo = ""
    private var selectedRackNo = ""
    private var busLocName = ""
    private var warehouseName = ""
    private var rackName = ""
    private var selectedShelveNo = ""
    private lateinit var bmp: Bitmap
    private val bmpList = mutableListOf<Bitmap>()
    private var STORAGE_CODE = 1001
    private var rackCode = ""
    private var shelfCapacity = ""
    private var whCode = ""
    private var shelfCode = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShelfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initListener()
        initObserver()
    }

    private fun setupUi(){

        val businessSpinner = binding.businessLocationSpinner
        val warehouseSpinner = binding.warehouseSpinner
        val rackSpinner = binding.rackSpinner

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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

        if ((businessSpinner.adapter == null) and (warehouseSpinner.adapter == null)
            and (rackSpinner.adapter != null))
        {
            viewModel.userLocation(
                Utils.getSimpleTextBody(
                    LocalPreferences.getInt(this, LocalPreferences.AppLoginPreferences.userNo).toString()
                ))
            warehouseSpinner.gone()
            binding.availableShelfTV.gone()
        }

    }

    private fun initListener(){


        binding.swipeRefresh.setOnRefreshListener {
            if (isNetworkConnected(this))
            {
                binding.shelfCont.visible()
                binding.connectionTimeout.gone()
                viewModel.userLocation(Utils.getSimpleTextBody(LocalPreferences.getInt(this, userNo).toString()))
                viewModel.getWarehouse("", selectedBusLocNo)
                viewModel.getRack(
                    Utils.getSimpleTextBody(""),
                    Utils.getSimpleTextBody(selectedWareHouseNo),
                    Utils.getSimpleTextBody(selectedBusLocNo)
                )
                viewModel.getShelf(
                    Utils.getSimpleTextBody(""),
                    Utils.getSimpleTextBody(LocalPreferences.getString(this, isSpinnerSelected).toString()),
                    Utils.getSimpleTextBody(selectedBusLocNo)
                )
            }
            else
            {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.shelfAddBTN.click {
            val businessSpinner = binding.businessLocationSpinner
            val warehouseSpinner = binding.warehouseSpinner
            val rackSpinner = binding.rackSpinner

            if (isNetworkConnected(this))
            {
                if ((businessSpinner.adapter != null) and (warehouseSpinner.adapter != null)
                    and (rackSpinner.adapter != null))
                {
                    val intent = Intent(this, AddUpdateShelfDetails::class.java)
                    intent.putExtra("addBusLocNo",selectedBusLocNo)
                    intent.putExtra("addWHNo",selectedWareHouseNo)
                    intent.putExtra("addRackNo",selectedRackNo)
                    intent.putExtra("addShelfNo",selectedShelveNo)
                    intent.putExtra("addBusLocName",busLocName)
                    intent.putExtra("addWHName",warehouseName)
                    intent.putExtra("addRackName",rackName)
                    intent.putExtra("shelfCap",shelfCapacity)
                    intent.putExtra("shelfCode",shelfCode)
                    intent.putExtra("AddShelfKey",true)
                    startActivity(intent)
                }
                else
                {
                    toast("No connection found")
                    businessSpinner.gone()
                    warehouseSpinner.gone()
                    rackSpinner.gone()
                    binding.availableShelfTV.gone()
                    binding.swipeRefresh.isRefreshing = false
                    binding.shelfRV.gone()
                    binding.shelfAddBTN.gone()
                }
            }
            else
            {

            }
        }

        binding.backBtn.click {
            onBackPressed()
        }

    }

    fun showAction(shelfName:String,shelfNo:String,shelfCode:String,palletCapacity:String){
        if (isNetworkConnected(this))
        {
            val intent = Intent(this, AddUpdateShelfDetails::class.java)

            intent.putExtra("updateBusLocNo",selectedBusLocNo)
            intent.putExtra("updateWHNo",selectedWareHouseNo)
            intent.putExtra("updateRackNo",selectedRackNo)
            intent.putExtra("updateShelfNo",shelfNo)
            intent.putExtra("updateBusinessLocName",busLocName)
            intent.putExtra("updateWHName",warehouseName)
            intent.putExtra("updateRackName",rackName)
            intent.putExtra("updateShelfName",shelfName)
            intent.putExtra("updatedShelfCode",shelfCode)
            intent.putExtra("updatedPalletCapacity",palletCapacity)
            intent.putExtra("UpdateShelfKey",true)
            startActivity(intent)
        }
        else
        {

        }
    }

    fun showQrCode(shelfCode:String, shelfName: String, shelfNo: String){
        val intent = Intent(this, QrCodeDetailActivity::class.java)
        intent.putExtra("shelfKey",true)
        intent.putExtra("shelfQrCode",shelfCode)
        intent.putExtra("shelfQrNo",shelfNo)
        intent.putExtra("shelfQrName",shelfName)
        startActivity(intent)
    }

    private fun initObserver(){

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
                    dialog.setCanceledOnTouchOutside(true);
                }
                Status.SUCCESS ->{
                    if (isNetworkConnected(this)){
                        binding.businessLocationSpinner.visible()
                        binding.businessSpinnerCont.visible()
                        try
                        {
                            if(it.data?.get(0)?.status == true)
                            {
                                dialog.dismiss()
                                showBusLocSpinner(it.data)
                            }

                            else
                            {
                                binding.shelfRV.adapter = null
                                binding.warehouseSpinnerCont.gone()
                                binding.rackSpinnerCont.gone()
                                binding.shelfAddBTN.gone()
                                binding.printIV.click {
                                    toast("Nothing to print!")
                                }
                            }
                        }
                        catch (e:Exception) { }
                    }
                    else
                    {
                        binding.shelfRV.adapter = null
                        toast("Please select any value")
                        binding.shelfAddBTN.isEnabled = false
                        binding.businessLocationSpinner.gone()
                    }
                }
                Status.ERROR ->{
                    dialog.dismiss()
                    binding.swipeRefresh.isRefreshing = false
                    binding.shelfCont.gone()
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
                    if (isNetworkConnected(this)){
                        binding.warehouseSpinner.visible()
                        binding.warehouseSpinnerCont.visible()
                        try {
                            if(it.data?.get(0)?.status == true)
                            {
                                it.data[0].wHName?.let { it1 -> Log.i("warehouseResponse", it1) }
                                binding.warehouseSpinnerCont.visible()
                                showWarehouseSpinner(it.data)
                            }
                            else
                            {
                                binding.shelfRV.adapter = null
                                binding.warehouseSpinner.onItemSelectedListener = null
                                binding.rackSpinner.onItemSelectedListener = null
                                binding.shelfAddBTN.gone()
                                binding.warehouseSpinnerCont.gone()
                                binding.rackSpinnerCont.gone()
                                binding.printIV.click {
                                    toast("Nothing to print!")
                                }
                            }
                        }

                        catch(e:Exception){
                            Log.i("rackAdapter","${e.message}")
                            Log.i("rackAdapter","${e.stackTrace}")
                        }
                    }
                    else
                    {
                        toast("Please select any value")
                        binding.shelfAddBTN.isEnabled = false
                        binding.warehouseSpinnerCont.gone()
                    }

                    //warehouseAdapter.addItems(list)
                }
                Status.ERROR ->{
                    dialog.dismiss()
                    binding.swipeRefresh.isRefreshing = false
                    binding.shelfCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

        /**
         *      GET RACK OBSERVER
         */

        viewModel.getRack.observe(this, Observer{
            when(it.status){

                Status.LOADING ->{}

                Status.SUCCESS ->{

                    binding.rackSpinner.visible()
                    binding.rackSpinnerCont.visible()

                    if (isNetworkConnected(this)){
                        try
                        {
                            if(it.data?.get(0)?.status == true)
                            {
                                showRackSpinner(it.data)
                                binding.shelfAddBTN.visible()
                                binding.rackSpinnerCont.visible()
                                binding.rackSpinner.visible()
                                binding.warehouseSpinnerCont.visible()
                            }
                            else
                            {
                                binding.shelfRV.adapter = null
                                intent.removeExtra("key")
                                binding.shelfAddBTN.gone()
                                binding.rackSpinnerCont.gone()
                                binding.rackSpinner.gone()
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
                        toast("Please select any value")
                        binding.shelfAddBTN.isEnabled = false
                        binding.rackSpinnerCont.gone()
                    }
                }

                Status.ERROR ->{
                    dialog.dismiss()
                    binding.swipeRefresh.isRefreshing = false
                    binding.shelfCont.gone()
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
                    binding.swipeRefresh.isRefreshing = true
                }
                Status.SUCCESS ->{
                    binding.availableShelfTV.visible()
                    if (isNetworkConnected(this))
                    {
                        binding.swipeRefresh.isRefreshing = false
                        dialog.setCanceledOnTouchOutside(true);
                        it.let{
                            if (isNetworkConnected(this)){
                                LocalPreferences.put(this, isRefreshRequired, true)
                                try {
                                    if(it.data?.get(0)?.status == true) {

                                        shelfList = ArrayList()
                                        shelfCode = it.data[0].shelfCode.toString()
                                        Log.i("shelfcode",shelfCode)
                                        shelfCapacity = it.data[0].capacity.toString()
                                        shelfAdapter = ShelfAdapter(this,  it.data as ArrayList<GetShelfResponse>)

                                        bmpList.clear()
                                        textList.clear()
                                        binding.printIV.click { btn ->

                                            if (isNetworkConnected(this))
                                            {
                                                for (i in it.data.indices)
                                                {
                                                    generateQRCode("${it.data[i].shelfCode}")
                                                    Log.i("ShelfName",it.data[i].shelfName.toString())
                                                    textList.add("${it.data[i].shelfName}")
                                                    Log.i("shelfArrayList","$textList")
                                                }
                                                generatePDF()
                                            }
                                            else
                                            {

                                            }


                                        }
                                        binding.shelfRV.apply {
                                            adapter = shelfAdapter
                                            layoutManager = LinearLayoutManager(this@ShelfActivity)
                                        }
                                    }
                                    else
                                    {
                                        binding.printIV.click {
                                            toast("Nothing to print!")
                                        }
                                        binding.shelfRV.adapter = null

                                    }
                                }
                                catch (e:Exception){
                                    Log.i("shelfException","${e.message}")
                                }
                            }
                            else
                            {
                                binding.shelfRV.adapter = null
                            }
                        }
                    }
                    else
                    {
                        toast("Please select any value")
                        binding.shelfAddBTN.isEnabled = false
                    }
                }

                Status.ERROR ->
                {
                    binding.shelfCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })

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

                selectedBusLocNo = data[position].orgBusLocNo.toString()
                busLocName = data[position].busLocationName.toString()
                binding.shelfRV.adapter = null
                binding.warehouseSpinner.adapter = returnNoAdapter()
                binding.rackSpinner.adapter = returnNoAdapter()

                if (businessLocSpinner.selectedItem != "No Record")
                {
                    if (isNetworkConnected(this@ShelfActivity))
                    {
                        Log.i("LocBus","business Location no ${data[position].orgBusLocNo}")
                        // binding.rackSpinnerCont.visible()
                        viewModel.getWarehouse("", selectedBusLocNo)
                        binding.shelfAddBTN.visible()
                        binding.shelfAddBTN.isEnabled =  true
                        binding.printIV.isEnabled = true
                    }
                    else
                    {
                        binding.shelfRV.adapter = null
                        binding.shelfAddBTN.gone()
                        toast(NoInternetFound)
                    }
                }
                else
                {
                    toast("No record")
                    binding.shelfAddBTN.isEnabled =  false
                    binding.printIV.isEnabled = false
                }

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toast("Please select any value")
                binding.shelfAddBTN.isEnabled = false
            }
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

                binding.rackSpinner.adapter = returnNoAdapter()
                selectedWareHouseNo = data[position].wHNo.toString()
                warehouseName = data[position].wHName.toString()
                whCode = data[position].wHCode.toString()

                if (warehouseSpinner.selectedItem != "No Record")
                {
                    if (isNetworkConnected(this@ShelfActivity))
                    {

                        viewModel.getRack(
                            Utils.getSimpleTextBody(""),
                            Utils.getSimpleTextBody(selectedWareHouseNo),
                            Utils.getSimpleTextBody(selectedBusLocNo)
                        )
                        Log.i("LocBus","This is warehouse name is ${adapter?.getItemAtPosition(position)}")
                        Log.i("LocBus","This is warehouse pos is ${data[position].wHNo}")
                        binding.shelfAddBTN.isEnabled =  true
                        binding.printIV.isEnabled = true
                    }

                    else
                    {
                        binding.shelfRV.adapter = null
                        toast(NoInternetFound)
                        binding.shelfAddBTN.gone()
                    }
                }
                else
                {
                    binding.shelfAddBTN.isEnabled = false
                    binding.printIV.isEnabled = false
                }

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toast("Please select any value")
                binding.shelfAddBTN.isEnabled = false
            }
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
                rackName = data[position].rackName.toString()
                rackCode = data[position].rackCode.toString()
                binding.shelfRV.adapter = null

                if (rackSpinner.selectedItem != "No Record")
                {
                    if (isNetworkConnected(this@ShelfActivity))
                    {
                        viewModel.getShelf(
                            Utils.getSimpleTextBody(""),
                            Utils.getSimpleTextBody(selectedRackNo),
                            Utils.getSimpleTextBody(selectedBusLocNo)
                        )
                        binding.shelfAddBTN.isEnabled = true
                        binding.printIV.isEnabled = true
                        Log.i("LocBus","This is rack pos ${adapter?.getItemAtPosition(position)}")
                    }
                    else
                    {
                        binding.shelfRV.adapter = null
                        toast(NoInternetFound)
                        val selectedRack = data[position].rackNo.toString()
                        LocalPreferences.put(this@ShelfActivity,isSpinnerSelected,selectedRack)

                    }
                }
                else
                {
                    binding.shelfAddBTN.isEnabled = false
                    binding.printIV.isEnabled = false
                }

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toast("Please select any value")
                binding.shelfAddBTN.isEnabled = false
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (LocalPreferences.getBoolean(this, isRefreshRequired)){

            viewModel.getShelf(
                Utils.getSimpleTextBody(""),
                Utils.getSimpleTextBody(selectedRackNo),
                Utils.getSimpleTextBody(selectedBusLocNo)
            )
        }
    }

    private fun clearPreferences(context: Context){
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        gotoActivity(LoginActivity::class.java)
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

                val headingPara = Paragraph(Chunk("Shelf"))
                headingPara.alignment = Element.ALIGN_CENTER

                val shelfcode = Paragraph(Chunk(textList[i]))
                shelfcode.alignment = Element.ALIGN_CENTER

                val pdfcell = PdfPCell()
                with(pdfcell)
                {
                    rowspan = 2
                    addElement(headingPara)
                    addElement(myImg)
                    addElement(shelfcode)
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

            Log.i("1 pdfException","${e.message}")
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
            Log.i("openPDFException 1","${e.message}")
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        finish()
    }

}