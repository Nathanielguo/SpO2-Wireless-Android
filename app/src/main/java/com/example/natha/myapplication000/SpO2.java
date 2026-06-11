package com.example.natha.myapplication000;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * SpO2 自定义波形控件
 * 用于实时绘制 660nm / 940nm 双波长的直流(DC)/交流(AC)分量波形
 *
 * 线程安全说明：
 *  - setLinePoint() 由数据接收线程（子线程）调用
 *  - onDraw() 由 UI 主线程调用
 *  - 二者共享 points/wavePointNum/curX/drawFloatCount 等状态，
 *    因此所有读写均通过 synchronized(lock) 保护，刷新统一切回主线程执行
 */
public class SpO2 extends View {

    private static final int MAX_ADC_VALUE = 4096; // STM32 12位ADC最大值

    // 自定义View属性
    private int backLineColor;
    private int titleColor;
    private int drawLineColor;
    private int titleSize;
    private int leftIndent = 5; // 左边距
    private Paint paintLine;    // 画笔
    private boolean isFirstDrawBackground = true; // 是否是第一次加载背景

    // 视图的高度和宽度
    private int viewHeight;
    private int viewWidth;
    private float effectiveWidth; // X轴有效长度
    private int unitLength;       // 点之间的水平和垂直间距
    private int ySize = 15;       // 纵坐标最大值（ADC 12位=4096，需缩放）
    private int xSize;            // 屏幕最大点数

    private final List<Integer> listXLine = new ArrayList<>(); // 背景网格横坐标
    private final List<Integer> listYLine = new ArrayList<>(); // 背景网格纵坐标

    private int curX = leftIndent; // 绘点的横坐标起始值
    private int curY = 0;          // 绘点的纵坐标起始值
    private int wavePointNum = 0;  // 当前已写入的点序号

    private float[] points = new float[1440 * 4];
    private int drawFloatCount = 0; // points 数组中当前可绘制的有效 float 数

    // 当前波形线条颜色
    private volatile int redValue, greenValue, blueValue;

    // 同步锁：保护 points / wavePointNum / curX / isScreenFull / drawFloatCount
    private final Object lock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SpO2(Context context) {
        this(context, null);
    }

    public SpO2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpO2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.elg);
        drawLineColor = typedArray.getColor(R.styleable.elg_BackLineColor, Color.RED);
        titleColor = typedArray.getColor(R.styleable.elg_TitleColor, Color.RED);
        backLineColor = typedArray.getColor(R.styleable.elg_PointerLineColor, Color.BLACK);
        titleSize = typedArray.getDimensionPixelSize(R.styleable.elg_TitleSize, 10);
        typedArray.recycle();
        initView();
    }

    // 初始画笔
    private void initView() {
        paintLine = new Paint();
        paintLine.setStrokeWidth(1);
        paintLine.setColor(backLineColor);
        paintLine.setAntiAlias(true);
    }

    /**
     * 设置波形线条颜色（建议在 UI 线程调用一次即可）
     */
    public void setLineColor(int redValue, int greenValue, int blueValue) {
        this.redValue = redValue;
        this.greenValue = greenValue;
        this.blueValue = blueValue;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureHeight(int heightMeasureSpec) {
        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }
        int result = 1000;
        if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    private int measureWidth(int widthMeasureSpec) {
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }
        int result = 1000;
        if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        viewHeight = getHeight();
        viewWidth = getWidth();
        effectiveWidth = viewWidth - leftIndent;
        drawBackGround(canvas);
        drawWave(canvas);
    }

    /**
     * 绘制背景网格（坐标系仅在首次绘制时计算一次）
     */
    private void drawBackGround(Canvas canvas) {
        if (isFirstDrawBackground) {
            unitLength = Math.max(1, viewHeight / ySize);
            xSize = ((int) (effectiveWidth / unitLength) + 1);

            int currentX = leftIndent;
            for (int i = 0; i < xSize; i++) {
                listXLine.add(currentX);
                currentX += unitLength;
            }

            int currentY = viewHeight - (ySize * unitLength);
            curY = currentY; // y轴起始偏移
            for (int j = 0; j <= ySize; j++) {
                listYLine.add(currentY);
                currentY += unitLength;
            }
            isFirstDrawBackground = false;
        }

        if (xSize <= 0) {
            return; // 视图尚未measure完成，跳过本次绘制
        }

        paintLine.setColor(backLineColor);
        for (int i = 0; i < xSize; i++) {
            paintLine.setStrokeWidth(i == 0 ? 5 : 1);
            canvas.drawLine(listXLine.get(i), listYLine.get(0), listXLine.get(i), listYLine.get(ySize), paintLine);
        }
        for (int j = 0; j <= ySize; j++) {
            paintLine.setStrokeWidth(j == ySize ? 10 : 1);
            canvas.drawLine(listXLine.get(0), listYLine.get(j), listXLine.get(xSize - 1), listYLine.get(j), paintLine);
        }
    }

    /**
     * 绘制波形：使用统一维护的 drawFloatCount，避免满屏后绘制越界/陈旧数据
     */
    private void drawWave(Canvas canvas) {
        paintLine.setColor(Color.rgb(redValue, greenValue, blueValue));
        paintLine.setStrokeWidth(3);

        float[] snapshot;
        int count;
        synchronized (lock) {
            count = drawFloatCount;
            if (count <= 0) {
                return;
            }
            // 拷贝快照，避免绘制期间数组被子线程修改
            snapshot = new float[count];
            System.arraycopy(points, 0, snapshot, 0, count);
        }
        canvas.drawLines(snapshot, 0, count, paintLine);
    }

    /**
     * 添加一个新的波形采样点（可在子线程调用）
     *
     * @param wavePointY 0-4095 范围内的 ADC 原始值
     */
    public void setLinePoint(int wavePointY) {
        if (unitLength <= 0 || xSize <= 0) {
            // 视图尚未完成首次布局，丢弃该点，避免除零
            return;
        }

        // 把 ADC 值换算成 Y 轴像素坐标
        float div = (float) MAX_ADC_VALUE / (ySize * unitLength);
        int measureYPoint = (int) (wavePointY / div);
        int drawYPoint = curY + (ySize * unitLength - measureYPoint);

        synchronized (lock) {
            int halfScreen = Math.max(1, ((xSize - 1) * unitLength) / 2);

            if (wavePointNum <= halfScreen) {
                if (wavePointNum == 0) {
                    points[0] = curX;
                    points[1] = drawYPoint;
                } else if (wavePointNum == halfScreen) {
                    points[4 * wavePointNum - 2] = curX;
                    points[4 * wavePointNum - 1] = drawYPoint;
                } else {
                    points[4 * wavePointNum - 2] = curX;
                    points[4 * wavePointNum - 1] = drawYPoint;
                    points[4 * wavePointNum] = curX;
                    points[4 * wavePointNum + 1] = drawYPoint;
                }
                wavePointNum++;
                curX += 2;
            } else {
                // 屏幕已满，从头开始覆盖
                wavePointNum = 0;
                curX = leftIndent;
                points[0] = curX;
                points[1] = drawYPoint;
                curX += 2;
                wavePointNum++;
            }

            // 统一计算可绘制的 float 数量，修复满屏后绘制陈旧/越界数据的问题
            int count = wavePointNum > 0 ? 4 * (wavePointNum - 1) : 0;
            drawFloatCount = Math.min(count, points.length);
        }

        // 子线程安全的刷新方式
        mainHandler.post(this::invalidate);
    }
}
