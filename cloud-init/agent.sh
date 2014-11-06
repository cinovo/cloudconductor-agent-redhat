#!/bin/bash

# Usage
#
# export CLOUDCONDUCTOR_URL=
# export TEMPLATE_NAME=
# curl https://raw.githubusercontent.com/cinovo/cloudconductor-agent/v2.7/cloud-init/agent.sh | bash

rpm -ivh http://yum.cloudconductor.net/cloudconductor-agent-2.7-1.noarch.rpm

cat > /opt/cloudconductor-agent/env.sh <<EOF
export CLOUDCONDUCTOR_URL=$CLOUDCONDUCTOR_URL
export TEMPLATE_NAME=$TEMPLATE_NAME
EOF

service cloudconductor-agent start