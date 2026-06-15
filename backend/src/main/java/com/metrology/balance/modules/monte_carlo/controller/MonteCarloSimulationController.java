package com.metrology.balance.modules.monte_carlo.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.modules.monte_carlo.model.MonteCarloSimulationResult;
import com.metrology.balance.modules.monte_carlo.model.SimulationParameter;
import com.metrology.balance.modules.monte_carlo.service.MonteCarloSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monte-carlo")
public class MonteCarloSimulationController {

    @Autowired
    private MonteCarloSimulationService simulationService;

    @PostMapping("/simulate")
    public Result<Map<String, Object>> startSimulation(@RequestBody(required = false) Map<String, Object> request) {
        try {
            int count = request != null && request.get("simulationCount") != null ?
                    Integer.parseInt(request.get("simulationCount").toString()) : 10000;

            String simulationId = simulationService.generateSimulationId();
            List<SimulationParameter> parameters = simulationService.getDefaultBalanceParameters();

            simulationService.runBalanceErrorSimulation(simulationId, parameters, count);

            return Result.success(Map.of(
                    "simulationId", simulationId,
                    "status", "RUNNING",
                    "message", "模拟已启动，请使用状态查询接口获取结果"
            ));
        } catch (Exception e) {
            return Result.error("模拟启动失败: " + e.getMessage());
        }
    }

    @GetMapping("/result/{simulationId}")
    public Result<MonteCarloSimulationResult> getResult(@PathVariable String simulationId) {
        MonteCarloSimulationResult result = simulationService.getSimulationResult(simulationId);
        if (result == null) {
            return Result.error("模拟任务不存在: " + simulationId);
        }
        return Result.success(result);
    }

    @GetMapping("/status/{simulationId}")
    public Result<Map<String, Object>> getStatus(@PathVariable String simulationId) {
        MonteCarloSimulationResult result = simulationService.getSimulationResult(simulationId);
        if (result == null) {
            return Result.error("模拟任务不存在: " + simulationId);
        }
        double progress = result.getTotalSimulations() > 0
                ? (double) result.getCompletedSimulations() / result.getTotalSimulations() * 100.0
                : 0.0;
        return Result.success(Map.of(
                "simulationId", simulationId,
                "status", result.getStatus(),
                "progress", progress,
                "completed", result.getCompletedSimulations(),
                "total", result.getTotalSimulations(),
                "durationMs", result.getDurationMs() != null ? result.getDurationMs() : 0
        ));
    }

    @GetMapping("/running")
    public Result<List<MonteCarloSimulationResult>> getRunningSimulations() {
        return Result.success(simulationService.getRunningSimulations());
    }

    @GetMapping("/parameters/default")
    public Result<List<SimulationParameter>> getDefaultParameters() {
        return Result.success(simulationService.getDefaultBalanceParameters());
    }

    @GetMapping("/pool-info")
    public Result<Map<String, Object>> getThreadPoolInfo() {
        return Result.success(Map.of(
                "corePoolSize", 4,
                "maxPoolSize", 8,
                "queueCapacity", 100,
                "threadPrefix", "monte-carlo-",
                "description", "蒙特卡洛模拟专用线程池，用于执行大规模随机模拟任务"
        ));
    }
}
