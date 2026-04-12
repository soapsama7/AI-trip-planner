package com.aitripplannerbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史行程分页响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "历史行程分页结果")
public class TripHistoryPageResponse {

    /** 当前页码（从 1 开始） */
    @Schema(description = "当前页码", example = "1")
    private Integer page;

    /** 每页数量 */
    @Schema(description = "每页条数", example = "5")
    private Integer size;

    /** 总记录数 */
    @Schema(description = "总记录数", example = "42")
    private Long total;

    /** 总页数 */
    @Schema(description = "总页数", example = "9")
    private Integer totalPages;

    /** 当前页数据 */
    @Schema(description = "当前页记录列表")
    private List<TripHistoryItem> records;
}
