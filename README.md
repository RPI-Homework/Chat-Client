Network Programming Project #4 - TCP/WebSockets Chat Client

Due Friday 5/4 

« hide details 

 Our fourth (and final) assignment is to be completed individually. Do not share code or review anyone else's code. Work on this project is to be your own. 

 Submit your project (client-side only!) via RPI LMS as a single compressed and zipped file (using tar and gzip); include only source code, image files, and documentation files, as applicable. 

 To package up your submission, use tar and gzip. More specifically, create a compressed tar file using your RCS userid, as in goldsd.tar.gz, that contains your source files (e.g. chat.html and chat.css); include a readme.txt file only if necessary. Here's an example: 
bash$ tar cvf goldsd.tar chat.html chat.css readme.txt
chat.html
chat.css
readme.txt
bash$ gzip -9 goldsd.tar

Be sure to provide comments within your source files; include your name at the top of each file submitted. 
CLARIFICATIONS:

(1) For this project, this file describes the protocol that
    your chat client must implement.  Note that there are
    some additions beyond that of Project #3 (in part to
    differentiate between broadcast and private messages
    the client receives).

(2) For your chat client to communicate with the server,
    use HTML5 WebSockets.  To test your client, you may use
    your Project #3 submission (though you'll need to modify
    it to act as a WebSockets server).


Chat Client: Based on the chat server from Project #3 above, write a chat client using HTML5, HTML5 WebSockets, CSS, and JavaScript. Use TCP/WebSockets to connect to the chat server (which will be provided to you). Match the screen shot below exactly, which includes color-coding your usernames, placing your RCS userid in the upper-right hand corner, etc. (see extra credit below). 

 

 Your chat client must connect to the chat server on port 8787 (you may hardcode the server hostname and port number). When you submit your project, be sure the hostname is set to localhost and the port number is 8787. 

 Your chat client must, at all times, provide links to the online validator; more specifically, provide a link to the HTML validator beneath every page of your client, plus a link to the CSS validator. If your code does not validate, you will lose points. Mimic the links and images at the bottom of this project page. 

 When your client starts up, users must specify their username, which (once validated by the client) is sent to the server via an I AM command. If an error occurs here (or elsewhere), use JavaScript alert() messages. 

 Messages sent via the "main" page (shown above) are sent to the chat server via the BROADCAST command. 

 A new USERS command must be sent by your client to obtain a list of all users currently logged in to the server. The USERS message ends in a newline character; once received, the chat server responds with a comma-separated list of all usernames, followed by a newline, as shown below. Note that you can assume no spaces will be in this response; further, all usernames will be valid (i.e. lowercase letters only, up to 15 characters in length, no duplicates). 
ladygaga,selenagomez,parishilton,justinbieber,etc.

 When a user clicks on a username anywhere within the "main" interface, the user interface opens a new window that allows the user to send and receive private messages (as shown below). Likewise, when a private message is received, show it in a new window. If a window is already open for a given username, send and receive all messages from that window (i.e. do not blindly open a new window for each private message exchange). 

 

 Users must also be able to send image (or other) files to each other via private messages or broadcast. You may assume that all files sent are image files and will be uploaded by the client as a stream of bytes. Add the necessary browse and send buttons. 

 When your client receives an image, display the image in a new window that frames the image and requires no scrollbars whatsoever. 

 No action by the user should cause the page to reload (i.e. avoid using "old-fashioned" forms that post request data to a server-side script). 

 Your client must be able to accept asynchronous communication from the chat server. More specifically, the chat server may send any number of messages to the client at any time. Use HTML5 WebSockets, which is supported in beta form by Firefox and Google Chrome. You may assume that all messages are correct according to the protocol and rules specified in Project #3. Also see this summary of the Project #4 protocol specifications. 

 To test your client, network with your friends in class. Each of you can use a different client as long as one of you is running the chat server. You might also test by using multiple tabs/windows or even multiple browsers. 

Extra Credit: Add support for inline images, meaning a client can send image data as part of their text (e.g. for smileys). 

Extra Credit: In addition to the required layout shown above, create a chat client of your own design. Be sure that your chat client layout meets the same set of functionality requirements described above. 

Important Note: Other "bells and whistles" is nice, but please be sure your solution meets all the required specifications! Do not make changes to the protocol, required layout, etc., lest you wish to lose points! For this project, we may stifle some of your creativity, but only to ensure all of the required functionality works and is straightforward for our TAs to test and grade.
