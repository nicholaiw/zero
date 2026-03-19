function showView(id) {
    document.getElementById("lobby").style.display = "none";
    document.getElementById("game").style.display = "none";
    document.getElementById(id).style.display = "flex";
}

function onJoinClick() {
    connect();
}

window.onload = function () {
    showView("lobby");
};