#!/bin/bash
shopt -s expand_aliases
YEAR=$(date +%Y)

fname=FeiertageBayern.ics
rm -f ${fname}
curl -s -o $fname https://calendar.google.com/calendar/ical/dbch66ql3ee9tm0sr97hrav20c%40group.calendar.google.com/public/basic.ics

# decode utf-8 string coming back from caldav server (vEvent ics file)
alias urldecode='python -c "import sys, urllib as ul; print ul.unquote_plus(sys.stdin.read().rstrip())"'

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
    INFO=$(egrep "(DTSTART;VALUE=DATE:|SUMMARY)" $file-$i.ics | sed -ne 's/.*://p' | tr '\r\n' '-' | sed -ne 's/--$//p')
    echo $INFO | grep ^$YEAR
done

