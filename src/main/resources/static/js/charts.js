(() => {
    const charts = new Map();
    const palette = ["#2f6f55", "#cf7047", "#769b8a", "#e1a27e", "#94a69e", "#6b8177", "#bd8e75", "#416e5b", "#d9b39d", "#82988e"];

    const canvasBackground = {
        id: "canvasBackground",
        beforeDraw(chart) {
            const {ctx, width, height} = chart;
            ctx.save();
            ctx.globalCompositeOperation = "destination-over";
            ctx.fillStyle = "#fffdf8";
            ctx.fillRect(0, 0, width, height);
            ctx.restore();
        }
    };

    const itemsFor = canvas => [...canvas.parentElement.querySelectorAll(".chart-data span")].map(item => ({
        label: item.dataset.label,
        value: Number(item.dataset.value),
        image: item.dataset.image
    }));

    const valueLabel = (value, unit) => {
        if (unit === "minutes") {
            const hours = Math.floor(value / 60);
            const minutes = value % 60;
            return hours === 0 ? `${minutes} Min.` : minutes === 0 ? `${hours} Std.` : `${hours} Std. ${minutes} Min.`;
        }
        return `${value} ${value === 1 ? "Partie" : "Partien"}`;
    };

    const baseOptions = (canvas, unit) => ({
        responsive: true,
        maintainAspectRatio: false,
        animation: {duration: 450},
        plugins: {
            legend: {display: false},
            tooltip: {
                callbacks: {
                    label: context => `${context.dataset.label ? `${context.dataset.label}: ` : ""}${valueLabel(context.parsed.x ?? context.parsed, unit)}`
                }
            }
        },
        scales: {
            x: {
                beginAtZero: true,
                grid: {color: "rgba(20, 36, 29, .08)"},
                ticks: {callback: value => unit === "minutes" ? valueLabel(value, unit) : value, color: "#68766f"}
            },
            y: {grid: {display: false}, ticks: {color: "#14241d", font: {weight: 700}}}
        }
    });

    const createBarChart = canvas => {
        const items = itemsFor(canvas);
        const accent = canvas.dataset.accent === "true";
        return new Chart(canvas, {
            type: "bar",
            data: {
                labels: items.map(item => item.label),
                datasets: [{data: items.map(item => item.value), backgroundColor: accent ? "#cf7047" : "#2f6f55", borderRadius: 7, borderSkipped: false}]
            },
            options: {...baseOptions(canvas, canvas.dataset.unit), indexAxis: "y"},
            plugins: [canvasBackground]
        });
    };

    const loadPattern = (canvas, source, fallback) => new Promise(resolve => {
        if (!source) {
            resolve(fallback);
            return;
        }
        const image = new Image();
        image.onload = () => {
            const tile = document.createElement("canvas");
            tile.width = 180;
            tile.height = 180;
            const context = tile.getContext("2d");
            const scale = Math.max(tile.width / image.naturalWidth, tile.height / image.naturalHeight);
            const width = image.naturalWidth * scale;
            const height = image.naturalHeight * scale;
            context.drawImage(image, (tile.width - width) / 2, (tile.height - height) / 2, width, height);
            context.fillStyle = "rgba(16, 37, 29, .14)";
            context.fillRect(0, 0, tile.width, tile.height);
            resolve(canvas.getContext("2d").createPattern(tile, "repeat"));
        };
        image.onerror = () => resolve(fallback);
        image.src = source;
    });

    const createCoverPie = async canvas => {
        const sorted = itemsFor(canvas).sort((left, right) => right.value - left.value || left.label.localeCompare(right.label, "de"));
        const topGames = sorted.slice(0, 10);
        const otherPlays = sorted.slice(10).reduce((sum, item) => sum + item.value, 0);
        const shown = otherPlays > 0 ? [...topGames, {label: "Sonstiges", value: otherPlays}] : topGames;
        const patterns = await Promise.all(shown.map((item, index) => loadPattern(canvas, item.image, palette[index])));
        return new Chart(canvas, {
            type: "pie",
            data: {
                labels: shown.map(item => item.label),
                datasets: [{data: shown.map(item => item.value), backgroundColor: patterns, borderColor: "#fffdf8", borderWidth: 4, hoverOffset: 8}]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {duration: 450},
                plugins: {
                    legend: {position: "bottom", labels: {padding: 18, usePointStyle: true, pointStyle: "rectRounded", color: "#14241d", font: {weight: 700}}},
                    tooltip: {callbacks: {label: context => `${context.label}: ${valueLabel(context.parsed, canvas.dataset.unit)}`}}
                }
            },
            plugins: [canvasBackground]
        });
    };

    const initialize = async canvas => {
        const chart = canvas.dataset.chart === "cover-pie" ? await createCoverPie(canvas) : createBarChart(canvas);
        charts.set(canvas.id, chart);
    };

    const save = button => {
        const chart = charts.get(button.dataset.chartSave);
        if (!chart) {
            return;
        }
        chart.canvas.toBlob(blob => {
            if (!blob) {
                return;
            }
            const link = document.createElement("a");
            const url = URL.createObjectURL(blob);
            link.download = `${button.dataset.chartSave}.png`;
            link.href = url;
            link.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        }, "image/png");
    };

    document.querySelectorAll("canvas[data-chart]").forEach(canvas => initialize(canvas));
    document.querySelectorAll("[data-chart-save]").forEach(button => button.addEventListener("click", () => save(button)));
})();
