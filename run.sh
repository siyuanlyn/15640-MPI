#!/bin/bash
rm -rf ./bin/*
mpijavac ./src/*/*.java -d ./bin
cd bin
mpirun -np 3 java dataPointClustering/ParallelClustering ../DataGeneratorScripts/input/cluster.csv ../DataGeneratorScripts/output/output.txt 2
