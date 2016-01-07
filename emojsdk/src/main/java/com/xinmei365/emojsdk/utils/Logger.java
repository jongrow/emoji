package com.xinmei365.emojsdk.utils;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 日志打印及上报工具类
 *
 * @date: 2014年9月29日 下午5:45:15 <br/>
 */
public class Logger {

    /**
     * 日志开关
     */
    private static boolean DEBUG = true;

    /**
     * @param d the dEBUG to set
     */
    public static void setDebug(boolean d) {
        DEBUG = d;
    }

    /**
     * 打印debug级别的日志
     *
     */
    public static int d(String tag, Object... args) {
        if (!DEBUG) {
            return -1;
        }
        String msg = getMsg(args);
        return Log.d(tag, msg);
    }

    public static int w(String tag, Object... args) {
        if (!DEBUG) {
            return -1;
        }
        String msg = getMsg(args);
        return Log.w(tag, msg);
    }

    /**
     * 打印Error日志
     *
     */
    public static int e(String tag, Object... args) {
        if (!DEBUG) {
            return -1;
        }
        String msg = getMsg(args);
        return Log.e(tag, msg);
    }

    /**
     * 统一的日志内容拼接方法
     *
     */
    private static String getMsg(Object[] args) {
        StringBuffer msg = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            Object obj = args[i];
            if (obj == null) {
                msg.append("null");
            } else if (obj instanceof Throwable) {
                msg.append(getStackTraceString(((Throwable) obj)));
            } else {
                msg.append(obj.toString());
            }
            msg.append(" ");
        }
        return msg.toString();
    }

    /**
     * 将堆栈信息转化为字符串
     *
     */
    public static String getStackTraceString(Throwable e) {
        try {
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            printWriter.append(e.getMessage());
            e.printStackTrace(printWriter);
            Log.getStackTraceString(e);
            Throwable cause = e.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            String msg = result.toString();
            printWriter.close();
            return msg;
        } catch (Exception ex) {
            return "";
        }
    }


}
