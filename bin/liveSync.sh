#!/bin/bash


MAIN_CLS="org.dbpedia.extraction.live.mirror.LiveSync"
MVN="mvn"

last_update=$(head -1 /appli/databus-data/last_update.txt)

echo $last_update"-00-000000" > ../lastDownloadDate.dat;
echo "========================="
$MVN install exec:java -q -Dexec.mainClass="$MAIN_CLS" -Dexec.args="$*"

