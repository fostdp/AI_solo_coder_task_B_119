package com.metrology.balance.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.entity.CalibrationDevice;
import com.metrology.balance.entity.CalibrationResult;
import com.metrology.balance.modules.calibration_app.ModernCalibrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calibration")
public class CalibrationController {

    @Autowired
    private ModernCalibrationService calibrationService;

    @GetMapping("/devices")
    public Result<List<CalibrationDevice>> getAllDevices() {
        List<CalibrationDevice> devices = calibrationService.getAllDevices();
        return Result.success(devices);
    }

    @GetMapping("/devices/{id}")
    public Result<CalibrationDevice> getDevice(@PathVariable Integer id) {
        return calibrationService.getDevice(id)
                .map(Result::success)
                .orElse(Result.error("校准装置不存在: " + id));
    }

    @PostMapping("/calibrate")
    public Result<CalibrationResult> calibrateBalance(@RequestBody Map<String, Object> request) {
        try {
            Integer deviceId = request.get("deviceId") != null ?
                    Integer.valueOf(request.get("deviceId").toString()) : 1;
            Integer balanceId = request.get("balanceId") != null ?
                    Integer.valueOf(request.get("balanceId").toString()) : null;
            String method = request.get("method") != null ?
                    request.get("method").toString() : null;

            CalibrationResult result = calibrationService.calibrateBalance(deviceId, balanceId, method);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("校准失败: " + e.getMessage());
        }
    }

    @GetMapping("/history/{balanceId}")
    public Result<List<CalibrationResult>> getCalibrationHistory(@PathVariable Integer balanceId) {
        List<CalibrationResult> history = calibrationService.getCalibrationHistory(balanceId);
        return Result.success(history);
    }

    @GetMapping("/latest/{balanceId}")
    public Result<CalibrationResult> getLatestCalibration(@PathVariable Integer balanceId) {
        return calibrationService.getLatestCalibration(balanceId)
                .map(Result::success)
                .orElse(Result.error("暂无校准记录"));
    }

    @GetMapping("/report/{resultId}")
    public Result<Map<String, Object>> getCalibrationReport(@PathVariable Integer resultId) {
        try {
            Map<String, Object> report = calibrationService.generateCalibrationReport(resultId);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error("报告生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/grades")
    public Result<Map<String, Object>> getCalibrationGrades() {
        Map<String, Object> grades = Map.of(
                "grades", List.of(
                        Map.of("code", "E1", "name", "一等标准", "uncertainty", "<1e-5",
                                "usage", "国家最高计量标准，一等砝码检定"),
                        Map.of("code", "E2", "name", "二等标准", "uncertainty", "<1e-4",
                                "usage", "工作计量标准，二等砝码检定"),
                        Map.of("code", "F1", "name", "一等工作", "uncertainty", "<1e-3",
                                "usage", "精密天平校准，实验室分析"),
                        Map.of("code", "F2", "name", "二等工作", "uncertainty", "<1e-2",
                                "usage", "商业天平校准，一般工业"),
                        Map.of("code", "M1", "name", "普通一级", "uncertainty", "<1e-1",
                                "usage", "市场衡器校准，一般贸易"),
                        Map.of("code", "M2", "name", "普通二级", "uncertainty", "≥1e-1",
                                "usage", "粗糙称量，非贸易用途")
                ),
                "kFactor", 2.0,
                "confidenceLevel", "95%"
        );
        return Result.success(grades);
    }
}
