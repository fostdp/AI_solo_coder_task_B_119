class CivilizationPanelController {
    constructor(api) {
        this.api = api;
        this.radarChart = null;
        this.allCivilizations = [];
        this.selectedCivilizations = [];
        this.currentComparison = null;
    }

    async init() {
        this.radarChart = new RadarChart('radarChart');
        await this.loadCivilizations();
        this.bindEvents();
        await this.loadComparison('china-rome');
    }

    async loadCivilizations() {
        try {
            const result = await this.api.get(`${AppConfig.API_BASE}${AppConfig.apiEndpoints.civilization.list}`);
            if (result.success) {
                this.allCivilizations = result.data;
                this.renderCivilizationList();
            }
        } catch (e) {
            console.error('加载文明列表失败:', e);
            this.loadMockCivilizations();
        }
    }

    loadMockCivilizations() {
        this.allCivilizations = [
            { id: 1, civilizationName: '唐代-中国', civilizationCode: 'CHN-TANG', periodStartYear: 618, periodEndYear: 907, balanceType: 'EQUAL_ARM', maxCapacity: 800, relativePrecision: 0.00006, materialHardness: 155, armRatioConsistency: 98, structureComplexity: 80, durabilityScore: 90 },
            { id: 2, civilizationName: '古罗马', civilizationCode: 'ROME', periodStartYear: -300, periodEndYear: 476, balanceType: 'UNEQUAL_ARM', maxCapacity: 300, relativePrecision: 0.0005, materialHardness: 180, armRatioConsistency: 80, structureComplexity: 85, durabilityScore: 75 }
        ];
        this.renderCivilizationList();
    }

    renderCivilizationList() {
        const container = document.getElementById('civilizationList');
        if (!container) return;

        const groups = [
            { title: '预设对比', items: [
                { code: 'china-rome', name: '中国唐代 vs 古罗马 (经典对比)' },
                { code: 'all-eastern', name: '东方文明对比' },
                { code: 'all-western', name: '西方文明对比' },
                { code: 'default', name: '全部文明对比' }
            ]}
        ];

        let html = '';
        groups.forEach(group => {
            html += `<div class="mb-3"><strong>${group.title}</strong></div>`;
            group.items.forEach(item => {
                html += `
                    <button class="btn btn-outline-primary btn-sm mb-2 me-2 civ-preset-btn" data-code="${item.code}">
                        ${item.name}
                    </button>
                `;
            });
        });

        html += `<div class="mb-3 mt-4"><strong>自定义选择</strong></div>`;
        html += `<div class="row">`;
        this.allCivilizations.forEach(civ => {
            const type = civ.balanceType === 'EQUAL_ARM' ? '等臂' : '不等臂';
            const selected = this.selectedCivilizations.includes(civ.civilizationCode);
            html += `
                <div class="col-md-6 mb-2">
                    <div class="form-check">
                        <input class="form-check-input civ-checkbox" type="checkbox" 
                               value="${civ.civilizationCode}" id="civ_${civ.id}"
                               ${selected ? 'checked' : ''}>
                        <label class="form-check-label" for="civ_${civ.id}">
                            ${civ.civilizationName} (${civ.periodStartYear > 0 ? '公元' : '公元前'}${Math.abs(civ.periodStartYear)}年, ${type})
                        </label>
                    </div>
                </div>
            `;
        });
        html += `</div>`;
        html += `<button class="btn btn-primary mt-3" id="customCompareBtn">开始自定义对比</button>`;

        container.innerHTML = html;
    }

    bindEvents() {
        document.addEventListener('click', async (e) => {
            if (e.target.classList.contains('civ-preset-btn')) {
                const code = e.target.dataset.code;
                await this.loadComparison(code);
            }

            if (e.target.id === 'customCompareBtn') {
                const checkboxes = document.querySelectorAll('.civ-checkbox:checked');
                const codes = Array.from(checkboxes).map(cb => cb.value);
                if (codes.length < 2) {
                    alert('请至少选择2个文明进行对比');
                    return;
                }
                await this.loadCustomComparison(codes);
            }

            if (e.target.id === 'exportRadarBtn') {
                this.exportRadarImage();
            }
        });
    }

    async loadComparison(type) {
        try {
            let endpoint;
            switch(type) {
                case 'china-rome':
                    endpoint = AppConfig.apiEndpoints.civilization.compareChinaRome;
                    break;
                case 'all-eastern':
                    endpoint = AppConfig.apiEndpoints.civilization.compareEastern;
                    break;
                case 'all-western':
                    endpoint = AppConfig.apiEndpoints.civilization.compareWestern;
                    break;
                default:
                    endpoint = AppConfig.apiEndpoints.civilization.compareDefault;
            }

            const result = await this.api.get(`${AppConfig.API_BASE}${endpoint}`);
            if (result.success) {
                this.currentComparison = result.data;
                this.renderComparison(result.data);
            }
        } catch (e) {
            console.error('对比失败:', e);
            this.loadMockComparison();
        }
    }

    async loadCustomComparison(codes) {
        try {
            const result = await this.api.post(`${AppConfig.API_BASE}${AppConfig.apiEndpoints.civilization.compare}`, {
                civilizationCodes: codes
            });
            if (result.success) {
                this.currentComparison = result.data;
                this.renderComparison(result.data);
            }
        } catch (e) {
            console.error('自定义对比失败:', e);
        }
    }

    loadMockComparison() {
        const mockData = {
            civilizationNames: ['唐代-中国', '古罗马'],
            civilizationCodes: ['CHN-TANG', 'ROME'],
            dimensions: ['最大称量', '相对精度', '材料硬度', '臂长一致性', '结构复杂度', '耐久性'],
            radarData: {
                '最大称量': [90, 60],
                '相对精度': [95, 70],
                '材料硬度': [78, 90],
                '臂长一致性': [98, 80],
                '结构复杂度': [80, 85],
                '耐久性': [90, 75]
            },
            overallWinner: '唐代-中国',
            comparativeAnalysis: [
                '【综合评估】唐代-中国 天平综合得分 88.5 分，在所有对比文明中表现最优',
                '【相对精度领先】唐代-中国 在相对精度方面达到 95.0 分，技术领先',
                '【臂长一致性领先】唐代-中国 在臂长一致性方面达到 98.0 分，技术领先',
                '【技术路线对比】等臂天平路线(88.5分)整体优于不等臂路线(76.7分)，适合精密计量',
                '【中国特色】中国古代等臂天平在相对精度方面达到世界领先水平，体现了精湛的工艺水准'
            ]
        };
        this.currentComparison = mockData;
        this.renderComparison(mockData);
    }

    renderComparison(data) {
        const dimensions = data.dimensions;
        const datasets = data.civilizationNames.map((name, idx) => {
            const values = dimensions.map(dim => data.radarData[dim][idx]);
            const avgScore = values.reduce((a, b) => a + b, 0) / values.length;
            return {
                label: name,
                values: values,
                avgScore: avgScore
            };
        });

        this.radarChart.setData(dimensions, datasets);

        this.renderSummaries(data);
        this.renderAnalysis(data);
        this.renderWinningBanner(data);
    }

    renderSummaries(data) {
        const container = document.getElementById('civilizationSummaries');
        if (!container) return;

        if (data.summaries) {
            let html = '';
            Object.entries(data.summaries).forEach(([code, summary]) => {
                const typeClass = summary.balanceType === '等臂天平' ? 'info' : 'warning';
                html += `
                    <div class="card mb-3">
                        <div class="card-header">
                            <h5 class="mb-0">
                                ${summary.name}
                                <span class="badge bg-${typeClass} ms-2">${summary.balanceType}</span>
                                <span class="badge bg-primary ms-2">${summary.avgScore.toFixed(1)}分</span>
                            </h5>
                            <small class="text-muted">
                                ${summary.periodStart > 0 ? '公元' : '公元前'}${Math.abs(summary.periodStart)}年 
                                - ${summary.periodEnd > 0 ? '公元' : '公元前'}${Math.abs(summary.periodEnd)}年
                            </small>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <h6>各维度得分</h6>
                                    ${this.renderScoreBars(summary.scores)}
                                </div>
                                <div class="col-md-6">
                                    ${summary.strengths && summary.strengths.length > 0 ? `
                                        <h6>优势</h6>
                                        <ul class="list-unstyled">
                                            ${summary.strengths.map(s => `<li class="text-success"><i class="bi bi-check-circle-fill"></i> ${s}</li>`).join('')}
                                        </ul>
                                    ` : ''}
                                    ${summary.weaknesses && summary.weaknesses.length > 0 ? `
                                        <h6>不足</h6>
                                        <ul class="list-unstyled">
                                            ${summary.weaknesses.map(s => `<li class="text-warning"><i class="bi bi-exclamation-triangle-fill"></i> ${s}</li>`).join('')}
                                        </ul>
                                    ` : ''}
                                </div>
                            </div>
                            <div class="mt-3">
                                <h6>代表文物</h6>
                                <p class="mb-1">${summary.representativeArtifact || '-'}</p>
                                <h6>文化意义</h6>
                                <p class="small text-muted">${summary.culturalSignificance || '-'}</p>
                            </div>
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
        } else {
            container.innerHTML = '<div class="alert alert-info">详细分析数据请从后端获取</div>';
        }
    }

    renderScoreBars(scores) {
        if (!scores) return '';
        let html = '';
        Object.entries(scores).forEach(([dim, score]) => {
            const color = score >= 85 ? 'success' : score >= 70 ? 'primary' : score >= 50 ? 'warning' : 'danger';
            html += `
                <div class="mb-2">
                    <div class="d-flex justify-content-between mb-1">
                        <small>${dim}</small>
                        <small>${score.toFixed(1)}分</small>
                    </div>
                    <div class="progress" style="height: 8px;">
                        <div class="progress-bar bg-${color}" style="width: ${score}%"></div>
                    </div>
                </div>
            `;
        });
        return html;
    }

    renderAnalysis(data) {
        const container = document.getElementById('comparativeAnalysis');
        if (!container || !data.comparativeAnalysis) return;

        let html = '<div class="list-group">';
        data.comparativeAnalysis.forEach((analysis, idx) => {
            const icon = analysis.includes('领先') ? 'trophy' : 
                         analysis.includes('对比') ? 'arrow-left-right' :
                         analysis.includes('演进') ? 'graph-up' : 'info-circle';
            const color = analysis.includes('领先') ? 'success' :
                          analysis.includes('对比') ? 'primary' :
                          analysis.includes('演进') ? 'info' : 'secondary';
            html += `
                <div class="list-group-item list-group-item-action flex-column align-items-start">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1 text-${color}">
                            <i class="bi bi-${icon}"></i> ${analysis}
                        </h6>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    renderWinningBanner(data) {
        const container = document.getElementById('overallWinner');
        if (!container || !data.overallWinner) return;

        container.innerHTML = `
            <div class="alert alert-success text-center">
                <h4 class="mb-0">
                    <i class="bi bi-award-fill text-warning"></i>
                    综合最优：${data.overallWinner}
                </h4>
            </div>
        `;
    }

    exportRadarImage() {
        const canvas = document.getElementById('radarChart');
        if (!canvas) return;

        const link = document.createElement('a');
        link.download = `文明对比雷达图_${new Date().toISOString().slice(0,10)}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
    }
}

if (typeof window !== 'undefined') {
    window.CivilizationPanelController = CivilizationPanelController;
}
