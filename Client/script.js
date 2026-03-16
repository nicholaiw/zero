let gameInfo = null;
let stompClient = null;
let gameId = null;

function connect() {
    const socket = new SockJS("http://localhost:8080/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        console.log("STOMP connected");

        stompClient.subscribe("/type/game/lobby", function (message) {
            const state = JSON.parse(message.body);
            if (!gameId) {
                gameId = state.gameId;
                stompClient.subscribe("/type/game/" + gameId, function (msg) {
                    updateGame(JSON.parse(msg.body));
                });
            }
            updateGame(state);
        });

        stompClient.send("/app/join", {}, JSON.stringify({}));

    }, function (error) {
        console.log("STOMP error", error);
    });
}

function updateGame(state) {
    gameInfo = {
        currentTurn: state.currentTurn,
        currentUser: "Player",
        phase: state.phase.toLowerCase(),
        round: state.round,
        maxRounds: state.maxRounds,
        bet: state.bet,
        players: state.players,
    };
    renderGame();
}

function calculatePlayedValue() {
    let total = 0;
    for (const player of gameInfo.players) {
        for (const card of player.cards) {
            if (card.played) {
                total += card.value;
            }
        }
    }
    return total;
}

function onRaise() {
    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify({ type: "RAISE" }));
}

function onCall() {
    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify({ type: "CALL" }));
}

function onFold() {
    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify({ type: "FOLD" }));
}

function onCardClick(playerName, cardIndex) {
    if (gameInfo.currentUser !== playerName) return;
    if (gameInfo.currentTurn !== gameInfo.currentUser) return;
    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify({ type: "PLAY_CARD", cardIndex: cardIndex }));
}

function renderGame() {
    if (!gameInfo) return;

    const playedValue = calculatePlayedValue();
    const isMyTurn = gameInfo.currentTurn === gameInfo.currentUser;
    const isBetting = gameInfo.phase === "betting";
    const isPlaying = gameInfo.phase === "playing";
    const isWaiting = gameInfo.phase === "waiting";

    let disabledAttr = "";
    if (!isMyTurn || isPlaying || isWaiting) {
        disabledAttr = "disabled";
    }

    document.getElementById("info").innerHTML = `
    <div class="info-blocks">
        <div class="info-block">
            <h3>ROUND</h3>
            <h1>${gameInfo.round} / ${gameInfo.maxRounds}</h1>
        </div>
        <div class="info-block">
            <h3>BET</h3>
            <h1>$${gameInfo.bet}</h1>
        </div>
        <div class="info-block">
            <h3>VALUE</h3>
            <h1>${playedValue} / 9</h1>
        </div>
    </div>
    <div class="actions">
        <button class="btn" onclick="onRaise()" ${disabledAttr}>+$3 RAISE</button>
        <button class="btn" onclick="onCall()" ${disabledAttr}>CALL</button>
        <button class="btn" onclick="onFold()" ${disabledAttr}>FOLD</button>
    </div>`;

    const allSlots = [0, 1, 2, 3].map(i => {
        const player = gameInfo.players[i];

        if (!player) {
            return `
            <div class="player">
                <div class="player-name" style="color:#ccc">...</div>
                <div class="cards">
                    <div class="card disabled" style="border-color:#eee"></div>
                    <div class="card disabled" style="border-color:#eee"></div>
                    <div class="card disabled" style="border-color:#eee"></div>
                    <div class="card disabled" style="border-color:#eee"></div>
                </div>
            </div>`;
        }

        const isMine = player.name === gameInfo.currentUser;
        let cardsHTML = "";

        for (let i = 0; i < player.cards.length; i++) {
            const card = player.cards[i];
            let classes = "card";
            let clickHandler = "";

            if (card.played) {
                classes += " played";
            }
            if (isMine) {
                classes += " mine";
            }
            if (isBetting || isWaiting) {
                classes += " disabled";
            } else if (isMine && isMyTurn && !card.played) {
                clickHandler = `onclick="onCardClick('${player.name}', ${i})"`;
            }

            cardsHTML += `<div class="${classes}" ${clickHandler}>${card.value}</div>`;
        }

        return `
        <div class="player">
            <div class="player-name">${player.name}</div>
            <div class="cards">${cardsHTML}</div>
        </div>`;
    }).join("");

    document.getElementById("table").innerHTML = allSlots;
}

window.onload = connect;