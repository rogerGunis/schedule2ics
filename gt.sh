#!/bin/bash

for i in $(grep -A1 '# G' Workers.csv  | grep -v '#' | cut -d, -f1); do 
    echo "\$(call geschwistertag,1,$i)";
done
