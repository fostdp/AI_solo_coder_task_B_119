class VirtualWeighingComponent {
    constructor(containerId, api, options = {}) {
        this.container = typeof containerId === 'string' 
            ? document.getElementById(containerId) 
            : containerId;
        
        if (!this.container) {
            throw new Error(`Container not found: ${containerId}`);
        }

        this.api = api || null;
        this.options = {
            templateUrl: options.templateUrl || '',
            balance3d: options.balance3d || null,
            apiBase: options.apiBase || 'http://localhost:8080/api',
            ...options
        };

        this.allItems = [];
        this.filteredItems = [];
        this.currentCategory = 'all';
        this.leftItems = [];
        this.rightItems = [];
        this.currentResult = null;
        this.balanceType = 'EQUAL_ARM';
        this.animating = false;
        this.animationFrameId = null;

        this.eventCallbacks = {
            onWeighComplete: options.onWeighComplete || null,
            onItemAdd: options.onItemAdd || null,
            onItemRemove: options.onItemRemove || null,
            onBalanceTypeChange: options.onBalanceTypeChange || null,
            onInit: options.onInit || null
        };

        this.elements = {};
    }

    async init() {
        await this.render();
        this.cacheElements();
        this.bindEvents();
        await this.loadItems();
        this.renderItemPalette();
        this.updatePanViews();
        this.updatePhysicsInfo();
        
        if (this.eventCallbacks.onInit) {
            this.eventCallbacks.onInit(this);
        }
    }

    async render() {
        if (this.options.templateUrl) {
            try {
                const response = await fetch(this.options.templateUrl);
                this.container.innerHTML = await response.text();
            } catch (e) {
                console.warn('加载模板失败，使用内置模板:', e);
                this.container.innerHTML = this.getDefaultTemplate();
            }
        } else {
            this.container.innerHTML = this.getDefaultTemplate();
        }
        
        this.injectStyles();
    }

    cacheElements() {
        const prefix = 'vw';
        const ids = [
            'ItemPalette', 'LeftPanDrop', 'RightPanDrop',
            'LeftPanItems', 'RightPanItems',
            'LeftWeightSummary', 'RightWeightSummary',
            'ClearLeftBtn', 'ClearRightBtn', 'SwapPansBtn',
            'WeighBtn', 'QuickExperienceBtn',
            'BalanceStatus', 'ConvertedWeights',
            'CulturalContext', 'PhysicsInfo',
            'BalanceBeam', 'BalanceAnimation',
            'LeftMoment', 'RightMoment', 'MomentDiff',
            'AnimLeftPan', 'AnimRightPan'
        ];

        ids.forEach(id => {
            this.elements[id] = document.getElementById(`${prefix}${id}`);
        });

        this.elements.balanceTypeButtons = this.container.querySelectorAll('[data-balance-type]');
        this.elements.categoryButtons = this.container.querySelectorAll('[data-category]');
    }

    bindEvents() {
        this.container.addEventListener('click', (e) => {
            const target = e.target.closest('[data-action]') || e.target;
            
            if (target.matches('[data-balance-type]')) {
                const type = target.dataset.balanceType;
                this.setBalanceType(type);
                return;
            }

            if (target.matches('[data-category]')) {
                const category = target.dataset.category;
                this.setCategory(category);
                return;
            }

            if (target.closest('.vw-add-to-left')) {
                const btn = target.closest('.vw-add-to-left');
                const id = parseInt(btn.dataset.id);
                this.addItem(id, 'left');
                return;
            }

            if (target.closest('.vw-add-to-right')) {
                const btn = target.closest('.vw-add-to-right');
                const id = parseInt(btn.dataset.id);
                this.addItem(id, 'right');
                return;
            }

            if (target.closest('.vw-remove-item')) {
                const btn = target.closest('.vw-remove-item');
                const uid = parseFloat(btn.dataset.uid);
                const side = btn.dataset.side;
                this.removeItem(uid, side);
                return;
            }

            if (target.id === 'vwClearLeftBtn' || target.closest('#vwClearLeftBtn')) {
                this.clearPan('left');
                return;
            }

            if (target.id === 'vwClearRightBtn' || target.closest('#vwClearRightBtn')) {
                this.clearPan('right');
                return;
            }

            if (target.id === 'vwSwapPansBtn' || target.closest('#vwSwapPansBtn')) {
                this.swapPans();
                return;
            }

            if (target.id === 'vwWeighBtn' || target.closest('#vwWeighBtn')) {
                this.performWeighing();
                return;
            }

            if (target.id === 'vwQuickExperienceBtn' || target.closest('#vwQuickExperienceBtn')) {
                this.quickExperience();
                return;
            }
        });

        this.container.addEventListener('dragstart', (e) => {
            const itemCard = e.target.closest('.vw-item-card');
            if (itemCard) {
                e.dataTransfer.setData('text/plain', itemCard.dataset.itemId);
                e.dataTransfer.effectAllowed = 'copy';
                itemCard.classList.add('vw-dragging');
            }
        });

        this.container.addEventListener('dragend', (e) => {
            const itemCard = e.target.closest('.vw-item-card');
            if (itemCard) {
                itemCard.classList.remove('vw-dragging');
            }
        });

        ['LeftPanDrop', 'RightPanDrop'].forEach(zoneKey => {
            const zone = this.elements[zoneKey];
            if (!zone) return;

            zone.addEventListener('dragover', (e) => {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
                zone.classList.add('vw-drag-over');
            });

            zone.addEventListener('dragleave', (e) => {
                if (!zone.contains(e.relatedTarget)) {
                    zone.classList.remove('vw-drag-over');
                }
            });

            zone.addEventListener('drop', (e) => {
                e.preventDefault();
                zone.classList.remove('vw-drag-over');
                const itemId = parseInt(e.dataTransfer.getData('text/plain'));
                const side = zoneKey === 'LeftPanDrop' ? 'left' : 'right';
                this.addItem(itemId, side);
            });
        });
    }

    async loadItems() {
        if (this.api && typeof this.api.get === 'function') {
            try {
                const result = await this.api.get(
                    `${this.options.apiBase}/virtual-weighing/items`
                );
                if (result.success) {
                    this.allItems = result.data;
                    this.filteredItems = [...this.allItems];
                    return;
                }
            } catch (e) {
                console.warn('从API加载物品失败，使用模拟数据:', e);
            }
        }
        
        this.loadMockItems();
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
        this.filteredItems = [...this.allItems];
    }

    setCategory(category) {
        this.currentCategory = category;
        
        this.elements.categoryButtons.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.category === category);
        });

        if (category === 'all') {
            this.filteredItems = [...this.allItems];
        } else {
            this.filteredItems = this.allItems.filter(i => i.category === category);
        }

        this.renderItemPalette();
    }

    renderItemPalette() {
        const container = this.elements.ItemPalette;
        if (!container) return;

        if (this.filteredItems.length === 0) {
            container.innerHTML = '<div class="vw-empty-hint"><span>暂无物品</span></div>';
            return;
        }

        let html = '<div class="vw-item-grid">';
        this.filteredItems.forEach(item => {
            html += `
                <div class="vw-item-card" draggable="true" data-item-id="${item.id}"
                     title="${item.description}">
                    <span class="vw-item-icon">${item.image}</span>
                    <span class="vw-item-name">${item.name}</span>
                    <span class="vw-item-weight">${this.formatWeight(item.weightGrams)}</span>
                    <div class="vw-item-actions">
                        <button class="vw-btn vw-btn-sm vw-btn-outline-primary vw-add-to-left" data-id="${item.id}" data-action="add-left">← 左</button>
                        <button class="vw-btn vw-btn-sm vw-btn-outline-primary vw-add-to-right" data-id="${item.id}" data-action="add-right">右 →</button>
                    </div>
                </div>
            `;
        });
        html += '</div>';

        container.innerHTML = html;
    }

    addItem(itemId, side) {
        const item = this.allItems.find(i => i.id === itemId);
        if (!item) return;

        const itemCopy = { ...item, uid: Date.now() + Math.random() };
        
        if (side === 'left') {
            this.leftItems.push(itemCopy);
        } else {
            this.rightItems.push(itemCopy);
        }

        this.updatePanViews();
        this.updatePhysicsInfo();

        if (this.eventCallbacks.onItemAdd) {
            this.eventCallbacks.onItemAdd(item, side);
        }
    }

    removeItem(uid, side) {
        let removedItem = null;
        
        if (side === 'left') {
            const index = this.leftItems.findIndex(i => i.uid === uid);
            if (index > -1) {
                removedItem = this.leftItems.splice(index, 1)[0];
            }
        } else {
            const index = this.rightItems.findIndex(i => i.uid === uid);
            if (index > -1) {
                removedItem = this.rightItems.splice(index, 1)[0];
            }
        }

        this.updatePanViews();
        this.updatePhysicsInfo();

        if (removedItem && this.eventCallbacks.onItemRemove) {
            this.eventCallbacks.onItemRemove(removedItem, side);
        }
    }

    clearPan(side) {
        if (side === 'left') {
            this.leftItems = [];
        } else {
            this.rightItems = [];
        }
        this.updatePanViews();
        this.updatePhysicsInfo();
    }

    swapPans() {
        const temp = this.leftItems;
        this.leftItems = this.rightItems;
        this.rightItems = temp;
        this.updatePanViews();
        this.updatePhysicsInfo();
    }

    setBalanceType(type) {
        this.balanceType = type;
        
        this.elements.balanceTypeButtons.forEach(btn => {
            if (btn.dataset.balanceType === type) {
                btn.classList.add('active');
                btn.classList.remove('vw-btn-outline-primary');
                btn.classList.add('vw-btn-primary');
            } else {
                btn.classList.remove('active');
                btn.classList.add('vw-btn-outline-primary');
                btn.classList.remove('vw-btn-primary');
            }
        });

        if (this.eventCallbacks.onBalanceTypeChange) {
            this.eventCallbacks.onBalanceTypeChange(type);
        }

        this.updatePhysicsInfo();
    }

    updatePanViews() {
        this.renderPan('left', this.leftItems);
        this.renderPan('right', this.rightItems);

        const leftWeight = this.leftItems.reduce((s, i) => s + i.weightGrams, 0);
        const rightWeight = this.rightItems.reduce((s, i) => s + i.weightGrams, 0);

        const leftSummary = this.elements.LeftWeightSummary;
        const rightSummary = this.elements.RightWeightSummary;

        if (leftSummary) {
            leftSummary.innerHTML = `
                <div class="vw-weight-value">${this.formatWeight(leftWeight)}</div>
                <div class="vw-weight-count">${this.leftItems.length} 件物品</div>
            `;
        }
        if (rightSummary) {
            rightSummary.innerHTML = `
                <div class="vw-weight-value">${this.formatWeight(rightWeight)}</div>
                <div class="vw-weight-count">${this.rightItems.length} 件物品</div>
            `;
        }
    }

    renderPan(side, items) {
        const containerKey = side === 'left' ? 'LeftPanItems' : 'RightPanItems';
        const container = this.elements[containerKey];
        if (!container) return;

        if (items.length === 0) {
            container.innerHTML = `
                <div class="vw-empty-hint">
                    <i class="bi bi-inbox"></i>
                    <span>拖拽物品或点击添加</span>
                </div>
            `;
            return;
        }

        let html = '';
        items.forEach(item => {
            html += `
                <div class="vw-pan-item">
                    <span class="vw-item-icon">${item.image}</span>
                    <div class="vw-pan-item-info">
                        <div class="vw-pan-item-name">${item.name}</div>
                        <div class="vw-pan-item-weight">${this.formatWeight(item.weightGrams)}</div>
                    </div>
                    <button class="vw-remove-item" data-uid="${item.uid}" data-side="${side}" data-action="remove">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>
            `;
        });
        container.innerHTML = html;
    }

    async performWeighing() {
        if (this.animating) return;

        this.animating = true;
        this.setWeighButtonState(true);

        try {
            let result;
            
            if (this.api && typeof this.api.post === 'function') {
                const leftIds = this.leftItems.map(i => i.id);
                const rightIds = this.rightItems.map(i => i.id);
                
                const apiResult = await this.api.post(
                    `${this.options.apiBase}/virtual-weighing/weigh`, {
                    leftItemIds: leftIds,
                    rightItemIds: rightIds,
                    balanceType: this.balanceType
                });

                if (apiResult.success) {
                    result = apiResult.data;
                } else {
                    throw new Error('API返回失败');
                }
            } else {
                result = this.simulateWeighing();
            }

            this.currentResult = result;
            await this.animateBalance(result);
            this.renderResult(result);

            if (this.eventCallbacks.onWeighComplete) {
                this.eventCallbacks.onWeighComplete(result);
            }
        } catch (e) {
            console.error('称量失败:', e);
            const result = this.simulateWeighing();
            this.currentResult = result;
            await this.animateBalance(result);
            this.renderResult(result);

            if (this.eventCallbacks.onWeighComplete) {
                this.eventCallbacks.onWeighComplete(result);
            }
        } finally {
            this.animating = false;
            this.setWeighButtonState(false);
        }
    }

    simulateWeighing() {
        const leftWeight = this.leftItems.reduce((s, i) => s + i.weightGrams, 0);
        const rightWeight = this.rightItems.reduce((s, i) => s + i.weightGrams, 0);
        const totalWeight = leftWeight + rightWeight;
        const diff = leftWeight - rightWeight;

        const maxTilt = 0.3;
        const targetAngle = totalWeight > 0 ? Math.max(-maxTilt, Math.min(maxTilt, diff / totalWeight)) : 0;

        const leftArmRatio = this.balanceType === 'UNEQUAL_ARM' ? 0.7 : 1;
        const rightArmRatio = this.balanceType === 'UNEQUAL_ARM' ? 1.3 : 1;
        
        const leftMoment = leftWeight * leftArmRatio;
        const rightMoment = rightWeight * rightArmRatio;
        const momentDiff = leftMoment - rightMoment;

        return {
            leftWeight: leftWeight,
            rightWeight: rightWeight,
            balanceAngle: targetAngle,
            isBalanced: Math.abs(momentDiff) < 0.1,
            tiltDirection: momentDiff > 0.01 ? 'LEFT' : (momentDiff < -0.01 ? 'RIGHT' : 'BALANCED'),
            equilibriumTime: 2.5,
            leftArmLength: leftArmRatio,
            rightArmLength: rightArmRatio,
            leftMoment: leftMoment,
            rightMoment: rightMoment,
            momentDiff: momentDiff,
            convertedWeights: {
                grams: { left: leftWeight, right: rightWeight, diff: diff },
                qinJin: { left: leftWeight / 253, right: rightWeight / 253, diff: diff / 253 },
                hanJin: { left: leftWeight / 250, right: rightWeight / 250, diff: diff / 250 },
                tangJin: { left: leftWeight / 661, right: rightWeight / 661, diff: diff / 661 },
                romanLibra: { left: leftWeight / 327, right: rightWeight / 327, diff: diff / 327 }
            },
            culturalContext: {
                story: this.getCulturalStory(),
                historicalFact: '《墨经》中记载："衡，加重于其一旁，必捶，权重相若也。"',
                funFact: this.getFunFact(leftWeight, rightWeight)
            },
            animation: {
                keyFrames: this.generateAnimationKeyframes(targetAngle)
            }
        };
    }

    generateAnimationKeyframes(targetAngle) {
        const keyframes = [];
        const duration = 2.5;
        const steps = 20;
        
        for (let i = 0; i <= steps; i++) {
            const t = i / steps;
            const time = t * duration;
            const decay = Math.exp(-3 * t);
            const oscillation = Math.sin(t * Math.PI * 3) * decay;
            const angle = targetAngle + (targetAngle * 0.3) * oscillation;
            keyframes.push({ time, angle });
        }
        
        return keyframes;
    }

    getCulturalStory() {
        const stories = [
            '杠杆原理的发现是古代计量文明的重要里程碑。早在战国时期，墨家学派就对杠杆平衡进行了系统研究。',
            '古代天平不仅是称量工具，更是公平与正义的象征。在许多文明中，天平都出现在神话和法律象征中。',
            '中国古代的"权衡"制度源远流长，从秦朝统一度量衡开始，天平在经济生活中扮演着重要角色。',
            '古埃及人用天平称量灵魂，认为只有灵魂比羽毛轻的人才能进入来世。',
            '古罗马的天平技术十分发达，他们已经能够制造精度很高的不等臂天平用于商业贸易。'
        ];
        return stories[Math.floor(Math.random() * stories.length)];
    }

    getFunFact(leftWeight, rightWeight) {
        if (Math.abs(leftWeight - rightWeight) < 0.1) {
            return '完美平衡！左右两边重量完全相等，天平会保持水平状态。';
        } else if (leftWeight > rightWeight) {
            return '左侧更重，天平会向左倾斜。这就是为什么曹冲称象时要把大象放在一边，另一边放石头。';
        } else {
            return '右侧更重，天平会向右倾斜。等臂天平的原理是两边力臂相等，所以重的一边会下沉。';
        }
    }

    async animateBalance(result) {
        return new Promise((resolve) => {
            const beam = this.elements.BalanceBeam;
            const leftPan = this.elements.AnimLeftPan;
            const rightPan = this.elements.AnimRightPan;
            
            if (!beam) {
                setTimeout(resolve, 2500);
                return;
            }

            const keyframes = result.animation?.keyFrames || this.generateAnimationKeyframes(result.balanceAngle);
            let currentFrame = 0;
            let startTime = null;

            const animate = (timestamp) => {
                if (!startTime) startTime = timestamp;
                const elapsed = (timestamp - startTime) / 1000;

                while (currentFrame < keyframes.length - 1 && 
                       keyframes[currentFrame + 1].time <= elapsed) {
                    currentFrame++;
                }

                if (currentFrame >= keyframes.length - 1) {
                    const finalAngle = keyframes[keyframes.length - 1].angle;
                    this.setBeamAngle(finalAngle);
                    resolve();
                    return;
                }

                const frame1 = keyframes[currentFrame];
                const frame2 = keyframes[currentFrame + 1];
                const t = (elapsed - frame1.time) / (frame2.time - frame1.time);
                const currentAngle = frame1.angle + (frame2.angle - frame1.angle) * t;

                this.setBeamAngle(currentAngle);
                this.animationFrameId = requestAnimationFrame(animate);
            };

            this.animationFrameId = requestAnimationFrame(animate);
        });
    }

    setBeamAngle(angle) {
        const beam = this.elements.BalanceBeam;
        if (!beam) return;

        const angleDeg = angle * (180 / Math.PI);
        beam.style.transform = `rotate(${angleDeg}deg)`;
    }

    setWeighButtonState(loading) {
        const btn = this.elements.WeighBtn;
        if (!btn) return;

        if (loading) {
            btn.disabled = true;
            btn.classList.add('vw-btn-loading');
            btn.innerHTML = '<i class="bi bi-hourglass-split"></i><span>称量中...</span>';
        } else {
            btn.disabled = false;
            btn.classList.remove('vw-btn-loading');
            btn.innerHTML = '<i class="bi bi-play-fill"></i><span>称量</span>';
        }
    }

    renderResult(data) {
        this.renderBalanceStatus(data);
        this.renderConvertedWeights(data);
        this.renderCulturalContext(data);
        this.updatePhysicsInfo(data);
    }

    renderBalanceStatus(data) {
        const container = this.elements.BalanceStatus;
        if (!container) return;

        const statusConfig = {
            BALANCED: { icon: 'check-circle-fill', color: 'success', text: '完美平衡！' },
            LEFT: { icon: 'arrow-down-left', color: 'primary', text: '左侧偏重 ⬇️' },
            RIGHT: { icon: 'arrow-down-right', color: 'warning', text: '右侧偏重 ⬇️' }
        };

        const status = statusConfig[data.tiltDirection] || statusConfig.BALANCED;
        const angleDeg = (data.balanceAngle * 180 / Math.PI).toFixed(2);
        const eqTime = data.equilibriumTime?.toFixed(1) || '2.5';

        container.innerHTML = `
            <div class="vw-alert vw-alert-${status.color}">
                <h4 class="vw-alert-title">
                    <i class="bi bi-${status.icon}"></i>
                    ${status.text}
                </h4>
                <div class="vw-alert-badges">
                    <span class="vw-badge vw-badge-${status.color}">倾角: ${angleDeg}°</span>
                    <span class="vw-badge vw-badge-secondary">平衡时间: ${eqTime}s</span>
                </div>
            </div>
        `;
    }

    renderConvertedWeights(data) {
        const container = this.elements.ConvertedWeights;
        if (!container || !data.convertedWeights) return;

        const unitNames = {
            grams: { name: '克 (g)', symbol: 'g' },
            qinJin: { name: '秦斤', symbol: '秦斤' },
            hanJin: { name: '汉斤', symbol: '汉斤' },
            tangJin: { name: '唐大斤', symbol: '唐斤' },
            romanLibra: { name: '罗马磅', symbol: 'lb' }
        };

        let html = '<div class="vw-conversion-table"><table><thead><tr>';
        html += '<th>计量单位</th><th class="text-end">左侧</th><th class="text-end">右侧</th><th class="text-end">差值</th></tr></thead><tbody>';

        Object.entries(data.convertedWeights).forEach(([key, val]) => {
            const unit = unitNames[key];
            if (!unit) return;
            const diffClass = val.diff > 0.01 ? 'vw-text-primary' : val.diff < -0.01 ? 'vw-text-warning' : 'vw-text-success';
            html += `<tr>
                <td>${unit.name}</td>
                <td class="text-end">${this.formatNumber(val.left)} ${unit.symbol}</td>
                <td class="text-end">${this.formatNumber(val.right)} ${unit.symbol}</td>
                <td class="text-end ${diffClass}">${val.diff > 0 ? '+' : ''}${this.formatNumber(val.diff)} ${unit.symbol}</td>
            </tr>`;
        });

        html += '</tbody></table></div>';
        container.innerHTML = html;
    }

    renderCulturalContext(data) {
        const container = this.elements.CulturalContext;
        if (!container || !data.culturalContext) return;

        const ctx = data.culturalContext;
        let html = '';
        
        if (ctx.story) {
            html += `<p class="vw-cultural-story">${ctx.story}</p>`;
        }
        if (ctx.historicalFact) {
            html += `
                <div class="vw-quote">
                    <i class="bi bi-quote"></i>
                    <em>${ctx.historicalFact}</em>
                </div>
            `;
        }
        if (ctx.funFact) {
            html += `
                <div class="vw-fun-fact">
                    <i class="bi bi-lightbulb text-warning"></i>
                    ${ctx.funFact}
                </div>
            `;
        }

        container.innerHTML = html;
    }

    updatePhysicsInfo(data) {
        const leftMomentEl = this.elements.LeftMoment;
        const rightMomentEl = this.elements.RightMoment;
        const momentDiffEl = this.elements.MomentDiff;

        let leftMoment, rightMoment, momentDiff;

        if (data) {
            leftMoment = data.leftMoment !== undefined ? data.leftMoment : data.leftWeight;
            rightMoment = data.rightMoment !== undefined ? data.rightMoment : data.rightWeight;
            momentDiff = data.momentDiff !== undefined ? data.momentDiff : (data.leftWeight - data.rightWeight);
        } else {
            const leftWeight = this.leftItems.reduce((s, i) => s + i.weightGrams, 0);
            const rightWeight = this.rightItems.reduce((s, i) => s + i.weightGrams, 0);
            const leftArmRatio = this.balanceType === 'UNEQUAL_ARM' ? 0.7 : 1;
            const rightArmRatio = this.balanceType === 'UNEQUAL_ARM' ? 1.3 : 1;
            leftMoment = leftWeight * leftArmRatio;
            rightMoment = rightWeight * rightArmRatio;
            momentDiff = leftMoment - rightMoment;
        }

        if (leftMomentEl) {
            leftMomentEl.textContent = `${leftMoment.toFixed(1)} g·L`;
        }
        if (rightMomentEl) {
            rightMomentEl.textContent = `${rightMoment.toFixed(1)} g·L`;
        }
        if (momentDiffEl) {
            momentDiffEl.textContent = `${momentDiff > 0 ? '+' : ''}${momentDiff.toFixed(1)} g·L`;
            momentDiffEl.className = 'vw-moment-value';
            if (Math.abs(momentDiff) < 0.1) {
                momentDiffEl.classList.add('vw-text-success');
            } else {
                momentDiffEl.classList.add('vw-text-warning');
            }
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

    on(event, callback) {
        const eventName = `on${event.charAt(0).toUpperCase() + event.slice(1)}`;
        if (this.eventCallbacks.hasOwnProperty(eventName)) {
            this.eventCallbacks[eventName] = callback;
        }
    }

    off(event) {
        const eventName = `on${event.charAt(0).toUpperCase() + event.slice(1)}`;
        if (this.eventCallbacks.hasOwnProperty(eventName)) {
            this.eventCallbacks[eventName] = null;
        }
    }

    getState() {
        return {
            leftItems: [...this.leftItems],
            rightItems: [...this.rightItems],
            balanceType: this.balanceType,
            currentResult: this.currentResult ? { ...this.currentResult } : null,
            allItems: [...this.allItems]
        };
    }

    destroy() {
        if (this.animationFrameId) {
            cancelAnimationFrame(this.animationFrameId);
        }
        this.container.innerHTML = '';
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

    getDefaultTemplate() {
        return `
<div class="virtual-weighing-component">
    <div class="vw-header">
        <h3 class="vw-title"><i class="bi bi-controller"></i> 虚拟称量体验</h3>
        <div class="vw-header-actions">
            <div class="vw-balance-type-group">
                <button class="vw-btn vw-btn-primary active" data-balance-type="EQUAL_ARM">
                    ⚖️ 等臂天平
                </button>
                <button class="vw-btn vw-btn-outline-primary" data-balance-type="UNEQUAL_ARM">
                    🔧 不等臂天平
                </button>
            </div>
            <button class="vw-btn vw-btn-outline-secondary" id="vwQuickExperienceBtn">
                <i class="bi bi-lightning"></i> 快速体验
            </button>
        </div>
    </div>

    <div class="vw-content">
        <div class="vw-sidebar">
            <div class="vw-card">
                <div class="vw-card-header">
                    <h6 class="vw-card-title">物品库</h6>
                    <div class="vw-category-filter">
                        <button class="vw-filter-btn active" data-category="all">全部</button>
                        <button class="vw-filter-btn" data-category="weight">砝码</button>
                        <button class="vw-filter-btn" data-category="artifact">文物</button>
                        <button class="vw-filter-btn" data-category="daily">日常</button>
                        <button class="vw-filter-btn" data-category="fun">趣味</button>
                    </div>
                </div>
                <div class="vw-card-body vw-item-palette" id="vwItemPalette"></div>
            </div>
        </div>

        <div class="vw-main">
            <div class="vw-pans-row">
                <div class="vw-pan-container">
                    <div class="vw-pan-drop-zone" id="vwLeftPanDrop">
                        <div class="vw-pan-header">
                            <h6>⬅️ 左盘</h6>
                            <div class="vw-weight-summary" id="vwLeftWeightSummary">
                                <div class="vw-weight-value">0.00 g</div>
                                <div class="vw-weight-count">0 件物品</div>
                            </div>
                        </div>
                        <div class="vw-pan-body" id="vwLeftPanItems">
                            <div class="vw-empty-hint">
                                <i class="bi bi-inbox"></i>
                                <span>拖拽物品或点击添加</span>
                            </div>
                        </div>
                        <div class="vw-pan-footer">
                            <button class="vw-btn vw-btn-sm vw-btn-outline-danger" id="vwClearLeftBtn">
                                <i class="bi bi-trash"></i> 清空
                            </button>
                        </div>
                    </div>
                </div>

                <div class="vw-actions-column">
                    <button class="vw-btn vw-btn-sm vw-btn-outline-secondary" id="vwSwapPansBtn">
                        <i class="bi bi-arrow-left-right"></i> 交换
                    </button>
                    <button class="vw-btn vw-btn-lg vw-btn-success" id="vwWeighBtn">
                        <i class="bi bi-play-fill"></i>
                        <span>称量</span>
                    </button>
                </div>

                <div class="vw-pan-container">
                    <div class="vw-pan-drop-zone" id="vwRightPanDrop">
                        <div class="vw-pan-header">
                            <h6>右盘 ➡️</h6>
                            <div class="vw-weight-summary" id="vwRightWeightSummary">
                                <div class="vw-weight-value">0.00 g</div>
                                <div class="vw-weight-count">0 件物品</div>
                            </div>
                        </div>
                        <div class="vw-pan-body" id="vwRightPanItems">
                            <div class="vw-empty-hint">
                                <i class="bi bi-inbox"></i>
                                <span>拖拽物品或点击添加</span>
                            </div>
                        </div>
                        <div class="vw-pan-footer">
                            <button class="vw-btn vw-btn-sm vw-btn-outline-danger" id="vwClearRightBtn">
                                <i class="bi bi-trash"></i> 清空
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="vw-balance-animation" id="vwBalanceAnimation">
                <div class="vw-balance-stand">
                    <div class="vw-balance-pillar"></div>
                    <div class="vw-balance-beam" id="vwBalanceBeam">
                        <div class="vw-beam-string vw-left-string"></div>
                        <div class="vw-beam-string vw-right-string"></div>
                        <div class="vw-pan vw-left-pan" id="vwAnimLeftPan"></div>
                        <div class="vw-pan vw-right-pan" id="vwAnimRightPan"></div>
                    </div>
                    <div class="vw-balance-base"></div>
                </div>
            </div>

            <div class="vw-result-section" id="vwBalanceStatus"></div>

            <div class="vw-info-row">
                <div class="vw-info-col">
                    <div class="vw-card">
                        <div class="vw-card-header">
                            <h6 class="vw-card-title"><i class="bi bi-arrow-left-right"></i> 多单位换算</h6>
                        </div>
                        <div class="vw-card-body vw-converted-weights" id="vwConvertedWeights">
                            <div class="vw-empty-hint small">
                                <span>称量后显示换算结果</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vw-info-col">
                    <div class="vw-card vw-card-light">
                        <div class="vw-card-header">
                            <h6 class="vw-card-title"><i class="bi bi-book-fill"></i> 文化解读</h6>
                        </div>
                        <div class="vw-card-body vw-cultural-context" id="vwCulturalContext">
                            <div class="vw-empty-hint small">
                                <span>称量后显示文化背景</span>
                            </div>
                        </div>
                    </div>
                    <div class="vw-card vw-mt-3">
                        <div class="vw-card-header">
                            <h6 class="vw-card-title"><i class="bi bi-cpu-fill"></i> 杠杆原理</h6>
                        </div>
                        <div class="vw-card-body vw-physics-info" id="vwPhysicsInfo">
                            <div class="vw-formula-box">
                                <strong>F₁ × L₁ = F₂ × L₂</strong>
                                <div class="vw-formula-desc">动力 × 动力臂 = 阻力 × 阻力臂</div>
                            </div>
                            <div class="vw-moment-row">
                                <div class="vw-moment-item">
                                    <div class="vw-moment-label">左力矩</div>
                                    <div class="vw-moment-value" id="vwLeftMoment">0.0 g·L</div>
                                </div>
                                <div class="vw-moment-item">
                                    <div class="vw-moment-label">右力矩</div>
                                    <div class="vw-moment-value" id="vwRightMoment">0.0 g·L</div>
                                </div>
                                <div class="vw-moment-item">
                                    <div class="vw-moment-label">力矩差</div>
                                    <div class="vw-moment-value" id="vwMomentDiff">0.0 g·L</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>`;
    }

    injectStyles() {
        const styleId = 'virtual-weighing-component-styles';
        if (document.getElementById(styleId)) return;

        const styles = `
.virtual-weighing-component {
    font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
    color: #333;
    background: #f8fafc;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.vw-header {
    padding: 16px 24px;
    background: linear-gradient(135deg, #1a2942 0%, #2c3e50 100%);
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
}

.vw-title {
    font-size: 20px;
    font-weight: 600;
    margin: 0;
}

.vw-header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
}

.vw-balance-type-group {
    display: flex;
    gap: 0;
}

.vw-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border: none;
    border-radius: 6px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
    text-decoration: none;
    white-space: nowrap;
}

.vw-btn-sm {
    padding: 4px 10px;
    font-size: 12px;
}

.vw-btn-lg {
    padding: 12px 24px;
    font-size: 16px;
    flex-direction: column;
    gap: 2px;
}

.vw-btn-primary {
    background: #1976d2;
    color: white;
}

.vw-btn-primary:hover {
    background: #1565c0;
}

.vw-btn-outline-primary {
    background: transparent;
    color: #1976d2;
    border: 1px solid #1976d2;
}

.vw-btn-outline-primary:hover,
.vw-btn-outline-primary.active {
    background: #1976d2;
    color: white;
}

.vw-btn-outline-secondary {
    background: transparent;
    color: rgba(255,255,255,0.9);
    border: 1px solid rgba(255,255,255,0.5);
}

.vw-btn-outline-secondary:hover {
    background: rgba(255,255,255,0.1);
    border-color: white;
}

.vw-btn-outline-danger {
    background: transparent;
    color: #d32f2f;
    border: 1px solid #d32f2f;
}

.vw-btn-outline-danger:hover {
    background: #d32f2f;
    color: white;
}

.vw-btn-success {
    background: #2e7d32;
    color: white;
    width: 100%;
    justify-content: center;
}

.vw-btn-success:hover {
    background: #1b5e20;
}

.vw-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.vw-btn-loading {
    pointer-events: none;
}

.vw-content {
    display: flex;
    gap: 16px;
    padding: 16px;
}

.vw-sidebar {
    width: 280px;
    flex-shrink: 0;
}

.vw-main {
    flex: 1;
    min-width: 0;
}

.vw-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    overflow: hidden;
}

.vw-card-light {
    background: #f5f7fa;
}

.vw-card-header {
    padding: 12px 16px;
    border-bottom: 1px solid #eee;
    background: #fafafa;
}

.vw-card-light .vw-card-header {
    background: #e8ecf1;
}

.vw-card-title {
    font-size: 14px;
    font-weight: 600;
    margin: 0;
    color: #333;
}

.vw-card-body {
    padding: 12px;
}

.vw-category-filter {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
    margin-top: 8px;
}

.vw-filter-btn {
    padding: 4px 10px;
    font-size: 11px;
    border: 1px solid #ddd;
    background: white;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;
}

.vw-filter-btn:hover {
    border-color: #1976d2;
    color: #1976d2;
}

.vw-filter-btn.active {
    background: #1976d2;
    color: white;
    border-color: #1976d2;
}

.vw-item-palette {
    max-height: 500px;
    overflow-y: auto;
}

.vw-item-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.vw-item-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px;
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    cursor: grab;
    transition: all 0.2s ease;
    min-width: 75px;
    position: relative;
}

.vw-item-card:hover {
    border-color: #1976d2;
    box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
    transform: translateY(-2px);
}

.vw-item-card.vw-dragging {
    opacity: 0.5;
    cursor: grabbing;
}

.vw-item-icon {
    font-size: 24px;
    margin-bottom: 4px;
}

.vw-item-name {
    font-size: 11px;
    color: #333;
    text-align: center;
    line-height: 1.2;
    margin-bottom: 2px;
}

.vw-item-weight {
    font-size: 10px;
    color: #666;
    background: #f0f0f0;
    padding: 1px 6px;
    border-radius: 8px;
}

.vw-item-actions {
    position: absolute;
    bottom: -8px;
    display: none;
    gap: 4px;
    z-index: 10;
}

.vw-item-card:hover .vw-item-actions {
    display: flex;
}

.vw-item-actions .vw-btn {
    padding: 2px 6px;
    font-size: 10px;
}

.vw-pans-row {
    display: flex;
    gap: 16px;
    align-items: stretch;
    margin-bottom: 16px;
}

.vw-pan-container {
    flex: 1;
    min-width: 0;
}

.vw-pan-drop-zone {
    background: white;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: all 0.2s;
}

.vw-pan-drop-zone.vw-drag-over {
    border-color: #1976d2;
    background: #e3f2fd;
}

.vw-pan-drop-zone.vw-drag-over .vw-pan-header {
    background: #1976d2;
    color: white;
}

.vw-pan-header {
    padding: 12px 16px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fafafa;
    border-radius: 6px 6px 0 0;
}

.vw-pan-header h6 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
}

.vw-weight-summary {
    text-align: right;
}

.vw-weight-value {
    font-size: 16px;
    font-weight: 600;
    color: #1976d2;
}

.vw-weight-count {
    font-size: 11px;
    color: #888;
}

.vw-pan-body {
    flex: 1;
    padding: 12px;
    overflow-y: auto;
    min-height: 150px;
    max-height: 200px;
}

.vw-pan-footer {
    padding: 8px 12px;
    border-top: 1px solid #eee;
    background: #fafafa;
}

.vw-pan-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px;
    background: #f8fafc;
    border-radius: 6px;
    margin-bottom: 6px;
    border: 1px solid #eee;
}

.vw-pan-item .vw-item-icon {
    font-size: 20px;
    margin-bottom: 0;
}

.vw-pan-item-info {
    flex: 1;
    min-width: 0;
}

.vw-pan-item-name {
    font-size: 12px;
    font-weight: 500;
    color: #333;
}

.vw-pan-item-weight {
    font-size: 11px;
    color: #888;
}

.vw-remove-item {
    background: none;
    border: none;
    color: #d32f2f;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.7;
    transition: all 0.2s;
}

.vw-remove-item:hover {
    opacity: 1;
    background: #ffebee;
}

.vw-actions-column {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 12px;
    width: 80px;
    flex-shrink: 0;
}

.vw-empty-hint {
    text-align: center;
    color: #999;
    padding: 30px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    font-size: 13px;
}

.vw-empty-hint i {
    font-size: 32px;
    opacity: 0.5;
}

.vw-empty-hint.small {
    padding: 20px 0;
    font-size: 12px;
}

.vw-empty-hint.small i {
    font-size: 24px;
}

.vw-balance-animation {
    height: 120px;
    background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    overflow: hidden;
}

.vw-balance-stand {
    position: relative;
    width: 200px;
    height: 100px;
}

.vw-balance-base {
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 80px;
    height: 12px;
    background: linear-gradient(180deg, #8b7355 0%, #6b5344 100%);
    border-radius: 4px;
}

.vw-balance-pillar {
    position: absolute;
    bottom: 12px;
    left: 50%;
    transform: translateX(-50%);
    width: 8px;
    height: 60px;
    background: linear-gradient(90deg, #8b7355 0%, #a08060 50%, #8b7355 100%);
    border-radius: 2px;
}

.vw-balance-beam {
    position: absolute;
    top: 15px;
    left: 50%;
    transform-origin: center center;
    transform: translateX(-50%) rotate(0deg);
    width: 180px;
    height: 6px;
    background: linear-gradient(90deg, #ffd700 0%, #ffb700 50%, #ffd700 100%);
    border-radius: 3px;
    transition: none;
}

.vw-beam-string {
    position: absolute;
    top: 6px;
    width: 2px;
    height: 25px;
    background: #666;
}

.vw-left-string {
    left: 10px;
}

.vw-right-string {
    right: 10px;
}

.vw-pan {
    position: absolute;
    top: 31px;
    width: 50px;
    height: 20px;
    background: linear-gradient(180deg, #cd853f 0%, #8b7355 100%);
    border-radius: 0 0 25px 25px;
    border: 2px solid #6b5344;
    border-top: none;
}

.vw-left-pan {
    left: -15px;
}

.vw-right-pan {
    right: -15px;
}

.vw-result-section {
    margin-bottom: 16px;
}

.vw-alert {
    padding: 16px;
    border-radius: 8px;
    text-align: center;
}

.vw-alert-success {
    background: #e8f5e9;
    color: #2e7d32;
}

.vw-alert-primary {
    background: #e3f2fd;
    color: #1565c0;
}

.vw-alert-warning {
    background: #fff3e0;
    color: #e65100;
}

.vw-alert-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 8px 0;
}

.vw-alert-badges {
    display: flex;
    gap: 8px;
    justify-content: center;
}

.vw-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
}

.vw-badge-success {
    background: #2e7d32;
    color: white;
}

.vw-badge-primary {
    background: #1976d2;
    color: white;
}

.vw-badge-warning {
    background: #f57c00;
    color: white;
}

.vw-badge-secondary {
    background: #78909c;
    color: white;
}

.vw-info-row {
    display: flex;
    gap: 16px;
}

.vw-info-col {
    flex: 1;
    min-width: 0;
}

.vw-mt-3 {
    margin-top: 12px;
}

.vw-conversion-table table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
}

.vw-conversion-table th,
.vw-conversion-table td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid #eee;
}

.vw-conversion-table th {
    background: #fafafa;
    font-weight: 600;
    font-size: 12px;
    color: #666;
}

.vw-conversion-table .text-end {
    text-align: right;
}

.vw-text-primary {
    color: #1976d2;
}

.vw-text-warning {
    color: #f57c00;
}

.vw-text-success {
    color: #2e7d32;
}

.vw-cultural-story {
    font-size: 13px;
    line-height: 1.6;
    margin: 0 0 12px 0;
    color: #333;
}

.vw-quote {
    padding: 12px;
    background: #e3f2fd;
    border-radius: 6px;
    font-size: 13px;
    color: #1565c0;
    margin-bottom: 12px;
}

.vw-quote i {
    margin-right: 6px;
    opacity: 0.7;
}

.vw-fun-fact {
    font-size: 12px;
    color: #666;
    display: flex;
    align-items: flex-start;
    gap: 6px;
}

.vw-fun-fact i {
    margin-top: 2px;
}

.vw-formula-box {
    padding: 16px;
    background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
    border-radius: 8px;
    text-align: center;
    font-size: 18px;
    font-family: 'Times New Roman', serif;
    margin-bottom: 12px;
}

.vw-formula-desc {
    font-size: 12px;
    font-family: inherit;
    color: #666;
    margin-top: 4px;
}

.vw-moment-row {
    display: flex;
    gap: 8px;
}

.vw-moment-item {
    flex: 1;
    text-align: center;
    padding: 8px;
    background: #f8fafc;
    border-radius: 6px;
}

.vw-moment-label {
    font-size: 11px;
    color: #888;
    margin-bottom: 4px;
}

.vw-moment-value {
    font-size: 14px;
    font-weight: 600;
    color: #333;
}

@media (max-width: 900px) {
    .vw-content {
        flex-direction: column;
    }
    
    .vw-sidebar {
        width: 100%;
    }
    
    .vw-info-row {
        flex-direction: column;
    }
    
    .vw-pans-row {
        flex-wrap: wrap;
    }
    
    .vw-pan-container {
        min-width: calc(50% - 48px);
    }
    
    .vw-actions-column {
        order: 3;
        width: 100%;
        flex-direction: row;
    }
}

@media (max-width: 600px) {
    .vw-header {
        flex-direction: column;
        align-items: flex-start;
    }
    
    .vw-pans-row {
        flex-direction: column;
    }
    
    .vw-pan-container {
        width: 100%;
    }
    
    .vw-actions-column {
        width: 100%;
        flex-direction: row;
        justify-content: center;
    }
    
    .vw-item-card {
        min-width: 60px;
    }
    
    .vw-item-icon {
        font-size: 20px;
    }
}
`;

        const styleElement = document.createElement('style');
        styleElement.id = styleId;
        styleElement.textContent = styles;
        document.head.appendChild(styleElement);
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = VirtualWeighingComponent;
}

if (typeof window !== 'undefined') {
    window.VirtualWeighingComponent = VirtualWeighingComponent;
}
