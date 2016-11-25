/*
Joseph Salomone
660658959
salomj@rpi.edu
chat-server.js
*/

//prevents dialog boxes from being shown
$( "#dialog:ui-dialog" ).dialog( "destroy" );
$( "#jquery-form" ).hide();

//clears the message box
$('#MessageBox').empty();

//UserName, Host, Socket Variable
var UserRequestTime = 500, //Time Between User Requests
	MaxChunkedSize = 999, //Maximum size of a chunk
	MaxNonChunkedSize = 99; //Maximum size of a non-chunk
	CSSMax = 4, //The number of the last css skin
	OnEchoServer = false, //false if using a real server, true if using an echo server
	name = $( "#name" ),
	host = $( "#host" ),
	socket = null,
	Hash = null,
	Users = new Object(),
	UserInterval = null,
	isChunking = false;
Users.COUNT = 0;
Users.MAXCOUNT = 16;

//checks if a user has WebSockets
if (!window.WebSocket)
{
	$('#MessageBox').append('<p>Your web browser does not support WebSockets. Please either enable WebSockets for your current broswer or use a browser which has WebSockets.</p>');
	alert('Your web browser does not support WebSockets. Please either enable WebSockets for your current broswer or use a browser which has WebSockets.');
}
else
{
	if(window.opener != null)
	{
		Users = window.opener.Users;
		
		//handles regular pop ups
		if(Users)
		{
			var CSSURL = $('#PageCSS').attr('href');
			CSSURL = CSSURL.substring(0, CSSURL.lastIndexOf("/") + 1) + "private.css";
			$('#PageCSS').attr('href', CSSURL);
			
			Hash = new Object();
			name = window.opener.$( "#name" );
			host = window.opener.$( "#host" );
			
			//window.location.hash is dumb in firefox, because firefox autodecodes it
			//var HashSplit = window.location.href.substring(window.location.href.lastIndexOf("/") + 1);
			var HashSplit = window.location.href.substring(window.location.href.indexOf("#") + 1).split("&");

			//gets the entire hash tag and stores it in an object
			var i;
			for(i=0; i < HashSplit.length; i++)
			{
				var temp = HashSplit[i].split("=", 1)[0];
				if(temp.length == HashSplit[i].length)
				{
					Hash[decodeURIComponent(temp)] = null;
				}
				else
				{
					Hash[decodeURIComponent(temp)] = decodeURIComponent(HashSplit[i].substring(temp.length + 1));
				}
			}
			
			//make sure this is a valid private window
			if(!Hash['User'])
			{
				window.close();
			}
			else
			{
				$("#Send").children("h1").text("Send a private message to \"" + Hash['User'] + "\" (to send, hit ENTER or click SEND)");
				
				//If there was a message, then display it
				if(Hash['Message'] && Hash['Message'].length > 0)
				{
					//Came from other user
					if(Hash['From'] == "0")
					{
						DisplayMessage(Hash['User'], Hash['Message'], false);
					}
					//Came from me
					else
					{
						DisplayMessage(name.val(), Hash['Message'], false);
					}
				}
			}
			
			//changes the css index to match primary page
			if(Hash['CSS'])
			{
				SetCSSIndex(Hash['CSS']);
			}
		}
	}
	if(Hash == null)
	{
		//user needs to authenticate
		OpenLogin();
	}
	window.location.hash = "";
}

//if haystack begins with needle
function beginsWith(needle, haystack)
{
	if(haystack.substr(0, needle.length) == needle)
	{
		return true;
	}
	return false;
}

//opens the log in dialog box
function OpenLogin()
{
	//contains a list of all the fields
	var allFields = $( [] ).add( name ).add( host ),
		tips = $("#login-form").children( "p.validateTips" );

	//Updates the log in tip box
	function updateTips( t )
	{
		tips
			.text( t )
			.addClass( "ui-state-highlight" );
		setTimeout
		(
			function()
			{
				tips.removeClass( "ui-state-highlight", 1500 );
			}, 
			500
		);
	}

	//checks the length of an object
	function checkLength( o, n, min, max )
	{
		if ( o.val().length > max || o.val().length < min )
		{
			o.addClass( "ui-state-error" );
			updateTips( "Length of " + n + " must be between " + min + " and " + max + "." );
			return false;
		} 
		else
		{
			return true;
		}
	}

	//checks an object's value against a regular expression
	function checkRegexp( o, regexp, n )
	{
		if ( !( regexp.test( o.val() ) ) )
		{
			o.addClass( "ui-state-error" );
			updateTips( n );
			return false;
		}
		else
		{
			return true;
		}
	}
	
	//may want to merge this function into a recieve function
	function USERSresponse(event)
	{
		//Update socket functions
		socket.onmessage = function (event) { RecieveMessage(event.data.toString()); };
		
		//Update progress bar
		$("#loading-form").children( ".progressbar" ).progressbar({value: 100});
		$("#loading-form").dialog("close");
		
		//send first message to the reciever
		RecieveMessage(event.data.toString());
		
		if(UserInterval == null)
		{
			UserInterval = setInterval
			(
				function()
				{
					if(socket != null && !isChunking)
					{
						socket.send("USERS");
					}
				},
				UserRequestTime
			);
		}
	}
	
	//Retrieves the response from I AM UserName command
	function IAMResponse(event)
	{
		//checks if I AM response is in error, if it is drop connection					
		if(!beginsWith("OK", event.data.toString()) && !beginsWith("I AM " + name.val(), event.data.toString()))
		{
			socket.close();
		}
		else
		{
			if(beginsWith("OK", event.data.toString()))
			{
				OnEchoServer = false;
			}
			else
			{
				OnEchoServer = true;
			}
			$("#Send").children("h1").text("Logged in as \"" + name.val() + "\" (type message and hit ENTER or click SEND)");
			
			//Update progress bar
			$("#loading-form").children( ".progressbar" ).progressbar({value: 80});
			$("#loading-form").children("p").text("Retrieving Users...");
			
			//Update socket functions
			socket.onmessage = function (event) { USERSresponse(event); };
			socket.onclose = function(event) { SocketClose(event); };
			
			//Send next message
			socket.send("USERS");
		}
	}
	
	//is executed when the socket opens
	function SocketOpen(event)
	{
		//Update progress bar
		$("#loading-form").children( ".progressbar" ).progressbar({value: 40});
		$("#loading-form").children("p").text("Authenticating...");
		
		//Update socket functions
		socket.onerror = function(event) { alert("Socket Error"); };
		socket.onmessage = function(event) { IAMResponse(event); };
		socket.onclose = function(event) { InvalidUser(event); };
		
		//Send next message
		socket.send("I AM " + name.val());
	}
	
	//is executed when the socket fails to open
	function SocketTimeout(event)
	{
		$("#loading-form").dialog("close");
		socket = null;
		updateTips("Server does not exist.");
		$("#login-form").dialog("open");
	}
	
	//is executed when ERROR is returned on I AM UserName command
	function InvalidUser(event)
	{
		$("#loading-form").dialog("close");
		socket = null;
		updateTips("UserName is already in use.");
		name.addClass( "ui-state-error" );
		$("#login-form").dialog("open");
	}
	
	//is executed when the server gets disconnected
	function SocketClose(event)
	{
		$("#loading-form").dialog("close");
		if(UserInterval != null)
		{
			clearInterval(UserInterval);
		}
		UserInterval = null;
		socket = null;
		updateTips("You were disconnected from the server.");
		$("#login-form").dialog("open");
	}
	
	//the dialog box for the login form
	$( "#login-form" ).dialog
	(
		{						
			autoOpen: false,
			closeOnEscape: false,
			open: function(event, ui) { $(".ui-dialog-titlebar-close").hide(); },
			height: 350,
			width: 500,
			show: "scale",
			hide: "scale",
			modal: true,
			buttons:
			{
				"LogIn" : function()
				{
					var bValid = true;
					allFields.removeClass("ui-state-error");

					bValid = bValid && checkLength( name, "UserName", 1, 15 );
					
					bValid = bValid && checkRegexp( name, /^([a-z])+$/g, "UserName may consist only of lowercase characters." );
					
					if ( bValid )
					{
						//place connecting message here
						$("#login-form").dialog("close");
						
						$("#loading-form").dialog("open");
						$("#loading-form").children( ".progressbar" ).progressbar({value: 10});
						$("#loading-form").children("p").text("Connecting...");
						$("#loading-form").dialog('option', 'title', 'Connecting');
						
						//try to connect to host and send username
						socket = new WebSocket( host.val() );
						socket.onopen = function(event) { SocketOpen(event); };
						socket.onclose = function(event) { SocketTimeout(event); };
					}
				}
			}
		}
	);
	
	//opens the log in form
	$("#login-form").dialog("open");
}

function hex2a(hex)
{
	var str = '';
	for (var i = 0; i < hex.length; i += 2)
	{
		str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
	}
	return str;
}

//checks if a haystack is all readable characters
function isValidCharacters(haystack)
{
	return regex(/^([\x20-\x7E])+$/g, haystack);
}

//does a regular expression test
function regex(needle, haystack)
{
	if (!(needle.test(haystack)))
	{
		return false;
	}
	else
	{
		return true;
	}
}

//when it receievs the first chunk execute this
function StartChunking(User, Chunks, IsPrivate)
{
	//When it recieves a C0 message
	function StopChunking()
	{
		socket.onmessage = function (event) { RecieveMessage(event.data.toString()); };
		return DisplayMessage(User, Chunks, IsPrivate);
	}
	
	//when it recieves a chunk
	function RecieveChunks(Message)
	{
		//in case a user message interrupts the chunking
		if(regex(/^([a-z,])+$/g, Message))
		{
			//USERS
			$("#UserList").html("");
			var users = Message.split(",");
			var i = 0;
			
			//Add each user to the userlist
			for(i=0; i < users.length; i++)
			{
				//catches the current user
				if(users[i] == name.val())
				{
					$("#UserList").append("<li><span class=\"UserMe\">" + name.val() + "</span></li>");
				}
				else
				{
					//add to user list if does not exist
					if(!Users[users[i]])
					{
						var user = new Object();
						user.name = users[i];
						user.color = Users.COUNT++;
						user.window = null;
						Users[user.name] = user;
						if(Users.COUNT > Users.MAXCOUNT)
						{
							Users.COUNT = 0;
						}
					}
					$("#UserList").append("<li><span class=\"User" + Users[users[i]].color + "\">" + Users[users[i]].name + "</span></li>");
				}
			}
			
			//Catches when a user name has been clicked
			$("span:not(.UserMe)").click
			(
				function(event)
				{
					PrivateMessageDialog($(this).text());
				}
			);
			return true;
		}
		else if(beginsWith("BROADCAST", Message))
		{
			//BROADCAST
			var FirstLine = Message.indexOf('\n');
			var SecondLine = Message.indexOf('\n', FirstLine + 1);
			User = Message.substring("BROADCAST FROM ".length, FirstLine);
			
			//Is chunking?
			if(Message.substring(FirstLine + 1, FirstLine + 2) == "C")
			{
				alert("Chunking occured while chunking, " + Message);
				return false
			}
			else
			{
				var Length = parseInt(Message.substring(FirstLine + 1, SecondLine));
				Message = Message.substring(SecondLine + 1);
				if(Message.length != Length)
				{
					alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
				}
			}
		}
		else if(beginsWith("PRIVATE", Message))
		{
			//PRIVATE
			IsPrivate = true;
			var FirstLine = Message.indexOf('\n');
			var SecondLine = Message.indexOf('\n', FirstLine + 1);
			User = Message.substring("PRIVATE FROM ".length, FirstLine);
			
			//Is chunking?
			if(Message.substring(FirstLine + 1, FirstLine + 2) == "C")
			{
				alert("Chunking occured while chunking, " + Message);
				return false
			}
			else
			{
				var Length = parseInt(Message.substring(FirstLine + 1, SecondLine));
				Message = Message.substring(SecondLine + 1);
				if(Message.length != Length)
				{
					alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
				}
			}
		}
		else if(beginsWith("C", Message))
		{
			//Chunked
			var FirstLine = Message.indexOf('\n');
			
			//Is chunking?
			if(Message.substring(0, 1) == "C")
			{
				var Length = parseInt(Message.substring(1, FirstLine));
				if(Length == 0)
				{
					return StopChunking();
				}
				
				Message = Message.substring(FirstLine + 1);
				if(Message.length != Length)
				{
					alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
				}
				
				Chunks += Message;
				return true;
			}
			else
			{
				alert("Bad Message: " + Message);
				return false;
			}
		}
		else if(Message == "USERS" && OnEchoServer)
		{
			return true;
		}
		else
		{
			//ERROR
			alert("Bad Message: " + Message);
			return false;
		}
	}
	
	socket.onmessage = function (event) { RecieveChunks(event.data.toString()); };
}

//recieves a message from the socket
function RecieveMessage(Message)
{
	var User = null;
	var IsPrivate = false;
	if(regex(/^([a-z,])+$/g, Message.trim()))
	{
		//USERS
		$("#UserList").html("");
		var users = Message.trim().split(",");
		var i = 0;
		
		//Add each user to the userlist
		for(i=0; i < users.length; i++)
		{
			//catches the current user
			if(users[i] == name.val())
			{
				$("#UserList").append("<li><span class=\"UserMe\">" + name.val() + "</span></li>");
			}
			else
			{
				//add to user list if does not exist
				if(!Users[users[i]])
				{
					var user = new Object();
					user.name = users[i];
					user.color = Users.COUNT++;
					user.window = null;
					Users[user.name] = user;
					if(Users.COUNT > Users.MAXCOUNT)
					{
						Users.COUNT = 0;
					}
				}
				$("#UserList").append("<li><span class=\"User" + Users[users[i]].color + "\">" + Users[users[i]].name + "</span></li>");
			}
		}
		
		//Catches when a user name has been clicked
		$("span:not(.UserMe)").click
		(
			function(event)
			{
				PrivateMessageDialog($(this).text());
			}
		);
		return true;
	}
	else if(beginsWith("BROADCAST", Message))
	{
		//BROADCAST
		var FirstLine = Message.indexOf('\n');
		var SecondLine = Message.indexOf('\n', FirstLine + 1);
		User = Message.substring("BROADCAST FROM ".length, FirstLine);
		
		//Is chunking?
		if(Message.substring(FirstLine + 1, FirstLine + 2) == "C")
		{
			var Length = parseInt(Message.substring(FirstLine + 2, SecondLine));
			Message = Message.substring(SecondLine + 1);
			if(Message.length != Length)
			{
				alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
			}
			
			StartChunking(User, Message, IsPrivate);
			return true;
		}
		else
		{
			var Length = parseInt(Message.substring(FirstLine + 1, SecondLine));
			Message = Message.substring(SecondLine + 1);
			if(Message.length != Length)
			{
				alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
			}
		}
	}
	else if(beginsWith("PRIVATE", Message))
	{
		//PRIVATE
		IsPrivate = true;
		var FirstLine = Message.indexOf('\n');
		var SecondLine = Message.indexOf('\n', FirstLine + 1);
		User = Message.substring("PRIVATE FROM ".length, FirstLine);
		
		//Is chunking?
		if(Message.substring(FirstLine + 1, FirstLine + 2) == "C")
		{
			var Length = parseInt(Message.substring(FirstLine + 2, SecondLine));
			Message = Message.substring(SecondLine + 1);
			if(Message.length != Length)
			{
				alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
			}
			
			StartChunking(User, Message, IsPrivate);
			return true;
		}
		else
		{
			var Length = parseInt(Message.substring(FirstLine + 1, SecondLine));
			Message = Message.substring(SecondLine + 1);
			if(Message.length != Length)
			{
				alert("Invalid message length, Got: " + Message.length + " Expected: " + Length);
			}
		}
	}
	else if(beginsWith("C", Message))
	{
		//Chunked
		alert("Invalid Chunk: " + Message);
		return false;
	}
	else if(Message == "USERS" && OnEchoServer)
	{
		return true;
	}
	else
	{
		//ERROR
		alert("Bad Message: " + Message);
		return false;
	}
	
	return DisplayMessage(User, Message, IsPrivate);
}

//places a message in the MessageBox
function DisplayMessage(User, Message, IsPrivate)
{
	//If is an image, display it. Otherwise just display the message
	if(!isValidCharacters(Message) && DisplayImage(User, Message))
	{
		return true;
	}
	else if(IsPrivate)
	{
		//send to private window
		if(Users[User].window == null || Users[User].window.closed)
		{
			Users[User].window = window.open("Chat.html#User=" + User + "&CSS=" + GetCSSIndex() + "&From=0&Message=" + encodeURIComponent(Message));
		}
		else
		{
			Users[User].window.DisplayMessage(User, Message, false);
		}
		
		return true;
	}
	else
	{
		//make the message safe to view
		//firefox automatically removes extra whitespace for some reason
		Message = Message
				.replace(/&/g, "&amp;")
				.replace(/\>/g, "&gt;")
				.replace(/\</g, "&lt;")
				.replace(/"/g, "&quot;")
				.replace(/'/g, "&apos;")
				.replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1 <br /> $2')
				.replace(/(^|\s):\[($|\s)/g, " <img src=\"images/Embarrassed.png\" /> ")
				.replace(/(^|\s):D($|\s)/g, " <img src=\"images/Grin.png\" /> ")
				.replace(/(^|\s):P($|\s)/g, " <img src=\"images/Tongue.png\" /> ")
				.replace(/(^|\s):&apos;\(($|\s)/g, " <img src=\"images/Crying.png\" /> ")
				.replace(/(^|\s)&gt;:O($|\s)/g, " <img src=\"images/Shouting.png\" /> ")
				.replace(/(^|\s)&gt;:D($|\s)/g, " <img src=\"images/Evil.png\" /> ")
				.replace(/(^|\s)&gt;:\(($|\s)/g, " <img src=\"images/Angry.png\" /> ")
				.replace(/(^|\s)&gt;:\)($|\s)/g, " <img src=\"images/Mischievous.png\" /> ")
				.replace(/(^|\s);D($|\s)/g, " <img src=\"images/Grinning-Wink.png\" /> ")
				.replace(/(^|\s):\]($|\s)/g, " <img src=\"images/Blushing.png\" /> ")
				.replace(/(^|\s):!($|\s)/g, " <img src=\"images/Oops.png\" /> ")
				.replace(/(^|\s):\\($|\s)/g, " <img src=\"images/Undecided.png\" /> ")
				.replace(/(^|\s):&gt;($|\s)/g, " <img src=\"images/Smirk.png\" /> ")
				.replace(/(^|\s)X\(($|\s)/g, " <img src=\"images/Grimmace.png\" /> ")
				.replace(/(^|\s):o($|\s)/g, " <img src=\"images/Gasp.png\" /> ")
				.replace(/(^|\s):\?($|\s)/g, " <img src=\"images/Confused.png\" /> ")
				.replace(/(^|\s);P($|\s)/g, " <img src=\"images/Wink-Tongue.png\" /> ")
				.replace(/(^|\s):X($|\s)/g, " <img src=\"images/Lips-Are-Sealed.png\" /> ")
				.replace(/(^|\s);\)($|\s)/g, " <img src=\"images/Wink.png\" /> ")
				.replace(/(^|\s):\*($|\s)/g, " <img src=\"images/Kiss.png\" /> ")
				.replace(/(^|\s)O:\)($|\s)/g, " <img src=\"images/Innocent.png\" /> ")
				.replace(/(^|\s):\$($|\s)/g, " <img src=\"images/Money-Mouth.png\" /> ")
				.replace(/(^|\s):\)($|\s)/g, " <img src=\"images/Smile.png\" /> ")
				.replace(/(^|\s)&gt;:\|($|\s)/g, " <img src=\"images/Not-Amused.png\" /> ")
				.replace(/(^|\s)&gt;:P($|\s)/g, " <img src=\"images/Angry-Tongue.png\" /> ")
				.replace(/(^|\s):\(($|\s)/g, " <img src=\"images/Frown.png\" /> ")
				.replace(/(^|\s)B\)($|\s)/g, " <img src=\"images/Cool.png\" /> ")
				.replace(/(^|\s):\|($|\s)/g, " <img src=\"images/Straight-Faced.png\" /> ")
				.replace(/(^|\s)\|\)($|\s)/g, " <img src=\"images/Sleeping.png\" /> ")
				.replace(/(^|\s):\[($|\s)/g, " <img src=\"images/Embarrassed.png\" /> ")
				.replace(/(^|\s):D($|\s)/g, " <img src=\"images/Grin.png\" /> ")
				.replace(/(^|\s):P($|\s)/g, " <img src=\"images/Tongue.png\" /> ")
				.replace(/(^|\s):&apos;\(($|\s)/g, " <img src=\"images/Crying.png\" /> ")
				.replace(/(^|\s)&gt;:O($|\s)/g, " <img src=\"images/Shouting.png\" /> ")
				.replace(/(^|\s)&gt;:D($|\s)/g, " <img src=\"images/Evil.png\" /> ")
				.replace(/(^|\s)&gt;:\(($|\s)/g, " <img src=\"images/Angry.png\" /> ")
				.replace(/(^|\s)&gt;:\)($|\s)/g, " <img src=\"images/Mischievous.png\" /> ")
				.replace(/(^|\s);D($|\s)/g, " <img src=\"images/Grinning-Wink.png\" /> ")
				.replace(/(^|\s):\]($|\s)/g, " <img src=\"images/Blushing.png\" /> ")
				.replace(/(^|\s):!($|\s)/g, " <img src=\"images/Oops.png\" /> ")
				.replace(/(^|\s):\\($|\s)/g, " <img src=\"images/Undecided.png\" /> ")
				.replace(/(^|\s):&gt;($|\s)/g, " <img src=\"images/Smirk.png\" /> ")
				.replace(/(^|\s)X\(($|\s)/g, " <img src=\"images/Grimmace.png\" /> ")
				.replace(/(^|\s):o($|\s)/g, " <img src=\"images/Gasp.png\" /> ")
				.replace(/(^|\s):\?($|\s)/g, " <img src=\"images/Confused.png\" /> ")
				.replace(/(^|\s);P($|\s)/g, " <img src=\"images/Wink-Tongue.png\" /> ")
				.replace(/(^|\s):X($|\s)/g, " <img src=\"images/Lips-Are-Sealed.png\" /> ")
				.replace(/(^|\s);\)($|\s)/g, " <img src=\"images/Wink.png\" /> ")
				.replace(/(^|\s):\*($|\s)/g, " <img src=\"images/Kiss.png\" /> ")
				.replace(/(^|\s)O:\)($|\s)/g, " <img src=\"images/Innocent.png\" /> ")
				.replace(/(^|\s):\$($|\s)/g, " <img src=\"images/Money-Mouth.png\" /> ")
				.replace(/(^|\s):\)($|\s)/g, " <img src=\"images/Smile.png\" /> ")
				.replace(/(^|\s)&gt;:\|($|\s)/g, " <img src=\"images/Not-Amused.png\" /> ")
				.replace(/(^|\s)&gt;:P($|\s)/g, " <img src=\"images/Angry-Tongue.png\" /> ")
				.replace(/(^|\s):\(($|\s)/g, " <img src=\"images/Frown.png\" /> ")
				.replace(/(^|\s)B\)($|\s)/g, " <img src=\"images/Cool.png\" /> ")
				.replace(/(^|\s):\|($|\s)/g, " <img src=\"images/Straight-Faced.png\" /> ")
				.replace(/(^|\s)\|\)($|\s)/g, " <img src=\"images/Sleeping.png\" /> ");
				
		//Append Message	
		if(User == name.val())
		{
			$('#MessageBox').append('<p><span class=\"UserMe\">' + User + '</span>: ' + Message + '</p>');
		}
		else
		{
			$('#MessageBox').append('<p><span class=\"User' + Users[User].color + '\">' + User + '</span>: ' + Message + '</p>');
		}
		
		//Need to figure out how to default scroll to bottom of div (none of these work)
		//$('#Messages').scrollTop($('#Messages').height());
		//$('#Messages').scrollTop("100%");
		//$('#Messages').scrollTop = $('#Messages').scrollHeight;
		//$('#Messages').scrollTop($('#Messages')[0].scrollHeight);
		
		return true;
	}
}

//Displays an image in a seperate window (currently not functioning properly)
function DisplayImage(User, content)
{			
	//taken from http://www.astro.keele.ac.uk/oldusers/rno/Computing/File_magic.html
	var extention = "*";
	if(beginsWith("\xff\xd8\xff\xe0", content)) //.jpg
	{
		extention = "jpg";
	}
	else if(beginsWith("\x89\x50\x4E\x47\x0D\x0A\x1A\x0A", content)) //.png
	{
		extention = "png";
	}
	else if(beginsWith("\x47\x49\x46\x38", content)) //.gif
	{
		extention = "gif";
	}
	else if(beginsWith("\x42\x4d", content)) //.bmp
	{
		extention = "bmp";
	}
	/*else if(beginsWith("\x25\x21", content)) //.ps
	{
		extention = "ps";
	}
	else if(beginsWith("\x25\x50\x44\x46", content)) //.pdf
	{
		extention = "pdf";
	}*/
	else if(beginsWith("MM\x00\x2a", content) || beginsWith("II\x2a\x00", content)) //.tif
	{
		extention = "tif";
	}
	else
	{
		return false;
		//alert("File is not a recongized image.");
	}
	
	//a base64 encoder function
	function base64_encode (data)
	{
		var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
		var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
		ac = 0,
		enc = "",
		tmp_arr = [];

		if (!data)
		{
			return data;
		}
		
		//don't do this, it sucks
		//data = this.utf8_encode(data + '');

		do 
		{
			// pack three octets into four hexets
			o1 = data.charCodeAt(i++);
			o2 = data.charCodeAt(i++);
			o3 = data.charCodeAt(i++);

			bits = o1 << 16 | o2 << 8 | o3;

			h1 = bits >> 18 & 0x3f;
			h2 = bits >> 12 & 0x3f;
			h3 = bits >> 6 & 0x3f;
			h4 = bits & 0x3f;

			// use hexets to index into b64, and append result to encoded string
			tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
		} while (i < data.length);

		enc = tmp_arr.join('');

		var r = data.length % 3;

		return (r ? enc.slice(0, r - 3) : enc) + '==='.slice(r || 3);
	}
	
	//create a new window and send the image to that window
	//need to determine size of image
	var img = new Image();
	img.onload = function()
	{
		var child; 
		if(this.width > 0 && this.height > 0)
		{
			child = window.open("", "", "width=" + this.width + ",height=" + this.height);
		}
		else
		{
			child = window.open();
		}
		child.document.write("<!DOCTYPE html><html lang='en'><head><title>" + User + "</title><meta charset='utf-8' /></head><body><img alt='' src='" + this.src + "' /></body></html>");
		child.focus();
	}
	img.src = "data:image/" + extention + ";base64," + base64_encode(content);
	return true;
}
	
//sends a message across the socket
function SendMessage()
{
	//Sends the current message
	var Message = $('#SendMessage').find('textarea').val();
	if(Message.length > 0)
	{
		//Send the current message
		if(socket != null)
		{
			if(Send(null, Message, null))
			{
				$('#SendMessage').find('textarea').val('');
			}
		}
		else if(Hash != null)
		{
			var func = function(value)
			{
				if(typeof value == "boolean")
				{
					if(value)
					{
						DisplayMessage(name.val(), Message, false);
						$('#SendMessage').find('textarea').val('');
					}
				}
			}
			window.opener.Send(Hash['User'], Message, func);
		}
		else
		{
			return false;
		}
	}
}

function Send(User, Message, UpdateFunction)
{
	if(socket != null && !isChunking)
	{
		//Sends Chunks
		if(Message.length > MaxNonChunkedSize)
		{
			isChunking = true;
			if(UpdateFunction != null)
			{
				UpdateFunction(0);
			}
			var IntialLength = Message.length;
			
			//Send the first part of the message
			var ToSend = Message.substr(0, MaxChunkedSize);
			Message = Message.substr(MaxChunkedSize);
			if(User == null)
			{
				if(OnEchoServer)
				{
					//Echo Server
					ToSend = "BROADCAST FROM " + name.val() + "\n" 
						+ "C" + ToSend.length + "\n"
						+ ToSend;
					socket.send(ToSend);
				}
				else
				{
					//Non Echo Server
					ToSend = "BROADCAST\n" 
						+ "C" + ToSend.length + "\n"
						+ ToSend;
					socket.send(ToSend);
				}
			}
			else
			{
				ToSend = "SEND " + User + "\n" 
					+ "C" + ToSend.length + "\n"
					+ ToSend;
				socket.send(ToSend);
			}
			
			//Send rest of the message
			while(Message.length > 0)
			{
				if(UpdateFunction != null)
				{
					UpdateFunction(((IntialLength - Message.length) * 100) / IntialLength);
				}
				ToSend = Message.substr(0, MaxChunkedSize);
				Message = Message.substr(MaxChunkedSize);
				ToSend = "C" + ToSend.length + "\n"
					+ ToSend;
				socket.send(ToSend);
			}
			
			socket.send("C0\n");
			
			if(UpdateFunction != null)
			{
				UpdateFunction(100);
			}
			isChunking = false;
		}
		else //Sends Non-Chunks
		{
			if(User == null)
			{
				if(OnEchoServer)
				{
					//Echo Server
					Message = "BROADCAST FROM " + name.val() + "\n" 
						+ Message.length + "\n"
						+ Message;
					socket.send(Message);
				}
				else
				{
					//Non Echo Server
					Message = "BROADCAST\n" 
						+ Message.length + "\n"
						+ Message;
					socket.send(Message);
				}
			}
			else
			{
				Message = "SEND " + User + "\n" 
					+ Message.length + "\n"
					+ Message;
				socket.send(Message);
			}
		}
		if(UpdateFunction != null)
		{
			UpdateFunction(true);
		}
		return true;
	}
	else
	{
		if(UpdateFunction != null)
		{
			UpdateFunction(false);
		}
		return false;
	}
}

//Occurs when the upload file button is clicked
$("#Upload-File").click
(
	function()
	{
		SendImageDialog();
	}
);

//Sets the current CSS Index
function SetCSSIndex(CSS_Number)
{
	var CSSURL = $('#PageCSS').attr('href');
	var CSS_Start = CSSURL.substring(0, CSSURL.lastIndexOf("/", CSSURL.lastIndexOf("/") - 1) + 1);
	var CSS_Type = CSSURL.substring(CSSURL.lastIndexOf("/"));

	//JqueryCSS, MainCSS, PageCSS
	$('#PageCSS').attr('href', CSS_Start + CSS_Number + CSS_Type);
	$('#MainCSS').attr('href', CSS_Start + CSS_Number + "/styles.css");
	$('#JqueryCSS').attr('href', CSS_Start + CSS_Number + "/jquery-ui-1.8.19.css");
}

//Gets the current CSS Index
function GetCSSIndex()
{
	var CSSURL = $('#PageCSS').attr('href');
	return parseInt(CSSURL.substring(CSSURL.lastIndexOf("/", CSSURL.lastIndexOf("/") - 1) + 1, CSSURL.lastIndexOf("/")));
}

//Changes the CSS Styles by either +1 or -1
$("*").keypress
(
	function(event)
	{
		//-
		if(event.which == 45 && event.ctrlKey)
		{
			var CSS_Number = GetCSSIndex();
			CSS_Number -= 1;
			if(CSS_Number < 1)
			{
				CSS_Number = CSSMax;
			}
			SetCSSIndex(CSS_Number);
			return false;
		}
		//=
		else if(event.which == 61 && event.ctrlKey)
		{
			var CSS_Number = GetCSSIndex();
			CSS_Number += 1;
			if(CSS_Number > CSSMax)
			{
				CSS_Number = 1;
			}
			SetCSSIndex(CSS_Number);
			return false;
		}
	}
);

//Catches an enter press, but not shift + enter
$("#SendMessage").keypress
(
	function(event)
	{
		//if an enter key was pressed
		if (event.which == 13 && !event.shiftKey)
		{
			SendMessage();
			return false;
		}
	}
);

//Catches a send press
$("form#SendMessage").submit
(
	function()
	{
		//if send button is pressed
		$('#SendMessage').find('textarea').focus();
		SendMessage();
		return false;
	}
);

//Opens a private message dialog box
function PrivateMessageDialog(User)
{
	var tips = $("#private-form").children( "p.validateTips" );

	//Updates the log in tip box
	function updateTips( t )
	{
		tips
			.text( t )
			.addClass( "ui-state-highlight" );
		setTimeout
		(
			function()
			{
				tips.removeClass( "ui-state-highlight", 1500 );
			}, 
			500
		);
	}

	$("#private-form").dialog
	(
		{						
			autoOpen: false,
			closeOnEscape: true,
			height: 400,
			width: 500,
			show: "scale",
			hide: "scale",
			modal: true,
			open: function(event, ui)
			{
				$("#UserNames").val(User + " ");
				$("#message").val("");
			},
			buttons:
			{
				"Send" : function()
				{
					var UsersNames = $("#UserNames").val().split(" ");
					
					var i;
					for(i = 0; i < UsersNames.length; i++)
					{
						//only send valid to valid names and make sure this is not a private window
						if(/^([a-z])+$/g.test(UsersNames[i]) && Hash == null)
						{
							if(Send(UsersNames[i], $("#message").val(), null))
							{
								if(Users[UsersNames[i]].window == null || Users[UsersNames[i]].window.closed)
								{
									Users[UsersNames[i]].window = window.open("Chat.html#User=" + UsersNames[i] + "&CSS=" + GetCSSIndex() + "&From=1&Message=" + encodeURIComponent($("#message").val()));
								}
								else
								{
									Users[UsersNames[i]].window.DisplayMessage(name.val(), $("#message").val(), false);
								}
							}
						}
						$(this).dialog("close");
					}
				},
				"Cancel" : function()
				{
					$(this).dialog("close");
				}
			}
		}
	);
	
	$("#private-form").dialog("open");
}

//Open the send image dialog box
function SendImageDialog()
{
	//the files to be sent
	var files = null,
		tips = $("#login-form").children( "p.validateTips" );

	//Updates the upload file tip box
	function updateTips( t )
	{
		tips
			.text( t )
			.addClass( "ui-state-highlight" );
		setTimeout
		(
			function()
			{
				tips.removeClass( "ui-state-highlight", 1500 );
			}, 
			500
		);
	}
	
	function Update(SomeValue)
	{
		//sucessful send or not
		if(typeof SomeValue == "boolean")
		{
			$("#loading-form").dialog("close");
			if(!SomeValue)
			{
				$("#upload-form").dialog("open");
				updateTips("Image did not send.");
			}
		}
		//double, float, short, int, long, or other numric
		else
		{
			$("#loading-form").children( ".progressbar" ).progressbar({value: parseInt(SomeValue)});
		}
	}
	
	//Sends an image over the socket
	function SendImage(image)
	{
		$("#loading-form").dialog("open");
		$("#loading-form").dialog('option', 'title', 'Uploading');
		$("#loading-form").children("p").text("Uploading...");
		
		var reader = new FileReader();
		reader.onload = function (event)
		{
			//the image's text
			var Message = event.target.result;
			
			if(socket != null)
			{
				Send(null, Message, Update)
			}
			else if(Hash != null)
			{
				window.opener.Send(Hash['User'], Message, Update);
			}
			else
			{
				$("#loading-form").dialog("close");
				$("#upload-form").dialog("open");
				updateTips("Image did not send.");
			}
		}
		reader.readAsBinaryString(image);
	}
	
	//catches the event when the files to be sent are changed
	$("#file-location").change
	(
		function(event)
		{
			files = event.target.files;
		}
	)
	
	//The send image dialog box
	$("#upload-form").dialog
	(
		{						
			autoOpen: false,
			closeOnEscape: true,
			height: 300,
			width: 500,
			show: "scale",
			hide: "scale",
			modal: true,
			buttons:
			{
				"Send" : function()
				{
					if(files.length > 0)
					{
						$(this).dialog("close");
						SendImage(files[0]);
						$("#file-location").val("");
					}
				},
				"Cancel" : function()
				{
					$(this).dialog("close");
				}
			}
		}
	);
	
	$("#upload-form").dialog("open");
}

//the dialog box for the loading form
$("#loading-form").dialog
(
	{						
		autoOpen: false,
		closeOnEscape: false,
		height: 125,
		width: 500,
		show: "scale",
		hide: "scale",
		modal: true,
		open: function(event, ui)
		{ 
			$(".ui-dialog-titlebar-close").hide(); 
			$(this).children( ".progressbar" ).progressbar({value: 0});
			$(this).children("p").text("Loading...");
			$(this).dialog('option', 'title', 'Loading');
		}
	}
);