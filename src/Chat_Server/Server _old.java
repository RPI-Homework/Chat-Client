package Chat_Server;

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
	public static void Send(String Username, String address, String message) //Verbose SEND message
	{
		if(Verbose)
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
	
	protected String readLine() throws IOException
	{
		String s = null;
		while(s == null)
		{
			try
			{
				s = fromClient.readLine();
			}
			catch(SocketTimeoutException ex)
			{
				
			}
		}
		return s;
	}
	
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
				this.Send("OK\n");
				return true;
			}
		}
		catch(Exception ex)
		{
			this.Send("ERROR\n");
			return false;
		}
		this.Send("ERROR\n");
		return false;
	}
	public void run()
	{
		if(this.CreateClient())
		{
			while(true)
			{
				//TODO: Determine when a client has dropped
				try 
				{
					//Get Query and Length
					String Query = this.readLine().trim();
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
						//TODO: Implement chunking
						//Chunked send
						define.Recieve(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), 
								Query + "\nC" + message.length() + "\n" + message);
					}
					else
					{
						//Non-chunking send
						define.Recieve(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), 
								Query + "\n" + message.length() + "\n" + message);
						this.Receive(Query, message);
					}
				}
				catch(Exception ex)
				{
					this.Send("Error\n");
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

	protected boolean Send(String s) 
	{
		try
		{
			this.toClient.print(s.toCharArray());
			this.toClient.flush();
			define.Send(this.GetUsername(), this.socket.getInetAddress().toString().substring(1), s);
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
	
	protected String read(int length) throws IOException, Exception
	{
		throw new UnsupportedOperationException();
	}
	protected String readLine() throws IOException
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
			this.Send("Error\n");
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
			this.Send("OK\n");
			//client has authenticated
			if(s.length > 1)
			{
				this.Receive(s[1]);
			}
		}
		else
		{
			this.Send("ERROR\n");
			//Client did not authenticate
		}
	}
	//If this is the client, send the data
	public boolean isClient(DatagramPacket packet)
	{
		//TODO: figure out unique addresses and ports for UDP (Solved?)
		if(this.address.toString().compareTo(packet.getAddress().toString()) == 0 && this.port == packet.getPort())
		{
			this.Receive(packet);
			return true;
		}
		return false;
	}
	
	public void Remove()
	{
		this.address = null;
		this.port = 0;
		this.server = null;
		this.RemoveClient();
	}

	protected boolean Send(String s) 
	{
		try
		{
			DatagramPacket packet = new DatagramPacket(s.getBytes(), s.getBytes().length, this.address, this.port);
			boolean out = this.server.Send(packet);
			if(out)
			{
				define.Send(this.GetUsername(), this.address.toString().substring(1), s);
			}
			return out;
		}
		catch(Exception ex)
		{
			return false;
		}
	}
}

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

class SynchronizedChunking
{
	private long CurrentChunkingID = -1;
	private long RequestChunkingCount = 0;
	private long NonChunkedSendCount = 0;
	
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
	
	protected synchronized void RemoveChunkingID() throws Exception
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
	
	protected synchronized boolean SetChunking()
	{
		RequestChunkingCount++;
		return true;
	}
	
	public synchronized void RemoveChunking()
	{
		RequestChunkingCount--;
	}
	
	protected synchronized boolean SetNonChunking()
	{
		if(this.CurrentChunkingID == -1 && RequestChunkingCount == 0)
		{
			NonChunkedSendCount++;
			return true;
		}
		return false;
	}
	
	public synchronized void RemoveNonChunking()
	{
		NonChunkedSendCount--;
	}
	
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
	
	public void NonChunkingWait()
	{
		while(!this.SetNonChunking());
	}
}

//An abstract class of clients communicating with each other
abstract class Client
{
	private static SynchronizedList<Client> ClientList = new SynchronizedList<Client>();
	private String Username = null;
	private SynchronizedChunking sync = new SynchronizedChunking();
	
	
	//Adds a client to the current list of clients
	protected void AddClient()
	{
		this.ClientList.AddClient(this);
	}
	
	//Removes a client from the current list of clients
	protected void RemoveClient()
	{
		this.ClientList.RemoveClient(this);
	}
	
	//Reads the next line (only works for TCP Client)
	abstract protected String readLine() throws IOException;
	
	//Reads the next few x words (only works for TCP Client)
	abstract protected String read(int length) throws IOException, Exception;
	
	private boolean SentToChunking(Client client, String message)
	{
		if(client != null)
		{
			boolean ret;
			ret = client.Send(message);
			client.sync.RemoveChunking();
			return ret;
		}
		else
		{
			return false;
		}
	}
	
	//Processes a chunking query
	protected void ReceiveChunking(String Query, String Message)
		throws IOException, Exception
	{
		//TODO: Implement chunking
		String s = "FROM " + this.Username + "\n"
		+ Message.length() + "\n" 
		+ Message;
		
		String [] Users = Query.split("\\s+");
		if(Users[0].compareTo("SEND") == 0)
		{
			if(Users.length > 1)
			{
				for(int i = 1; i < Users.length; i++)
				{
					Client client = this.FindUsername(Users[i]);
					client.sync.ChunkingWait();
					this.SendTo(Users[i], s);
				}
			}
			else
			{
				//Incorrect use (Assume BROADCAST?)
				this.SendAll(s);
			}
		}
		else if(Users[0].compareTo("BROADCAST") == 0)
		{
			this.SendAll(s);
		}
		else if(Users[0].compareTo("I AM") == 0)
		{
			this.Send("ERROR\n");
		}
		else
		{
			throw new Exception("Invalid Request (" + Users[0] + ")");
		}
	}
	
	//Processes a non-chunking query
	protected void Receive(String Query, String Message) 
		throws Exception
	{
		String s = "FROM " + this.Username + "\n"
			+ Message.length() + "\n" 
			+ Message;
		
		String [] Users = Query.split("\\s+");
		if(Users[0].compareTo("SEND") == 0)
		{
			if(Users.length > 1)
			{
				for(int i = 0; i < Users.length; i++)
				{
					this.SendTo(Users[i], s);
				}
			}
			else
			{
				//Incorrect use (Assume BROADCAST?)
				this.SendAll(s);
			}
		}
		else if(Users[0].compareTo("BROADCAST") == 0)
		{
			this.SendAll(s);
		}
		else if(Users[0].compareTo("I AM") == 0)
		{
			this.Send("ERROR\n");
		}
		else
		{
			throw new Exception("Invalid Request (" + Users[0] + ")");
		}
	}
	
	//Returns true when the send was successful, false otherwise
	abstract protected boolean Send(String s);
	
	//Gets the current UserName
	public String GetUsername()
	{
		return this.Username;
	}
	
	//Sends a message to all TCP clients
	protected void SendAllTCP(String s)
	{
		for(Client client : ClientList)
		{
			if(client.getClass().toString().compareTo(Client_TCP.class.toString()) == 0)
			{
				client.sync.NonChunkingWait();
				client.Send(s);
				client.sync.RemoveNonChunking();
			}
		}
	}
	
	//Sends a message to all UDP clients
	protected void SendAllUDP(String s)
	{
		for(Client client : ClientList)
		{
			if(client.getClass().toString().compareTo(Client_UDP.class.toString()) == 0)
			{
				client.sync.NonChunkingWait();
				client.Send(s);
				client.sync.RemoveNonChunking();
			}
		}
	}
	
	//Sends a message to all clients
	protected void SendAll(String s)
	{
		for(Client client : ClientList)
		{
			client.sync.NonChunkingWait();
			client.Send(s);
			client.sync.RemoveNonChunking();
		}
	}
	
	//Returns true on a successful send, false otherwise
	protected boolean SendTo(Client client, String s)
	{
		if(client != null)
		{
			boolean ret;
			client.sync.NonChunkingWait();
			ret = client.Send(s);
			client.sync.RemoveNonChunking();
			return ret;
		}
		else
		{
			return false;
		}
	}
	
	//Returns true on a successful send, false otherwise
	protected boolean SendTo(String Username, String s)
	{
		Client client = this.FindUsername(Username);
		return this.SendTo(client, s);
	}
	
	//Find a user in the current list
	protected Client FindUsername(String Username)
	{
		for(Client client : ClientList)
		{
			if(client.Username.compareTo(Username) == 0)
			{
				return client;
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
		        
		        if(!HasClient(packet))
		        {
		        	//define.println("UDP Client Connected (" + packet.getAddress().toString().substring(1) + ":" + packet.getPort() + ")");
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
				if(!truefalse)
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
		int Port = 8000;
		define.Verbose = true;
		
		if(args.length > 0)//If port number is provided
		{
			for(String s : args)
			{
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
		else//Generate own port number
		{
			System.err.println("No port number provided.\nCorrect Use: java Server [Port #]");
		}
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
