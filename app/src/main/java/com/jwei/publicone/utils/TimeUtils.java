package com.jwei.publicone.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.jwei.publicone.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TimeUtils {
    public final static String DATEFORMAT_HOUR_MIN = "HH:mm";
    public final static String DATEFORMAT_YEAR_MONTH_DAY = "yyyy.MM.dd";
    public final static String DATEFORMAT_DAY = "yyyy-MM-dd";
    public final static String DATEFORMAT_COMM = "yyyy-MM-dd HH:mm:ss";
    public final static String DATEFORMAT_COM_YYMMDD_HHMM = "yyyy-MM-dd HH:mm";

    public static String getSleepTimeH(String totalMin, String defaultValue) {

        int mins = 0;

        if (totalMin == null)
            return defaultValue;
        try {
            mins = Integer.valueOf(totalMin);
        } catch (NumberFormatException x) {
            return defaultValue;
        }

        int hours_value = 0;

        String hours_str = "00";

        if (mins >= 60) {
            hours_value = mins / 60;

            if (hours_value < 10) {
                hours_str = "0" + hours_value;
            } else {
                hours_str = String.valueOf(hours_value);
            }

        } else {
            hours_str = "00";
        }


        return hours_str;
    }

    /**
     * 分钟转换成小时
     * 90分钟 = 》 1.5 小时
     *
     * @param totalMin
     * @return
     */
    public static String getSleepTimeM(String totalMin, String defaultValue) {

        int mins = 0;

        if (totalMin == null)
            return defaultValue;
        try {
            mins = Integer.valueOf(totalMin);
        } catch (NumberFormatException x) {
            return defaultValue;
        }


        int min_value = 0;

        String min_str = "00";


        if (mins > 0) {
            min_value = mins % 60;

            if (min_value < 10) {
                min_str = "0" + min_value;
            } else {
                min_str = String.valueOf(min_value);
            }

        } else {
            min_str = "00";
        }


        return min_str;
    }

    /**
     * 毫秒转HH:mm:ss格式字符串
     *
     * @param millis The milliseconds.
     */
    public static String millis2String(long millis) {
        SimpleDateFormat formatter = getSafeDateFormat("mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date curDate = new Date(millis);
        String hh = String.valueOf((int) millis / 60 / 60 / 1000);
        if (hh.length() < 2) {
            hh = "0" + hh;
        }
        String date = hh + ":" + formatter.format(curDate);
        return date;
    }

    /**
     * 毫秒转HH'mm"ss格式字符串
     *
     * @param millis The milliseconds.
     */
    public static String millis2String2(long millis) {
        SimpleDateFormat formatter = getSafeDateFormat("mm\"ss\"");
        Date curDate = new Date(millis);
        String hh = String.valueOf((int) millis / 60 / 60 / 1000);
        if (hh.length() < 2) {
            hh = "0" + hh;
        }
        String date = hh + "'" + formatter.format(curDate);
        return date;
    }

    /**
     * 将分钟数转换成小数分钟，如124分钟转成02小时04分钟
     *
     * @param minutes
     * @return
     */
    public static String getHoursAndMinutes(int minutes, Context context) {
        String result;
        if (minutes == 0) {
            result = 0 + " " + context.getString(R.string.minutes_text);
        } else if (minutes < 10) {
            result = "0" + minutes + " " + context.getString(R.string.minutes_text);
        } else if (minutes < 60) {
            result = minutes + " " + context.getString(R.string.minutes_text);
        } else {
            int hour = minutes / 60;
            String hour_str = String.valueOf(hour);
            if (hour < 10) {
                hour_str = "0" + hour_str;
            }
            int minute = minutes % 60;
            if (minute == 0) {
                result = hour_str + " " + context.getString(R.string.hours_text);
            } else if (minute < 10) {
                result = hour_str + " " + context.getString(R.string.hours_text) + " " + "0" + minute + " " + context.getString(R.string.minutes_text);
            } else {
                result = hour_str + " " + context.getString(R.string.hours_text) + " " + minute + " " + context.getString(R.string.minutes_text);
            }
        }
        return result;
    }

    /**
     * 将分钟数转换成小数分钟，如124分钟转成2小时4分钟
     *
     * @param minutes
     * @return
     */
    public static String getHoursAndMinutes2(int minutes, Context context) {
        String result;
        boolean flag = false;
        if (minutes < 0) {
            minutes = -minutes;
            flag = true;
        }
        if (minutes == 0) {
            result = 0 + " " + context.getString(R.string.hours);
        } else if (minutes < 60) {
            result = minutes + " " + context.getString(R.string.minutes);
        } else {
            int hour = minutes / 60;
            int minute = minutes % 60;
            if (minute == 0) {
                result = hour + " " + context.getString(R.string.hours);
            } else {
                result = hour + " " + context.getString(R.string.hours) + " " + minute + " " + context.getString(R.string.minutes);
            }
        }
        if (flag) {
            result = "- " + result;
        } else {
            result = "+ " + result;
        }
        return result;
    }


    /**
     * 01:30 转换成分钟数 90
     *
     * @param timeStr
     * @return
     */
    public static String getMinutesByTimeForStyle(String timeStr) {
        String result = null;
        if (TextUtils.isEmpty(timeStr) || timeStr.equals("null")) {
            return "";
        }
        SimpleDateFormat sdf = getSafeDateFormat(DATEFORMAT_HOUR_MIN);
        try {
            long dateEnd = sdf.parse(timeStr).getTime();
            long dateStart = sdf.parse("00:00").getTime();
            result = String.valueOf((int) ((dateEnd - dateStart) / (1000 * 60)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 分钟数 90转换成01:30
     *
     * @param minutes
     * @return
     */
    public static String[] getTimeForStyleByMinutes(int minutes) {
        int hours = minutes / 60;
        int minute = minutes % 60;
        String[] result = new String[2];
        result[0] = getSpecialStr(hours);
        result[1] = getSpecialStr(minute);
        return result;
    }

    /**
     * 小于十的前面加零
     *
     * @param num
     * @return
     */
    public static String getSpecialStr(int num) {
        String minuteStr = "";
        if (num == 0) {
            minuteStr = "00";
        } else if (num < 10) {
            minuteStr = "0" + num;
        } else {
            minuteStr = "" + num;
        }
        return minuteStr;
    }

    /**
     * 设置小时和分钟，返回日期对象(24小时制)
     *
     * @return
     */
    public static Date setHoursAndMinutesForDate(int hours, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 日期转换成字符串:指定的日期格式
     */
    public static String date2Str(Date date, String pattern) {
        SimpleDateFormat sdf = getSafeDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * 通过生日返回岁数，参数格式为yyyy-MM-dd，如为空，返回0岁
     * <p>
     * created at 2020/7/11 9:04
     */
    public static int getAge(String birthday) {
        if (TextUtils.isEmpty(birthday)) {
            return 0;
        }
        Date dateBirthday = str2Date(birthday);
        if (dateBirthday == null) {
            return 0;
        }
        return getAge(dateBirthday);
    }

    /**
     * 通过生日返回岁数，参数格式为yyyy-MM-dd，如为空，返回0岁
     * <p>
     * created at 2020/7/11 9:04
     */
    public static int getAge(Date dateBirthday) {

        Date date = new Date();
        date.setTime(System.currentTimeMillis());

        if (dateBirthday == null) {
            return 0;
        }

        int age = date.getYear() - dateBirthday.getYear();
        if (dateBirthday.getMonth() > date.getMonth()) {
            return age - 1;
        } else if (dateBirthday.getMonth() == date.getMonth() && dateBirthday.getDate() > date.getDate()) {
            return age - 1;
        }
        return age;
    }

    /**
     * 字符串转换成日期
     */
    public static Date str2Date(String str) {
        if (TextUtils.isEmpty(str))
            return null;
        //先用时间戳转换，失败之后再用日期转换
        DateFormat format = getSafeDateFormat(DATEFORMAT_COMM);
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
//            e.printStackTrace();
            format = getSafeDateFormat(DATEFORMAT_DAY);
            try {
                date = format.parse(str);
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
        }
        return date;
    }

    /**
     * 将输入的日期字符串格式转成Date对象
     */
    public static synchronized Date str2Date(String time, String pattern) {
        SimpleDateFormat mFormatter = getSafeDateFormat(DATEFORMAT_DAY);
        mFormatter.applyPattern(pattern);
        try {
            return mFormatter.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过日期对象得年份的整数值
     *
     * @param date
     * @return
     */
    public static int getYearFromDate(Date date) {
        int result = -1;
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            result = cal.get(Calendar.YEAR);
        }
        return result;
    }

    /**
     * 通过日期对象得月份的整数值
     *
     * @param date
     * @return
     */
    public static int getMonthFromDate(Date date) {
        int result = -1;
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            result = cal.get(Calendar.MONTH) + 1;
        }
        return result;
    }

    /**
     * 求时间是几号
     *
     * @return
     */
    public static int getDayFromDate(Date date) {
        int result = -1;
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            result = cal.get(Calendar.DAY_OF_MONTH);
        }
        return result;
    }


    /**
     * 获取完整时间
     *
     * @return
     */
    public static String getDate() {
        SimpleDateFormat format = getSafeDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return format.format(date);
    }

    /**
     * 获取完整时间
     *
     * @return
     */
    public static String getTime() {
        SimpleDateFormat format = getSafeDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return format.format(date);
    }

    /**
     * 完整日期 转换 成时间
     * 2015-10-10 15:36:57  = 》 15:36:57
     *
     * @param time
     * @return
     */
    public static String AllTimeToTime(String time) {
        String result = "";
        SimpleDateFormat format = getSafeDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat format2 = getSafeDateFormat("HH:mm:ss");
        result = format2.format(date);

        return result;

    }

    /**
     * 完整日期 转换 成时间
     * 2015-10-10 15:36:57  = 》 2015-10-10
     *
     * @param time
     * @return
     */
    public static String AllTimeToDate(String time) {
        String result = "";
        SimpleDateFormat format = getSafeDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat format2 = getSafeDateFormat("yyyy-MM-dd");
        result = format2.format(date);

        return result;

    }

    /*
     * 将时间转换为时间戳
     */
    public static long timeToStamp(String s) throws ParseException {
        SimpleDateFormat simpleDateFormat = getSafeDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        return date.getTime();
    }

    /**
     * 带时令的GMT date
     *
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    public static Date getGreenDate() {
        TimeZone gmtTz = TimeZone.getTimeZone("GMT");
        SimpleDateFormat df = getSafeDateFormat(DATEFORMAT_COMM);
        df.setTimeZone(gmtTz);
        String strDate = df.format(new Date());
        df = getSafeDateFormat(DATEFORMAT_COMM);
        Date parse = new Date();
        try {
            parse = df.parse(strDate);
        } catch (ParseException e) {

        }
        return parse;
    }

    public static int toTimezoneInt(String s) {
        int value = 0;
        if (s.contains("+")) {
            String replace = s.replace("+", "");
            Date date = str2Date(replace, DATEFORMAT_HOUR_MIN);
            value = date.getHours() * 60 + date.getMinutes();
        } else if (s.contains("-")) {
            String replace = s.replace("-", "");
            Date date = str2Date(replace, DATEFORMAT_HOUR_MIN);
            value = -(date.getHours() * 60 + date.getMinutes());
        }
        return value;
    }

    /**
     * 获取当前时间偏移UTC时间的分钟数
     *
     * @return
     */
    public static int getCurrentTimeZone() {
        Calendar calendar = Calendar.getInstance();
        int timezoneMinutes = calendar.getTimeZone().getRawOffset() / (60 * 1000);
        return timezoneMinutes;
    }

    //region 获取日期格式化对象
    /**
     * 日期格式化缓存map
     */
    private static final ThreadLocal<Map<String, SimpleDateFormat>> SDF_THREAD_LOCAL
            = new ThreadLocal<Map<String, SimpleDateFormat>>() {
        @Override
        protected Map<String, SimpleDateFormat> initialValue() {
            return new HashMap<>();
        }
    };

    /**
     * 格式化对象
     * @param pattern
     * @return
     */
    public static SimpleDateFormat getSafeDateFormat(String pattern) {
        Map<String, SimpleDateFormat> sdfMap = SDF_THREAD_LOCAL.get();
        SimpleDateFormat simpleDateFormat = sdfMap.get(pattern);
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat(pattern,Locale.ENGLISH);
            sdfMap.put(pattern, simpleDateFormat);
        }
        return simpleDateFormat;
    }

    //endregion

}
