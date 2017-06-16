#!/bin/bash

export CLOUDCONDUCTOR_URL="server:8090"
export TEMPLATE_NAME="template"
export MACHINE_IP=`/sbin/ifconfig em1 | grep "inet addr" | awk -F: '{print $2}' | awk '{print $1}'`
export UUID="dummy"