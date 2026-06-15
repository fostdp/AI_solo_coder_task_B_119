package com.metrology.balance.modules.calibration_app;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.CalibrationDevice;
import com.metrology.balance.entity.CalibrationResult;
import com.metrology.balance.model.KnifeEdgeWearModel;
import com.metrology.balance.repository.BalanceRepository;
import com.metrology.balance.repository.CalibrationDeviceRepository;
import com.metrology.balance.repository.CalibrationResultRepository;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ModernCalibrationService {

    private static final Logger logger = LoggerFactory.getLogger(ModernCalibrationService.class);

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
        VIBRATION_LEVELS.put("VC_A", new VibrationLevel("VC-A", "极安静实验室",
                0.5e-6, 1.25e-6, 0.25e-6));
        VIBRATION_LEVELS.put("VC_B", new VibrationLevel("VC-B", "安静实验室",
                1.0e-6, 2.5e-6, 0.5e-6));
        VIBRATION_LEVELS.put("VC_C", new VibrationLevel("VC-C", "标准实验室",
                2.0e-6, 5.0e-6, 1.0e-6));
        VIBRATION_LEVELS.put("VC_D", new VibrationLevel("VC-D", "一般工作区",
                5.0e-6, 12.5e-6, 2.5e-6));
        VIBRATION_LEVELS.put("VC_E", new VibrationLevel("VC-E", "工业环境",
                12.5e-6, 25.0e-6, 5.0e-6));
        VIBRATION_LEVELS.put("WORKSHOP", new VibrationLevel("WORKSHOP", "普通车间",
                25.0e-6, 50.0e-6, 12.5e-6));

        ISOLATION_SYSTEMS.put("NONE", new VibrationIsolationSystem("无减振",
                1.0, 1.0, 1.0, 0.0));
        ISOLATION_SYSTEMS.put("PASSIVE_RUBBER", new VibrationIsolationSystem("橡胶垫被动减振",
                0.4, 0.5, 0.6, 3.5));
        ISOLATION_SYSTEMS.put("PASSIVE_AIR", new VibrationIsolationSystem("空气弹簧被动减振",
                0.1, 0.15, 0.2, 6.0));
        ISOLATION_SYSTEMS.put("ACTIVE_PIEZO", new VibrationIsolationSystem("压电陶瓷主动减振",
                0.03, 0.05, 0.08, 10.0));
        ISOLATION_SYSTEMS.put("ACTIVE_MAGNETIC", new VibrationIsolationSystem("磁悬浮主动减振",
                0.01, 0.02, 0.03, 15.0));
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

        double residualVibrationX = vibLevel.displacementX * isolation.transmissibilityX;
        double residualVibrationY = vibLevel.displacementY * isolation.transmissibilityY;
        double residualVibrationZ = vibLevel.displacementZ * isolation.transmissibilityZ;
        double rmsVibration = Math.sqrt(residualVibrationX * residualVibrationX
                + residualVibrationY * residualVibrationY
                + residualVibrationZ * residualVibrationZ) / Math.sqrt(3.0);

        double knifeRadius = device.getKnifeEdgeRadius() != null ? device.getKnifeEdgeRadius() : 0.5;
        double vibrationInducedError_mm = rmsVibration * 1000.0 / (knifeRadius * 1000.0);
        double vibrationNoiseScale = Math.max(noiseScale * 0.5, vibrationInducedError_mm * 50.0);

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

        double vibrationUncertainty = vibrationInducedError_mm / Math.sqrt(3.0);

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
        vibrationAnalysis.put("environmentLabel", vibLevel.label);
        vibrationAnalysis.put("isolationSystem", isolationType);
        vibrationAnalysis.put("isolationLabel", isolation.name);
        vibrationAnalysis.put("isolationEfficiency_dB", isolation.isolationEfficiencyDb);
        vibrationAnalysis.put("inputVibrationX_um", vibLevel.displacementX * 1e6);
        vibrationAnalysis.put("inputVibrationY_um", vibLevel.displacementY * 1e6);
        vibrationAnalysis.put("inputVibrationZ_um", vibLevel.displacementZ * 1e6);
        vibrationAnalysis.put("residualVibrationRMS_um", rmsVibration * 1e6);
        vibrationAnalysis.put("vibrationInducedError_mm", vibrationInducedError_mm);
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
            logger.info("校准完成: device={}, balance={}, grade={}, U={:.2e}",
                    device.getDeviceCode(), balance.getBalanceCode(), grade, expandedUncertainty);
        } else {
            logger.info("校准完成: device={}, grade={}, U={:.2e}",
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

    public Map<String, Object> generateCalibrationReport(Integer resultId) {
        CalibrationResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("校准结果不存在: " + resultId));

        CalibrationDevice device = deviceRepository.findById(result.getDeviceId())
                .orElse(null);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportTitle", "古代杠杆原理天平校准证书");
        report.put("reportNumber", "CAL-" + System.currentTimeMillis());
        report.put("calibrationDate", result.getCalibrationTime().toString());
        report.put("calibrationMethod", result.getCalibrationMethod());

        if (device != null) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceName", device.getDeviceName());
            deviceInfo.put("deviceType", device.getDeviceType());
            deviceInfo.put("leftArmLength", device.getLeftArmLength());
            deviceInfo.put("rightArmLength", device.getRightArmLength());
            deviceInfo.put("armRatio", device.getRightArmLength() / device.getLeftArmLength());
            deviceInfo.put("knifeEdgeRadius", device.getKnifeEdgeRadius());
            deviceInfo.put("maxCapacity", device.getMaxCapacity());
            deviceInfo.put("minReadability", device.getMinReadability());
            deviceInfo.put("material", device.getMaterial());
            report.put("deviceInfo", deviceInfo);
        }

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("calibrationGrade", result.getCalibrationGrade());
        results.put("expandedUncertainty", result.getCorrectedUncertainty());
        results.put("kFactor", 2.0);
        results.put("confidenceLevel", "95%");
        results.put("leftArmCorrection", result.getLeftArmCorrection());
        results.put("rightArmCorrection", result.getRightArmCorrection());
        results.put("armRatioCorrection", result.getArmLengthRatioCorrection());
        results.put("zeroPointDrift", result.getZeroPointDrift());
        results.put("linearityError", result.getLinearityError());
        results.put("repeatabilityStd", result.getRepeatabilityStd());
        results.put("hysteresisError", result.getHysteresisError());
        results.put("correctionTable", result.getCorrectionTable());
        report.put("calibrationResults", results);

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
        report.put("conclusions", conclusions);

        report.put("uncertaintyBudget", generateUncertaintyBudget(result));

        return report;
    }

    private List<Map<String, Object>> generateUncertaintyBudget(CalibrationResult result) {
        List<Map<String, Object>> budget = new ArrayList<>();

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
                    info.put("label", e.getValue().label);
                    info.put("displacementX_um", e.getValue().displacementX * 1e6);
                    info.put("displacementY_um", e.getValue().displacementY * 1e6);
                    info.put("displacementZ_um", e.getValue().displacementZ * 1e6);
                    return info;
                })));
        meta.put("isolationSystems", ISOLATION_SYSTEMS.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", e.getValue().name);
                    info.put("transmissibilityX", e.getValue().transmissibilityX);
                    info.put("transmissibilityY", e.getValue().transmissibilityY);
                    info.put("transmissibilityZ", e.getValue().transmissibilityZ);
                    info.put("isolationEfficiency_dB", e.getValue().isolationEfficiencyDb);
                    return info;
                })));
        return meta;
    }

    private void addUncertaintySource(List<Map<String, Object>> budget, String source,
                                       double value, String type, int dof) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("source", source);
        item.put("value", value);
        item.put("type", type);
        item.put("degreesOfFreedom", dof);
        item.put("sensitivity", 1.0);
        item.put("contribution", value * value);
        budget.add(item);
    }

    private static class VibrationLevel {
        String code;
        String label;
        double displacementX;
        double displacementY;
        double displacementZ;

        public VibrationLevel(String code, String label, double displacementX,
                              double displacementY, double displacementZ) {
            this.code = code;
            this.label = label;
            this.displacementX = displacementX;
            this.displacementY = displacementY;
            this.displacementZ = displacementZ;
        }
    }

    private static class VibrationIsolationSystem {
        String name;
        double transmissibilityX;
        double transmissibilityY;
        double transmissibilityZ;
        double isolationEfficiencyDb;

        public VibrationIsolationSystem(String name, double transmissibilityX,
                                        double transmissibilityY, double transmissibilityZ,
                                        double isolationEfficiencyDb) {
            this.name = name;
            this.transmissibilityX = transmissibilityX;
            this.transmissibilityY = transmissibilityY;
            this.transmissibilityZ = transmissibilityZ;
            this.isolationEfficiencyDb = isolationEfficiencyDb;
        }
    }
}
