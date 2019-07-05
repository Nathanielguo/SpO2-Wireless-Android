package com.example.natha.myapplication000;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpO2 extends View {
    //自定义View属性
    private int backLineColor;
    private int titleColor;
    private int drawLineColor;
    private int titleSize;
    private String title = "心电图";
    private int leftIndent = 5;//左边距
    private Paint paintLine;//画笔
    private Boolean isFristDrawBackGround = true;// 是否是第一次加载背景
    //视图的高度和宽度
    private int viewHeight;
    private int viewWidth;
    private float  effectiveWidth;//X轴有效长度
    private int  unitLength;//点之间的水平和垂直间距
    private int ySize=15;//纵坐标最大值(STM32的ADC12位=4096，所以应该将ADC的值进行缩小)
    private int xSize;//屏幕最大点数
    private List<Integer> listXLine = new ArrayList<Integer>();//装背景网格横坐标
    private List<Integer> listYLine = new ArrayList<Integer>();//装背景网格纵坐标
    private final static String X_KEY = "Xpos";//X坐标的键值
    private final static String Y_KEY = "Ypos";//Y坐标的键值
    private int curX = leftIndent;//绘点的横坐标起始值
    private int curY = 0;//绘点的纵坐标起始值
    private int wavePointNum = 0;//当前添加点
    private float[] points = new float[1440*4];
    private List<Map<String, Integer>> listPoints = new ArrayList<Map<String, Integer>>();//添加wavePoint坐标点的集合
    private int removedPointNum = 0;//removedPointNum个点不显示
    //private Map<String, Integer> temp = new HashMap<String, Integer>();
    private boolean isScreenFull = false;
    private int redvalue,greenvalue,bluevalue;

    public SpO2(Context context) {
        this(context, null);
        initView();
    }

    public SpO2(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.elg);
        drawLineColor = typedArray.getColor(R.styleable.elg_BackLineColor, Color.RED);
        titleColor = typedArray.getColor(R.styleable.elg_TitleColor, Color.RED);
        backLineColor = typedArray.getColor(R.styleable.elg_PointerLineColor, Color.BLACK);
        titleSize = typedArray.getDimensionPixelSize(R.styleable.elg_TitleSize, 10);
        typedArray.recycle();
        initView();
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
    //初始画笔
    private void initView() {
        paintLine = new Paint();
        paintLine.setStrokeWidth(1);
        paintLine.setColor(backLineColor);
        paintLine.setAntiAlias(true);
    }

    public void setLineColor(int redvalue,int greenvalue,int bluevalue) {

        this.redvalue=redvalue;
        this.greenvalue=greenvalue;
        this.bluevalue=bluevalue;
    }

    //重定义View视图大小
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),measureHeight(heightMeasureSpec));
    }

    private int measureHeight(int heightMeasureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if(specMode == MeasureSpec.EXACTLY){
            result = specSize;
        }else {
            result = 1000;
            if(specMode == MeasureSpec.AT_MOST){
                result = Math.min(result,specSize);
            }
        }
        return result;
    }

    private int measureWidth(int widthMeasureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        if(specMode == MeasureSpec.EXACTLY){
            result = specSize;
        }else {
            result = 1000;
            if(specMode == MeasureSpec.AT_MOST){
                result = Math.min(result,specSize);
            }
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        viewHeight= getHeight();
        viewWidth= getWidth();
        effectiveWidth =viewWidth-leftIndent;
        drawBackGround(canvas);
        drawWave(canvas);

    }

    public void drawBackGround(Canvas canvas) {
        //添加背景坐标点
        if (isFristDrawBackGround) {
            unitLength = viewHeight / ySize;
            xSize = ((int) (effectiveWidth / unitLength) + 1);
            int currentX = leftIndent;
            for (int i = 0; i < xSize; i++) {
                listXLine.add(currentX);
                currentX += unitLength;
            }
            int currentY = viewHeight - (ySize * unitLength);
            curY = currentY;              //y轴余数
            for (int j = 0; j <= ySize; j++) {
                listYLine.add(currentY);
                currentY += unitLength;
            }
            isFristDrawBackGround = false;
        }
        //画背景网格
        paintLine.setColor(backLineColor);
        for (int i = 0; i < xSize; i++) {
            if (i == 0) {
                paintLine.setStrokeWidth(5);
                canvas.drawLine(listXLine.get(i), listYLine.get(0), listXLine.get(i), listYLine.get(ySize), paintLine);
            } else {
                paintLine.setStrokeWidth(1);
                canvas.drawLine(listXLine.get(i), listYLine.get(0), listXLine.get(i), listYLine.get(ySize), paintLine);
            }

        }
        for (int j = 0; j <= ySize; j++) {
            if (j == ySize) {
                paintLine.setStrokeWidth(10);
                canvas.drawLine(listXLine.get(0), listYLine.get(j), listXLine.get(xSize - 1), listYLine.get(j), paintLine);
            } else {
                canvas.drawLine(listXLine.get(0), listYLine.get(j), listXLine.get(xSize - 1), listYLine.get(j), paintLine);
            }
        }
    }

    public void drawWave(Canvas canvas) {
        //paintLine.setColor(drawLineColor);
        paintLine.setColor(Color.rgb(redvalue, greenvalue, bluevalue));
        paintLine.setStrokeWidth(3);
        if(isScreenFull==false){
            if(wavePointNum>0){
                canvas.drawLines(points,0,4*(wavePointNum-1),paintLine);
            }
        }
        else{
            canvas.drawLines(points,0,4*((xSize-1)*unitLength),paintLine);
        }
        /**
         for (int index = 0; index < listPoints.size(); index++) {
         if (listPoints.size() ==xSize && (index >= wavePointNum && index < wavePointNum + removedPointNum)) {
         continue; //removedPointNum个点间隔，本实验要求心电图连续更新，removedPointNum为0，条件表达式总是false;
         }


         if (index > 0) {

         canvas.drawLine(listPoints.get(index - 1).get(X_KEY), listPoints.get(index - 1).get(Y_KEY),
         listPoints.get(index).get(X_KEY),listPoints.get(index).get(Y_KEY), paintLine);

         canvas.drawLines(points,0,2*index,paintLine);
         }

         }
         **/
    }

    //添加点到points集合中
    public void setLinePoint(int wavePointY) {
        //Map<String, Integer> temp = new HashMap<String, Integer>();
        //temp.put(X_KEY, curX);
        //curX += unitLength;
        //curX += 1;
        float div= 4096/(ySize*unitLength);                //把Y值大小转换成Y轴的像素点值
        int measureYPoint= (int)(wavePointY/div);
        int drawYPoint = curY+(ySize*unitLength-measureYPoint); //绘点从左上角开始，坐标转换
        //temp.put(Y_KEY, drawYPoint);
        // 判断当前点是否大于最大点数
        if (wavePointNum <= ((xSize-1)*unitLength)/2) {
            //屏幕点数超过最大数清零后替换前面的点
            /**
             try {
             if (listPoints.size() == ((xSize-1)*unitLength+1) && listPoints.get(wavePointNum) != null) {
             listPoints.remove(wavePointNum);
             }
             } catch (Exception e) {
             e.printStackTrace();
             }
             **/
            //listPoints.add(wavePointNum, temp);
            //首尾两点不需要重复
            if(wavePointNum==0){
                points[2*wavePointNum]=curX;
                points[2*wavePointNum+1]=drawYPoint;
            }else if(wavePointNum==(xSize-1)*unitLength/2){
                points[4*wavePointNum-2]=curX;
                points[4*wavePointNum-1]=drawYPoint;
            }else{
                points[4*wavePointNum-2]=curX;
                points[4*wavePointNum-1]=drawYPoint;
                points[4*wavePointNum]=curX;
                points[4*wavePointNum+1]=drawYPoint;
            }
            wavePointNum++;
            curX += 2;
        } else {
            //对溢出第一个点的处理
            isScreenFull = true;
            wavePointNum= 0;
            curX = leftIndent;
            // temp.put(X_KEY,curX);
            // listPoints.remove(wavePointNum);
            //listPoints.add(wavePointNum, temp);
            points[2*wavePointNum]=curX;
            points[2*wavePointNum+1]=drawYPoint;
            curX+=2;
            wavePointNum++;

        }
        invalidate();    //刷新绘图界面，重新加载onDraw()方法
    }
}






