package com.igalata.bubblepicker.physics

import com.igalata.bubblepicker.model.PickerItem
import com.igalata.bubblepicker.rendering.Item
import com.igalata.bubblepicker.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * Created by irinagalata on 1/26/17.
 */
object Engine {

    val selectedBodies: List<CircleBody>
        get() = bodies.filter { it.increased || it.toBeIncreased || it.isIncreasing }
    var maxSelectedCount: Int? = null
    var radius = 50
        set(value) {
            field = value
            bubbleRadius = interpolate(0.1f, 0.25f, value / 100f)
        }
    var centerImmediately = false
    private var standardIncreasedGravity = interpolate(400f, 600f, 0.5f)
    private var bubbleRadius = 0.17f

    private val world = World(Vec2(0f, 0f), false)
    private const val step = 0.001f
    private var bodies: ArrayList<CircleBody> = ArrayList()
    private var borders: ArrayList<Border> = ArrayList()
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    private var gravity = 55f
    private var increasedGravity = 45f
    private var gravityCenterPoint = Vec2(0f, 0f)
    private val currentGravity: Float
        get() = if (touch) increasedGravity else gravity
    private val toBeResized = ArrayList<Item>()
    private val startX
        get() = if (centerImmediately) 0.5f else 2.2f
    private var stepsCount = 0

    fun build(items: ArrayList<PickerItem>, scaleX: Float, scaleY: Float): List<CircleBody> {
        val density = getInterpolate()

        val itemsCount = items.size

        val bodiesTemp = arrayListOf<CircleBody>()
        for (i in 0 until itemsCount) {
            val x = if (Random().nextBoolean()) -startX else startX
            val y = if (Random().nextBoolean()) -0.5f / scaleY else 0.5f / scaleY
            bodiesTemp.add(i, buildCircleBody(items[i], density, scaleX, x, y))
        }
        bodies = bodiesTemp

        this.scaleX = scaleX
        this.scaleY = scaleY
        initBorders()

        return bodies
    }

    fun buildNewItem(item: PickerItem, scaleX: Float, scaleY: Float): CircleBody {
        val density = getInterpolate()

        val x = if (Random().nextBoolean()) -startX else startX
        val y = if (Random().nextBoolean()) -0.5f / scaleY else 0.5f / scaleY

        val newBody = buildCircleBody(item, density, scaleX, x, y)
        bodies.add(newBody)

        this.scaleX = scaleX
        this.scaleY = scaleY

        return newBody
    }

    fun removeBody(position: Int) {
        bodies[position].let{
            world.destroyBody(it.physicalBody)
        }
        bodies.removeAt(position)
    }

    private fun getInterpolate(): Float {
        return interpolate(0.8f, 0.2f, radius / 100f)
    }

    private fun buildCircleBody(
            item: PickerItem,
            density: Float,
            scaleX: Float,
            x: Float,
            y: Float
    ): CircleBody {
        return CircleBody(
                world = world,
                position = Vec2(x, y),
                radius = bubbleRadius * scaleX * item.circleScale,
                increasedRadius = (bubbleRadius * scaleX) * 1.3f * item.circleScale,
                density = density,
                newRadius = bubbleRadius * scaleX * item.circleScale
        )
    }

    fun changeCircleRadius(position: Int, newCircleScale: Float, scaleX: Float) {
        bodies.getOrNull(position)?.also { body ->
            body.newRadius = bubbleRadius * scaleX * newCircleScale
        }
    }

    fun move() {
        if (toBeResized.isNotEmpty()) {
            toBeResized.forEach { it.circleBody.resize() }
            toBeResized.removeAll(toBeResized.filter { it.circleBody.finished })
        }

        world.step(if (centerImmediately) 0.035f else step, 11, 11)
        bodies.forEach {
            checkRadius(it)
            move(it)
        }

        stepsCount++
        if (stepsCount >= 10)
            centerImmediately = false
    }

    fun swipe(x: Float, y: Float) {
        if (abs(gravityCenterPoint.x) < 2) gravityCenterPoint.x += -x
        if (abs(gravityCenterPoint.y) < 0.5f / scaleY) gravityCenterPoint.y += y
        increasedGravity = standardIncreasedGravity * abs(x * 13) * abs(y * 13)
        touch = true
    }

    fun release() {
        gravityCenterPoint.setZero()
        touch = false
        increasedGravity = standardIncreasedGravity
    }

    fun clear() {
        borders.forEach { world.destroyBody(it.itemBody) }
        bodies.forEach { world.destroyBody(it.physicalBody) }
        borders.clear()
        bodies.clear()
    }

    fun resize(item: Item): Boolean {
        if (selectedBodies.size >= maxSelectedCount ?: bodies.size && !item.circleBody.increased) return false

        if (item.circleBody.isBusy) return false

        item.circleBody.defineState()

        toBeResized.add(item)

        return true
    }

    private fun initBorders() {
        borders = arrayListOf(
                Border(world, Vec2(0f, 0.5f / scaleY), Border.HORIZONTAL),
                Border(world, Vec2(0f, -0.5f / scaleY), Border.HORIZONTAL)
        )
    }

    private fun move(body: CircleBody) {
        body.physicalBody.apply {
            body.isVisible = centerImmediately.not()
            val direction = gravityCenterPoint.sub(position)
            val distance = direction.length()
            val gravity = if (body.increased) 1.3f * currentGravity else currentGravity
            if (distance > step * 200) {
                applyForce(direction.mul(gravity / distance.sqr()), position)
            }
        }
    }

    private fun checkRadius(body: CircleBody) {
        if (body.radius < body.newRadius)
            body.scaleUp()
        else
            body.scaleDown()
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)

}