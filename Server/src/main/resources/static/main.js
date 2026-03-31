const loginDiv = document.getElementById("login");
const chooseUsernameDiv = document.getElementById("choose-username");
const usernameInput = document.getElementById("username");
const submitUsernameBtn = document.getElementById("submit-username-btn");

document.getElementById("github-login").addEventListener("click", () => {
    window.location.href = "/oauth2/authorization/github";
});

submitUsernameBtn.addEventListener("click", async () => {
    const username = usernameInput.value.trim();
    if (!username) {
        document.getElementById("username-error").textContent = "Enter a username";
        return;
    }

    const res = await fetch("/set-username", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username }),
    });

    if (res.ok) {
        connect();
        showView("game");
    } else {
        document.getElementById("username-error").textContent = await res.text();
    }
});

function showView(view) {
    loginDiv.style.display = view === "login" ? "flex" : "none";
    chooseUsernameDiv.style.display = view === "choose-username" ? "flex" : "none";
    document.getElementById("game").style.display = view === "game" ? "flex" : "none";
}

async function init() {
    const res = await fetch("/me");

    if (res.status === 401) {
        showView("login");
        return;
    }

    const data = await res.json();

    if (data.needsUsername) {
        showView("choose-username");
    } else {
        connect();
        showView("game");
    }
}

init();