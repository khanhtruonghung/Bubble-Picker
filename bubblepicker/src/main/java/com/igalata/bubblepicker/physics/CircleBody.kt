package com.igalata.bubblepicker.physics

import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import kotlin.math.abs

/**
 * Created by irinagalata on 1/26/17.
 */
class CircleBody(val world: World, var position: Vec2, var radius: Float, var newRadius: Float, var increasedRadius: Float, var density: Float) {

    private val resizeStep = 0.001f

    val decreasedRadius: Float = radius

    var isIncreasing = false

    var isDecreasing = false

    var toBeIncreased = false

    var toBeDecreased = false

    val finished: Boolean
        get() = !toBeIncreased && !toBeDecreased && !isIncreasing && !isDecreasing

    val isBusy: Boolean
        get() = isIncreasing || isDecreasing

    lateinit var physicalBody: Body

    var increased = false

    var isVisible = true

    private val margin = 0.01f
    private val damping = 25f
    private val shape: CircleShape
        get() = CircleShape().apply {
            m_radius = radius + margin
            m_p.setZero()
        }

    private val fixture: FixtureDef
        get() = FixtureDef().apply {
            this.shape = this@CircleBody.shape
            this.density = this@CircleBody.density
        }

    private val bodyDef: BodyDef
        get() = BodyDef().apply {
            type = BodyType.DYNAMIC
            this.position = this@CircleBody.position
        }

    init {
        while (true) {
            if (world.isLocked.not()) {
                initializeBody()
                break
            }
        }
    }

    private fun initializeBody() {
        physicalBody = world.createBody(bodyDef).apply {
            createFixture(fixture)
            linearDamping = damping
        }
    }

    fun resize() = if (increased) decrease() else increase()

    private fun decrease() {
        isDecreasing = true
        radius -= resizeStep
        reset()

        if (abs(radius - decreasedRadius) < resizeStep) {
            increased = false
            clear()
        }
    }

    private fun increase() {
        isIncreasing = true
        radius += resizeStep
        reset()

        if (abs(radius - increasedRadius) < resizeStep) {
            increased = true
            clear()
        }
    }

    private fun reset() {
        physicalBody.fixtureList?.shape?.m_radius = radius + margin
    }

    fun defineState() {
        toBeIncreased = !increased
        toBeDecreased = increased
    }

    private fun clear() {
        toBeIncreased = false
        toBeDecreased = false
        isIncreasing = false
        isDecreasing = false
    }

    /***
     * New content added
     */

    fun scaleUp() {
        if (radius >= newRadius) {
            radius = newRadius
            return
        }
        radius += resizeStep
        reset()
    }

    fun scaleDown() {
        if (radius <= newRadius) {
            radius = newRadius
            return
        }
        radius -= resizeStep
        reset()
    }

    /***
     * End content added
     */
}