#!/usr/bin/env bash
  
# Exit on error. Append "|| true" if you expect an error.
set -o errexit
# Exit on error inside any functions or subshells.
set -o errtrace
# Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
set -o nounset
# Catch the error in case mysqldump fails (but gzip succeeds) in `mysqldump |gzip`
set -o pipefail
# Catch line number trace function which called error
set -eE -o functrace

shopt -s expand_aliases
 
# remove echoing directory name on 'cd' command
unset CDPATH
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)" # the full path of the directory where the script resides
readonly SCRIPT_PATH="${SCRIPT_DIR}/$(basename "${BASH_SOURCE[0]}")"     # full path including script name
readonly SCRIPT_NAME="$(basename "${SCRIPT_PATH}" .sh)"                # script name without path and also without file extension
readonly HOST_NAME=$(hostname)
readonly LOG_PREFIX='echo "# [${HOST_NAME}:${SCRIPT_NAME} $(date +'"'"'%Y-%m-%d %H:%M:%S'"'"')]"'
readonly LOG_LEVEL_ERROR=0
readonly LOG_LEVEL_WARN=1
readonly LOG_LEVEL_INFO=2
readonly LOG_LEVEL_DEBUG=3
 
LOG_LEVEL=LOG_LEVEL_INFO
EXAMPLE=""
 
 
debug() {
    (( LOG_LEVEL >= LOG_LEVEL_DEBUG )) && >&2 echo -e "$(eval "${LOG_PREFIX}") DEBUG: ${*}" || true
}
 
info() {
    (( LOG_LEVEL >= LOG_LEVEL_INFO)) && >&2 echo -e "$(eval "${LOG_PREFIX}") \e[32mINFO: \e[39m ${*}" || true
}
 
warn() {
    (( LOG_LEVEL >= LOG_LEVEL_WARN)) && >&2 echo -e "$(eval "${LOG_PREFIX}") \e[33mWARN: \e[39m ${*}" || true
}
 
error() {
    (( LOG_LEVEL >= LOG_LEVEL_ERROR )) && >&2 echo -e "$(eval "${LOG_PREFIX}") \e[31mERROR:\e[39m ${*}" || true
}
 
onExit() {
  # your cleanup code here ...
  debug "clean up"
}

onError() {
  local errorCode=$1
  local lineno=$2
  info "Exiting with errors <$errorCode>, lineNumber <$lineno>"
}
  
#cat errors and jump into that function
trap 'onError $? ${LINENO}' ERR

#exit traps in any case, also in case of ERR
trap 'onExit' EXIT
 
parseArguments() {
    for i in "$@"; do
        case $i in
            --year=*)
            setYear "${i#*=}"
            shift
            ;;
            --help|-h)
            printUsageAndExit
            ;;
            *)
            error "Unknown option: ${i}"
            printUsageAndExit
            ;;
        esac
    done
}
 
setYear() {
   YEAR="${1}"
}
 
 
getYear() {
   echo "${YEAR:-$(date +%Y)}"
}

getNextYear() {
	echo $(($(getYear)+1))
}
    
 
printUsageAndExit() {
{ IFS="" read -r -d '' usageText << EOT
usage: ${SCRIPT_NAME} --year=[YYYY]
  example:
     ./${SCRIPT_NAME} --year=2022
EOT
} || /bin/true
    info "\n${usageText}"
    exit 1
}

parseIcsFileEchoDates() {
    fname="${1}"

    local calendarFile=testfile.txt

    #Get individual events:
    bevents=($(grep -n BEGIN:VEVENT "${fname}" | cut -d: -f1))
    eevents=($(grep -n END:VEVENT "${fname}" | cut -d: -f1))
    
    ehead=${bevents[0]}
    let "ehead -= 1"
    
    bfoot=$(wc -l < ${fname})
    let "bfoot -= ${eevents[-1]}"
    
    for (( i=0; i < ${#bevents[@]} ; i++))
    do
        head -n $ehead ${fname} > ${fname}-$i.ics
        sed -n ${bevents[$i]},${eevents[$i]}p ${fname} >> ${fname}-$i.ics
        tail -n $bfoot ${fname} >> ${fname}-$i.ics
    
    done
    for (( i=${#bevents[@]} - 1; i >= 0 ; i--))
    do
        INFO=$(egrep "(DTSTART;TZID|SUMMARY|DESCRIPTION)" ${fname}-$i.ics  | sed -e 's/DTS/1/;s/SUMM/2/' | sort -n | sed -ne 's/.*://p' | grep -v '^/' | tr '\r\n' '-' | tr '\n' '-'  | tr '\r' '-' | sed -e 's#^-##g;s#-$##g')
        echo $INFO | grep -q 'kein ' && IS_HOLIDAY=$? || IS_HOLIDAY=$?

        if [ $IS_HOLIDAY = 0 ];then
			warn "skipping holiday ${INFO}"
            continue
        fi
        DESC=$(echo $INFO | sed -e 's/[^-]*[-]*/-/;s/-$//;s/^-//')
        # INFO=$(echo $INFO | sed -e 's/[^-]*[-]*/-/;s/-$//')
        DOW=$(/bin/date -d ${INFO/-*/} +%u)
        CURRENT_YEAR=$(/bin/date -d ${INFO/-*/} +%Y)
        HUMAN_DATE=$(/bin/date -d ${INFO/-*/} "+%d.%m.%Y")
        HUMAN_DATE_SORTABLE=$(/bin/date -d ${INFO/-*/} "+%Y%m%d")
    
        if [ $CURRENT_YEAR != $(getYear) -a $CURRENT_YEAR != $(getNextYear) ];then
			warn "skipping year ${INFO} ${CURRENT_YEAR}"
            continue;
        fi
        
        if [ $DOW = 6 -o $DOW = 7 ];then
            # sun, sat skipping
			warn "skipping day (sa,so) ${INFO}"
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
            BRUECKE_DATE_SORTABLE=$(/bin/date -d "${INFO/-*/}${BRUECKE}" "+%Y%m%d")
        fi
        echo "$HUMAN_DATE_SORTABLE,$HUMAN_DATE,$HUMAN_DATE,${INFO/*-/},0" >> ${calendarFile}
        if [ -n "$BRUECKE" ];then
            echo "$BRUECKE_DATE_SORTABLE,$BRUECKE_DATE,$BRUECKE_DATE,geschlossen wegen Brückentag,1" >> ${calendarFile}
        fi
    done | sort -r
    
    sort ${calendarFile} | cut -d\, -f2,3,4,5,6 > ${calendarFile}_tmp
    cat ${calendarFile}_tmp | cut -d\, -f1,2,3
    
    rm -f ${fname}
    rm -f ${fname}-*
    rm -f ${calendarFile}
}
 
main() {
    # info "SCRIPT_DIR is ${SCRIPT_DIR}"
    # info "SCRIPT_PATH is ${SCRIPT_PATH}"
    # info "SCRIPT_NAME is ${SCRIPT_NAME}"
    # info "BASH_SOURCE is ${BASH_SOURCE[@]}"
    parseArguments "$@"

    urlName="feiertage_bayern_${YEAR}.ics"
    # urlName="schulferien_bayern_${YEAR}.ics"
    # urlName="schulferien_baden-wuerttemberg_${YEAR}.ics"
    fname=$(basename 'http://de-kalender.de/downloads/'${urlName})
    curl -s -o $fname 'http://de-kalender.de/downloads/'${urlName}
    
    if [ ! -f $fname ];then
        error 'no file found '$fname' exiting'
        exit 1;
    fi

	# preformat file
	grep '^$' "${fname}" || echo >> "${fname}"

	parseIcsFileEchoDates ${fname}
}
 
main "$@"

