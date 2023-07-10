package com.jwei.publicone.utils;

import android.annotation.SuppressLint;

import com.jwei.publicone.R;
import com.jwei.publicone.base.BaseApplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static final String TIME_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String TIME_YYYY_MM_DD_HH = "yyyy-MM-dd HH";
    public static final String TIME_MM_DD_HHMMSS = "MM-dd HH:mm:ss";
    public static final String TIME_YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_YYYY_MM_DD_HHMM = "yyyy-MM-dd HH:mm";
    public static final String HHMMSS = "HH:mm:ss";
    public static final String TIME_YYYY_MM = "yyyy-MM";
    public static final String TIME_MM_DD = "MM-dd";
    public static final String TIMEYYYYMMDD_SLASH = "yyyy/MM/dd";
    public static final String TIMEYYYYMMDD_SLASH_RAIL = "yyyy-MM-dd";
    public static final String TIMEYYYYMM_SLASH = "yyyy/MM";
    public static final String TIMEYYYYMM_SLASH_RAIL = "yyyy-MM";
    public static final String MMdd_12 = "MM/dd aa";

    public static String FormatDateYYYYMMDD(com.haibin.calendarview.Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(calendar.getTimeInMillis());
    }

    public static String FormatDateYYYYMMDD(com.haibin.calendarview.Calendar calendar, String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH).format(calendar.getTimeInMillis());
    }

    @SuppressLint("SimpleDateFormat")
    public static String getStringDate(long time, String format) {
        String date = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        date = simpleDateFormat.format(time);
        return date;
    }

    @SuppressLint("SimpleDateFormat")
    public static long getLongTime(String time, String format) {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        try {
            date = simpleDateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            date.setTime(0);
        }
        return date.getTime();
    }

    public static String getTimeString(long second) {
        String time = "";
        long hour = (second / 3600);
        long minute = (second % 3600) / 60;

        if (hour != 0) {
            if (hour < 10) {
                time = "0" + hour + ":";
            } else {
                time = hour + ":";
            }
        }
        if (minute < 10) {
            time = time + "0" + minute + ":";
        } else {
            time = time + minute + ":";
        }

        int sec = (int) ((second % 3600) % 60);
        if (sec < 10) {
            time = time + "0" + sec;
        } else {
            time = time + sec;
        }

        return time;
    }

    public static String getTimeStringHHMM(int hour, int minute) {
        String time = "";
        time = String.format(Locale.ENGLISH, "%02d:%02d", hour, minute);
        return time;
    }

    public static String getDayOfMonthEnd(String startDay) {
        long time = getLongTime(startDay, TIME_YYYY_MM_DD);
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.add(Calendar.MONTH, 1);
        ca.add(Calendar.DAY_OF_YEAR, -1);

        return getStringDate(ca.getTimeInMillis(), TIME_YYYY_MM_DD);
    }

    public static String getDayOfWeekStart(String day) {
        long time = getLongTime(day, TIME_YYYY_MM_DD);
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.set(Calendar.DAY_OF_WEEK, ca.getFirstDayOfWeek());
        return getStringDate(ca.getTimeInMillis(), TIME_YYYY_MM_DD);
    }

    public static String getDayOfWeekMonday(String day) {
        long time = getLongTime(day, TIME_YYYY_MM_DD);
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.setFirstDayOfWeek(Calendar.MONDAY);
        ca.set(Calendar.DAY_OF_WEEK, ca.getFirstDayOfWeek());

        return getStringDate(ca.getTimeInMillis(), TIME_YYYY_MM_DD);
    }

    public static String getDayOfWeekMonday(String day, String format) {
        long time = getLongTime(day, TIME_YYYY_MM_DD);
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.setFirstDayOfWeek(Calendar.MONDAY);
        ca.set(Calendar.DAY_OF_WEEK, ca.getFirstDayOfWeek());

        return getStringDate(ca.getTimeInMillis(), format);
    }

    public static long getNextDay(String startDay, int addDay) {
        long time = getLongTime(startDay, TIME_YYYY_MM_DD);
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.add(Calendar.DAY_OF_YEAR, addDay);

        return ca.getTimeInMillis();
    }

    public static String getWeek(long timeStamp) {
        int date = 0;
        String week = "";
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date(timeStamp));
        date = ca.get(Calendar.DAY_OF_WEEK);
        if (date == 1) {
            week = BaseApplication.mContext.getString(R.string.week_easy_7);
        } else if (date == 2) {
            week = BaseApplication.mContext.getString(R.string.week_easy_1);
        } else if (date == 3) {
            week = BaseApplication.mContext.getString(R.string.week_easy_2);
        } else if (date == 4) {
            week = BaseApplication.mContext.getString(R.string.week_easy_3);
        } else if (date == 5) {
            week = BaseApplication.mContext.getString(R.string.week_easy_4);
        } else if (date == 6) {
            week = BaseApplication.mContext.getString(R.string.week_easy_5);
        } else if (date == 7) {
            week = BaseApplication.mContext.getString(R.string.week_easy_6);
        }
        return week;
    }

    /**
     * 获取某月的天数
     *
     * @param year  年
     * @param month 月
     * @return 某月的天数
     */
    public static int getMonthDaysCount(int year, int month) {
        int count = 0;
        //判断大月份
        if (month == 1 || month == 3 || month == 5 || month == 7
                || month == 8 || month == 10 || month == 12) {
            count = 31;
        }

        //判断小月
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            count = 30;
        }

        //判断平年与闰年
        if (month == 2) {
            if (isLeapYear(year)) {
                count = 29;
            } else {
                count = 28;
            }
        }
        return count;
    }

    /**
     * 是否是闰年
     *
     * @param year year
     * @return 是否是闰年
     */
    public static boolean isLeapYear(int year) {
        return ((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0);
    }

    /**
     * 判断给定时间是否在当前时间前30天未来2天 总33天左右
     *
     * @param tag
     * @return
     */
    public static boolean isEffectiveDate(String tag) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        long beginTime = calendar.getTimeInMillis();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        long endTime = calendar.getTimeInMillis();
        long tagTime = DateUtils.getLongTime(tag, DateUtils.TIME_YYYY_MM_DD);

        Date startDate = new Date(beginTime);
        Date endDate = new Date(endTime);
        Date tagDate = new Date(tagTime);

        if (tagDate.getTime() == startDate.getTime() || tagDate.getTime() == endDate.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(tagDate);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startDate);

        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取年龄
     *
     * @param birthday yyyy-MM-dd
     * @return
     */
    public static int getAgeFromBirthTime(Date birthday) {
        Calendar now = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        b.setTime(birthday);
        int year = now.get(Calendar.YEAR) - b.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) - b.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH) - b.get(Calendar.DAY_OF_MONTH);
        if (month < 0) {
            month = 12 - b.get(Calendar.MONTH) + now.get(Calendar.MONTH);
            year -= 1;
        }
        if (day < 0) {
            day = b.getMaximum(Calendar.DAY_OF_MONTH) - b.get(Calendar.DAY_OF_MONTH) + now.get(Calendar.DAY_OF_MONTH);
            month -= 1;
        }
        // year + "岁" + month + "个月" + day + "天";
        return year;
    }
}
