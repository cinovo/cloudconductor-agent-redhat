#!/bin/bash

# the url and port of the cloudconductor. Try to avoid protocol prefixes.
export CLOUDCONDUCTOR_URL="localhost:8090"
#the name of the template to use by this agent
export TEMPLATE_NAME="template"
#the protocol the agent might use for REST calls. Options: http or https.
export COM_PROTOCOL="http"
# the uuid of this agent. Will be filled automatically.
export UUID="uuid"