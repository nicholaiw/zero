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

        stompClient.subscribe("/type/game/lobby", function (message) {
            const state = JSON.parse(message.body);
            if (!currentUser) {
                currentUser = state.yourName;
                console.log(currentUser);
            }
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
        currentUser: currentUser,
        phase: state.phase.toLowerCase(),
        round: state.round,
        maxRounds: state.maxRounds,
        bet: state.bet,
        players: state.players,
    };
    renderGame();
}

function getMyPlayer() {
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

function onAllIn() {
    stompClient.send("/app/game/" + gameId + "/action", {}, JSON.stringify({ type: "ALL_IN" }));
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
    const me = getMyPlayer();
    const highest = getHighestBet();

    let callDifference = 0;
    if (me) {
        callDifference = highest - me.currentBet;
    }

    let canCall = false;
    if (me && callDifference > 0 && me.balance >= callDifference) {
        canCall = true;
    }

    let canRaise = false;
    if (me && me.balance >= callDifference + 3) {
        canRaise = true;
    }

    let raiseDisabled = "disabled";
    if (isMyTurn && isBetting && canRaise) {
        raiseDisabled = "";
    }

    let callDisabled = "disabled";
    if (isMyTurn && isBetting && canCall) {
        callDisabled = "";
    }

    let allInDisabled = "disabled";
    if (isMyTurn && isBetting) {
        allInDisabled = "";
    }

    let foldDisabled = "disabled";
    if (isMyTurn && isBetting) {
        foldDisabled = "";
    }

    let balanceHTML = "";
    if (me) {
        balanceHTML = `
        <div class="info-block">
            <h3>BALANCE</h3>
            <h1>$${me.balance}</h1>
        </div>`;
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
        ${balanceHTML}
    </div>
    <div class="actions">
        <button class="btn" onclick="onRaise()" ${raiseDisabled}>+$3 RAISE</button>
        <button class="btn" onclick="onCall()" ${callDisabled}>CALL</button>
        <button class="btn" onclick="onAllIn()" ${allInDisabled}>ALL IN</button>
        <button class="btn" onclick="onFold()" ${foldDisabled}>FOLD</button>
    </div>`;

    let allSlotsHTML = "";
    for (let i = 0; i < 4; i++) {
        const player = gameInfo.players[i];

        if (!player) {
            allSlotsHTML += `
            <div class="player">
                <div class="player-name" style="color:#ccc">...</div>
                <div class="cards">
                    <div class="card disabled" style="border-color:#eee"></div>
                    <div class="card disabled" style="border-color:#eee"></div>
                    <div class="card disabled" style="border-color:#eee"></div>
                    <div class="card disabled" style="border-color:#eee"></div>
                </div>
            </div>`;
            continue;
        }

        let statusLabel = "";
        if (player.folded) {
            statusLabel = `<span style="font-size:0.9vw;color:#ccc">folded</span>`;
        } else if (player.allIn) {
            statusLabel = `<span style="font-size:0.9vw;color:#aaa">all in</span>`;
        }

        const isMine = player.name === gameInfo.currentUser;
        let cardsHTML = "";

        for (let j = 0; j < player.cards.length; j++) {
            const card = player.cards[j];
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
                clickHandler = `onclick="onCardClick('${player.name}', ${j})"`;
            }

            cardsHTML += `<div class="${classes}" ${clickHandler}>${card.value}</div>`;
        }

        allSlotsHTML += `
        <div class="player">
            <div class="player-name">${player.name} ${statusLabel}</div>
            <div class="cards">${cardsHTML}</div>
        </div>`;
    }

    document.getElementById("table").innerHTML = allSlotsHTML;
}

window.onload = connect;