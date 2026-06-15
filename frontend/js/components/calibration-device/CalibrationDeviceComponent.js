class CalibrationDeviceComponent {
    constructor(containerId, api, options = {}) {
        this.container = document.getElementById(containerId);
        this.api = api;
        this.options = {
            apiBase: 'http://localhost:8080/api',
            templatePath: 'js/components/calibration-device/calibration-device-template.html',
            ...options
        };

        this.devices = [];
        this.balances = [];
        this.currentResult = null;
        this.vibrationMetadata = null;
        this.initialized = false;

        this.callbacks = {
            onCalibrationStart: null,
            onCalibrationComplete: null,
            onCalibrationError: null,
            onDeviceChange: null,
            onBalanceChange: null
        };

        this._elements = {};
    }

    on(event, callback) {
        if (this.callbacks.hasOwnProperty(event)) {
            this.callbacks[event] = callback;
        }
    }

    async init() {
        if (this.initialized) return;

        await this._loadTemplate();
        this._cacheElements();
        this.bindEvents();
        await this._loadInitialData();

        this.initialized = true;
    }

    async _loadTemplate() {
        try {
            const response = await fetch(this.options.templatePath);
            const html = await response.text();
            this.container.innerHTML = html;
        } catch (e) {
            console.warn('加载模板失败，使用内置模板:', e);
            this.container.innerHTML = this._getFallbackTemplate();
        }
    }

    _getFallbackTemplate() {
        return `
        <div class="calibration-device-component">
            <div class="card mb-4">
                <div class="card-header">
                    <h5 class="mb-0"><i class="bi bi-target"></i> 校准装置控制面板</h5>
                </div>
                <div class="card-body">
                    <div class="row g-3">
                        <div class="col-md-3">
                            <label class="form-label">校准装置</label>
                            <select class="form-select" data-role="device-select">
                                <option value="">请选择校准装置...</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">待校天平</label>
                            <select class="form-select" data-role="balance-select">
                                <option value="">请选择待校天平...</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">校准方法</label>
                            <select class="form-select" data-role="method-select">
                                <option value="SEVEN_POINT">七点线性校准</option>
                                <option value="THREE_POINT">三点校准</option>
                                <option value="SUBSTITUTION">替代法</option>
                            </select>
                        </div>
                        <div class="col-md-3 d-flex align-items-end">
                            <button class="btn btn-primary w-100" data-role="calibrate-btn">
                                <i class="bi bi-play-circle"></i> 开始校准
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            <div data-role="result-banner"></div>
            <div class="row mt-4" data-role="result-container" style="display: none;">
                <div class="col-md-8">
                    <div class="card mb-4">
                        <div class="card-header">
                            <h6 class="mb-0"><i class="bi bi-graph-up-arrow"></i> 七点校准数据</h6>
                        </div>
                        <div class="card-body">
                            <div class="table-responsive">
                                <table class="table table-sm table-hover" data-role="calibration-points-table">
                                    <thead>
                                        <tr>
                                            <th>序号</th>
                                            <th>标称质量 (g)</th>
                                            <th>左盘示值 (g)</th>
                                            <th>右盘示值 (g)</th>
                                            <th>平均示值 (g)</th>
                                            <th>误差 (%)</th>
                                            <th>状态</th>
                                        </tr>
                                    </thead>
                                    <tbody></tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                    <div class="card mb-4">
                        <div class="card-header">
                            <h6 class="mb-0"><i class="bi bi-speedometer2"></i> 误差指标</h6>
                        </div>
                        <div class="card-body">
                            <div class="row g-2" data-role="error-metrics"></div>
                        </div>
                    </div>
                    <div class="card">
                        <div class="card-header">
                            <h6 class="mb-0"><i class="bi bi-calculator-fill"></i> 不确定度预算表 (JJF 1059.1-2012)</h6>
                        </div>
                        <div class="card-body">
                            <div class="table-responsive">
                                <table class="table table-sm" data-role="uncertainty-table">
                                    <thead>
                                        <tr>
                                            <th>不确定度来源</th>
                                            <th>类型</th>
                                            <th>数值</th>
                                            <th>自由度</th>
                                            <th>贡献占比</th>
                                        </tr>
                                    </thead>
                                    <tbody></tbody>
                                </table>
                            </div>
                            <div class="row mt-3 text-center">
                                <div class="col-4">
                                    <div class="small text-muted">标准不确定度 u_c</div>
                                    <div class="h5 mb-0" data-role="std-uncertainty">-</div>
                                </div>
                                <div class="col-4">
                                    <div class="small text-muted">包含因子 k</div>
                                    <div class="h5 mb-0" data-role="k-factor">-</div>
                                </div>
                                <div class="col-4">
                                    <div class="small text-muted">扩展不确定度 U</div>
                                    <div class="h5 mb-0 text-primary" data-role="expanded-uncertainty">-</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card mb-4">
                        <div class="card-header">
                            <h6 class="mb-0"><i class="bi bi-award"></i> 校准等级判定</h6>
                        </div>
                        <div class="card-body text-center">
                            <div class="display-3 mb-2" data-role="grade-display">-</div>
                            <div class="badge" data-role="grade-badge">-</div>
                            <p class="mt-3 mb-0 small text-muted" data-role="grade-description">-</p>
                        </div>
                    </div>
                    <div class="card mb-4">
                        <div class="card-header">
                            <h6 class="mb-0"><i class="bi bi-activity"></i> 振动分析</h6>
                        </div>
                        <div class="card-body">
                            <div class="mb-3">
                                <div class="d-flex justify-content-between mb-1">
                                    <span class="small text-muted">环境振动等级</span>
                                    <span class="small fw-bold" data-role="vib-level">-</span>
                                </div>
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar bg-info" data-role="vib-level-bar" style="width: 0%"></div>
                                </div>
                            </div>
                            <div class="mb-3">
                                <div class="d-flex justify-content-between mb-1">
                                    <span class="small text-muted">减振系统</span>
                                    <span class="small fw-bold" data-role="isolation-system">-</span>
                                </div>
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar bg-success" data-role="isolation-bar" style="width: 0%"></div>
                                </div>
                            </div>
                            <div class="row text-center mt-3">
                                <div class="col-6">
                                    <div class="small text-muted">输入振动 RMS</div>
                                    <div class="h6 mb-0" data-role="input-vib">-</div>
                                </div>
                                <div class="col-6">
                                    <div class="small text-muted">残余振动 RMS</div>
                                    <div class="h6 mb-0" data-role="residual-vib">-</div>
                                </div>
                            </div>
                            <hr>
                            <div class="text-center">
                                <div class="small text-muted">振动引入误差</div>
                                <div class="h6 mb-0 text-warning" data-role="vib-error">-</div>
                            </div>
                        </div>
                    </div>
                    <div class="card">
                        <div class="card-header">
                            <h6 class="mb-0"><i class="bi bi-sliders"></i> 振动模拟</h6>
                        </div>
                        <div class="card-body">
                            <div class="mb-3">
                                <label class="form-label small">环境振动等级</label>
                                <select class="form-select form-select-sm" data-role="sim-vib-level"></select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label small">减振系统</label>
                                <select class="form-select form-select-sm" data-role="sim-isolation"></select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label small">刀口半径 (mm)</label>
                                <input type="number" class="form-control form-control-sm" data-role="sim-knife-radius" value="0.5" step="0.01" min="0.01">
                            </div>
                            <button class="btn btn-outline-primary btn-sm w-100" data-role="simulate-btn">
                                <i class="bi bi-play"></i> 模拟振动影响
                            </button>
                            <div class="mt-3" data-role="sim-result" style="display: none;">
                                <hr>
                                <div class="text-center">
                                    <div class="small text-muted">可达到等级</div>
                                    <div class="h4 mb-1" data-role="sim-grade">-</div>
                                    <div class="small text-muted">扩展不确定度估计</div>
                                    <div class="h6 mb-0 text-primary" data-role="sim-uncertainty">-</div>
                                </div>
                                <p class="mt-2 mb-0 small" data-role="sim-assessment">-</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card mt-4" data-role="history-container" style="display: none;">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h6 class="mb-0"><i class="bi bi-clock-history"></i> 校准历史记录</h6>
                    <button class="btn btn-outline-secondary btn-sm" data-role="load-history-btn">
                        <i class="bi bi-arrow-clockwise"></i> 刷新
                    </button>
                </div>
                <div class="card-body">
                    <div class="list-group" data-role="history-list"></div>
                </div>
            </div>
        </div>
        `;
    }

    _cacheElements() {
        const roles = [
            'device-select', 'balance-select', 'method-select', 'calibrate-btn',
            'result-banner', 'result-container', 'calibration-points-table',
            'error-metrics', 'uncertainty-table', 'std-uncertainty',
            'k-factor', 'expanded-uncertainty', 'grade-display', 'grade-badge',
            'grade-description', 'vib-level', 'vib-level-bar', 'isolation-system',
            'isolation-bar', 'input-vib', 'residual-vib', 'vib-error',
            'sim-vib-level', 'sim-isolation', 'sim-knife-radius', 'simulate-btn',
            'sim-result', 'sim-grade', 'sim-uncertainty', 'sim-assessment',
            'history-container', 'load-history-btn', 'history-list'
        ];

        roles.forEach(role => {
            const el = this.container.querySelector(`[data-role="${role}"]`);
            this._elements[role] = el;
        });
    }

    bindEvents() {
        if (this._elements['calibrate-btn']) {
            this._elements['calibrate-btn'].addEventListener('click', () => this._handleCalibrate());
        }

        if (this._elements['device-select']) {
            this._elements['device-select'].addEventListener('change', (e) => {
                if (this.callbacks.onDeviceChange) {
                    this.callbacks.onDeviceChange(e.target.value);
                }
            });
        }

        if (this._elements['balance-select']) {
            this._elements['balance-select'].addEventListener('change', (e) => {
                if (this.callbacks.onBalanceChange) {
                    this.callbacks.onBalanceChange(e.target.value);
                }
            });
        }

        if (this._elements['simulate-btn']) {
            this._elements['simulate-btn'].addEventListener('click', () => this._handleSimulate());
        }

        if (this._elements['load-history-btn']) {
            this._elements['load-history-btn'].addEventListener('click', () => this.loadHistory());
        }
    }

    async _loadInitialData() {
        await Promise.all([
            this.loadDevices(),
            this.loadBalances(),
            this.loadVibrationMetadata()
        ]);
    }

    async loadDevices() {
        try {
            const result = await this.api.get(
                `${this.options.apiBase}/calibration-device/devices`
            );
            if (result.success) {
                this.devices = result.data;
                this._renderDeviceSelect();
            }
        } catch (e) {
            console.warn('加载校准装置失败，使用模拟数据:', e);
            this._loadMockDevices();
        }
    }

    _loadMockDevices() {
        this.devices = [
            { id: 1, deviceCode: 'DEV001', deviceName: '杠杆原理校准装置', deviceType: 'PRECISION_CALIBRATOR', leftArmLength: 500, rightArmLength: 500.02, knifeEdgeRadius: 0.1, maxCapacity: 5000, minReadability: 0.001, material: '青铜+玛瑙', description: '基于古代杠杆原理设计的高精度校准装置' },
            { id: 2, deviceCode: 'DEV002', deviceName: '罗马式校准装置', deviceType: 'STANDARD_CALIBRATOR', leftArmLength: 400, rightArmLength: 400.05, knifeEdgeRadius: 0.15, maxCapacity: 3000, minReadability: 0.01, material: '铸铁+钢', description: '不等臂设计，罗马式结构' },
            { id: 3, deviceCode: 'DEV003', deviceName: '精密玛瑙刀口装置', deviceType: 'PRECISION_CALIBRATOR', leftArmLength: 600, rightArmLength: 600.01, knifeEdgeRadius: 0.05, maxCapacity: 2000, minReadability: 0.0001, material: '铝合金+玛瑙', description: '高精度玛瑙刀口，一等标准级' }
        ];
        this._renderDeviceSelect();
    }

    _renderDeviceSelect() {
        const select = this._elements['device-select'];
        if (!select) return;

        select.innerHTML = '<option value="">请选择校准装置...</option>';
        this.devices.forEach(d => {
            select.innerHTML += `<option value="${d.id}">${d.deviceName || d.name}</option>`;
        });
    }

    async loadBalances() {
        try {
            const result = await this.api.get(`${this.options.apiBase}/balances`);
            if (result.success) {
                this.balances = result.data;
                this._renderBalanceSelect();
            }
        } catch (e) {
            console.warn('加载天平列表失败，使用模拟数据:', e);
            this._loadMockBalances();
        }
    }

    _loadMockBalances() {
        this.balances = [
            { id: 1, balanceCode: 'BAL001', balanceName: '战国青铜天平', dynasty: '战国' },
            { id: 2, balanceCode: 'BAL002', balanceName: '唐代玛瑙天平', dynasty: '唐' },
            { id: 3, balanceCode: 'BAL003', balanceName: '宋代精密天平', dynasty: '宋' }
        ];
        this._renderBalanceSelect();
    }

    _renderBalanceSelect() {
        const select = this._elements['balance-select'];
        if (!select) return;

        select.innerHTML = '<option value="">请选择待校天平...</option>';
        this.balances.forEach(b => {
            const name = b.balanceName || b.name;
            const dynasty = b.dynasty ? ` (${b.dynasty})` : '';
            select.innerHTML += `<option value="${b.id}">${name}${dynasty}</option>`;
        });
    }

    async loadVibrationMetadata() {
        try {
            const result = await this.api.get(
                `${this.options.apiBase}/calibration-device/vibration/metadata`
            );
            if (result.success) {
                this.vibrationMetadata = result.data;
                this._renderVibrationSelects();
            }
        } catch (e) {
            console.warn('加载振动元数据失败，使用模拟数据:', e);
            this._loadMockVibrationMetadata();
        }
    }

    _loadMockVibrationMetadata() {
        this.vibrationMetadata = {
            vibrationLevels: {
                'VC_A': { code: 'VC-A', label: '极安静实验室', rmsDisplacement_um: 0.4 },
                'VC_B': { code: 'VC-B', label: '安静实验室', rmsDisplacement_um: 1.0 },
                'VC_C': { code: 'VC-C', label: '标准实验室', rmsDisplacement_um: 2.5 },
                'VC_D': { code: 'VC-D', label: '一般工作区', rmsDisplacement_um: 6.0 },
                'VC_E': { code: 'VC-E', label: '工业环境', rmsDisplacement_um: 15.0 },
                'WORKSHOP': { code: 'WORKSHOP', label: '普通车间', rmsDisplacement_um: 30.0 }
            },
            isolationSystems: {
                'NONE': { code: 'NONE', name: '无减振', isolationEfficiency_percent: 0 },
                'PASSIVE_RUBBER': { code: 'PASSIVE_RUBBER', name: '橡胶垫被动减振', isolationEfficiency_percent: 60 },
                'PASSIVE_AIR': { code: 'PASSIVE_AIR', name: '空气弹簧被动减振', isolationEfficiency_percent: 85 },
                'ACTIVE_PIEZO': { code: 'ACTIVE_PIEZO', name: '压电陶瓷主动减振', isolationEfficiency_percent: 95 },
                'ACTIVE_MAGNETIC': { code: 'ACTIVE_MAGNETIC', name: '磁悬浮主动减振', isolationEfficiency_percent: 99 }
            }
        };
        this._renderVibrationSelects();
    }

    _renderVibrationSelects() {
        const vibSelect = this._elements['sim-vib-level'];
        const isoSelect = this._elements['sim-isolation'];

        if (vibSelect && this.vibrationMetadata?.vibrationLevels) {
            vibSelect.innerHTML = '';
            Object.entries(this.vibrationMetadata.vibrationLevels).forEach(([key, val]) => {
                vibSelect.innerHTML += `<option value="${key}">${val.label} (${val.code})</option>`;
            });
        }

        if (isoSelect && this.vibrationMetadata?.isolationSystems) {
            isoSelect.innerHTML = '';
            Object.entries(this.vibrationMetadata.isolationSystems).forEach(([key, val]) => {
                isoSelect.innerHTML += `<option value="${key}">${val.name}</option>`;
            });
        }
    }

    async _handleCalibrate() {
        const deviceId = this._elements['device-select']?.value;
        const balanceId = this._elements['balance-select']?.value;
        const method = this._elements['method-select']?.value;

        if (!deviceId) {
            alert('请选择校准装置');
            return;
        }

        if (this.callbacks.onCalibrationStart) {
            this.callbacks.onCalibrationStart();
        }

        this._setCalibrating(true);

        try {
            const result = await this.api.post(
                `${this.options.apiBase}/calibration-device/calibrate`,
                {
                    deviceId: parseInt(deviceId),
                    balanceId: balanceId ? parseInt(balanceId) : null,
                    method: method
                }
            );

            if (result.success) {
                this.currentResult = result.data;
                this.renderResult(result.data);
                this._elements['result-container'].style.display = 'flex';
                this._elements['history-container'].style.display = 'block';

                if (this.callbacks.onCalibrationComplete) {
                    this.callbacks.onCalibrationComplete(result.data);
                }
            } else {
                throw new Error(result.message || '校准失败');
            }
        } catch (e) {
            console.error('校准失败:', e);
            this._loadMockResult();

            if (this.callbacks.onCalibrationError) {
                this.callbacks.onCalibrationError(e);
            }
        } finally {
            this._setCalibrating(false);
        }
    }

    _setCalibrating(isCalibrating) {
        const btn = this._elements['calibrate-btn'];
        if (!btn) return;

        if (isCalibrating) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>校准中...';
        } else {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-play-circle"></i> 开始校准';
        }
    }

    _loadMockResult() {
        const mockData = {
            id: Date.now(),
            deviceId: 1,
            balanceId: 1,
            calibrationTime: new Date().toISOString(),
            calibrationMethod: 'SEVEN_POINT',
            calibrationGrade: 'F1',
            correctedUncertainty: 0.0016,
            linearityError: 0.06,
            repeatabilityStd: 0.0003,
            hysteresisError: 0.0002,
            zeroPointDrift: 0.00015,
            leftArmCorrection: 0.0008,
            rightArmCorrection: 0.0009,
            armLengthRatioCorrection: 0.00004,
            positionsData: {
                pos_0: { nominal: 0, leftReading: 0.0002, rightReading: 0.0001, leftError: 0.0002, rightError: 0.0001, correction: 0 },
                pos_1: { nominal: 10, leftReading: 10.002, rightReading: 10.001, leftError: 0.002, rightError: 0.001, correction: -0.0015 },
                pos_2: { nominal: 20, leftReading: 20.005, rightReading: 20.003, leftError: 0.005, rightError: 0.003, correction: -0.003 },
                pos_3: { nominal: 50, leftReading: 50.012, rightReading: 50.008, leftError: 0.012, rightError: 0.008, correction: -0.008 },
                pos_4: { nominal: 100, leftReading: 100.025, rightReading: 100.018, leftError: 0.025, rightError: 0.018, correction: -0.015 },
                pos_5: { nominal: 200, leftReading: 200.052, rightReading: 200.038, leftError: 0.052, rightError: 0.038, correction: -0.03 },
                pos_6: { nominal: 500, leftReading: 500.125, rightReading: 500.095, leftError: 0.125, rightError: 0.095, correction: -0.075 }
            },
            rawMeasurements: {
                vibrationAnalysis: {
                    environmentLevel: 'VC_C',
                    environmentLabel: '标准实验室',
                    isolationSystem: 'PASSIVE_AIR',
                    isolationLabel: '空气弹簧被动减振',
                    isolationEfficiency_dB: 6.0,
                    inputVibrationX_um: 2.0,
                    inputVibrationY_um: 5.0,
                    inputVibrationZ_um: 1.0,
                    residualVibrationRMS_um: 0.85,
                    vibrationInducedError_mm: 0.0017,
                    vibrationNoiseScale: 0.000085,
                    vibrationUncertaintyComponent: 0.00098
                }
            },
            correctionTable: {
                '0': { nominal: 0, correctionValue: 0, uncertainty: 0, kFactor: 2.0 },
                '10': { nominal: 10, correctionValue: -0.0015, uncertainty: 0.016, kFactor: 2.0 },
                '20': { nominal: 20, correctionValue: -0.003, uncertainty: 0.032, kFactor: 2.0 },
                '50': { nominal: 50, correctionValue: -0.008, uncertainty: 0.08, kFactor: 2.0 },
                '100': { nominal: 100, correctionValue: -0.015, uncertainty: 0.16, kFactor: 2.0 },
                '200': { nominal: 200, correctionValue: -0.03, uncertainty: 0.32, kFactor: 2.0 },
                '500': { nominal: 500, correctionValue: -0.075, uncertainty: 0.8, kFactor: 2.0 }
            }
        };

        this.currentResult = mockData;
        this.renderResult(mockData);
        this._elements['result-container'].style.display = 'flex';
        this._elements['history-container'].style.display = 'block';
    }

    renderResult(data) {
        this._renderGradeBanner(data);
        this._renderCalibrationPoints(data);
        this._renderErrorMetrics(data);
        this._renderUncertainty(data);
        this._renderVibrationAnalysis(data);
    }

    _renderGradeBanner(data) {
        const banner = this._elements['result-banner'];
        if (!banner) return;

        const grade = data.calibrationGrade || 'F1';
        const pass = this._isGradePassing(grade);

        const gradeColors = {
            'E1': 'success', 'E2': 'primary', 'F1': 'info',
            'F2': 'warning', 'M1': 'secondary', 'M2': 'danger'
        };
        const color = gradeColors[grade] || 'secondary';

        const device = this.devices.find(d => d.id === data.deviceId);
        const balance = this.balances.find(b => b.id === data.balanceId);

        banner.innerHTML = `
            <div class="alert ${pass ? 'alert-success' : 'alert-danger'}">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h3 class="mb-0">
                            ${pass ? '<i class="bi bi-check-circle-fill"></i>' : '<i class="bi bi-x-circle-fill"></i>'}
                            校准${pass ? '通过' : '未通过'}
                        </h3>
                        <small>
                            ${device ? device.deviceName || device.name : '未知装置'}
                            ${balance ? ' → ' + (balance.balanceName || balance.name) : ''}
                            · ${new Date(data.calibrationTime).toLocaleString()}
                        </small>
                    </div>
                    <div class="text-end">
                        <div class="display-4 mb-0 text-${color}">${grade}</div>
                        <small>等级</small>
                    </div>
                </div>
                <div class="mt-2">
                    <span class="badge bg-${color} me-2">扩展不确定度: ${(data.correctedUncertainty * 100).toFixed(4)}%</span>
                    <span class="badge bg-info me-2">线性误差: ${(data.linearityError || 0).toFixed(4)}%</span>
                    <span class="badge bg-secondary">置信度: 95.45%</span>
                </div>
            </div>
        `;
    }

    _isGradePassing(grade) {
        const passingGrades = ['E1', 'E2', 'F1', 'F2', 'M1'];
        return passingGrades.includes(grade);
    }

    _renderCalibrationPoints(data) {
        const table = this._elements['calibration-points-table'];
        if (!table || !data.positionsData) return;

        const tbody = table.querySelector('tbody');
        tbody.innerHTML = '';

        const positions = Object.entries(data.positionsData).sort((a, b) => {
            return a[1].nominal - b[1].nominal;
        });

        positions.forEach((pos, index) => {
            const pt = pos[1];
            const avgReading = (pt.leftReading + pt.rightReading) / 2;
            const error = pt.nominal > 0 ? ((avgReading - pt.nominal) / pt.nominal) * 100 : 0;
            const pass = Math.abs(error) < 0.01;

            tbody.innerHTML += `
                <tr>
                    <td>${index + 1}</td>
                    <td>${pt.nominal}</td>
                    <td>${pt.leftReading?.toFixed?.(4) ?? pt.leftReading}</td>
                    <td>${pt.rightReading?.toFixed?.(4) ?? pt.rightReading}</td>
                    <td>${avgReading.toFixed(4)}</td>
                    <td class="${pass ? 'text-success' : 'text-warning'}">${error.toFixed(4)}%</td>
                    <td>${pass ? '<span class="badge bg-success">合格</span>' : '<span class="badge bg-warning">偏高</span>'}</td>
                </tr>
            `;
        });
    }

    _renderErrorMetrics(data) {
        const container = this._elements['error-metrics'];
        if (!container) return;

        const errors = [
            { name: '线性误差', value: data.linearityError || 0, unit: '%' },
            { name: '重复性误差', value: (data.repeatabilityStd || 0) * 100, unit: '%' },
            { name: '滞后误差', value: (data.hysteresisError || 0) * 100, unit: '%' },
            { name: '零点漂移', value: (data.zeroPointDrift || 0) * 100, unit: '%' },
            { name: '左臂修正', value: (data.leftArmCorrection || 0) * 100, unit: '%' },
            { name: '右臂修正', value: (data.rightArmCorrection || 0) * 100, unit: '%' },
            { name: '臂比修正', value: (data.armLengthRatioCorrection || 0) * 100, unit: '%' },
            { name: '扩展不确定度', value: (data.correctedUncertainty || 0) * 100, unit: '%' }
        ];

        let html = '';
        errors.forEach(err => {
            const pass = Math.abs(err.value) < 0.1;
            html += `
                <div class="col-md-3 col-6">
                    <div class="card text-center ${pass ? '' : 'border-warning'}">
                        <div class="card-body py-2">
                            <div class="small text-muted">${err.name}</div>
                            <div class="h6 mb-0 ${pass ? 'text-success' : 'text-warning'}">
                                ${err.value.toFixed(4)}${err.unit}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        container.innerHTML = html;
    }

    _renderUncertainty(data) {
        const table = this._elements['uncertainty-table'];
        if (!table) return;

        const sources = this._generateUncertaintySources(data);
        const tbody = table.querySelector('tbody');
        tbody.innerHTML = '';

        const totalContribution = sources.reduce((s, u) => s + u.value * u.value, 0);

        sources.forEach(src => {
            const contribution = totalContribution > 0
                ? (src.value * src.value / totalContribution * 100).toFixed(1)
                : 0;
            tbody.innerHTML += `
                <tr>
                    <td>${src.source}</td>
                    <td><span class="badge ${src.type === 'A类' ? 'bg-primary' : 'bg-secondary'}">${src.type}</span></td>
                    <td>${(src.value * 1000000).toFixed(1)} ppm</td>
                    <td>${src.degreesOfFreedom || '-'}</td>
                    <td>
                        <div class="progress" style="height: 6px;">
                            <div class="progress-bar" style="width: ${contribution}%"></div>
                        </div>
                        <small>${contribution}%</small>
                    </td>
                </tr>
            `;
        });

        const stdUncertainty = Math.sqrt(totalContribution);
        const kFactor = 2.0;
        const expandedUncertainty = data.correctedUncertainty || stdUncertainty * kFactor;

        if (this._elements['std-uncertainty']) {
            this._elements['std-uncertainty'].textContent = (stdUncertainty * 100).toFixed(4) + '%';
        }
        if (this._elements['k-factor']) {
            this._elements['k-factor'].textContent = kFactor;
        }
        if (this._elements['expanded-uncertainty']) {
            this._elements['expanded-uncertainty'].textContent = (expandedUncertainty * 100).toFixed(4) + '%';
        }
    }

    _generateUncertaintySources(data) {
        const sources = [
            { source: '重复性测量', value: data.repeatabilityStd || 0.0003, type: 'A类', degreesOfFreedom: 9 },
            { source: '线性误差', value: (data.linearityError || 0.06) / 100.0, type: 'B类', degreesOfFreedom: 50 },
            { source: '零点漂移', value: Math.abs(data.zeroPointDrift || 0.00015), type: 'B类', degreesOfFreedom: 20 },
            { source: '滞后误差', value: data.hysteresisError || 0.0002, type: 'B类', degreesOfFreedom: 10 },
            { source: '环境振动影响', value: 0.00008, type: 'B类', degreesOfFreedom: 30 },
            { source: '标准砝码不确定度', value: 0.0001, type: 'B类', degreesOfFreedom: 50 },
            { source: '温度影响', value: 0.00005, type: 'B类', degreesOfFreedom: 50 },
            { source: '湿度影响', value: 0.00002, type: 'B类', degreesOfFreedom: 50 }
        ];
        return sources;
    }

    _renderVibrationAnalysis(data) {
        const vibAnalysis = data.rawMeasurements?.vibrationAnalysis;
        if (!vibAnalysis) return;

        if (this._elements['vib-level']) {
            this._elements['vib-level'].textContent = vibAnalysis.environmentLabel || vibAnalysis.environmentLevel || '-';
        }
        if (this._elements['vib-level-bar']) {
            const levelIndex = this._getVibrationLevelIndex(vibAnalysis.environmentLevel);
            this._elements['vib-level-bar'].style.width = levelIndex + '%';
        }
        if (this._elements['isolation-system']) {
            this._elements['isolation-system'].textContent = vibAnalysis.isolationLabel || vibAnalysis.isolationSystem || '-';
        }
        if (this._elements['isolation-bar']) {
            const eff = vibAnalysis.isolationEfficiency_dB || 0;
            this._elements['isolation-bar'].style.width = Math.min(eff * 6, 100) + '%';
        }
        if (this._elements['input-vib']) {
            const rms = this._calculateInputRMS(vibAnalysis);
            this._elements['input-vib'].textContent = rms.toFixed(2) + ' μm';
        }
        if (this._elements['residual-vib']) {
            this._elements['residual-vib'].textContent =
                (vibAnalysis.residualVibrationRMS_um || 0).toFixed(3) + ' μm';
        }
        if (this._elements['vib-error']) {
            this._elements['vib-error'].textContent =
                (vibAnalysis.vibrationInducedError_mm || 0).toFixed(5) + ' mm';
        }

        this._renderGradeDisplay(data);
    }

    _getVibrationLevelIndex(level) {
        const levels = ['VC_A', 'VC_B', 'VC_C', 'VC_D', 'VC_E', 'WORKSHOP'];
        const index = levels.indexOf(level);
        return index >= 0 ? ((index + 1) / levels.length * 100) : 50;
    }

    _calculateInputRMS(vibAnalysis) {
        const x = vibAnalysis.inputVibrationX_um || 0;
        const y = vibAnalysis.inputVibrationY_um || 0;
        const z = vibAnalysis.inputVibrationZ_um || 0;
        return Math.sqrt((x * x + y * y + z * z) / 3);
    }

    _renderGradeDisplay(data) {
        const grade = data.calibrationGrade || 'F1';

        const gradeInfo = {
            'E1': { name: '一等标准', desc: '国家最高计量标准，一等砝码检定', color: 'text-success' },
            'E2': { name: '二等标准', desc: '工作计量标准，二等砝码检定', color: 'text-primary' },
            'F1': { name: '一等工作', desc: '精密天平校准，实验室分析', color: 'text-info' },
            'F2': { name: '二等工作', desc: '商业天平校准，一般工业', color: 'text-warning' },
            'M1': { name: '普通一级', desc: '市场衡器校准，一般贸易', color: 'text-secondary' },
            'M2': { name: '普通二级', desc: '粗糙称量，非贸易用途', color: 'text-danger' }
        };

        const info = gradeInfo[grade] || { name: '未知', desc: '-', color: 'text-muted' };

        if (this._elements['grade-display']) {
            this._elements['grade-display'].textContent = grade;
            this._elements['grade-display'].className = 'display-3 mb-2 ' + info.color;
        }
        if (this._elements['grade-badge']) {
            this._elements['grade-badge'].textContent = info.name;
            this._elements['grade-badge'].className = 'badge bg-' + info.color.replace('text-', '');
        }
        if (this._elements['grade-description']) {
            this._elements['grade-description'].textContent = info.desc;
        }
    }

    async _handleSimulate() {
        const vibLevel = this._elements['sim-vib-level']?.value || 'VC_C';
        const isolationType = this._elements['sim-isolation']?.value || 'NONE';
        const knifeRadius = parseFloat(this._elements['sim-knife-radius']?.value || '0.5');

        try {
            const result = await this.api.get(
                `${this.options.apiBase}/calibration-device/vibration/simulate?vibLevel=${vibLevel}&isolationType=${isolationType}&knifeRadius=${knifeRadius}`
            );

            if (result.success) {
                this._renderSimulationResult(result.data);
            } else {
                throw new Error(result.message || '模拟失败');
            }
        } catch (e) {
            console.warn('振动模拟失败，使用模拟数据:', e);
            this._loadMockSimulationResult(vibLevel, isolationType, knifeRadius);
        }
    }

    _loadMockSimulationResult(vibLevel, isolationType, knifeRadius) {
        const vibLevels = {
            'VC_A': 0.4, 'VC_B': 1.0, 'VC_C': 2.5,
            'VC_D': 6.0, 'VC_E': 15.0, 'WORKSHOP': 30.0
        };
        const isoEff = {
            'NONE': 0, 'PASSIVE_RUBBER': 0.6, 'PASSIVE_AIR': 0.85,
            'ACTIVE_PIEZO': 0.95, 'ACTIVE_MAGNETIC': 0.99
        };

        const inputVib = vibLevels[vibLevel] || 2.5;
        const efficiency = isoEff[isolationType] || 0;
        const residualVib = inputVib * (1 - efficiency);
        const vibError = residualVib / (knifeRadius * 1000) * 100;

        let grade, uncertainty;
        if (vibError < 0.001) { grade = 'E1'; uncertainty = 0.0008; }
        else if (vibError < 0.01) { grade = 'E2'; uncertainty = 0.005; }
        else if (vibError < 0.1) { grade = 'F1'; uncertainty = 0.03; }
        else if (vibError < 1) { grade = 'F2'; uncertainty = 0.15; }
        else if (vibError < 10) { grade = 'M1'; uncertainty = 0.8; }
        else { grade = 'M2'; uncertainty = 2.0; }

        const assessment = this._generateAssessment(vibError, grade);

        this._renderSimulationResult({
            achievableGrade: grade,
            expandedUncertaintyEstimate: uncertainty,
            assessment: assessment,
            residualVibration: { rmsDisplacement_um: residualVib }
        });
    }

    _generateAssessment(vibError, grade) {
        if (vibError < 0.001) {
            return '振动影响极小，可忽略不计，完全满足' + grade + '级精度要求';
        } else if (vibError < 0.01) {
            return '振动影响较小，在可接受范围内，满足' + grade + '级精度要求';
        } else if (vibError < 0.1) {
            return '振动影响中等，需关注，建议采用更高级的减振系统以提升精度';
        } else if (vibError < 1.0) {
            return '振动影响较大，可能影响测量准确性，强烈建议升级减振系统';
        } else {
            return '振动影响严重，无法保证测量精度，必须采取有效减振措施';
        }
    }

    _renderSimulationResult(data) {
        const resultEl = this._elements['sim-result'];
        if (!resultEl) return;

        resultEl.style.display = 'block';

        if (this._elements['sim-grade']) {
            this._elements['sim-grade'].textContent = data.achievableGrade || '-';
        }
        if (this._elements['sim-uncertainty']) {
            this._elements['sim-uncertainty'].textContent =
                (data.expandedUncertaintyEstimate || 0).toFixed(4) + '%';
        }
        if (this._elements['sim-assessment']) {
            this._elements['sim-assessment'].textContent = data.assessment || '';
        }
    }

    async loadHistory() {
        const balanceId = this._elements['balance-select']?.value;
        if (!balanceId) {
            alert('请先选择待校天平');
            return;
        }

        try {
            const result = await this.api.get(
                `${this.options.apiBase}/calibration-device/history/${balanceId}`
            );
            if (result.success) {
                this._renderHistory(result.data);
            }
        } catch (e) {
            console.warn('加载历史失败:', e);
            this._loadMockHistory();
        }
    }

    _loadMockHistory() {
        const history = [
            {
                id: 1,
                deviceId: 1,
                balanceId: 1,
                calibrationTime: new Date(Date.now() - 86400000 * 7).toISOString(),
                calibrationGrade: 'F1',
                correctedUncertainty: 0.0016,
                pass: true
            },
            {
                id: 2,
                deviceId: 1,
                balanceId: 1,
                calibrationTime: new Date(Date.now() - 86400000 * 30).toISOString(),
                calibrationGrade: 'F2',
                correctedUncertainty: 0.008,
                pass: true
            },
            {
                id: 3,
                deviceId: 2,
                balanceId: 1,
                calibrationTime: new Date(Date.now() - 86400000 * 90).toISOString(),
                calibrationGrade: 'M1',
                correctedUncertainty: 0.05,
                pass: false
            }
        ];
        this._renderHistory(history);
    }

    _renderHistory(history) {
        const list = this._elements['history-list'];
        if (!list) return;

        if (!history || history.length === 0) {
            list.innerHTML = '<p class="placeholder-text mb-0">暂无校准历史记录</p>';
            return;
        }

        let html = '';
        history.forEach(item => {
            const device = this.devices.find(d => d.id === item.deviceId);
            const gradeColors = {
                'E1': 'success', 'E2': 'primary', 'F1': 'info',
                'F2': 'warning', 'M1': 'secondary', 'M2': 'danger'
            };
            const color = gradeColors[item.calibrationGrade] || 'secondary';
            const pass = item.pass !== false;

            html += `
                <div class="list-group-item list-group-item-action" style="cursor: pointer;" data-history-id="${item.id}">
                    <div class="d-flex justify-content-between">
                        <div>
                            <h6 class="mb-1">${device ? device.deviceName || device.name : '未知装置'}</h6>
                            <small class="text-muted">${new Date(item.calibrationTime).toLocaleString()}</small>
                        </div>
                        <div class="text-end">
                            <span class="badge ${pass ? 'bg-success' : 'bg-danger'}">
                                ${pass ? '通过' : '未通过'}
                            </span>
                            <div><small class="text-${color}">${item.calibrationGrade} · ${(item.correctedUncertainty * 100).toFixed(4)}%</small></div>
                        </div>
                    </div>
                </div>
            `;
        });
        list.innerHTML = html;

        list.querySelectorAll('[data-history-id]').forEach(el => {
            el.addEventListener('click', () => {
                const id = parseInt(el.dataset.historyId);
                this._loadHistoryDetail(id);
            });
        });
    }

    _loadHistoryDetail(id) {
        console.log('加载历史详情:', id);
    }

    render() {
        if (this.currentResult) {
            this.renderResult(this.currentResult);
            this._elements['result-container'].style.display = 'flex';
            this._elements['history-container'].style.display = 'block';
        }
    }

    getCurrentResult() {
        return this.currentResult;
    }

    getDevices() {
        return this.devices;
    }

    getBalances() {
        return this.balances;
    }

    destroy() {
        this.container.innerHTML = '';
        this.devices = [];
        this.balances = [];
        this.currentResult = null;
        this.initialized = false;
        this._elements = {};
    }
}

if (typeof window !== 'undefined') {
    window.CalibrationDeviceComponent = CalibrationDeviceComponent;
}
