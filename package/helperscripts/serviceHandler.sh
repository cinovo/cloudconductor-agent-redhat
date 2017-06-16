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
			echo "Unknow command!" 1>&2
			exit 1	
			;;
    esac
done

prErr() {
	if [ $? -gt 0 ]; then
		echo "$1 of element $2 failed." 1>&2
	fi
}

which systemcl

if [ $? -eq 1 ]; then
    ## handle restart
    for element in "${RESTART[@]}"
    do
        service $element restart &> /dev/null
        if [ $? -gt 0 ]; then
            echo "RESTART of $element failed. Attemp to stop and start seperately." 1>&2
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
else
    ## handle restart
    for element in "${RESTART[@]}"
    do
        systemctl restart $element &> /dev/null
        if [ $? -eq 5 ]; then
            service $element restart &> /dev/null
        fi
        if [ $? -gt 0 ]; then
            echo "RESTART of $element failed. Attemp to stop and start seperately." 1>&2
            systemctl stop $element &> /dev/null
            if [ $? -eq 5 ]; then
                service $element stop &> /dev/null
            fi
            prErr "STOPPING" $element
            systemctl start $element &> /dev/null
            if [ $? -eq 5 ]; then
                service $element start &> /dev/null
            fi
            prErr "STARTING" $element
        fi
    done

    ##handle stop
    for element in "${STOP[@]}"
    do
        systemctl stop $element &> /dev/null
        if [ $? -eq 5 ]; then
                service $element stop &> /dev/null
        fi
        prErr "STOPPING" $element
    done

    ##handle start
    for element in "${START[@]}"
    do
        systemctl start $element &> /dev/null
        if [ $? -eq 5 ]; then
                service $element start &> /dev/null
        fi
        prErr "STARTING" $element
    done
fi