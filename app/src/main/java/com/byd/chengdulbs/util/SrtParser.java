package com.byd.chengdulbs.util;

import android.content.Context;
import android.util.Log;
import com.byd.chengdulbs.model.SubtitleItem;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SrtParser {

    public static List<SubtitleItem> parseSrt(Context context, String assetsPath) {
        List<SubtitleItem> list = new ArrayList<>();
        try {
            Log.d("SRT_DEBUG", "正在尝试读取字幕文件: " + assetsPath);

            // 1. 强制使用 UTF-8 读取，防止中文乱码
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(assetsPath), StandardCharsets.UTF_8));

            String line;
            long startTime = -1;
            long endTime = -1;
            StringBuilder text = new StringBuilder();

            // 2. 升级版正则：
            // - 允许时:分:秒 后面跟 , 或 .
            // - 允许 --> 前后有空格
            // - 格式示例: 00:00:00,120 --> 00:00:04,040
            Pattern timePattern = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})[,.](\\d{1,3})\\s*-->\\s*(\\d{1,2}):(\\d{1,2}):(\\d{1,2})[,.](\\d{1,3})");

            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 3. 处理 BOM 头 (文件开头的隐形字符 \uFEFF)
                if (isFirstLine) {
                    line = line.replace("\uFEFF", "");
                    isFirstLine = false;
                }

                line = line.trim();

                // 空行：说明上一段结束，保存
                if (line.isEmpty()) {
                    if (startTime != -1 && text.length() > 0) {
                        list.add(new SubtitleItem(startTime, endTime, text.toString()));
                        text.setLength(0);
                        startTime = -1;
                    }
                    continue;
                }

                // 纯数字行：序号，直接跳过
                if (line.matches("^\\d+$")) continue;

                // 时间轴行：尝试解析
                Matcher matcher = timePattern.matcher(line);
                if (matcher.find()) {
                    // 提取开始时间
                    startTime = parseTime(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
                    // 提取结束时间
                    endTime = parseTime(matcher.group(5), matcher.group(6), matcher.group(7), matcher.group(8));
                } else {
                    // 既不是序号，也不是时间，那就是字幕内容了
                    if (text.length() > 0) text.append("\n");
                    text.append(line);
                }
            }

            // 保存最后一段
            if (startTime != -1 && text.length() > 0) {
                list.add(new SubtitleItem(startTime, endTime, text.toString()));
            }

            reader.close();

            Log.d("SRT_DEBUG", "✅ 解析成功，共读取到 " + list.size() + " 条字幕");

        } catch (Exception e) {
            Log.e("SRT_DEBUG", "❌ 解析失败: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    private static long parseTime(String h, String m, String s, String ms) {
        try {
            long hours = Long.parseLong(h);
            long minutes = Long.parseLong(m);
            long seconds = Long.parseLong(s);
            long milliseconds = Long.parseLong(ms.trim());
            return (hours * 3600 + minutes * 60 + seconds) * 1000 + milliseconds;
        } catch (Exception e) {
            return 0;
        }
    }
}