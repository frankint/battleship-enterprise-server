const API_URL = 'http://localhost:8080/api/games';
let stompClient = null;
let gameId = null;
let playerId = null;

async function createGame() {
    playerId = document.getElementById('playerId').value;
    const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerId: playerId })
    });
    const game = await response.json();
    gameId = game.gameId;
    document.getElementById('gameIdInput').value = gameId;

    renderGame(game);
    connectWebSocket();
}

async function joinGame() {
    playerId = document.getElementById('playerId').value;
    gameId = document.getElementById('gameIdInput').value;

    const response = await fetch(`${API_URL}/${gameId}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerId: playerId })
    });
    const game = await response.json()

    renderGame(game);
    connectWebSocket();
}

async function placeDefaultShips() {
    const response = await fetch(`${API_URL}/${gameId}/place`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Player-ID': playerId
        },
        body: JSON.stringify({
            shipId: "destroyer", size: 3,
            start: { x: 0, y: 0 }, orientation: "HORIZONTAL"
        })
    });

    if (response.ok) {
        const game = await response.json();
        renderGame(game);
        console.log("Ships placed:", game);
    } else {
        alert("Failed to place ships. Check console.");
    }
}

function connectWebSocket() {
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    // Disable debug logs to keep console clean
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('Connected via WebSockets');
        document.getElementById('gameState').innerText = "CONNECTED";

        stompClient.subscribe(`/topic/game/${gameId}/${playerId}`, function (message) {
            console.log("Received WebSocket Update:", message.body);
            const gameState = JSON.parse(message.body);
            renderGame(gameState);
        });

        stompClient.subscribe(`/topic/game/${gameId}/${playerId}/error`, function (message) {
            const err = JSON.parse(message.body);
            document.getElementById('error-msg').innerText = err.message;
        });
    });
}

function fireShot(x, y) {
    if (!stompClient) {
        alert("Not connected!");
        return;
    }
    stompClient.send(`/app/game/${gameId}/move`,
        { "playerId": playerId },
        JSON.stringify({ target: { x: x, y: y } })
    );
    document.getElementById('error-msg').innerText = "";
}

function renderGame(state) {
    console.log("Rendering State:", state); // Debug log
    document.getElementById('currentTurn').innerText = state.currentTurnPlayerId;

    // Safety check: ensure self/opponent exist before rendering
    if (state.self) renderBoard('my-board', state.self, false);
    if (state.opponent) renderBoard('opponent-board', state.opponent, true);
}

function renderBoard(elementId, playerData, isClickable) {
    const container = document.getElementById(elementId);
    container.innerHTML = '';

    // Grid Loop: y is Row, x is Column
    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 10; x++) {
            const cell = document.createElement('div');
            cell.className = 'cell';

            // Check for Ships (Only visible on MY board)
            // Note: ships array might be null/empty on opponent board
            if (playerData.ships && playerData.ships.length > 0) {
                const hasShip = playerData.ships.some(s =>
                    s.coordinates.some(c => c.x === x && c.y === y)
                );
                if (hasShip) cell.classList.add('ship');
            }

            // Check for Hits/Misses (Works on both boards)
            if (playerData.hits && playerData.hits.some(h => h.x === x && h.y === y)) {
                cell.classList.add('hit');
            }
            else if (playerData.misses && playerData.misses.some(m => m.x === x && m.y === y)) {
                cell.classList.add('miss');
            }

            if (isClickable) {
                cell.onclick = () => fireShot(x, y);
                cell.title = `(${x}, ${y})`; // Tooltip for debugging
            }

            container.appendChild(cell);
        }
    }
}