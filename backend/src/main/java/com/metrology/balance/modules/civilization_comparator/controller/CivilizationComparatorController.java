package com.metrology.balance.modules.civilization_comparator.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.dto.CivilizationComparisonResult;
import com.metrology.balance.entity.CivilizationBalance;
import com.metrology.balance.modules.civilization_comparator.model.ExpertValidationResult;
import com.metrology.balance.modules.civilization_comparator.service.CivilizationComparatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/civilization-comparator")
@RequiredArgsConstructor
public class CivilizationComparatorController {

    private final CivilizationComparatorService comparatorService;

    @GetMapping("/civilizations")
    public Result<List<CivilizationBalance>> getAllCivilizations() {
        List<CivilizationBalance> civilizations = comparatorService.getAllCivilizations();
        return Result.success(civilizations);
    }

    @GetMapping("/civilizations/{code}")
    public Result<CivilizationBalance> getCivilization(@PathVariable String code) {
        return comparatorService.getCivilization(code)
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
            CivilizationComparisonResult result = comparatorService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("对比分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/default")
    public Result<CivilizationComparisonResult> compareDefault() {
        try {
            CivilizationComparisonResult result = comparatorService.compareCivilizations(null);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("对比分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/china-rome")
    public Result<CivilizationComparisonResult> compareChinaVsRome() {
        try {
            List<String> codes = List.of("CHN-TANG", "ROME");
            CivilizationComparisonResult result = comparatorService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("中罗马对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/all-eastern")
    public Result<CivilizationComparisonResult> compareAllEastern() {
        try {
            List<String> codes = comparatorService.getEasternCivilizationCodes();
            CivilizationComparisonResult result = comparatorService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("东方文明对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/all-western")
    public Result<CivilizationComparisonResult> compareAllWestern() {
        try {
            List<String> codes = comparatorService.getWesternCivilizationCodes();
            CivilizationComparisonResult result = comparatorService.compareCivilizations(codes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("西方文明对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/east-west")
    public Result<CivilizationComparisonResult> compareEastVsWest() {
        try {
            List<String> easternCodes = comparatorService.getEasternCivilizationCodes();
            List<String> westernCodes = comparatorService.getWesternCivilizationCodes();
            List<String> allCodes = new java.util.ArrayList<>();
            allCodes.addAll(easternCodes);
            allCodes.addAll(westernCodes);
            CivilizationComparisonResult result = comparatorService.compareCivilizations(allCodes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("东西方文明对比失败: " + e.getMessage());
        }
    }

    @GetMapping("/dimensions")
    public Result<Map<String, String>> getDimensions() {
        return Result.success(comparatorService.getDimensionLabels());
    }

    @GetMapping("/standardization/metadata")
    public Result<Map<String, Object>> getStandardizationMetadata() {
        return Result.success(comparatorService.getStandardizationMetadata());
    }

    @GetMapping("/civilization-groups")
    public Result<Map<String, Object>> getCivilizationGroups() {
        return Result.success(comparatorService.getCivilizationGroups());
    }

    @PostMapping("/validate/{code}")
    public Result<ExpertValidationResult> validateCivilizationData(@PathVariable String code) {
        try {
            ExpertValidationResult result = comparatorService.validateCivilizationData(code);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("校验失败: " + e.getMessage());
        }
    }
}
