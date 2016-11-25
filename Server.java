/*
 * Joseph Salomone
 * 660658959 
 * salomj
 * Server.java
 * File Contains all that is required to run the Chat_Server
 */

//package Chat_Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread.State;

final class define
{
	public static final int UDP_SERVER_TIMEOUT = 100; //How long to wait on UDP receive
	public static final int TCP_SERVER_TIMEOUT = 100; //How long to wait on TCP accept
	public static final int TCP_CLIENT_TIMEOUT = 100; //How long to wait on TCP read
	public static final int UDP_CLIENT_LIMIT = 100; //Max # of UDP clients allowed
	public static final int TCP_CLIENT_LIMIT = 100; //Max # of TCP clients allowed
	public static final int MAX_CHCUKED_LENGTH = 999; //Max length of chunked packets
	public static final int MAX_NONCHCUKED_LENGTH = 99; //Max length of non-chunked packets
	public static boolean Verbose = false; //If in verbose mode or not
	public static void Recieve(String Username, String address, String message) //Verbose RCVD message
	{
		if(Verbose)
		{
			if(Username == null)
			{
				System.out.println("RCVD from " + address + ":\n" + message);
			}
			else
			{
				System.out.println("RCVD from " + Username + " (" + address + "):\n" + message);
			}
		}
	}
	public static void Send(String Username, String address, String message, boolean isRandom) //Verbose SEND message
	{
		if(Verbose)
		{
			if(isRandom)
			{
				if(Username == null)
				{
					System.out.println("SENT (randomly!) TO " + address + ":\n" + message);
				}
				else
				{
					System.out.println("SENT (randomly!) TO " + Username + " (" + address + "):\n" + message);
				}
			}
			else
			{
				if(Username == null)
				{
					System.out.println("SENT to " + address + ":\n" + message);
				}
				else
				{
					System.out.println("SENT to " + Username + " (" + address + "):\n" + message);
				}
			}
		}
	}
	public static void Send(String Username, String address, String message) //Verbose SEND message
	{
		define.Send(Username, address, message, false);
	}
	public static final int EveryXMessages = 4; //How many message are send until a random message is sent
	public static final String [] RandomMessages = //The possible random messages to be sent
	{
		"Hey, you're kinda hot.",
		"No way!",
		"Why does it say paper jam when these is no paper jam?!",
		"Why are you so mean to me?",
		"Why aren't you naked?",
		"Back off?",
		"You want to see some naked pictures of me?",
		"DUDE! Your mom is SOO hot!",
		"Where are my clothes?",
		"I'll be over in a bit.",
		"I'm Pro-Choice!",
		"I'm Pro-Life!",
		"Your mom is very good at the Tri-Force.",
		"Anybody who disapproves of Barack Obama is stupid.",
		"Anybody who disapproves of Mitt Romney is stupid.",
		"Anybody who disapproves of Rick Santorum is stupid.",
		"Anybody who disapproves of Newt Gingrich is stupid.",
		"Anybody who disapproves of Ron Paul is stupid.",
		"Anybody who likes Barack Obama is stupid.",
		"Anybody who likes Mitt Romney is stupid.",
		"Anybody who likes Rick Santorum is stupid.",
		"Anybody who likes Newt Gingrich is stupid.",
		"Anybody who likes Ron Paul is stupid.",
		"All criminal should be executed.",
		"Hey, did you see that porno with your mom in it?",
		"Go Die!",
		"Fuck you!",
		"Go fuck yourself!",
		"Bitch!",
		"Cunt!",
		"Whore!",
		"Slut!",
		"Burn in hell!"
	};
}
//Contains a client using TCP
class Client_TCP extends Client implements Runnable
{
	Socket socket = null;
	BufferedReader fromClient = null;
	PrintWriter toClient = null;
	
	public Client_TCP(Socket socket)
	{
		this.socket = socket;
		try
		{
			this.socket.setSoTimeout(define.TCP_CLIENT_TIMEOUT);
		}
		catch (SocketException ex)
		{
			System.err.println("Unable to create TCP socket timeout: " + ex.getMessage());
		}
	}
	
	//Gets the IP address
	protected InetAddress GetAddress()
	{
		return this.socket.getInetAddress();
	}
	
	//Reads a line from a TCP client
	protected String readLine() throws IOException, Exception
	{
		String s = null;
		while(s == null)
		{
			try
			{
				s = fromClient.readLine();
				if(s == null)
				{
					throw new Exception("Client Dropped");
				}
			}
			catch(SocketTimeoutException ex)
			{
				
			}
		}
		return s;
	}
	
	//Reads a given length from a TCP client
	protected String read(int length) throws IOException, Exception
	{
		char [] buffer = new char[length];
		int offset = 0;
		int ret = 0;
		while(offset < length)
		{
			try
			{
				ret = this.fromClient.read(buffer, offset, length - offset);
				if(ret == 0)
				{
					throw new Exception("Client Dropped");
				}
				offset += ret;
			}
			catch(SocketTimeoutException ex)
			{
			}
		}
		return new String(buffer);
	}
	
	//Initializes a TCP client
	private boolean CreateClient()
	{
		//Create Readers and Writer
		try
		{
			this.fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.toClient = new PrintWriter(socket.getOutputStream(), true);
		}
		catch (Exception ex)
		{
			//System.err.println("TCP Client Establishment Error: " + ex.getMessage());
			return false;
		}
		
		try
		{
			String s = this.readLine().trim();
			define.Recieve(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), s);
			if(this.SetUserName(s))
			{
				this.AddClient();
				this.Send("OK\n", false);
				return true;
			}
		}
		catch(Exception ex)
		{
			this.Send("ERROR\n", false);
			return false;
		}
		this.Send("ERROR\n", false);
		return false;
	}
	
	//Runs the current TCP client
	public void run()
	{
		if(this.CreateClient())
		{
			while(true)
			{
				try 
				{
					//Get Query and Length
					String Query = this.readLine().trim();
					
					if(Query.startsWith("I AM "))
					{
						this.Send("ERROR\n", false);
						continue;
					}
					
					String Length = this.readLine().trim();
					int length = 0;
					
					//Check for client drop
					if(Query == null || Length == null)
					{
						throw new Exception("Client Dropped");
					}
					
					//Get initial length
					if(Length.toUpperCase().charAt(0) == 'C')
					{
						//chunking
						length = Integer.parseInt(Length.substring(1));
						if(length > define.MAX_CHCUKED_LENGTH || length < 0)
						{
							throw new Exception("Bad Chunking Length");
						}
					}
					else
					{
						length = Integer.parseInt(Length);
						if(length > define.MAX_NONCHCUKED_LENGTH || length < 0)
						{
							throw new Exception("Bad Non-Chunking Length");
						}
					}
					
					//Receive complete length
					String message = this.read(length);					
					
					if(Length.toUpperCase().charAt(0) == 'C')
					{
						//Chunked send
						define.Recieve(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), 
								Query + "\nC" + message.length() + "\n" + message);
						this.ReceiveChunking(Query, message);
					}
					else
					{
						//Non-chunking send
						define.Recieve(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), 
								Query + "\n" + message.length() + "\n" + message);
						this.Receive(Query, message);
					}
				}
				//if error occurs just drop the client
				catch(Exception ex)
				{
					this.Send("ERROR\n", false);
					//System.err.println("Exception: " + ex.getMessage());
					//Just drop client
					break;
				}
			}
		}
		
		//Close the socket
		try
		{
			//define.println("DROP " + this.GetUsername() + "(" + this.socket.getInetAddress().toString().substring(1) + ")");
			this.RemoveClient();
			this.socket.close();
		}
		catch(Exception ex)
		{
		}
	}

	//Sends a message to a TCP client
	protected boolean Send(String s, boolean isRandom) 
	{
		try
		{
			this.toClient.print(s.toCharArray());
			this.toClient.flush();
			define.Send(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), s, isRandom);
		}
		catch(Exception ex)
		{
			return false;
		}
		return true;
	}
}

//Contains a client using UDP
class Client_UDP extends Client
{
	InetAddress address;
	int port;
	Server_UDP server;
	
	//Gets the current IP
	protected InetAddress GetAddress()
	{
		return address;
	}
	
	//Used for TCP client in Chunking Mode
	protected String read(int length) throws IOException, Exception
	{
		throw new UnsupportedOperationException();
	}
	protected String readLine() throws IOException, Exception
	{
		throw new UnsupportedOperationException();
	}
	//Initializes the client based off the packet
	public Client_UDP(Server_UDP server, DatagramPacket packet)
	{
		this.server = server;
		this.address = packet.getAddress();
		this.port = packet.getPort();
		this.Receive(packet);
	}
	//Read the data received
	private void Receive(DatagramPacket packet)
	{
		this.Receive(new String(packet.getData()).substring(0, packet.getLength()));
	}
	
	//Gets a non-chunked message from a UDP client
	private void Receive(String s)
	{
		try 
		{
			String Message = s;
			//Continue while there is still a message
			while(Message.length() > 0)
			{
				String [] packet = Message.split("\\r?\\n", 2);
				String Query = packet[0];
				Message = packet[1];
				
				if(Query.startsWith("I AM "))
				{
					this.CreateClient(Query);
				}
				else
				{
					packet = packet[1].split("\\r?\\n", 2);
					
					int length = Integer.parseInt(packet[0]);
					if(length > define.MAX_NONCHCUKED_LENGTH || length < 0)
					{
						throw new Exception("Bad Non-Chunking Length");
					}
					String message = packet[1].substring(0, length);
					
					
					define.Recieve(this.GetUsername(), this.address.toString().substring(1), 
							Query + "\n" + message.length() + "\n" + message);				
					this.Receive(Query, message);
					
					Message = packet[1].substring(length);
				}
			}
		}
		catch(Exception ex)
		{
			this.Send("ERROR\n", false);
		}
	}
	//Creates a client based of the packet
	private void CreateClient(String data)
	{
		String s[] = data.split("\\r?\\n", 2);
		define.Recieve(this.GetUsername(), this.address.toString().substring(1), s[0]);
		if(this.GetUsername() == null && this.SetUserName(s[0]))
		{
			this.AddClient();
			this.Send("OK\n", false);
			//client has authenticated
			if(s.length > 1)
			{
				this.Receive(s[1]);
			}
		}
		else
		{
			this.Send("ERROR\n", false);
			//Client did not authenticate
		}
	}
	//If this is the client, send the data
	public boolean isClient(DatagramPacket packet)
	{
		if(this.address.toString().compareTo(packet.getAddress().toString()) == 0 && this.port == packet.getPort())
		{
			this.Receive(packet);
			return true;
		}
		return false;
	}
	
	//Removes the current client
	public void Remove()
	{
		this.address = null;
		this.port = 0;
		this.server = null;
		this.RemoveClient();
	}

	//Sends a message to a UDP client
	protected boolean Send(String s, boolean isRandom) 
	{
		try
		{
			DatagramPacket packet = new DatagramPacket(s.getBytes(), s.getBytes().length, this.address, this.port);
			boolean out = this.server.Send(packet);
			if(out)
			{
				define.Send(this.GetUsername(), this.address.toString().substring(1), s, isRandom);
			}
			return out;
		}
		catch(Exception ex)
		{
			return false;
		}
	}
}

//A synchronized list (used to keep track of all of the clients)
class SynchronizedList<T> implements Iterable<T>
{
	private LinkedList<T> List = new LinkedList<T>();
	
	public synchronized void AddClient(T c)
	{
		this.List.addFirst(c);
	}
	
	//Removes a client from the current list of clients
	public synchronized void RemoveClient(T c)
	{
		this.List.removeFirstOccurrence(c);
	}
	
	public synchronized Iterator<T> iterator()
	{
		return this.List.iterator();
	}
}

//A class which is used to prevent a client from retrieving a message while in chunking mode
class SynchronizedChunking
{
	private long CurrentChunkingID = -1;
	private long RequestChunkingCount = 0;
	private long NonChunkedSendCount = 0;
	
	//Sets the current user send a chunk
	protected synchronized boolean SetChunkingID()
	{
		if(this.CurrentChunkingID == -1 && NonChunkedSendCount == 0)
		{
			CurrentChunkingID = Thread.currentThread().getId();
			return true;
		}
		else
		{
			return false;
		}
	}
	
	//Gets the current user sending a chunk
	protected synchronized long GetChunkingID()
	{
		return this.CurrentChunkingID;
	}
	
	//Removes the current user from sending a chunk
	public synchronized void RemoveChunkingID() throws Exception
	{
		if(CurrentChunkingID == Thread.currentThread().getId())
		{
			this.CurrentChunkingID = -1;
		}
		else
		{
			throw new Exception("Synchronization Error: Cannot remove chunking ID when it is not the current thread.");
		}
	}
	
	//Sets the number of user waiting to send chunks
	protected synchronized boolean SetChunking()
	{
		RequestChunkingCount++;
		return true;
	}
	
	//Sets the number of users waiting to send chunks
	public synchronized void RemoveChunking()
	{
		RequestChunkingCount--;
	}
	
	//Sets the number of users sending non-chunked messages
	protected synchronized boolean SetNonChunking()
	{
		if(this.CurrentChunkingID == -1 && RequestChunkingCount == 0)
		{
			NonChunkedSendCount++;
			return true;
		}
		return false;
	}
	
	//Sets the number of users sending non-chunked messages
	public synchronized void RemoveNonChunking()
	{
		NonChunkedSendCount--;
	}
	
	//Wait until chunked messages can be sent to the current user
	public void ChunkingWait() throws Exception
	{
		while(!this.SetChunking());
		while(this.CurrentChunkingID != Thread.currentThread().getId())
		{
			if(this.SetChunkingID() && this.CurrentChunkingID != Thread.currentThread().getId())
			{
				throw new Exception("Unknown Synchronization Error");
			}
		}
	}
	
	//Wait until non-chunked messages can be sent to the current user
	public void NonChunkingWait()
	{
		while(!this.SetNonChunking());
	}
}

//Used to send random messages to clients
class RandomClientSends
{
	private Client client = null;
	private String Query = null;
	
	RandomClientSends(Client client, String Query)
	{
		this.client = client;
		this.Query = Query;
	}
	
	//Code which sends a random message
	public void DoRecieve()
	{
		Random random = new Random();
		int message = random.nextInt() % define.RandomMessages.length;
		try 
		{	
			if(message < 0)
			{
				message += define.RandomMessages.length;
			}
			client.Receive(Query, define.RandomMessages[message], true);
		}
		catch (Exception e)
		{
		}
	}
}

//A class which manages a list of the last few messages (used for random messages)
class RandomClientSendsList
{
	private LinkedList<RandomClientSends> list = new LinkedList<RandomClientSends>();
	
	//A messages to random message list and sends them when ready
	public synchronized void addClient(Client client, String Query)
	{
		this.list.add(new RandomClientSends(client, Query));
		
		//If it is time to send a random message, send a random message
		if(this.list.size() >= define.EveryXMessages)
		{
			Random random = new Random();
			int message = random.nextInt() % define.EveryXMessages;
			
			//This exists because java does modules stupidly
			if(message < 0)
			{
				message += define.EveryXMessages;
			}
			list.get(message).DoRecieve();
			list.clear();
		}
	}
}

//An abstract class of clients communicating with each other
abstract class Client
{
	//A list containing the last few messages
	private static RandomClientSendsList random = new RandomClientSendsList();
	
	//A list containing all the clients
	private static SynchronizedList<Client> ClientList = new SynchronizedList<Client>();
	
	//The current client's UserName
	private String Username = null;
	
	//The current client's synchronization state (prevents the client from receiving other messages while receiving a chunk)
	private SynchronizedChunking sync = new SynchronizedChunking();
	
	
	//Adds a client to the current list of clients
	protected void AddClient()
	{
		Client.ClientList.AddClient(this);
	}
	
	//Removes a client from the current list of clients
	protected void RemoveClient()
	{
		Client.ClientList.RemoveClient(this);
	}
	
	//Gets the current address
	abstract protected InetAddress GetAddress();
	
	//Reads the next line (only works for TCP Client)
	abstract protected String readLine() throws IOException, Exception;
	
	//Reads the next few x words (only works for TCP Client)
	abstract protected String read(int length) throws IOException, Exception;
	
	//Adds a client to the chunking state
	private void ChunkingReserveClient(Client client)
	{
		while(client.sync.GetChunkingID() != Thread.currentThread().getId())
		{
			try
			{
				client.sync.ChunkingWait();
			}
			catch(Exception ex)
			{
				
			}
		}
		client.sync.RemoveChunking();
	}
	
	//Removes a client from the chunking state
	private void ChunkingRemoveClient(Client client)
	{
		try
		{
			client.sync.RemoveChunkingID();
		}
		catch(Exception ex)
		{
			
		}
	}
	
	//Processes a chunking query
	public void ReceiveChunking(String Query, String Message)
		throws IOException, Exception
	{
		if(this.Username == null)
		{
			throw new Exception("User is not autenticated");
		}
		
		LinkedList<Client_TCP> ChunkingList = new LinkedList<Client_TCP>();
		
		String [] Users = Query.split("\\s+");
		if(Users[0].compareTo("SEND") == 0)
		{
			Client.random.addClient(this, Query);
			if(Users.length > 1)
			{
				for(int i = 1; i < Users.length; i++)
				{
					try
					{
						Client client = this.FindUsername(Users[i]);
						if(client.getClass().toString().compareTo(Client_TCP.class.toString()) == 0)
						{
							this.ChunkingReserveClient(client);
							ChunkingList.add((Client_TCP)client);
						}
					}
					catch(Exception ex)
					{
						
					}
				}
			}
			else
			{
				//Incorrect use (Assume BROADCAST?)
				for(Client client : Client.ClientList)
				{
					try
					{
						if(client.getClass().toString().compareTo(Client_TCP.class.toString()) == 0)
						{
							this.ChunkingReserveClient(client);
							ChunkingList.add((Client_TCP)client);
						}
					}
					catch(Exception ex)
					{
					}
				}
			}
		}
		else if(Users[0].compareTo("BROADCAST") == 0)
		{
			Client.random.addClient(this, Query);
			for(Client client : Client.ClientList)
			{
				try
				{
					if(client.getClass().toString().compareTo(Client_TCP.class.toString()) == 0)
					{
						this.ChunkingReserveClient(client);
						ChunkingList.add((Client_TCP)client);
					}
				}
				catch(Exception ex)
				{
				}
			}
		}
		else if(Users[0].compareTo("I AM") == 0)
		{
			this.Send("ERROR\n", false);
			return;
		}
		else
		{
			throw new Exception("Invalid Request (" + Users[0] + ")");
		}
		
		//Generate start message
		
		int Length = Message.length();
		
		String s = "FROM " + this.Username + "\n"
		+ "C" + Length + "\n" 
		+ Message;
		
		//Send message to all reserved clients
		do
		{
			try
			{
				//Send message to clients
				for(Client_TCP client : ChunkingList)
				{
					try
					{
						client.Send(s, false);
					}
					catch(Exception ex)
					{
						
					}
				}
				
				//Find next message
				s = this.readLine();
				if(s.charAt(0) != 'C')
				{
					break;
				}
				
				Length = Integer.parseInt(s.substring(1));
			
				if(Length < 1)
				{
					define.Recieve(this.Username, this.GetAddress().toString().substring(1), "C0\n");
					break;
				}
			
				Message = this.read(Length);
				
				s = "C" + Length + "\n" 
				+ Message;
				
				define.Recieve(this.Username, this.GetAddress().toString().substring(1), s);
			}
			catch(Exception ex)
			{
				break;
			}
		} while(Message != null);
		
		//If something breaks, send C0 to all receivers
		for(Client_TCP client : ChunkingList)
		{
			try
			{
				client.Send("C0\n", false);
				this.ChunkingRemoveClient(client);
			}
			catch(Exception ex)
			{
				
			}
		}
	}
	
	//Processes a non-chunking query
	public void Receive(String Query, String Message) 
		throws Exception
	{
		this.Receive(Query, Message, false);
	}
	
	//Processes a non-chunking query
	public void Receive(String Query, String Message, boolean IsRandom) 
		throws Exception
	{
		if(this.Username == null)
		{
			throw new Exception("User is not autenticated");
		}
		
		String s = "FROM " + this.Username + "\n"
			+ Message.length() + "\n" 
			+ Message;
		
		String [] Users = Query.split("\\s+");
		if(Users[0].compareTo("SEND") == 0)
		{
			if(!IsRandom)
			{
				Client.random.addClient(this, Query);
			}
			if(Users.length > 1)
			{
				for(int i = 0; i < Users.length; i++)
				{
					this.SendTo(Users[i], s, IsRandom);
				}
			}
			else
			{
				//Incorrect use (Assume BROADCAST?)
				this.SendAll(s, IsRandom);
			}
		}
		else if(Users[0].compareTo("BROADCAST") == 0)
		{
			if(!IsRandom)
			{
				Client.random.addClient(this, Query);
			}
			this.SendAll(s, IsRandom);
		}
		else if(Users[0].compareTo("I AM") == 0)
		{
			if(!IsRandom)
			{
				this.Send("ERROR\n", false);
			}
		}
		else
		{
			if(!IsRandom)
			{
				throw new Exception("Invalid Request (" + Users[0] + ")");
			}
		}
	}
	
	//Returns true when the send was successful, false otherwise
	abstract protected boolean Send(String s, boolean isRandom);
	
	//Gets the current UserName
	public String GetUsername()
	{
		return this.Username;
	}
	
	//Sends a message to all clients
	protected void SendAll(String s, boolean isRandom)
	{
		for(Client client : Client.ClientList)
		{
			try
			{
				client.sync.NonChunkingWait();
				client.Send(s, isRandom);
				client.sync.RemoveNonChunking();
			}
			catch(Exception ex)
			{
				
			}
		}
	}
	
	//Returns true on a successful send, false otherwise
	protected boolean SendTo(Client client, String s, boolean isRandom)
	{
		try
		{
			if(client != null)
			{
				boolean ret;
				client.sync.NonChunkingWait();
				ret = client.Send(s, isRandom);
				client.sync.RemoveNonChunking();
				return ret;
			}
			else
			{
				return false;
			}
		}
		catch(Exception ex)
		{
			return false;
		}
	}
	
	//Returns true on a successful send, false otherwise
	protected boolean SendTo(String Username, String s, boolean isRandom)
	{
		Client client = this.FindUsername(Username);
		return this.SendTo(client, s, isRandom);
	}
	
	//Find a user in the current list
	protected Client FindUsername(String Username)
	{
		for(Client client : ClientList)
		{
			try
			{
				if(client.Username.compareTo(Username) == 0)
				{
					return client;
				}
			}
			catch(Exception ex)
			{
				
			}
		}
		return null;
	}
	
	//Returns true if the UserName being set is unique
	protected boolean SetUserName(String s)
	{
		try
		{
			if(s.startsWith("I AM "))
			{
				//Split is to insure that UserName does not contain whitespace
				String [] strings = s.split("\\s+", 4);
				//checks for an invalid declaration
				if(strings.length > 3)
				{
					this.Username = null;
					return false;
				}
				this.Username = strings[2].trim();
				if(FindUsername(this.Username) == null)
				{
					return true;
				}
			}
		}
		catch(Exception ex)
		{
		}
		this.Username = null;
		return false;
	}
}

//A UDP server
class Server_UDP extends Server_All
{
	private DatagramSocket socket = null;
	private LinkedList<Client_UDP> ClientList = new LinkedList<Client_UDP>();
	
	Server_UDP(DatagramSocket socket)
	{
		this.socket = socket;
		try
		{
			this.socket.setSoTimeout(define.UDP_SERVER_TIMEOUT);
		}
		catch (SocketException ex)
		{
			System.err.println("Unable to create UDP socket timeout: " + ex.getMessage());
		}
	}
	
	//Return true if it has a client (then sends the packet to the client), false otherwise
	private boolean HasClient(DatagramPacket packet)
	{
		for(Client_UDP client : ClientList)
		{
			if(client.isClient(packet))
			{
				 return true;
			}
		}
		return false;
	}
	
	protected void DoServer()
	{
		while(true)
		{
			byte[] buffer = new byte[2048];
		    try
		    {
	    		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		        this.socket.receive(packet);
		        
		        //If no UDP client contains that port and ip
		        if(!HasClient(packet))
		        {
		        	//define.println("UDP Client Connected (" + packet.getAddress().toString().substring(1) + ":" + packet.getPort() + ")");
		        	
		        	//Drop all UDP clients
		        	if(this.ClientList.size() >= define.UDP_CLIENT_LIMIT)
		        	{
			        	for(Client_UDP client : ClientList)
			    		{
			    			client.Remove();
			    		}
			        	this.ClientList.clear();
		        	}
		        	
		        	//Add new UDP client
		        	this.ClientList.push(new Client_UDP(this, packet));
		        }	    	
		    }
		    catch(SocketTimeoutException ex)
	    	{
	    	}
		    catch (IOException ex)
			{
				System.err.println("UDP Server I/O Exception: " + ex.getMessage());
			}
		    catch (Exception ex)
		    {
		        System.err.println("UDP Server Error: " + ex.getMessage());
		    }
		}
	}
	
	//Sends a packet of UDP (should only be sent from UDP clients)
	public boolean Send(DatagramPacket packet)
	{
		try
		{
			this.socket.send(packet);
		} 
		catch (Exception ex) 
		{
			return false;
		}
		return true;
	}
}

//A TCP server
class Server_TCP extends Server_All
{
	private ServerSocket socket = null;
	
	Server_TCP(ServerSocket socket)
	{
		this.socket = socket;
		try
		{
			this.socket.setSoTimeout(define.TCP_SERVER_TIMEOUT);
		}
		catch (SocketException ex)
		{
			System.err.println("Unable to create TCP socket timeout: " + ex.getMessage());
		}
	}

	protected void DoServer()
	{
		LinkedList<Thread> ThreadsList = new LinkedList<Thread>();
		while (true)
		{
			try
			{
				//Look For new clients
				Socket socket = this.socket.accept();
				//define.println("TCP Client Connected (" + socket.getInetAddress().toString().substring(1) + ":" + socket.getPort() + ")");
				
				//Deal with Client in new Thread, contained in ThreadsList
				boolean truefalse = false;
				//Check ThreadsList for a terminated thread
				for(Thread A : ThreadsList)
				{
					if(A.getState() == State.TERMINATED)
					{
						A = new Thread(new Client_TCP(socket));
						A.start();
						truefalse = true;
						break;
					}
				}
				//Create new thread if no terminated thread in thread list
				if(!truefalse && ThreadsList.size() < define.TCP_CLIENT_LIMIT)
				{
					Thread A = new Thread(new Client_TCP(socket));
					A.start();
					ThreadsList.add(A);
				}
			}
			catch (SocketTimeoutException ex)
			{
			}
			catch (IOException ex)
			{
				System.err.println("TCP Server I/O Exception: " + ex.getMessage());
			}
			catch (Exception ex)
			{
				System.err.println("TCP Server Exception: " + ex.getMessage());
			}
		}
	}
}

//A abstract server for starting both UDP and TCP
abstract class Server_All implements Runnable
{
	public void run()
	{
		this.DoServer();
	}
	abstract protected void DoServer();
}

//The main class for starting the program
public class Server
{
	public static void main(String[] args)
	{
		//Default values
		int Port = 0;
		define.Verbose = false;
		
		//checks input arguments
		if(args.length > 0)
		{
			for(String s : args)
			{
				//Check for verbose argument
				if(s.charAt(0) == '-')
				{
					if(s.compareTo("--verbose") == 0 || s.compareTo("-v") == 0)
					{
						define.Verbose = true;
					}
					else
					{
						Port = 0;
						System.err.println("Invalid flag.\nCorrect Use: java Server [Port #]");
						break;
					}
				}
				//Check for port number argument
				else
				{
					try
					{
						Port = Integer.parseInt(s.trim());
					}
					catch (NumberFormatException ex)
					{
						Port = 0;
						System.err.println("Invalid Port Number: " + ex.getMessage() + "\nCorrect Use: java Server [Port #]");
						break;
					}
				}
			}
		}
		//Invalid use
		else
		{
			System.err.println("Incorrect Use.\nCorrect Use: java Server [Port #]");
		}
		//
		if(Port > 0)
		{
			CreateServers(Port);
		}
	}
	private static void CreateServers(int Port)
	{
		try
		{
			//Open a socket on the local port
			SocketAddress localport = new InetSocketAddress(Port);
	
			//Create TCP listening socket
	        ServerSocket tcpserver = new ServerSocket();
	        tcpserver.bind(localport);
	
	        //Create UDP listening socket
	        DatagramSocket udpserver = new DatagramSocket(localport);
	                
	        //Start the server
	        Thread TCP = new Thread(new Server_TCP(tcpserver));
	        Thread UDP = new Thread(new Server_UDP(udpserver));
	        TCP.start();
	        UDP.start();
		}
		catch(Exception ex)
		{
			System.err.println("Server failed to start: " + ex.getMessage());
		}
	}
}
