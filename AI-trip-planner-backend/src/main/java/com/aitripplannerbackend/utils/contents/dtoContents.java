package com.aitripplannerbackend.utils.contents;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DTO 相关的常量和工具。
 *
 * 这里放的是多个类都会用到的日期格式化等。
 */
public class dtoContents {

    /**
     * 日期格式化器，输出中文格式如 "4月10日 星期五"。
     * {@code Locale.CHINA} 确保星期几显示为中文而不是英文。
     *
     * 用法：{@code localDate.format(DAY_FMT)} → "4月10日 星期五"
     */
    public static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA);
}
