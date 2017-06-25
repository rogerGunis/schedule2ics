#!/bin/bash

shopt -s expand_aliases

# decode utf-8 string coming back from caldav server (vEvent ics file)
alias urldecode='python -c "import sys, urllib as ul; print ul.unquote_plus(sys.stdin.read().rstrip())"'

fname=$1
if [ ! -f $fname ];then
    echo 'no file found '$fname' exiting'
    exit 1;
fi

grep '^$' "${fname}" || echo >> "${fname}"
file="${fname}"

#Get individual events:
bevents=($(grep -n BEGIN:VEVENT "$file" | cut -d: -f1))
eevents=($(grep -n END:VEVENT "$file" | cut -d: -f1))

ehead=${bevents[0]}
let "ehead -= 1"

bfoot=$(wc -l < $file)
let "bfoot -= ${eevents[-1]}"

for (( i=0; i < ${#bevents[@]}; i++))
do
    head -n $ehead $file > $file-$i.ics
    sed -n ${bevents[$i]},${eevents[$i]}p $file >> $file-$i.ics
    tail -n $bfoot $file >> $file-$i.ics
done

alreadyDeployedEventsArray=($(curl -s -k --user "$CREDENTIALS" -X PROPFIND $URL | tr ';' '\n' | sed -ne 's#.*kochplan\/\([^<]*\)<.*#\1#p' | sed -ne 's#.ics##p' | urldecode  ))

echo "Getting all Entries"
for (( i=0; i < ${#alreadyDeployedEventsArray[@]}; i++))
do
    FROM_GITHUB_CALCULATED_DATE=${alreadyDeployedEventsArray[$i]/_*/}
    ORIGINAL_DATE=$(curl -o - -s -k --user "$CREDENTIALS" -X GET $URL/${alreadyDeployedEventsArray[$i]}.ics | sed -ne 's/DTSTART;VALUE=DATE://p')

    if [ "$ORIGINAL_DATE" = "${FROM_GITHUB_CALCULATED_DATE}" ];then
        curl -s -k --user "$CREDENTIALS" -X DELETE $URL/${alreadyDeployedEventsArray[$i]}
    fi
done

# check for modified events and skip them
eventsWhichAreModified=$(curl -s -k --user "$CREDENTIALS" -X PROPFIND $URL | tr ';' '\n' | sed -ne 's#.*kochplan\/\([^<]*\)<.*#\1#p' | sed -ne 's#.ics##p' | urldecode | tr '\n' '|' | sed -ne 's/|$//p')
test -z "${eventsWhichAreModified}" && eventsWhichAreModified=NONE
missingEvents=($(grep "^UID:" $file-*.ics | egrep -v "($eventsWhichAreModified)" | sed -ne 's/\([^:]*\).*/\1/p' ))

if [ ${#missingEvents[@]} == 0 ];then
    echo 'no new events found'
    exit;
fi

echo ${missingEvents[@]}

echo "Repopulating the calendar with up-to-date events"
    for (( i=0; i < ${#missingEvents[@]}; i++))
    do
        uid=`grep -e "^UID:"  ${missingEvents[$i]} | sed 's/UID://' | tr -d '\n' | tr -d '\r'`

	    echo "Missing Event $(($i+1)) out of ${#bevents[@]} $uid "
        curl -s -k -T $file-$i.ics  --user "$CREDENTIALS" $URL/"${uid}".ics
    done

echo "Done"
