package com.igalata.bubblepicker.rendering

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.view.View
import com.igalata.bubblepicker.*
import com.igalata.bubblepicker.model.Color
import com.igalata.bubblepicker.model.PickerItem
import com.igalata.bubblepicker.physics.Engine
import com.igalata.bubblepicker.rendering.BubbleShader.A_POSITION
import com.igalata.bubblepicker.rendering.BubbleShader.A_UV
import com.igalata.bubblepicker.rendering.BubbleShader.U_BACKGROUND
import com.igalata.bubblepicker.rendering.BubbleShader.fragmentShader
import com.igalata.bubblepicker.rendering.BubbleShader.vertexShader
import org.jbox2d.common.Vec2
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.ArrayList
import kotlin.math.sqrt

/**
 * Created by irinagalata on 1/19/17.
 */
class PickerRenderer(private val glView: View) : GLSurfaceView.Renderer {
    /**
     * New Variables
     * */
    // Add new Item
    private var isNewItemAdded = false
    private var itemsAdd: ArrayList<PickerItem> = arrayListOf()

    private var itemsDelete: ArrayList<PickerItem> = arrayListOf()
    private var isDeleteItems = false

    private var isUpdateItem = false
    private var itemsUpdate: ArrayList<PickerItem> = arrayListOf()

    /**
     * End new variables
     * */


    var backgroundColor: Color? = null
    var maxSelectedCount: Int? = null
        set(value) {
            Engine.maxSelectedCount = value
        }
    var bubbleSize = 50
        set(value) {
            Engine.radius = value
        }
    var listener: BubblePickerListener? = null
    lateinit var items: ArrayList<PickerItem>
    val selectedItems: List<PickerItem?>
        get() = Engine.selectedBodies.map { circles.firstOrNull { circle -> circle.circleBody == it }?.pickerItem }
    var centerImmediately = false
        set(value) {
            field = value
            Engine.centerImmediately = value
        }

    private var programId = 0
    private var vertices: FloatArray = floatArrayOf()
    private var uvBuffer: FloatBuffer = FloatBuffer.wrap(floatArrayOf())
    private var verticesBuffer: FloatBuffer = FloatBuffer.wrap(floatArrayOf())
    private var textureVertices: FloatArray = floatArrayOf()
    private var textureIds: IntArray = intArrayOf()

    private val scaleX: Float
        get() = if (glView.width < glView.height) glView.height.toFloat() / glView.width.toFloat() else 1f
    private val scaleY: Float
        get() = if (glView.width < glView.height) 1f else glView.width.toFloat() / glView.height.toFloat()
    private var circles = ArrayList<Item>()


    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        glClearColor(
                backgroundColor?.red ?: 1f, backgroundColor?.green ?: 1f,
                backgroundColor?.blue ?: 1f, backgroundColor?.alpha ?: 1f
        )
        enableTransparency()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        initialize()
    }

    override fun onDrawFrame(gl: GL10) {
        if (isUpdateItem)
            doUpdateItems()
        if (isDeleteItems)
            doDeleteItems()
        if (isNewItemAdded)
            doAddNewItem()
        calculateVertices()
        Engine.move()
        drawFrame()
    }

    /**
     * Init circle and trigger when surface changed
     * */
    fun initialize() {
        clear()
        Engine.centerImmediately = centerImmediately
        buildItems()
    }

    private fun buildItems() {
        val circlesTemp = arrayListOf<Item>()
        Engine.build(items, scaleX, scaleY).forEachIndexed { index, body ->
            circlesTemp.add(Item(items[index], body))
        }
        circles = circlesTemp
        textureIds = IntArray(circles.size)
        initializeArrays()
    }

    private fun initializeArrays() {
        vertices = FloatArray(circles.size * 8)
        textureVertices = FloatArray(circles.size * 8)
        circles.forEachIndexed { i, item -> initializeItem(item, i) }
        verticesBuffer = vertices.toFloatBuffer()
        uvBuffer = textureVertices.toFloatBuffer()
    }

    private fun initializeItem(item: Item, index: Int) {
        initializeVertices(item, index)
        textureVertices.passTextureVertices(index)
        item.bindTextures(textureIds, index)
    }

    private fun initializeVertices(body: Item, index: Int) {
        val radius = body.radius
        val radiusX = radius * scaleX
        val radiusY = radius * scaleY

        body.initialPosition.apply {
            vertices.put(
                    index * 8, floatArrayOf(
                    x - radiusX,
                    y + radiusY,
                    x - radiusX,
                    y - radiusY,
                    x + radiusX,
                    y + radiusY,
                    x + radiusX,
                    y - radiusY
            )
            )
        }
    }
    /**
     * End changed surface
     * */

    /**
     * Draw Frame functions
     * */
    private fun calculateVertices() {
        circles.forEachIndexed { i, item ->
            initializeVertices(item, i)
        }
        val tempBuffer = vertices.toFloatBuffer()
        vertices.forEachIndexed { i, float ->
            tempBuffer.put(i, float)
        }
        verticesBuffer = tempBuffer
        val a = circles.map {
            it.pickerItem
        }
        items = ArrayList(a)
    }

    private fun drawFrame() {
        glClear(GL_COLOR_BUFFER_BIT)
        glUniform4f(glGetUniformLocation(programId, U_BACKGROUND), 1f, 1f, 1f, 0f)
        verticesBuffer.passToShader(programId, A_POSITION)
        uvBuffer.passToShader(programId, A_UV)
        circles.forEachIndexed { i, circle ->
            circle.drawItself(programId, i, scaleX, scaleY)
        }
    }

    fun preloadItems(
            updateItems: ArrayList<PickerItem>,
            removeItems: ArrayList<PickerItem>,
            addItems: ArrayList<PickerItem>
    ) {
        preloadUpdateItems(updateItems)
        preloadNewItem(addItems)
        preloadDeleteItems(removeItems)
    }

    private fun preloadUpdateItems(items: ArrayList<PickerItem>) {
        itemsUpdate = items
        isUpdateItem = true
    }

    private fun preloadNewItem(items: ArrayList<PickerItem>) {
        itemsAdd = items
        isNewItemAdded = true
    }

    private fun preloadDeleteItems(items: ArrayList<PickerItem>) {
        itemsDelete = items
        isDeleteItems = true
    }

    private fun doDeleteItems() {
        if (itemsDelete.isEmpty())
            return

        itemsDelete.forEach { pickerItem ->
            getPosition(pickerItem)?.let { position ->
                Engine.removeBody(position)
                circles.removeAt(position)

                textureIds.drop(position)

                vertices.drop(position * 8)
                vertices.drop(position * 8 + 1)
                vertices.drop(position * 8 + 2)
                vertices.drop(position * 8 + 3)
                vertices.drop(position * 8 + 4)
                vertices.drop(position * 8 + 5)
                vertices.drop(position * 8 + 6)
                vertices.drop(position * 8 + 7)

                textureVertices.drop(position * 8)
                textureVertices.drop(position * 8 + 1)
                textureVertices.drop(position * 8 + 2)
                textureVertices.drop(position * 8 + 3)
                textureVertices.drop(position * 8 + 4)
                textureVertices.drop(position * 8 + 5)
                textureVertices.drop(position * 8 + 6)
                textureVertices.drop(position * 8 + 7)
            }
        }

        verticesBuffer = vertices.toFloatBuffer()
        uvBuffer = textureVertices.toFloatBuffer()

        isDeleteItems = false
    }

    private fun doUpdateItems() {
        itemsUpdate.forEach { new ->
            circles.forEachIndexed { index, old ->
                if (old.pickerItem.id == new.id) {
                    circles[index].pickerItem = new
                    itemRadiusChanged(index, new.circleScale)
                }
                glBindTexture(GL_TEXTURE_2D, textureIds[index])
                circles[index].createBitmap().apply {
                    GLUtils.texImage2D(GL_TEXTURE_2D, 0, this, 0)
                    this.recycle()
                }
                glBindTexture(GL_TEXTURE_2D, 0)
            }
        }
        isUpdateItem = false
    }

    private fun getPosition(pickerItem: PickerItem): Int? {
        circles.forEachIndexed { index, item ->
            if (pickerItem.id == item.pickerItem.id)
                return index
        }
        return null
    }

    private fun doAddNewItem() {
        if (itemsAdd.isEmpty())
            return

        itemsAdd.forEachIndexed { _, item ->
            val newCircleItem = Item(item, Engine.buildNewItem(item, scaleX, scaleY))
            circles.add(newCircleItem)

            val newPos = getPosition(item)!!

            textureIds = textureIds.copyOf(textureIds.size + 1)
            vertices = vertices.copyOf(vertices.size + 8)
            textureVertices = textureVertices.copyOf(textureVertices.size + 8)

            val radius = newCircleItem.radius
            val radiusX = radius * scaleX
            val radiusY = radius * scaleY

            newCircleItem.initialPosition.apply {
                vertices.put(
                        newPos * 8, floatArrayOf(
                        x - radiusX,
                        y + radiusY,
                        x - radiusX,
                        y - radiusY,
                        x + radiusX,
                        y + radiusY,
                        x + radiusX,
                        y - radiusY
                )
                )
            }

            textureVertices.passTextureVertices(newPos)
            newCircleItem.bindTextures(textureIds, newPos)
        }

        verticesBuffer = vertices.toFloatBuffer()
        uvBuffer = textureVertices.toFloatBuffer()

        isNewItemAdded = false
    }
    /**
     * End draw frame
     * */

    /**
     * Init Playground for bubbles
     * */
    private fun enableTransparency() {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        attachShaders()
    }

    fun attachShaders() {
        programId = createProgram(
                createShader(GL_VERTEX_SHADER, vertexShader),
                createShader(GL_FRAGMENT_SHADER, fragmentShader)
        )
        glLinkProgram(programId)
        glUseProgram(programId)
    }

    private fun createProgram(vertexShader: Int, fragmentShader: Int) = glCreateProgram().apply {
        glAttachShader(this, vertexShader)
        glAttachShader(this, fragmentShader)
        glLinkProgram(this)
    }

    private fun createShader(type: Int, shader: String) = glCreateShader(type).apply {
        glShaderSource(this, shader)
        glCompileShader(this)
    }

    /**
     * End init step
     * */

    fun swipe(x: Float, y: Float) = Engine.swipe(
            x.convertValue(glView.width, scaleX),
            y.convertValue(glView.height, scaleY)
    )

    fun release() = Engine.release()

    private fun getItem(position: Vec2) = position.let { pos ->
        val x = pos.x.convertPoint(glView.width, scaleX)
        val y = pos.y.convertPoint(glView.height, scaleY)
        circles.find { circle ->
            sqrt(((x - circle.x).sqr() + (y - circle.y).sqr()).toDouble()) <= circle.radius
        }
    }

    fun resize(x: Float, y: Float) = getItem(Vec2(x, glView.height - y))?.apply {
        if (Engine.resize(this)) {
            listener?.let {
                if (circleBody.increased)
                    it.onBubbleDeselected(pickerItem)
                else
                    it.onBubbleSelected(pickerItem)
            }
        }
    }

    fun getData(x: Float, y: Float) = getItem(Vec2(x, glView.height - y))

    private fun clear() {
        circles.clear()
        Engine.clear()
    }

    /***
     * New content added
     */

    fun itemRadiusChanged(position: Int, newCircleScale: Float) {
        Engine.changeCircleRadius(position, newCircleScale, scaleX)
    }

    /***
     * End content added
     */
}