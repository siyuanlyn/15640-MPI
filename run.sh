#!/bin/bash
cluster_num=$1
p_per_cluster=$2
echo *****************GENERATING $p_per_cluster INPUT POINTS EACH IN $cluster_num CLUSTERS*****************
python ./DataGeneratorScripts/randomclustergen/generaterawdata.py -c $cluster_num -p $p_per_cluster -o ./DataGeneratorScripts/input/cluster.csv
rm -rf ./bin/*
mpijavac ./src/*/*.java -d ./bin
cd bin
echo ---------- Parallel Data Point Clustering ----------
start=$(date +'%s')
mpirun -np 4 java dataPointClustering/ParallelClustering ../DataGeneratorScripts/input/cluster.csv ../DataGeneratorScripts/output/parallel_dataPoint_output.txt $cluster_num 
end=$(date +'%s')
diff=$(( $end - $start))
echo "parallel data point clustering took $diff seconds"
echo ---------- Sequential Data Point Clustering ----------
start=$(date +'%s')
java dataPointClustering/SequentialClustering ../DataGeneratorScripts/input/cluster.csv ../DataGeneratorScripts/output/senquential_dataPoint_output.txt $cluster_num
end=$(date +'%s')
diff=$(( $end - $start))
echo "sequential data point clustering took $diff seconds"
