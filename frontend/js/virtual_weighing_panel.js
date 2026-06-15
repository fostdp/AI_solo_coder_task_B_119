class VirtualWeighingPanelController {
    constructor(api, balance3d) {
        this.api = api;
        this.balance3d = balance3d;
        this.allItems = [];
        this.leftItems = [];
        this.rightItems = [];
        this.currentResult = null;
        this.balanceType = 'EQUAL_ARM';
        this.animating = false;
    }

    async init() {
        await this.loadItems();
        this.bindEvents();
        this.renderItemPalette();
        this.updatePanViews();
        await this.quickExperience();
    }

    async loadItems() {
        try {
            const result = await this.api.get(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.virtualWeighing.items}`
            );
            if (result.success) {
                this.allItems = result.data;
            }
        } catch (e) {
            console.error('加载物品失败:', e);
            this.loadMockItems();
        }
    }

    loadMockItems() {
        this.allItems = [
            { id: 1, name: '秦权 (1斤)', category: 'weight', weightGrams: 253, civilization: 'CHN-QIN', image: '⚖️', description: '秦朝标准权，青铜质地' },
            { id: 2, name: '汉权 (1斤)', category: 'weight', weightGrams: 250, civilization: 'CHN-HAN', image: '⚖️', description: '汉代标准权' },
            { id: 3, name: '唐大斤 (1斤)', category: 'weight', weightGrams: 661, civilization: 'CHN-TANG', image: '⚖️', description: '唐代大斤' },
            { id: 4, name: '罗马磅', category: 'weight', weightGrams: 327, civilization: 'ROME', image: '⚖️', description: '古罗马标准磅' },
            { id: 5, name: '埃及德本', category: 'weight', weightGrams: 91, civilization: 'EGYPT', image: '⚖️', description: '古埃及重量单位' },
            { id: 6, name: '米诺斯塔兰特', category: 'weight', weightGrams: 2900, civilization: 'GREECE', image: '⚖️', description: '古希腊重量单位' },
            { id: 7, name: '100g砝码', category: 'weight', weightGrams: 100, civilization: 'MODERN', image: '⚪', description: '现代标准砝码' },
            { id: 8, name: '50g砝码', category: 'weight', weightGrams: 50, civilization: 'MODERN', image: '⚪', description: '现代标准砝码' },
            { id: 9, name: '20g砝码', category: 'weight', weightGrams: 20, civilization: 'MODERN', image: '⚪', description: '现代标准砝码' },
            { id: 10, name: '10g砝码', category: 'weight', weightGrams: 10, civilization: 'MODERN', image: '⚪', description: '现代标准砝码' },
            { id: 11, name: '5g砝码', category: 'weight', weightGrams: 5, civilization: 'MODERN', image: '⚪', description: '现代标准砝码' },
            { id: 12, name: '1g砝码', category: 'weight', weightGrams: 1, civilization: 'MODERN', image: '⚪', description: '现代标准砝码' },
            { id: 13, name: '汉金饼', category: 'artifact', weightGrams: 250, civilization: 'CHN-HAN', image: '🥇', description: '汉代金饼，纯度99%，重约汉制1斤' },
            { id: 14, name: '秦半两', category: 'artifact', weightGrams: 8, civilization: 'CHN-QIN', image: '🪙', description: '秦代半两钱币' },
            { id: 15, name: '罗马金币', category: 'artifact', weightGrams: 7.3, civilization: 'ROME', image: '🪙', description: '古罗马奥里斯金币' },
            { id: 16, name: '郢爰金版', category: 'artifact', weightGrams: 260, civilization: 'CHN-WARRING', image: '🟨', description: '楚国称量货币' },
            { id: 17, name: '汉代五铢', category: 'artifact', weightGrams: 3.5, civilization: 'CHN-HAN', image: '🪙', description: '汉代五铢钱' },
            { id: 18, name: '唐代开元通宝', category: 'artifact', weightGrams: 4, civilization: 'CHN-TANG', image: '🪙', description: '唐代开元通宝' },
            { id: 19, name: '大米 (1升)', category: 'daily', weightGrams: 800, civilization: 'GENERAL', image: '🍚', description: '古代一升大米约800克' },
            { id: 20, name: '食盐 (1升)', category: 'daily', weightGrams: 1200, civilization: 'GENERAL', image: '🧂', description: '食盐密度较大' },
            { id: 21, name: '丝绸 (1匹)', category: 'daily', weightGrams: 500, civilization: 'CHN-TANG', image: '🧣', description: '唐代一匹丝绸' },
            { id: 22, name: '青铜酒器', category: 'daily', weightGrams: 1500, civilization: 'CHN-ZHOU', image: '🍶', description: '周代青铜爵' },
            { id: 23, name: '唐三彩', category: 'daily', weightGrams: 2000, civilization: 'CHN-TANG', image: '🏺', description: '唐代三彩骆驼俑' },
            { id: 24, name: '羽毛', category: 'fun', weightGrams: 0.01, civilization: 'GENERAL', image: '🪶', description: '看看羽毛有多轻' },
            { id: 25, name: '鸡蛋', category: 'fun', weightGrams: 50, civilization: 'GENERAL', image: '🥚', description: '一个普通鸡蛋' },
            { id: 26, name: '苹果', category: 'fun', weightGrams: 150, civilization: 'GENERAL', image: '🍎', description: '一个红苹果' },
            { id: 27, name: '大象 (假想)', category: 'fun', weightGrams: 5000000, civilization: 'FUN', image: '🐘', description: '曹冲称象的大象' }
        ];
    }

    renderItemPalette() {
        const container = document.getElementById('itemPalette');
        if (!container) return;

        const categories = ['weight', 'artifact', 'daily', 'fun'];
        const categoryNames = {
            weight: '⚖️ 砝码',
            artifact: '🏺 文物',
            daily: '📦 日常',
            fun: '🎮 趣味'
        };

        let html = '';
        categories.forEach(cat => {
            const items = this.allItems.filter(i => i.category === cat);
            if (items.length === 0) return;

            html += `<div class="mb-4">
                <h6 class="mb-2 text-muted">${categoryNames[cat]}</h6>
                <div class="d-flex flex-wrap gap-2">`;

            items.forEach(item => {
                html += `
                    <div class="item-card" draggable="true" data-item-id="${item.id}"
                         title="${item.description}">
                        <span class="item-icon">${item.image}</span>
                        <span class="item-name">${item.name}</span>
                        <span class="item-weight">${this.formatWeight(item.weightGrams)}</span>
                        <div class="item-actions">
                            <button class="btn btn-sm btn-outline-primary add-to-left" data-id="${item.id}">← 左</button>
                            <button class="btn btn-sm btn-outline-primary add-to-right" data-id="${item.id}">右 →</button>
                        </div>
                    </div>
                `;
            });

            html += `</div></div>`;
        });

        container.innerHTML = html;
    }

    bindEvents() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('add-to-left')) {
                const id = parseInt(e.target.dataset.id);
                this.addItem(id, 'left');
            }

            if (e.target.classList.contains('add-to-right')) {
                const id = parseInt(e.target.dataset.id);
                this.addItem(id, 'right');
            }

            if (e.target.classList.contains('remove-item')) {
                const id = parseInt(e.target.dataset.id);
                const side = e.target.dataset.side;
                this.removeItem(id, side);
            }

            if (e.target.id === 'clearLeftBtn') {
                this.clearPan('left');
            }

            if (e.target.id === 'clearRightBtn') {
                this.clearPan('right');
            }

            if (e.target.id === 'swapPansBtn') {
                this.swapPans();
            }

            if (e.target.id === 'weighBtn') {
                this.performWeighing();
            }

            if (e.target.id === 'quickExperienceBtn') {
                this.quickExperience();
            }

            if (e.target.id === 'balanceTypeEqual') {
                this.balanceType = 'EQUAL_ARM';
                this.updateBalanceTypeUI();
            }

            if (e.target.id === 'balanceTypeUnequal') {
                this.balanceType = 'UNEQUAL_ARM';
                this.updateBalanceTypeUI();
            }
        });

        document.addEventListener('dragstart', (e) => {
            if (e.target.classList.contains('item-card')) {
                e.dataTransfer.setData('itemId', e.target.dataset.itemId);
                e.target.classList.add('dragging');
            }
        });

        document.addEventListener('dragend', (e) => {
            if (e.target.classList.contains('item-card')) {
                e.target.classList.remove('dragging');
            }
        });

        ['leftPanDrop', 'rightPanDrop'].forEach(zoneId => {
            const zone = document.getElementById(zoneId);
            if (!zone) return;

            zone.addEventListener('dragover', (e) => {
                e.preventDefault();
                zone.classList.add('drag-over');
            });

            zone.addEventListener('dragleave', () => {
                zone.classList.remove('drag-over');
            });

            zone.addEventListener('drop', (e) => {
                e.preventDefault();
                zone.classList.remove('drag-over');
                const itemId = parseInt(e.dataTransfer.getData('itemId'));
                const side = zoneId === 'leftPanDrop' ? 'left' : 'right';
                this.addItem(itemId, side);
            });
        });
    }

    addItem(itemId, side) {
        const item = this.allItems.find(i => i.id === itemId);
        if (!item) return;

        if (side === 'left') {
            this.leftItems.push({ ...item, uid: Date.now() + Math.random() });
        } else {
            this.rightItems.push({ ...item, uid: Date.now() + Math.random() });
        }

        this.updatePanViews();
    }

    removeItem(uid, side) {
        if (side === 'left') {
            this.leftItems = this.leftItems.filter(i => i.uid !== uid);
        } else {
            this.rightItems = this.rightItems.filter(i => i.uid !== uid);
        }
        this.updatePanViews();
    }

    clearPan(side) {
        if (side === 'left') {
            this.leftItems = [];
        } else {
            this.rightItems = [];
        }
        this.updatePanViews();
    }

    swapPans() {
        const temp = this.leftItems;
        this.leftItems = this.rightItems;
        this.rightItems = temp;
        this.updatePanViews();
    }

    updatePanViews() {
        this.renderPan('left', this.leftItems);
        this.renderPan('right', this.rightItems);

        const leftWeight = this.leftItems.reduce((s, i) => s + i.weightGrams, 0);
        const rightWeight = this.rightItems.reduce((s, i) => s + i.weightGrams, 0);

        const leftSummary = document.getElementById('leftWeightSummary');
        const rightSummary = document.getElementById('rightWeightSummary');

        if (leftSummary) {
            leftSummary.innerHTML = `
                <div class="h5 mb-0">${this.formatWeight(leftWeight)}</div>
                <small class="text-muted">${this.leftItems.length} 件物品</small>
            `;
        }
        if (rightSummary) {
            rightSummary.innerHTML = `
                <div class="h5 mb-0">${this.formatWeight(rightWeight)}</div>
                <small class="text-muted">${this.rightItems.length} 件物品</small>
            `;
        }
    }

    renderPan(side, items) {
        const containerId = side === 'left' ? 'leftPanItems' : 'rightPanItems';
        const container = document.getElementById(containerId);
        if (!container) return;

        if (items.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted py-5">
                    <i class="bi bi-inbox display-4 mb-2 d-block"></i>
                    <div>拖拽物品或点击添加</div>
                </div>
            `;
            return;
        }

        let html = '';
        items.forEach(item => {
            html += `
                <div class="pan-item">
                    <span class="item-icon">${item.image}</span>
                    <div class="flex-grow-1">
                        <div class="small fw-bold">${item.name}</div>
                        <div class="text-muted small">${this.formatWeight(item.weightGrams)}</div>
                    </div>
                    <button class="btn btn-sm btn-link text-danger remove-item p-0"
                            data-id="${item.uid}" data-side="${side}">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>
            `;
        });
        container.innerHTML = html;
    }

    async performWeighing() {
        if (this.animating) return;

        try {
            this.animating = true;
            const leftIds = this.leftItems.map(i => i.id);
            const rightIds = this.rightItems.map(i => i.id);

            const result = await this.api.post(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.virtualWeighing.weigh}`, {
                leftItemIds: leftIds,
                rightItemIds: rightIds,
                balanceType: this.balanceType
            });

            if (result.success) {
                this.currentResult = result.data;
                this.animateBalance(result.data);
                this.renderResult(result.data);
            }
        } catch (e) {
            console.error('称量失败:', e);
            this.simulateWeighing();
        }
    }

    simulateWeighing() {
        const leftWeight = this.leftItems.reduce((s, i) => s + i.weightGrams, 0);
        const rightWeight = this.rightItems.reduce((s, i) => s + i.weightGrams, 0);
        const totalWeight = leftWeight + rightWeight;
        const diff = leftWeight - rightWeight;

        const maxTilt = 0.3;
        const targetAngle = totalWeight > 0 ? Math.max(-maxTilt, Math.min(maxTilt, diff / totalWeight)) : 0;

        const result = {
            leftWeight: leftWeight,
            rightWeight: rightWeight,
            balanceAngle: targetAngle,
            isBalanced: Math.abs(diff) < 0.1,
            tiltDirection: diff > 0.01 ? 'LEFT' : (diff < -0.01 ? 'RIGHT' : 'BALANCED'),
            equilibriumTime: 2.5,
            convertedWeights: {
                grams: { left: leftWeight, right: rightWeight, diff: diff },
                qinJin: { left: leftWeight / 253, right: rightWeight / 253, diff: diff / 253 },
                hanJin: { left: leftWeight / 250, right: rightWeight / 250, diff: diff / 250 },
                tangJin: { left: leftWeight / 661, right: rightWeight / 661, diff: diff / 661 },
                romanLibra: { left: leftWeight / 327, right: rightWeight / 327, diff: diff / 327 }
            },
            culturalContext: {
                story: '杠杆原理的发现是古代计量文明的重要里程碑',
                historicalFact: '《墨经》中记载："衡，加重于其一旁，必捶，权重相若也。"',
                funFact: '如果左右等重，天平会完美平衡，这是等臂天平的核心原理'
            },
            animation: {
                keyFrames: [
                    { time: 0, angle: 0 },
                    { time: 0.5, angle: targetAngle * 1.2 },
                    { time: 1.2, angle: targetAngle * 0.9 },
                    { time: 2.0, angle: targetAngle * 1.05 },
                    { time: 2.5, angle: targetAngle }
                ]
            }
        };

        this.currentResult = result;
        this.animateBalance(result);
        this.renderResult(result);
    }

    animateBalance(result) {
        if (!this.balance3d) {
            setTimeout(() => {
                this.animating = false;
            }, 2500);
            return;
        }

        try {
            const angle = result.balanceAngle;
            const config = AppConfig.balance3d.physics;

            const applyTilt = (targetAngle) => {
                if (this.balance3d.beamGroup) {
                    this.balance3d.beamGroup.rotation.z = targetAngle;
                }
            };

            const keyframes = result.animation?.keyFrames || [
                { time: 0, angle: 0 },
                { time: 0.5, angle: angle * 1.2 },
                { time: 1.2, angle: angle * 0.9 },
                { time: 2.0, angle: angle * 1.05 },
                { time: 2.5, angle: angle }
            ];

            let currentFrame = 0;
            const animateStep = () => {
                if (currentFrame >= keyframes.length) {
                    this.animating = false;
                    return;
                }

                const frame = keyframes[currentFrame];
                applyTilt(frame.angle);
                currentFrame++;

                if (currentFrame < keyframes.length) {
                    const delay = (keyframes[currentFrame].time - frame.time) * 1000;
                    setTimeout(animateStep, delay);
                } else {
                    this.animating = false;
                }
            };

            animateStep();
        } catch (e) {
            console.error('动画失败:', e);
            this.animating = false;
        }
    }

    renderResult(data) {
        this.renderBalanceStatus(data);
        this.renderConvertedWeights(data);
        this.renderCulturalContext(data);
        this.renderPhysicsInfo(data);
    }

    renderBalanceStatus(data) {
        const container = document.getElementById('balanceStatus');
        if (!container) return;

        const statusConfig = {
            BALANCED: { icon: 'check-circle-fill', color: 'success', text: '完美平衡！' },
            LEFT: { icon: 'arrow-down-left', color: 'primary', text: '左侧偏重 ⬇️' },
            RIGHT: { icon: 'arrow-down-right', color: 'warning', text: '右侧偏重 ⬇️' }
        };

        const status = statusConfig[data.tiltDirection] || statusConfig.BALANCED;

        container.innerHTML = `
            <div class="alert alert-${status.color} text-center mb-3">
                <h4 class="mb-0">
                    <i class="bi bi-${status.icon}"></i>
                    ${status.text}
                </h4>
                <div class="mt-2">
                    <span class="badge bg-${status.color}">倾角: ${(data.balanceAngle * 180 / Math.PI).toFixed(2)}°</span>
                    <span class="badge bg-secondary">平衡时间: ${data.equilibriumTime?.toFixed(1) || 2.5}s</span>
                </div>
            </div>
        `;
    }

    renderConvertedWeights(data) {
        const container = document.getElementById('convertedWeights');
        if (!container || !data.convertedWeights) return;

        const unitNames = {
            grams: { name: '克 (g)', symbol: 'g' },
            qinJin: { name: '秦斤', symbol: '秦斤' },
            hanJin: { name: '汉斤', symbol: '汉斤' },
            tangJin: { name: '唐大斤', symbol: '唐斤' },
            romanLibra: { name: '罗马磅', symbol: 'lb' },
            mingJin: { name: '明斤', symbol: '明斤' }
        };

        let html = '<div class="card"><div class="card-header"><h6 class="mb-0">多单位换算</h6></div><div class="card-body p-0">';
        html += '<div class="table-responsive"><table class="table table-sm mb-0"><thead><tr>';
        html += '<th>计量单位</th><th class="text-end">左侧</th><th class="text-end">右侧</th><th class="text-end">差值</th></tr></thead><tbody>';

        Object.entries(data.convertedWeights).forEach(([key, val]) => {
            const unit = unitNames[key];
            if (!unit) return;
            const diffClass = val.diff > 0.01 ? 'text-primary' : val.diff < -0.01 ? 'text-warning' : 'text-success';
            html += `<tr>
                <td>${unit.name}</td>
                <td class="text-end">${this.formatNumber(val.left)} ${unit.symbol}</td>
                <td class="text-end">${this.formatNumber(val.right)} ${unit.symbol}</td>
                <td class="text-end ${diffClass}">${val.diff > 0 ? '+' : ''}${this.formatNumber(val.diff)} ${unit.symbol}</td>
            </tr>`;
        });

        html += '</tbody></table></div></div></div>';
        container.innerHTML = html;
    }

    renderCulturalContext(data) {
        const container = document.getElementById('culturalContext');
        if (!container || !data.culturalContext) return;

        const ctx = data.culturalContext;
        container.innerHTML = `
            <div class="card bg-light">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-book-fill"></i> 文化解读</h6>
                </div>
                <div class="card-body">
                    ${ctx.story ? `<p class="mb-2">${ctx.story}</p>` : ''}
                    ${ctx.historicalFact ? `
                        <div class="alert alert-info mb-2">
                            <i class="bi bi-quote"></i>
                            <em>${ctx.historicalFact}</em>
                        </div>
                    ` : ''}
                    ${ctx.funFact ? `
                        <div class="text-muted small">
                            <i class="bi bi-lightbulb text-warning"></i>
                            ${ctx.funFact}
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    renderPhysicsInfo(data) {
        const container = document.getElementById('physicsInfo');
        if (!container) return;

        const leftMoment = data.leftWeight * 1;
        const rightMoment = data.rightWeight * 1;

        container.innerHTML = `
            <div class="card mt-3">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-cpu-fill"></i> 杠杆原理</h6>
                </div>
                <div class="card-body">
                    <div class="formula-box text-center mb-3">
                        <strong>F₁ × L₁ = F₂ × L₂</strong>
                        <div class="small text-muted">动力 × 动力臂 = 阻力 × 阻力臂</div>
                    </div>
                    <div class="row text-center">
                        <div class="col-4">
                            <div class="small text-muted">左力矩</div>
                            <div class="h5 mb-0">${(leftMoment).toFixed(1)} g·L</div>
                        </div>
                        <div class="col-4">
                            <div class="small text-muted">右力矩</div>
                            <div class="h5 mb-0">${(rightMoment).toFixed(1)} g·L</div>
                        </div>
                        <div class="col-4">
                            <div class="small text-muted">力矩差</div>
                            <div class="h5 mb-0 ${Math.abs(leftMoment - rightMoment) < 0.1 ? 'text-success' : 'text-warning'}">
                                ${(leftMoment - rightMoment).toFixed(1)} g·L
                            </div>
                        </div>
                    </div>
                    <div class="mt-3 small text-muted">
                        平衡条件：左右力矩相等，即 F₁·L₁ = F₂·L₂。
                        ${this.balanceType === 'EQUAL_ARM' ? '等臂天平 L₁=L₂，故 F₁=F₂ 时平衡。' :
                          '不等臂天平可通过调整力臂称量更重的物体。'}
                    </div>
                </div>
            </div>
        `;
    }

    updateBalanceTypeUI() {
        const equalBtn = document.getElementById('balanceTypeEqual');
        const unequalBtn = document.getElementById('balanceTypeUnequal');
        if (!equalBtn || !unequalBtn) return;

        if (this.balanceType === 'EQUAL_ARM') {
            equalBtn.classList.add('active', 'btn-primary');
            equalBtn.classList.remove('btn-outline-primary');
            unequalBtn.classList.remove('active', 'btn-primary');
            unequalBtn.classList.add('btn-outline-primary');
        } else {
            unequalBtn.classList.add('active', 'btn-primary');
            unequalBtn.classList.remove('btn-outline-primary');
            equalBtn.classList.remove('active', 'btn-primary');
            equalBtn.classList.add('btn-outline-primary');
        }
    }

    async quickExperience() {
        this.clearPan('left');
        this.clearPan('right');

        const weights = this.allItems.filter(i => i.category === 'weight' && i.weightGrams <= 100);
        const artifact = this.allItems.find(i => i.id === 13);

        if (artifact) this.addItem(artifact.id, 'left');

        let remaining = artifact ? artifact.weightGrams : 250;
        weights.sort((a, b) => b.weightGrams - a.weightGrams);

        for (const w of weights) {
            while (remaining >= w.weightGrams - 0.01) {
                this.addItem(w.id, 'right');
                remaining -= w.weightGrams;
            }
        }

        await this.performWeighing();
    }

    formatWeight(grams) {
        if (grams >= 1000000) return (grams / 1000000).toFixed(2) + ' 吨';
        if (grams >= 1000) return (grams / 1000).toFixed(2) + ' kg';
        if (grams >= 1) return grams.toFixed(2) + ' g';
        if (grams >= 0.001) return (grams * 1000).toFixed(2) + ' mg';
        return grams.toFixed(4) + ' g';
    }

    formatNumber(num) {
        if (Math.abs(num) >= 1000) return num.toFixed(1);
        if (Math.abs(num) >= 1) return num.toFixed(2);
        if (Math.abs(num) >= 0.01) return num.toFixed(3);
        return num.toFixed(6);
    }
}

if (typeof window !== 'undefined') {
    window.VirtualWeighingPanelController = VirtualWeighingPanelController;
}
