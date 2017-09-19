#!/bin/bash
shopt -s expand_aliases
YEAR=$(date +%Y)
let NEXT_YEAR=YEAR+1 

fname=FeiertageBayern.ics
rm -f ${fname}
rm -f testfile.txt
# curl -s -o $fname https://calendar.google.com/calendar/ical/dbch66ql3ee9tm0sr97hrav20c%40group.calendar.google.com/public/basic.ics
curl -s -o $fname 'http://www.ifeiertage.de/calendar.php?bl=by&o3=kirche&t=dnl'

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

for (( i=0; i < ${#bevents[@]} ; i++))
do
    head -n $ehead $file > $file-$i.ics
    sed -n ${bevents[$i]},${eevents[$i]}p $file >> $file-$i.ics
    tail -n $bfoot $file >> $file-$i.ics

done
for (( i=${#bevents[@]} - 1; i >= 0 ; i--))
do
    INFO=$(egrep "(DTSTART;VALUE=DATE:|SUMMARY|DESCRIPTION)" $file-$i.ics | sed -e "s/2000/$NEXT_YEAR/" | sed -ne 's/.*://p' | tr '\r\n' '-' | sed -ne 's/--$//;s/--/-/p')
    IS_HOLIDAY=$(echo $INFO | grep -q 'kein ' ; echo $?)
    if [ $IS_HOLIDAY = 0 ];then
        continue
    fi
    INFO=$(echo $INFO | sed -e 's/--[^-]*[-]*/-/;s/-$//')
    DOW=$(/bin/date -d ${INFO/-*/} +%u)
    CURRENT_YEAR=$(/bin/date -d ${INFO/-*/} +%Y)
    HUMAN_DATE=$(/bin/date -d ${INFO/-*/} "+%d.%m.%Y")

    if [ $CURRENT_YEAR != $YEAR -a $CURRENT_YEAR != $NEXT_YEAR ];then
        continue;
    fi
    
    if [ $DOW = 6 -o $DOW = 7 ];then
        # sun, sat skipping
        continue
    fi
    BRUECKE=""
    if [ $DOW = 2 ];then
        BRUECKE="-1day"
    fi
    if [ $DOW = 4 ];then
        BRUECKE="+1day"
    fi
    if [ -n "$BRUECKE" ];then
        BRUECKE_DATE=$(/bin/date -d "${INFO/-*/}${BRUECKE}" "+%d.%m.%Y")
    fi
    echo "$HUMAN_DATE,$HUMAN_DATE,${INFO/*-/},0" >> testfile.txt
    if [ -n "$BRUECKE" ];then
        echo "$BRUECKE_DATE,$BRUECKE_DATE,geschlossen wegen Brückentag,1" >> testfile.txt
    fi
    echo ${INFO}-${DOW} | egrep "^($YEAR|$NEXT_YEAR)"
done | sort -r

rm -f FeiertageBayern.ics
rm -f FeiertageBayern.ics-*
