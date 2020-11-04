package com.movehome.armoving

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.movehome.armoving.model.CardData
import com.movehome.armoving.model.CardListData
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import com.google.ar.sceneform.rendering.Color as arColor
import kotlin.math.pow
import kotlin.math.sqrt


class Measurement : AppCompatActivity(), Scene.OnUpdateListener {
    private val MIN_OPENGL_VERSION = 3.0
    private val TAG: String = Measurement::class.java.getSimpleName()

    private var arFragment: ArFragment? = null

    private var distanceModeTextView: TextView? = null
    private lateinit var pointTextView: TextView

    private lateinit var arrow1UpLinearLayout: LinearLayout
    private lateinit var arrow1DownLinearLayout: LinearLayout
    private lateinit var arrow1UpView: ImageView
    private lateinit var arrow1DownView: ImageView
    private lateinit var arrow1UpRenderable: Renderable
    private lateinit var arrow1DownRenderable: Renderable

    private lateinit var arrow10UpLinearLayout: LinearLayout
    private lateinit var arrow10DownLinearLayout: LinearLayout
    private lateinit var arrow10UpView: ImageView
    private lateinit var arrow10DownView: ImageView
    private lateinit var arrow10UpRenderable: Renderable
    private lateinit var arrow10DownRenderable: Renderable

    private lateinit var multipleDistanceTableLayout: TableLayout

    private var cubeRenderable: ModelRenderable? = null
    private var distanceCardViewRenderable: ViewRenderable? = null

    private lateinit var distanceModeSpinner: Spinner
    private val distanceModeArrayList = ArrayList<String>()
    private var distanceMode: String = ""

    private val placedAnchors = ArrayList<Anchor>()
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val midAnchors: MutableMap<String, Anchor> = mutableMapOf()
    private val midAnchorNodes: MutableMap<String, AnchorNode> = mutableMapOf()
    private val fromGroundNodes = ArrayList<List<Node>>()

    private val multipleDistances = Array(Constants.maxNumMultiplePoints,
        {Array<TextView?>(Constants.maxNumMultiplePoints){null} })
    private lateinit var initCM: String

    private lateinit var clearButton: Button

    var imageView: ImageView? = null


    var data: Bundle? = null
    var items: ArrayList<CardListData>? = null
    var position: Int? = null
    var item: CardListData? = null
    var myDir: String? = null
    var imageFloderPath: String? = null
    var filepath: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        Constants.maxNumMultiplePoints = 4;

        data = intent.extras
        items = data?.getParcelableArrayList<CardListData>("items")!!
        position = data?.getInt("position")
        item = items!![position!!]
        Log.d(TAG, "item("+position+").data("+ item!!.data.size+")")

        myDir = this.externalCacheDir?.path.toString()
        imageFloderPath = "/image/"+ item!!.title
        createDir()

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_measurement)
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment?


        val button = findViewById<Button>(R.id.doneButton)
        button.setOnClickListener {
            takePhoto()

            var volume: Float = 1f
            for (i in 0..placedAnchorNodes.size-2){
                val j = i+1
                Log.d(TAG, "volume("+volume+") * "+multipleDistances[i][j]!!.text.toString())
                volume = volume * multipleDistances[i][j]!!.text.toString().toFloat()
            }
            Log.d(TAG, "volume = "+volume)


            item!!.data.add(CardData(filepath, item!!.title+" "+item!!.data.size, volume.toString()))
            Log.d(TAG, "item.data("+ item!!.data.size+"): "+item!!.data[item!!.data.size-1])

            backToMain()
        }

        imageView = findViewById<ImageView>(R.id.imageView_test)
        imageView!!.setOnClickListener(View.OnClickListener { takePhoto() })


        val distanceModeArray = resources.getStringArray(R.array.distance_mode)
        distanceModeArray.map{it-> distanceModeArrayList.add(it) }
        distanceModeTextView = findViewById(R.id.distance_view)
        multipleDistanceTableLayout = findViewById(R.id.multiple_distance_table)

        initCM = resources.getString(R.string.initCM)

        configureSpinner()
        initArrowView()
        initRenderable()
        clearButton()
        initDistanceTable()


        arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (cubeRenderable == null || distanceCardViewRenderable == null) return@setOnTapArPlaneListener
            // Creating Anchor.
            when (distanceMode){
                distanceModeArrayList[0] -> {
                    clearAllAnchors()
                    placeAnchor(hitResult, distanceCardViewRenderable!!)
                }
                distanceModeArrayList[1] -> {
                    tapDistanceOf2Points(hitResult)
                }
                distanceModeArrayList[2] -> {
                    tapDistanceOfMultiplePoints(hitResult)
                }
                distanceModeArrayList[3] -> {
                    tapDistanceFromGround(hitResult)
                }
                else -> {
                    clearAllAnchors()
                    placeAnchor(hitResult, distanceCardViewRenderable!!)
                }
            }
        }
    }

    private fun initDistanceTable(){ //給多點的距離表
        Log.d(TAG, "initDistanceTable")
        for (i in 0 until Constants.maxNumMultiplePoints+1){
            val tableRow = TableRow(this) //新的一行
            multipleDistanceTableLayout.addView(tableRow, multipleDistanceTableLayout.width, Constants.multipleDistanceTableHeight / (Constants.maxNumMultiplePoints + 1))
            for (j in 0 until Constants.maxNumMultiplePoints+1){
                val textView = TextView(this)
                textView.setTextColor(Color.WHITE)
                if (i==0){
                    if (j==0) textView.setText("cm")
                    else textView.setText((j-1).toString()) //上方的點編號
                }
                else{
                    if (j==0) textView.setText((i-1).toString()) //左側的點編號
                    else if(i==j){ //對角線，不顯示數字
                        textView.setText("-")
                        multipleDistances[i-1][j-1] = textView
                    }
                    else{ //初始化數字
                        textView.setText(initCM)
                        multipleDistances[i-1][j-1] = textView
                    }
                }
                tableRow.addView(textView, tableRow.layoutParams.width / (Constants.maxNumMultiplePoints + 1), tableRow.layoutParams.height)
            }
        }
        multipleDistanceTableLayout.visibility = View.GONE //隱藏表格
    }

    private fun initArrowView(){ //給Gound模式用的
        Log.d(TAG, "initArrowView")
        arrow1UpLinearLayout = LinearLayout(this)
        arrow1UpLinearLayout.orientation = LinearLayout.VERTICAL
        arrow1UpLinearLayout.gravity = Gravity.CENTER
        arrow1UpView = ImageView(this)
        arrow1UpView.setImageResource(R.drawable.arrow_1up)
        arrow1UpLinearLayout.addView(arrow1UpView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)

        arrow1DownLinearLayout = LinearLayout(this)
        arrow1DownLinearLayout.orientation = LinearLayout.VERTICAL
        arrow1DownLinearLayout.gravity = Gravity.CENTER
        arrow1DownView = ImageView(this)
        arrow1DownView.setImageResource(R.drawable.arrow_1down)
        arrow1DownLinearLayout.addView(arrow1DownView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)

        arrow10UpLinearLayout = LinearLayout(this)
        arrow10UpLinearLayout.orientation = LinearLayout.VERTICAL
        arrow10UpLinearLayout.gravity = Gravity.CENTER
        arrow10UpView = ImageView(this)
        arrow10UpView.setImageResource(R.drawable.arrow_10up)
        arrow10UpLinearLayout.addView(arrow10UpView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)

        arrow10DownLinearLayout = LinearLayout(this)
        arrow10DownLinearLayout.orientation = LinearLayout.VERTICAL
        arrow10DownLinearLayout.gravity = Gravity.CENTER
        arrow10DownView = ImageView(this)
        arrow10DownView.setImageResource(R.drawable.arrow_10down)
        arrow10DownLinearLayout.addView(arrow10DownView,
            Constants.arrowViewSize,
            Constants.arrowViewSize)
    }

    private fun initRenderable() {
        Log.d(TAG, "initRenderable")
        //球型紅色圓點
        MaterialFactory.makeTransparentWithColor(this, arColor(Color.RED))
            .thenAccept { material: Material? ->
                cubeRenderable = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material)
                cubeRenderable!!.setShadowCaster(false)
                cubeRenderable!!.setShadowReceiver(false)
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("1. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        //普通的，可以顯示文字
        ViewRenderable
            .builder()
            .setView(this, R.layout.distance_text_layout)
            .build()
            .thenAccept{
                distanceCardViewRenderable = it
                distanceCardViewRenderable!!.isShadowCaster = false
                distanceCardViewRenderable!!.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("2. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        //以下都是Gound模式用的
        ViewRenderable
            .builder()
            .setView(this, arrow1UpLinearLayout)
            .build()
            .thenAccept{
                arrow1UpRenderable = it
                arrow1UpRenderable.isShadowCaster = false
                arrow1UpRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("3. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow1DownLinearLayout)
            .build()
            .thenAccept{
                arrow1DownRenderable = it
                arrow1DownRenderable.isShadowCaster = false
                arrow1DownRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("4. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow10UpLinearLayout)
            .build()
            .thenAccept{
                arrow10UpRenderable = it
                arrow10UpRenderable.isShadowCaster = false
                arrow10UpRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("5. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }

        ViewRenderable
            .builder()
            .setView(this, arrow10DownLinearLayout)
            .build()
            .thenAccept{
                arrow10DownRenderable = it
                arrow10DownRenderable.isShadowCaster = false
                arrow10DownRenderable.isShadowReceiver = false
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("6. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun configureSpinner(){ //選擇模式
        Log.d(TAG, "configureSpinner")
        distanceModeSpinner = findViewById(R.id.distance_mode_spinner)
        val distanceModeAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            distanceModeArrayList //顯示的模式陣列
        )
        distanceModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        distanceModeSpinner.adapter = distanceModeAdapter
        distanceModeSpinner.setSelection(1) //選擇「兩點(1)」模式  // 「多點(2)」模式

        //選擇另一個模式
        distanceModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                distanceMode = spinnerParent.selectedItem as String
                clearAllAnchors() //清除所有點
                setMode() //顯示模式文字
//                toastMode() //Toast模式文字
                if (distanceMode == distanceModeArrayList[2]){ //多點模式
                    val layoutParams = multipleDistanceTableLayout.layoutParams
                    layoutParams.height = Constants.multipleDistanceTableHeight
                    multipleDistanceTableLayout.layoutParams = layoutParams
                    initDistanceTable() //顯示距離表格
                }
                else{ //不是多點模式，高度設為0
                    val layoutParams = multipleDistanceTableLayout.layoutParams
                    layoutParams.height = 0
                    multipleDistanceTableLayout.layoutParams = layoutParams
                }
                Log.i(TAG, "Selected arcore focus on ${distanceMode}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                clearAllAnchors()
                setMode()
//                toastMode()
            }
        }
    }

    private fun setMode(){ //顯示模式文字
        Log.d(TAG, "setMode")
        distanceModeTextView!!.text = distanceMode
    }

    private fun clearButton(){ //點擊「清除」按鈕
        Log.d(TAG, "clearButton")
        clearButton = findViewById(R.id.clearButton)
        clearButton.setOnClickListener({ clearAllAnchors() }) //清除所有圓點
    }

    private fun clearAllAnchors(){ //清除所有圓點
        Log.d(TAG, "clearAllAnchors")

        placedAnchors.clear()
        for (anchorNode in placedAnchorNodes){
            arFragment!!.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor!!.detach()
            anchorNode.setParent(null)
        }
        placedAnchorNodes.clear()

        midAnchors.clear()
        for ((k,anchorNode) in midAnchorNodes){
            arFragment!!.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor!!.detach()
            anchorNode.setParent(null)
        }
        midAnchorNodes.clear()

        //初始化距離表格
        for (i in 0 until Constants.maxNumMultiplePoints){
            for (j in 0 until Constants.maxNumMultiplePoints){
                if (multipleDistances[i][j] != null){
                    multipleDistances[i][j]!!.setText(if(i==j) "-" else initCM)
                }
            }
        }

        fromGroundNodes.clear()
    }

    private fun tapDistanceFromGround(hitResult: HitResult){
        Log.d(TAG, "tapDistanceFromGround")
        clearAllAnchors()
        val anchor = hitResult.createAnchor()
        placedAnchors.add(anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val transformableNode = TransformableNode(arFragment!!.transformationSystem)
            .apply{
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true //If you want to move to the better position.
                this.renderable = renderable
                setParent(anchorNode)
            }

        val node = Node()
            .apply {
                setParent(transformableNode)
                this.worldPosition = Vector3(
                    anchorNode.worldPosition.x,
                    anchorNode.worldPosition.y,
                    anchorNode.worldPosition.z)
                this.renderable = distanceCardViewRenderable
            }

        val arrow1UpNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y+0.1f,
                    node.worldPosition.z
                )
                this.renderable = arrow1UpRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y+0.01f,
                        node.worldPosition.z
                    )
                }
            }

        val arrow1DownNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y-0.08f,
                    node.worldPosition.z
                )
                this.renderable = arrow1DownRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y-0.01f,
                        node.worldPosition.z
                    )
                }
            }

        val arrow10UpNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y+0.18f,
                    node.worldPosition.z
                )
                this.renderable = arrow10UpRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y+0.1f,
                        node.worldPosition.z
                    )
                }
            }

        val arrow10DownNode = Node()
            .apply {
                setParent(node)
                this.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y-0.167f,
                    node.worldPosition.z
                )
                this.renderable = arrow10DownRenderable
                this.setOnTapListener { hitTestResult, motionEvent ->
                    node.worldPosition = Vector3(
                        node.worldPosition.x,
                        node.worldPosition.y-0.1f,
                        node.worldPosition.z
                    )
                }
            }

        fromGroundNodes.add(listOf(node, arrow1UpNode, arrow1DownNode, arrow10UpNode, arrow10DownNode))

        arFragment!!.arSceneView.scene.addOnUpdateListener(this)
        arFragment!!.arSceneView.scene.addChild(anchorNode)
        transformableNode.select()
    }

    private fun placeAnchor(hitResult: HitResult, renderable: Renderable){ //放置圓點
        Log.d(TAG, "placeAnchor")
        val anchor = hitResult.createAnchor() //要放的位置(一個圓型的範圍)
        placedAnchors.add(anchor)

        //在anchor的位置放上node
        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        placedAnchorNodes.add(anchorNode) //儲存圓點

        val node = TransformableNode(arFragment!!.transformationSystem)
            .apply{
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true
                this.renderable = renderable //圓點的造型
                setParent(anchorNode) //顯示圓點的造型
            }

        arFragment!!.arSceneView.scene.addOnUpdateListener(this) //更新頁面(計算距離)
        arFragment!!.arSceneView.scene.addChild(anchorNode) //前面anchorNode已經setParent了，不知道為什麼這裡還要再一次
        node.select() //和上面那行總是一對出現
    }


    private fun tapDistanceOf2Points(hitResult: HitResult){
        Log.d(TAG, "tapDistanceOf2Points")

        //If you have 0 anchor, place one.
        if (placedAnchorNodes.size == 0){ //儲存的圓點數量為0
            placeAnchor(hitResult, cubeRenderable!!) //放上紅色圓點
        }
        else if (placedAnchorNodes.size >= 1 && placedAnchorNodes.size < Constants.maxNumMultiplePoints){
            //If you already have 1, place another one with distance pop in the middle.
            placeAnchor(hitResult, cubeRenderable!!) //放上紅色圓點

            //兩個圓點中間的位置
            val midPosition = floatArrayOf(
                (placedAnchorNodes[placedAnchorNodes.size-2].worldPosition.x + placedAnchorNodes[placedAnchorNodes.size-1].worldPosition.x) / 2,
                (placedAnchorNodes[placedAnchorNodes.size-2].worldPosition.y + placedAnchorNodes[placedAnchorNodes.size-1].worldPosition.y) / 2,
                (placedAnchorNodes[placedAnchorNodes.size-2].worldPosition.z + placedAnchorNodes[placedAnchorNodes.size-1].worldPosition.z) / 2)
            val quaternion = floatArrayOf(0.0f,0.0f,0.0f,0.0f)
            val pose = Pose(midPosition, quaternion)

            //放上中間的圖形
            ViewRenderable
                .builder()
                .setView(this, R.layout.distance_text_layout)
                .build()
                .thenAccept{
                    it.isShadowReceiver = false
                    it.isShadowCaster = false
                    measureDistanceOf2Points(it)
                    placeMidAnchor(pose, it, arrayOf(placedAnchorNodes.size-2, placedAnchorNodes.size-1))
                }
            }
        else {
            clearAllAnchors()
            placeAnchor(hitResult, cubeRenderable!!)
        }
    }

    private fun placeMidAnchor(pose: Pose, renderable: Renderable, between: Array<Int>){ //放上中間的圖形
        Log.d(TAG, "placeMidAnchor")
        val midKey = "${between[0]}_${between[1]}" //表示後面的位置是在哪兩個圓球之間的key

        val anchor = arFragment!!.arSceneView.session!!.createAnchor(pose) //要放的位置(在pose的位置上)
        midAnchors.put(midKey, anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        midAnchorNodes.put(midKey, anchorNode)

        val node = TransformableNode(arFragment!!.transformationSystem)
            .apply{
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true
                this.renderable = renderable
                setParent(anchorNode)
            }
        arFragment!!.arSceneView.scene.addOnUpdateListener(this)
        arFragment!!.arSceneView.scene.addChild(anchorNode)
    }

    private fun tapDistanceOfMultiplePoints(hitResult: HitResult){ //多點模式
        Log.d(TAG, "tapDistanceOfMultiplePoints")
        if (placedAnchorNodes.size >= Constants.maxNumMultiplePoints){ //超過上限就把點清掉
            clearAllAnchors()
        }
        ViewRenderable
            .builder()
            .setView(this, R.layout.point_text_layout) //多點的圓點是普通的點(和距離的普通點不一樣)
            .build()
            .thenAccept{
                it.isShadowReceiver = false
                it.isShadowCaster = false
                pointTextView = it.getView() as TextView
                pointTextView.setText(placedAnchors.size.toString())
                placeAnchor(hitResult, it) //把這裡的普通點放上去
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("7. "+it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
        Log.i(TAG, "Number of anchors: ${placedAnchorNodes.size}")
    }

    @SuppressLint("SetTextI18n")
    override fun onUpdate(frameTime: FrameTime) {
        Log.d(TAG, "onUpdate")
        when(distanceMode) {
            distanceModeArrayList[0] -> {
                measureDistanceFromCamera()
            }
            distanceModeArrayList[1] -> {
                measureDistanceOf2Points(distanceCardViewRenderable!!) //兩個點之間的普通點顯示距離
            }
            distanceModeArrayList[2] -> {
                measureMultipleDistances() //距離表格
            }
            distanceModeArrayList[3] -> {
                measureDistanceFromGround()
            }
            else -> {
                measureDistanceFromCamera()
            }
        }
    }

    private fun measureDistanceFromGround(){
        Log.d(TAG, "measureDistanceFromGround")
        if (fromGroundNodes.size == 0) return
        for (node in fromGroundNodes){
            val textView = (distanceCardViewRenderable!!.view as LinearLayout)
                .findViewById<TextView>(R.id.distanceCard)
            val distanceCM = changeUnit(node[0].worldPosition.y + 1.0f, "cm")
            textView.text = "%.0f".format(distanceCM) + " cm"
        }
    }

    private fun measureDistanceFromCamera(){
        Log.d(TAG, "measureDistanceFromCamera")
        val frame = arFragment!!.arSceneView.arFrame
        if (placedAnchorNodes.size >= 1) {
            val distanceMeter = calculateDistance(
                placedAnchorNodes[0].worldPosition,
                frame!!.camera.pose)
            measureDistanceOf2Points(distanceMeter, distanceCardViewRenderable!!)
        }
    }

    private fun measureDistanceOf2Points(renderable: ViewRenderable){
        Log.d(TAG, "measureDistanceOf2Points")
//        if (placedAnchorNodes.size == 2) {
//            //計算兩個點之間的距離
//            val distanceMeter = calculateDistance(
//                placedAnchorNodes[0].worldPosition,
//                placedAnchorNodes[1].worldPosition)
//            measureDistanceOf2Points(distanceMeter) //顯示距離文字
//        }
        if (placedAnchorNodes.size >= 2) {
            //計算兩個點之間的距離
            val distanceMeter = calculateDistance(
                placedAnchorNodes[placedAnchorNodes.size-2].worldPosition,
                placedAnchorNodes[placedAnchorNodes.size-1].worldPosition)
            val distanceCM = changeUnit(distanceMeter, "cm")
            val distanceCMFloor = "%.2f".format(distanceCM)
            multipleDistances[placedAnchorNodes.size-2][placedAnchorNodes.size-1]!!.setText(distanceCMFloor)
            measureDistanceOf2Points(distanceMeter, renderable) //顯示距離文字
        }
    }

    private fun measureDistanceOf2Points(distanceMeter: Float, renderable: ViewRenderable){ //將普通點 加上距離文字
        Log.d(TAG, "measureDistanceOf2Points(distanceMeter)")
        val distanceTextCM = makeDistanceTextWithCM(distanceMeter)
        val textView = (renderable.view as LinearLayout).findViewById<TextView>(R.id.distanceCard)
        textView.text = distanceTextCM
        Log.d(TAG, "distance: ${distanceTextCM}")
    }

    private fun measureMultipleDistances(){ //顯示距離表格
        Log.d(TAG, "measureMultipleDistances")
        if (placedAnchorNodes.size > 1){
            for (i in 0 until placedAnchorNodes.size){
                for (j in i+1 until placedAnchorNodes.size){ //只處理對角線以上的值
                    val distanceMeter = calculateDistance(
                        placedAnchorNodes[i].worldPosition,
                        placedAnchorNodes[j].worldPosition)
                    val distanceCM = changeUnit(distanceMeter, "cm")
                    val distanceCMFloor = "%.2f".format(distanceCM)
                    multipleDistances[i][j]!!.setText(distanceCMFloor)
                    multipleDistances[j][i]!!.setText(distanceCMFloor) //對角線另一半的值
                }
            }
        }
    }

    private fun makeDistanceTextWithCM(distanceMeter: Float): String{
        Log.d(TAG, "makeDistanceTextWithCM")
        val distanceCM = changeUnit(distanceMeter, "cm")
        val distanceCMFloor = "%.2f".format(distanceCM)
        return "${distanceCMFloor} cm"
    }

    private fun calculateDistance(x: Float, y: Float, z: Float): Float{
        Log.d(TAG, "calculateDistance")
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    private fun calculateDistance(objectPose0: Pose, objectPose1: Pose): Float{
        Log.d(TAG, "calculateDistance(objectPose: Pose)")
        return calculateDistance(
            objectPose0.tx() - objectPose1.tx(),
            objectPose0.ty() - objectPose1.ty(),
            objectPose0.tz() - objectPose1.tz())
    }


    private fun calculateDistance(objectPose0: Vector3, objectPose1: Pose): Float{
        Log.d(TAG, "calculateDistance(objectPose: Vector3, Pose)")
        return calculateDistance(
            objectPose0.x - objectPose1.tx(),
            objectPose0.y - objectPose1.ty(),
            objectPose0.z - objectPose1.tz()
        )
    }

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Vector3): Float{
        Log.d(TAG, "calculateDistance(objectPose: Vector3)")
        return calculateDistance(
            objectPose0.x - objectPose1.x,
            objectPose0.y - objectPose1.y,
            objectPose0.z - objectPose1.z
        )
    }

    private fun changeUnit(distanceMeter: Float, unit: String): Float{
        Log.d(TAG, "changeUnit")
        return when(unit){
            "cm" -> distanceMeter * 100
            "mm" -> distanceMeter * 1000
            else -> distanceMeter
        }
    }

    private fun toastMode(){
        Log.d(TAG, "toastMode")
        Toast.makeText(this@Measurement,
            when(distanceMode){
                distanceModeArrayList[0] -> "Find plane and tap somewhere"
                distanceModeArrayList[1] -> "Find plane and tap 2 points"
                distanceModeArrayList[2] -> "Find plane and tap multiple points"
                distanceModeArrayList[3] -> "Find plane and tap point"
                else -> "???"
            },
            Toast.LENGTH_LONG)
            .show()
    }


    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        Log.d(TAG, "checkIsSupportedDeviceOrFinish")
        val openGlVersionString =
            (Objects.requireNonNull(activity
                .getSystemService(Context.ACTIVITY_SERVICE)) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES ${MIN_OPENGL_VERSION} later")
            Toast.makeText(activity,
                "Sceneform requires OpenGL ES ${MIN_OPENGL_VERSION} or later",
                Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun takePhoto() {
        filepath = generateFilename()
        val view = arFragment!!.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread: HandlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request(view, bitmap, { copyResult: Int ->
                runOnUiThread{ imageView!!.setImageBitmap(bitmap) }
                saveBitmapToDisk(bitmap, filepath!!)
                handlerThread.quitSafely()
            }, Handler(handlerThread.looper))
        }
    }

    private fun createDir() { //產生路徑(包括圖片地區資料夾)
        val imageFloder = File(myDir+imageFloderPath)
        if(!imageFloder.isDirectory) imageFloder.mkdirs()
    }

    private fun generateFilename(): String { //圖片名稱(路徑+地點_編號.jpg)
        val filename = "/"+item!!.title+"_"+item!!.data.size+".jpg"
        return myDir+imageFloderPath+filename
    }

    private fun saveBitmapToDisk(bitmap: Bitmap, filepath: String) { //將圖片儲存至手機
        val file = File(filepath)
        try {
            Log.d(TAG, "file path: "+ filepath)
            if(file.isFile) file.delete() //刪掉原本的圖片(因為無法直接覆蓋)

            val bStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bStream) //將bipmap轉成圖片
            val byteArray = bStream.toByteArray() //將圖片轉成010101
            file.writeBytes(byteArray) //寫入檔案
        } catch (e: Exception) {
            Log.e(TAG, "create new file", e)
        }
    }

    private fun backToMain() { //回到Main，把data都傳回去
        Log.d(TAG, "backToMain")
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("items", items)
        intent.putExtra("position", position)
        startActivity(intent)
    }
}