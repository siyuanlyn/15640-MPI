#!/bin/bash
cluster_num=$1
p_per_cluster=$2
proc_num=$3
if [ $# -ne 3 -o -z "$1" -o -z "$2" -o -z "$3" ]
        then echo "usage: bash run.sh <cluster_num> <points_per_cluster> <process_number>"
        exit
fi
echo *****************GENERATING $p_per_cluster INPUT POINTS EACH IN $cluster_num CLUSTERS*****************
python ./DataGeneratorScripts/randomclustergen/generaterawdata.py -c $cluster_num -p $p_per_cluster -o ./DataGeneratorScripts/input/cluster.csv
rm -rf ./bin/*
mpijavac ./src/*/*.java -d ./bin
cd bin
echo ---------- Parallel Data Point Clustering ----------
start=$(date +'%s%N')/1000000
mpirun -np $proc_num java dataPointClustering/ParallelClustering ../DataGeneratorScripts/input/cluster.csv ../DataGeneratorScripts/output/parallel_dataPoint_output.txt $cluster_num
end=$(date +'%s%N')/1000000
diff=$(($end - $start))
echo "parallel data point clustering took $diff ms"
echo "$(wc -l ../DataGeneratorScripts/output/parallel_dataPoint_output.txt | cut -d' ' -f1) coordinates has been written to output file"
echo ---------- Sequential Data Point Clustering ----------
start=$(date +'%s%N')/1000000
java dataPointClustering/SequentialClustering ../DataGeneratorScripts/input/cluster.csv ../DataGeneratorScripts/output/sequential_dataPoint_output.txt $cluster_num
end=$(date +'%s%N')/1000000
diff=$(($end - $start))
echo "sequential data point clustering took $diff ms"
echo "$(wc -l ../DataGeneratorScripts/output/sequential_dataPoint_output.txt | cut -d' ' -f1) coordinates has been written to output file"
