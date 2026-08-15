const copyButtons = document.querySelectorAll("[data-copy-target]");

copyButtons.forEach((copyButton) => {
    const copyTarget = document.querySelector(copyButton.dataset.copyTarget);
    const copyLabel = copyButton.querySelector("[data-copy-label]");
    const copyStatus = document.querySelector(`#${copyButton.getAttribute("aria-describedby")}`);
    const defaultLabel = copyLabel.textContent;

    const copyWithFallback = (text) => {
        copyTarget.focus();
        copyTarget.select();
        copyTarget.setSelectionRange(0, text.length);
        if (!document.execCommand("copy")) {
            throw new Error("Copy command failed");
        }
    };

    copyButton.addEventListener("click", async () => {
        copyButton.disabled = true;
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(copyTarget.value);
            } else {
                copyWithFallback(copyTarget.value);
            }
            copyLabel.textContent = "In die Zwischenablage kopiert";
            copyStatus.textContent = copyButton.dataset.copySuccess;
        } catch (copyError) {
            copyLabel.textContent = "Kopieren nicht möglich";
            copyStatus.textContent = copyButton.dataset.copyError;
        } finally {
            copyButton.disabled = false;
            copyButton.focus();
            window.setTimeout(() => {
                copyLabel.textContent = defaultLabel;
            }, 3_000);
        }
    });
});
