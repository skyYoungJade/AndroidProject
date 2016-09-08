package com.uu898.mobileorder.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
/**
 * @author xingbo.chai
 *
 */
public class Logs {
	public static String msg = "uu898_log:";
	public static final int DEBUGGER = 0;
	public static final int Demo = 1;
	public static final int STATE = DEBUGGER;

	public static void debug(Object obj){
		switch (STATE) {
		case DEBUGGER:
			Log.v(msg, String.valueOf(obj));
			break;
		default:
			break;
		}
	}

	public static void debug(Context context,Object obj){
		switch (STATE) {
		case DEBUGGER:
			msg += context.getClass().getName();
			Log.v(msg, String.valueOf(obj));
			break;
		default:
			break;
		}
	}

	public static void toast(Context context,Object msg){
		try {
			switch (STATE) {
			case DEBUGGER:
				Toast.makeText(context, String.valueOf(msg), Toast.LENGTH_LONG).show();
				break;
			default:
				break;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
