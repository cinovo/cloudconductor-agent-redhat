#!/bin/bash

createArray() {
        IFS='; ' read -a $1 <<< "$2"
}

while getopts y:d:i:u: OPT; do
	case "$OPT" in
		d)
			if [[ ! -z "$OPTARG" ]]; then
				createArray DELETE $OPTARG
			fi 
			;;
 		i)
			if [[ ! -z "$OPTARG" ]]; then
				createArray INSTALL $OPTARG
			fi 
			;;
		u)
			if [[ ! -z "$OPTARG" ]]; then
				createArray UPDATE $OPTARG
			fi 
			;;
		y)
			if [[ ! -z "$OPTARG" ]]; then
				REPO="$OPTARG"
			fi 
			;;
		\?)
			echo "Unknow command!" 1>&2
			exit 1	
			;;
    esac
done

eoe() {
	if [ $? -gt 0 ]; then
		echo -e "\e[01;31mStopping current execution! $1 \e[0m" 1>&2
	fi
}

grepEchoDef() {
	local msg=$(grep "$2" errorlog)
        if [ ! -z "$msg" ]; then
        	echo "$1 : $msg" 1>&2
        fi
}

grepecho() {	
	grepEchoDef $1 "No package *"
	grepEchoDef $1 "No Match for argument: *"
	grepEchoDef $1 "Package \<.*\> already installed"
}

if [ -z "$REPO" ]; then
	echo "No repo was given" 1>&2
	exit 1;
fi

##clean yum
yum -q --enablerepo=$REPO clean all
eoe "Yum clean all failed."

## delete 
for element in "${DELETE[@]}"
do
	delete_str="$delete_str $element"
done

if [ ! -z "$delete_str" ]; then
	yum -y --enablerepo=$REPO remove $delete_str &>log/yumError.log
	grepecho "DELETE"
fi

## install
for element in "${INSTALL[@]}"
do
        install_str="$install_str $element"
done
if [ ! -z "$install_str" ]; then
	yum -y --enablerepo=$REPO install $install_str &>log/yumError.log
	grepecho "INSTALL"
fi

## update
for element in "${UPDATE[@]}"
do
        update_str="$update_str $element"
done
if [ ! -z "$update_str" ]; then
	yum -y --enablerepo=$REPO update $update_str &>log/yumError.log
	grepecho "UPDATE"
fi

#rm -f errorlog
exit 0;
