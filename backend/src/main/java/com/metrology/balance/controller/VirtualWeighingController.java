package com.metrology.balance.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.dto.VirtualWeighingResult;
import com.metrology.balance.entity.VirtualWeighingItem;
import com.metrology.balance.modules.virtual_weighing.VirtualWeighingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/virtual-weighing")
public class VirtualWeighingController {

    @Autowired
    private VirtualWeighingService weighingService;

    @GetMapping("/items")
    public Result<List<VirtualWeighingItem>> getAllItems() {
        List<VirtualWeighingItem> items = weighingService.getAllItems();
        return Result.success(items);
    }

    @GetMapping("/items/category/{category}")
    public Result<List<VirtualWeighingItem>> getItemsByCategory(@PathVariable String category) {
        List<VirtualWeighingItem> items = weighingService.getItemsByCategory(category);
        return Result.success(items);
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        List<String> categories = weighingService.getCategories();
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

            VirtualWeighingResult result = weighingService.performWeighing(
                    leftItemIds, rightItemIds, balanceId, balanceType);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("称量失败: " + e.getMessage());
        }
    }

    @GetMapping("/context/{civilization}")
    public Result<Map<String, Object>> getHistoricalContext(@PathVariable String civilization) {
        Map<String, Object> context = weighingService.getHistoricalContext(civilization);
        return Result.success(context);
    }

    @GetMapping("/lever-principle")
    public Result<Map<String, Object>> getLeverPrincipleExplanation() {
        Map<String, Object> explanation = weighingService.getLeverPrincipleExplanation();
        return Result.success(explanation);
    }

    @GetMapping("/quick-experience")
    public Result<VirtualWeighingResult> quickExperience() {
        try {
            List<Integer> leftIds = List.of(1);
            List<Integer> rightIds = List.of(7, 8, 9);

            VirtualWeighingResult result = weighingService.performWeighing(
                    leftIds, rightIds, null, "EQUAL_ARM");
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("体验失败: " + e.getMessage());
        }
    }
}
