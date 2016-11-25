Joseph Salomone
660658959
salomj@rpi.edu
Read_Me.txt

Tested using Firefox 12.0

----------
~Contents~
----------
-Chat.html, is the html file to be run
-Read_Me.txt, the current file
-images folder, contains 29 different emotion icons
-scripts folder, contains 3 different javascript files
-styles folder, contains 4 different CSS folders
-CSS folders, contains an image folder and 4 diffent CSS files
-CSS image folder, contains all the images used by the CSS files

---------
~Defines~
---------
There are 5 changable variables in chat-server.js, and are located on lines 16-20.

-UserRequestTime-
The time between user requests

-MaxChunkedSize-
Maximum size of a chunk

-MaxNonChunkedSize-
Maximum size of a non-chunk

-CSSMax-
The last value of a CSS Skin

-OnEchoServer-
True means you want to use an Echo Server
False means you want to use a Real Chat Server
This value is determined when connecting to a server and will change if you disconnect
and reconnect to a different server

------------
~Connecting~
------------
You can connect to any server of your choosing with any name.
Error messages will appear if unable to connect.
The two types of error messages that appear are unable to connect, and username already
in use.

The echo server I used was 'ws://echo.websocket.org:80/'

You can disconnect by pressing 'Esc'.

-------------------
~Sending a Message~
-------------------
To send a message either click the 'SEND' button or press 'Enter'. If 'Shift' + 'Enter'
is pressed, this will create a new line in the message.

C0 chunks always end with a '\n'.

If the message does not send, then it has failed to send. This is mostly likely due to
trying to send a message while sending a Chunked Message.

To send a private message, click on a UserName in the main window only. This will open
a dialog where you can send to as many users as possible as long as they are space 
delimited. A new window for each user will open and the message will be sent to all the 
users.

The client may make 'USERS' request message while sending a chunked message. But other
messages will not be allowed.

---------------------
~Recieving a Message~
---------------------
'\n', '\r', '\n\r', and '\r\n' inside a message will all be turned into '<br />'

While chunking, the client can recieve all messages except for another chunked message.

------------------
~Private Messages~
------------------
To send a private message first click on someone's name. Multiple people are allowed, 
just put spaces between the names. The same message will be sent to all of them. Each
person sent will get their own private message window.

When recieving a private message, a new window will open (unless one is already open)
and the message will be displayed

----------------
~Chaging Styles~
----------------
There are 4 different styles provided. The only difference between these styles is
the CSS file.

To change between styles use 'Ctrl' + '-' to go to the previous style and 'Ctrl' + '='
to go to the next style.

To add more styles just add another folder as the next number. The first style should
start at 1, and there should be no gaps in the numbering. Then CSSMax should be set to
the last number of the CSS skin. 

---------------
~Inline Images~
---------------
-Emotions-
The Emotion Icons were taken from http://www.adiumxtras.com/index.php?a=xtras&xtra_id=980

-Avaible Icons-
:[
:D
:P
:'(
>:O
>:D
>:(
>:)
;D
:]
:!
:\
:>
X(
:o
:?
;P
:X
;)
:*
O:)
:$
:)
>:|
>:P
:(
B)
:|
|)

Note: To use icons, they must be their own word.

-----------------
~Uploaded Images~
-----------------
*Valid formats include jpeg, gif, bmp, png, tif
*Only the file is sent, no title is sent.
*File content is sent as binary text, 
no conversions are done when it is sent to the socket
*File image type is detected by file content and not by the title
*The new window size has a minimum size of 100 by 100, 
so images under this will be forced into a 100 by 100 window
*If an image contains all human-readable characters, then it is interrupted as a message.
This is only possible for bmp, since all other magic numbers have unreadable characters.

NOTE!! If the chat server is having problems with null characters, then you can change
the "OnEchoServer" variable to true and connect to "ws://echo.websocket.org:80/". You can
then validate that images work with a server, but the current server terminates at null 
characters.