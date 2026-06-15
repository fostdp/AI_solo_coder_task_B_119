const ApiClient = {
    get: async (url) => {
        const response = await fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });
        return await response.json();
    },
    post: async (url, data) => {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    }
};

const PanelManager = {
    panels: {
        main: 'mainView',
        manufacturing: 'manufacturingPanel',
        civilization: 'civilizationPanel',
        calibration: 'calibrationPanel',
        virtual: 'virtualWeighingPanel'
    },

    showPanel(panelName) {
        Object.values(this.panels).forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                if (id === this.panels[panelName]) {
                    el.style.display = 'flex';
                } else {
                    el.style.display = 'none';
                }
            }
        });

        if (window.__controllers) {
            const { manufacturing, civilization, calibration, virtualWeighing } = window.__controllers;
            if (panelName === 'manufacturing' && manufacturing && !manufacturing.initialized) {
                manufacturing.init();
                manufacturing.initialized = true;
            }
            if (panelName === 'civilization' && civilization && !civilization.initialized) {
                civilization.init();
                civilization.initialized = true;
            }
            if (panelName === 'calibration' && calibration && !calibration.initialized) {
                calibration.init();
                calibration.initialized = true;
            }
            if (panelName === 'virtual' && virtualWeighing && !virtualWeighing.initialized) {
                virtualWeighing.init();
                virtualWeighing.initialized = true;
            }
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const balance3d = new Balance3D(AppConfig.balance3d.canvasId);
    const errorChart = new ErrorChart(AppConfig.chart.errorCanvasId);
    errorChart.maxPoints = AppConfig.chart.maxDataPoints;

    const panel = new MetrologyPanelController({
        balance3d: balance3d,
        errorChart: errorChart
    });

    const manufacturing = new ManufacturingPanelController(ApiClient);
    const civilization = new CivilizationPanelController(ApiClient);
    const calibration = new CalibrationPanelController(ApiClient);
    const virtualWeighing = new VirtualWeighingPanelController(ApiClient, balance3d);

    window.__controllers = {
        manufacturing,
        civilization,
        calibration,
        virtualWeighing,
        main: panel,
        initialized: false
    };

    document.addEventListener('click', (e) => {
        const tabBtn = e.target.closest('[data-main-tab]');
        if (tabBtn) {
            const tabName = tabBtn.dataset.mainTab;
            if (PanelManager.panels[tabName]) {
                PanelManager.showPanel(tabName);
            }
        }
    });

    panel.init().then(() => {
        console.log('[App] 古代天平衡器系统初始化完成');
        console.log('[App] 设备模式:', balance3d.isMobile ? '移动端' : '桌面端',
                    '| 性能等级:', balance3d.performanceLevel,
                    '| DPR:', balance3d.targetPixelRatio.toFixed(2));
    }).catch(err => console.error('[App] 初始化失败:', err));

    window.__metrology = { balance3d, errorChart, panel, ApiClient };

    window.closeModal = (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    };
});
