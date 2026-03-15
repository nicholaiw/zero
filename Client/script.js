const gameInfo = {
    currentTurn: "Player 1",
    currentUser: "Player 1",
    phase: "playing",
    timeRemaining: 12,
    round: 3,
    maxRounds: 4,
    bet: 30,
    players: [
        {
            name: "Player 1",
            cards: [
                { value: 0, played: false },
                { value: 1, played: false },
                { value: 2, played: false },
                { value: 3, played: true },
            ],
        },
        {
            name: "Player 2",
            cards: [
                { value: 0, played: false    },
                { value: 1, played: false },
                { value: 2, played: false },
                { value: 3, played: true },
            ],
        },
        {
            name: "Player 3",
            cards: [
                { value: 0, played: false },
                { value: 1, played: false },
                { value: 2, played: true },
                { value: 2, played: false },
            ],
        },
        {
            name: "Player 4",
            cards: [
                { value: 2, played: false },
                { value: 0, played: false },
                { value: 1, played: true },
                { value: 1, played: false },
            ],
        },
    ],
};

function calculatePlayedValue() {
    return gameInfo.players.reduce((total, player) => {
        const playerSum = player.cards.reduce((sum, card) => {
            if (card.played) {
                return sum + card.value;
            } else {
                return sum;
            }
        }, 0);
        return total + playerSum;
    }, 0);
}


function onRaise() {
    console.log("raise");
}

function onCall() {
    console.log("call");
}

function onFold() {
    console.log("fold");
}

function onCardClick(playerName, cardIndex) {
    if (gameInfo.currentUser !== playerName) return;
    if (gameInfo.currentTurn !== gameInfo.currentUser) return;
    console.log("card clicked", playerName, cardIndex);
}

function renderGame() {
    const playedValue = calculatePlayedValue();
    const isMyTurn = gameInfo.currentTurn === gameInfo.currentUser;
    const isBetting = gameInfo.phase === "betting";
    const isPlaying = gameInfo.phase === "playing";

    let disabledAttr = "";
    if (!isMyTurn || isPlaying) {
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
    </div>
    `;

    const playersHTML = gameInfo.players
        .map((player) => {
            const isMine = player.name === gameInfo.currentUser;
            const cardsHTML = player.cards
                .map((card, i) => {
                    let classes = "card";
                    let clickHandler = "";
                    if (card.played) {
                        classes += " played";
                    }
                    if (isMine) {
                        classes += " mine";
                    }
                    if (isBetting) {
                        classes += " disabled";
                    } else if (isMine && isMyTurn && !card.played) {
                        clickHandler = `onclick="onCardClick('${player.name}', ${i})"`;
                    }
                    return `<div class="${classes}" ${clickHandler}>${card.value}</div>`;
                })
                .join("");
            return `
    <div class="player">
        <div class="player-name">${player.name}</div>
        <div class="cards">${cardsHTML}</div>
    </div>
            `;
        })
        .join("");

    document.getElementById("table").innerHTML = playersHTML;
}

renderGame();