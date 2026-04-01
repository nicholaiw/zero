const loginDiv = document.getElementById("login");
const chooseUsernameDiv = document.getElementById("choose-username");
const lobbyDiv = document.getElementById("lobby");
const gameDiv = document.getElementById("game");

function showView(view) {
    loginDiv.style.display = "none";
    chooseUsernameDiv.style.display = "none";
    lobbyDiv.style.display = "none";
    gameDiv.style.display = "none";

    if (view === "login") loginDiv.style.display = "flex";
    if (view === "choose-username") chooseUsernameDiv.style.display = "flex";
    if (view === "lobby") lobbyDiv.style.display = "flex";
    if (view === "game") gameDiv.style.display = "flex";
}

document.getElementById("github-login").addEventListener("click", () => {
    window.location.href = "/oauth2/authorization/github";
});

document.getElementById("submit-username-btn").addEventListener("click", async () => {
    const username = document.getElementById("username").value.trim();
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
        document.getElementById("lobby-name").textContent = username;
        showView("lobby");
    } else {
        document.getElementById("username-error").textContent = await res.text();
    }
});

document.getElementById("find-game-btn").addEventListener("click", () => {
    document.getElementById("find-game-btn").disabled = true;
    document.getElementById("find-game-btn").textContent = "Searching...";
    connect();
});

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
        document.getElementById("lobby-name").textContent = data.username;
        showView("lobby");
    }
}

init();