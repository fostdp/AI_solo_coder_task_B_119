package com.metrology.balance.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.dto.CivilizationComparisonResult;
import com.metrology.balance.entity.CivilizationBalance;
import com.metrology.balance.modules.civilization_comparison.CivilizationComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/civilization")
public class CivilizationController {

    @Autowired
    private CivilizationComparisonService comparisonService;

    @GetMapping("/balances")
    public Result<List<CivilizationBalance>> getAllCivilizations() {
        List<CivilizationBalance> civilizations = comparisonService.getAllCivilizations();
        return Result.success(civilizations);
    }

    @GetMapping("/balances/{code}")
    public Result<CivilizationBalance> getCivilization(@PathVariable String code) {
        return comparisonService.getCivilization(code)
                .map(Result::success)
                .orElse(Result.error("文明不存在: " + code));
    }

    @PostMapping("/compare")
    public Result<CivilizationComparisonResult> compareCivilizations(
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            List<String> codes = null;
            if (request != null && request.containsKey("civilizationCodes")) {
                codes = (List<String>) request.get("civilizationCodes");
            }
            CivilizationComparisonResult result = comparisonService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("对比分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/default")
    public Result<CivilizationComparisonResult> compareDefault() {
        try {
            CivilizationComparisonResult result = comparisonService.compareCivilizations(null);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("对比分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/china-rome")
    public Result<CivilizationComparisonResult> compareChinaVsRome() {
        try {
            List<String> codes = List.of("CHN-TANG", "ROME");
            CivilizationComparisonResult result = comparisonService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("中罗马对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/all-eastern")
    public Result<CivilizationComparisonResult> compareAllEastern() {
        try {
            List<String> codes = List.of("CHN-WARRING", "CHN-WEST-HAN", "CHN-TANG", "CHN-MING", "INDIA");
            CivilizationComparisonResult result = comparisonService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("东方文明对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/all-western")
    public Result<CivilizationComparisonResult> compareAllWestern() {
        try {
            List<String> codes = List.of("EGYPT", "BABYLON", "GREECE", "ROME", "EUROPE-MED", "EUROPE-RENAISS");
            CivilizationComparisonResult result = comparisonService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("西方文明对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/dimensions")
    public Result<Map<String, String>> getDimensions() {
        return Result.success(comparisonService.getDimensionLabels());
    }
}
