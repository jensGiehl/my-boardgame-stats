const loadingRoot = document.querySelector("#catalog-loading");

if (loadingRoot) {
    const progress = document.querySelector("#loading-progress");
    const message = document.querySelector("#loading-message");
    const error = document.querySelector("#loading-error");
    const errorMessage = document.querySelector("#loading-error-message");

    const showError = (text) => {
        progress.hidden = true;
        error.hidden = false;
        errorMessage.textContent = text;
    };

    const checkStatus = async () => {
        try {
            const response = await fetch(loadingRoot.dataset.statusUrl, {
                cache: "no-store",
                headers: {Accept: "application/json"}
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const status = await response.json();
            if (status.state === "READY") {
                message.textContent = status.message;
                window.location.reload();
                return;
            }
            if (status.state === "FAILED") {
                showError(status.message);
                return;
            }
            message.textContent = status.message;
        } catch (requestError) {
            message.textContent = "Der Ladestatus konnte kurzzeitig nicht abgefragt werden. Es wird weiter versucht.";
        }
        window.setTimeout(checkStatus, 1_250);
    };

    if (loadingRoot.dataset.loadingState !== "FAILED") {
        window.setTimeout(checkStatus, 500);
    }
}
