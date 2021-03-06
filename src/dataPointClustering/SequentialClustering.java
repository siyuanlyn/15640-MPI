package dataPointClustering;
import io.IO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lib.Point;


public class SequentialClustering {
	
	private List<Point> dataPoints;
	private int clusterNum;
	private List<Point> centroids;
	private List<Point> oldCentroids;
	double minX = Double.MAX_VALUE;
	double maxX = Double.MIN_VALUE;
	double minY = Double.MAX_VALUE;
	double maxY = Double.MIN_VALUE;
	Random rand;
	
	
	public SequentialClustering(List<Point> dataPoints, int clusterNum){
		this.dataPoints = dataPoints;
		this.clusterNum = clusterNum;
	}
	
	public void init(){
		// generate the initial centroids
		for(Point p : dataPoints){
			minX = Math.min(minX, p.getX());
			maxX = Math.max(maxX, p.getX());
			minY = Math.min(minY, p.getY());
			maxY = Math.max(maxY, p.getY());
		}
		
		rand = new Random(System.currentTimeMillis());
		centroids = new ArrayList<Point>();
		oldCentroids = new ArrayList<Point>();
		for(int i=0; i<clusterNum; i++){
			double x = minX + (maxX - minX) * rand.nextDouble();
			double y = minY + (maxY - minY) * rand.nextDouble();
			Point centroid = new Point(x, y);
			centroids.add(centroid);
		}
	}
	
	public List<List<Point>> cluster(){
		List<List<Point>> clusters = new ArrayList<List<Point>>();
		for(int i=0; i<clusterNum; i++){
			clusters.add(new ArrayList<Point>());
		}
		while(!oldCentroids.equals(centroids)){
			oldCentroids.clear();
			for(Point p : centroids){
				oldCentroids.add(p.clone());
			}
			for(int j=0; j<clusters.size(); j++){
				clusters.get(j).clear();
			}
			
			for(Point p : dataPoints){
				int clusterIdx = calculateClusterIndex(p);
				clusters.get(clusterIdx).add(p);
			}
			
			reCalculateMean(clusters);
		}
		return clusters;
	}
	
	public int calculateClusterIndex(Point p){
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
	
	public void reCalculateMean(List<List<Point>> clusters){
		for(int i=0; i<centroids.size(); i++){
			if(clusters.get(i).size() == 0){
				double x = minX + (maxX - minX) * rand.nextDouble();
				double y = minY + (maxY - minY) * rand.nextDouble();
				centroids.get(i).setX(x);
				centroids.get(i).setY(y);
				continue;
			}
			double sumX = 0;
			double sumY = 0;
			for(Point p : clusters.get(i)){
				sumX += p.getX();
				sumY += p.getY();
			}
			centroids.get(i).setX(sumX/clusters.get(i).size());
			centroids.get(i).setY(sumY/clusters.get(i).size());
		}
	}
	
	public static void main(String[] args){
		if(args.length != 3){
			System.err.println("wrong input parameters");
			return;
		}
		String inputFilePath = args[0];
		String outputFilePath = args[1];
		int clusterNum = Integer.parseInt(args[2]);
		List<Point> points = IO.parseInput(inputFilePath);
		SequentialClustering dataPointClustering = new SequentialClustering(points, clusterNum);
		dataPointClustering.init();
		List<List<Point>> res = dataPointClustering.cluster();
		IO.writeOut(res, outputFilePath);
	}
}
