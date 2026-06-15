package com.metrology.balance.modules.calibration_device.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.entity.CalibrationDevice;
import com.metrology.balance.entity.CalibrationResult;
import com.metrology.balance.modules.calibration_device.model.CalibrationReport;
import com.metrology.balance.modules.calibration_device.service.CalibrationDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calibration-device")
public class CalibrationDeviceController {

    @Autowired
    private CalibrationDeviceService calibrationDeviceService;

    @GetMapping("/devices")
    public Result<List<CalibrationDevice>> getAllDevices() {
        List<CalibrationDevice> devices = calibrationDeviceService.getAllDevices();
        return Result.success(devices);
    }

    @GetMapping("/devices/{id}")
    public Result<CalibrationDevice> getDevice(@PathVariable Integer id) {
        return calibrationDeviceService.getDevice(id)
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

            CalibrationResult result = calibrationDeviceService.calibrateBalance(deviceId, balanceId, method);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("校准失败: " + e.getMessage());
        }
    }

    @GetMapping("/history/{balanceId}")
    public Result<List<CalibrationResult>> getCalibrationHistory(@PathVariable Integer balanceId) {
        List<CalibrationResult> history = calibrationDeviceService.getCalibrationHistory(balanceId);
        return Result.success(history);
    }

    @GetMapping("/latest/{balanceId}")
    public Result<CalibrationResult> getLatestCalibration(@PathVariable Integer balanceId) {
        return calibrationDeviceService.getLatestCalibration(balanceId)
                .map(Result::success)
                .orElse(Result.error("暂无校准记录"));
    }

    @GetMapping("/report/{resultId}")
    public Result<CalibrationReport> getCalibrationReport(@PathVariable Integer resultId) {
        try {
            CalibrationReport report = calibrationDeviceService.generateCalibrationReport(resultId);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error("报告生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/grades")
    public Result<Map<String, Object>> getCalibrationGrades() {
        Map<String, Object> grades = calibrationDeviceService.getCalibrationGrades();
        return Result.success(grades);
    }

    @GetMapping("/vibration/metadata")
    public Result<Map<String, Object>> getVibrationMetadata() {
        Map<String, Object> metadata = calibrationDeviceService.getVibrationMetadata();
        return Result.success(metadata);
    }

    @GetMapping("/vibration/simulate")
    public Result<Map<String, Object>> simulateVibrationImpact(
            @RequestParam(defaultValue = "VC_C") String vibLevel,
            @RequestParam(defaultValue = "NONE") String isolationType,
            @RequestParam(defaultValue = "0.5") double knifeRadius) {
        try {
            Map<String, Object> result = calibrationDeviceService.simulateVibrationImpact(vibLevel, isolationType, knifeRadius);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("振动模拟失败: " + e.getMessage());
        }
    }
}
