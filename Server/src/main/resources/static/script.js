let gameInfo = null;
let stompClient = null;
let gameId = null;
let currentUser = null;

function connect() {
    const socket = new SockJS("http://localhost:8080/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        console.log("STOMP connected");

        stompClient.subscribe("/user/queue/game", function (message) {
            const state = JSON.parse(message.body);

            if (state.yourName) currentUser = state.yourName;

            gameId = state.gameId;

            updateGame(state);
        });

        stompClient.send("/app/join", {}, JSON.stringify({}));

    }, function (error) {
        console.log("STOMP error", error);
    });
}

function updateGame(state) {
    showView("game");

    gameInfo = {
        currentTurn: state.currentTurn,
        currentUser: currentUser,
        phase: state.phase ? state.phase.toLowerCase() : "unknown",
        round: state.round,
        maxRounds: state.maxRounds,
        bet: state.bet,
        players: state.players
    };

    if (gameInfo.phase === "finished") {
        resetGame();
        return;
    }

    renderGame();
}

function resetGame() {
    gameInfo = null;
    gameId = null;
    currentUser = null;

    if (stompClient) stompClient.disconnect();
    stompClient = null;

    document.getElementById("find-game-btn").disabled = false;
    document.getElementById("find-game-btn").textContent = "Find Game";
    showView("lobby");
}

function getMyPlayer() {
    if (!gameInfo) return null;
    for (let i = 0; i < gameInfo.players.length; i++) {
        if (gameInfo.players[i].name === gameInfo.currentUser) return gameInfo.players[i];
    }
    return null;
}

function getHighestBet() {
    let highest = 0;
    for (let i = 0; i < gameInfo.players.length; i++) {
        if (gameInfo.players[i].currentBet > highest) highest = gameInfo.players[i].currentBet;
    }
    return highest;
}

function calculatePlayedValue() {
    let total = 0;
    for (let i = 0; i < gameInfo.players.length; i++) {
        const player = gameInfo.players[i];
        for (let j = 0; j < player.cards.length; j++) {
            if (player.cards[j].played) total += player.cards[j].value;
        }
    }
    return total;
}

function sendAction(payload) {
    if (!stompClient || gameId == null) return;
    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify(payload));
}

function onRaise() { sendAction({ type: "RAISE" }); }
function onCall() { sendAction({ type: "CALL" }); }
function onFold() { sendAction({ type: "FOLD" }); }
function onAllIn() { sendAction({ type: "ALL_IN" }); }

function onCardClick(playerName, cardIndex) {
    if (!gameInfo) return;
    if (gameInfo.currentUser !== playerName) return;
    if (gameInfo.currentTurn !== gameInfo.currentUser) return;
    sendAction({ type: "PLAY_CARD", cardIndex: cardIndex });
}

function renderGame() {
    if (!gameInfo || !gameInfo.currentUser) return;

    const me = getMyPlayer();
    const highest = getHighestBet();
    const playedValue = calculatePlayedValue();

    const isMyTurn = gameInfo.currentTurn === gameInfo.currentUser;
    const isBetting = gameInfo.phase === "betting";
    const isWaiting = gameInfo.phase === "waiting";

    let callDifference = me ? highest - me.currentBet : 0;
    let canCall = me && me.balance >= callDifference;
    let canRaise = me && me.balance >= callDifference + 3;

    let raiseDisabled = "disabled";
    let callDisabled = "disabled";
    let allInDisabled = "disabled";
    let foldDisabled = "disabled";

    if (isMyTurn && isBetting) {
        if (canRaise) raiseDisabled = "";
        if (canCall) callDisabled = "";
        allInDisabled = "";
        foldDisabled = "";
    }

    const balanceHTML = me
        ? '<div class="info-block"><h3>BALANCE</h3><h1>$' + me.balance + "</h1></div>"
        : "";

    document.getElementById("info").innerHTML =
        '<div class="info-blocks">' +
        '<div class="info-block"><h3>ROUND</h3><h1>' + gameInfo.round + " / " + gameInfo.maxRounds + "</h1></div>" +
        '<div class="info-block"><h3>BET</h3><h1>$' + gameInfo.bet + "</h1></div>" +
        '<div class="info-block"><h3>VALUE</h3><h1>' + playedValue + " / 9</h1></div>" +
        balanceHTML +
        "</div>" +
        '<div class="actions">' +
        '<button class="btn" onclick="onRaise()" ' + raiseDisabled + '>+3 RAISE</button>' +
        '<button class="btn" onclick="onCall()" ' + callDisabled + '>CALL</button>' +
        '<button class="btn" onclick="onAllIn()" ' + allInDisabled + '>ALL IN</button>' +
        '<button class="btn" onclick="onFold()" ' + foldDisabled + '>FOLD</button>' +
        "</div>";

    let allSlotsHTML = "";

    for (let i = 0; i < 4; i++) {
        const player = gameInfo.players[i];

        if (!player) {
            allSlotsHTML +=
                '<div class="player">' +
                '<div class="player-name" style="color:#ccc">...</div>' +
                '<div class="cards">' +
                '<div class="card disabled"></div>'.repeat(4) +
                '</div>' +
                '<div class="player-status"></div>' +
                '</div>';
            continue;
        }

        const isMine = player.name === gameInfo.currentUser;

        let statusLabel = "";
        if (player.folded) statusLabel = '<span style="color:#ccc">folded</span>';
        else if (player.allIn) statusLabel = '<span style="color:#aaa">all in</span>';

        let cardsHTML = "";

        for (let j = 0; j < player.cards.length; j++) {
            const card = player.cards[j];
            let classes = "card";
            let clickHandler = "";
            const display = card.value === -1 ? "?" : card.value;

            if (card.played) classes += " played";
            if (isMine) classes += " mine";

            if (isBetting || isWaiting) {
                classes += " disabled";
            } else if (isMine && isMyTurn && !card.played) {
                classes += " clickable";
                clickHandler = 'onclick="onCardClick(\'' + player.name + "', " + j + ')"';
            }

            cardsHTML += '<div class="' + classes + '" ' + clickHandler + ">" + display + "</div>";
        }

        allSlotsHTML +=
            '<div class="player">' +
            '<div class="player-name' + (isMine ? " my-player" : "") + '">' + player.name + '</div>' +
            '<div class="cards">' + cardsHTML + '</div>' +
            '<div class="player-status">' + statusLabel + '</div>' +
            '</div>';
    }

    document.getElementById("table").innerHTML = allSlotsHTML;
}