#!/bin/bash

b=5

k=2

echo ********GENERATING $b INPUT POINTS EACH IN $k CLUSTERS
python ./randomclustergen/generaterawdata.py -c $k -p $b -o input/cluster.csv
