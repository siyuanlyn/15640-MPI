package io;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import lib.Point;


public class IO {
	
	public static List<Point> parseInput(String path){
		File inputFile = new File(path);
		List<Point> points = new ArrayList<Point>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
			String line;
			while((line = in.readLine()) != null){
				String[] coordinates = line.split(",");
				if(coordinates.length != 2){
					System.err.println("input line format error");
					continue;
				}
				points.add(new Point(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1])));
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return points;
	}
	
	public static void writeOut(List<List<Point>> points, String path){
		File outputFile = new File(path);
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<points.size(); i++){
				List<Point> cluster = points.get(i);
				for(Point p : cluster){
					sb.append(p.toString() + ", " + (i+1) + "\n");
				}
			}
			out.write(sb.toString());
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
