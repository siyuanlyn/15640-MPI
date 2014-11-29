package test;
import mpi.MPI;
import mpi.MPIException;
import mpi.Status;


public class Hello {
	public static void main(String[] args) throws MPIException{
		MPI.Init(args);
		int myrank = MPI.COMM_WORLD.Rank();
		if(myrank == 0){
			char[] msg = "Hello, there".toCharArray();
			MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, 1, 99);
		} else {
			char[] msg = new char[20];
			Status status = MPI.COMM_WORLD.Recv(msg, 0, 20, MPI.CHAR, 0, 99);
			System.out.println("received: " + new String(msg) + ":");
			System.out.println("getCount: " + status.Get_count(MPI.CHAR));
			System.out.println("getElement: " + status.Get_elements(MPI.CHAR));
		}
		MPI.Finalize();
	}
}
