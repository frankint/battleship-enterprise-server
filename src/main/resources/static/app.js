const HOST = window.location.origin;

const API_URL = `${HOST}/api`;
const AUTH_URL = `${HOST}/auth`;
const WS_URL = `${HOST}/ws`;

let stompClient = null;
let currentUser = null;
let authHeader = null;
let currentGameId = null;
let selectedShipType = null;
let isRegisterMode = false;
let pendingPlacement = null; // Stores { x, y, type, orientation }
let lastKnownState = null;
let gameSub = null;  // Stores the game state subscription
let errorSub = null; // Stores the error subscription
let lastSunkCount = 0;

// ================= INITIALIZATION =================
// Run this when the script loads to check for existing session
document.addEventListener("DOMContentLoaded", () => {
    // Add Enter key listeners for login
    const inputs = [document.getElementById('username'), document.getElementById('password')];
    inputs.forEach(input => {
        if(input) {
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') handleAuth();
            });
        }
    });

    // Check LocalStorage (Registered) OR SessionStorage (Guest)
    let savedUser = localStorage.getItem("battleship_user");
    let savedPass = localStorage.getItem("battleship_token");

    if (!savedUser) {
        savedUser = sessionStorage.getItem("battleship_user");
        savedPass = sessionStorage.getItem("battleship_token");
    }

    if (savedUser && savedPass) {
        currentUser = savedUser;
        authHeader = `Basic ${savedPass}`;
        // Validate token
        fetch(`${API_URL}/games`, { headers: { 'Authorization': authHeader } })
            .then(response => {
                if (response.ok) {
                    document.getElementById('display-user').innerText = currentUser;
                    showScreen('lobby-screen');
                    response.json().then(loadHistory);
                    loadFriends();
                    connectGlobalSocket();
                } else {
                    logout();
                }
            })
            .catch(() => logout());
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
        title.innerText = "Battleship";
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
            loadFriends();
            connectGlobalSocket();
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
    sessionStorage.removeItem("battleship_user");
    sessionStorage.removeItem("battleship_token");

    currentUser = null;
    authHeader = null;
    location.reload();
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

        // Game Info
        const info = document.createElement('span');
        info.innerHTML = `Vs: <strong>${getOpponentName(g)}</strong> <small>(${g.state})</small>`;

        // Action Container
        const actions = document.createElement('div');
        actions.style.display = 'flex';
        actions.style.gap = '10px';

        // 1. Open Button
        const btnJoin = document.createElement('button');
        btnJoin.innerText = "Open";
        btnJoin.className = "small primary";
        btnJoin.onclick = () => joinGame(g.gameId);

        // 2. Hide Button (NEW)
        const btnHide = document.createElement('button');
        btnHide.innerText = "❌";
        btnHide.title = "Remove from history";
        btnHide.className = "small secondary";
        btnHide.style.backgroundColor = "#ef4444"; // Red
        btnHide.onclick = (e) => {
            e.stopPropagation(); // Prevent triggering other clicks
            hideGame(g.gameId);
        };

        actions.appendChild(btnJoin);
        actions.appendChild(btnHide);

        li.appendChild(info);
        li.appendChild(actions);
        list.appendChild(li);
    });
}

async function hideGame(gameId) {
    if(!confirm("Hide this game from your history?")) return;

    const response = await fetch(`${API_URL}/games/${gameId}/hide`, {
        method: 'POST',
        headers: { 'Authorization': authHeader }
    });

    if (response.ok) {
        // Refresh the list
        fetch(`${API_URL}/games`, { headers: { 'Authorization': authHeader } })
            .then(r => r.json())
            .then(loadHistory);
    } else {
        alert("Failed to hide game.");
    }
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
    lastSunkCount = 0;
    document.getElementById('display-game-id').innerText = game.gameId;
    showScreen('game-screen');
    renderGame(game);
    subscribeToGame(currentGameId);
}

function leaveGame() {
    // Do NOT disconnect. Just unsubscribe from the game.
    if (gameSub) gameSub.unsubscribe();
    if (errorSub) errorSub.unsubscribe();

    gameSub = null;
    errorSub = null;
    currentGameId = null;

    // Refresh data
    fetch(`${API_URL}/games`, { headers: { 'Authorization': authHeader } })
        .then(r => r.json())
        .then(loadHistory);

    showScreen('lobby-screen');
}

function connectWebSocket() {
    // Prevent double connection
    if (stompClient && stompClient.connected) return;

    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        // ALWAYS subscribe to personal notifications
        stompClient.subscribe(`/topic/user/${currentUser}/notifications`, function (msg) {
            const notif = JSON.parse(msg.body);
            if (notif.type === 'CHALLENGE') {
                handleIncomingChallenge(notif);
            }
        });

        // IF inside a game, subscribe to game updates
        if (currentGameId) {
            stompClient.subscribe(`/topic/game/${currentGameId}/${currentUser}`, function (msg) {
                renderGame(JSON.parse(msg.body));
            });
            stompClient.subscribe(`/topic/game/${currentGameId}/${currentUser}/error`, function (msg) {
                showError(JSON.parse(msg.body).message);
            });
        }
    });
}
// ================= RENDERING =================
const ALL_SHIPS = [
    { id: "Carrier", size: 5 }, { id: "Battleship", size: 4 },
    { id: "Cruiser", size: 3 }, { id: "Submarine", size: 3 },
    { id: "Destroyer", size: 2 }
];

function renderGame(state) {
    lastKnownState = state;

    document.getElementById('game-state').innerText = state.state;

    // Turn Indicator Logic
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

    if (state.opponent && state.opponent.sunkShips) {
        const currentCount = state.opponent.sunkShips.length;
        if (currentCount > lastSunkCount) {
            // We have a new sunk ship!
            // Find which one is new (optional, but nice)
            // Simple approach: Just alert
            showError("ENEMY SHIP SUNK!");
            // Note: showError uses the red toast, which is perfect for this.
        }
        lastSunkCount = currentCount;
    }

    // Setup Controls Logic
    const setupControls = document.getElementById('setup-controls');
    const shipYard = document.getElementById('ship-yard');
    const actions = document.getElementById('placement-actions');

    if (state.state === 'SETUP' || state.state === 'WAITING_FOR_PLAYER') {
        setupControls.classList.remove('hidden');
        if (pendingPlacement) {
            shipYard.classList.add('hidden');
            actions.classList.remove('hidden');
        } else {
            shipYard.classList.remove('hidden');
            actions.classList.add('hidden');
            renderShipYard(state.self.ships);
        }
    } else {
        setupControls.classList.add('hidden');
    }

    renderBoard('my-board', state.self, false);
    renderBoard('opponent-board', state.opponent, true);

    // Game Over Alert
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

        // Highlight if currently selected
        if (selectedShipType === ship.id) {
            btn.classList.add('selected');
        }

        btn.onclick = () => {
            selectedShipType = ship.id;

            // This attaches the 'onmouseenter' events to the grid cells
            // because selectedShipType is no longer null.
            renderGame(lastKnownState);
        };
        container.appendChild(btn);
    });
}

function handleGridClick(isOpponent, x, y) {
    if (isOpponent) {
        // ... existing shooting logic ...
        if(!stompClient) return;
        stompClient.send(`/app/game/${currentGameId}/move`,
            { "playerId": currentUser },
            JSON.stringify({ target: { x, y } })
        );
    } else {

        // 1. Ignore click if we already have a pending placement (must resolve first)
        if (pendingPlacement) return;

        // 2. Ignore if no ship selected
        if (!selectedShipType) return;

        // 3. Store the intent
        const orientation = document.querySelector('input[name="orient"]:checked').value;

        pendingPlacement = {
            x: x,
            y: y,
            shipType: selectedShipType,
            orientation: orientation
        };

        // 4. Trigger re-render to show the "Pending" yellow ship
        // We pass the current game state back into renderGame to refresh the view
        // (We need to store the last known state globally to do this cleanly)
        renderGame(lastKnownState);
    }
}

function renderBoard(elementId, playerData, isOpponent) {
    const container = document.getElementById(elementId);
    container.innerHTML = '';

    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 10; x++) {
            const cell = document.createElement('div');
            cell.className = 'cell';

            let alreadyShot = false;

            if (playerData) {
                // 1. Render Standard Ships (My ships OR Game Over revealed ships)
                if (playerData.ships && playerData.ships.some(s => s.coordinates.some(c => c.x===x && c.y===y))) {
                    if (isOpponent) {
                        cell.classList.add('ship-revealed'); // Grey for Game Over reveal
                    } else {
                        cell.classList.add('ship'); // Standard grey for me
                    }
                }

                // 2. ISSUE 21: Render Sunk Ships (Opponent only)
                if (isOpponent && playerData.sunkShips && playerData.sunkShips.some(s => s.coordinates.some(c => c.x===x && c.y===y))) {
                    cell.classList.remove('ship-revealed'); // Override grey
                    cell.classList.add('ship-sunk'); // Red border
                }

                // 3. Hits
                if (playerData.hits && playerData.hits.some(c => c.x===x && c.y===y)) {
                    cell.classList.add('hit');
                    alreadyShot = true;
                }
                // 4. Misses
                if (playerData.misses && playerData.misses.some(c => c.x===x && c.y===y)) {
                    cell.classList.add('miss');
                    alreadyShot = true;
                }
            }

            // --- PENDING PLACEMENT (My Board) ---
            if (!isOpponent && pendingPlacement) {
                const shipInfo = ALL_SHIPS.find(s => s.id === pendingPlacement.shipType);
                const p = pendingPlacement;
                let isPendingCell = false;
                for(let i=0; i < shipInfo.size; i++) {
                    const px = p.x + (p.orientation === "HORIZONTAL" ? i : 0);
                    const py = p.y + (p.orientation === "VERTICAL" ? i : 0);
                    if (x === px && y === py) isPendingCell = true;
                }
                if (isPendingCell) cell.classList.add('preview-pending');
            }

            // --- CLICK HANDLERS ---
            if (isOpponent) {
                if (alreadyShot) {
                    cell.style.cursor = 'not-allowed';
                    cell.title = "Already fired here";
                } else {
                    cell.onclick = () => handleGridClick(isOpponent, x, y);
                    cell.style.cursor = 'crosshair';
                }
            } else {
                cell.onclick = () => handleGridClick(isOpponent, x, y);
                // Hover Effects
                if (selectedShipType && !pendingPlacement) {
                    cell.onmouseenter = () => handleShipHover(x, y, playerData);
                    cell.onmouseleave = () => clearPreviews();
                }
            }

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

function handleShipHover(x, y, playerData) {
    if (!selectedShipType) return; // Do nothing if no ship selected from yard

    // 1. Find ship size
    const shipInfo = ALL_SHIPS.find(s => s.id === selectedShipType);
    if (!shipInfo) return;

    const size = shipInfo.size;
    const orientation = document.querySelector('input[name="orient"]:checked').value;

    // 2. Calculate target coordinates
    const coords = [];
    let isValid = true;

    for (let i = 0; i < size; i++) {
        const targetX = x + (orientation === "HORIZONTAL" ? i : 0);
        const targetY = y + (orientation === "VERTICAL" ? i : 0);

        // Check Bounds (0-9)
        if (targetX > 9 || targetY > 9) {
            isValid = false;
            // We still add it to coords to show the red overflow,
            // but only if it's within the visible grid
            if (targetX <= 9 && targetY <= 9) {
                coords.push({ x: targetX, y: targetY });
            }
        } else {
            coords.push({ x: targetX, y: targetY });

            // Check Overlap with existing ships
            // (Note: playerData.ships might be null if board is empty)
            if (playerData.ships && playerData.ships.some(s =>
                s.coordinates.some(c => c.x === targetX && c.y === targetY)
            )) {
                isValid = false;
            }
        }
    }

    // 3. Apply CSS Classes
    coords.forEach(c => {
        // Find the specific cell div.
        // We need a reliable way to find the cell.
        // Let's rely on the DOM order: My Board is the first .board in the DOM
        const boardDiv = document.getElementById('my-board');
        const cellIndex = c.y * 10 + c.x;
        const cell = boardDiv.children[cellIndex];

        if (cell) {
            cell.classList.add(isValid ? 'preview-valid' : 'preview-invalid');
        }
    });
}

function clearPreviews() {
    document.querySelectorAll('.preview-valid, .preview-invalid')
        .forEach(el => {
            el.classList.remove('preview-valid');
            el.classList.remove('preview-invalid');
        });
}

function confirmPlacement() {
    if (!pendingPlacement) return;

    const { x, y, shipType, orientation } = pendingPlacement;

    fetch(`${API_URL}/games/${currentGameId}/place`, {
        method: 'POST',
        headers: {
            'Authorization': authHeader,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            shipType: shipType,
            start: { x, y },
            orientation: orientation
        })
    }).then(async r => {
        if (r.ok) {
            const newState = await r.json();

            // 1. Clear current state
            pendingPlacement = null;
            selectedShipType = null;

            // 2. Logic to Auto-Select Next Ship
            const placedShips = newState.self.ships;
            // Find the first ship in ALL_SHIPS that is NOT in placedShips
            const nextShip = ALL_SHIPS.find(s => !placedShips.some(placed => placed.id === s.id));

            if (nextShip) {
                selectedShipType = nextShip.id;
            }

            // 3. Render Game (which will now highlight the new ship)
            renderGame(newState);

            // 4. Clear previews
            clearPreviews();

        } else {
            showError("Invalid Placement (Overlap or Bounds)");
            cancelPlacement();
        }
    });
}

function cancelPlacement() {
    pendingPlacement = null;
    renderGame(lastKnownState);
}

async function handleGuestLogin() {
    const msg = document.getElementById('auth-msg');
    msg.innerText = "Creating guest account...";

    try {
        // 1. Get Credentials from Server
        const response = await fetch(`${AUTH_URL}/guest`, {
            method: 'POST'
        });

        if (!response.ok) throw new Error("Failed to create guest.");

        const creds = await response.json();

        // 2. Log in using those credentials
        const token = btoa(creds.username + ":" + creds.password);
        const header = `Basic ${token}`;

        currentUser = creds.username;
        authHeader = header;

        // 3. Save to SESSION Storage (Ephemeral)
        sessionStorage.setItem("battleship_user", currentUser);
        sessionStorage.setItem("battleship_token", token);

        document.getElementById('display-user').innerText = currentUser;
        showScreen('lobby-screen');
        loadHistory([]); // New guest has no history
        loadFriends();
        connectGlobalSocket();
        msg.innerText = "";

    } catch (e) {
        msg.innerText = e.message;
    }
}
// ================= SOCIAL FEATURES =================
async function addFriend() {
    const input = document.getElementById('friendInput');
    const username = input.value.trim();
    if(!username) return;

    await fetch(`${API_URL}/social/friends`, {
        method: 'POST',
        headers: {
            'Authorization': authHeader,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username: username })
    });

    input.value = '';
    loadFriends(); // Refresh list
}


function loadFriends() {
    fetch(`${API_URL}/social/friends`, { headers: { 'Authorization': authHeader } })
        .then(r => r.json())
        .then(friends => {
            const list = document.getElementById('friends-list');
            list.innerHTML = '';

            // ... (empty check) ...

            friends.forEach(friendName => {
                const li = document.createElement('li');

                const span = document.createElement('span');
                span.className = 'friend-name';
                span.innerText = friendName;

                const btnBox = document.createElement('div');
                btnBox.style.display = 'flex';
                btnBox.style.gap = '5px';

                // Challenge Button
                const btnChal = document.createElement('button');
                btnChal.className = 'icon-btn primary';
                btnChal.innerText = "⚔";
                btnChal.title = "Challenge";
                btnChal.onclick = () => sendInvite(friendName);

                // Remove Button (NEW)
                const btnRem = document.createElement('button');
                btnRem.className = "icon-btn secondary";
                btnRem.style.backgroundColor = '#ef4444';
                btnRem.innerText = "🗑";
                btnRem.title = "Remove Friend";
                btnRem.onclick = () => removeFriend(friendName);

                btnBox.appendChild(btnChal);
                btnBox.appendChild(btnRem);

                li.appendChild(span);
                li.appendChild(btnBox);
                list.appendChild(li);
            });
        });
}

async function removeFriend(username) {
    if(!confirm(`Remove ${username} from friends?`)) return;

    await fetch(`${API_URL}/social/friends/${username}`, {
        method: 'DELETE',
        headers: { 'Authorization': authHeader }
    });
    loadFriends();
}

async function sendInvite(username) {
    if(!confirm(`Challenge ${username} to a game?`)) return;

    const response = await fetch(`${API_URL}/social/invite`, {
        method: 'POST',
        headers: {
            'Authorization': authHeader,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username: username })
    });

    if (response.ok) {
        const gameId = await response.text();
        joinGame(gameId);
    } else {
        // Show the specific error from the server (e.g., "User 'bob' is not online")
        const errorMsg = await response.text();
        alert(errorMsg);
    }
}

function handleIncomingChallenge(notif) {
    if (confirm(notif.message + "\nClick OK to Accept, Cancel to Deny.")) {
        joinGame(notif.gameId);
    } else {
        // Send Decline
        fetch(`${API_URL}/social/invite/decline`, {
            method: 'POST',
            headers: {
                'Authorization': authHeader,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                gameId: notif.gameId,
                challenger: notif.sender
            })
        });
    }
}

// ================= WEBSOCKETS =================

function connectGlobalSocket() {
    // If already connected, do nothing
    if (stompClient && stompClient.connected) return;

    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logs for cleaner console

    stompClient.connect({}, function () {
        console.log("Connected to WebSocket");

        // 1. ALWAYS Subscribe to Personal Notifications (Invites)
        stompClient.subscribe(`/topic/user/${currentUser}/notifications`, function (msg) {
            const notif = JSON.parse(msg.body);

            if (notif.type === 'CHALLENGE') {
                handleIncomingChallenge(notif);
            }
            else if (notif.type === 'DECLINED') {
                alert(notif.message);
                leaveGame(); // Kick them out of the "Waiting" room back to Lobby
            }
        });

        // 2. If we happened to be in a game (e.g. reconnect logic), subscribe now
        if (currentGameId) {
            subscribeToGame(currentGameId);
        }
    });
}

function subscribeToGame(gameId) {
    if (!stompClient || !stompClient.connected) {
        console.error("Socket not connected yet. Waiting...");
        setTimeout(() => subscribeToGame(gameId), 500);
        return;
    }

    // 1. Unsubscribe from previous game if needed
    if (gameSub) gameSub.unsubscribe();
    if (errorSub) errorSub.unsubscribe();

    console.log("Subscribing to Game:", gameId);

    // 2. Subscribe and Store the reference
    gameSub = stompClient.subscribe(`/topic/game/${gameId}/${currentUser}`, function (msg) {
        renderGame(JSON.parse(msg.body));
    });

    errorSub = stompClient.subscribe(`/topic/game/${gameId}/${currentUser}/error`, function (msg) {
        showError(JSON.parse(msg.body).message);
    });
}

function challengeStranger() {
    const input = document.getElementById('friendInput');
    const username = input.value.trim();
    if(!username) return alert("Enter a username to challenge");

    sendInvite(username); // Re-use existing invite logic
}
// ================= KEYBOARD CONTROLS =================

document.addEventListener('keydown', (e) => {
    // Only active if we are in the SETUP phase
    const setupControls = document.getElementById('setup-controls');
    if (setupControls.classList.contains('hidden')) return;

    // Check for 'R' key
    if (e.key.toLowerCase() === 'r') {
        toggleOrientation();
    }
});

function toggleOrientation() {
    const horizBtn = document.querySelector('input[value="HORIZONTAL"]');
    const vertBtn = document.querySelector('input[value="VERTICAL"]');

    if (horizBtn.checked) {
        vertBtn.checked = true;
    } else {
        horizBtn.checked = true;
    }

    // If we have a pending placement (Yellow Ship), update its orientation instantly
    if (pendingPlacement) {
        pendingPlacement.orientation = vertBtn.checked ? "VERTICAL" : "HORIZONTAL";
        renderGame(lastKnownState); // Re-render to show rotation
    }

    // If we just have a ship selected (Green Hover), force a re-render to update hover preview
    if (selectedShipType && !pendingPlacement) {
        renderGame(lastKnownState);
    }
}