package com.byd.chengdulbs.model;

/**
 * 字幕实体类
 * 用于存储每一句歌词/字幕的时间和文本
 */
public class SubtitleItem {
    public long startTime; // 开始时间 (毫秒)
    public long endTime;   // 结束时间 (毫秒)
    public String text;    // 字幕内容

    public SubtitleItem(long startTime, long endTime, String text) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }
}