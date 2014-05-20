// window.requestAnimFrame = (function(){
//   return  window.requestAnimationFrame       || 
//     window.webkitRequestAnimationFrame || 
//     window.mozRequestAnimationFrame    || 
//     window.oRequestAnimationFrame      || 
//     window.msRequestAnimationFrame     ||  
//     function( callback ){
//       return window.setTimeout(callback, 100 / 60);
//     };
// })();

// window.cancelRequestAnimFrame = ( function() {
//   return window.cancelAnimationFrame          ||
//     window.webkitCancelRequestAnimationFrame    ||
//     window.mozCancelRequestAnimationFrame       ||
//     window.oCancelRequestAnimationFrame     ||
//     window.msCancelRequestAnimationFrame        ||
//     clearTimeout
// } )();

var canvas = document.getElementById("canvas"),
  ctx = canvas.getContext("2d"), 
  W = 729, 
  H = 537;

function paintCanvas() {
  ctx.fillStyle = "black";
  ctx.fillRect(0, 0, W, H);
}

function Paddle(pos) {
  return {
    w: 5,
    h: 150,      
    x: (pos == "left") ? 0 : W - 5,
    y: W/2 - 5/2,

    draw: function() {
      ctx.fillStyle = "white";
      ctx.fillRect(this.x, this.y, this.w, this.h);
    }}
}

ball = {
  x: 50,
  y: 50, 
  r: 5,
  
  draw: function() {
    ctx.beginPath();
    ctx.fillStyle = "white";
    ctx.arc(this.x, this.y, this.r, 0, Math.PI*2, false);
    ctx.fill();
  }
};

left = Paddle("left");   
right =  Paddle("right");

function draw() {    
  paintCanvas();
  left.draw();
  right.draw();
  ball.draw();
}

var ws = new WebSocket("ws://localhost:8080/ws");

ws.onclose = function() { 
	alert("Connection closed...");
  window.location.replace("http://localhost:8080/");
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
      draw();
			break;
    case "ball-move":
      ball.x = json.params.x;
      ball.y = json.params.y;
      draw();
      break;
    case "platform-move":
      if (json.params.side == "left") 
        left.y = json.params.y;      
      else
        right.y = json.params.y;
      draw();
    	break;
    case "game-end":
      alert(json.params);
      // window.location.replace("http://localhost:8080/");
      break;
  	case "user-move":
  		break;
		default:
			console.log("Unknown action: " + json.method);
	}
};

$(document).ready(function() {
  $("#canvas").mousemove(function(evt){
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