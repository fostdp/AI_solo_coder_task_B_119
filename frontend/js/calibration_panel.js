class CalibrationPanelController {
    constructor(api) {
        this.api = api;
        this.devices = [];
        this.balances = [];
        this.currentResult = null;
    }

    async init() {
        await this.loadDevices();
        await this.loadBalances();
        this.bindEvents();
        await this.loadLatest();
    }

    async loadDevices() {
        try {
            const result = await this.api.get(`${AppConfig.API_BASE}${AppConfig.apiEndpoints.calibration.devices}`);
            if (result.success) {
                this.devices = result.data;
                this.renderDeviceSelect();
            }
        } catch (e) {
            console.error('加载校准装置失败:', e);
            this.loadMockDevices();
        }
    }

    loadMockDevices() {
        this.devices = [
            { id: 1, name: '杠杆原理校准装置', description: '基于古代杠杆原理设计', armLength: 500, knifeRadius: 0.1, maxCapacity: 5000, calibrationMethod: 'LEVER_PRINCIPLE' },
            { id: 2, name: '罗马式校准装置', description: '不等臂设计', armLength: 400, knifeRadius: 0.15, maxCapacity: 3000, calibrationMethod: 'ROME_STEELYARD' },
            { id: 3, name: '精密玛瑙刀口装置', description: '高精度玛瑙刀口', armLength: 600, knifeRadius: 0.05, maxCapacity: 2000, calibrationMethod: 'PRECISION_AGATE' }
        ];
        this.renderDeviceSelect();
    }

    renderDeviceSelect() {
        const select = document.getElementById('calibrationDeviceSelect');
        if (!select) return;

        select.innerHTML = '<option value="">请选择校准装置...</option>';
        this.devices.forEach(d => {
            select.innerHTML += `<option value="${d.id}">${d.name}</option>`;
        });
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
            this.balances = [
                { id: 1, name: '战国青铜天平' },
                { id: 2, name: '唐代玛瑙天平' }
            ];
            this.renderBalanceSelect();
        }
    }

    renderBalanceSelect() {
        const select = document.getElementById('calibrationBalanceSelect');
        if (!select) return;

        select.innerHTML = '<option value="">请选择待校天平...</option>';
        this.balances.forEach(b => {
            select.innerHTML += `<option value="${b.id}">${b.name}</option>`;
        });
    }

    bindEvents() {
        document.addEventListener('click', async (e) => {
            if (e.target.id === 'calibrateBtn') {
                const deviceId = document.getElementById('calibrationDeviceSelect').value;
                const balanceId = document.getElementById('calibrationBalanceSelect').value;
                const method = document.getElementById('calibrationMethodSelect').value;

                if (!deviceId || !balanceId) {
                    alert('请选择校准装置和待校天平');
                    return;
                }

                await this.calibrate(parseInt(deviceId), parseInt(balanceId), method);
            }

            if (e.target.id === 'loadHistoryBtn') {
                await this.loadHistory();
            }

            if (e.target.id === 'generateReportBtn') {
                this.generateReport();
            }

            if (e.target.id === 'exportReportBtn') {
                this.exportReport();
            }
        });
    }

    async calibrate(deviceId, balanceId, method) {
        try {
            this.showLoading();
            const result = await this.api.post(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.calibration.calibrate}`, {
                deviceId: deviceId,
                balanceId: balanceId,
                calibrationMethod: method || 'SEVEN_POINT'
            });
            if (result.success) {
                this.currentResult = result.data;
                this.renderResult(result.data);
            }
        } catch (e) {
            console.error('校准失败:', e);
            this.loadMockResult();
        } finally {
            this.hideLoading();
        }
    }

    async loadLatest() {
        try {
            const result = await this.api.get(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.calibration.latest}`
            );
            if (result.success && result.data) {
                this.currentResult = result.data;
                this.renderResult(result.data);
            }
        } catch (e) {
            console.error('加载最新失败:', e);
        }
    }

    async loadHistory() {
        try {
            const balanceId = document.getElementById('calibrationBalanceSelect').value;
            const result = await this.api.get(
                `${AppConfig.API_BASE}${AppConfig.apiEndpoints.calibration.history}/${balanceId || 'all'}`
            );
            if (result.success) {
                this.renderHistory(result.data);
            }
        } catch (e) {
            console.error('加载历史失败:', e);
        }
    }

    loadMockResult() {
        const mockData = {
            deviceName: '杠杆原理校准装置',
            balanceName: '战国青铜天平',
            calibrationTime: new Date().toISOString(),
            calibrationMethod: '七点线性校准',
            calibrationGrade: 'F1',
            overallError: 0.0008,
            linearityError: 0.0006,
            repeatabilityError: 0.0003,
            hysteresisError: 0.0002,
            zeroDrift: 0.00015,
            sensitivityDrift: 0.0004,
            temperatureEffect: 0.0002,
            eccentricityError: 0.00025,
            repeatabilityStdDev: 0.0001,
            expandedUncertainty: 0.0016,
            coverageFactor: 2,
            confidenceLevel: 95.45,
            pass: true,
            uncertaintySources: [
                { name: '重复性(A类)', value: 0.0003, type: 'A' },
                { name: '线性误差(B类)', value: 0.0006, type: 'B' },
                { name: '零点漂移(B类)', value: 0.00015, type: 'B' },
                { name: '滞后误差(B类)', value: 0.0002, type: 'B' },
                { name: '标准砝码(B类)', value: 0.0001, type: 'B' },
                { name: '温度影响(B类)', value: 0.0002, type: 'B' },
                { name: '湿度影响(B类)', value: 0.00005, type: 'B' }
            ],
            calibrationPoints: [
                { load: 0, indication: 0.0002, error: 0.0002 },
                { load: 500, indication: 500.1, error: 0.0002 },
                { load: 1000, indication: 1000.3, error: 0.0003 },
                { load: 2000, indication: 2000.8, error: 0.0004 },
                { load: 3000, indication: 3001.5, error: 0.0005 },
                { load: 4000, indication: 4002.0, error: 0.0005 },
                { load: 5000, indication: 5002.5, error: 0.0005 }
            ],
            environment: {
                temperature: 22.5, humidity: 45, temperatureStability: 0.3
            },
            adjustment: {
                beforeError: 0.005, afterError: 0.0008, improvementPercent: 84
            }
        };
        this.currentResult = mockData;
        this.renderResult(mockData);
    }

    renderResult(data) {
        this.renderGradeBanner(data);
        this.renderErrorMetrics(data);
        this.renderUncertainty(data);
        this.renderCalibrationCurve(data);
        this.renderEnvironment(data);
        this.renderAdjustment(data);
    }

    renderGradeBanner(data) {
        const banner = document.getElementById('calibrationResultBanner');
        if (!banner) return;

        const gradeColors = {
            'E1': 'success', 'E2': 'primary', 'F1': 'info',
            'F2': 'warning', 'M1': 'secondary', 'M2': 'danger'
        };
        const color = gradeColors[data.calibrationGrade] || 'secondary';

        banner.innerHTML = `
            <div class="alert ${data.pass ? 'alert-success' : 'alert-danger'}">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h3 class="mb-0">
                        ${data.pass ? '<i class="bi bi-check-circle-fill"></i>' : '<i class="bi bi-x-circle-fill"></i>'}
                        校准${data.pass ? '通过' : '未通过'}
                        </h3>
                        <small>${new Date(data.calibrationTime).toLocaleString()}</small>
                    </div>
                    <div class="text-end">
                        <div class="display-4 mb-0 text-${color}">${data.calibrationGrade}</div>
                        <small>等级</small>
                    </div>
                </div>
                <div class="mt-2">
                    <span class="badge bg-${color} me-2">综合误差: ${(data.overallError * 100).toFixed(4)}%</span>
                    <span class="badge bg-info me-2">扩展不确定度: ${(data.expandedUncertainty * 100).toFixed(4)}%</span>
                    <span class="badge bg-secondary">置信度: ${data.confidenceLevel}%</span>
                </div>
            </div>
        `;
    }

    renderErrorMetrics(data) {
        const container = document.getElementById('errorMetrics');
        if (!container) return;

        const errors = [
            { name: '综合误差', value: data.overallError, unit: '%' },
            { name: '线性误差', value: data.linearityError, unit: '%' },
            { name: '重复性误差', value: data.repeatabilityError, unit: '%' },
            { name: '滞后误差', value: data.hysteresisError, unit: '%' },
            { name: '零点漂移', value: data.zeroDrift, unit: '%' },
            { name: '灵敏度漂移', value: data.sensitivityDrift, unit: '%' },
            { name: '温度影响', value: data.temperatureEffect, unit: '%' },
            { name: '偏载误差', value: data.eccentricityError, unit: '%' }
        ];

        let html = '<div class="row">';
        errors.forEach(err => {
            const pass = err.value < 0.001;
            html += `
                <div class="col-md-3 mb-3">
                    <div class="card text-center ${pass ? '' : 'border-warning'}">
                        <div class="card-body py-3">
                            <div class="small text-muted">${err.name}</div>
                            <div class="h5 mb-0 ${pass ? 'text-success' : 'text-warning'}">
                                ${(err.value * 100).toFixed(4)}${err.unit}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    renderUncertainty(data) {
        const container = document.getElementById('uncertaintyBudget');
        if (!container || !data.uncertaintySources) return;

        let html = `
            <div class="card">
                <div class="card-header">
                    <h6 class="mb-0">
                        <i class="bi bi-calculator-fill"></i>
                        不确定度评定 (JJF 1059.1-2012)
                    </h6>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>不确定度来源</th>
                                    <th>类型</th>
                                    <th>数值</th>
                                    <th>贡献</th>
                                </tr>
                            </thead>
                            <tbody>
        `;

        const total = data.uncertaintySources.reduce((s, u) => s + u.value * u.value, 0);
        data.uncertaintySources.forEach(src => {
            const contribution = (src.value * src.value / total * 100).toFixed(1);
            html += `
                <tr>
                    <td>${src.name}</td>
                    <td><span class="badge ${src.type === 'A' ? 'bg-primary' : 'bg-secondary'}">${src.type}类</span></td>
                    <td>${(src.value * 1000000).toFixed(1)} ppm</td>
                    <td>
                        <div class="progress" style="height: 6px;">
                            <div class="progress-bar" style="width: ${contribution}%"></div>
                        </div>
                        <small>${contribution}%</small>
                    </td>
                </tr>
            `;
        });

        html += `
                            </tbody>
                        </table>
                    </div>
                    <div class="row mt-3 text-center">
                        <div class="col-4">
                            <div class="small text-muted">标准不确定度</div>
                            <div class="h5 mb-0">${(data.expandedUncertainty / data.coverageFactor * 100).toFixed(4)}%</div>
                        </div>
                        <div class="col-4">
                            <div class="small text-muted">包含因子 k</div>
                            <div class="h5 mb-0">${data.coverageFactor}</div>
                        </div>
                        <div class="col-4">
                            <div class="small text-muted">扩展不确定度 U</div>
                            <div class="h5 mb-0 text-primary">${(data.expandedUncertainty * 100).toFixed(4)}%</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.innerHTML = html;
    }

    renderCalibrationCurve(data) {
        const container = document.getElementById('calibrationCurve');
        if (!container || !data.calibrationPoints) return;

        let html = `
            <div class="card">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-graph-up-arrow"></i> 校准曲线</h6>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover">
                            <thead>
                                <tr>
                                    <th>载荷 (g)</th>
                                    <th>示值 (g)</th>
                                    <th>误差 (%)</th>
                                    <th>状态</th>
                                </tr>
                            </thead>
                            <tbody>
        `;

        data.calibrationPoints.forEach(pt => {
            const pass = pt.error < 0.001;
            html += `
                <tr>
                    <td>${pt.load}</td>
                    <td>${pt.indication.toFixed(1)}</td>
                    <td class="${pass ? 'text-success' : 'text-warning'}">${(pt.error * 100).toFixed(4)}%</td>
                    <td>${pass ? '<span class="badge bg-success">合格</span>' : '<span class="badge bg-warning">偏高</span>'}</td>
                </tr>
            `;
        });

        html += `
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
        container.innerHTML = html;
    }

    renderEnvironment(data) {
        const container = document.getElementById('environmentData');
        if (!container || !data.environment) return;

        const env = data.environment;
        container.innerHTML = `
            <div class="card bg-light">
                <div class="card-body">
                    <div class="row text-center">
                        <div class="col-4">
                            <i class="bi bi-thermometer-half display-6"></i>
                            <div class="h5 mb-0">${env.temperature}°C</div>
                            <small class="text-muted">环境温度</small>
                        </div>
                        <div class="col-4">
                            <i class="bi bi-moisture display-6"></i>
                            <div class="h5 mb-0">${env.humidity}%</div>
                            <small class="text-muted">相对湿度</small>
                        </div>
                        <div class="col-4">
                            <i class="bi bi-thermometer-sun display-6"></i>
                            <div class="h5 mb-0">±${env.temperatureStability}°C</div>
                            <small class="text-muted">温度波动</small>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderAdjustment(data) {
        const container = document.getElementById('adjustmentResult');
        if (!container || !data.adjustment) return;

        const adj = data.adjustment;
        container.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-sliders"></i> 调整效果</h6>
                </div>
                <div class="card-body">
                    <div class="row text-center">
                        <div class="col-4">
                            <div class="small text-muted">调整前误差</div>
                            <div class="h4 mb-0 text-warning">${(adj.beforeError * 100).toFixed(2)}%</div>
                        </div>
                        <div class="col-4">
                            <div class="small text-muted">调整后误差</div>
                            <div class="h4 mb-0 text-success">${(adj.afterError * 100).toFixed(4)}%</div>
                        </div>
                        <div class="col-4">
                            <div class="small text-muted">改善幅度</div>
                            <div class="h4 mb-0 text-primary">${adj.improvementPercent}%</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderHistory(data) {
        const container = document.getElementById('calibrationHistory');
        if (!container) return;

        let html = '<div class="list-group">';
        data.forEach(item => {
            html += `
                <div class="list-group-item">
                    <div class="d-flex justify-content-between">
                        <div>
                            <h6 class="mb-1">${item.deviceName} → ${item.balanceName}</h6>
                            <small class="text-muted">${new Date(item.calibrationTime).toLocaleString()}</small>
                        </div>
                        <div class="text-end">
                            <span class="badge ${item.pass ? 'bg-success' : 'bg-danger'}">
                                ${item.pass ? '通过' : '未通过'}
                            </span>
                            <div><small>${item.calibrationGrade} · ${(item.overallError * 100).toFixed(4)}%</small></div>
                        </div>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    generateReport() {
        if (!this.currentResult) {
            alert('请先进行校准');
            return;
        }
        const data = this.currentResult;
        const report = {
            title: '天平校准证书',
            reportNo: 'CAL-' + Date.now(),
            date: new Date().toLocaleDateString(),
            data: data
        };
        console.log('生成报告:', report);
        alert('校准报告已生成，请查看控制台');
    }

    exportReport() {
        if (!this.currentResult) {
            alert('请先进行校准');
            return;
        }
        const data = this.currentResult;
        const text = `
天平校准报告
================================
校准装置: ${data.deviceName}
待校天平: ${data.balanceName}
校准时间: ${new Date(data.calibrationTime).toLocaleString()}
校准方法: ${data.calibrationMethod}

【校准结果】
校准等级: ${data.calibrationGrade}
综合误差: ${(data.overallError * 100).toFixed(4)}%
重复性误差: ${(data.repeatabilityError * 100).toFixed(4)}%
线性误差: ${(data.linearityError * 100).toFixed(4)}%
滞后误差: ${(data.hysteresisError * 100).toFixed(4)}%
零点漂移: ${(data.zeroDrift * 100).toFixed(4)}%
灵敏度漂移: ${(data.sensitivityDrift * 100).toFixed(4)}%
温度影响: ${(data.temperatureEffect * 100).toFixed(4)}%
偏载误差: ${(data.eccentricityError * 100).toFixed(4)}%

【不确定度评定】
扩展不确定度 U = ${(data.expandedUncertainty * 100).toFixed(4)}%
包含因子 k = ${data.coverageFactor}
置信水平 = ${data.confidenceLevel}%

【环境条件】
温度: ${data.environment?.temperature}°C
湿度: ${data.environment?.humidity}%
温度波动: ±${data.environment?.temperatureStability}°C

【校准结论】
校准${data.pass ? '通过' : '未通过'}，达到 ${data.calibrationGrade} 等级要求

校准人员: 自动校准系统
        `.trim();

        const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `校准报告_${data.balanceName}_${new Date().toISOString().slice(0,10)}.txt`;
        link.click();
    }

    showLoading() {
        const btn = document.getElementById('calibrateBtn');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>校准中...';
        }
    }

    hideLoading() {
        const btn = document.getElementById('calibrateBtn');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-play-circle"></i> 开始校准';
        }
    }
}

if (typeof window !== 'undefined') {
    window.CalibrationPanelController = CalibrationPanelController;
}
