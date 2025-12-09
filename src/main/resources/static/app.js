const API_URL = 'http://localhost:8080/api';
const AUTH_URL = 'http://localhost:8080/auth';

let stompClient = null;
let currentUser = null;
let authHeader = null;
let currentGameId = null;
let selectedShipType = null;
let isRegisterMode = false;

// ================= INITIALIZATION =================
// Run this when the script loads to check for existing session
document.addEventListener("DOMContentLoaded", () => {
    const savedUser = localStorage.getItem("battleship_user");
    const savedPass = localStorage.getItem("battleship_token"); // We store the base64 string

    if (savedUser && savedPass) {
        // Auto-login
        currentUser = savedUser;
        authHeader = `Basic ${savedPass}`;
        // Validate token by fetching history
        fetch(`${API_URL}/games`, { headers: { 'Authorization': authHeader } })
            .then(response => {
                if (response.ok) {
                    document.getElementById('display-user').innerText = currentUser;
                    showScreen('lobby-screen');
                    response.json().then(loadHistory);
                } else {
                    // Token invalid/expired
                    logout();
                }
            })
            .catch(() => logout()); // Server down
    }
});

// ================= AUTHENTICATION =================

function toggleAuthMode() {
    isRegisterMode = !isRegisterMode;
    const title = document.getElementById('auth-title');
    const btn = document.getElementById('btn-action');
    const toggleText = document.getElementById('toggle-text');
    const toggleLink = document.querySelector('.toggle-link a');
    const msg = document.getElementById('auth-msg');

    msg.innerText = ""; // Clear errors

    if (isRegisterMode) {
        title.innerText = "Register";
        btn.innerText = "Create Account";
        toggleText.innerText = "Have an account?";
        toggleLink.innerText = "Login here";
    } else {
        title.innerText = "Login";
        btn.innerText = "Login";
        toggleText.innerText = "Need an account?";
        toggleLink.innerText = "Click here";
    }
}

async function handleAuth() {
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    const msg = document.getElementById('auth-msg');

    if (!user || !pass) {
        msg.innerText = "Please enter username and password";
        return;
    }

    msg.innerText = "Processing...";

    try {
        if (isRegisterMode) {
            // 1. Register Request
            const regResponse = await fetch(`${AUTH_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: user, password: pass })
            });

            if (!regResponse.ok) {
                throw new Error("Username already taken or invalid.");
            }
            // If register success, fall through to login logic
        }

        // 2. Create Credentials
        // Note: In a real app, storing password even in base64 in localStorage is risky.
        // JWT is better, but Basic Auth is fine for this CV project scope.
        const token = btoa(user + ":" + pass);
        const header = `Basic ${token}`;

        // 3. Verify Credentials by fetching data
        const loginResponse = await fetch(`${API_URL}/games`, {
            headers: { 'Authorization': header }
        });

        if (loginResponse.ok) {
            // Success! Save to Session
            localStorage.setItem("battleship_user", user);
            localStorage.setItem("battleship_token", token);

            currentUser = user;
            authHeader = header;
            document.getElementById('display-user').innerText = user;

            showScreen('lobby-screen');
            loadHistory(await loginResponse.json());
            msg.innerText = "";
        } else {
            throw new Error("Invalid username or password.");
        }

    } catch (err) {
        msg.innerText = err.message;
    }
}

function logout() {
    localStorage.removeItem("battleship_user");
    localStorage.removeItem("battleship_token");
    currentUser = null;
    authHeader = null;
    location.reload(); // Refresh to clear state
}

// ================= LOBBY =================
async function createGame() {
    const response = await fetch(`${API_URL}/games`, {
        method: 'POST',
        headers: { 'Authorization': authHeader }
    });
    const game = await response.json();
    enterGame(game);
}

async function joinGame(id) {
    const gameId = id || document.getElementById('gameIdInput').value;
    if(!gameId) return alert("Enter a game ID");

    const response = await fetch(`${API_URL}/games/${gameId}/join`, {
        method: 'POST',
        headers: { 'Authorization': authHeader }
    });
    if (response.ok) enterGame(await response.json());
    else alert("Could not join game (Full or Finished)");
}

function loadHistory(games) {
    const list = document.getElementById('games-list');
    list.innerHTML = '';
    if(games.length === 0) {
        list.innerHTML = '<li>No games played yet.</li>';
        return;
    }
    games.forEach(g => {
        const li = document.createElement('li');
        li.innerHTML = `<span>Vs: ${getOpponentName(g)} <small>(${g.state})</small></span>`;
        const btn = document.createElement('button');
        btn.innerText = "Open";
        btn.onclick = () => joinGame(g.gameId); // Re-join handles "opening" existing games
        li.appendChild(btn);
        list.appendChild(li);
    });
}

function getOpponentName(game) {
    if (game.self.playerId === currentUser) {
        return game.opponent ? game.opponent.playerId : "Waiting...";
    }
    return game.self.playerId;
}

// ================= GAME LOGIC =================
function enterGame(game) {
    currentGameId = game.gameId;
    document.getElementById('display-game-id').innerText = game.gameId;
    showScreen('game-screen');
    renderGame(game);
    connectWebSocket();
}

function leaveGame() {
    if (stompClient) stompClient.disconnect();
    // Refresh history
    fetch(`${API_URL}/games`, { headers: { 'Authorization': authHeader } })
        .then(r => r.json())
        .then(loadHistory);
    showScreen('lobby-screen');
}

function connectWebSocket() {
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        stompClient.subscribe(`/topic/game/${currentGameId}/${currentUser}`, function (msg) {
            renderGame(JSON.parse(msg.body));
        });
        stompClient.subscribe(`/topic/game/${currentGameId}/${currentUser}/error`, function (msg) {
            showError(JSON.parse(msg.body).message);
        });
    });
}

// ================= RENDERING =================
const ALL_SHIPS = [
    { id: "Carrier", size: 5 }, { id: "Battleship", size: 4 },
    { id: "Cruiser", size: 3 }, { id: "Submarine", size: 3 },
    { id: "Destroyer", size: 2 }
];

function renderGame(state) {
    document.getElementById('game-state').innerText = state.state;

    // Turn Indicator
    const turnSpan = document.getElementById('turn-indicator');
    if(state.state === 'ACTIVE') {
        if(state.currentTurnPlayerId === currentUser) {
            turnSpan.innerText = " (YOUR TURN)";
            turnSpan.style.color = "#2ecc71";
        } else {
            turnSpan.innerText = " (ENEMY TURN)";
            turnSpan.style.color = "#e74c3c";
        }
    } else {
        turnSpan.innerText = "";
    }

    const setupControls = document.getElementById('setup-controls');
    if (state.state === 'SETUP' || state.state === 'WAITING_FOR_PLAYER') {
        setupControls.classList.remove('hidden');
        renderShipYard(state.self.ships);
    } else {
        setupControls.classList.add('hidden');
    }

    renderBoard('my-board', state.self, false);
    renderBoard('opponent-board', state.opponent, true);

    // Check Win/Loss
    if (state.state === 'FINISHED') {
        if (state.winnerId === currentUser) alert("VICTORY!");
        else alert("DEFEAT!");
    }
}

function renderShipYard(placedShips) {
    const container = document.getElementById('ship-yard');
    container.innerHTML = '';

    ALL_SHIPS.forEach(ship => {
        const btn = document.createElement('button');
        btn.className = 'ship-btn';
        btn.innerText = `${ship.id} (${ship.size})`;

        const isPlaced = placedShips.some(s => s.id === ship.id);
        if (isPlaced) btn.disabled = true;

        btn.onclick = () => {
            document.querySelectorAll('.ship-btn').forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            selectedShipType = ship.id;
        };
        container.appendChild(btn);
    });
}

function handleGridClick(isOpponent, x, y) {
    if (isOpponent) {
        if(!stompClient) return;
        stompClient.send(`/app/game/${currentGameId}/move`,
            { "playerId": currentUser },
            JSON.stringify({ target: { x, y } })
        );
    } else {
        if (!selectedShipType) return;
        const orientation = document.querySelector('input[name="orient"]:checked').value;

        fetch(`${API_URL}/games/${currentGameId}/place`, {
            method: 'POST',
            headers: {
                'Authorization': authHeader,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                shipType: selectedShipType,
                start: { x, y },
                orientation: orientation
            })
        }).then(async r => {
            if (r.ok) {
                renderGame(await r.json());
                selectedShipType = null;
                // Auto-select next ship? Optional
            } else {
                showError("Invalid Placement (Overlap or Bounds)");
            }
        });
    }
}

function renderBoard(elementId, playerData, isOpponent) {
    const container = document.getElementById(elementId);
    container.innerHTML = '';

    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 10; x++) {
            const cell = document.createElement('div');
            cell.className = 'cell';

            if (playerData) {
                // Ships
                if (!isOpponent && playerData.ships.some(s => s.coordinates.some(c => c.x===x && c.y===y))) {
                    cell.classList.add('ship');
                }
                // Hits/Misses (Check null safety)
                if (playerData.hits && playerData.hits.some(c => c.x===x && c.y===y)) cell.classList.add('hit');
                if (playerData.misses && playerData.misses.some(c => c.x===x && c.y===y)) cell.classList.add('miss');
            }

            cell.onclick = () => handleGridClick(isOpponent, x, y);
            container.appendChild(cell);
        }
    }
}

function showScreen(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.add('hidden'));
    document.getElementById(id).classList.remove('hidden');
}

function showError(msg) {
    const el = document.getElementById('error-msg');
    el.innerText = msg;
    el.classList.remove('hidden');
    setTimeout(() => el.classList.add('hidden'), 3000);
}