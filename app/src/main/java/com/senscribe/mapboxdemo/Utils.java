package com.senscribe.mapboxdemo;

import android.util.Log;

/**
 * Created by chengpf on 15/6/10.
 */
public class Utils {

    public static void d(String tag,String message)
    {
        Log.d(tag,message);
    }

    public static void e(String tag,String error)
    {
        Log.d(tag, error);
    }

    public static void p(int[] input)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0;i < input.length;i++){
            buffer.append(input[i]);
            buffer.append(" ");
        }
        System.out.println(buffer.toString());
    }

    public static void p(String tag,int[] input)
    {
        StringBuffer buffer = new StringBuffer(tag);
        buffer.append("-->\n");
        for(int i = 0;i < input.length;i++){
            buffer.append(input[i]);
            buffer.append(" ");
        }
        System.out.println(buffer.toString());
    }

    public static void p(int[][] input)
    {
        StringBuffer buffer = new StringBuffer();
        for(int x = 0;x < input.length;x++)
        {
            for(int y = 0;y < input[x].length;y++)
            {
                buffer.append(x);
                buffer.append("_");
                buffer.append(y);
                buffer.append(":");
                buffer.append(input[x][y]);
                buffer.append(" ");
            }
            buffer.append("\n");
        }
        buffer.append("\n");
        System.out.println(buffer.toString());
    }

    public static void p(double[] input)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0;i < input.length;i++){
            buffer.append(input[i]);
            buffer.append(" ");
        }
        System.out.println(buffer.toString());
    }

    public static void p(double[][] input)
    {
        StringBuffer buffer = new StringBuffer();
        for(int x = 0;x < input.length;x++)
        {
            for(int y = 0;y < input[x].length;y++)
            {
                buffer.append(x);
                buffer.append("_");
                buffer.append(y);
                buffer.append(":");
                buffer.append(input[x][y]);
                buffer.append(" ");
            }
            buffer.append("\n");
        }
        buffer.append("\n");
        System.out.println(buffer.toString());
    }
}
