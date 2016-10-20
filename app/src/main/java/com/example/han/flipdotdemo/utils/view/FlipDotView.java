package com.example.han.flipdotdemo.utils.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.han.flipdotdemo.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by reikyZ on 16/8/31.
 */
public class FlipDotView extends LinearLayout {
    /**
     * FlipDotView 的翻动音效 当翻动数量过多时声音可能会不自然或者无声，可以调整SoundPool构造函数第一个参数
     * 设置成一个比较大的数量，允许可能多的声音对象播放
     */
    Context mContext;

    private float mDotSize;
    private float mDotPadding;
    private int mWidthNum;
    private int mHeightNum;
    private Drawable mDot;
    private Drawable mDotBack;
    private boolean mSoundOn;

    int duration = 50;

    List<List<Integer>> oldList = new ArrayList<>();

    SoundPool soundPool = new SoundPool(40, AudioManager.STREAM_MUSIC, 0);
    HashMap<Integer, Integer> soundPoolMap = new HashMap<Integer, Integer>();

    public FlipDotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
        mContext = context;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FlipDot);
        /**
         * dotSize 为每个ImageView的边长（默认为正方形）
         * dotPadding 为ImagView 的Padding值 ，四边相同
         * widthNum为显示点阵列数
         * heightNum为行数
         * dotDrawable 为ImageView状态是显示时的图片
         * dotBackDrawable 为ImageView状态时不时显示图片
         * soundOn是磁翻翻动时的声音开关
         * 动画总时长
         *  动画间隔时长
         *  动画方向
         *
         */
        mDotSize = typedArray.getDimensionPixelSize(R.styleable.FlipDot_dotSize, 50);
        mDotPadding = typedArray.getDimensionPixelSize(R.styleable.FlipDot_dotPadding, 5);
        mWidthNum = typedArray.getInteger(R.styleable.FlipDot_widthNum, 1);
        mHeightNum = typedArray.getInteger(R.styleable.FlipDot_heightNum, 1);
        mDot = typedArray.getDrawable(R.styleable.FlipDot_dotDrawable);
        mDotBack = typedArray.getDrawable(R.styleable.FlipDot_dotBackDrawable);
        mSoundOn = typedArray.getBoolean(R.styleable.FlipDot_soundOn, true);

        typedArray.recycle();

        initStauts();
        initViews(context, attrs);
        initSound();
    }

    /**
     * 预先构造一个二维容器，存放初始化的显示状态和改变后的状态
     */
    private void initStauts() {
        oldList.clear();
        for (int i = 0; i < mHeightNum; i++) {
            List<Integer> subList = new ArrayList<>();
            subList.clear();
            for (int j = 0; j < mWidthNum; j++) {
                subList.add(1);
            }
            oldList.add(subList);
        }
    }

    /**
     * view 的主要实现 根据得到的行数，列数以LinearLayout为容器，生成imageView 的阵列
     *
     * @param context
     * @param attrs
     */
    private void initViews(Context context, AttributeSet attrs) {
        for (int i = 0; i < mHeightNum; i++) {
            LinearLayout ll = new LinearLayout(context);
            LayoutParams llParam = new LayoutParams((int) (mWidthNum * mDotSize), (int) mDotSize);
            ll.setLayoutParams(llParam);

            for (int j = 0; j < mWidthNum; j++) {
                ImageView iv = new ImageView(context);
                LayoutParams ivParam = new LayoutParams(
                        Math.round(mDotSize),
                        Math.round(mDotSize));
                iv.setLayoutParams(ivParam);
                int padding = (int) mDotPadding;
                iv.setPadding(padding, padding, padding, padding);
                iv.setImageDrawable(mDot);
                ll.addView(iv);
            }
            addView(ll);
        }
    }

    /**
     * 初始化了SoundPool,作为磁翻翻动时的生效，其中存放了几个不同的轻微敲击生效，避免大量连续播放产生的机械感和爆音
     */
    private void initSound() {

        soundPoolMap.put(0, soundPool.load(mContext, R.raw.click_0, 1));
        soundPoolMap.put(1, soundPool.load(mContext, R.raw.click_1, 2));
        soundPoolMap.put(2, soundPool.load(mContext, R.raw.click_2, 3));
    }

    /**
     * ]
     * 实现了showBitmap的从FlipDotView 中心向外辐射渐变翻动效果
     *
     * @param list
     */
    public void flipFromCenter(final List<List<Integer>> list) {
        Random random = new Random();

        int centerX = (mHeightNum - 1) / 2, centerY = (mWidthNum - 1) / 2;
        //计算出FlipDotView中心的行列位置
        for (int i = 0; i < mHeightNum; i++) {
            //遍历二维数组根据坐标到中心的距离，根据每个距离间隔，加上一个随机延迟（duration*randow.nextInt(5)）
            //获得每个ImageVIew的动画延迟deley
            int delay = 0;
            for (int j = 0; j < list.get(i).size(); j++) {
                delay = distance(centerX, centerY, i, j) * 300 + duration * random.nextInt(5);

                final ImageView iv = (ImageView) ((LinearLayout) getChildAt(i)).getChildAt(j);
                final int finalI = i;
                final int finalJ = j;
                if (!oldList.get(i).get(j).equals(list.get(i).get(j))) {

                    iv.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Rotate3d rotate = new Rotate3d();
                            rotate.setDuration(200);
                            rotate.setAngle(180);
                            iv.startAnimation(rotate);
                            //判断目标阵列list与当前显示阵列中（i,j）位置上的值是否相同，相同则表示不需要翻动操作，继续遍历
                            //如果不同则调用当前点ImagVIew.postDetayed()方法，将delay传入

                            //判断需要翻转的list点状态，播放翻转动画，并设置图片；
                            //最后如果需要播放声音，则播放声效
                            if (list.get(finalI).get(finalJ) == 1) {
                                iv.setImageDrawable(mDot);
                            } else if (list.get(finalI).get(finalJ) == 0) {
                                iv.setImageDrawable(mDotBack);
                            } else {
                                Log.e("sssss", "ERROR");
                            }
                            if (mSoundOn)
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playSound(mContext, finalJ % soundPoolMap.size(), 0);
                                    }
                                }).start();
                        }
                    }, delay);
                    oldList.get(i).set(j, list.get(i).get(j));
                }
            }
        }
    }

    public void flipFromLeftTop(final List<List<Integer>> list) {
        Random random = new Random();
        int start = 0;

        for (int i = 0; i < list.size(); i++) {
            start += random.nextInt(5) * duration + 50;
            int delay = 0;
            for (int j = 0; j < list.get(i).size(); j++) {
                delay += random.nextInt(5) * duration + 50;
                final ImageView iv = (ImageView) ((LinearLayout) getChildAt(i)).getChildAt(j);
                final int finalI = i;
                final int finalJ = j;
                if (!oldList.get(i).get(j).equals(list.get(i).get(j))) {

                    iv.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Rotate3d rotate = new Rotate3d();
                            rotate.setDuration(200);
                            rotate.setAngle(180);
                            iv.startAnimation(rotate);


                            if (list.get(finalI).get(finalJ) == 1) {
                                iv.setImageDrawable(mDot);
                            } else if (list.get(finalI).get(finalJ) == 0) {
                                iv.setImageDrawable(mDotBack);
                            } else {
                                Log.e("sssss", "ERROR");
                            }
                            if (mSoundOn)
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playSound(mContext, finalJ % soundPoolMap.size(), 0);
                                    }
                                }).start();
                        }
                    }, start + delay);
                    oldList.get(i).set(j, list.get(i).get(j));
                }
            }
        }
        System.gc();
    }

    public void flip(final List<List<Integer>> list) {
        Random random = new Random();
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < list.get(i).size(); j++) {
                final ImageView iv = (ImageView) ((LinearLayout) getChildAt(i)).getChildAt(j);
                final int finalI = i;
                final int finalJ = j;
                if (!oldList.get(i).get(j).equals(list.get(i).get(j))) {

                    iv.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Rotate3d rotate = new Rotate3d();
                            rotate.setDuration(200);
                            rotate.setAngle(180);
                            iv.startAnimation(rotate);


                            if (list.get(finalI).get(finalJ) == 1) {
                                iv.setImageDrawable(mDot);
                            } else if (list.get(finalI).get(finalJ) == 0) {
                                iv.setImageDrawable(mDotBack);
                            } else {
                                Log.e("sssss", "ERROR");
                            }
                            if (mSoundOn)
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playSound(mContext, finalJ % soundPoolMap.size(), 0);
                                    }
                                }).start();
                        }
                    }, random.nextInt(20) * duration);
                    oldList.get(i).set(j, list.get(i).get(j));
                }
            }
        }
        System.gc();
    }


    private void playSound(Context mContext, int sound, int loop) {
        AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        float volume = streamVolumeCurrent / streamVolumeMax;
        soundPool.play(soundPoolMap.get(sound), volume, volume, 1, loop, 1f);
    }

    public int getmWidthNum() {
        return mWidthNum;
    }

    public int getmHeightNum() {
        return mHeightNum;
    }

    public boolean ismSoundOn() {
        return mSoundOn;
    }

    public void setmSoundOn(boolean mSoundOn) {
        this.mSoundOn = mSoundOn;
    }

    private int distance(int x1, int y1, int x2, int y2) {
        int dis = (int) (Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)));
        return dis;
    }

    class Rotate3d extends Animation {

        int mAngle = 90;

        public void setAngle(int angle) {
            mAngle = angle;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            Matrix matrix = t.getMatrix();
            Camera camera = new Camera();
            camera.save();
            camera.rotateY(180 * interpolatedTime);
            camera.getMatrix(matrix);
            camera.restore();
        }
    }
}
