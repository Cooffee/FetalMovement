package com.cooffee.fetalmovement.util;

import android.util.Log;

import com.cooffee.fetalmovement.bean.MyTime;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by eric on 16/1/26.
 */
public class MyTimeUtils {

    public static final int RATIO = 60;
    private static final int MIN_PERIOD = 5; // 最小胎动时间间隔

    private static final String TAG = "fetal_move";

    /**
     * 规范化秒数，将时间数值（单位：秒）转化为自定义的时间对象
     * @param value
     */
    public static MyTime value2Time(int value) {
        int t = value;
        int second = t % RATIO;
        t /= RATIO;
        int minute = t % RATIO;
        t /= RATIO;
        int hour = t % RATIO;
        return new MyTime(hour, minute, second);
    }

    /**
     * 自定义的时间对象转化为数值（单位：秒）
     * @return
     */
    private static int time2Value(MyTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        int second = time.getSecond();
        int value = 0;
        value += hour * RATIO * RATIO + minute * RATIO + second;
        return value;
    }

    /**
     * 通过倒计时的时间，得出当前胎动点击时的时刻点（单位：秒）
     * @param myTimeLeft
     * @param period
     * @return
     */
    public static int timePoint(MyTime myTimeLeft, int period) {
        int totalTime = period;
        int timeLeft = time2Value(myTimeLeft);
        return totalTime - timeLeft;
    }

    /**
     * 时间减少一秒
     * @param time
     * @return
     */
    public static MyTime tick(MyTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        int second = time.getSecond();

        if (second == 0) {
            if (minute == 0) {
                if (hour == 0) {
                    return time;
                } else {
                    hour--;
                    minute = 59;
                    second = 59;
                }
            } else {
                minute--;
                second = 59;
            }
        } else {
            second--;
        }

        time.setHour(hour);
        time.setMinute(minute);
        time.setSecond(second);
        return time;
    }

    /**
     * 规范化数值
     * 如果为个位数，转化为两位显示，如：7->07
     * @param value
     * @return
     */
    public static String format(int value) {
        if (value >= 10) {
            return String.valueOf(value);
        } else {
            return "0" + value;
        }
    }

    /**
     * 筛选出有效的胎动
     * @param timePoints
     */
    public static ArrayList<Integer> filterPoint(ArrayList<Integer> timePoints) {
        int lastPeriod = -1;
        logInConsole(timePoints);
        Iterator<Integer> it = timePoints.iterator();
        while(it.hasNext()) {
            int tmp = it.next();
            if (lastPeriod != -1 && tmp - lastPeriod < MIN_PERIOD) {
                it.remove();
            } else {
                lastPeriod = tmp;
            }
        }
        logInConsole(timePoints);
        return timePoints;
    }

    private static void logInConsole(ArrayList<Integer> timePoints) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < timePoints.size(); i++) {
            sb.append(timePoints.get(i))
                    .append(" ");
        }
        Log.d(TAG, sb.toString());
    }
}
