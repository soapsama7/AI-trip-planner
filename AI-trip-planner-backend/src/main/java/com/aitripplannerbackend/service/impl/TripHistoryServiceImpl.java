package com.aitripplannerbackend.service.impl;

import com.aitripplannerbackend.dto.TripHistoryItem;
import com.aitripplannerbackend.dto.TripHistoryPageResponse;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.entity.TripRecordEntity;
import com.aitripplannerbackend.mapper.TripRecordMapper;
import com.aitripplannerbackend.service.TripHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 历史记录查询服务实现。
 *
 * 从 MySQL 的 trip_record 表中分页查询历史记录，
 * 把数据库的 Entity 转换成前端需要的 DTO（TripHistoryItem）。
 *
 * <h3>Entity → DTO 的转换</h3>
 * 数据库里存的 plan_json 是一个 JSON 字符串，
 * 这里会用 ObjectMapper 反序列化成 TripPlanResult 对象，
 * 这样前端拿到的就是结构化的数据，可以直接渲染。
 */
@Service
@RequiredArgsConstructor
public class TripHistoryServiceImpl implements TripHistoryService {

    private final TripRecordMapper tripRecordMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询历史记录。
     *
     * @param page 页码（从 1 开始，Math.max 兜底防止传 0 或负数）
     * @param size 每页数量（同样兜底）
     * @return DTO 列表，plan_json 已反序列化为对象
     */
    @Override
    public TripHistoryPageResponse listHistory(int page, int size) {
        // 做一层安全性校验，防止传入的参数为0,0或其它的非法参数
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;
        long total = tripRecordMapper.countHistory();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        List<TripRecordEntity> rows = tripRecordMapper.selectHistory(offset, safeSize);
        List<TripHistoryItem> result = new ArrayList<>();
        for (TripRecordEntity row : rows) {
            TripHistoryItem item = new TripHistoryItem();
            item.setId(row.getId());
            item.setTaskId(row.getTaskId());
            item.setCity(row.getCity());
            item.setTravelTime(row.getTravelTime());
            item.setBudget(row.getBudget());
            item.setTotalCost(row.getTotalCost());
            item.setCreatedAt(row.getCreatedAt());
            try {
                item.setPlan(objectMapper.readValue(row.getPlanJson(), TripPlanResult.class));
            } catch (Exception ignored) {
                item.setPlan(null);
            }
            result.add(item);
        }
        return TripHistoryPageResponse.builder()
                .page(safePage)
                .size(safeSize)
                .total(total)
                .totalPages(totalPages)
                .records(result)
                .build();
    }
}
