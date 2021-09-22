package wq;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GrafoAmiciRMIimpl extends UnicastRemoteObject implements GrafoAmiciRMI {
									
											//VARIABILI

	private static final long serialVersionUID = -5155330967742220595L;
	private  ConcurrentHashMap<String,Allegato> nodi;
						
								///////////////////////////////////////
										//COSTRUTTORE


	/**
	 * Costruttore con argomenti
	 */

	public GrafoAmiciRMIimpl(ConcurrentHashMap<String,Allegato> x)throws RemoteException {

		this.nodi = x;
	}
	
	
	
	
									///////////////////////////////
												//METODI
	 /**
  	  * @return Stringa contenente l'esito dell'operazione		   
     */
	  public String registraUtente(String nome,String pass,InetAddress address,int porta) throws RemoteException 
	  {	
	
		  String esito="errore";
		  
		  if(nome==null||pass==null)
		  
			  esito="nome/password = null";
		  
		 
		  else  if(!this.nodi.containsKey(nome) ) 
		  {
			  Allegato allegato=new Allegato();
			  allegato.setPass(pass);
			  
			  if(this.addNodo(nome,allegato,address,porta)) 
			 
				  esito="effettua login";
				  
	
		  }
		  
		   else if(this.nodi.containsKey(nome))
			 
			   esito="Utente registrato";
		  
		  
		  return esito;
		}//fine registraUtente

	
	  
	  public boolean addNodo(String x,Allegato a,InetAddress address,Integer porta) throws RemoteException {
			
		  boolean bool=false;
		  if(!(this.nodi.containsKey(x))) {	
	      Set<Arco> lista = new HashSet<Arco>();
	      a.setArco(lista);
	      a.setAddress(address);
	      a.setPorta(porta);
	      
	      this.nodi.putIfAbsent(x, a);
	      bool=true;
	    }
	 return bool;
	  }//fine addNodo
	
	
}
