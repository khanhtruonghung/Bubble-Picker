package com.igalata.bubblepicker.rendering

import android.graphics.*
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.igalata.bubblepicker.model.BubbleGradient
import com.igalata.bubblepicker.model.PickerItem
import com.igalata.bubblepicker.physics.CircleBody
import com.igalata.bubblepicker.rendering.BubbleShader.U_MATRIX
import com.igalata.bubblepicker.toTexture
import org.jbox2d.common.Vec2

/**
 * Created by irinagalata on 1/19/17.
 */
data class Item(var pickerItem: PickerItem, val circleBody: CircleBody) {

    val x: Float
        get() = circleBody.physicalBody.position.x

    val y: Float
        get() = circleBody.physicalBody.position.y

    val radius: Float
        get() = circleBody.radius

    val initialPosition: Vec2
        get() = circleBody.position

    val currentPosition: Vec2
        get() = circleBody.physicalBody.position

    private var isVisible = true
        get() = circleBody.isVisible
    private var texture: Int = 0
    private val bitmapSize = 256f
    private val gradient: LinearGradient?
        get() {
            return pickerItem.gradient?.let {
                val horizontal = it.direction == BubbleGradient.HORIZONTAL
                LinearGradient(if (horizontal) 0f else bitmapSize / 2f,
                        if (horizontal) bitmapSize / 2f else 0f,
                        if (horizontal) bitmapSize else bitmapSize / 2f,
                        if (horizontal) bitmapSize / 2f else bitmapSize,
                        it.startColor, it.endColor, Shader.TileMode.CLAMP)
            }
        }

    fun drawItself(programId: Int, index: Int, scaleX: Float, scaleY: Float) {
        glActiveTexture(GL_TEXTURE)
        glBindTexture(GL_TEXTURE_2D, texture)
        glUniform1i(glGetUniformLocation(programId, BubbleShader.U_TEXT), 0)
        glUniform1i(glGetUniformLocation(programId, BubbleShader.U_VISIBILITY), if (isVisible) 1 else -1)
        glUniformMatrix4fv(glGetUniformLocation(programId, U_MATRIX), 1, false, calculateMatrix(scaleX, scaleY), 0)
        glDrawArrays(GL_TRIANGLE_STRIP, index * 4, 4)
    }

    fun bindTextures(textureIds: IntArray, index: Int) {
        texture = bindTexture(textureIds, index)
    }

    fun createBitmap(): Bitmap {
        var bitmap = Bitmap.createBitmap(bitmapSize.toInt(), bitmapSize.toInt(), Bitmap.Config.ARGB_4444)
        val bitmapConfig: Bitmap.Config = bitmap.config ?: Bitmap.Config.ARGB_8888
        bitmap = bitmap.copy(bitmapConfig, true)

        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawIcon(canvas)
        drawText(canvas)

        return bitmap
    }

    private fun drawBackground(canvas: Canvas) {
        val bgPaint = Paint()
        bgPaint.style = Paint.Style.FILL
        pickerItem.color?.let { bgPaint.color = pickerItem.color!! }
        pickerItem.gradient?.let { bgPaint.shader = gradient }
        canvas.drawRect(0f, 0f, bitmapSize, bitmapSize, bgPaint)
    }

    private fun drawText(canvas: Canvas) {
        if (pickerItem.title == null || pickerItem.textColor == null) return

        var title = pickerItem.title!!

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pickerItem.textColor!!
            textSize = pickerItem.textSize
            typeface = pickerItem.typeface
        }

        var textLayout = placeText(paint, title)

        when {
            pickerItem.icon == null -> {
                canvas.translate((bitmapSize - textLayout.width) / 2f, (bitmapSize - textLayout.height) / 2f)
            }
            pickerItem.iconOnTop -> {
                canvas.translate((bitmapSize - textLayout.width) / 2f, bitmapSize / 2f)
            }
            else -> {
                canvas.translate((bitmapSize - textLayout.width) / 2f, bitmapSize / 2 - textLayout.height)
            }
        }
        textLayout.draw(canvas)
    }

    private fun placeText(paint: TextPaint, text: String): StaticLayout {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, (bitmapSize * 0.85).toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .setLineSpacing(0.0f, 1.0f)

        return builder.build()
    }

    private fun drawIcon(canvas: Canvas) {
        pickerItem.icon?.let {
            val width = it.intrinsicWidth
            val height = it.intrinsicHeight

            val left = (bitmapSize / 2 - width / 2).toInt()
            val right = (bitmapSize / 2 + width / 2).toInt()

            when {
                pickerItem.title == null -> {
                    it.bounds = Rect(left, (bitmapSize / 2 - height / 2).toInt(), right, (bitmapSize / 2 + height / 2).toInt())
                }
                pickerItem.iconOnTop -> {
                    it.bounds = Rect(left, (bitmapSize / 2 - height).toInt(), right, (bitmapSize / 2).toInt())
                }
                else -> {
                    it.bounds = Rect(left, (bitmapSize / 2).toInt(), right, (bitmapSize / 2 + height).toInt())
                }
            }

            it.draw(canvas)
        }
    }

    private fun bindTexture(textureIds: IntArray, index: Int): Int {
        glGenTextures(1, textureIds, index)
        createBitmap().toTexture(textureIds[index])
        return textureIds[index]
    }

    private fun calculateMatrix(scaleX: Float, scaleY: Float) = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.translateM(this, 0, currentPosition.x * scaleX - initialPosition.x,
                currentPosition.y * scaleY - initialPosition.y, 0f)
    }

}