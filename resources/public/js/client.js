var ws = new WebSocket("ws://localhost:8080/ws");
var game_id = "";

ws.onclose = function() { 
	alert("Connection closed...") 
}; 

ws.onmessage = function(evt) { 
	var json = jQuery.parseJSON(evt.data);
	switch(json.method) {
		case "signin":
			if (json.result == "ok"){
				$("#signin").hide();
				$("#lobby").removeClass("hidden");
			}
			else {
				alert("такое имя уже занято");
			}
			break;
		case "add-user":
			$("#lobby table tbody").append(
				"<tr data-id='" + 
			  json.id +
			  "'><td>" + 
			  json.name + 
			  "</td><td><button type='button' class='fight-btn btn btn-primary'>Fight</button></td></tr>");
			break;	
		case "rem-user":
			$("#lobby table tbody tr[data-id='" + json.id + "']").remove();
			break;
		case "invate-fight":
			if (json.result == "error") {			
				alert("bad fight");
			}
			break;
		case "fight":
      game_id = json.id;
			$("#lobby").hide();
			$("#game").removeClass("hidden");	
			break;
    case "ball-move":
      break;
    case "game-end":
      alert(json.params);
      break;
		default:
			console.log("Unknown action: " + json);
	}
};

function getMousePos(canvas, evt) {
  var rect = canvas.getBoundingClientRect();
  return {
    x: evt.clientX - rect.left,
    y: evt.clientY - rect.top
  };
}

var canvas = document.getElementById('myCanvas');
var context = canvas.getContext('2d');

canvas.addEventListener('mousemove', function(evt) {
  var mousePos = getMousePos(canvas, evt);
  var message = 'Mouse position: ' + mousePos.x + ',' + mousePos.y;
  ws.send(JSON.stringify({
  	method: "user-move",
    id: game_id,
  	pos: mousePos.y
  }));
}, false);

$(document).ready(function() {
  $("#signin form").submit(function(event){
  	event.preventDefault();
    ws.send(JSON.stringify({
    	method: "signin",
    	login: $("#signin form input[name='username']").val()
    }));    
  });

  $("body").on('click', ".fight-btn", function() {
		var id = $(this).parent().parent().data("id");
		ws.send(JSON.stringify({
			method: "invate-fight",
			opponent: id
		}));
	});
});