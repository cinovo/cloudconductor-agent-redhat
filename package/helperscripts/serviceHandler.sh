#!/bin/bash

createArray() {
        IFS='; ' read -a $1 <<< "$2"
}

# r=run/start s=stop u=update/reload

while getopts r:s:u: OPT; do
	case "$OPT" in
		r)
			if [[ ! -z "$OPTARG" ]]; then
				createArray START $OPTARG
			fi 
			;;
 		s)
			if [[ ! -z "$OPTARG" ]]; then
				createArray STOP $OPTARG
			fi 
			;;
		u)
			if [[ ! -z "$OPTARG" ]]; then
				createArray RESTART $OPTARG
			fi 
			;;		
		\?)
			echo "Unknown command!" 1>&2
			exit 1	
			;;
    esac
done

prErr() {
	if [ $? -gt 0 ]; then
		echo "$1 of element $2 failed." 1>&2
	fi
}

## handle restart
for element in "${RESTART[@]}"
do
	service $element restart &> /dev/null
	if [ $? -gt 0 ]; then
        echo "RESTART of $element failed. Attempt to stop and start separately." 1>&2
        service $element stop &> /dev/null
        prErr "STOPPING" $element
        service $element start &> /dev/null
		prErr "STARTING" $element
	fi
done

##handle stop
for element in "${STOP[@]}"
do
    service $element stop &> /dev/null
	prErr "STOPPING" $element
done

##handle start
for element in "${START[@]}"
do
    service $element start &> /dev/null
	prErr "STARTING" $element
done