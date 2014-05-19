var ws = new WebSocket("ws://localhost:8080/ws");

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
			$("#lobby").hide();
			$("#game").removeClass("hidden");	
			break;
    case "ball-move":
    	console.log("ball move" + json.params.x + ":" + json.params.y);
      break;
    case "platform-move":
    	console.log(json.params.side + " move to " + json.params.y);
    	break;
    case "game-end":
      alert(json.params);
      break;
  	case "user-move":
  		break;
		default:
			console.log("Unknown action: " + json.method);
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