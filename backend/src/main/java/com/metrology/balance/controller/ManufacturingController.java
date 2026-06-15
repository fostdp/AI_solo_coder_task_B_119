package com.metrology.balance.controller;

import com.metrology.balance.common.Result;
import com.metrology.balance.entity.ManufacturingAnalysis;
import com.metrology.balance.modules.manufacturing_reconstruction.ManufacturingReconstructionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manufacturing")
public class ManufacturingController {

    @Autowired
    private ManufacturingReconstructionService reconstructionService;

    @PostMapping("/analyze/{balanceId}")
    public Result<ManufacturingAnalysis> analyzeManufacturingTechnology(@PathVariable Integer balanceId) {
        try {
            ManufacturingAnalysis analysis = reconstructionService.analyzeManufacturingTechnology(balanceId);
            return Result.success(analysis);
        } catch (Exception e) {
            return Result.error("工艺分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/history/{balanceId}")
    public Result<List<ManufacturingAnalysis>> getAnalysisHistory(@PathVariable Integer balanceId) {
        List<ManufacturingAnalysis> history = reconstructionService.getAnalysisHistory(balanceId);
        return Result.success(history);
    }

    @GetMapping("/latest/{balanceId}")
    public Result<ManufacturingAnalysis> getLatestAnalysis(@PathVariable Integer balanceId) {
        return reconstructionService.getLatestAnalysis(balanceId)
                .map(Result::success)
                .orElse(Result.error("暂无分析记录"));
    }

    @GetMapping("/craft-methods")
    public Result<Map<String, Object>> getCraftMethodKnowledge() {
        Map<String, Object> knowledge = Map.of(
                "methods", List.of(
                        Map.of("key", "青铜-范铸", "name", "青铜范铸法", "period", "战国-汉"),
                        Map.of("key", "青铜-失蜡", "name", "青铜失蜡法", "period", "春秋-清"),
                        Map.of("key", "钢铁-锻打", "name", "钢铁锻打法", "period", "战国-现代"),
                        Map.of("key", "玉石-琢磨", "name", "玉石琢磨法", "period", "新石器-现代"),
                        Map.of("key", "玛瑙-钻孔", "name", "玛瑙钻孔法", "period", "唐-现代"),
                        Map.of("key", "木-切削", "name", "木切削法", "period", "新石器-现代")
                ),
                "grades", List.of(
                        Map.of("grade", "神品", "scoreRange", "95-100", "description", "工艺精湛，误差极小"),
                        Map.of("grade", "妙品", "scoreRange", "85-94", "description", "工艺优秀，精度很高"),
                        Map.of("grade", "能品", "scoreRange", "75-84", "description", "工艺良好，精度较高"),
                        Map.of("grade", "佳品", "scoreRange", "65-74", "description", "工艺合格，精度一般"),
                        Map.of("grade", "常品", "scoreRange", "50-64", "description", "工艺普通，精度较低"),
                        Map.of("grade", "残品", "scoreRange", "<50", "description", "工艺粗糙，误差较大")
                )
        );
        return Result.success(knowledge);
    }
}
