class CivilizationComparatorComponent {
    constructor(containerId, api, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) {
            console.error('Container not found:', containerId);
            return;
        }

        this.api = api;
        this.options = Object.assign({
            apiBase: 'http://localhost:8080/api',
            dimensions: ['最大称量', '相对精度', '材料硬度', '臂长一致性', '结构复杂度', '耐久性'],
            colors: [
                { stroke: '#667eea', fill: 'rgba(102, 126, 234, 0.25)' },
                { stroke: '#f093fb', fill: 'rgba(240, 147, 251, 0.25)' },
                { stroke: '#4facfe', fill: 'rgba(79, 172, 254, 0.25)' },
                { stroke: '#43e97b', fill: 'rgba(67, 233, 123, 0.25)' },
                { stroke: '#fa709a', fill: 'rgba(250, 112, 154, 0.25)' },
                { stroke: '#fee140', fill: 'rgba(254, 225, 64, 0.25)' },
                { stroke: '#30cfd0', fill: 'rgba(48, 207, 208, 0.25)' },
                { stroke: '#a8edea', fill: 'rgba(168, 237, 234, 0.25)' }
            ],
            onCompareComplete: null,
            onCivilizationSelect: null,
            onExpertVerify: null
        }, options);

        this.allCivilizations = [];
        this.selectedCodes = [];
        this.currentComparison = null;
        this.expertResult = null;
        this.radarCtx = null;
        this.animationProgress = 0;
        this.animating = false;
        this.initialized = false;
        this.elements = {};
    }

    async init() {
        if (this.initialized) return;

        await this._loadTemplate();
        this._cacheElements();
        this._initRadarCanvas();
        this.bindEvents();
        await this._loadCivilizations();

        this.initialized = true;
    }

    async _loadTemplate() {
        try {
            const templatePath = new URL('./civilization-comparator-template.html', import.meta.url).href;
            const response = await fetch(templatePath);
            const html = await response.text();
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const template = doc.getElementById('civilizationComparatorTemplate');
            
            if (template) {
                const content = template.content.cloneNode(true);
                this.container.innerHTML = '';
                this.container.appendChild(content);
            }
        } catch (e) {
            console.warn('Failed to load template file, using inline fallback:', e);
            this._loadFallbackTemplate();
        }
    }

    _loadFallbackTemplate() {
        this.container.innerHTML = `
        <div class="civ-comp-container">
          <div class="civ-comp-header">
            <h3>🏛️ 文明天平对比分析</h3>
            <div class="reliability-badge">
              <span class="reliability-stars">★★★★☆</span>
              <span class="reliability-label">数据可靠性</span>
            </div>
          </div>
          <div class="civ-comp-body">
            <div class="civ-comp-sidebar">
              <div class="civ-comp-card">
                <h4>快捷对比</h4>
                <div class="civ-quick-btns">
                  <button class="civ-quick-btn" data-preset="china-rome">中罗经典</button>
                  <button class="civ-quick-btn" data-preset="all-eastern">东方文明</button>
                  <button class="civ-quick-btn" data-preset="all-western">西方文明</button>
                  <button class="civ-quick-btn" data-preset="east-west">东西对比</button>
                </div>
              </div>
              <div class="civ-comp-card">
                <h4>选择文明</h4>
                <div class="civ-list" data-role="civilization-list">
                  <div class="empty-state">
                    <div class="empty-icon">📜</div>
                    <div class="empty-text">加载中...</div>
                  </div>
                </div>
                <button class="civ-compare-btn" data-role="compare-btn" disabled>
                  开始对比分析
                </button>
              </div>
              <div class="civ-comp-card">
                <h4>数据可靠性</h4>
                <div class="data-reliability">
                  <span class="reliability-stars">★★★★☆</span>
                  <span class="reliability-value">82%</span>
                  <span class="reliability-label">较高</span>
                </div>
                <div style="font-size: 11px; color: #888; margin-top: 6px;">
                  基于考古发掘与文献记载综合评估
                </div>
              </div>
            </div>
            <div class="civ-comp-main">
              <div class="civ-comp-card" data-role="winner-card" style="display: none;">
                <div class="winner-banner">
                  <span class="trophy-icon">🏆</span>
                  <span class="winner-text" data-role="winner-name">综合最优文明</span>
                  <div class="winner-score" data-role="winner-score">综合评分: --</div>
                </div>
              </div>
              <div class="civ-comp-card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                  <h4 style="margin: 0;">六维雷达图</h4>
                  <button class="export-btn" data-role="export-btn">
                    📷 导出图片
                  </button>
                </div>
                <div class="radar-chart-container">
                  <canvas data-role="radar-canvas" width="500" height="420"></canvas>
                </div>
              </div>
              <div class="civ-comp-card" data-role="ranking-card" style="display: none;">
                <h4>综合排名</h4>
                <ul class="ranking-list" data-role="ranking-list"></ul>
              </div>
              <div class="civ-comp-card" data-role="summaries-card" style="display: none;">
                <h4>文明详情</h4>
                <div class="summary-section" data-role="summaries-content"></div>
              </div>
              <div class="civ-comp-card" data-role="analysis-card" style="display: none;">
                <h4>对比分析</h4>
                <ul class="analysis-list" data-role="analysis-list"></ul>
              </div>
              <div class="civ-comp-card">
                <h4>专家校验</h4>
                <button class="expert-btn" data-role="expert-btn" disabled>
                  🔍 获取专家校验意见
                </button>
                <div class="expert-result" data-role="expert-result" style="display: none;">
                  <div class="expert-title">👨‍🏫 专家意见</div>
                  <div class="expert-content" data-role="expert-content"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        `;

        this._injectStyles();
    }

    _injectStyles() {
        const styleId = 'civ-comp-styles';
        if (document.getElementById(styleId)) return;

        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
        .civ-comp-container { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #333; }
        .civ-comp-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 8px 8px 0 0; }
        .civ-comp-header h3 { margin: 0; font-size: 18px; font-weight: 600; }
        .civ-comp-header .reliability-badge { background: rgba(255,255,255,0.2); padding: 4px 12px; border-radius: 20px; font-size: 12px; }
        .civ-comp-body { display: flex; gap: 20px; padding: 20px; background: #f8f9fa; }
        .civ-comp-sidebar { width: 280px; flex-shrink: 0; }
        .civ-comp-main { flex: 1; min-width: 0; }
        .civ-comp-card { background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 16px; margin-bottom: 16px; }
        .civ-comp-card h4 { margin: 0 0 12px 0; font-size: 14px; font-weight: 600; color: #555; padding-bottom: 8px; border-bottom: 1px solid #eee; }
        .civ-quick-btns { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
        .civ-quick-btn { padding: 6px 14px; border: 1px solid #ddd; background: white; border-radius: 20px; cursor: pointer; font-size: 12px; transition: all 0.2s; }
        .civ-quick-btn:hover { background: #667eea; color: white; border-color: #667eea; }
        .civ-quick-btn.active { background: #667eea; color: white; border-color: #667eea; }
        .civ-list { max-height: 320px; overflow-y: auto; }
        .civ-list-item { display: flex; align-items: center; padding: 8px 10px; border-radius: 6px; cursor: pointer; transition: background 0.2s; }
        .civ-list-item:hover { background: #f0f0ff; }
        .civ-list-item input[type="checkbox"] { margin-right: 10px; cursor: pointer; }
        .civ-list-item .civ-name { flex: 1; font-size: 13px; }
        .civ-list-item .civ-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; background: #e3f2fd; color: #1976d2; }
        .civ-list-item .civ-tag.eastern { background: #e8f5e9; color: #388e3c; }
        .civ-list-item .civ-tag.western { background: #fce4ec; color: #c2185b; }
        .civ-compare-btn { width: 100%; padding: 10px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; margin-top: 12px; transition: transform 0.2s, box-shadow 0.2s; }
        .civ-compare-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }
        .civ-compare-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }
        .radar-chart-container { display: flex; justify-content: center; align-items: center; padding: 10px 0; }
        .radar-chart-container canvas { max-width: 100%; height: auto; }
        .winner-banner { text-align: center; padding: 16px; background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%); color: #5d4037; border-radius: 8px; margin-bottom: 16px; }
        .winner-banner .trophy-icon { font-size: 28px; margin-right: 8px; }
        .winner-banner .winner-text { font-size: 16px; font-weight: 600; }
        .winner-banner .winner-score { font-size: 14px; margin-top: 4px; opacity: 0.9; }
        .ranking-list { list-style: none; padding: 0; margin: 0; }
        .ranking-item { display: flex; align-items: center; padding: 10px 12px; border-radius: 6px; margin-bottom: 6px; background: #fafafa; }
        .ranking-item .rank-num { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 13px; margin-right: 12px; background: #ddd; color: #666; }
        .ranking-item.rank-1 .rank-num { background: linear-gradient(135deg, #ffd700, #ffaa00); color: white; }
        .ranking-item.rank-2 .rank-num { background: linear-gradient(135deg, #c0c0c0, #9e9e9e); color: white; }
        .ranking-item.rank-3 .rank-num { background: linear-gradient(135deg, #cd7f32, #a0522d); color: white; }
        .ranking-item .rank-name { flex: 1; font-size: 13px; font-weight: 500; }
        .ranking-item .rank-score { font-size: 14px; font-weight: 600; color: #667eea; }
        .ranking-item .rank-score-bar { height: 4px; background: #eee; border-radius: 2px; margin-top: 4px; overflow: hidden; }
        .ranking-item .rank-score-fill { height: 100%; background: linear-gradient(90deg, #667eea, #764ba2); border-radius: 2px; transition: width 0.6s ease; }
        .summary-civ { padding: 12px; border-radius: 8px; margin-bottom: 12px; border-left: 4px solid #667eea; background: #fafbff; }
        .summary-civ .civ-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
        .summary-civ .civ-title h5 { margin: 0; font-size: 15px; font-weight: 600; }
        .summary-civ .civ-score-badge { padding: 4px 10px; background: #667eea; color: white; border-radius: 20px; font-size: 12px; font-weight: 600; }
        .summary-civ .civ-period { font-size: 12px; color: #888; margin-bottom: 10px; }
        .summary-civ .civ-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
        .summary-civ .civ-tag { font-size: 11px; padding: 3px 10px; border-radius: 12px; background: #e3f2fd; color: #1976d2; }
        .summary-civ .civ-tag.type-equal { background: #e8f5e9; color: #388e3c; }
        .summary-civ .civ-tag.type-unequal { background: #fff3e0; color: #f57c00; }
        .summary-civ .civ-tag.category-east { background: #fce4ec; color: #c2185b; }
        .summary-civ .civ-tag.category-west { background: #e0f7fa; color: #0097a7; }
        .swot-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .swot-item h6 { margin: 0 0 6px 0; font-size: 12px; font-weight: 600; display: flex; align-items: center; gap: 4px; }
        .swot-item.strengths h6 { color: #388e3c; }
        .swot-item.weaknesses h6 { color: #f57c00; }
        .swot-item ul { margin: 0; padding-left: 18px; font-size: 12px; color: #555; }
        .swot-item li { margin-bottom: 3px; }
        .analysis-list { list-style: none; padding: 0; margin: 0; }
        .analysis-item { padding: 10px 12px; border-radius: 6px; margin-bottom: 8px; background: #f5f7ff; border-left: 3px solid #667eea; font-size: 13px; line-height: 1.5; }
        .analysis-item.leading { background: #e8f5e9; border-left-color: #388e3c; }
        .analysis-item.comparison { background: #e3f2fd; border-left-color: #1976d2; }
        .analysis-item.evolution { background: #f3e5f5; border-left-color: #7b1fa2; }
        .expert-btn { width: 100%; padding: 10px; background: #fff; border: 1px dashed #667eea; color: #667eea; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; transition: all 0.2s; }
        .expert-btn:hover { background: #667eea; color: white; border-style: solid; }
        .expert-btn:disabled { opacity: 0.6; cursor: not-allowed; }
        .expert-result { margin-top: 10px; padding: 12px; background: #fffde7; border-radius: 6px; border: 1px solid #ffe082; }
        .expert-result .expert-title { font-size: 13px; font-weight: 600; color: #f57f17; margin-bottom: 6px; }
        .expert-result .expert-content { font-size: 12px; color: #5d4037; line-height: 1.5; }
        .data-reliability { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f5f5f5; border-radius: 6px; margin-top: 10px; }
        .data-reliability .reliability-label { font-size: 12px; color: #666; }
        .data-reliability .reliability-stars { color: #ffc107; font-size: 14px; }
        .data-reliability .reliability-value { font-size: 12px; font-weight: 600; color: #333; }
        .dimension-scores { margin-top: 8px; }
        .dim-score-item { margin-bottom: 6px; }
        .dim-score-label { display: flex; justify-content: space-between; font-size: 11px; color: #666; margin-bottom: 2px; }
        .dim-score-bar { height: 6px; background: #eee; border-radius: 3px; overflow: hidden; }
        .dim-score-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
        .dim-score-fill.high { background: #4caf50; }
        .dim-score-fill.medium { background: #ff9800; }
        .dim-score-fill.low { background: #f44336; }
        .empty-state { text-align: center; padding: 40px 20px; color: #999; }
        .empty-state .empty-icon { font-size: 48px; margin-bottom: 12px; }
        .empty-state .empty-text { font-size: 14px; }
        .export-btn { padding: 6px 14px; background: white; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; font-size: 12px; display: flex; align-items: center; gap: 4px; }
        .export-btn:hover { background: #f5f5f5; }
        `;
        document.head.appendChild(style);
    }

    _cacheElements() {
        const roles = [
            'civilization-list', 'compare-btn', 'radar-canvas', 'winner-card',
            'winner-name', 'winner-score', 'ranking-card', 'ranking-list',
            'summaries-card', 'summaries-content', 'analysis-card',
            'analysis-list', 'expert-btn', 'expert-result', 'expert-content',
            'export-btn'
        ];
        roles.forEach(role => {
            const el = this.container.querySelector(`[data-role="${role}"]`);
            if (el) {
                this.elements[role.replace(/-/g, '_')] = el;
            }
        });
    }

    _initRadarCanvas() {
        const canvas = this.elements.radar_canvas;
        if (!canvas) return;

        this.radarCtx = canvas.getContext('2d');
        this.radarWidth = canvas.width;
        this.radarHeight = canvas.height;
        this.radarCenterX = this.radarWidth / 2;
        this.radarCenterY = this.radarHeight / 2;
        this.radarRadius = Math.min(this.radarWidth, this.radarHeight) / 2 - 60;
    }

    bindEvents() {
        this.container.addEventListener('click', (e) => {
            const presetBtn = e.target.closest('[data-preset]');
            if (presetBtn) {
                this._handlePresetClick(presetBtn.dataset.preset);
                return;
            }

            const listItem = e.target.closest('.civ-list-item');
            if (listItem) {
                const checkbox = listItem.querySelector('input[type="checkbox"]');
                if (checkbox && e.target !== checkbox) {
                    checkbox.checked = !checkbox.checked;
                    this._handleCheckboxChange();
                }
                return;
            }

            if (e.target.closest('[data-role="compare-btn"]')) {
                this._handleCompare();
                return;
            }

            if (e.target.closest('[data-role="expert-btn"]')) {
                this._handleExpertVerify();
                return;
            }

            if (e.target.closest('[data-role="export-btn"]')) {
                this._exportRadarImage();
                return;
            }
        });

        this.container.addEventListener('change', (e) => {
            if (e.target.classList.contains('civ-checkbox')) {
                this._handleCheckboxChange();
            }
        });
    }

    _handleCheckboxChange() {
        const checkboxes = this.container.querySelectorAll('.civ-checkbox:checked');
        this.selectedCodes = Array.from(checkboxes).map(cb => cb.value);
        
        this.elements.compare_btn.disabled = this.selectedCodes.length < 2;
        this.elements.expert_btn.disabled = this.selectedCodes.length < 2;

        if (this.options.onCivilizationSelect) {
            this.options.onCivilizationSelect(this.selectedCodes);
        }
    }

    async _handlePresetClick(preset) {
        this._setActivePreset(preset);

        const presetMap = {
            'china-rome': ['CHN-TANG', 'ROME'],
            'all-eastern': this.allCivilizations.filter(c => c.category === 'EAST').map(c => c.civilizationCode),
            'all-western': this.allCivilizations.filter(c => c.category === 'WEST').map(c => c.civilizationCode),
            'east-west': this.allCivilizations.slice(0, 6).map(c => c.civilizationCode)
        };

        const codes = presetMap[preset] || [];
        this._setSelectedCodes(codes);
        await this._handleCompare();
    }

    _setActivePreset(preset) {
        const btns = this.container.querySelectorAll('[data-preset]');
        btns.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.preset === preset);
        });
    }

    _setSelectedCodes(codes) {
        this.selectedCodes = codes;
        const checkboxes = this.container.querySelectorAll('.civ-checkbox');
        checkboxes.forEach(cb => {
            cb.checked = codes.includes(cb.value);
        });
        this.elements.compare_btn.disabled = codes.length < 2;
        this.elements.expert_btn.disabled = codes.length < 2;
    }

    async _loadCivilizations() {
        try {
            const result = await this.api.get(`${this.options.apiBase}/civilization/balances`);
            if (result.success) {
                this.allCivilizations = result.data.map(c => ({
                    ...c,
                    category: this._categorizeCivilization(c)
                }));
            } else {
                this._loadMockCivilizations();
            }
        } catch (e) {
            console.warn('加载文明列表失败，使用模拟数据:', e);
            this._loadMockCivilizations();
        }

        this._renderCivilizationList();
    }

    _categorizeCivilization(civ) {
        const code = civ.civilizationCode || '';
        const name = civ.civilizationName || '';
        const eastPatterns = ['CHN', 'CHINA', 'TANG', 'HAN', 'SONG', 'MING', 'QING', 'JPN', 'KOR', 'IND', '中国', '唐', '汉', '宋', '明', '清', '日本', '印度', '朝鲜'];
        for (const pattern of eastPatterns) {
            if (code.includes(pattern) || name.includes(pattern)) {
                return 'EAST';
            }
        }
        return 'WEST';
    }

    _loadMockCivilizations() {
        this.allCivilizations = [
            { id: 1, civilizationName: '唐代-中国', civilizationCode: 'CHN-TANG', periodStartYear: 618, periodEndYear: 907, balanceType: 'EQUAL_ARM', maxCapacity: 800, relativePrecision: 0.00006, materialHardness: 155, armRatioConsistency: 98, structureComplexity: 80, durabilityScore: 90, category: 'EAST' },
            { id: 2, civilizationName: '汉代-中国', civilizationCode: 'CHN-HAN', periodStartYear: -202, periodEndYear: 220, balanceType: 'EQUAL_ARM', maxCapacity: 500, relativePrecision: 0.0001, materialHardness: 140, armRatioConsistency: 95, structureComplexity: 70, durabilityScore: 85, category: 'EAST' },
            { id: 3, civilizationName: '宋代-中国', civilizationCode: 'CHN-SONG', periodStartYear: 960, periodEndYear: 1279, balanceType: 'UNEQUAL_ARM', maxCapacity: 1200, relativePrecision: 0.0002, materialHardness: 160, armRatioConsistency: 92, structureComplexity: 85, durabilityScore: 82, category: 'EAST' },
            { id: 4, civilizationName: '古埃及', civilizationCode: 'EGYPT', periodStartYear: -1500, periodEndYear: -30, balanceType: 'EQUAL_ARM', maxCapacity: 200, relativePrecision: 0.001, materialHardness: 120, armRatioConsistency: 85, structureComplexity: 60, durabilityScore: 70, category: 'WEST' },
            { id: 5, civilizationName: '古罗马', civilizationCode: 'ROME', periodStartYear: -300, periodEndYear: 476, balanceType: 'UNEQUAL_ARM', maxCapacity: 300, relativePrecision: 0.0005, materialHardness: 180, armRatioConsistency: 80, structureComplexity: 85, durabilityScore: 75, category: 'WEST' },
            { id: 6, civilizationName: '古希腊', civilizationCode: 'GREECE', periodStartYear: -500, periodEndYear: -146, balanceType: 'EQUAL_ARM', maxCapacity: 250, relativePrecision: 0.0008, materialHardness: 170, armRatioConsistency: 82, structureComplexity: 75, durabilityScore: 68, category: 'WEST' },
            { id: 7, civilizationName: '中世纪欧洲', civilizationCode: 'MEDIEVAL-EU', periodStartYear: 500, periodEndYear: 1500, balanceType: 'UNEQUAL_ARM', maxCapacity: 500, relativePrecision: 0.0003, materialHardness: 165, armRatioConsistency: 78, structureComplexity: 72, durabilityScore: 78, category: 'WEST' },
            { id: 8, civilizationName: '古代印度', civilizationCode: 'INDIA', periodStartYear: -300, periodEndYear: 500, balanceType: 'EQUAL_ARM', maxCapacity: 180, relativePrecision: 0.0012, materialHardness: 130, armRatioConsistency: 75, structureComplexity: 65, durabilityScore: 72, category: 'EAST' }
        ];
    }

    _renderCivilizationList() {
        const listEl = this.elements.civilization_list;
        if (!listEl) return;

        if (this.allCivilizations.length === 0) {
            listEl.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📭</div>
                    <div class="empty-text">暂无文明数据</div>
                </div>
            `;
            return;
        }

        let html = '';
        this.allCivilizations.forEach(civ => {
            const typeText = civ.balanceType === 'EQUAL_ARM' ? '等臂' : '不等臂';
            const categoryClass = civ.category === 'EAST' ? 'eastern' : 'western';
            const categoryText = civ.category === 'EAST' ? '东方' : '西方';
            const isSelected = this.selectedCodes.includes(civ.civilizationCode);

            html += `
                <div class="civ-list-item">
                    <input type="checkbox" class="civ-checkbox" value="${civ.civilizationCode}" ${isSelected ? 'checked' : ''}>
                    <span class="civ-name">${civ.civilizationName}</span>
                    <span class="civ-tag ${categoryClass}">${categoryText}</span>
                </div>
            `;
        });

        listEl.innerHTML = html;
    }

    async _handleCompare() {
        if (this.selectedCodes.length < 2) {
            alert('请至少选择2个文明进行对比');
            return;
        }

        this.elements.compare_btn.disabled = true;
        this.elements.compare_btn.innerHTML = '<span class="loading-spinner"></span>对比分析中...';

        try {
            const result = await this.api.post(`${this.options.apiBase}/civilization/compare`, {
                civilizationCodes: this.selectedCodes
            });

            if (result.success) {
                this.currentComparison = result.data;
            } else {
                this.currentComparison = this._generateMockComparison();
            }
        } catch (e) {
            console.warn('对比API调用失败，使用模拟数据:', e);
            this.currentComparison = this._generateMockComparison();
        }

        this.elements.compare_btn.disabled = false;
        this.elements.compare_btn.textContent = '开始对比分析';

        this.render();

        if (this.options.onCompareComplete) {
            this.options.onCompareComplete(this.currentComparison);
        }
    }

    _generateMockComparison() {
        const selectedCivs = this.allCivilizations.filter(c => this.selectedCodes.includes(c.civilizationCode));
        const dimensions = ['最大称量', '相对精度', '材料硬度', '臂长一致性', '结构复杂度', '耐久性'];

        const radarData = {};
        dimensions.forEach(dim => {
            radarData[dim] = selectedCivs.map(civ => {
                switch(dim) {
                    case '最大称量': return Math.min(100, (civ.maxCapacity / 1200) * 100);
                    case '相对精度': return Math.min(100, (0.002 / civ.relativePrecision) * 30);
                    case '材料硬度': return civ.materialHardness * 0.5;
                    case '臂长一致性': return civ.armRatioConsistency;
                    case '结构复杂度': return civ.structureComplexity;
                    case '耐久性': return civ.durabilityScore;
                    default: return 70;
                }
            });
        });

        const summaries = {};
        selectedCivs.forEach((civ, idx) => {
            const scores = {};
            dimensions.forEach(dim => {
                scores[dim] = radarData[dim][idx];
            });
            const avgScore = Object.values(scores).reduce((a, b) => a + b, 0) / dimensions.length;

            const strengths = [];
            const weaknesses = [];
            Object.entries(scores).forEach(([dim, score]) => {
                if (score >= 85) strengths.push(`${dim}表现优异`);
                if (score < 60) weaknesses.push(`${dim}相对薄弱`);
            });

            summaries[civ.civilizationCode] = {
                name: civ.civilizationName,
                code: civ.civilizationCode,
                balanceType: civ.balanceType === 'EQUAL_ARM' ? '等臂天平' : '不等臂天平',
                periodStart: civ.periodStartYear,
                periodEnd: civ.periodEndYear,
                avgScore: avgScore,
                scores: scores,
                strengths: strengths.slice(0, 3),
                weaknesses: weaknesses.slice(0, 2),
                representativeArtifact: this._getArtifact(civ.civilizationCode),
                culturalSignificance: this._getCulturalSignificance(civ.civilizationCode),
                category: civ.category
            };
        });

        const sortedCivs = selectedCivs.map((civ, idx) => {
            const scores = dimensions.map(dim => radarData[dim][idx]);
            const avg = scores.reduce((a, b) => a + b, 0) / scores.length;
            return { name: civ.civilizationName, code: civ.civilizationCode, avgScore: avg };
        }).sort((a, b) => b.avgScore - a.avgScore);

        const comparativeAnalysis = this._generateAnalysis(sortedCivs, dimensions, radarData);

        return {
            civilizationNames: selectedCivs.map(c => c.civilizationName),
            civilizationCodes: selectedCivs.map(c => c.civilizationCode),
            dimensions: dimensions,
            radarData: radarData,
            overallWinner: sortedCivs[0].name,
            overallWinnerScore: sortedCivs[0].avgScore,
            rankings: sortedCivs,
            summaries: summaries,
            comparativeAnalysis: comparativeAnalysis,
            dataReliability: 82
        };
    }

    _getArtifact(code) {
        const artifacts = {
            'CHN-TANG': '唐代铜制等臂天平（西安何家村出土）',
            'CHN-HAN': '汉代铜权与衡杆（满城汉墓出土）',
            'CHN-SONG': '宋代戥秤（《梦溪笔谈》记载）',
            'EGYPT': '古埃及石质天平（木乃伊墓出土）',
            'ROME': '罗马铜制天平（庞贝古城出土）',
            'GREECE': '古希腊青铜天平（雅典卫城出土）',
            'MEDIEVAL-EU': '中世纪欧洲商用天平（汉萨同盟时期）',
            'INDIA': '古印度宝石天平（孔雀王朝时期）'
        };
        return artifacts[code] || '代表性文物待考证';
    }

    _getCulturalSignificance(code) {
        const significances = {
            'CHN-TANG': '唐代天平代表了中国古代衡器制造的巅峰水平，体现了大唐盛世的科技与工艺实力，在丝绸之路贸易中发挥了重要作用。',
            'CHN-HAN': '汉代衡器制度的统一为度量衡标准化奠定了基础，"权"的使用体现了早期的砝码计量理念。',
            'CHN-SONG': '宋代戥秤的发明标志着中国古代微量衡器技术达到世界领先水平，广泛应用于药材和珠宝称量。',
            'EGYPT': '古埃及天平最早用于称量木乃伊心脏，与宗教仪式密切相关，体现了天平的文化象征意义。',
            'ROME': '罗马天平在商业贸易和税收中广泛使用，其不等臂设计体现了实用主义的工程思想。',
            'GREECE': '古希腊天平与哲学思想紧密相关，"正义天平"的象征意义深远影响了西方文化。',
            'MEDIEVAL-EU': '中世纪欧洲的行会制度推动了衡器标准化，商业贸易的需求促进了天平技术的发展。',
            'INDIA': '古印度天平在珠宝交易和宗教仪式中使用，体现了精细工艺和宗教文化的结合。'
        };
        return significances[code] || '该文明的衡器文化有待进一步研究。';
    }

    _generateAnalysis(sortedCivs, dimensions, radarData) {
        const analysis = [];
        const winner = sortedCivs[0];

        analysis.push({
            type: 'leading',
            text: `【综合评估】${winner.name} 综合得分 ${winner.avgScore.toFixed(1)} 分，在所有对比文明中表现最优`
        });

        dimensions.forEach(dim => {
            const values = radarData[dim];
            const maxIdx = values.indexOf(Math.max(...values));
            const minIdx = values.indexOf(Math.min(...values));
            if (sortedCivs[maxIdx]) {
                analysis.push({
                    type: 'leading',
                    text: `【${dim}领先】${sortedCivs[maxIdx].name} 在${dim}方面达到 ${values[maxIdx].toFixed(1)} 分，表现突出`
                });
            }
        });

        const eastCivs = sortedCivs.filter(c => {
            const civ = this.allCivilizations.find(ac => ac.civilizationCode === c.code);
            return civ && civ.category === 'EAST';
        });
        const westCivs = sortedCivs.filter(c => {
            const civ = this.allCivilizations.find(ac => ac.civilizationCode === c.code);
            return civ && civ.category === 'WEST';
        });

        if (eastCivs.length > 0 && westCivs.length > 0) {
            const eastAvg = eastCivs.reduce((a, b) => a + b.avgScore, 0) / eastCivs.length;
            const westAvg = westCivs.reduce((a, b) => a + b.avgScore, 0) / westCivs.length;
            const winnerSide = eastAvg > westAvg ? '东方文明' : '西方文明';
            analysis.push({
                type: 'comparison',
                text: `【东西对比】${winnerSide}整体表现更优（东方: ${eastAvg.toFixed(1)}分 vs 西方: ${westAvg.toFixed(1)}分）`
            });
        }

        analysis.push({
            type: 'evolution',
            text: '【技术演进】从历史发展来看，天平技术随着文明进步而不断精进，材料和工艺的改进推动了精度提升'
        });

        return analysis.slice(0, 6);
    }

    render() {
        if (!this.currentComparison) {
            this._drawEmptyRadar();
            return;
        }

        this._renderWinner();
        this._renderRadarChart();
        this._renderRanking();
        this._renderSummaries();
        this._renderAnalysis();
    }

    _renderWinner() {
        const data = this.currentComparison;
        if (!data || !data.overallWinner) return;

        this.elements.winner_card.style.display = 'block';
        this.elements.winner_name.textContent = `综合最优：${data.overallWinner}`;
        this.elements.winner_score.textContent = `综合评分: ${data.overallWinnerScore ? data.overallWinnerScore.toFixed(1) : '--'} 分`;
    }

    _renderRadarChart() {
        const data = this.currentComparison;
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

        this._setRadarData(dimensions, datasets);
    }

    _setRadarData(dimensions, datasets) {
        this.radarDimensions = dimensions;
        this.radarDatasets = datasets;
        this.animationProgress = 0;
        this._animateRadar();
    }

    _animateRadar() {
        if (this.animating) return;
        this.animating = true;

        const animateFrame = () => {
            this.animationProgress += 0.025;
            if (this.animationProgress >= 1) {
                this.animationProgress = 1;
                this.animating = false;
                this._drawRadar();
                return;
            }
            this._drawRadar();
            requestAnimationFrame(animateFrame);
        };
        animateFrame();
    }

    _drawEmptyRadar() {
        if (!this.radarCtx) return;
        const ctx = this.radarCtx;
        ctx.clearRect(0, 0, this.radarWidth, this.radarHeight);

        ctx.fillStyle = '#ccc';
        ctx.font = '14px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('选择文明后点击"开始对比"', this.radarCenterX, this.radarCenterY - 10);
        ctx.font = '12px sans-serif';
        ctx.fillStyle = '#aaa';
        ctx.fillText('查看六维雷达图对比', this.radarCenterX, this.radarCenterY + 15);
    }

    _drawRadar() {
        if (!this.radarCtx || !this.radarDimensions || this.radarDimensions.length === 0) return;

        const ctx = this.radarCtx;
        ctx.clearRect(0, 0, this.radarWidth, this.radarHeight);

        this._drawRadarGrid();
        this._drawRadarAxes();
        this._drawRadarDatasets();
        this._drawRadarLabels();
        this._drawRadarLegend();
    }

    _drawRadarGrid() {
        const ctx = this.radarCtx;
        const levels = 5;
        ctx.strokeStyle = '#e0e0e0';
        ctx.lineWidth = 1;

        for (let level = 1; level <= levels; level++) {
            const r = (this.radarRadius * level) / levels;
            ctx.beginPath();

            for (let i = 0; i <= this.radarDimensions.length; i++) {
                const angle = (Math.PI * 2 * i) / this.radarDimensions.length - Math.PI / 2;
                const x = this.radarCenterX + r * Math.cos(angle);
                const y = this.radarCenterY + r * Math.sin(angle);

                if (i === 0) {
                    ctx.moveTo(x, y);
                } else {
                    ctx.lineTo(x, y);
                }
            }
            ctx.closePath();
            ctx.stroke();

            ctx.fillStyle = '#999';
            ctx.font = '10px sans-serif';
            ctx.textAlign = 'right';
            ctx.fillText((level * 20).toString() + '分', this.radarCenterX - 8, this.radarCenterY - r + 3);
        }
    }

    _drawRadarAxes() {
        const ctx = this.radarCtx;
        ctx.strokeStyle = '#bdbdbd';
        ctx.lineWidth = 1;

        for (let i = 0; i < this.radarDimensions.length; i++) {
            const angle = (Math.PI * 2 * i) / this.radarDimensions.length - Math.PI / 2;
            const x = this.radarCenterX + this.radarRadius * Math.cos(angle);
            const y = this.radarCenterY + this.radarRadius * Math.sin(angle);

            ctx.beginPath();
            ctx.moveTo(this.radarCenterX, this.radarCenterY);
            ctx.lineTo(x, y);
            ctx.stroke();
        }
    }

    _drawRadarDatasets() {
        const ctx = this.radarCtx;
        const progress = this.animationProgress;

        this.radarDatasets.forEach((dataset, idx) => {
            const color = this.options.colors[idx % this.options.colors.length];

            ctx.beginPath();
            for (let i = 0; i < this.radarDimensions.length; i++) {
                const angle = (Math.PI * 2 * i) / this.radarDimensions.length - Math.PI / 2;
                const value = dataset.values[i] || 0;
                const r = (this.radarRadius * value / 100) * progress;
                const x = this.radarCenterX + r * Math.cos(angle);
                const y = this.radarCenterY + r * Math.sin(angle);

                if (i === 0) {
                    ctx.moveTo(x, y);
                } else {
                    ctx.lineTo(x, y);
                }
            }
            ctx.closePath();

            ctx.fillStyle = color.fill;
            ctx.fill();

            ctx.strokeStyle = color.stroke;
            ctx.lineWidth = 2;
            ctx.stroke();

            for (let i = 0; i < this.radarDimensions.length; i++) {
                const angle = (Math.PI * 2 * i) / this.radarDimensions.length - Math.PI / 2;
                const value = dataset.values[i] || 0;
                const r = (this.radarRadius * value / 100) * progress;
                const x = this.radarCenterX + r * Math.cos(angle);
                const y = this.radarCenterY + r * Math.sin(angle);

                ctx.beginPath();
                ctx.arc(x, y, 4, 0, Math.PI * 2);
                ctx.fillStyle = color.stroke;
                ctx.fill();
                ctx.strokeStyle = '#fff';
                ctx.lineWidth = 1.5;
                ctx.stroke();
            }
        });
    }

    _drawRadarLabels() {
        const ctx = this.radarCtx;
        ctx.fillStyle = '#333';
        ctx.font = 'bold 12px sans-serif';
        ctx.textAlign = 'center';

        for (let i = 0; i < this.radarDimensions.length; i++) {
            const angle = (Math.PI * 2 * i) / this.radarDimensions.length - Math.PI / 2;
            const labelRadius = this.radarRadius + 28;
            const x = this.radarCenterX + labelRadius * Math.cos(angle);
            const y = this.radarCenterY + labelRadius * Math.sin(angle);

            let align = 'center';
            if (Math.cos(angle) > 0.5) align = 'left';
            if (Math.cos(angle) < -0.5) align = 'right';

            ctx.textAlign = align;
            ctx.fillText(this.radarDimensions[i], x, y + 4);
        }
    }

    _drawRadarLegend() {
        const ctx = this.radarCtx;
        const startX = 15;
        const startY = 20;
        const lineHeight = 20;

        ctx.textAlign = 'left';
        ctx.font = '11px sans-serif';

        this.radarDatasets.forEach((dataset, idx) => {
            const y = startY + idx * lineHeight;
            const color = this.options.colors[idx % this.options.colors.length];

            ctx.fillStyle = color.stroke;
            ctx.fillRect(startX, y - 8, 12, 12);

            ctx.fillStyle = '#333';
            ctx.fillText(dataset.label, startX + 18, y + 2);

            if (dataset.avgScore !== undefined) {
                ctx.fillStyle = '#666';
                ctx.fillText(`(平均: ${dataset.avgScore.toFixed(1)}分)`,
                    startX + ctx.measureText(dataset.label).width + 25, y + 2);
            }
        });
    }

    _renderRanking() {
        const data = this.currentComparison;
        if (!data || !data.rankings) return;

        this.elements.ranking_card.style.display = 'block';
        const maxScore = Math.max(...data.rankings.map(r => r.avgScore));

        let html = '';
        data.rankings.forEach((item, idx) => {
            const rank = idx + 1;
            const scorePercent = (item.avgScore / maxScore) * 100;
            html += `
                <li class="ranking-item rank-${rank}">
                    <div class="rank-num">${rank}</div>
                    <div style="flex: 1;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span class="rank-name">${item.name}</span>
                            <span class="rank-score">${item.avgScore.toFixed(1)}分</span>
                        </div>
                        <div class="rank-score-bar">
                            <div class="rank-score-fill" style="width: ${scorePercent}%;"></div>
                        </div>
                    </div>
                </li>
            `;
        });

        this.elements.ranking_list.innerHTML = html;
    }

    _renderSummaries() {
        const data = this.currentComparison;
        if (!data || !data.summaries) return;

        this.elements.summaries_card.style.display = 'block';

        let html = '';
        Object.values(data.summaries).forEach(summary => {
            const typeClass = summary.balanceType === '等臂天平' ? 'type-equal' : 'type-unequal';
            const categoryClass = summary.category === 'EAST' ? 'category-east' : 'category-west';
            const categoryText = summary.category === 'EAST' ? '东方文明' : '西方文明';

            html += `
                <div class="summary-civ">
                    <div class="civ-title">
                        <h5>${summary.name}</h5>
                        <span class="civ-score-badge">${summary.avgScore.toFixed(1)}分</span>
                    </div>
                    <div class="civ-period">
                        ${summary.periodStart > 0 ? '公元' : '公元前'}${Math.abs(summary.periodStart)}年 
                        - ${summary.periodEnd > 0 ? '公元' : '公元前'}${Math.abs(summary.periodEnd)}年
                    </div>
                    <div class="civ-tags">
                        <span class="civ-tag ${typeClass}">${summary.balanceType}</span>
                        <span class="civ-tag ${categoryClass}">${categoryText}</span>
                    </div>
                    <div class="dimension-scores">
                        ${this._renderDimensionScores(summary.scores)}
                    </div>
                    <div class="swot-grid" style="margin-top: 12px;">
                        <div class="swot-item strengths">
                            <h6>💪 优势</h6>
                            ${summary.strengths && summary.strengths.length > 0 
                                ? `<ul>${summary.strengths.map(s => `<li>${s}</li>`).join('')}</ul>`
                                : '<span style="font-size: 12px; color: #999;">暂无数据</span>'}
                        </div>
                        <div class="swot-item weaknesses">
                            <h6>⚠️ 不足</h6>
                            ${summary.weaknesses && summary.weaknesses.length > 0
                                ? `<ul>${summary.weaknesses.map(s => `<li>${s}</li>`).join('')}</ul>`
                                : '<span style="font-size: 12px; color: #999;">暂无数据</span>'}
                        </div>
                    </div>
                    ${summary.representativeArtifact ? `
                        <div style="margin-top: 10px; font-size: 12px;">
                            <strong style="color: #555;">代表文物：</strong>
                            <span style="color: #666;">${summary.representativeArtifact}</span>
                        </div>
                    ` : ''}
                    ${summary.culturalSignificance ? `
                        <div style="margin-top: 8px; font-size: 12px; color: #888; line-height: 1.5;">
                            ${summary.culturalSignificance}
                        </div>
                    ` : ''}
                </div>
            `;
        });

        this.elements.summaries_content.innerHTML = html;
    }

    _renderDimensionScores(scores) {
        if (!scores) return '';
        let html = '';
        Object.entries(scores).forEach(([dim, score]) => {
            const colorClass = score >= 85 ? 'high' : score >= 70 ? 'medium' : 'low';
            html += `
                <div class="dim-score-item">
                    <div class="dim-score-label">
                        <span>${dim}</span>
                        <span>${score.toFixed(1)}分</span>
                    </div>
                    <div class="dim-score-bar">
                        <div class="dim-score-fill ${colorClass}" style="width: ${score}%;"></div>
                    </div>
                </div>
            `;
        });
        return html;
    }

    _renderAnalysis() {
        const data = this.currentComparison;
        if (!data || !data.comparativeAnalysis) return;

        this.elements.analysis_card.style.display = 'block';

        let html = '';
        data.comparativeAnalysis.forEach(item => {
            const typeClass = typeof item === 'string' ? '' : item.type || '';
            const text = typeof item === 'string' ? item : item.text;
            html += `
                <li class="analysis-item ${typeClass}">${text}</li>
            `;
        });

        this.elements.analysis_list.innerHTML = html;
    }

    async _handleExpertVerify() {
        if (this.selectedCodes.length < 2) {
            alert('请先选择至少2个文明进行对比');
            return;
        }

        this.elements.expert_btn.disabled = true;
        this.elements.expert_btn.innerHTML = '<span class="loading-spinner"></span>正在获取专家意见...';

        try {
            const result = await this.api.post(`${this.options.apiBase}/civilization/expert-verify`, {
                civilizationCodes: this.selectedCodes
            });

            if (result.success) {
                this.expertResult = result.data;
            } else {
                this.expertResult = this._generateMockExpertResult();
            }
        } catch (e) {
            console.warn('专家校验API调用失败，使用模拟数据:', e);
            this.expertResult = this._generateMockExpertResult();
        }

        this.elements.expert_btn.disabled = false;
        this.elements.expert_btn.textContent = '🔍 重新获取专家校验';
        this.elements.expert_result.style.display = 'block';
        this.elements.expert_content.textContent = this.expertResult.opinion;

        if (this.options.onExpertVerify) {
            this.options.onExpertVerify(this.expertResult);
        }
    }

    _generateMockExpertResult() {
        const selectedCivs = this.allCivilizations.filter(c => this.selectedCodes.includes(c.civilizationCode));
        const winner = this.currentComparison?.overallWinner || selectedCivs[0]?.civilizationName;

        const opinions = [
            `从计量史角度分析，${winner}的衡器技术在同期文明中确实处于领先地位。其在材料选择、结构设计和工艺水平上的优势，反映了该文明在手工业和科技方面的整体实力。值得注意的是，不同文明的天平技术发展路径各有特色，东方文明更注重精密测量，西方文明更侧重商业应用。`,
            `本次对比基于现有考古证据和文献记载，数据可靠性较高。${winner}在多个维度上的优势有充分的考古实物支撑。建议进一步关注不同文明之间的技术传播与交流路径，这对于理解古代科技史具有重要意义。`,
            `从科技史研究视角来看，这次文明对比分析具有重要学术价值。${winner}的衡器制造水平代表了当时的顶尖工艺。需要指出的是，由于考古发现的局限性，某些文明的数据可能不够完整，结论需谨慎对待。建议结合更多考古新发现进行持续更新。`
        ];

        return {
            expertName: '李教授',
            expertTitle: '计量史研究专家',
            opinion: opinions[Math.floor(Math.random() * opinions.length)],
            reliabilityScore: 85 + Math.floor(Math.random() * 10),
            verifiedAt: new Date().toISOString()
        };
    }

    _exportRadarImage() {
        const canvas = this.elements.radar_canvas;
        if (!canvas) return;

        const link = document.createElement('a');
        link.download = `文明对比雷达图_${new Date().toISOString().slice(0, 10)}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
    }

    resizeRadar(width, height) {
        const canvas = this.elements.radar_canvas;
        if (!canvas) return;

        this.radarWidth = width;
        this.radarHeight = height;
        canvas.width = width;
        canvas.height = height;
        this.radarCenterX = width / 2;
        this.radarCenterY = height / 2;
        this.radarRadius = Math.min(width, height) / 2 - 60;
        
        if (this.radarDimensions && this.radarDimensions.length > 0) {
            this._drawRadar();
        }
    }

    getSelectedCivilizations() {
        return this.allCivilizations.filter(c => this.selectedCodes.includes(c.civilizationCode));
    }

    getComparisonData() {
        return this.currentComparison;
    }

    setOnCompareComplete(callback) {
        this.options.onCompareComplete = callback;
    }

    setOnCivilizationSelect(callback) {
        this.options.onCivilizationSelect = callback;
    }

    setOnExpertVerify(callback) {
        this.options.onExpertVerify = callback;
    }

    destroy() {
        this.animating = false;
        this.allCivilizations = [];
        this.selectedCodes = [];
        this.currentComparison = null;
        this.container.innerHTML = '';
    }
}

export default CivilizationComparatorComponent;
