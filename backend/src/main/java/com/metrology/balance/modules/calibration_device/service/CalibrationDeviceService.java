package com.metrology.balance.modules.calibration_device.service;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.CalibrationDevice;
import com.metrology.balance.entity.CalibrationResult;
import com.metrology.balance.modules.calibration_device.model.CalibrationReport;
import com.metrology.balance.modules.calibration_device.model.VibrationIsolationSystem;
import com.metrology.balance.modules.calibration_device.model.VibrationLevel;
import com.metrology.balance.repository.BalanceRepository;
import com.metrology.balance.repository.CalibrationDeviceRepository;
import com.metrology.balance.repository.CalibrationResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CalibrationDeviceService {

    @Autowired
    private CalibrationDeviceRepository deviceRepository;

    @Autowired
    private CalibrationResultRepository resultRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    private static final double[] CALIBRATION_MASSES = {0.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0};

    private static final Map<String, VibrationLevel> VIBRATION_LEVELS = new LinkedHashMap<>();
    private static final Map<String, VibrationIsolationSystem> ISOLATION_SYSTEMS = new LinkedHashMap<>();

    static {
        VIBRATION_LEVELS.put("VC_A", VibrationLevel.builder()
                .code("VC-A")
                .label("极安静实验室")
                .displacementX(0.5e-6)
                .displacementY(1.25e-6)
                .displacementZ(0.25e-6)
                .build());
        VIBRATION_LEVELS.put("VC_B", VibrationLevel.builder()
                .code("VC-B")
                .label("安静实验室")
                .displacementX(1.0e-6)
                .displacementY(2.5e-6)
                .displacementZ(0.5e-6)
                .build());
        VIBRATION_LEVELS.put("VC_C", VibrationLevel.builder()
                .code("VC-C")
                .label("标准实验室")
                .displacementX(2.0e-6)
                .displacementY(5.0e-6)
                .displacementZ(1.0e-6)
                .build());
        VIBRATION_LEVELS.put("VC_D", VibrationLevel.builder()
                .code("VC-D")
                .label("一般工作区")
                .displacementX(5.0e-6)
                .displacementY(12.5e-6)
                .displacementZ(2.5e-6)
                .build());
        VIBRATION_LEVELS.put("VC_E", VibrationLevel.builder()
                .code("VC-E")
                .label("工业环境")
                .displacementX(12.5e-6)
                .displacementY(25.0e-6)
                .displacementZ(5.0e-6)
                .build());
        VIBRATION_LEVELS.put("WORKSHOP", VibrationLevel.builder()
                .code("WORKSHOP")
                .label("普通车间")
                .displacementX(25.0e-6)
                .displacementY(50.0e-6)
                .displacementZ(12.5e-6)
                .build());

        ISOLATION_SYSTEMS.put("NONE", VibrationIsolationSystem.builder()
                .code("NONE")
                .name("无减振")
                .transmissibilityX(1.0)
                .transmissibilityY(1.0)
                .transmissibilityZ(1.0)
                .isolationEfficiencyDb(0.0)
                .build());
        ISOLATION_SYSTEMS.put("PASSIVE_RUBBER", VibrationIsolationSystem.builder()
                .code("PASSIVE_RUBBER")
                .name("橡胶垫被动减振")
                .transmissibilityX(0.4)
                .transmissibilityY(0.5)
                .transmissibilityZ(0.6)
                .isolationEfficiencyDb(3.5)
                .build());
        ISOLATION_SYSTEMS.put("PASSIVE_AIR", VibrationIsolationSystem.builder()
                .code("PASSIVE_AIR")
                .name("空气弹簧被动减振")
                .transmissibilityX(0.1)
                .transmissibilityY(0.15)
                .transmissibilityZ(0.2)
                .isolationEfficiencyDb(6.0)
                .build());
        ISOLATION_SYSTEMS.put("ACTIVE_PIEZO", VibrationIsolationSystem.builder()
                .code("ACTIVE_PIEZO")
                .name("压电陶瓷主动减振")
                .transmissibilityX(0.03)
                .transmissibilityY(0.05)
                .transmissibilityZ(0.08)
                .isolationEfficiencyDb(10.0)
                .build());
        ISOLATION_SYSTEMS.put("ACTIVE_MAGNETIC", VibrationIsolationSystem.builder()
                .code("ACTIVE_MAGNETIC")
                .name("磁悬浮主动减振")
                .transmissibilityX(0.01)
                .transmissibilityY(0.02)
                .transmissibilityZ(0.03)
                .isolationEfficiencyDb(15.0)
                .build());
    }

    @Transactional
    public CalibrationResult calibrateBalance(Integer deviceId, Integer balanceId, String method) {
        CalibrationDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("校准装置不存在: " + deviceId));

        Balance balance = null;
        if (balanceId != null) {
            balance = balanceRepository.findById(balanceId.longValue())
                    .orElseThrow(() -> new IllegalArgumentException("天平不存在: " + balanceId));
        }

        CalibrationResult result = new CalibrationResult();
        result.setDeviceId(deviceId);
        result.setBalanceId(balanceId);
        result.setCalibrationTime(LocalDateTime.now());
        result.setCalibrationMethod(method != null ? method : "MULTI_POSITION_LEVER");

        double leftArm = device.getLeftArmLength();
        double rightArm = device.getRightArmLength();

        double armRatio = rightArm / leftArm;
        double ratioCorrection = 1.0 - armRatio;
        result.setArmLengthRatioCorrection(ratioCorrection);

        Map<String, Object> positionsData = new LinkedHashMap<>();
        Map<String, Object> correctionTable = new LinkedHashMap<>();
        Map<String, Object> rawMeasurements = new LinkedHashMap<>();

        double[] nominalMasses = CALIBRATION_MASSES;
        double[] leftMeasurements = new double[nominalMasses.length];
        double[] rightMeasurements = new double[nominalMasses.length];

        Random random = new Random(42);
        double noiseScale = device.getMinReadability() * 2;

        String envVibLevel = determineEnvironmentVibrationLevel(device);
        String isolationType = determineIsolationSystem(device);
        VibrationLevel vibLevel = VIBRATION_LEVELS.getOrDefault(envVibLevel, VIBRATION_LEVELS.get("VC_C"));
        VibrationIsolationSystem isolation = ISOLATION_SYSTEMS.getOrDefault(isolationType, ISOLATION_SYSTEMS.get("NONE"));

        double residualVibrationX = vibLevel.getDisplacementX() * isolation.getTransmissibilityX();
        double residualVibrationY = vibLevel.getDisplacementY() * isolation.getTransmissibilityY();
        double residualVibrationZ = vibLevel.getDisplacementZ() * isolation.getTransmissibilityZ();
        double rmsVibration = Math.sqrt(residualVibrationX * residualVibrationX
                + residualVibrationY * residualVibrationY
                + residualVibrationZ * residualVibrationZ) / Math.sqrt(3.0);

        double knifeRadius = device.getKnifeEdgeRadius() != null ? device.getKnifeEdgeRadius() : 0.5;
        double vibrationInducedErrorMm = rmsVibration * 1000.0 / (knifeRadius * 1000.0);
        double vibrationNoiseScale = Math.max(noiseScale * 0.5, vibrationInducedErrorMm * 50.0);

        double combinedNoiseScale = Math.sqrt(noiseScale * noiseScale + vibrationNoiseScale * vibrationNoiseScale);

        for (int i = 0; i < nominalMasses.length; i++) {
            double m = nominalMasses[i];
            double idealLeft = m * armRatio;
            double idealRight = m;
            double baseNoise = random.nextGaussian() * noiseScale;
            double vibNoise = random.nextGaussian() * vibrationNoiseScale * Math.sin(2 * Math.PI * i / 3.0);

            leftMeasurements[i] = idealLeft + baseNoise + vibNoise;
            rightMeasurements[i] = idealRight + baseNoise + vibNoise;

            Map<String, Object> posData = new HashMap<>();
            posData.put("nominal", m);
            posData.put("leftReading", leftMeasurements[i]);
            posData.put("rightReading", rightMeasurements[i]);
            posData.put("leftError", leftMeasurements[i] - m);
            posData.put("rightError", rightMeasurements[i] - m);
            posData.put("correction", m * (1 - armRatio));
            positionsData.put("pos_" + i, posData);
        }
        result.setPositionsData(positionsData);

        double leftAvgError = 0;
        double rightAvgError = 0;
        for (int i = 1; i < nominalMasses.length; i++) {
            leftAvgError += (leftMeasurements[i] - nominalMasses[i]);
            rightAvgError += (rightMeasurements[i] - nominalMasses[i]);
        }
        leftAvgError /= (nominalMasses.length - 1);
        rightAvgError /= (nominalMasses.length - 1);

        result.setLeftArmCorrection(leftAvgError);
        result.setRightArmCorrection(rightAvgError);
        result.setZeroPointDrift(leftMeasurements[0]);

        SimpleRegression linearityRegression = new SimpleRegression();
        for (int i = 0; i < nominalMasses.length; i++) {
            double avgReading = (leftMeasurements[i] + rightMeasurements[i]) / 2;
            linearityRegression.addData(nominalMasses[i], avgReading);
        }
        double slope = linearityRegression.getSlope();
        double rSquare = linearityRegression.getRSquare();
        double linearityError = Math.abs(1.0 - slope) * 100;
        result.setLinearityError(linearityError);

        DescriptiveStatistics repeatabilityStats = new DescriptiveStatistics();
        for (int i = 0; i < 10; i++) {
            double m = 100.0;
            double baseNoise = random.nextGaussian() * noiseScale;
            double vibNoise = random.nextGaussian() * vibrationNoiseScale;
            double reading = m + baseNoise + vibNoise;
            repeatabilityStats.addValue(reading);
            rawMeasurements.put("rep_" + i, reading);
        }
        result.setRepeatabilityStd(repeatabilityStats.getStandardDeviation());

        double vibrationUncertainty = vibrationInducedErrorMm / Math.sqrt(3.0);

        double hysteresis = Math.abs(leftMeasurements[nominalMasses.length - 1]
                - leftMeasurements[nominalMasses.length - 2]) / 100.0;
        result.setHysteresisError(hysteresis);

        double combinedUncertainty = Math.sqrt(
                result.getRepeatabilityStd() * result.getRepeatabilityStd() +
                        linearityError * linearityError +
                        result.getZeroPointDrift() * result.getZeroPointDrift() +
                        vibrationUncertainty * vibrationUncertainty * 10000.0
        ) / 100.0;
        double expandedUncertainty = combinedUncertainty * 2.0;
        result.setCorrectedUncertainty(expandedUncertainty);

        Map<String, Object> vibrationAnalysis = new LinkedHashMap<>();
        vibrationAnalysis.put("environmentLevel", envVibLevel);
        vibrationAnalysis.put("environmentLabel", vibLevel.getLabel());
        vibrationAnalysis.put("isolationSystem", isolationType);
        vibrationAnalysis.put("isolationLabel", isolation.getName());
        vibrationAnalysis.put("isolationEfficiency_dB", isolation.getIsolationEfficiencyDb());
        vibrationAnalysis.put("inputVibrationX_um", vibLevel.getDisplacementXUm());
        vibrationAnalysis.put("inputVibrationY_um", vibLevel.getDisplacementYUm());
        vibrationAnalysis.put("inputVibrationZ_um", vibLevel.getDisplacementZUm());
        vibrationAnalysis.put("residualVibrationRMS_um", rmsVibration * 1e6);
        vibrationAnalysis.put("vibrationInducedError_mm", vibrationInducedErrorMm);
        vibrationAnalysis.put("vibrationNoiseScale", vibrationNoiseScale);
        vibrationAnalysis.put("vibrationUncertaintyComponent", vibrationUncertainty);
        rawMeasurements.put("vibrationAnalysis", vibrationAnalysis);

        String grade;
        if (expandedUncertainty < 0.00001) grade = "E1";
        else if (expandedUncertainty < 0.0001) grade = "E2";
        else if (expandedUncertainty < 0.001) grade = "F1";
        else if (expandedUncertainty < 0.01) grade = "F2";
        else if (expandedUncertainty < 0.1) grade = "M1";
        else grade = "M2";
        result.setCalibrationGrade(grade);

        for (int i = 0; i < nominalMasses.length; i++) {
            double m = nominalMasses[i];
            double correction = -leftAvgError * (m / 100.0);
            Map<String, Object> entry = new HashMap<>();
            entry.put("nominal", m);
            entry.put("correctionValue", correction);
            entry.put("uncertainty", expandedUncertainty * m);
            entry.put("kFactor", 2.0);
            correctionTable.put(String.valueOf(m), entry);
        }
        result.setCorrectionTable(correctionTable);
        result.setRawMeasurements(rawMeasurements);

        result = resultRepository.save(result);

        if (balance != null) {
            log.info("校准完成: device={}, balance={}, grade={}, U={}",
                    device.getDeviceCode(), balance.getBalanceCode(), grade, expandedUncertainty);
        } else {
            log.info("校准完成: device={}, grade={}, U={}",
                    device.getDeviceCode(), grade, expandedUncertainty);
        }

        return result;
    }

    public List<CalibrationDevice> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Optional<CalibrationDevice> getDevice(Integer id) {
        return deviceRepository.findById(id);
    }

    public List<CalibrationResult> getCalibrationHistory(Integer balanceId) {
        return resultRepository.findByBalanceIdOrderByCalibrationTimeDesc(balanceId);
    }

    public Optional<CalibrationResult> getLatestCalibration(Integer balanceId) {
        return resultRepository.findTopByBalanceIdOrderByCalibrationTimeDesc(balanceId);
    }

    public CalibrationReport generateCalibrationReport(Integer resultId) {
        CalibrationResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("校准结果不存在: " + resultId));

        CalibrationDevice device = deviceRepository.findById(result.getDeviceId())
                .orElse(null);

        CalibrationReport.DeviceInfo deviceInfo = null;
        if (device != null) {
            deviceInfo = CalibrationReport.DeviceInfo.builder()
                    .deviceName(device.getDeviceName())
                    .deviceType(device.getDeviceType())
                    .leftArmLength(device.getLeftArmLength())
                    .rightArmLength(device.getRightArmLength())
                    .armRatio(device.getRightArmLength() / device.getLeftArmLength())
                    .knifeEdgeRadius(device.getKnifeEdgeRadius())
                    .maxCapacity(device.getMaxCapacity())
                    .minReadability(device.getMinReadability())
                    .material(device.getMaterial())
                    .build();
        }

        CalibrationReport.CalibrationResults calibrationResults = CalibrationReport.CalibrationResults.builder()
                .calibrationGrade(result.getCalibrationGrade())
                .expandedUncertainty(result.getCorrectedUncertainty())
                .kFactor(2.0)
                .confidenceLevel("95%")
                .leftArmCorrection(result.getLeftArmCorrection())
                .rightArmCorrection(result.getRightArmCorrection())
                .armRatioCorrection(result.getArmLengthRatioCorrection())
                .zeroPointDrift(result.getZeroPointDrift())
                .linearityError(result.getLinearityError())
                .repeatabilityStd(result.getRepeatabilityStd())
                .hysteresisError(result.getHysteresisError())
                .correctionTable(result.getCorrectionTable())
                .positionsData(result.getPositionsData())
                .build();

        List<String> conclusions = new ArrayList<>();
        if ("E1".equals(result.getCalibrationGrade()) || "E2".equals(result.getCalibrationGrade())) {
            conclusions.add("本装置达到国家最高等级计量标准，可用于一等砝码检定");
        } else if ("F1".equals(result.getCalibrationGrade())) {
            conclusions.add("本装置达到高精度工作标准，可用于二等砝码及精密天平校准");
        } else {
            conclusions.add("本装置适合一般商业和工业计量用途");
        }

        double ratio = result.getArmLengthRatioCorrection();
        if (Math.abs(ratio) < 0.0001) {
            conclusions.add("等臂性优秀，两臂长度偏差小于0.01%");
        } else if (Math.abs(ratio) < 0.001) {
            conclusions.add("等臂性良好，两臂长度偏差小于0.1%");
        } else {
            conclusions.add("等臂性需要改善，建议进行机械调整");
        }

        conclusions.add("基于古代杠杆原理的现代校准装置，充分体现了中国古代等臂天平的科学价值");
        conclusions.add("校准结果可溯源至国家质量基准");

        List<CalibrationReport.UncertaintySource> uncertaintyBudget = generateUncertaintyBudget(result);

        CalibrationReport.VibrationAnalysis vibrationAnalysis = null;
        if (result.getRawMeasurements() != null && result.getRawMeasurements().containsKey("vibrationAnalysis")) {
            Map<String, Object> va = (Map<String, Object>) result.getRawMeasurements().get("vibrationAnalysis");
            vibrationAnalysis = CalibrationReport.VibrationAnalysis.builder()
                    .environmentLevel((String) va.get("environmentLevel"))
                    .environmentLabel((String) va.get("environmentLabel"))
                    .isolationSystem((String) va.get("isolationSystem"))
                    .isolationLabel((String) va.get("isolationLabel"))
                    .isolationEfficiencyDb(va.get("isolationEfficiency_dB") != null ? ((Number) va.get("isolationEfficiency_dB")).doubleValue() : null)
                    .inputVibrationXUm(va.get("inputVibrationX_um") != null ? ((Number) va.get("inputVibrationX_um")).doubleValue() : null)
                    .inputVibrationYUm(va.get("inputVibrationY_um") != null ? ((Number) va.get("inputVibrationY_um")).doubleValue() : null)
                    .inputVibrationZUm(va.get("inputVibrationZ_um") != null ? ((Number) va.get("inputVibrationZ_um")).doubleValue() : null)
                    .residualVibrationRMSUm(va.get("residualVibrationRMS_um") != null ? ((Number) va.get("residualVibrationRMS_um")).doubleValue() : null)
                    .vibrationInducedErrorMm(va.get("vibrationInducedError_mm") != null ? ((Number) va.get("vibrationInducedError_mm")).doubleValue() : null)
                    .vibrationNoiseScale(va.get("vibrationNoiseScale") != null ? ((Number) va.get("vibrationNoiseScale")).doubleValue() : null)
                    .vibrationUncertaintyComponent(va.get("vibrationUncertaintyComponent") != null ? ((Number) va.get("vibrationUncertaintyComponent")).doubleValue() : null)
                    .build();
        }

        return CalibrationReport.builder()
                .reportTitle("古代杠杆原理天平校准证书")
                .reportNumber("CAL-" + System.currentTimeMillis())
                .calibrationDate(result.getCalibrationTime())
                .calibrationMethod(result.getCalibrationMethod())
                .deviceInfo(deviceInfo)
                .calibrationResults(calibrationResults)
                .conclusions(conclusions)
                .uncertaintyBudget(uncertaintyBudget)
                .vibrationAnalysis(vibrationAnalysis)
                .build();
    }

    private List<CalibrationReport.UncertaintySource> generateUncertaintyBudget(CalibrationResult result) {
        List<CalibrationReport.UncertaintySource> budget = new ArrayList<>();

        addUncertaintySource(budget, "重复性测量", result.getRepeatabilityStd(), "A类", 9);
        addUncertaintySource(budget, "线性误差", result.getLinearityError() / 100.0, "B类", 50);
        addUncertaintySource(budget, "零点漂移", Math.abs(result.getZeroPointDrift()) / 100.0, "B类", 20);
        addUncertaintySource(budget, "滞后误差", result.getHysteresisError(), "B类", 10);
        addUncertaintySource(budget, "环境振动影响", 0.00008, "B类", 30);
        addUncertaintySource(budget, "标准砝码不确定度", 0.0001, "B类", 50);
        addUncertaintySource(budget, "温度影响", 0.00005, "B类", 50);
        addUncertaintySource(budget, "湿度影响", 0.00002, "B类", 50);

        return budget;
    }

    private String determineEnvironmentVibrationLevel(CalibrationDevice device) {
        Double minRead = device.getMinReadability();
        String type = device.getDeviceType();
        if (minRead != null && minRead < 1e-5) return "VC_A";
        if (minRead != null && minRead < 1e-4) return "VC_B";
        if ("PRECISION_CALIBRATOR".equals(type)) return "VC_C";
        if ("STANDARD_CALIBRATOR".equals(type)) return "VC_D";
        return "VC_E";
    }

    private String determineIsolationSystem(CalibrationDevice device) {
        Double minRead = device.getMinReadability();
        if (minRead != null && minRead < 1e-5) return "ACTIVE_MAGNETIC";
        if (minRead != null && minRead < 1e-4) return "ACTIVE_PIEZO";
        if (minRead != null && minRead < 1e-3) return "PASSIVE_AIR";
        if (minRead != null && minRead < 1e-2) return "PASSIVE_RUBBER";
        return "NONE";
    }

    public Map<String, Object> getVibrationMetadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("vibrationLevels", VIBRATION_LEVELS.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("code", e.getValue().getCode());
                    info.put("label", e.getValue().getLabel());
                    info.put("displacementX_um", e.getValue().getDisplacementXUm());
                    info.put("displacementY_um", e.getValue().getDisplacementYUm());
                    info.put("displacementZ_um", e.getValue().getDisplacementZUm());
                    info.put("rmsDisplacement_um", e.getValue().getRmsDisplacementUm());
                    return info;
                })));
        meta.put("isolationSystems", ISOLATION_SYSTEMS.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("code", e.getValue().getCode());
                    info.put("name", e.getValue().getName());
                    info.put("transmissibilityX", e.getValue().getTransmissibilityX());
                    info.put("transmissibilityY", e.getValue().getTransmissibilityY());
                    info.put("transmissibilityZ", e.getValue().getTransmissibilityZ());
                    info.put("isolationEfficiency_dB", e.getValue().getIsolationEfficiencyDb());
                    info.put("isolationEfficiency_percent", e.getValue().getIsolationEfficiencyPercent());
                    return info;
                })));
        return meta;
    }

    public Map<String, Object> simulateVibrationImpact(String vibLevel, String isolationType, double knifeRadius) {
        VibrationLevel level = VIBRATION_LEVELS.getOrDefault(vibLevel, VIBRATION_LEVELS.get("VC_C"));
        VibrationIsolationSystem isolation = ISOLATION_SYSTEMS.getOrDefault(isolationType, ISOLATION_SYSTEMS.get("NONE"));

        VibrationLevel residualVibration = isolation.applyIsolation(level);

        double rmsVibration = residualVibration.getRmsDisplacement();
        double vibrationInducedErrorMm = rmsVibration * 1000.0 / (knifeRadius * 1000.0);
        double relativeError = vibrationInducedErrorMm / knifeRadius;

        double vibrationNoiseScale = Math.max(1e-6, vibrationInducedErrorMm * 50.0);
        double vibrationUncertainty = vibrationInducedErrorMm / Math.sqrt(3.0);

        String achievableGrade;
        double expandedUncertainty = vibrationUncertainty * 2.0 * 100.0;
        if (expandedUncertainty < 0.00001) achievableGrade = "E1";
        else if (expandedUncertainty < 0.0001) achievableGrade = "E2";
        else if (expandedUncertainty < 0.001) achievableGrade = "F1";
        else if (expandedUncertainty < 0.01) achievableGrade = "F2";
        else if (expandedUncertainty < 0.1) achievableGrade = "M1";
        else achievableGrade = "M2";

        Random random = new Random(12345);
        double[] testMasses = {10.0, 50.0, 100.0, 200.0, 500.0};
        List<Map<String, Object>> simulationData = new ArrayList<>();
        for (double mass : testMasses) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("nominalMass", mass);
            double baseReading = mass;
            double[] readings = new double[10];
            double sumError = 0.0;
            for (int i = 0; i < 10; i++) {
                double noise = random.nextGaussian() * vibrationNoiseScale;
                readings[i] = baseReading + noise;
                sumError += Math.abs(noise);
            }
            point.put("readings", readings);
            point.put("meanError", sumError / 10.0);
            point.put("maxError", Arrays.stream(readings).map(r -> Math.abs(r - mass)).max().orElse(0));
            simulationData.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputVibration", Map.of(
                "level", level.getCode(),
                "label", level.getLabel(),
                "displacementX_um", level.getDisplacementXUm(),
                "displacementY_um", level.getDisplacementYUm(),
                "displacementZ_um", level.getDisplacementZUm(),
                "rmsDisplacement_um", level.getRmsDisplacementUm()
        ));
        result.put("isolationSystem", Map.of(
                "type", isolation.getCode(),
                "name", isolation.getName(),
                "transmissibilityX", isolation.getTransmissibilityX(),
                "transmissibilityY", isolation.getTransmissibilityY(),
                "transmissibilityZ", isolation.getTransmissibilityZ(),
                "isolationEfficiency_dB", isolation.getIsolationEfficiencyDb(),
                "isolationEfficiency_percent", isolation.getIsolationEfficiencyPercent()
        ));
        result.put("residualVibration", Map.of(
                "displacementX_um", residualVibration.getDisplacementXUm(),
                "displacementY_um", residualVibration.getDisplacementYUm(),
                "displacementZ_um", residualVibration.getDisplacementZUm(),
                "rmsDisplacement_um", residualVibration.getRmsDisplacementUm()
        ));
        result.put("knifeEdgeRadius_mm", knifeRadius);
        result.put("vibrationInducedError_mm", vibrationInducedErrorMm);
        result.put("relativeError", relativeError);
        result.put("vibrationNoiseScale", vibrationNoiseScale);
        result.put("vibrationUncertaintyComponent", vibrationUncertainty);
        result.put("achievableGrade", achievableGrade);
        result.put("expandedUncertaintyEstimate", expandedUncertainty);
        result.put("simulationData", simulationData);
        result.put("assessment", generateVibrationAssessment(vibrationInducedErrorMm, knifeRadius, achievableGrade));

        return result;
    }

    private String generateVibrationAssessment(double vibrationErrorMm, double knifeRadius, String grade) {
        double relativeError = vibrationErrorMm / knifeRadius * 100.0;
        if (relativeError < 0.001) {
            return "振动影响极小，可忽略不计，完全满足" + grade + "级精度要求";
        } else if (relativeError < 0.01) {
            return "振动影响较小，在可接受范围内，满足" + grade + "级精度要求";
        } else if (relativeError < 0.1) {
            return "振动影响中等，需关注，建议采用更高级的减振系统以提升精度";
        } else if (relativeError < 1.0) {
            return "振动影响较大，可能影响测量准确性，强烈建议升级减振系统";
        } else {
            return "振动影响严重，无法保证测量精度，必须采取有效减振措施";
        }
    }

    private void addUncertaintySource(List<CalibrationReport.UncertaintySource> budget, String source,
                                       double value, String type, int dof) {
        budget.add(CalibrationReport.UncertaintySource.builder()
                .source(source)
                .value(value)
                .type(type)
                .degreesOfFreedom(dof)
                .sensitivity(1.0)
                .contribution(value * value)
                .build());
    }

    public Map<String, Object> getCalibrationGrades() {
        Map<String, Object> grades = new LinkedHashMap<>();
        grades.put("grades", List.of(
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
        ));
        grades.put("kFactor", 2.0);
        grades.put("confidenceLevel", "95%");
        grades.put("standard", "OIML R111");
        return grades;
    }
}
