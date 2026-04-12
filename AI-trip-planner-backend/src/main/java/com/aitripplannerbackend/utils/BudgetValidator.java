package com.aitripplannerbackend.utils;

import com.aitripplannerbackend.dto.BudgetCheckResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.aitripplannerbackend.utils.contents.systemContents.MIN_NIGHTLY_PRICE;
import static com.aitripplannerbackend.utils.contents.systemContents.REJECT_PER_DAY;
import static com.aitripplannerbackend.utils.contents.systemContents.WARN_PER_DAY;

/**
 * 预算合理性两级校验。
 *
 * <ul>
 *   <li>每日预算 &lt; {@link #REJECT_PER_DAY} 元 → 硬拒绝（连基本食宿都无法覆盖）</li>
 *   <li>每日预算 &lt; {@link #WARN_PER_DAY} 元   → 软警告（极度节俭，仍可生成）</li>
 *   <li>其余情况 → 正常通过</li>
 * </ul>
 */
@Data
public class BudgetValidator {

    public static BudgetCheckResult check(BigDecimal budget, String travelTime) {
        int days = parseDays(travelTime);
        int perDay = budget.intValue() / Math.max(days, 1);

        if (perDay < REJECT_PER_DAY) {
            String msg = String.format(
                    "当前日均预算偏低。为便于覆盖餐饮、市内交通等基本开销，建议每日预算不低于 %d 元。",
                    REJECT_PER_DAY);
            return BudgetCheckResult.rejected(msg, perDay);
        }

        if (perDay < WARN_PER_DAY) {
            String msg = String.format(
                    "预算偏低提醒：您的行程共 %d 天，平均每天 %d 元，属于节俭型预算。"
                            + "系统将尽量为您规划经济实惠的行程，但部分收费景点和舒适住宿可能超出预算。",
                    days, perDay);
            return BudgetCheckResult.warned(msg, perDay);
        }

        return BudgetCheckResult.ok(perDay);
    }

    /**
     * 从 "yyyy-MM-dd~yyyy-MM-dd" 格式的 travelTime 中解析出行程天数。
     * 前端保证了传来的travelTime一定非空且格式一致，因此这里不用考虑travelTime为空、格式错误等（若真出现了某些异常状况，返回1天作为兜底）
     */
    public static int parseDays(String travelTime) {
        try {
            String[] parts = travelTime.split("~");
            if (parts.length != 2) {
                return 1;
            }
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            return (int) Math.max(days, 1);
        } catch (Exception e) {
            return 1;
        }
    }
}
