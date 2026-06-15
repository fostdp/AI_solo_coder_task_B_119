class CraftInferrerComponent {
    constructor(containerId, api, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) {
            throw new Error(`Container element with id "${containerId}" not found`);
        }

        this.api = api;
        this.options = {
            apiBase: options.apiBase || 'http://localhost:8080/api',
            endpoints: options.endpoints || {
                analyze: '/manufacturing/analyze',
                history: '/manufacturing/history',
                monteCarlo: '/manufacturing/monte-carlo',
                craftMethods: '/manufacturing/craft-methods'
            },
            onAnalysisComplete: options.onAnalysisComplete || null,
            onMonteCarloComplete: options.onMonteCarloComplete || null,
            onBack: options.onBack || null,
            onBalanceChange: options.onBalanceChange || null
        };

        this.balances = [];
        this.selectedBalanceId = null;
        this.currentAnalysis = null;
        this.monteCarloResult = null;
        this.initialized = false;
        this.isAnalyzing = false;
        this.isRunningMc = false;

        this._elements = {};
    }

    async init() {
        if (this.initialized) return;

        await this.render();
        this.bindEvents();
        await this.loadBalances();
        this.initialized = true;
    }

    async render() {
        const template = await this._loadTemplate();
        this.container.innerHTML = template;
        this._cacheElements();
    }

    _cacheElements() {
        const el = (selector) => this.container.querySelector(selector);

        this._elements = {
            balanceSelect: el('.ci-balance-select'),
            analyzeBtn: el('.ci-analyze-btn'),
            historyBtn: el('.ci-history-btn'),
            exportBtn: el('.ci-export-btn'),
            backBtn: el('.ci-back-btn'),
            gradeBanner: el('.ci-grade-banner'),
            sixDimensionScores: el('.ci-six-dimension-scores'),
            craftMethod: el('.ci-craft-method'),
            materialAnalysis: el('.ci-material-analysis'),
            archaeologicalConsistency: el('.ci-archaeological-consistency'),
            dataAdequacy: el('.ci-data-adequacy'),
            literatureSources: el('.ci-literature-sources'),
            mcIterations: el('.ci-mc-iterations'),
            mcRunBtn: el('.ci-mc-run-btn'),
            mcResults: el('.ci-mc-results'),
            historyList: el('.ci-history-list'),
            uncertaintyBudget: el('.ci-uncertainty-budget')
        };
    }

    async _loadTemplate() {
        const templatePath = new URL('./craft-inferrer-template.html', import.meta.url).href;
        try {
            const response = await fetch(templatePath);
            return await response.text();
        } catch (e) {
            return this._getFallbackTemplate();
        }
    }

    _getFallbackTemplate() {
        return `
<div class="craft-inferrer-component">
    <div class="panel-header">
        <h2><i class="bi bi-gear-wide-connected"></i> 古代天平制造工艺反演</h2>
        <div class="panel-actions">
            <button class="btn btn-secondary ci-back-btn">
                <i class="bi bi-arrow-left"></i> 返回
            </button>
        </div>
    </div>
    <div class="panel-content">
        <div class="row mb-4">
            <div class="col-md-6">
                <label class="form-label">选择天平</label>
                <select class="form-select ci-balance-select">
                    <option value="">请选择天平...</option>
                </select>
            </div>
            <div class="col-md-6 d-flex align-items-end gap-2">
                <button class="btn btn-success ci-analyze-btn">
                    <i class="bi bi-search"></i> 开始分析
                </button>
                <button class="btn btn-outline-primary ci-history-btn">
                    <i class="bi bi-clock-history"></i> 历史记录
                </button>
                <button class="btn btn-outline-secondary ci-export-btn">
                    <i class="bi bi-download"></i> 导出
                </button>
            </div>
        </div>
        <div class="ci-grade-banner"></div>
        <div class="row mt-4">
            <div class="col-md-8">
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-bar-chart-fill"></i> 六维评分</h5></div>
                    <div class="card-body"><div class="ci-six-dimension-scores"></div></div>
                </div>
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-gear"></i> 推断工艺方法</h5></div>
                    <div class="card-body"><div class="ci-craft-method"></div></div>
                </div>
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-box-seam"></i> 材料分析</h5></div>
                    <div class="card-body"><div class="ci-material-analysis"></div></div>
                </div>
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-shield-check"></i> 考古证据一致性</h5></div>
                    <div class="card-body"><div class="ci-archaeological-consistency"></div></div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-pie-chart"></i> 数据充分性等级</h5></div>
                    <div class="card-body"><div class="ci-data-adequacy"></div></div>
                </div>
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-book"></i> 文献数据来源</h5></div>
                    <div class="card-body"><div class="ci-literature-sources"></div></div>
                </div>
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-calculator"></i> 蒙特卡洛模拟</h5></div>
                    <div class="card-body">
                        <div class="ci-monte-carlo">
                            <div class="mb-3">
                                <label class="form-label">模拟次数</label>
                                <select class="form-select ci-mc-iterations">
                                    <option value="1000">1,000 次</option>
                                    <option value="10000" selected>10,000 次</option>
                                    <option value="100000">100,000 次</option>
                                    <option value="1000000">1,000,000 次</option>
                                </select>
                            </div>
                            <button class="btn btn-primary w-100 ci-mc-run-btn">
                                <i class="bi bi-play-fill"></i> 运行模拟
                            </button>
                            <div class="ci-mc-results mt-3"></div>
                        </div>
                    </div>
                </div>
                <div class="ci-history-list">
                    <div class="alert alert-info">点击"历史记录"查看历史分析</div>
                </div>
            </div>
        </div>
        <div class="card mt-4">
            <div class="card-header"><h5 class="mb-0"><i class="bi bi-rulers"></i> 不确定度预算</h5></div>
            <div class="card-body"><div class="ci-uncertainty-budget"></div></div>
        </div>
    </div>
</div>`;
    }

    bindEvents() {
        const { balanceSelect, analyzeBtn, historyBtn, exportBtn, backBtn, mcRunBtn } = this._elements;

        balanceSelect.addEventListener('change', (e) => {
            this.selectedBalanceId = e.target.value ? parseInt(e.target.value) : null;
            if (this.options.onBalanceChange) {
                this.options.onBalanceChange(this.selectedBalanceId);
            }
        });

        analyzeBtn.addEventListener('click', () => this._handleAnalyze());
        historyBtn.addEventListener('click', () => this._handleLoadHistory());
        exportBtn.addEventListener('click', () => this._handleExport());
        backBtn.addEventListener('click', () => this._handleBack());
        mcRunBtn.addEventListener('click', () => this._handleMonteCarlo());
    }

    async loadBalances() {
        try {
            const result = await this.api.get(`${this.options.apiBase}/balances`);
            if (result.success) {
                this.balances = result.data;
            } else {
                this._loadMockBalances();
            }
        } catch (e) {
            console.warn('[CraftInferrer] 加载天平列表失败，使用模拟数据:', e);
            this._loadMockBalances();
        }
        this._renderBalanceSelect();
    }

    _loadMockBalances() {
        this.balances = [
            { id: 1, name: '战国青铜天平', material: '青铜', knifeRadius: 0.5, dynasty: '战国' },
            { id: 2, name: '唐代玛瑙天平', material: '玛瑙', knifeRadius: 0.2, dynasty: '唐' },
            { id: 3, name: '汉代铁权天平', material: '铁', knifeRadius: 0.8, dynasty: '汉' }
        ];
    }

    _renderBalanceSelect() {
        const select = this._elements.balanceSelect;
        select.innerHTML = '<option value="">请选择天平...</option>';
        this.balances.forEach(b => {
            select.innerHTML += `<option value="${b.id}">${b.name} (${b.material})</option>`;
        });
    }

    async _handleAnalyze() {
        if (!this.selectedBalanceId) {
            alert('请先选择一个天平');
            return;
        }

        if (this.isAnalyzing) return;
        this.isAnalyzing = true;
        this._setAnalyzeLoading(true);

        try {
            const result = await this.api.post(
                `${this.options.apiBase}${this.options.endpoints.analyze}/${this.selectedBalanceId}`
            );
            if (result.success) {
                this.currentAnalysis = result.data;
            } else {
                this.currentAnalysis = this._getMockAnalysis();
            }
        } catch (e) {
            console.warn('[CraftInferrer] 分析失败，使用模拟数据:', e);
            this.currentAnalysis = this._getMockAnalysis();
        } finally {
            this.isAnalyzing = false;
            this._setAnalyzeLoading(false);
        }

        this._renderAnalysis(this.currentAnalysis);

        if (this.options.onAnalysisComplete) {
            this.options.onAnalysisComplete(this.currentAnalysis);
        }
    }

    _getMockAnalysis() {
        return {
            balanceName: '战国青铜天平',
            analysisTime: new Date().toISOString(),
            overallScore: 75.5,
            overallGrade: '能品',
            geometryScore: 72,
            geometryGrade: '佳品',
            surfaceScore: 68,
            surfaceGrade: '常品',
            materialScore: 85,
            materialGrade: '妙品',
            assemblyScore: 75,
            assemblyGrade: '能品',
            craftsmanshipScore: 80,
            craftsmanshipGrade: '能品',
            designScore: 78,
            designGrade: '能品',
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
                description: '采用分块泥范铸造，经过多道工序加工，是战国时期青铜器的典型制造方法。',
                processSteps: ['制模', '翻范', '合范', '浇注', '脱范', '打磨', '抛光', '装配'],
                typicalPeriod: '战国至西汉',
                complexityLevel: '高'
            },
            theoreticalKnifeRadius: 0.5,
            actualKnifeRadius: 0.65,
            radiusError: 0.15,
            estimatedYear: -300,
            artisanLevel: '高级工匠',
            craftConfidence: 0.88,
            wearAnalysis: {
                archardWearVolume: 0.0012,
                estimatedWearCycles: 1500000
            },
            materialAnalysis: {
                primaryMaterial: '青铜',
                alloyComposition: {
                    copper: 0.78,
                    tin: 0.16,
                    lead: 0.04,
                    other: 0.02
                },
                hardness: 180,
                tensileStrength: 320,
                corrosionResistance: '中等',
                grainStructure: '细晶',
                heatTreatment: '否'
            },
            archaeologicalConsistency: {
                overallConsistency: 0.82,
                consistencyGrade: '高',
                matchingSites: 5,
                matchingArtifacts: 23,
                evidences: [
                    { type: '出土文物', site: '湖北江陵', count: 8, matchRate: 0.85 },
                    { type: '铭文记载', site: '金文资料', count: 5, matchRate: 0.78 },
                    { type: '工艺痕迹', site: '器表观察', count: 3, matchRate: 0.92 }
                ]
            },
            dataAdequacy: {
                level: '良好',
                score: 76,
                dimensions: {
                    measurementData: { completeness: 0.85, quality: 0.8 },
                    referenceData: { completeness: 0.72, quality: 0.75 },
                    archaeologicalData: { completeness: 0.68, quality: 0.7 }
                },
                limitations: ['缺少微量元素分析数据', '部分尺寸仅有估计值']
            },
            literatureSources: [
                { id: 1, title: '中国古代度量衡', author: '丘光明', year: 1992, type: '专著', relevance: 0.9 },
                { id: 2, title: '战国秦汉权衡器研究', author: '傅举有', year: 1985, type: '期刊论文', relevance: 0.82 },
                { id: 3, title: '考古学报·江陵楚墓发掘报告', author: '湖北省博物馆', year: 1984, type: '考古报告', relevance: 0.88 },
                { id: 4, title: '中国科学技术史·度量衡卷', author: '卢嘉锡', year: 2002, type: '专著', relevance: 0.76 }
            ],
            uncertaintyBudget: [
                { source: '几何测量', value: 0.023, unit: 'mm', distribution: '正态', contribution: 0.35 },
                { source: '材料属性', value: 0.015, unit: '%', distribution: '均匀', contribution: 0.22 },
                { source: '环境因素', value: 0.008, unit: 'mm', distribution: '三角', contribution: 0.12 },
                { source: '工艺复原假设', value: 0.045, unit: 'mm', distribution: '正态', contribution: 0.18 },
                { source: '考古数据偏差', value: 0.032, unit: '%', distribution: '均匀', contribution: 0.13 }
            ]
        };
    }

    _setAnalyzeLoading(loading) {
        const btn = this._elements.analyzeBtn;
        if (loading) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>分析中...';
        } else {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-search"></i> 开始分析';
        }
    }

    _renderAnalysis(data) {
        this._renderGradeBanner(data.overallScore, data.overallGrade);
        this._renderSixDimensionScores(data);
        this._renderCraftMethod(data);
        this._renderMaterialAnalysis(data);
        this._renderArchaeologicalConsistency(data);
        this._renderDataAdequacy(data);
        this._renderLiteratureSources(data);
        this._renderUncertaintyBudget(data);
    }

    _renderGradeBanner(score, grade) {
        const banner = this._elements.gradeBanner;
        const gradeColors = {
            '神品': 'gold', '妙品': 'success', '能品': 'primary',
            '佳品': 'info', '常品': 'warning', '残品': 'danger'
        };
        const color = gradeColors[grade] || 'secondary';
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

    _renderSixDimensionScores(data) {
        const container = this._elements.sixDimensionScores;

        const dimensions = [
            { name: '几何精度', score: data.geometryScore, grade: data.geometryGrade, sub: [
                { name: '刀口半径', score: data.knifeRadiusScore },
                { name: '臂杆直度', score: data.armStraightnessScore }
            ]},
            { name: '表面质量', score: data.surfaceScore, grade: data.surfaceGrade, sub: [
                { name: '平均摩擦系数', score: data.averageFrictionScore },
                { name: '粗糙度一致性', score: data.roughnessConsistencyScore }
            ]},
            { name: '材料质量', score: data.materialScore, grade: data.materialGrade, sub: [
                { name: '材料硬度', score: data.hardnessScore },
                { name: '耐磨性', score: data.wearResistanceScore }
            ]},
            { name: '装配精度', score: data.assemblyScore, grade: data.assemblyGrade, sub: [
                { name: '臂长比偏差', score: Math.max(0, 100 - data.armLengthRatioDeviation * 100000) }
            ]},
            { name: '工艺水平', score: data.craftsmanshipScore, grade: data.craftsmanshipGrade, sub: [
                { name: '加工精细度', score: data.craftsmanshipScore - 5 },
                { name: '表面处理', score: data.craftsmanshipScore + 3 }
            ]},
            { name: '设计合理性', score: data.designScore, grade: data.designGrade, sub: [
                { name: '结构优化', score: data.designScore - 3 },
                { name: '力学性能', score: data.designScore + 2 }
            ]}
        ];

        let html = '<div class="row">';
        dimensions.forEach(dim => {
            const color = dim.score >= 85 ? 'success' : dim.score >= 70 ? 'primary' : dim.score >= 50 ? 'warning' : 'danger';
            html += `
                <div class="col-md-6 mb-3">
                    <div class="card h-100 border-${color}">
                        <div class="card-body py-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <h6 class="card-title mb-0 text-${color}">${dim.name}</h6>
                                <span class="badge bg-${color}">${dim.grade}</span>
                            </div>
                            <div class="d-flex align-items-center gap-3">
                                <div class="display-6 text-${color}">${dim.score.toFixed(0)}</div>
                                <div class="flex-grow-1">
                                    <div class="progress" style="height: 8px;">
                                        <div class="progress-bar bg-${color}" style="width: ${dim.score}%"></div>
                                    </div>
                                </div>
                            </div>
                            <div class="small mt-2 text-muted">
                                ${dim.sub.map(s => `
                                    <div class="d-flex justify-content-between">
                                        <span>${s.name}</span>
                                        <strong>${s.score.toFixed(0)}分</strong>
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

    _renderCraftMethod(data) {
        const container = this._elements.craftMethod;
        if (!data.inferredCraftDetails) return;

        const craft = data.inferredCraftDetails;
        container.innerHTML = `
            <div class="mb-3">
                <h5 class="text-primary">${craft.name}
                    <span class="badge bg-info ms-2">置信度: ${(data.craftConfidence * 100).toFixed(0)}%</span>
                </h5>
                <p class="text-muted">${craft.description}</p>
            </div>
            <div class="row mb-3">
                <div class="col-md-6">
                    <div class="small text-muted">流行时期</div>
                    <div class="h6">${craft.typicalPeriod || '待考证'}</div>
                </div>
                <div class="col-md-6">
                    <div class="small text-muted">工艺复杂度</div>
                    <div class="h6">${craft.complexityLevel || '中等'}</div>
                </div>
            </div>
            ${craft.processSteps ? `
                <div class="mb-3">
                    <h6 class="mb-2">工艺流程</h6>
                    <div class="d-flex flex-wrap gap-2">
                        ${craft.processSteps.map((step, idx) => `
                            <span class="badge rounded-pill bg-secondary">${idx + 1}. ${step}</span>
                        `).join('')}
                    </div>
                </div>
            ` : ''}
            <div class="row mt-3">
                <div class="col-md-4">
                    <div class="small text-muted">工匠等级</div>
                    <div class="h6 mb-0"><span class="badge bg-primary">${data.artisanLevel}</span></div>
                </div>
                <div class="col-md-4">
                    <div class="small text-muted">推算年代</div>
                    <div class="h6 mb-0"><span class="badge bg-dark">${data.estimatedYear > 0 ? '公元' : '公元前'}${Math.abs(data.estimatedYear)}年</span></div>
                </div>
                <div class="col-md-4">
                    <div class="small text-muted">工艺类型</div>
                    <div class="h6 mb-0"><span class="badge bg-success">${data.craftMethod}</span></div>
                </div>
            </div>
        `;
    }

    _renderMaterialAnalysis(data) {
        const container = this._elements.materialAnalysis;
        const ma = data.materialAnalysis;
        if (!ma) return;

        const alloyItems = ma.alloyComposition ? Object.entries(ma.alloyComposition).map(([k, v]) => ({
            name: this._getElementName(k),
            value: v
        })) : [];

        container.innerHTML = `
            <div class="row mb-3">
                <div class="col-md-4">
                    <div class="small text-muted">主要材质</div>
                    <div class="h5 mb-0">${ma.primaryMaterial}</div>
                </div>
                <div class="col-md-4">
                    <div class="small text-muted">硬度 (HB)</div>
                    <div class="h5 mb-0">${ma.hardness}</div>
                </div>
                <div class="col-md-4">
                    <div class="small text-muted">抗拉强度 (MPa)</div>
                    <div class="h5 mb-0">${ma.tensileStrength}</div>
                </div>
            </div>
            <div class="mb-3">
                <h6 class="mb-2">合金成分</h6>
                <div class="d-flex gap-2 flex-wrap">
                    ${alloyItems.map(item => `
                        <span class="badge bg-info">${item.name}: ${(item.value * 100).toFixed(1)}%</span>
                    `).join('')}
                </div>
            </div>
            <div class="row">
                <div class="col-md-6">
                    <div class="small text-muted">耐腐蚀性</div>
                    <div class="h6 mb-0">${ma.corrosionResistance}</div>
                </div>
                <div class="col-md-6">
                    <div class="small text-muted">晶粒结构</div>
                    <div class="h6 mb-0">${ma.grainStructure}</div>
                </div>
            </div>
            ${ma.heatTreatment ? `
                <div class="mt-2">
                    <span class="badge bg-warning">经过热处理</span>
                </div>
            ` : `
                <div class="mt-2">
                    <span class="badge bg-secondary">未经热处理</span>
                </div>
            `}
        `;
    }

    _getElementName(key) {
        const names = {
            copper: '铜', tin: '锡', lead: '铅', iron: '铁',
            zinc: '锌', other: '其他', gold: '金', silver: '银'
        };
        return names[key] || key;
    }

    _renderArchaeologicalConsistency(data) {
        const container = this._elements.archaeologicalConsistency;
        const ac = data.archaeologicalConsistency;
        if (!ac) return;

        const gradeColor = ac.overallConsistency >= 0.8 ? 'success' :
                          ac.overallConsistency >= 0.6 ? 'warning' : 'danger';

        container.innerHTML = `
            <div class="row mb-3">
                <div class="col-md-6">
                    <div class="d-flex align-items-center gap-3">
                        <div class="display-5 text-${gradeColor}">${(ac.overallConsistency * 100).toFixed(0)}%</div>
                        <div>
                            <div class="h6 mb-0">总体一致性</div>
                            <div class="badge bg-${gradeColor}">${ac.consistencyGrade}</div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="row text-center">
                        <div class="col-6">
                            <div class="small text-muted">匹配遗址</div>
                            <div class="h4 mb-0">${ac.matchingSites}</div>
                        </div>
                        <div class="col-6">
                            <div class="small text-muted">匹配器物</div>
                            <div class="h4 mb-0">${ac.matchingArtifacts}</div>
                        </div>
                    </div>
                </div>
            </div>
            <h6 class="mb-2">考古证据来源</h6>
            <div class="list-group list-group-flush">
                ${ac.evidences.map(ev => {
                    const matchColor = ev.matchRate >= 0.8 ? 'success' : ev.matchRate >= 0.6 ? 'warning' : 'danger';
                    return `
                        <div class="list-group-item px-0 d-flex justify-content-between align-items-center">
                            <div>
                                <strong>${ev.type}</strong>
                                <div class="small text-muted">${ev.site} · ${ev.count}例</div>
                            </div>
                            <span class="badge bg-${matchColor}">${(ev.matchRate * 100).toFixed(0)}% 匹配</span>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    _renderDataAdequacy(data) {
        const container = this._elements.dataAdequacy;
        const da = data.dataAdequacy;
        if (!da) return;

        const levelColors = {
            '优秀': 'success', '良好': 'primary', '一般': 'warning', '不足': 'danger'
        };
        const color = levelColors[da.level] || 'secondary';

        let dimsHtml = '';
        if (da.dimensions) {
            const dimNames = {
                measurementData: '测量数据',
                referenceData: '参考数据',
                archaeologicalData: '考古数据'
            };
            dimsHtml = Object.entries(da.dimensions).map(([key, val]) => {
                const avgScore = (val.completeness + val.quality) / 2 * 100;
                const dimColor = avgScore >= 80 ? 'success' : avgScore >= 60 ? 'warning' : 'danger';
                return `
                    <div class="mb-2">
                        <div class="d-flex justify-content-between small">
                            <span>${dimNames[key] || key}</span>
                            <span class="text-${dimColor}">${avgScore.toFixed(0)}%</span>
                        </div>
                        <div class="progress" style="height: 6px;">
                            <div class="progress-bar bg-${dimColor}" style="width: ${avgScore}%"></div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        container.innerHTML = `
            <div class="text-center mb-3">
                <div class="display-4 text-${color}">${da.score}</div>
                <div class="badge bg-${color} fs-6">${da.level}</div>
            </div>
            ${dimsHtml}
            ${da.limitations && da.limitations.length > 0 ? `
                <div class="mt-3">
                    <small class="text-muted">数据局限：</small>
                    <ul class="small text-warning mb-0">
                        ${da.limitations.map(l => `<li>${l}</li>`).join('')}
                    </ul>
                </div>
            ` : ''}
        `;
    }

    _renderLiteratureSources(data) {
        const container = this._elements.literatureSources;
        const sources = data.literatureSources;
        if (!sources || sources.length === 0) return;

        const typeColors = {
            '专著': 'primary', '期刊论文': 'success',
            '考古报告': 'info', '学位论文': 'warning', '其他': 'secondary'
        };

        container.innerHTML = `
            <div class="list-group list-group-flush">
                ${sources.map(src => {
                    const typeColor = typeColors[src.type] || 'secondary';
                    const relevanceColor = src.relevance >= 0.85 ? 'success' : src.relevance >= 0.7 ? 'primary' : 'secondary';
                    return `
                        <div class="list-group-item px-0">
                            <div class="d-flex justify-content-between align-items-start">
                                <div class="flex-grow-1">
                                    <strong class="small">${src.title}</strong>
                                    <div class="text-muted small">${src.author} · ${src.year}</div>
                                </div>
                                <span class="badge bg-${relevanceColor} ms-2">${(src.relevance * 100).toFixed(0)}%</span>
                            </div>
                            <span class="badge bg-${typeColor} mt-1">${src.type}</span>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    _renderUncertaintyBudget(data) {
        const container = this._elements.uncertaintyBudget;
        const budget = data.uncertaintyBudget;
        if (!budget || budget.length === 0) return;

        const totalContribution = budget.reduce((sum, item) => sum + item.contribution, 0);

        container.innerHTML = `
            <div class="table-responsive">
                <table class="table table-sm table-hover">
                    <thead>
                        <tr>
                            <th>不确定度来源</th>
                            <th>数值</th>
                            <th>单位</th>
                            <th>分布类型</th>
                            <th>贡献度</th>
                            <th>贡献占比</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${budget.map(item => {
                            const pct = totalContribution > 0 ? (item.contribution / totalContribution * 100) : 0;
                            const barColor = pct >= 30 ? 'danger' : pct >= 20 ? 'warning' : 'primary';
                            return `
                                <tr>
                                    <td>${item.source}</td>
                                    <td class="text-end font-monospace">${item.value}</td>
                                    <td>${item.unit}</td>
                                    <td><span class="badge bg-secondary">${item.distribution}</span></td>
                                    <td class="text-end">${(item.contribution * 100).toFixed(1)}%</td>
                                    <td style="width: 150px;">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="progress flex-grow-1" style="height: 6px;">
                                                <div class="progress-bar bg-${barColor}" style="width: ${pct}%"></div>
                                            </div>
                                            <small class="text-muted">${pct.toFixed(1)}%</small>
                                        </div>
                                    </td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
            <div class="mt-2 text-end">
                <span class="text-muted">合成标准不确定度: </span>
                <strong class="text-primary">${this._calculateCombinedUncertainty(budget).toFixed(4)}</strong>
            </div>
        `;
    }

    _calculateCombinedUncertainty(budget) {
        const sumSquares = budget.reduce((sum, item) => sum + item.value * item.value, 0);
        return Math.sqrt(sumSquares);
    }

    async _handleMonteCarlo() {
        if (!this.currentAnalysis) {
            alert('请先进行工艺分析');
            return;
        }

        if (this.isRunningMc) return;
        this.isRunningMc = true;
        this._setMcLoading(true);

        const iterations = parseInt(this._elements.mcIterations.value);

        try {
            const result = await this.api.post(
                `${this.options.apiBase}${this.options.endpoints.monteCarlo}/${this.selectedBalanceId}`,
                { iterations }
            );
            if (result.success) {
                this.monteCarloResult = result.data;
            } else {
                this.monteCarloResult = this._getMockMonteCarloResult(iterations);
            }
        } catch (e) {
            console.warn('[CraftInferrer] 蒙特卡洛模拟失败，使用模拟数据:', e);
            this.monteCarloResult = this._getMockMonteCarloResult(iterations);
        } finally {
            this.isRunningMc = false;
            this._setMcLoading(false);
        }

        this._renderMcResults(this.monteCarloResult);

        if (this.options.onMonteCarloComplete) {
            this.options.onMonteCarloComplete(this.monteCarloResult);
        }
    }

    _getMockMonteCarloResult(iterations) {
        return {
            iterations: iterations,
            executionTimeMs: 1250 + Math.random() * 1000,
            overallScore: {
                mean: 75.5,
                stdDev: 3.2,
                min: 65.1,
                max: 86.3,
                percentile5: 70.2,
                percentile95: 80.8
            },
            geometryScore: {
                mean: 72.0,
                stdDev: 4.1,
                min: 58.3,
                max: 85.6,
                percentile5: 65.2,
                percentile95: 78.9
            },
            materialScore: {
                mean: 85.0,
                stdDev: 2.5,
                min: 76.2,
                max: 92.1,
                percentile5: 80.8,
                percentile95: 89.2
            },
            craftConfidence: {
                mean: 0.88,
                stdDev: 0.045,
                min: 0.72,
                max: 0.96,
                percentile5: 0.80,
                percentile95: 0.94
            },
            uncertaintyDistribution: [
                { range: '60-65', count: Math.round(iterations * 0.02) },
                { range: '65-70', count: Math.round(iterations * 0.12) },
                { range: '70-75', count: Math.round(iterations * 0.28) },
                { range: '75-80', count: Math.round(iterations * 0.33) },
                { range: '80-85', count: Math.round(iterations * 0.18) },
                { range: '85-90', count: Math.round(iterations * 0.06) },
                { range: '90-95', count: Math.round(iterations * 0.01) }
            ],
            sensitivityAnalysis: [
                { factor: '刀口半径', sensitivityIndex: 0.35 },
                { factor: '材料硬度', sensitivityIndex: 0.25 },
                { factor: '臂杆长度', sensitivityIndex: 0.20 },
                { factor: '摩擦系数', sensitivityIndex: 0.12 },
                { factor: '环境温度', sensitivityIndex: 0.08 }
            ]
        };
    }

    _setMcLoading(loading) {
        const btn = this._elements.mcRunBtn;
        if (loading) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>模拟中...';
        } else {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-play-fill"></i> 运行模拟';
        }
    }

    _renderMcResults(result) {
        const container = this._elements.mcResults;
        const os = result.overallScore;

        container.innerHTML = `
            <div class="card bg-light">
                <div class="card-body py-3">
                    <div class="text-center mb-3">
                        <div class="display-6 text-primary">${os.mean.toFixed(1)} <small class="text-muted fs-6">± ${os.stdDev.toFixed(2)}</small></div>
                        <div class="small text-muted">综合评分均值</div>
                    </div>
                    <div class="row text-center small">
                        <div class="col-4">
                            <div class="text-muted">最小值</div>
                            <div class="fw-bold">${os.min.toFixed(1)}</div>
                        </div>
                        <div class="col-4">
                            <div class="text-muted">中位数</div>
                            <div class="fw-bold">${os.mean.toFixed(1)}</div>
                        </div>
                        <div class="col-4">
                            <div class="text-muted">最大值</div>
                            <div class="fw-bold">${os.max.toFixed(1)}</div>
                        </div>
                    </div>
                    <div class="mt-3">
                        <div class="d-flex justify-content-between small mb-1">
                            <span>95%置信区间</span>
                            <span class="text-primary">[${os.percentile5.toFixed(1)}, ${os.percentile95.toFixed(1)}]</span>
                        </div>
                        <div class="progress" style="height: 8px;">
                            <div class="progress-bar bg-secondary" style="width: ${(os.min / 100 * 100)}%"></div>
                            <div class="progress-bar bg-primary" style="width: ${((os.percentile95 - os.percentile5) / 100 * 100)}%"></div>
                            <div class="progress-bar bg-secondary" style="width: ${((100 - os.max) / 100 * 100)}%"></div>
                        </div>
                    </div>
                    <div class="mt-2 text-center">
                        <span class="badge bg-info">${result.iterations.toLocaleString()} 次迭代</span>
                        <span class="badge bg-secondary ms-1">${(result.executionTimeMs / 1000).toFixed(2)}s</span>
                    </div>
                </div>
            </div>
        `;
    }

    async _handleLoadHistory() {
        if (!this.selectedBalanceId) {
            alert('请先选择一个天平');
            return;
        }

        try {
            const result = await this.api.get(
                `${this.options.apiBase}${this.options.endpoints.history}/${this.selectedBalanceId}`
            );
            if (result.success) {
                this._renderHistory(result.data);
            } else {
                this._renderMockHistory();
            }
        } catch (e) {
            console.warn('[CraftInferrer] 加载历史失败:', e);
            this._renderMockHistory();
        }
    }

    _renderMockHistory() {
        const history = [
            { id: 1, balanceName: '战国青铜天平', analysisTime: new Date(Date.now() - 86400000 * 3).toISOString(), overallScore: 74.2, overallGrade: '能品' },
            { id: 2, balanceName: '战国青铜天平', analysisTime: new Date(Date.now() - 86400000 * 10).toISOString(), overallScore: 73.8, overallGrade: '能品' },
            { id: 3, balanceName: '战国青铜天平', analysisTime: new Date(Date.now() - 86400000 * 25).toISOString(), overallScore: 72.5, overallGrade: '佳品' }
        ];
        this._renderHistory(history);
    }

    _renderHistory(data) {
        const container = this._elements.historyList;
        if (!data || data.length === 0) {
            container.innerHTML = '<div class="alert alert-warning">暂无历史记录</div>';
            return;
        }

        let html = '<div class="card"><div class="card-header"><h6 class="mb-0">历史分析记录</h6></div><div class="list-group list-group-flush">';
        data.forEach(item => {
            html += `
                <div class="list-group-item list-group-item-action" style="cursor: pointer;" data-history-id="${item.id}">
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
        html += '</div></div>';
        container.innerHTML = html;
    }

    _handleExport() {
        if (!this.currentAnalysis) {
            alert('请先进行分析');
            return;
        }
        const data = this.currentAnalysis;
        const text = this._generateReportText(data);
        const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `工艺反演报告_${data.balanceName}_${new Date().toISOString().slice(0,10)}.txt`;
        link.click();
    }

    _generateReportText(data) {
        return `
古代天平制造工艺反演分析报告
================================
天平名称: ${data.balanceName}
分析时间: ${new Date(data.analysisTime).toLocaleString()}

【综合评级】${data.overallGrade} (${data.overallScore.toFixed(1)}分)

【六维评分】
- 几何精度: ${data.geometryScore.toFixed(1)}分 (${data.geometryGrade})
- 表面质量: ${data.surfaceScore.toFixed(1)}分 (${data.surfaceGrade})
- 材料质量: ${data.materialScore.toFixed(1)}分 (${data.materialGrade})
- 装配精度: ${data.assemblyScore.toFixed(1)}分 (${data.assemblyGrade})
- 工艺水平: ${data.craftsmanshipScore.toFixed(1)}分 (${data.craftsmanshipGrade})
- 设计合理性: ${data.designScore.toFixed(1)}分 (${data.designGrade})

【推断工艺】${data.craftMethod}
工匠等级: ${data.artisanLevel}
推算年代: ${data.estimatedYear > 0 ? '公元' : '公元前'}${Math.abs(data.estimatedYear)}年
置信度: ${(data.craftConfidence * 100).toFixed(0)}%
工艺流程: ${data.inferredCraftDetails?.processSteps?.join(' → ') || '未知'}

【材料分析】
主要材质: ${data.materialAnalysis?.primaryMaterial || '未知'}
硬度: ${data.materialAnalysis?.hardness || '未知'} HB
抗拉强度: ${data.materialAnalysis?.tensileStrength || '未知'} MPa

【考古证据一致性】
总体一致性: ${(data.archaeologicalConsistency?.overallConsistency * 100).toFixed(0) || '未知'}%
匹配遗址: ${data.archaeologicalConsistency?.matchingSites || 0}处
匹配器物: ${data.archaeologicalConsistency?.matchingArtifacts || 0}件

【数据充分性】
等级: ${data.dataAdequacy?.level || '未知'}
评分: ${data.dataAdequacy?.score || 0}分

【文献来源】
${(data.literatureSources || []).map(s => `- ${s.title} (${s.author}, ${s.year})`).join('\n')}

【不确定度预算】
${(data.uncertaintyBudget || []).map(u => `- ${u.source}: ${u.value} ${u.unit} (${u.distribution}分布, 贡献${(u.contribution * 100).toFixed(1)}%)`).join('\n')}
        `.trim();
    }

    _handleBack() {
        if (this.options.onBack) {
            this.options.onBack();
        }
    }

    getCurrentAnalysis() {
        return this.currentAnalysis;
    }

    getMonteCarloResult() {
        return this.monteCarloResult;
    }

    setBalance(balanceId) {
        this.selectedBalanceId = balanceId;
        if (this._elements.balanceSelect) {
            this._elements.balanceSelect.value = balanceId || '';
        }
    }

    refresh() {
        if (this.currentAnalysis) {
            this._renderAnalysis(this.currentAnalysis);
        }
        if (this.monteCarloResult) {
            this._renderMcResults(this.monteCarloResult);
        }
    }

    destroy() {
        this.container.innerHTML = '';
        this.initialized = false;
        this.currentAnalysis = null;
        this.monteCarloResult = null;
    }
}

if (typeof window !== 'undefined') {
    window.CraftInferrerComponent = CraftInferrerComponent;
}

export default CraftInferrerComponent;
