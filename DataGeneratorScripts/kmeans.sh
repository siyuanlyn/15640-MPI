#!/bin/bash

b=3

k=5

echo ********GENERATING $b INPUT POINTS EACH IN $k CLUSTERS
python ./randomclustergen/generaterawdata.py -c $k -p $b -o input/cluster.csv
