class RadarChart {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            console.error('Canvas not found:', canvasId);
            return;
        }
        this.ctx = this.canvas.getContext('2d');
        this.width = this.canvas.width;
        this.height = this.canvas.height;
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;
        this.radius = Math.min(this.width, this.height) / 2 - 50;

        this.dimensions = [];
        this.datasets = [];
        this.colors = [
            { stroke: '#1976d2', fill: 'rgba(25, 118, 210, 0.2)' },
            { stroke: '#e53935', fill: 'rgba(229, 57, 53, 0.2)' },
            { stroke: '#43a047', fill: 'rgba(67, 160, 71, 0.2)' },
            { stroke: '#fb8c00', fill: 'rgba(251, 140, 0, 0.2)' },
            { stroke: '#8e24aa', fill: 'rgba(142, 36, 170, 0.2)' },
            { stroke: '#00acc1', fill: 'rgba(0, 172, 193, 0.2)' },
            { stroke: '#6d4c41', fill: 'rgba(109, 76, 65, 0.2)' },
            { stroke: '#fdd835', fill: 'rgba(253, 216, 53, 0.2)' }
        ];

        this.animationProgress = 0;
        this.animating = false;
    }

    setData(dimensions, datasets) {
        this.dimensions = dimensions;
        this.datasets = datasets;
        this.animationProgress = 0;
        this.animate();
    }

    animate() {
        if (this.animating) return;
        this.animating = true;

        const animateFrame = () => {
            this.animationProgress += 0.02;
            if (this.animationProgress >= 1) {
                this.animationProgress = 1;
                this.animating = false;
                this.draw();
                return;
            }
            this.draw();
            requestAnimationFrame(animateFrame);
        };
        animateFrame();
    }

    draw() {
        if (!this.ctx || this.dimensions.length === 0) return;

        this.ctx.clearRect(0, 0, this.width, this.height);

        this.drawGrid();
        this.drawAxes();
        this.drawDatasets();
        this.drawLabels();
        this.drawLegend();
    }

    drawGrid() {
        const levels = 5;
        this.ctx.strokeStyle = '#e0e0e0';
        this.ctx.lineWidth = 1;

        for (let level = 1; level <= levels; level++) {
            const r = (this.radius * level) / levels;
            this.ctx.beginPath();

            for (let i = 0; i <= this.dimensions.length; i++) {
                const angle = (Math.PI * 2 * i) / this.dimensions.length - Math.PI / 2;
                const x = this.centerX + r * Math.cos(angle);
                const y = this.centerY + r * Math.sin(angle);

                if (i === 0) {
                    this.ctx.moveTo(x, y);
                } else {
                    this.ctx.lineTo(x, y);
                }
            }
            this.ctx.closePath();
            this.ctx.stroke();

            this.ctx.fillStyle = '#999';
            this.ctx.font = '10px sans-serif';
            this.ctx.textAlign = 'right';
            this.ctx.fillText((level * 20).toString() + '分', this.centerX - 5, this.centerY - r + 3);
        }
    }

    drawAxes() {
        this.ctx.strokeStyle = '#bdbdbd';
        this.ctx.lineWidth = 1;

        for (let i = 0; i < this.dimensions.length; i++) {
            const angle = (Math.PI * 2 * i) / this.dimensions.length - Math.PI / 2;
            const x = this.centerX + this.radius * Math.cos(angle);
            const y = this.centerY + this.radius * Math.sin(angle);

            this.ctx.beginPath();
            this.ctx.moveTo(this.centerX, this.centerY);
            this.ctx.lineTo(x, y);
            this.ctx.stroke();
        }
    }

    drawDatasets() {
        const progress = this.animationProgress;

        this.datasets.forEach((dataset, idx) => {
            const color = this.colors[idx % this.colors.length];

            this.ctx.beginPath();
            for (let i = 0; i < this.dimensions.length; i++) {
                const angle = (Math.PI * 2 * i) / this.dimensions.length - Math.PI / 2;
                const value = dataset.values[i] || 0;
                const r = (this.radius * value / 100) * progress;
                const x = this.centerX + r * Math.cos(angle);
                const y = this.centerY + r * Math.sin(angle);

                if (i === 0) {
                    this.ctx.moveTo(x, y);
                } else {
                    this.ctx.lineTo(x, y);
                }
            }
            this.ctx.closePath();

            this.ctx.fillStyle = color.fill;
            this.ctx.fill();

            this.ctx.strokeStyle = color.stroke;
            this.ctx.lineWidth = 2;
            this.ctx.stroke();

            for (let i = 0; i < this.dimensions.length; i++) {
                const angle = (Math.PI * 2 * i) / this.dimensions.length - Math.PI / 2;
                const value = dataset.values[i] || 0;
                const r = (this.radius * value / 100) * progress;
                const x = this.centerX + r * Math.cos(angle);
                const y = this.centerY + r * Math.sin(angle);

                this.ctx.beginPath();
                this.ctx.arc(x, y, 4, 0, Math.PI * 2);
                this.ctx.fillStyle = color.stroke;
                this.ctx.fill();
                this.ctx.strokeStyle = '#fff';
                this.ctx.lineWidth = 1;
                this.ctx.stroke();
            }
        });
    }

    drawLabels() {
        this.ctx.fillStyle = '#333';
        this.ctx.font = 'bold 12px sans-serif';
        this.ctx.textAlign = 'center';

        for (let i = 0; i < this.dimensions.length; i++) {
            const angle = (Math.PI * 2 * i) / this.dimensions.length - Math.PI / 2;
            const labelRadius = this.radius + 25;
            const x = this.centerX + labelRadius * Math.cos(angle);
            const y = this.centerY + labelRadius * Math.sin(angle);

            let align = 'center';
            if (Math.cos(angle) > 0.5) align = 'left';
            if (Math.cos(angle) < -0.5) align = 'right';

            this.ctx.textAlign = align;
            this.ctx.fillText(this.dimensions[i], x, y + 4);
        }
    }

    drawLegend() {
        const startX = 10;
        const startY = 20;
        const lineHeight = 20;

        this.ctx.textAlign = 'left';
        this.ctx.font = '11px sans-serif';

        this.datasets.forEach((dataset, idx) => {
            const y = startY + idx * lineHeight;
            const color = this.colors[idx % this.colors.length];

            this.ctx.fillStyle = color.stroke;
            this.ctx.fillRect(startX, y - 8, 12, 12);

            this.ctx.fillStyle = '#333';
            this.ctx.fillText(dataset.label, startX + 18, y + 2);

            if (dataset.avgScore !== undefined) {
                this.ctx.fillStyle = '#666';
                this.ctx.fillText(`(avg: ${dataset.avgScore.toFixed(1)}分)`,
                    startX + this.ctx.measureText(dataset.label).width + 25, y + 2);
            }
        });
    }

    resize(width, height) {
        this.width = width;
        this.height = height;
        this.canvas.width = width;
        this.canvas.height = height;
        this.centerX = width / 2;
        this.centerY = height / 2;
        this.radius = Math.min(width, height) / 2 - 50;
        this.draw();
    }
}

if (typeof window !== 'undefined') {
    window.RadarChart = RadarChart;
}
