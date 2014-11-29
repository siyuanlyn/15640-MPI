#!/bin/bash
cluster_num=$1
p_per_cluster=$2
echo *****************GENERATING $p_per_cluster INPUT POINTS EACH IN $cluster_num CLUSTERS*****************
python ./DataGeneratorScripts/randomclustergen/generaterawdata.py -c $cluster_num -p $p_per_cluster -o ./DataGeneratorScripts/input/cluster.csv
rm -rf ./bin/*
mpijavac ./src/*/*.java -d ./bin
cd bin
mpirun -np 3 java dataPointClustering/ParallelClustering ../DataGeneratorScripts/input/cluster.csv ../DataGeneratorScripts/output/output.txt $cluster_num 
