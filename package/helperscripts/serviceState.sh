#!/bin/bash

for element in "${@:1}"
do
	service $element status &> /dev/null
	if [ $? -eq 0 ]; then
		echo $element
	fi	
done
