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

            console.log("RAW STATE:", state);

            if (state.yourName) {
                currentUser = state.yourName;
            }

            gameId = state.gameId;

            console.log("gameId:", gameId);

            if (gameId == null) {
                console.error("Missing gameId from server!", state);
            }

            console.log("currentUser:", currentUser);

            updateGame(state);
        });

        stompClient.send("/app/join", {}, JSON.stringify({}));

    }, function (error) {
        console.log("STOMP error", error);
    });
}

function updateGame(state) {
    if (!gameInfo) showView("game");

    gameInfo = {
        currentTurn: state.currentTurn,
        currentUser: currentUser,
        phase: state.phase ? state.phase.toLowerCase() : "unknown",
        round: state.round,
        maxRounds: state.maxRounds,
        bet: state.bet,
        players: state.players
    };

    console.log("GAME INFO:", gameInfo);

    if (gameInfo.phase === "finished") {
        resetGame();
        showView("lobby");
        return;
    }

    renderGame();
}

function resetGame() {
    console.log("RESET GAME");

    gameInfo = null;
    gameId = null;
    currentUser = null;

    if (stompClient) {
        stompClient.disconnect();
    }

    stompClient = null;
}

function getMyPlayer() {
    if (!gameInfo) return null;

    for (let i = 0; i < gameInfo.players.length; i++) {
        if (gameInfo.players[i].name === gameInfo.currentUser) {
            return gameInfo.players[i];
        }
    }

    return null;
}

function getHighestBet() {
    let highest = 0;

    for (let i = 0; i < gameInfo.players.length; i++) {
        if (gameInfo.players[i].currentBet > highest) {
            highest = gameInfo.players[i].currentBet;
        }
    }

    return highest;
}

function calculatePlayedValue() {
    let total = 0;

    for (let i = 0; i < gameInfo.players.length; i++) {
        const player = gameInfo.players[i];

        for (let j = 0; j < player.cards.length; j++) {
            if (player.cards[j].played) {
                total += player.cards[j].value;
            }
        }
    }

    return total;
}

function sendAction(payload) {
    console.log("SENDING ACTION:", payload);

    if (!stompClient) {
        console.log("BLOCKED: stompClient missing");
        return;
    }

    if (gameId == null) {
        console.log("BLOCKED: gameId missing");
        return;
    }

    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify(payload));
}

function onRaise() { sendAction({ type: "RAISE" }); }
function onCall() { sendAction({ type: "CALL" }); }
function onFold() { sendAction({ type: "FOLD" }); }
function onAllIn() { sendAction({ type: "ALL_IN" }); }

function onCardClick(playerName, cardIndex) {
    if (!gameInfo) return;

    if (gameInfo.currentUser !== playerName) {
        console.log("BLOCKED: not your card");
        return;
    }

    if (gameInfo.currentTurn !== gameInfo.currentUser) {
        console.log("BLOCKED: not your turn");
        return;
    }

    sendAction({ type: "PLAY_CARD", cardIndex: cardIndex });
}

function renderGame() {
    if (!gameInfo || !gameInfo.currentUser) {
        console.log("RENDER BLOCKED: missing gameInfo/currentUser");
        return;
    }

    const me = getMyPlayer();
    console.log("ME:", me);

    const highest = getHighestBet();
    const playedValue = calculatePlayedValue();

    const isMyTurn = gameInfo.currentTurn === gameInfo.currentUser;
    const isBetting = gameInfo.phase === "betting";
    const isPlaying = gameInfo.phase === "playing";
    const isWaiting = gameInfo.phase === "waiting";

    console.log({
        isMyTurn,
        isBetting,
        phase: gameInfo.phase,
        currentTurn: gameInfo.currentTurn,
        currentUser: gameInfo.currentUser
    });

    let callDifference = 0;

    if (me) {
        callDifference = highest - me.currentBet;
    }

    let canCall = false;
    let canRaise = false;

    if (me) {
        if (me.balance >= callDifference) {
            canCall = true;
        }

        if (me.balance >= callDifference + 3) {
            canRaise = true;
        }
    }

    let raiseDisabled = "disabled";
    let callDisabled = "disabled";
    let allInDisabled = "disabled";
    let foldDisabled = "disabled";

    if (isMyTurn && isBetting) {
        if (canRaise) raiseDisabled = "";
        if (canCall) callDisabled = "";
        allInDisabled = "";
        foldDisabled = "";
    } else {
        console.log("BUTTONS DISABLED:", {
            isMyTurn,
            isBetting,
            canCall,
            canRaise
        });
    }

    let balanceHTML = "";

    if (me) {
        balanceHTML =
            '<div class="info-block">' +
            '<h3>BALANCE</h3>' +
            '<h1>$' + me.balance + "</h1>" +
            "</div>";
    }

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

        if (player.folded) {
            statusLabel = '<span style="color:#ccc">folded</span>';
        } else if (player.allIn) {
            statusLabel = '<span style="color:#aaa">all in</span>';
        }

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