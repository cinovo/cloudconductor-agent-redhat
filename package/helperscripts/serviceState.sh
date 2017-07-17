#!/bin/bash
which systemctl

if [ $? -eq 1 ]
then
    for element in "${@:1}"
    do
        service $element status &> /dev/null
        if [ $? -eq 0 ]; then
            echo $element
        fi
    done
else
    for element in "${@:1}"
    do
        systemctl status $element &> /dev/null
        if [ $? -eq 0 ]; then
            echo $element
        else
            service $element status
            if [ $? -eq 0 ]; then
                echo $element
            fi
        fi
    done
fi