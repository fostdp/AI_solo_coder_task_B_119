package com.metrology.balance.modules.craft_inferrer.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.modules.craft_inferrer.model.CraftMethodKnowledge;
import com.metrology.balance.modules.craft_inferrer.model.LiteratureReference;
import com.metrology.balance.modules.craft_inferrer.service.CraftInferrerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/craft-inferrer")
public class CraftInferrerController {

    @Autowired
    private CraftInferrerService craftInferrerService;

    @PostMapping("/infer/{balanceId}")
    public Result<Map<String, Object>> inferCraftMethod(@PathVariable Long balanceId) {
        try {
            Map<String, Object> result = craftInferrerService.inferCraftMethod(balanceId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("工艺推断失败: " + e.getMessage());
        }
    }

    @GetMapping("/craft-methods")
    public Result<List<CraftMethodKnowledge>> getCraftMethods() {
        try {
            List<CraftMethodKnowledge> methods = craftInferrerService.getAllCraftMethods();
            return Result.success(methods);
        } catch (Exception e) {
            return Result.error("获取工艺方法失败: " + e.getMessage());
        }
    }

    @GetMapping("/literature-references")
    public Result<List<LiteratureReference>> getLiteratureReferences() {
        try {
            List<LiteratureReference> references = craftInferrerService.getAllLiteratureReferences();
            return Result.success(references);
        } catch (Exception e) {
            return Result.error("获取文献数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/monte-carlo/{balanceId}")
    public Result<String> runMonteCarloSimulation(
            @PathVariable Long balanceId,
            @RequestParam(defaultValue = "10000") int simCount) {
        try {
            if (simCount <= 0 || simCount > 1000000) {
                return Result.error("模拟次数必须在 1 到 1000000 之间");
            }
            craftInferrerService.runMonteCarloSimulation(balanceId, simCount);
            return Result.success("蒙特卡洛模拟已启动，请稍后查询结果");
        } catch (Exception e) {
            return Result.error("启动蒙特卡洛模拟失败: " + e.getMessage());
        }
    }

    @GetMapping("/monte-carlo/status/{balanceId}")
    public Result<Map<String, Object>> getMonteCarloStatus(@PathVariable Long balanceId) {
        try {
            Map<String, Object> status = craftInferrerService.getMonteCarloStatus(balanceId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("获取蒙特卡洛模拟状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/monte-carlo/result/{balanceId}")
    public Result<CraftInferrerService.MonteCarloResult> getMonteCarloResult(@PathVariable Long balanceId) {
        try {
            CraftInferrerService.MonteCarloResult result = craftInferrerService.getMonteCarloResult(balanceId);
            if (result == null) {
                return Result.error("尚未执行蒙特卡洛模拟");
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取蒙特卡洛模拟结果失败: " + e.getMessage());
        }
    }
}
