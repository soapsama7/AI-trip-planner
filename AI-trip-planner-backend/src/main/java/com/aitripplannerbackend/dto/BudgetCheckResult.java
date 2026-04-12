package com.aitripplannerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 预算校验结果。
 *
 * 三种级别：
 * <ul>
 *   <li>OK — 预算合理，正常生成</li>
 *   <li>WARNED — 预算偏低但可执行，附带警告文案</li>
 *   <li>REJECTED — 预算严重不足，直接拒绝</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@Builder
public class BudgetCheckResult {

    private String level;
    private String message;
    private int perDayBudget;

    public boolean isRejected() {
        return "REJECTED".equals(level);
    }

    public boolean isWarned() {
        return "WARNED".equals(level);
    }

    public static BudgetCheckResult ok(int perDayBudget) {
        return new BudgetCheckResult("OK", null, perDayBudget);
    }

    public static BudgetCheckResult warned(String message, int perDayBudget) {
        return new BudgetCheckResult("WARNED", message, perDayBudget);
    }

    public static BudgetCheckResult rejected(String message, int perDayBudget) {
        return new BudgetCheckResult("REJECTED", message, perDayBudget);
    }
}
