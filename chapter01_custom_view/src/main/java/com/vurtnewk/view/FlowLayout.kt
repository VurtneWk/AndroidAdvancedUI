package com.vurtnewk.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.vurtnewk.core.ext.dpToPx


/**
 * createTime:  2025/1/3 17:45
 * author:      vurtnewk
 * description: 流式布局
 */
class FlowLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val mHorizontalSpacing = 16.dpToPx().toInt()
    private val mVerticalSpacing = 8.dpToPx().toInt()

    private val allLines: MutableList<MutableList<View>> = mutableListOf() // 记录所有的行，一行一行的存储，用于layout
    private val lineHeights: MutableList<Int> = mutableListOf() // 记录每一行的行高，用于layout

    private fun clearMeasureParams() {
        allLines.clear()
        lineHeights.clear()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        /**
         * onMeasure 可能多次调用，导致数据混乱
         */
        clearMeasureParams()
        //step1 先度量孩子
        //保存一行中的所有的View
        var lineViews = mutableListOf<View>()
        var lineWidthUsed = 0 //记录一行已经使用了多宽
        var lineHeight = 0 //一行的行高

        var parentNeededWidth = 0 // measure过程中，子View要求的父ViewGroup的宽
        var parentNeededHeight = 0 // measure过程中，子View要求的父ViewGroup的高

        val selfWidth = MeasureSpec.getSize(widthMeasureSpec) //ViewGroup解析的父亲给我的宽度
        val selfHeight = MeasureSpec.getSize(heightMeasureSpec) // ViewGroup解析的父亲给我的高度
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.visibility == View.GONE) continue


            val layoutParams = childView.layoutParams
            val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, layoutParams.width)
            val childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom, layoutParams.height)

            childView.measure(childWidthMeasureSpec, childHeightMeasureSpec)

            //如果需要换行
            if (lineWidthUsed + childView.measuredWidth + mHorizontalSpacing > selfWidth) {
                //一旦换行，我们就可以判断当前行需要的宽和高了，所以此时要记录下来
                allLines.add(lineViews)
                lineHeights.add(lineHeight)
                //
                parentNeededWidth = parentNeededWidth.coerceAtLeast(lineWidthUsed + mHorizontalSpacing)
                parentNeededHeight += lineHeight + mVerticalSpacing
                //清除行数据
                lineViews = mutableListOf()
                lineWidthUsed = 0
                lineHeight = 0
            }

            //添加数据
            lineViews.add(childView)
            lineWidthUsed += childView.measuredWidth + mHorizontalSpacing
            //行高取 每行里每个子view的最大值
            lineHeight = lineHeight.coerceAtLeast(childView.measuredHeight)

            //处理最后一行数据
            if (i == childCount - 1) {
                allLines.add(lineViews)
                lineHeights.add(lineHeight)
                parentNeededWidth = parentNeededWidth.coerceAtLeast(lineWidthUsed + mHorizontalSpacing)
                parentNeededHeight += lineHeight + mVerticalSpacing
            }

        }

        //step2 再度量自己 , 保存
        //根据子View的度量结果，来重新度量自己ViewGroup
        // 作为一个ViewGroup，它自己也是一个View,它的大小也需要根据它的父亲给它提供的宽高来度量
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val realWidth = if ((widthMode == MeasureSpec.EXACTLY)) selfWidth else parentNeededWidth
        val realHeight = if ((heightMode == MeasureSpec.EXACTLY)) selfHeight else parentNeededHeight

        setMeasuredDimension(realWidth, realHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        //当前的左和上的位置
        var curLeft = paddingLeft //第一个view的左坐标
        var curTop = paddingTop
        for ((lineIndex, views) in allLines.withIndex()) {
            //每一行的view
            for (view in views) {
                val left = curLeft
                val top = curTop
                //注意：  view.width 和 view.measuredWidth 的区别
//                val right = curLeft + view.width
//                val bottom = top + view.height
                val right = curLeft + view.measuredWidth
                val bottom = top + view.measuredHeight

                view.layout(left, top, right, bottom)
                // 下一个view的坐标 是当前view的右坐标加间隔
                curLeft = right + mHorizontalSpacing
            }
            //一行结束后，行高改变，左坐标还原
            curTop += lineHeights[lineIndex] + mVerticalSpacing
            curLeft = paddingLeft
        }
    }
}