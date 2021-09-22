package wq;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface GrafoAmiciRMI extends Remote {
	
																			//VARIABILI
	public String SERVICE_NAME = "grafoAmiciService";
																////////////////////////////////////
																			//METODI

	/**
	 * 
	 * @param nome
	 * @param pass
	 * @param address
	 * @param porta
	 * @return 
	 * @throws RemoteException
	 */
	String registraUtente(String nome,String pass,InetAddress address,int porta) throws RemoteException;
	

	
	   
}
