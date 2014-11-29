package dataPointClustering;
import io.IO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lib.Point;
import mpi.MPI;
import mpi.MPIException;
import mpi.Status;

public class ParallelClustering {

	final static int TAG_CENTROIDS_X = 0;
	final static int TAG_CENTROIDS_Y = 1;
	final static int TAG_SPLITS_X = 2;
	final static int TAG_SPLITS_Y = 3;
	final static int TAG_BUFFER_SIZE = 4;
	final static int TAG_JOB_FINISHED = 5;
	
	final static int MASTER_RANK = 0;
	
	final static boolean DEBUG = true;
	
	private static String inputFilePath;
	private static String outputFilePath;
	private static int myrank;
	private static int slaveNum;
	
	static double minX = Double.MAX_VALUE;
	static double maxX = Double.MIN_VALUE;
	static double minY = Double.MAX_VALUE;
	static double maxY = Double.MIN_VALUE;
	
	static Random rand;
	
	public static void init(String[] args){
		inputFilePath = args[0];
		if(DEBUG) System.out.println("Master: input path: " + inputFilePath);
		outputFilePath = args[1];
		if(DEBUG) System.out.println("Master: input path: " + outputFilePath); 
	}
	
	public static void slave(){
		// try to get the buffer size first
		int bufferSize = 0;
		try {
			bufferSize = getBufferSizeFromMaster();
			if(bufferSize == -1){
				throw new MPIException();
			}
		} catch (MPIException e1) {
			System.err.println("fail to get buffer size from master");
			e1.printStackTrace();
		}
		double[] buffer = new double[bufferSize];
		try {
			while(true){
				boolean[] jobFinished = new boolean[1];
				MPI.COMM_WORLD.Recv(jobFinished, 0, 1, MPI.BOOLEAN, MASTER_RANK, TAG_JOB_FINISHED);
				if(jobFinished[0]){
					break;
				}
				// receive centroids from master
				List<Point> centroids = getCentroidsFromMaster(buffer);
				// receive splits from master
				List<Point> split = getSplitFromMaster(buffer);
				// clustering
				List<List<Point>> clusters = cluster(centroids, split);
				// send result back to master
				sendResultToMaster(clusters);
			}
		} catch (MPIException e) {
			System.err.println("fail to get job finishes msg from master");
			e.printStackTrace();
		}
	}
	
	public static void sendResultToMaster(List<List<Point>> clusters) throws MPIException{
		if(DEBUG) System.out.println("Rank: " + myrank + " send " + clusters.size() + " clusters to master");
		for(int i=0; i<clusters.size(); i++){
			List<Point> cluster = clusters.get(i);
			double[] x = new double[cluster.size()];
			double[] y = new double[cluster.size()];
			for(int j=0; j<cluster.size(); j++){
				x[j] = cluster.get(j).getX();
				y[j] = cluster.get(j).getY(); 
			}
			MPI.COMM_WORLD.Send(x, 0, x.length, MPI.DOUBLE, MASTER_RANK, TAG_SPLITS_X);
			MPI.COMM_WORLD.Send(y, 0, y.length, MPI.DOUBLE, MASTER_RANK, TAG_SPLITS_Y);
		}
	}
	
	public static List<List<Point>> cluster(List<Point> centroids, List<Point> split){
		List<List<Point>> clusters = new ArrayList<List<Point>>();
		for(int i=0; i<centroids.size(); i++){
			clusters.add(new ArrayList<Point>());
		}
		for(Point p : split){
			int clusterIdx = calculateClusterIndex(centroids, p);
			clusters.get(clusterIdx).add(p);
		}
		return clusters;
	}
	
	public static int calculateClusterIndex(List<Point> centroids, Point p){
		int index = -1;
		double minDistance = Double.MAX_VALUE;
		for(int i=0; i<centroids.size(); i++){
			Point centroid = centroids.get(i);
			double euclideanDistance = Math.sqrt(Math.pow(p.getX() - centroid.getX(), 2) + Math.pow(p.getY() - centroid.getY(), 2));
			if(euclideanDistance < minDistance){
				minDistance = euclideanDistance;
				index = i;
			}
		}
		return index;
	}
	
	public static List<Point> getSplitFromMaster(double[] buffer) throws MPIException{
		if(DEBUG) System.out.println("Rank: " + myrank + " get split from master");
		List<Point> split = new ArrayList<Point>();
		Status status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.DOUBLE, MASTER_RANK, TAG_SPLITS_X);
		double[] split_x = new double[status.Get_count(MPI.DOUBLE)];
		System.arraycopy(buffer, 0, split_x, 0, split_x.length);
		status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.DOUBLE, MASTER_RANK, TAG_SPLITS_Y);
		double[] split_y = new double[status.Get_count(MPI.DOUBLE)];
		System.arraycopy(buffer, 0, split_y, 0, split_y.length);
		if(split_x.length != split_y.length){
			System.err.println("centroids from master have dimention mismatch");
		}
		for(int i=0; i<split_x.length; i++){
			split.add(new Point(split_x[i], split_y[i]));
		}
		if(DEBUG) System.out.println("Rank: " + myrank + " split size: " + split.size());
		return split;
	}
	
	public static List<Point> getCentroidsFromMaster(double[] buffer) throws MPIException{
		if(DEBUG) System.out.println("Rank: " + myrank + " get centroids from master");
		List<Point> centroids = new ArrayList<Point>();
		Status status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.DOUBLE, MASTER_RANK, TAG_CENTROIDS_X);
		double[] centroid_x = new double[status.Get_count(MPI.DOUBLE)];
		System.arraycopy(buffer, 0, centroid_x, 0, centroid_x.length);
		status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.DOUBLE, MASTER_RANK, TAG_CENTROIDS_Y);
		double[] centroid_y = new double[status.Get_count(MPI.DOUBLE)];
		System.arraycopy(buffer, 0, centroid_y, 0, centroid_y.length);
		if(centroid_x.length != centroid_y.length){
			System.err.println("centroids from master have dimention mismatch");
		}
		for(int i=0; i<centroid_x.length; i++){
			centroids.add(new Point(centroid_x[i], centroid_y[i]));
		}
		return centroids;
	}
	
	public static int getBufferSizeFromMaster() throws MPIException{
		int[] bufferSize = new int[1];
		MPI.COMM_WORLD.Recv(bufferSize, 0, 1, MPI.INT, MASTER_RANK, TAG_BUFFER_SIZE);
		if(DEBUG) System.out.println("Rank: " + myrank + " get buffer size from master: " + bufferSize[0]);
		return bufferSize[0];
	}
	
	public static void master(String[] args) {
		// initialize the input file path and output file path
		init(args);
		// import the random generated coordinates
		List<Point> rawDataPoints = IO.parseInput(inputFilePath);
		// build centroids
		int clusterNum = Integer.parseInt(args[2]);
		if(DEBUG) System.out.println("clusterNUM: " + clusterNum);
		List<Point> centroids = genCentroids(clusterNum, rawDataPoints);
		List<Point> oldCentroids = new ArrayList<Point>();
		// create buffer
		double[] buffer = new double[rawDataPoints.size()];
		// send buffer size to slaves
		try {
			sendBufferSizeToSlaves(clusterNum, buffer.length);
		} catch (MPIException e2) {
			System.err.println("fail to send buffer size to slaves");
			e2.printStackTrace();
		}
		// split chunks
		List<List<Point>> splits = split(clusterNum, rawDataPoints);
		List<List<Point>> res = null;
		while(!oldCentroids.equals(centroids)){
			// send job continue message to slaves
			try {
				jobFinished(clusterNum, false);
			} catch (MPIException e1) {
				System.err.println("fail to send job continue msg to slaves");
				e1.printStackTrace();
			}
			// save old centroids
			oldCentroids.clear();
			for(Point centroid : centroids){
				oldCentroids.add(centroid.clone());
			}
			// send centroid to slave
			try {
				sendCentroidsToSlave(clusterNum, centroids);
			} catch (MPIException e) {
				System.err.println("send centroids to slaves failed");
				e.printStackTrace();
			}
			// send splits to slave
			try {
				sendSplitsToSlave(clusterNum, splits);
			} catch (MPIException e) {
				System.err.println("send splits to slaves failed");
				e.printStackTrace();
			}
			// get clustering result from slave
			try {
				res = getResultFromSlave(clusterNum, buffer);
				// recalculate centroids based on results from slaves
				reCalculateCentroids(res, centroids);
			} catch (MPIException e) {
				System.err.println("get clustering result from slaves failed");
				e.printStackTrace();
			}
		}
		// terminate slaves
		try {
			jobFinished(clusterNum, true);
		} catch (MPIException e) {
			System.err.println("fail to send job finishes msg to slaves");
			e.printStackTrace();
		}
		
		// write the result to output file
		if(res != null){
			if(DEBUG) System.out.println("result cluster num: " + res.size());
			IO.writeOut(res, outputFilePath);
		} else {
			System.err.println("no output generated");
		}
		
	}
	
	public static void sendBufferSizeToSlaves(int clusterNum, int bufferSize) throws MPIException{
		for(int slaveID = 1; slaveID <= slaveNum; slaveID++){
			int[] buffer = new int[]{bufferSize};
			MPI.COMM_WORLD.Send(buffer, 0, 1, MPI.INT, slaveID, TAG_BUFFER_SIZE);
			if(DEBUG) System.out.println("send buffer size " + bufferSize + " to slave" + slaveID);
		}
	}

	public static void jobFinished(int clusterNum, boolean jobFinished) throws MPIException{
		for (int slaveID = 1; slaveID <= slaveNum; slaveID++) {
			boolean[] buffer = new boolean[]{jobFinished};
			MPI.COMM_WORLD.Send(buffer, 0, 1, MPI.BOOLEAN, slaveID, TAG_JOB_FINISHED);
		}
	}
	
	public static void reCalculateCentroids(List<List<Point>> res, List<Point> centroids){
		for(int i=0; i<centroids.size(); i++){
			if(res.get(i).size() == 0){
				double x = minX + (maxX - minX) * rand.nextDouble();
				double y = minY + (maxY - minY) * rand.nextDouble();
				centroids.get(i).setX(x);
				centroids.get(i).setY(y);
				continue;
			}
			double sumX = 0;
			double sumY = 0;
			for(Point p : res.get(i)){
				sumX += p.getX();
				sumY += p.getY();
			}
			centroids.get(i).setX(sumX/res.get(i).size());
			centroids.get(i).setY(sumY/res.get(i).size());
		}
	}
	
	public static List<List<Point>> getResultFromSlave(int clusterNum, double[] buffer) throws MPIException{
		List<List<Point>> res = new ArrayList<List<Point>>();
		for(int i=0; i<clusterNum; i++){
			res.add(new ArrayList<Point>());
		}
		
		for(int slaveID=1; slaveID<=slaveNum; slaveID++){
			if(DEBUG) System.out.println("receive clustering result from slave" + slaveID);
			for(int i=0; i<res.size(); i++){
				Status status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.DOUBLE, slaveID, TAG_SPLITS_X);
				double[] x = new double[status.Get_count(MPI.DOUBLE)];
				System.arraycopy(buffer, 0, x, 0, x.length);
				status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.DOUBLE, slaveID, TAG_SPLITS_Y);
				double[] y = new double[status.Get_count(MPI.DOUBLE)];
				System.arraycopy(buffer, 0, y, 0, y.length);
				if(x.length != y.length){
					System.err.println("result dimension mismatch");
					continue;
				}
				for(int j=0; j<status.Get_count(MPI.DOUBLE); j++){
					res.get(i).add(new Point(x[j], y[j]));
				}
			}
		}
		return res;
	}
	
	public static void sendSplitsToSlave(int clusterNum, List<List<Point>> splits) throws MPIException{
		for(int slaveID=1; slaveID<=slaveNum; slaveID++){
			List<Point> split = splits.get(slaveID-1);
			// convert List<Point> to two double[]
			double[] x = new double[split.size()];
			double[] y = new double[split.size()];
			for(int i=0; i<split.size(); i++){
				Point p = split.get(i);
				x[i] = p.getX();
				y[i] = p.getY();
			}
			// send two arrays to all slaves;
			// tag 2 for x, tag 3 for y
			MPI.COMM_WORLD.Send(x, 0, x.length, MPI.DOUBLE, slaveID, TAG_SPLITS_X);
			MPI.COMM_WORLD.Send(y, 0, y.length, MPI.DOUBLE, slaveID, TAG_SPLITS_Y);
			if(DEBUG) System.out.println("send split to slave" + slaveID + " split size: " + split.size());
		}
	}
	
	public static void sendCentroidsToSlave(int clusterNum, List<Point> centroids) throws MPIException{
		// convert List<Point> to two double[], first for x, second for y
		double[] x = new double[centroids.size()];
		double[] y = new double[centroids.size()];
		for(int i=0; i<centroids.size(); i++){
			Point centroid = centroids.get(i);
			x[i] = centroid.getX();
			y[i] = centroid.getY();
		}
		// send two arrays to all slaves;
		for(int slaveID = 1; slaveID <= slaveNum; slaveID++){
			// tag 0 for x, tag 1 for y
			MPI.COMM_WORLD.Send(x, 0, x.length, MPI.DOUBLE, slaveID, TAG_CENTROIDS_X);
			MPI.COMM_WORLD.Send(y, 0, y.length, MPI.DOUBLE, slaveID, TAG_CENTROIDS_Y);
			if(DEBUG) System.out.println("send centroids to slave" + slaveID);
		}
	}
	
	public static List<List<Point>> split(int clusterNum, List<Point> rawDataPoints){
		if(DEBUG) System.out.println("splitting.. slaveNum: " + slaveNum);
		List<List<Point>> splits = new ArrayList<List<Point>>();
		for(int i=0; i<slaveNum; i++){
			splits.add(new ArrayList<Point>());
		}
		for(int i=0; i<rawDataPoints.size(); i++){
			splits.get(i%slaveNum).add(rawDataPoints.get(i));
		}
		return splits;
	}
	
	public static List<Point> genCentroids(int clusterNum, List<Point> rawDataPoints){
		List<Point> centroids = new ArrayList<Point>();
		for(Point p : rawDataPoints){
			minX = Math.min(minX, p.getX());
			maxX = Math.max(maxX, p.getX());
			minY = Math.min(minY, p.getY());
			maxY = Math.max(maxY, p.getY());
		}
		rand = new Random(System.currentTimeMillis());
		for(int i=0; i<clusterNum; i++){
			double x = minX + (maxX - minX) * rand.nextDouble();
			double y = minY + (maxY - minY) * rand.nextDouble();
			centroids.add(new Point(x, y));
		}
		return centroids;
	}
	
	public static void main(String[] args) throws MPIException{
		if(args.length != 3){
			System.err.println("wrong input parameters");
			return;
		}
		MPI.Init(args);
		myrank = MPI.COMM_WORLD.Rank();
		slaveNum = MPI.COMM_WORLD.Size()-1;
		if(myrank == 0){
			// master node
			master(args);
		} else {
			// slave node
			slave();
		}
		MPI.Finalize();
	}
}
