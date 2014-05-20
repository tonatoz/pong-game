var leftY = 80;
var rightY = 80;
var ball = {x:250, y:200};

var canvas = document.getElementById("myCanvas");
var ctx = canvas.getContext("2d");

function circle(x,y,r) {
  ctx.beginPath();
  ctx.arc(x, y, r, 0, Math.PI*2, true);
  ctx.fillStyle = "black";
  ctx.fill();
}

function rect(x,y,w,h) {
  ctx.beginPath();
  ctx.rect(x,y,w,h);
  ctx.closePath();
  ctx.fillStyle = "red";
  ctx.fill();
  ctx.stroke();
}

function clear() {
  ctx.clearRect(0, 0, 500, 400);
}

function drawField(){
  clear();
  rect(0, leftY, 5, 75);
  rect(495, rightY, 5, 75);
  circle(ball.x, ball.y, 10);
}

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
      drawField();
			break;
    case "ball-move":
      ball.x = json.params.x;
      ball.y = json.params.y;
      drawField();
      break;
    case "platform-move":
      if (json.params.side == "left") 
        leftY = json.params.y;      
      else
        rightY = json.params.y;
      drawField();
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

$(document).ready(function() {
  $("#myCanvas").mousemove(function(evt){
    var mouseY = evt.pageY - canvas.getBoundingClientRect().top;
    ws.send(JSON.stringify({
      method: "user-move",
      pos: mouseY
    })); 
  });

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