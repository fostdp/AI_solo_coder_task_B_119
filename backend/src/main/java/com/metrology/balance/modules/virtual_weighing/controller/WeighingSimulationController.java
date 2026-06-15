package com.metrology.balance.modules.virtual_weighing.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.dto.VirtualWeighingResult;
import com.metrology.balance.entity.VirtualWeighingItem;
import com.metrology.balance.modules.virtual_weighing.service.WeighingSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/weighing-simulation")
@RequiredArgsConstructor
public class WeighingSimulationController {

    private final WeighingSimulationService weighingSimulationService;

    @GetMapping("/items")
    public Result<List<VirtualWeighingItem>> getAllItems() {
        List<VirtualWeighingItem> items = weighingSimulationService.getAllItems();
        return Result.success(items);
    }

    @GetMapping("/items/category/{category}")
    public Result<List<VirtualWeighingItem>> getItemsByCategory(@PathVariable String category) {
        List<VirtualWeighingItem> items = weighingSimulationService.getItemsByCategory(category);
        return Result.success(items);
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        List<String> categories = weighingSimulationService.getCategories();
        return Result.success(categories);
    }

    @PostMapping("/weigh")
    public Result<VirtualWeighingResult> performWeighing(@RequestBody Map<String, Object> request) {
        try {
            List<Integer> leftItemIds = (List<Integer>) request.get("leftItemIds");
            List<Integer> rightItemIds = (List<Integer>) request.get("rightItemIds");
            Integer balanceId = request.get("balanceId") != null ?
                    Integer.valueOf(request.get("balanceId").toString()) : null;
            String balanceType = request.get("balanceType") != null ?
                    request.get("balanceType").toString() : "EQUAL_ARM";

            VirtualWeighingResult result = weighingSimulationService.performWeighing(
                    leftItemIds, rightItemIds, balanceId, balanceType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("称量失败", e);
            return Result.error("称量失败: " + e.getMessage());
        }
    }

    @GetMapping("/context/{civilization}")
    public Result<Map<String, Object>> getHistoricalContext(@PathVariable String civilization) {
        Map<String, Object> context = weighingSimulationService.getHistoricalContext(civilization);
        return Result.success(context);
    }

    @GetMapping("/lever-principle")
    public Result<Map<String, Object>> getLeverPrincipleExplanation() {
        Map<String, Object> explanation = weighingSimulationService.getLeverPrincipleExplanation();
        return Result.success(explanation);
    }

    @GetMapping("/quick-experience")
    public Result<VirtualWeighingResult> quickExperience() {
        try {
            List<Integer> leftIds = List.of(1);
            List<Integer> rightIds = List.of(7, 8, 9);

            VirtualWeighingResult result = weighingSimulationService.performWeighing(
                    leftIds, rightIds, null, "EQUAL_ARM");
            return Result.success(result);
        } catch (Exception e) {
            log.error("体验失败", e);
            return Result.error("体验失败: " + e.getMessage());
        }
    }
}
