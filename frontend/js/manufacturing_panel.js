class ManufacturingPanelController {
    constructor(api) {
        this.api = api;
        this.balances = [];
        this.currentAnalysis = null;
    }

    async init() {
        await this.loadBalances();
        this.bindEvents();
    }

    async loadBalances() {
        try {
            const result = await this.api.get(`${AppConfig.API_BASE}/balances`);
            if (result.success) {
                this.balances = result.data;
                this.renderBalanceSelect();
            }
        } catch (e) {
            console.error('加载天平列表失败:', e);
            this.loadMockBalances();
        }
    }

    loadMockBalances() {
        this.balances = [
            { id: 1, name: '战国青铜天平', material: '青铜', knifeRadius: 0.5 },
            { id: 2, name: '唐代玛瑙天平', material: '玛瑙', knifeRadius: 0.2 },
            { id: 3, name: '汉代铁权天平', material: '铁', knifeRadius: 0.8 }
        ];
        this.renderBalanceSelect();
    }

    renderBalanceSelect() {
        const select = document.getElementById('analysisBalanceSelect');
        if (!select) return;

        select.innerHTML = '<option value="">请选择天平...</option>';
        this.balances.forEach(b => {
            select.innerHTML += `<option value="${b.id}">${b.name} (${b.material})</option>`;
        });
    }

    bindEvents() {
        document.addEventListener('click', async (e) => {
            if (e.target.id === 'analyzeBtn') {
                const balanceId = document.getElementById('analysisBalanceSelect').value;
                if (!balanceId) {
                    alert('请先选择一个天平');
                    return;
                }
                await this.analyze(parseInt(balanceId));
            }

            if (e.target.id === 'loadHistoryBtn') {
                await this.loadHistory();
            }

            if (e.target.id === 'exportAnalysisBtn') {
                this.exportAnalysis();
            }
        });
    }

    async analyze(balanceId) {
        try {
            this.showLoading();
            const result = await this.api.post(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.manufacturing.analyze}/${balanceId}`
            );
            if (result.success) {
                this.currentAnalysis = result.data;
                this.renderAnalysis(result.data);
            }
        } catch (e) {
            console.error('分析失败:', e);
            this.loadMockAnalysis();
        } finally {
            this.hideLoading();
        }
    }

    async loadHistory() {
        try {
            const balanceId = document.getElementById('analysisBalanceSelect').value;
            const result = await this.api.get(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.manufacturing.history}/${balanceId || 'all'}`
            );
            if (result.success) {
                this.renderHistory(result.data);
            }
        } catch (e) {
            console.error('加载历史失败:', e);
        }
    }

    loadMockAnalysis() {
        const mockData = {
            balanceName: '战国青铜天平',
            analysisTime: new Date().toISOString(),
            geometryScore: 72,
            surfaceScore: 68,
            materialScore: 85,
            assemblyScore: 75,
            overallScore: 75,
            overallGrade: '能品',
            geometryGrade: '佳品',
            surfaceGrade: '常品',
            materialGrade: '妙品',
            assemblyGrade: '能品',
            knifeRadiusScore: 75,
            armStraightnessScore: 68,
            averageFrictionScore: 72,
            roughnessConsistencyScore: 65,
            hardnessScore: 88,
            wearResistanceScore: 82,
            armLengthRatioDeviation: 0.0003,
            craftMethod: '青铜范铸工艺',
            inferredCraftDetails: {
                name: '青铜范铸工艺',
                description: '采用分块泥范铸造，经过多道工序加工',
                processSteps: ['制模', '翻范', '合范', '浇注', '脱范', '打磨', '抛光', '装配']
            },
            theoreticalKnifeRadius: 0.5,
            actualKnifeRadius: 0.65,
            radiusError: 0.15,
            estimatedYear: -300,
            artisanLevel: '高级工匠',
            craftConfidence: 0.88,
            wearAnalysis: { archardWearVolume: 0.0012, estimatedWearCycles: 1500000 }
        };
        this.currentAnalysis = mockData;
        this.renderAnalysis(mockData);
    }

    renderAnalysis(data) {
        this.renderGradeBanner(data.overallScore, data.overallGrade);
        this.renderDimensionScores(data);
        this.renderInferredCraft(data);
        this.renderWearAnalysis(data);
    }

    renderGradeBanner(score, grade) {
        const banner = document.getElementById('overallGradeBanner');
        if (!banner) return;

        const gradeColors = {
            '神品': 'gold', '妙品': 'success', '能品': 'primary',
            '佳品': 'info', '常品': 'warning', '残品': 'danger'
        };
        const color = gradeColors[grade] || 'secondary';
        const colorClass = color === 'gold' ? 'text-warning' : `text-${color}`;
        const bgClass = color === 'gold' ? 'bg-warning' : `bg-${color}`;

        banner.innerHTML = `
            <div class="alert ${bgClass} text-white p-4 rounded-3 shadow-lg">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h2 class="mb-0">${grade}</h2>
                        <div class="opacity-75">综合评级</div>
                    </div>
                    <div class="text-end">
                        <h1 class="display-4 mb-0">${score.toFixed(1)}</h1>
                        <div class="opacity-75">分</div>
                    </div>
                </div>
            </div>
        `;
    }

    renderDimensionScores(data) {
        const container = document.getElementById('dimensionScores');
        if (!container) return;

        const dimensions = [
            { name: '几何精度', score: data.geometryScore, sub: [
                { name: '刀口半径', score: data.knifeRadiusScore },
                { name: '臂杆直度', score: data.armStraightnessScore }
            ]},
            { name: '表面粗糙度', score: data.surfaceScore, sub: [
                { name: '平均摩擦系数', score: data.averageFrictionScore },
                { name: '粗糙度一致性', score: data.roughnessConsistencyScore }
            ]},
            { name: '材料质量', score: data.materialScore, sub: [
                { name: '材料硬度', score: data.hardnessScore },
                { name: '耐磨性', score: data.wearResistanceScore }
            ]},
            { name: '装配精度', score: data.assemblyScore, sub: [
                { name: '臂长比偏差', score: Math.max(0, 100 - data.armLengthRatioDeviation * 100000) }
            ]}
        ];

        let html = '<div class="row">';
        dimensions.forEach(dim => {
            const color = dim.score >= 85 ? 'success' : dim.score >= 70 ? 'primary' : dim.score >= 50 ? 'warning' : 'danger';
            html += `
                <div class="col-md-6 mb-4">
                    <div class="card h-100">
                        <div class="card-body">
                            <h5 class="card-title text-${color}">${dim.name}</h5>
                            <div class="display-4 text-${color} mb-3">${dim.score.toFixed(1)} 分</div>
                            <div class="mb-3">
                                <div class="progress" style="height: 12px;">
                                    <div class="progress-bar bg-${color}" style="width: ${dim.score}%"></div>
                                </div>
                            </div>
                            <div class="small">
                                ${dim.sub.map(s => `
                                    <div class="d-flex justify-content-between">
                                        <span>${s.name}</span>
                                        <strong>${s.score.toFixed(1)}分</strong>
                                    </div>
                                `).join('')}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    renderInferredCraft(data) {
        const container = document.getElementById('inferredCraft');
        if (!container || !data.inferredCraftDetails) return;

        const craft = data.inferredCraftDetails;
        let html = `
            <div class="card">
                <div class="card-header">
                    <h5 class="mb-0">
                        <i class="bi bi-gear-wide-connected"></i>
                        推断工艺：${craft.name}
                        <span class="badge bg-info ms-2">置信度: ${(data.craftConfidence * 100).toFixed(0)}%</span>
                    </h5>
                </div>
                <div class="card-body">
                    <p>${craft.description}</p>
                    ${craft.processSteps ? `
                        <h6>工艺流程</h6>
                        <div class="d-flex flex-wrap gap-2">
                            ${craft.processSteps.map((step, idx) => `
                                <span class="badge rounded-pill bg-secondary">${idx + 1}. ${step}</span>
                            `).join('')}
                        </div>
                    ` : ''}
                    <div class="mt-3">
                        <h6>工匠等级：<span class="badge bg-primary">${data.artisanLevel}</span></h6>
                        <h6>推算年代：<span class="badge bg-dark">${data.estimatedYear > 0 ? '公元' : '公元前'}${Math.abs(data.estimatedYear)}年</span></h6>
                    </div>
                    <div class="mt-3">
                        <h6>刀口参数</h6>
                        <div class="row text-center">
                            <div class="col-4">
                                <div class="small text-muted">理论半径</div>
                                <div class="h5 mb-0">${data.theoreticalKnifeRadius} mm</div>
                            </div>
                            <div class="col-4">
                                <div class="small text-muted">实际半径</div>
                                <div class="h5 mb-0">${data.actualKnifeRadius} mm</div>
                            </div>
                            <div class="col-4">
                                <div class="small text-muted">加工误差</div>
                                <div class="h5 mb-0 text-warning">${data.radiusError} mm</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.innerHTML = html;
    }

    renderWearAnalysis(data) {
        const container = document.getElementById('wearAnalysis');
        if (!container || !data.wearAnalysis) return;

        const wear = data.wearAnalysis;
        container.innerHTML = `
            <div class="card bg-light">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-graph-down"></i> 磨损分析 (Archard定律)</h6>
                </div>
                <div class="card-body">
                    <div class="row text-center">
                        <div class="col-6">
                            <div class="small text-muted">理论磨损体积</div>
                            <div class="h4 mb-0">${wear.archardWearVolume.toFixed(4)} mm³</div>
                        </div>
                        <div class="col-6">
                            <div class="small text-muted">预估使用次数</div>
                            <div class="h4 mb-0">${wear.estimatedWearCycles.toLocaleString()} 次</div>
                        </div>
                    </div>
                </div>
                <div class="card-footer small text-muted">
                    基于测量数据反演的磨损量，使用 Archard 磨损定律计算
                </div>
            </div>
        `;
    }

    renderHistory(data) {
        const container = document.getElementById('analysisHistory');
        if (!container) return;

        let html = '<div class="list-group">';
        data.forEach(item => {
            html += `
                <div class="list-group-item">
                    <div class="d-flex justify-content-between">
                        <div>
                            <h6 class="mb-1">${item.balanceName}</h6>
                            <small class="text-muted">${new Date(item.analysisTime).toLocaleString()}</small>
                        </div>
                        <div class="text-end">
                            <span class="badge bg-primary">${item.overallScore.toFixed(1)}分</span>
                            <div><small>${item.overallGrade}</small></div>
                        </div>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    showLoading() {
        const btn = document.getElementById('analyzeBtn');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>分析中...';
        }
    }

    hideLoading() {
        const btn = document.getElementById('analyzeBtn');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-search"></i> 开始分析';
        }
    }

    exportAnalysis() {
        if (!this.currentAnalysis) {
            alert('请先进行分析');
            return;
        }
        const data = this.currentAnalysis;
        const text = `
古代天平制造工艺反演分析报告
================================
天平名称: ${data.balanceName}
分析时间: ${new Date(data.analysisTime).toLocaleString()}

【综合评级】${data.overallGrade} (${data.overallScore.toFixed(1)}分)

【四维评分】
- 几何精度: ${data.geometryScore.toFixed(1)}分
- 表面粗糙度: ${data.surfaceScore.toFixed(1)}分
- 材料质量: ${data.materialScore.toFixed(1)}分
- 装配精度: ${data.assemblyScore.toFixed(1)}分

【推断工艺】${data.craftMethod}
工匠等级: ${data.artisanLevel}
推算年代: ${data.estimatedYear > 0 ? '公元' : '公元前'}${Math.abs(data.estimatedYear)}年
置信度: ${(data.craftConfidence * 100).toFixed(0)}%

【刀口参数】
理论半径: ${data.theoreticalKnifeRadius}mm
实际半径: ${data.actualKnifeRadius}mm
加工误差: ${data.radiusError}mm

【磨损分析】
磨损体积: ${data.wearAnalysis?.archardWearVolume?.toFixed(4)}mm³
预估使用: ${data.wearAnalysis?.estimatedWearCycles?.toLocaleString()}次
        `.trim();

        const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `工艺反演报告_${data.balanceName}_${new Date().toISOString().slice(0,10)}.txt`;
        link.click();
    }
}

if (typeof window !== 'undefined') {
    window.ManufacturingPanelController = ManufacturingPanelController;
}
