package com.cooffee.fetalmovement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cooffee.fetalmovement.bean.MyTime;
import com.cooffee.fetalmovement.util.MyTimeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class FetalMovActivity extends BaseActivity {

    @InjectView(R.id.btn_start_end)
    Button btnStartEnd;
    @InjectView(R.id.btn_fetal_move)
    ImageButton btnFetalMove;
    @InjectView(R.id.sp_period)
    Spinner spPeriod;
    @InjectView(R.id.tv_time_left)
    TextView tvTimeLeft;
    @InjectView(R.id.tv_fetal_move_count)
    TextView tvFetalMoveCount;

    private MyTime mTime; // 时间

    private int mPeriod;    // 计数时长
    private int mCount = 0; // 胎动次数（非有效胎动次数）
    private static final String TAG = "fetal_move";

    private boolean isCounting = false; // 当前是否在计数

    private final Context mContext = FetalMovActivity.this;

    private ArrayList<Integer> mTimeLine = new ArrayList<>();

    private Timer mTimer;
    private TimerTask mTask;

    private static final int MIN_EFFECTIVE_PERIOD = 1800; // 最短有效检测时长（单位：秒）
    private static final String DIALOG_TITLE = "提示";
    private static final String DIALOG_MSG = "本次胎动计数时长不够，确定放弃本次计数？";

    private static final int MSG_COUTING = 0x01;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COUTING:
                    output(mTime);  // 输出剩余时长
                    break;
            }
        }
    };

    PowerManager mPowerManager = null;
    PowerManager.WakeLock mWakeLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetal_mov);

        // 屏幕常亮
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWakeLock.release();
    }

    /**
     * 胎动按钮操作
     * @param imgBtn
     */
    @OnClick(R.id.btn_fetal_move)
    void fetalMove(ImageButton imgBtn) {

        if (isCounting) {   // 如果当前正在检测胎动
            // 点击动画
            AnimationDrawable animationDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.btn_fetal_move_animation);
            imgBtn.setBackgroundDrawable(animationDrawable);
            animationDrawable.start();

            mCount++; // 计数
            tvFetalMoveCount.setText(mCount + "次"); // 显示当前胎动次数（非有效胎动）

            int timePoint = MyTimeUtils.timePoint(mTime, mPeriod);  // 计算当前胎动点击的时刻点（单位：秒）
            mTimeLine.add(timePoint);   // 将胎动的时刻点存入数组链表中

            // Toast出时刻点
            StringBuffer sb = new StringBuffer();
            sb.append("时长：")
                    .append(String.valueOf(mPeriod))
                    .append("\n时间点：");
            for (Integer i : mTimeLine) {
                sb.append(i.toString())
                        .append(" ");
            }

        } else {    // 如果当前未在检测胎动，无动画点击效果
            imgBtn.setBackgroundResource(R.drawable.btn_fetal_move_selector);
            Toast.makeText(mContext, "请点击\"开始\"进行计数", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 开始结束按钮回调事件
     * @param button
     */
    @OnClick(R.id.btn_start_end)
    void startEnd(Button button) {
        if (isCounting == false) {
            isCounting = true;
            btnStartEnd.setText("结束");
            start();    // 开始监测
        } else {
            end();      // 结束监测
        }
    }

    /**
     * 开始监测
     */
    private void start() {
        mPeriod = Integer.parseInt((String) spPeriod.getSelectedItem()); // 从Spinner中获取检测时长 单位：分钟
        mPeriod *= MyTimeUtils.RATIO;   // 将时长单位由“分钟”换算成“秒”
        mTime = MyTimeUtils.value2Time(mPeriod);    // 将秒数转化为“时:分:秒”格式

        output(mTime);  // 时长输出

        // 设定定时器，每秒钟减少一秒，时间倒计时效果
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTask == null) {
            mTask = new TimerTask() {
                @Override
                public void run() {
                    mTime = MyTimeUtils.tick(mTime);
                    mHandler.sendEmptyMessage(MSG_COUTING);
                }
            };
        }
        mTimer.schedule(mTask, 1000, 1000); // 每秒钟执行一次
    }

    /**
     * 结束监测
     */
    private void end() {
        int periodCheck = MyTimeUtils.timePoint(mTime, mPeriod);
        if (periodCheck >= MIN_EFFECTIVE_PERIOD) {
            // TODO: 先筛选有效胎动，再传递ArrayList
            MyTimeUtils.filterPoint(mTimeLine);
            reset();    // 重置
            Toast.makeText(mContext, "先筛选有效胎动，在传递ArrayList", Toast.LENGTH_SHORT).show();

        } else {
            new AlertDialog.Builder(this)
                    .setTitle(DIALOG_TITLE)
                    .setMessage(DIALOG_MSG)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO: 跳转上一界面
                            MyTimeUtils.filterPoint(mTimeLine);
                            reset();    // 重置
                            Toast.makeText(mContext, "跳转上一界面", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();

        }
    }

    /**
     * 重置
     */
    private void reset() {
        isCounting = false; // 计数标记
        btnStartEnd.setText("开始");
        mPeriod = 0;
        mCount = 0;
        tvTimeLeft.setText("00:00:00");
        tvFetalMoveCount.setText("0次");
        // 结束定时器
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        // 结束定时器任务
        if (mTask != null) {
            mTask.cancel();
            mTask = null;
        }

        mTimeLine.clear();
    }

    /**
     * 将时长规范化输出，如：01:23:32
     */
    private void output(MyTime time) {
        StringBuffer output = new StringBuffer();
        String hour = MyTimeUtils.format(time.getHour());
        String minute = MyTimeUtils.format(time.getMinute());
        String second = MyTimeUtils.format(time.getSecond());
        output.append(hour)
                .append(":")
                .append(minute)
                .append(":")
                .append(second);
        tvTimeLeft.setText(output.toString());
    }
}
